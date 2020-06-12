package org.intellij.plugin.tracker

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import org.intellij.plugin.tracker.data.changes.Change
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.services.LinkUpdaterService
import org.intellij.plugin.tracker.view.TreeView

/**
 * Action for accepting link changes
 */
class AcceptAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val myChanges = TreeView.acceptedChangeList
        val myScanResult = TreeView.ourScanResult
        val myProject = myScanResult.myProject
        val commitSHA = TreeView.myCommitSHA

        updateLinks(myChanges, myProject, commitSHA)
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
