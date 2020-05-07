package org.intellij.plugin.tracker.view

import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import org.intellij.plugin.tracker.data.FileChange
import org.intellij.plugin.tracker.data.Link
import org.intellij.plugin.tracker.data.WebLink
import java.awt.BorderLayout
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.swing.JPanel
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * Class creating tree view
 */
class TreeView : JPanel(BorderLayout()) {
    private var tree: Tree

    /**
     * Updating tree view
     */
    fun updateModel(fileChanges: MutableCollection<Pair<Link, FileChange>>) {
        val root = tree.model.root as DefaultMutableTreeNode
        root.removeAllChildren()

        // Adds current links and their information
        val curr = DefaultMutableTreeNode("Current Files & Links")
        val links = fileChanges.groupBy { getFileName(it.first.proveniencePath) }

        for (linkList in links) {
            val file = DefaultMutableTreeNode(linkList.key)
            for (link in linkList.value) {
                val linkTree = DefaultMutableTreeNode(link.first.linkPath)
                addNodeTree("Link Type", link.first.linkType.name, linkTree)
                addNodeTree("Link Text", link.first.linkText, linkTree)
                addNodeTree("Link Path", link.first.linkPath, linkTree)
                addNodeTree("Provenience Path", link.first.proveniencePath, linkTree)
                addNodeTree("Found at Line", link.first.foundAtLineNumber.toString(), linkTree)
                if (link.first is WebLink) {
                    addNodeTree("Platform Name", (link.first as WebLink).platformName, linkTree)
                    addNodeTree("Project Owner Name", (link.first as WebLink).projectOwnerName, linkTree)
                    addNodeTree("Project Name", (link.first as WebLink).projectName, linkTree)
                    addNodeTree("Relative Path", (link.first as WebLink).relativePath, linkTree)
                    addNodeTree("Reference Type", (link.first as WebLink).referenceType.name, linkTree)
                    addNodeTree("Reference Name", (link.first as WebLink).referenceName, linkTree)
                    addNodeTree("Line Referenced", (link.first as WebLink).lineReferenced.toString(), linkTree)
                    addNodeTree("Start Referenced Line", (link.first as WebLink).startReferencedLine.toString(), linkTree)
                    addNodeTree("End Referenced Line", (link.first as WebLink).endReferencedLine.toString(), linkTree)
                }
                file.add(linkTree)
            }
            curr.add(file)
        }
        root.add(curr)

        // Add changes of file
        val changes = DefaultMutableTreeNode("Changes")
        for (change in fileChanges) {
            if (change.second.fileName != null) {
                val file = DefaultMutableTreeNode(change.second.fileName)
                addNodeTree("File Name", change.second.fileName, file)
                addNodeTree("Change Type", change.second.changeType, file)
                addNodeTree("Before Path", change.second.beforePath, file)
                addNodeTree("After Path", change.second.afterPath, file)
                addNodeTree("Move Relative Path", change.second.moveRelativePath, file)
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

    fun getFileName(proveniencePath: String): String {
        val pattern = Pattern.compile("([a-zA-Z-_/]+)/([a-zA-Z0-9-_.]+)")
        val matcher: Matcher = pattern.matcher(proveniencePath)
        if(matcher.matches()) {
            return matcher.group(2)
        } else {
            return ""
        }
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