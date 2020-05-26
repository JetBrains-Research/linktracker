package org.intellij.plugin.tracker.view

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.ui.SideBorder
import org.intellij.plugin.tracker.data.changes.ChangeType
import org.intellij.plugin.tracker.data.changes.LinkChange
import org.intellij.plugin.tracker.data.links.Link
import java.awt.BorderLayout
import java.awt.Color
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.*
import javax.swing.border.Border
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel


/**
 * Class creating tree view
 */
class TreeView : JPanel(BorderLayout()) {

    private var tree: JTree
    private val project = ProjectManager.getInstance().openProjects[0]

    /**
     * Updating tree view
     */
    fun updateModel(changes: MutableList<Pair<Link, LinkChange>>) {
        val root = tree.model.root as DefaultMutableTreeNode
        root.removeAllChildren()

        val changedOnes = changes.filter { it.second.changeType == ChangeType.DELETED
                || it.second.changeType == ChangeType.MOVED}.groupBy { it.first.linkInfo.proveniencePath }
        val unchangedOnes = changes.filter { it.second.changeType == ChangeType.ADDED
                || it.second.changeType == ChangeType.MODIFIED }.groupBy { it.first.linkInfo.proveniencePath }
        val invalidOnes = changes.filter { it.second.changeType == ChangeType.INVALID }
            .groupBy { it.first.linkInfo.proveniencePath }

        var count = 0
        for (changed in changedOnes) {
            count += changed.value.size
        }

        val changed = DefaultMutableTreeNode("Changed Links ${count(changedOnes)} links")
        val unchanged = DefaultMutableTreeNode("Unchanged Links ${count(unchangedOnes)} links")
        val invalid = DefaultMutableTreeNode("Invalid Links ${count(invalidOnes)} links")

        val info = changes.map {
            mutableListOf(it.first.linkInfo.linkPath, it.first.linkInfo.proveniencePath,
                it.first.linkInfo.foundAtLineNumber) }

        callListener(info)

        root.add(addNodeTree(changedOnes, changed))
        root.add(addNodeTree(unchangedOnes, unchanged))
        root.add(addNodeTree(invalidOnes, invalid))
        (tree.model as DefaultTreeModel).reload()
    }

    private fun addNodeTree(changeList: Map<String, List<Pair<Link, LinkChange>>>, node: DefaultMutableTreeNode):
            DefaultMutableTreeNode {
        for (linkList in changeList) {
            val fileName = linkList.value[0].first.linkInfo.fileName
            var path = linkList.key.replace(fileName, "")
            if (path.endsWith("/")) {
                path = path.dropLast(1)
            }
            val file = DefaultMutableTreeNode("$fileName $path")
            for (links in linkList.value) {
                val link = DefaultMutableTreeNode(links.first.linkInfo.linkPath)
                link.add(DefaultMutableTreeNode("(${links.first.linkInfo.foundAtLineNumber}) " +
                        links.first.linkInfo.linkText))
                file.add(link)
            }
            node.add(file)
        }

        return node
    }

    private fun count(list: Map<String, List<Pair<Link, LinkChange>>>): Int {
        var count = 0
        for (el in list) {
            count += el.value.size
        }
        return count
    }

    private fun callListener(info: List<MutableList<*>>) {
        val treePopup = TreePopup(tree)
        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent) {
                val selRow = tree.getRowForLocation(e.x, e.y)
                val selPath = tree.getPathForLocation(e.x, e.y)
                if (SwingUtilities.isRightMouseButton(e) && selPath != null && selPath.pathCount == 4) {
                    tree.selectionPath = selPath
                    treePopup.show(e.component, e.x, e.y)
                    if (selRow > -1) {
                        tree.setSelectionRow(selRow)
                    }
                }
                if (SwingUtilities.isLeftMouseButton(e) && selPath != null && selPath.pathCount == 4) {
                    val name = selPath.lastPathComponent.toString()
                    val prev = selPath.getPathComponent(selPath.path.size - 2).toString()
                    val path = if (prev.contains("/")) {
                        val paths = prev.split(" ")
                        paths[1] + "/" + paths[0]
                    } else { prev.replace(" ", "") }

                    for (information in info) {
                        if (information[0].toString() == name && information[1].toString() == path) {
                            val file = File(project.basePath + "/" + information[1])
                            val virtualFile = VfsUtil.findFileByIoFile(file, true)
                            OpenFileDescriptor(project, virtualFile!!,
                                information[2] as Int - 1, 0).navigate(true)
                        }
                    }
                }
            }
        })
    }

    /**
     * Constructor of class
     */
    init {
        tree = JTree(DefaultMutableTreeNode("markdown"))
        val root = tree.model.root as DefaultMutableTreeNode
        root.removeAllChildren()
        root.add(DefaultMutableTreeNode("Changed Links"))
        root.add(DefaultMutableTreeNode("Unchanged Links"))
        root.add(DefaultMutableTreeNode("Invalid Links"))
        (tree.model as DefaultTreeModel).reload()
        tree.isRootVisible = false
        tree.cellRenderer = CustomCellRenderer()
        val scrollPane = JScrollPane(tree)
        val border: Border = SideBorder(Color.LIGHT_GRAY, SideBorder.LEFT, 1)
        scrollPane.border = border
        val actionManager: ActionManager = ActionManager.getInstance()
        val actionGroup = DefaultActionGroup("ACTION_GROUP", false)
        actionGroup.add(ActionManager.getInstance().getAction("LinkTracker"))
        val actionToolbar: ActionToolbar = actionManager.createActionToolbar("ACTION_TOOLBAR", actionGroup, true)
        actionToolbar.setOrientation(SwingConstants.VERTICAL)
        add(actionToolbar.component, BorderLayout.PAGE_START)
        val contentPane = JPanel()
        contentPane.layout = BorderLayout()
        contentPane.add(actionToolbar.component, BorderLayout.WEST)
        contentPane.add(scrollPane, BorderLayout.CENTER)
        add(contentPane, BorderLayout.CENTER)
    }
}

class TreePopup(tree: JTree?) : JPopupMenu() {
    init {
        val add = JMenuItem("Accept Change")
        add.addActionListener { println("Accept change") }
        add(add)
    }
}
