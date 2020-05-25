package org.intellij.plugin.tracker.view

import org.intellij.plugin.tracker.data.changes.ChangeType
import org.intellij.plugin.tracker.data.changes.LinkChange
import org.intellij.plugin.tracker.data.links.Link
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.*
import javax.swing.event.TreeSelectionListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel


/**
 * Class creating tree view
 */
class TreeView : JPanel(BorderLayout()) {

    private var tree: JTree

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

        val changed = DefaultMutableTreeNode("Changed Links ${changedOnes.size} links")
        val unchanged = DefaultMutableTreeNode("Unchanged Links ${unchangedOnes.size} links")
        val invalid = DefaultMutableTreeNode("Invalid Links ${invalidOnes.size} links")

        root.add(addNodeTree(changedOnes, changed))
        root.add(addNodeTree(unchangedOnes, unchanged))
        root.add(addNodeTree(invalidOnes, invalid))
        (tree.model as DefaultTreeModel).reload()
    }

    private fun addNodeTree(changeList: Map<String, List<Pair<Link, LinkChange>>>, tree: DefaultMutableTreeNode): DefaultMutableTreeNode {
        for (linkList in changeList) {
            val fileName = linkList.value[0].first.linkInfo.fileName
            var path = linkList.key.replace(fileName, "")
            if (path.endsWith("/")) {
                path = path.dropLast(1)
            }
            val file = DefaultMutableTreeNode("$fileName $path")
            for (links in linkList.value) {
                val link  = DefaultMutableTreeNode(links.first.linkInfo.linkPath)
                link.add(DefaultMutableTreeNode("(${links.first.linkInfo.foundAtLineNumber}, ${links.first.linkInfo.textOffset}) ${links.first.linkInfo.linkText}"))
                file.add(link)
            }
            tree.add(file)
        }
        return tree
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

        // right click listener
        val treePopup = TreePopup(tree)
        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    val selRow = tree.getRowForLocation(e.x, e.y)
                    val selPath = tree.getPathForLocation(e.x, e.y)
                    if (selPath.pathCount == 4) {
                        tree.selectionPath = selPath
                        if (selRow > -1) {
                            tree.setSelectionRow(selRow)
                        }
                        treePopup.show(e.component, e.x, e.y)
                    }
                }
            }
        })

        val scrollPane = JScrollPane(tree)
        add(scrollPane, BorderLayout.CENTER)
    }
}

internal class TreePopup(tree: JTree?) : JPopupMenu() {
    init {
        val add = JMenuItem("Accept Change")
        val delete = JMenuItem("Deny Change")
        add.addActionListener { println("Accept change") }
        delete.addActionListener { println("Deny change") }
        add(add)
        add(JSeparator())
        add(delete)
    }
}
