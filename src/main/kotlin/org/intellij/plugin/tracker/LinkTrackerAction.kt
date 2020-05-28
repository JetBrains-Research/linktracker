package org.intellij.plugin.tracker

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vcs.VcsException
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
            linkService = linkService,
            historyService = historyService,
            gitOperationManager = gitOperationManager,
            linkUpdateService = linkUpdateService,
            uiService = uiService,
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
    ) : Task.Backgroundable(currentProject, "Tracking links", true) {

        // initialize lists
        private val linksAndChangesList: MutableList<Pair<Link, LinkChange>> = mutableListOf()
        private val linkInfoList: MutableList<LinkInfo> = mutableListOf()

        fun getLinks(): MutableList<Pair<Link, LinkChange>> {
            return linksAndChangesList
        }

        /**
         * Parses links and related changes from the current project.
         */
        override fun run(indicator: ProgressIndicator) {
            ApplicationManager.getApplication().runReadAction {
                linkService.getLinks(linkInfoList)
            }
            for (linkInfo: LinkInfo in linkInfoList) {
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
            }
            historyService.saveCommitSHA(gitOperationManager.getHeadCommitSHA()!!)
            uiService.updateView(linksAndChangesList)
        }
    }
}
