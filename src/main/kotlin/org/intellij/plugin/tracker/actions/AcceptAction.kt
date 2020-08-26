package org.intellij.plugin.tracker.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.intellij.plugin.tracker.data.changes.Change
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.data.links.RelativeLink
import org.intellij.plugin.tracker.core.update.UpdateManager
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
        val updateManager = UpdateManager()

        val verifiedChanges = mutableListOf<Pair<Link, Change>>()
        for (linkChange in acceptedChanges) {
            if (linkChange.first is RelativeLink<*> && linkChange.second.hasWorkingTreeChanges() &&
                !UpdateManager.WorkingTreeChangeDialog(linkChange.first).showAndGet()) {
                continue
            } else {
                verifiedChanges.add(linkChange)
            }
        }
        updateManager.updateLinks(myScanResult.myLinkChanges, verifiedChanges, myProject, commitSHA)
    }
}
