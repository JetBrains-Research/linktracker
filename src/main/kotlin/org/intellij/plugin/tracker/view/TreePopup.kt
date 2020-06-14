package org.intellij.plugin.tracker.view

import com.intellij.openapi.project.Project
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import org.intellij.plugin.tracker.data.ScanResult
import org.intellij.plugin.tracker.utils.UpdateManager

/**
 * [TreePopup] class which is used to accept changes
 */
class TreePopup(
    myScanResult: ScanResult,
    private val myInfo: List<MutableList<*>>,
    private val myName: String,
    line: String,
    path: String,
    private val myCommitSHA: String?
) : JPopupMenu() {

    private val myProject: Project = myScanResult.myProject

    init {
        val updateManager = UpdateManager()
        val changes = myScanResult.myLinkChanges
        val item = JMenuItem("Accept Change")
        item.addActionListener {
            for ((counter, information) in myInfo.withIndex()) {
                if (information[0].toString() == myName && information[1].toString() == path && information[2].toString() == line) {
                    val pair = changes[counter]
                    updateManager.updateLinks(mutableListOf(pair), myProject, myCommitSHA)
                    updateManager.removeUpdatedLinks(myScanResult.myLinkChanges, mutableListOf(pair), myProject)
                }
            }
        }
        add(item)
    }
}
