package org.intellij.plugin.tracker

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import org.intellij.plugin.tracker.data.ScanResult
import org.intellij.plugin.tracker.data.changes.Change
import org.intellij.plugin.tracker.data.changes.CustomChange
import org.intellij.plugin.tracker.data.changes.LineChange
import org.intellij.plugin.tracker.data.changes.LinesChange
import org.intellij.plugin.tracker.data.changes.CustomChangeType
import org.intellij.plugin.tracker.data.changes.LineChangeType
import org.intellij.plugin.tracker.data.changes.LinesChangeType
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.services.LinkUpdaterService
import org.intellij.plugin.tracker.services.UIService
import org.intellij.plugin.tracker.view.TreeView

/**
 * Action for accepting link changes
 */
class AcceptAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val acceptedChanges = TreeView.acceptedChangeList
        val myScanResult = TreeView.ourScanResult
        val myProject = myScanResult.myProject
        val commitSHA = TreeView.myCommitSHA

        updateLinks(acceptedChanges, myProject, commitSHA)

        val linkAndChange = myScanResult.myLinkChanges

        // Remove updated links from UI
        for (acceptedChange in acceptedChanges) {
            when (val change = acceptedChange.second) {
                is CustomChange -> {
                    linkAndChange.remove(acceptedChange)
                    linkAndChange.add(Pair(acceptedChange.first, CustomChange(CustomChangeType.ADDED, change.afterPathString)))
                }
                is LineChange -> {
                    linkAndChange.remove(acceptedChange)
                    linkAndChange.add(Pair(acceptedChange.first, LineChange(change.fileChange, LineChangeType.UNCHANGED)))
                }
                is LinesChange -> {
                    linkAndChange.remove(acceptedChange)
                    linkAndChange.add(Pair(acceptedChange.first, LinesChange(change.fileChange, LinesChangeType.UNCHANGED)))
                }
            }
        }

        // Update view again to remove the updated links
        // after user accepted the changes
        val uiService = UIService.getInstance(myProject)
        val newScanResult = ScanResult(myLinkChanges = linkAndChange, myProject = myProject)
        uiService.updateView(newScanResult)
    }

    /**
     * Checks if the link is still valid, if so updates the link.
     */
    private fun updateLinks(list: MutableCollection<Pair<Link, Change>>, project: Project, commitSHA: String?) {
        val myLinkUpdaterService = LinkUpdaterService(project)
        ApplicationManager.getApplication().runWriteAction {
            WriteCommandAction.runWriteCommandAction(project) {
                myLinkUpdaterService.updateLinks(list, commitSHA)
            }
        }
    }
}
