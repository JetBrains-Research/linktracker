package org.intellij.plugin.tracker.core

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import com.intellij.psi.PsiManager
import org.intellij.plugin.tracker.core.change.ChangeTracker
import org.intellij.plugin.tracker.core.change.GitOperationManager
import org.intellij.plugin.tracker.data.ChangeGatheringException
import org.intellij.plugin.tracker.data.changes.Change
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.data.links.LinkInfo
import org.intellij.plugin.tracker.data.links.NotSupportedLink
import org.intellij.plugin.tracker.data.results.ScanResult
import org.intellij.plugin.tracker.services.LinkRetrieverService
import org.intellij.plugin.tracker.services.LinkUpdaterService
import org.intellij.plugin.tracker.services.UIService
import org.intellij.plugin.tracker.utils.LinkFactory
import org.intellij.plugin.tracker.utils.PsiTreeChangeListenerImpl

/**
 * A runnable task that executes the plugin's main logic:
 * parses links from throughout the project, then for each one,
 * if found to be valid, fetches the changes to the referenced file
 * from the VCS API, then, unless dryRun is enabled, updates them
 * according to the found changes.
 *
 * @param currentProject the IntelliJ Project object on which the plugin is being run.
 * @param myLinkService the service tasked with retrieving links from files
 * @param myLinkUpdateService the service tasked with updating outdated links
 * @param dryRun if true the task will not automatically update links once data retrieval is complete
 */
class DataParsingTask(
    private val currentProject: Project,
    private val myLinkService: LinkRetrieverService,
    private val myLinkUpdateService: LinkUpdaterService,
    private val myChangeTrackerService: ChangeTracker,
    private val myUIService: UIService = UIService.getInstance(currentProject),
    private val dryRun: Boolean = true
) : Task.Backgroundable(currentProject, "Tracking links", true) {
    // initialize lists
    private val myLinksAndChangesList: MutableList<Pair<Link, Change>> = mutableListOf()
    private val myLinkInfoList: MutableList<LinkInfo> = mutableListOf()
    private val myScanResult: ScanResult =
        ScanResult(
            myLinkChanges = myLinksAndChangesList,
            myProject = project
        )

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
            val link: Link = LinkFactory.createLink(linkInfo)
            if (link is NotSupportedLink) {
                continue
            }
            try {
                myLinksAndChangesList.add(Pair(link, link.visit(myChangeTrackerService)))
            } catch (e: ChangeGatheringException) {
                myLinksAndChangesList.add(Pair(link, e.change))
            } catch (e: VcsException) {
            }
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
        myUIService.updateView(myScanResult)
    }

    /**
     * Updates outdated links according to the changes parsed.
     */
    private fun updateLinks() {
        ApplicationManager.getApplication().invokeLater {
            if (myLinksAndChangesList.size != 0) {
                myLinkUpdateService.updateLinks(
                    myLinksAndChangesList,
                    GitOperationManager(myProject).getHeadCommitSHA()
                )
            }
        }
    myUIService.updateView(myScanResult)
}
}
