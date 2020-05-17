package org.intellij.plugin.tracker.view

import org.intellij.plugin.tracker.data.changes.LinkChange
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.data.links.WebLink
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
        val curr = DefaultMutableTreeNode("Current Files & Links")
        val links = changes.groupBy { it.first.linkInfo.proveniencePath }

        for (linkList in links) {
            val file = DefaultMutableTreeNode(linkList.key)
            for (link in linkList.value) {
                val linkTree = DefaultMutableTreeNode(link.first.linkInfo.getMarkdownDirectoryRelativeLinkPath())
                addNodeTree("Link Text", link.first.linkInfo.linkText, linkTree)
                addNodeTree("Link Path", link.first.linkInfo.linkPath, linkTree)
                addNodeTree("Provenience Path", link.first.linkInfo.proveniencePath, linkTree)
                addNodeTree("Found at Line", link.first.linkInfo.foundAtLineNumber.toString(), linkTree)
                if (link.first is WebLink) {
                    addNodeTree("Platform Name", (link.first as WebLink).getPlatformName(), linkTree)
                    addNodeTree("Project Owner Name", (link.first as WebLink).getProjectOwnerName(), linkTree)
                    addNodeTree("Project Name", (link.first as WebLink).getProjectName(), linkTree)
                    addNodeTree("Relative Path", (link.first as WebLink).getPath(), linkTree)
                }
                file.add(linkTree)
            }
            curr.add(file)
        }
        root.add(curr)

        // Add changes of links
        val changeNode = DefaultMutableTreeNode("Changes")
        for (change in changes) {
            val linkChange = change.second
            val file = DefaultMutableTreeNode(linkChange.changeType.toString())
            addNodeTree("Change Type:", linkChange.changeType.toString(), file)
            addNodeTree("After Path:", linkChange.afterPath, file)
            if (linkChange.errorMessage != null) addNodeTree("Error message:", linkChange.errorMessage, file)
            addNodeTree("Accept Change", "", file)
            addNodeTree("Deny Change", "", file)
            changeNode.add(file)
        }
        root.add(changeNode)
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
            if (path == "Accept Change ") {
                val dialogResult = JOptionPane.showConfirmDialog(null,
                        "Would you like to save the change?", "Accept Change", JOptionPane.YES_NO_OPTION)
                if (dialogResult == JOptionPane.YES_OPTION) {
                    // TO DO : method saving change needs to be called
                    println("change accepted")
                }
            }
            if (path == "Deny Change ") {
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