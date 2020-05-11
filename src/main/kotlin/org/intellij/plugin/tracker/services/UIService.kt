package org.intellij.plugin.tracker.services

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import org.intellij.plugin.tracker.data.changes.LinkChange
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.view.MDView
import org.intellij.plugin.tracker.view.TreeView

class UIService(val project: Project) {

    private val mdView: MDView = MDView()
    private val treeView: TreeView = TreeView()

    init {
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val contentFactory = ContentFactory.SERVICE.getInstance()

        val treeWindow = toolWindowManager.registerToolWindow("Markdown Files", false, ToolWindowAnchor.BOTTOM)
        val treeContent = contentFactory.createContent(treeView, null, true)
        treeWindow.contentManager.addContent(treeContent)

        val mdWindow = toolWindowManager.registerToolWindow("Statistics", false, ToolWindowAnchor.BOTTOM)
        val mdContent = contentFactory.createContent(mdView, null, true)
        mdWindow.contentManager.addContent(mdContent)
    }

    /**
     * Update the view.
     * @param project the currently open project
     * @param changes changes in the currently open MD file
     */
    fun updateView(project: Project?, fileChanges: MutableList<Pair<Link, LinkChange>>) {
        val toolWindow =
            ToolWindowManager.getInstance(project!!).getToolWindow("Markdown Files")
        treeView.updateModel(fileChanges)
        toolWindow!!.hide(null)
    }

    /**
     * Update the view.
     * @param project the currently open project
     * @param changes changes in the currently open MD file
     */
    fun updateStatistics(project: Project?, statistics: MutableList<Any>) {
        val toolWindow =
            ToolWindowManager.getInstance(project!!).getToolWindow("Statistics")
        mdView.updateModel(statistics)
        toolWindow!!.hide(null)
    }

    /**
     * Used by IDEA to get a reference to the single instance of this class.
     */
    companion object {
        fun getInstance(project: Project): UIService =
            ServiceManager.getService(project, UIService::class.java)
    }
}