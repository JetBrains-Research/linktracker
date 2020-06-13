package org.intellij.plugin.tracker.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import org.intellij.plugin.tracker.LinkTrackerAction
import org.intellij.plugin.tracker.data.ScanResult
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
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * Class that handles the update operations
 */
class UpdateManager {

    /**
     * Updates the links in the list.
     */
    fun updateLinks(list: MutableCollection<Pair<Link, Change>>, project: Project, commitSHA: String?) {
        val myLinkUpdaterService = LinkUpdaterService(project)
        ApplicationManager.getApplication().runWriteAction {
            WriteCommandAction.runWriteCommandAction(project) {
                try {
                    myLinkUpdaterService.updateLinks(list, commitSHA)
                } catch (e: Exception) {
                    showRefreshDialog(project)
                }
            }
        }
    }

    /**
     * Removes the updated links from UI and refresh the view.
     */
    fun removeUpdatedLinks(linkAndChange: MutableList<Pair<Link, Change>>, acceptedChanges: MutableList<Pair<Link, Change>>, project: Project) {
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
        val uiService = UIService.getInstance(project)
        val newScanResult = ScanResult(myLinkChanges = linkAndChange, myProject = project)
        uiService.updateView(newScanResult)
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
