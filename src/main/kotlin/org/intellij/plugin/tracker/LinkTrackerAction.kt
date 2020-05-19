package org.intellij.plugin.tracker

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vcs.VcsException
import com.intellij.psi.PsiDocumentManager
import org.intellij.plugin.tracker.data.UpdateResult
import org.intellij.plugin.tracker.data.changes.ChangeType
import org.intellij.plugin.tracker.data.changes.LinkChange
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.data.links.LinkInfo
import org.intellij.plugin.tracker.data.links.NotSupportedLink
import org.intellij.plugin.tracker.services.*
import org.intellij.plugin.tracker.utils.GitOperationManager
import org.intellij.plugin.tracker.utils.LinkFactory
import org.intellij.plugin.tracker.utils.LinkProcessingRouter
import java.io.File


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
            linkService = linkService,
            historyService = historyService,
            gitOperationManager = gitOperationManager,
            linkUpdateService = linkUpdateService,
            uiService = uiService,
            dryRun = false
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
     * @param linkService the service tasked with retrieving links from files
     * @param historyService the service tasked with storing information about past runs of the plugin
     * @param gitOperationManager the service tasked with retrieving information about version control history
     * @param linkUpdateService the service tasked with updating outdated links
     * @param uiService the service tasked with displaying results of the plugin's operation
     * @param dryRun if true the task will not automatically update links once data retrieval is complete
     */
    class DataParsingTask(
        private val currentProject: Project,
        private val linkService: LinkRetrieverService,
        private val historyService: HistoryService,
        private val gitOperationManager: GitOperationManager,
        private val linkUpdateService: LinkUpdaterService,
        private val uiService: UIService,
        private val dryRun: Boolean
    ): Task.Modal(currentProject, "Tracking links", true) {

        // initialize lists
        private val linksAndChangesList: MutableList<Pair<Link, LinkChange>> = mutableListOf()
        private val linkInfoList: MutableList<LinkInfo> = mutableListOf()

        /**
         * Parses links and related changes from the current project.
         */
        override fun run(indicator: ProgressIndicator) {

            ApplicationManager.getApplication().runReadAction {
                linkService.getLinks(linkInfoList)
            }

            for (linkInfo in linkInfoList) {
                indicator.text = "Tracking link with path ${linkInfo.linkPath}.."
                val link: Link = LinkFactory.createLink(linkInfo, historyService.stateObject.commitSHA)

                println("LINK IS: $link")

                if (link is NotSupportedLink) {
                    continue
                }

                try {
                    linksAndChangesList.add(LinkProcessingRouter.getChangesForLink(link = link))
                    // temporary solution to ignoring not implemented stuff
                } catch (e: NotImplementedError) {
                    continue
                    // catch any errors that might result from using vcs commands (git).
                } catch(e: VcsException) {
                    linksAndChangesList.add(
                        Pair(link, LinkChange(ChangeType.INVALID, errorMessage = e.message, afterPath = link.linkInfo.linkPath)))
                    // horrible generic exception catch: just in case.
                } catch(e: Exception) {
                    linksAndChangesList.add(
                        Pair(link, LinkChange(ChangeType.INVALID, errorMessage = e.message, afterPath = link.linkInfo.linkPath)))
                }

                // TODO: for each link and change pair, pass it to the core to get final results before showing in the UI.
            }
            // Debug
            println("Link tracking finished!")

            historyService.saveCommitSHA(gitOperationManager.getHeadCommitSHA())
        }

        /**
         * Callback executed when link data parsing is complete.
         */
        override fun onSuccess() {
            if (!dryRun) {
                updateLinks()
            }

            uiService.updateView(currentProject, linksAndChangesList)
        }

        /**
         * Updates outdated links according to the changes parsed.
         */
        fun updateLinks() {
            val statistics =
                mutableListOf<Any>(linkService.noOfFiles, linkService.noOfLinks, linkService.noOfFilesWithLinks)

            // Run linkUpdater thread
            // There should be a better way to wait for the Tracking Links task to finish
            ApplicationManager.getApplication().invokeLater {
                WriteCommandAction.runWriteCommandAction(currentProject) {
                    // Debug
                    println("Finished waiting")
                    if (linksAndChangesList.size != 0) {
                        // Debug
                        println("All changes: ")
                        // Debug
                        linksAndChangesList.map { pair -> println(pair) }
                        val result = linkUpdateService.updateLinks(linksAndChangesList,
                            gitOperationManager.getHeadCommitSHA())
                        // Debug
                        println("Update result: $result")
                    } else {
                        // Debug
                        println("No links to update...")
                    }
                }
            }
        }
    }
}
