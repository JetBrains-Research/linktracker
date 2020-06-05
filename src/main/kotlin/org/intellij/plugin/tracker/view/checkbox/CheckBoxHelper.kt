package org.intellij.plugin.tracker.view.checkbox

import org.intellij.plugin.tracker.data.changes.Change
import org.intellij.plugin.tracker.data.changes.ChangeType
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.view.TreeView
import java.util.HashMap
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

/**
 * Helper methods for checkbox operations.
 */
class CheckBoxHelper {

    /**
     * Adds checkbox node to tree
     */
    fun addCheckBoxNodeTree(changeList: Map<String, List<Pair<Link, Change>>>, node: DefaultMutableTreeNode): DefaultMutableTreeNode {
        for (linkList in changeList) {
            val fileName = linkList.value[0].first.linkInfo.fileName
            var path = linkList.key.replace(fileName, "")
            if (path.endsWith("/")) {
                path = path.dropLast(1)
            }
            val file = add(node, "$fileName $path", false)
            for (links in linkList.value) {
                val link = add(file, links.first.linkInfo.linkPath, false)
                link.add(
                    DefaultMutableTreeNode(
                        "(${links.first.linkInfo.foundAtLineNumber}) " +
                                links.first.linkInfo.linkText
                    )
                )
                if (links.second.requiresUpdate) {
                    var displayString = ""
                    for ((index: Int, changeType: ChangeType) in links.second.changes.withIndex()) {
                        displayString += changeType.changeTypeString
                        if (index != links.second.changes.size - 1) {
                            displayString += " and "
                        }
                    }
                    link.add(DefaultMutableTreeNode(displayString))
                } else if (links.second.errorMessage != null) {
                    link.add(DefaultMutableTreeNode("MESSAGE: ${links.second.errorMessage.toString()}"))
                }

                file.add(link)
            }
            node.add(file)
        }
        return node
    }

    /**
     * Updates nodesCheckingState and checkedPaths
     */
    fun add (parent: DefaultMutableTreeNode, text: String, checked: Boolean): DefaultMutableTreeNode {
        val data = CheckBoxNodeData(text, checked)
        val node = DefaultMutableTreeNode(data)
        parent.add(node)
        TreeView.nodesCheckingState[TreePath(node.path)] = data
        if (checked) {
            TreeView.checkedPaths.add(TreePath(node.path))
        }
        return node
    }

    /**
     * Checks children of a node and makes required updates
     */
    fun checkChildren() {
        if(getParentNode().second!!.isChecked) {
            var last = false
            for(link in getLinkNodes()) {
                if(!link.key.isChecked) {
                    var result = false
                    for(l in link.value) {
                        if(l.isChecked) {
                            result = true
                        }
                    }
                    if(result) {
                        last = true
                    }
                }
                if(link.key.isChecked) {
                    last = true
                }
            }
            if(!last) {
                val node = getParentNode()
                for(n in TreeView.nodesCheckingState) {
                    if(n.key == node.first && n.value == node.second) {
                        n.value.isChecked = false
                        TreeView.checkedPaths.remove(node.first!!)
                    }
                }
            }
        }
    }

    /**
     * Adds the change to accepted changes
     */
    fun addToAcceptedChangeList(changes : MutableList<Pair<Link, Change>>, path : TreePath) {
        for( pair in changes) {
            if(pair.first.path == path.lastPathComponent.toString()) {
                TreeView.acceptedChangeList.add(pair)
            }
        }
    }

    /**
     * Removes the changes from accepted changes
     */
    fun removeFromAcceptedChangeList(changes : MutableList<Pair<Link, Change>>, path : TreePath) {
        for(pair in changes) {
            if(pair.first.path == path.lastPathComponent.toString()) {
                TreeView.acceptedChangeList.remove(pair)
            }
        }
    }

    /**
     * Gets parent node of a node
     */
    private fun getParentNode() : Pair<TreePath?, CheckBoxNodeData?> {
        var result : Pair<TreePath?, CheckBoxNodeData?> = Pair(null, null)
        for(node in TreeView.nodesCheckingState) {
            if(node.key.pathCount==2) {
                result = Pair(node.key, node.value)
            }
        }
        return result
    }

    /**
     * Gets all files nodes with their data
     */
    private fun getFileNodes() : MutableList<Pair<TreePath, CheckBoxNodeData>> {
        val result : MutableList<Pair<TreePath, CheckBoxNodeData>> = mutableListOf()
        val paths = mutableListOf<TreePath>()
        for(node in TreeView.nodesCheckingState) {
            if(node.key.pathCount==3) {
                val pair = Pair(node.key, node.value)
                if(!paths.contains(node.key)) {
                    result.add(pair)
                    paths.add(node.key)
                }
            }
        }
        return result
    }

    /**
     * Gets all link nodes with their data
     */
    private fun getLinkNodes() : HashMap<CheckBoxNodeData, MutableList<CheckBoxNodeData>> {
        val result : HashMap<CheckBoxNodeData, MutableList<CheckBoxNodeData>> = HashMap()
        for(file in getFileNodes()) {
            val list = mutableListOf<CheckBoxNodeData>()
            for(node in TreeView.nodesCheckingState) {
                if(node.key.pathCount==4 && node.key.toString().contains(file.first.toString().replace("]", ""))) {
                    list.add(node.value)
                }
            }
            result[file.second] = list
        }
        return result
    }
}