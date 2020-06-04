package org.intellij.plugin.tracker.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import com.intellij.psi.PsiManager
import org.intellij.plugin.tracker.data.*
import org.intellij.plugin.tracker.data.changes.*
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.data.links.LinkInfo
import org.intellij.plugin.tracker.data.links.NotSupportedLink
import org.intellij.plugin.tracker.services.*

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
    private val myChangeTrackerService: ChangeTrackerService,
    private val myUiService: UIService,
    private val dryRun: Boolean
) : Task.Backgroundable(currentProject, "Tracking links", true) {

    // initialize lists
    private val myLinksAndChangesList: MutableList<Pair<Link, Change>> = mutableListOf()
    private val myLinkInfoList: MutableList<LinkInfo> = mutableListOf()
    private val myScanResult: ScanResult = ScanResult(myLinkChanges = myLinksAndChangesList, myProject = project)

    fun getResult(): ScanResult {
        return myScanResult
    }

    /**
     * Parses links and related changes from the current project.
     */
    override fun run(indicator: ProgressIndicator) {
        ApplicationManager.getApplication().runReadAction {
            myLinkService.getLinks(myLinkInfoList)
        }
        for (linkInfo: LinkInfo in myLinkInfoList) {
            indicator.text = "Tracking link with path ${linkInfo.linkPath}.."
            val link: Link = LinkFactory.createLink(linkInfo, myHistoryService.stateObject.commitSHA)

            if (link is NotSupportedLink) {
                continue
            }

            try {
                println("LINK IS: $link")
                val change = link.visit(myChangeTrackerService)
                println("CHANGE IS: $change")
                println("AFTER PATH IS: ${change.afterPath}")
                myLinksAndChangesList.add(Pair(link, change))
                // temporary solution to ignoring not implemented stuff
            } catch (e: NotImplementedError) {
                continue
            } catch (e: FileChangeGatheringException) {
                myLinksAndChangesList.add(
                    Pair(
                        link,
                        FileChange(FileChangeType.INVALID, afterPathString = "", errorMessage = e.message)
                    )
                )
            } catch (e: DirectoryChangeGatheringException) {
                myLinksAndChangesList.add(Pair(link, DirectoryChange(DirectoryChangeType.INVALID, afterPathString = "", errorMessage = e.message)))
            } catch (e: LineChangeGatheringException) {
                val lineChange = LineChange(
                    fileChange = e.fileChange,
                    lineChangeType = LineChangeType.INVALID,
                    errorMessage = e.message
                )
                myLinksAndChangesList.add(Pair(link, lineChange))
            } catch (e: LinesChangeGatheringException) {
                val linesChange = LinesChange(
                    fileChange = e.fileChange,
                    linesChangeType = LinesChangeType.INVALID,
                    errorMessage = e.message
                )
                myLinksAndChangesList.add(Pair(link, linesChange))
                // catch any errors that might result from using vcs commands (git).
            } catch (e: VcsException) {
                println("here: ${e.message}")
            }
        }
        try {
            myHistoryService.saveCommitSHA(myGitOperationManager.getHeadCommitSHA())
        } catch (e: VcsException) {

        }
    }


    /**
     * Callback executed when link data parsing is complete.
     */
    override fun onSuccess() {

        if (!dryRun) {
            updateLinks()
        }

        myScanResult.lockResults()
        PsiTreeChangeListenerImpl.updateResult(scanResult = myScanResult)
        PsiManager.getInstance(project).removePsiTreeChangeListener(PsiTreeChangeListenerImpl)
        PsiManager.getInstance(project).addPsiTreeChangeListener(PsiTreeChangeListenerImpl)
        myUiService.updateView(myScanResult)
    }

    /**
     * Updates outdated links according to the changes parsed.
     */
    private fun updateLinks() {

        ApplicationManager.getApplication().invokeLater {
            WriteCommandAction.runWriteCommandAction(currentProject) {
                if (myLinksAndChangesList.size != 0) {
                    myLinksAndChangesList.map { println(it) }
                    val result = myLinkUpdateService.updateLinks(
                        myLinksAndChangesList,
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
        myHistoryService.saveCommitSHA(myGitOperationManager.getHeadCommitSHA())
        myUiService.updateView(myScanResult)
    }
}