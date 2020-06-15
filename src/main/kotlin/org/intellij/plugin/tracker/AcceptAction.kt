package org.intellij.plugin.tracker

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.intellij.plugin.tracker.utils.UpdateManager
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

        updateManager.updateLinks(myScanResult.myLinkChanges, acceptedChanges, myProject, commitSHA)
    }
}
