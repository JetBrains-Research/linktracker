package org.intellij.plugin.tracker.services

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.RegisterToolWindowTask
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import org.intellij.plugin.tracker.data.changes.Change
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.view.TreeView

class UIService(val project: Project) {

    private val treeView: TreeView = TreeView()

    init {
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val contentFactory = ContentFactory.SERVICE.getInstance()

        val treeWindow = toolWindowManager
                .registerToolWindow(RegisterToolWindowTask("Markdown Files", ToolWindowAnchor.BOTTOM))
        val treeContent = contentFactory.createContent(treeView, null, true)
        treeWindow.contentManager.addContent(treeContent)
    }

    /**
     * Update the view.
     * @param project the currently open project
     * @param fileChanges changes in the currently open MD file
     */
    fun updateView(project: Project?, fileChanges: MutableList<Pair<Link, Change>>) {
        val toolWindow =
            ToolWindowManager.getInstance(project!!).getToolWindow("Markdown Files")
        treeView.updateModel(fileChanges)
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