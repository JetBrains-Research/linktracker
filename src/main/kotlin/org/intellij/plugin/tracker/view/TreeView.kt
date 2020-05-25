package org.intellij.plugin.tracker.view

import org.intellij.plugin.tracker.data.changes.ChangeType
import org.intellij.plugin.tracker.data.changes.LinkChange
import org.intellij.plugin.tracker.data.links.Link
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
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
                file.add(DefaultMutableTreeNode(links.first.linkInfo.linkPath))
            }
            tree.add(file)
        }
        return tree
    }

    private fun createSelectionListener(): TreeSelectionListener? {
        return TreeSelectionListener { e ->
            val pathCount: Int = e.path.pathCount
            val path: String = e.path.getPathComponent(pathCount - 1).toString()
            if (path == "Accept ") {
                val dialogResult = JOptionPane.showConfirmDialog(null,
                        "Would you like to save the change?", "Accept Change", JOptionPane.YES_NO_OPTION)
                if (dialogResult == JOptionPane.YES_OPTION) {
                    // TO DO : method saving change needs to be called
                    println("change accepted")
                }
            }
            if (path == "Deny ") {
                val dialogResult = JOptionPane.showConfirmDialog(null,
                        "Change will not be saved.", "Deny Change", JOptionPane.YES_NO_OPTION)
                if (dialogResult == JOptionPane.YES_OPTION) {
                    // TO DO : method not saving change needs to be called
                    println("change denied")
                }
            }
        }
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
        tree.addTreeSelectionListener(createSelectionListener())
        tree.cellRenderer = CustomCellRenderer()

        // right click listener
        val treePopup = TreePopup(tree)
        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    treePopup.show(e.component, e.x, e.y)
                }
            }
        })

        val scrollPane = JScrollPane(tree)
        add(scrollPane, BorderLayout.CENTER)
    }
}

internal class TreePopup(tree: JTree?) : JPopupMenu() {
    init {
        val add = JMenuItem("Accept")
        val delete = JMenuItem("Deny")
        add.addActionListener { println("Accept change") }
        delete.addActionListener { println("Deny change") }
        add(add)
        add(JSeparator())
        add(delete)
    }
}
