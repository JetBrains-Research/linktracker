package org.intellij.plugin.tracker

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vcs.VcsException
import org.intellij.plugin.tracker.data.DirectoryChangeGatheringException
import org.intellij.plugin.tracker.data.FileChangeGatheringException
import org.intellij.plugin.tracker.data.LineChangeGatheringException
import org.intellij.plugin.tracker.data.changes.*
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.data.links.LinkInfo
import org.intellij.plugin.tracker.data.links.NotSupportedLink
import org.intellij.plugin.tracker.services.*
import org.intellij.plugin.tracker.utils.GitOperationManager
import org.intellij.plugin.tracker.utils.LinkFactory


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
        val changeTrackerServiceImpl: ChangeTrackerService = ChangeTrackerServiceImpl.getInstance(currentProject)
        val uiService: UIService = UIService.getInstance(currentProject)
        val gitOperationManager = GitOperationManager(currentProject)

        val dataParsingTask = DataParsingTask(
            currentProject = currentProject,
            linkService = linkService,
            historyService = historyService,
            changeTrackerService = changeTrackerServiceImpl,
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
     * @param changeTrackerService the service responsible for gathering changes
     * @param uiService the service tasked with displaying results of the plugin's operation
     * @param dryRun if true the task will not automatically update links once data retrieval is complete
     */
    class DataParsingTask(
        private val currentProject: Project,
        private val linkService: LinkRetrieverService,
        private val historyService: HistoryService,
        private val gitOperationManager: GitOperationManager,
        private val linkUpdateService: LinkUpdaterService,
        private val changeTrackerService: ChangeTrackerService,
        private val uiService: UIService,
        private val dryRun: Boolean
    ) : Task.Backgroundable(currentProject, "Tracking links", true) {

        // initialize lists
        private val linksAndChangesList: MutableList<Pair<Link, Change>> = mutableListOf()
        private val linkInfoList: MutableList<LinkInfo> = mutableListOf()

        fun getLinks(): MutableList<Pair<Link, Change>> {
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
                    linksAndChangesList.add(Pair(link, link.visit(changeTrackerService)))
                // temporary solution to ignoring not implemented stuff
                } catch (e: NotImplementedError) {
                    continue
                } catch (e: FileChangeGatheringException) {
                    linksAndChangesList.add(Pair(link, FileChange(ChangeType.INVALID, afterPath = "", errorMessage = e.message)))
                } catch (e: DirectoryChangeGatheringException) {
                    linksAndChangesList.add(Pair(link, DirectoryChange(ChangeType.INVALID, errorMessage = e.message)))
                } catch (e: LineChangeGatheringException) {
                    linksAndChangesList.add(Pair(link, LineChange(fileChange = e.fileChange, errorMessage = e.message)))
                // catch any errors that might result from using vcs commands (git).
                } catch (e: VcsException) {

                }
            }
            try {
                historyService.saveCommitSHA(gitOperationManager.getHeadCommitSHA())
            } catch (e: VcsException) {

            }
            uiService.updateView(linksAndChangesList)
        }
    }
}
