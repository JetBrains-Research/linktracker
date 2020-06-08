package org.intellij.plugin.tracker.view

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import org.intellij.plugin.tracker.LinkTrackerAction
import org.intellij.plugin.tracker.data.ScanResult
import org.intellij.plugin.tracker.data.changes.Change
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.services.LinkUpdaterService
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu

/**
 * [TreePopup] class which is used to accept changes
 */
class TreePopup(
        private val myScanResult: ScanResult,
        private val myInfo: List<MutableList<*>>,
        private val myName: String, line: String, path: String,
        private val myCommitSHA: String?
) : JPopupMenu() {

    private val myProject: Project = myScanResult.myProject
    private val myLinkUpdaterService = LinkUpdaterService(myProject)

    init {
        val changes = myScanResult.myLinkChanges
        val item = JMenuItem("Accept Change")
        item.addActionListener {
            for ((counter, information) in myInfo.withIndex()) {
                if (information[0].toString() == myName && information[1].toString() == path && information[2].toString() == line) {
                    val pair = changes[counter]
                    updateLink(pair.first, pair.second)
                    LinkTrackerAction.run(myProject)
                }
            }
        }
        add(item)
    }

    /**
     * Checks if the link is still valid, if so updates the link, otherwise shows the refresh dialog.
     */
    private fun updateLink(link: Link, change: Change) {

        if (myScanResult.isValid(link)) {
            ApplicationManager.getApplication().runWriteAction {
                WriteCommandAction.runWriteCommandAction(myProject) {
                    myLinkUpdaterService.updateLinks(mutableListOf(Pair(link, change)), myCommitSHA)
                }
            }
        } else {
            showRefreshDialog()
        }
    }

    /**
     * Show a popup warning the user that the chosen link is not valid.
     */
    private fun showRefreshDialog() {

        if (RefreshDialog().showAndGet()) {
            LinkTrackerAction.run(project = myScanResult.myProject)
        }
    }

    /**
     * A dialog popup warning the user about an out of date link
     * and asking whether them to rerun a scan to update the links.
     */
    inner class RefreshDialog : DialogWrapper(true) {

        private val text = "This file has changed, do you want to run a new scan to update the links' data?"

        init {
            super.init()
            title = "Link Out Of Date"
        }

        /**
         * method creates the center panel for popup
         */
        override fun createCenterPanel(): JComponent? {
            val dialogPanel = JPanel(BorderLayout())
            val label = JLabel(text)
            label.preferredSize = Dimension(100, 50)
            dialogPanel.add(label, BorderLayout.CENTER)
            return dialogPanel
        }
    }
}
