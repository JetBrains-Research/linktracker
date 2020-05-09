package org.intellij.plugin.tracker.view

import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import org.intellij.plugin.tracker.data.changes.FileChange
import org.intellij.plugin.tracker.data.changes.LinkChange
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.data.links.WebLink
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import kotlin.reflect.typeOf

/**
 * Class creating tree view
 */
class TreeView : JPanel(BorderLayout()) {
    private var tree: Tree

    /**
     * Updating tree view
     */
    fun updateModel(fileChanges: MutableList<Pair<Link, LinkChange>>) {
        val root = tree.model.root as DefaultMutableTreeNode
        root.removeAllChildren()

        // Adds current links and their information
        val curr = DefaultMutableTreeNode("Current Files & Links")
        val links = fileChanges.groupBy { it.first.proveniencePath }

        for (linkList in links) {
            val file = DefaultMutableTreeNode(linkList.key)
            for (link in linkList.value) {
                val linkTree = DefaultMutableTreeNode(link.first.linkPath)
                //addNodeTree("Link Type", link.first.linkType.name, linkTree)
                addNodeTree("Link Text", link.first.linkText, linkTree)
                addNodeTree("Link Path", link.first.linkPath, linkTree)
                addNodeTree("Provenience Path", link.first.proveniencePath, linkTree)
                addNodeTree("Found at Line", link.first.foundAtLineNumber.toString(), linkTree)
                if (link.first is WebLink) {
                    addNodeTree("Platform Name", (link.first as WebLink).getPlatformName(), linkTree)
                    addNodeTree("Project Owner Name", (link.first as WebLink).getProjectOwnerName(), linkTree)
                    addNodeTree("Project Name", (link.first as WebLink).getProjectName(), linkTree)
                    addNodeTree("Relative Path", (link.first as WebLink).getPath(), linkTree)
                    //addNodeTree("Reference Type", (link.first as WebLink).referenceType.name, linkTree)
                    //addNodeTree("Reference Name", (link.first as WebLink).referenceName, linkTree)
                    //addNodeTree("Line Referenced", (link.first as WebLink).lineReferenced.toString(), linkTree)
                    //addNodeTree("Start Referenced Line", (link.first as WebLink).startReferencedLine.toString(), linkTree)
                    //addNodeTree("End Referenced Line", (link.first as WebLink).endReferencedLine.toString(), linkTree)
                }
                file.add(linkTree)
            }
            curr.add(file)
        }
        root.add(curr)

        // Add changes of file
        val changes = DefaultMutableTreeNode("Changes")
        for (change in fileChanges) {
            if (change.second is FileChange) {
                // for now, cast to FileChange.
                val fileChange = change.second as FileChange
                val file = DefaultMutableTreeNode(fileChange.fileName)
                addNodeTree("File Name", fileChange.fileName, file)
                addNodeTree("Change Type", fileChange.changeType, file)
                addNodeTree("Before Path", fileChange.beforePath, file)
                addNodeTree("After Path", fileChange.afterPath, file)
                addNodeTree("Move Relative Path", fileChange.moveRelativePath, file)
                changes.add(file)
            }
        }
        root.add(changes)

        (tree.model as DefaultTreeModel).reload()
    }

    /**
     * Adds new node to tree
     */
    private fun addNodeTree(name: String, value: String?, file: DefaultMutableTreeNode) {
        val tree = DefaultMutableTreeNode("$name: $value")
        file.add(tree)
    }

    /**
     * Constructor of class
     */
    init {
        val mdFiles = DefaultMutableTreeNode("Markdown Files")
        tree = Tree(DefaultTreeModel(mdFiles))
        val scrollPane = JBScrollPane(tree)
        layout = BorderLayout()
        add(scrollPane, BorderLayout.CENTER)
    }
}