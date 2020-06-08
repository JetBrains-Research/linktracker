package org.intellij.plugin.tracker

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import org.intellij.plugin.tracker.data.ScanResult
import org.intellij.plugin.tracker.data.changes.Change
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.services.LinkUpdaterService
import org.intellij.plugin.tracker.utils.GitOperationManager
import org.intellij.plugin.tracker.view.TreeView
import java.util.HashSet
import javax.swing.tree.TreePath

class AcceptAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {

        var changes = TreeView.acceptedChangeList

        var scanResult = TreeView.scanResults.get(0)
        val project = scanResult.myProject
        val commitSHA = TreeView().myCommitSHA
        for (change in changes) {
            updateLink(change.first, change.second, scanResult, project, commitSHA)
        }
        LinkTrackerAction.run(project)
    }

    /**
     * Checks if the link is still valid, if so updates the link, otherwise shows the refresh dialog.
     */
    private fun updateLink(link: Link, change: Change, scanResult: ScanResult, project: Project, commitSHA: String?) {
        val myLinkUpdaterService = LinkUpdaterService(project)
        ApplicationManager.getApplication().runWriteAction {
            WriteCommandAction.runWriteCommandAction(project) {
                myLinkUpdaterService.updateLinks(mutableListOf(Pair(link, change)), commitSHA)
            }
        }
    }
}
