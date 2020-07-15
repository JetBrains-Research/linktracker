package org.intellij.plugin.tracker.services

import com.intellij.icons.AllIcons
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.RegisterToolWindowTask
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import org.intellij.plugin.tracker.data.results.ScanResult
import org.intellij.plugin.tracker.view.TreeView

class UIService(val project: Project) {

    private val treeView: TreeView = TreeView()

    init {
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val contentFactory = ContentFactory.SERVICE.getInstance()

        val treeWindow = toolWindowManager.registerToolWindow(
            RegisterToolWindowTask("Markdown Files", ToolWindowAnchor.BOTTOM, icon = AllIcons.Vcs.ShowUnversionedFiles))
        val treeContent = contentFactory.createContent(treeView, "Links", true)
        treeContent.isCloseable = false
        treeWindow.contentManager.addContent(treeContent)
        treeWindow.contentManager.setSelectedContent(treeContent)
    }

    /**
     * Update the view.
     * @param scanResult changes in the currently open MD file
     */
    fun updateView(scanResult: ScanResult) {
        treeView.updateModel(scanResult)
    }

    /**
     * Used by IDEA to get a reference to the single instance of this class.
     */
    companion object {
        fun getInstance(project: Project): UIService =
            ServiceManager.getService(project, UIService::class.java)
    }
}
