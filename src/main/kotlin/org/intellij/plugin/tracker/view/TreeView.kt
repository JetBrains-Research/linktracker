package org.intellij.plugin.tracker.view

import org.intellij.plugin.tracker.data.changes.ChangeType
import org.intellij.plugin.tracker.data.changes.LinkChange
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.data.links.WebLink
import org.intellij.plugin.tracker.data.links.checkRelativeLink
import java.awt.BorderLayout
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTree
import javax.swing.event.TreeSelectionListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeCellRenderer


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

        // Adds current links and their information
        val groupedLinks = changes.groupBy { it.first.linkInfo.proveniencePath }

        for (linkList in groupedLinks) {
            val file = DefaultMutableTreeNode(linkList.key)
            for (links in linkList.value) {
                val link = links.first
                val change = links.second
                val linkTree = DefaultMutableTreeNode(checkRelativeLink(link.linkInfo.getMarkdownDirectoryRelativeLinkPath()))
                addNodeTree("Link Text:", link.linkInfo.linkText, linkTree)
                addNodeTree("Link Path:", link.linkInfo.linkPath, linkTree)
                addNodeTree("Provenience Path:", link.linkInfo.proveniencePath, linkTree)
                addNodeTree("Found at Line:", link.linkInfo.foundAtLineNumber.toString(), linkTree)
                if (link is WebLink) {
                    addNodeTree("Platform Name:", link.getPlatformName(), linkTree)
                    addNodeTree("Project Owner Name:", link.getProjectOwnerName(), linkTree)
                    addNodeTree("Project Name:", link.getProjectName(), linkTree)
                    addNodeTree("Relative Path:", link.getPath(), linkTree)
                }

                val changeTree = DefaultMutableTreeNode("Change")
                addNodeTree("Change Type:", change.changeType.toString(), changeTree)
                if (change.errorMessage != null) addNodeTree("Error message:", change.errorMessage, changeTree)
                if (change.changeType == ChangeType.MOVED || change.changeType == ChangeType.DELETED) {
                    if (change.changeType == ChangeType.MOVED) {
                        addNodeTree("After Path:", change.afterPath, changeTree)
                    }
                    addNodeTree("Accept", "", changeTree)
                    addNodeTree("Deny", "", changeTree)
                    linkTree.add(changeTree)
                }
                file.add(linkTree)
            }
            root.add(file)
        }
        (tree.model as DefaultTreeModel).reload()
    }

    /**
     * Adds new node to tree
     */
    private fun addNodeTree(name: String, value: String?, file: DefaultMutableTreeNode) {
        val tree = DefaultMutableTreeNode("$name $value")
        file.add(tree)
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
        val mdFiles = DefaultMutableTreeNode("Markdown Files")
        tree = JTree(mdFiles)
        tree.addTreeSelectionListener(createSelectionListener());
        val renderer: TreeCellRenderer = CustomCellRenderer()
        tree.cellRenderer = renderer
        val scrollPane = JScrollPane(tree)
        layout = BorderLayout()
        add(scrollPane, BorderLayout.CENTER)
    }
}