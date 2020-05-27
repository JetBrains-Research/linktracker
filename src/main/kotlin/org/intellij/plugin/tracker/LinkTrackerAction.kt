package org.intellij.plugin.tracker

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import org.intellij.plugin.tracker.data.ScanResult
import org.intellij.plugin.tracker.data.changes.ChangeType
import org.intellij.plugin.tracker.data.changes.LinkChange
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.data.links.LinkInfo
import org.intellij.plugin.tracker.data.links.NotSupportedLink
import org.intellij.plugin.tracker.services.HistoryService
import org.intellij.plugin.tracker.services.LinkRetrieverService
import org.intellij.plugin.tracker.services.LinkUpdaterService
import org.intellij.plugin.tracker.services.UIService
import org.intellij.plugin.tracker.utils.GitOperationManager
import org.intellij.plugin.tracker.utils.LinkFactory
import org.intellij.plugin.tracker.utils.LinkProcessingRouter


class LinkTrackerAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val currentProject = event.getData(PlatformDataKeys.PROJECT)

        if (currentProject == null) {
            Messages.showErrorDialog(
                "Please open a project to run the link tracking plugin.",
                "Link Tracker"
            )
            return
        }

        // initialize all services
        val historyService: HistoryService = HistoryService.getInstance(currentProject)
        val linkService: LinkRetrieverService = LinkRetrieverService.getInstance(currentProject)
        val linkUpdateService: LinkUpdaterService = LinkUpdaterService.getInstance(currentProject)
        val uiService: UIService = UIService.getInstance(currentProject)
        val gitOperationManager = GitOperationManager(currentProject)

        val dataParsingTask = DataParsingTask(
            currentProject = currentProject,
            myLinkService = linkService,
            myHistoryService = historyService,
            myGitOperationManager = gitOperationManager,
            myLinkUpdateService = linkUpdateService,
            myUiService = uiService,
            dryRun = true
        )

        ProgressManager.getInstance().run(dataParsingTask)
    }

    /**
     * A runnable task that executes the plugin's main logic:
     * parses links from throughout the project, then for each one,
     * if found to be valid, fetches the changes to the referenced file
     * from the VCS API, then, unless dryRun is enabled, updates them
     * according to the found changes.
     *
     * @param currentProject the IntelliJ Project object on which the plugin is being run.
     * @param myLinkService the service tasked with retrieving links from files
     * @param myHistoryService the service tasked with storing information about past runs of the plugin
     * @param myGitOperationManager the service tasked with retrieving information about version control history
     * @param myLinkUpdateService the service tasked with updating outdated links
     * @param myUiService the service tasked with displaying results of the plugin's operation
     * @param dryRun if true the task will not automatically update links once data retrieval is complete
     */
    class DataParsingTask(
        private val currentProject: Project,
        private val myLinkService: LinkRetrieverService,
        private val myHistoryService: HistoryService,
        private val myGitOperationManager: GitOperationManager,
        private val myLinkUpdateService: LinkUpdaterService,
        private val myUiService: UIService,
        private val dryRun: Boolean
    ) : Task.Backgroundable(currentProject, "Tracking links", true) {

        // initialize lists
        private val linksAndChangesList: MutableList<Pair<Link, LinkChange>> = mutableListOf()
        private val linkInfoList: MutableList<LinkInfo> = mutableListOf()
        private val scanResult: ScanResult = ScanResult(
            linkChanges = linksAndChangesList,
            isValid = true
        )

        fun getResult(): ScanResult {
            return scanResult
        }

        /**
         * Parses links and related changes from the current project.
         */
        override fun run(indicator: ProgressIndicator) {

            ApplicationManager.getApplication().runReadAction {

                myLinkService.getLinks(linkInfoList)

                for (linkInfo in linkInfoList) {
                    indicator.text = "Tracking link with path ${linkInfo.linkPath}.."
                    val link: Link = LinkFactory.createLink(linkInfo, myHistoryService.stateObject.commitSHA)

                    println("[ DataParsingTask ][ run() ] - LINK IS: $link")

                    if (link is NotSupportedLink) {
                        continue
                    }

                    try {
                        linksAndChangesList.add(LinkProcessingRouter.getChangesForLink(link = link))
                        // temporary solution to ignoring not implemented stuff
                    } catch (e: NotImplementedError) {
                        continue
                        // catch any errors that might result from using vcs commands (git).
                    } catch (e: VcsException) {
                        linksAndChangesList.add(
                            Pair(
                                link,
                                LinkChange(
                                    ChangeType.INVALID,
                                    errorMessage = e.message,
                                    afterPath = link.linkInfo.linkPath
                                )
                            )
                        )
                        // horrible generic exception catch: just in case.
                    } catch (e: Exception) {
                        linksAndChangesList.add(
                            Pair(
                                link,
                                LinkChange(
                                    ChangeType.INVALID,
                                    errorMessage = e.message,
                                    afterPath = link.linkInfo.linkPath
                                )
                            )
                        )
                    }
                    // TODO: for each link and change pair, pass it to the core to get final results before showing in the UI.
                }
            }

            myHistoryService.saveCommitSHA(myGitOperationManager.getHeadCommitSHA())
        }

        /**
         * Callback executed when link data parsing is complete.
         */
        override fun onSuccess() {
            if (!dryRun) {
                updateLinks()
            }

            // Setup change listener to detect changes that might invalidate the scanned links' info
            VirtualFileManager.getInstance().addAsyncFileListener(object : AsyncFileListener {
                override fun prepareChange(events: MutableList<out VFileEvent>): AsyncFileListener.ChangeApplier? {
                    if (!scanResult.isValid) {
                        return null
                    }
                    for (event in events) {
                        for (fileRelativePath in scanResult.files) {
                            if (event.path.endsWith(fileRelativePath)) {
                                return object : AsyncFileListener.ChangeApplier {
                                    override fun beforeVfsChange() {
                                        scanResult.isValid = false
                                    }
                                }
                            }
                        }
                    }
                    return null
                }
            }, Disposable { })

            myUiService.updateView(currentProject, scanResult)
        }

        /**
         * Updates outdated links according to the changes parsed.
         */
        fun updateLinks() {
            val statistics =
                mutableListOf<Any>(myLinkService.noOfFiles, myLinkService.noOfLinks, myLinkService.noOfFilesWithLinks)

            // Run linkUpdater thread
            // There should be a better way to wait for the Tracking Links task to finish
            ApplicationManager.getApplication().invokeLater {
                WriteCommandAction.runWriteCommandAction(currentProject) {
                    if (linksAndChangesList.size != 0) {
                        // Debug
                        println("[ DataParsingTask ][ updateLinks() ] - All changes: ")
                        // Debug
                        linksAndChangesList.map { pair -> println(pair) }
                        val result = myLinkUpdateService.updateLinks(
                            linksAndChangesList,
                            myGitOperationManager.getHeadCommitSHA()
                        )
                        // Debug
                        println("[ DataParsingTask ][ updateLinks() ] - Update result: $result")
                    } else {
                        // Debug
                        println("[ DataParsingTask ][ updateLinks() ] - No links to update...")
                    }
                }
            }
        }
    }
}
