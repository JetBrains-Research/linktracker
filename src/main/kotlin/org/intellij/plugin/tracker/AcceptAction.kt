package org.intellij.plugin.tracker

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import org.intellij.plugin.tracker.data.changes.Change
import org.intellij.plugin.tracker.data.changes.CustomChange
import org.intellij.plugin.tracker.data.changes.CustomChangeType
import org.intellij.plugin.tracker.data.changes.LineChange
import org.intellij.plugin.tracker.data.changes.LineChangeType
import org.intellij.plugin.tracker.data.changes.LinesChange
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

        val myChanges = TreeView.acceptedChangeList
        val myScanResult = TreeView.ourScanResult
        val myProject = myScanResult.myProject
        val commitSHA = TreeView.myCommitSHA

        // Check validity of each link
        val validChanges = mutableListOf<Pair<Link, Change>>()
        var allValid = true
        for (pair in myChanges) {
            if (allValid && myScanResult.isValid(pair.first)) {
                validChanges.add(pair)
            } else {
                allValid = false
            }
        }
        if (allValid) {
            updateLinks(validChanges, myProject, commitSHA)
            removeUpdatedLinks(myScanResult.myLinkChanges, validChanges, myProject)
            // LinkTrackerAction.run(myProject)
        } else {
            showRefreshDialog(myProject)
        }
    }

    /**
     * Checks if the link is still valid, if so updates the link, otherwise shows the refresh dialog.
     */
    private fun updateLinks(list: MutableCollection<Pair<Link, Change>>, project: Project, commitSHA: String?) {
        val myLinkUpdaterService = LinkUpdaterService(project)
        ApplicationManager.getApplication().runWriteAction {
            WriteCommandAction.runWriteCommandAction(project) {
                myLinkUpdaterService.updateLinks(list, commitSHA)
            }
        }
    }

    /**
     * Removes the updated links from UI and refresh the view.
     */
    private fun removeUpdatedLinks(linkAndChange: MutableList<Pair<Link, Change>>, acceptedChanges: MutableList<Pair<Link, Change>>, project: Project) {
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
        val myScanResult = TreeView.ourScanResult
        myScanResult.myLinkChanges = linkAndChange
        val uiService = UIService.getInstance(project)
        uiService.updateView(myScanResult)
    }

    /**
     * Show a popup warning the user that the chosen link is not valid.
     */
    private fun showRefreshDialog(project: Project) {

        if (RefreshDialog().showAndGet()) {
            LinkTrackerAction.run(project = project)
        }
    }

    /**
     * A dialog popup warning the user about an out of date link
     * and asking whether them to rerun a scan to update the links.
     */
    inner class RefreshDialog : DialogWrapper(true) {

        private val text = "Some files have changed since you ran the scan, " +
                "and the links' info could be out of date. " +
                "Click 'OK' to run a new scan and update the link info."

        init {
            super.init()
            title = "Link Out Of Date"
        }

        override fun createCenterPanel(): JComponent? {
            val dialogPanel = JPanel(BorderLayout())
            val label = JLabel(text)
            label.preferredSize = Dimension(100, 50)
            dialogPanel.add(label, BorderLayout.CENTER)
            return dialogPanel
        }
    }
}
