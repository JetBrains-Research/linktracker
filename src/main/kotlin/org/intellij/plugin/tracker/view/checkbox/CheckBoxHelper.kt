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
    fun addCheckBoxNodeTree(
        changeList: Map<String, List<Pair<Link, Change>>>,
        node: DefaultMutableTreeNode
    ): DefaultMutableTreeNode {
        for (linkList in changeList) {
            val fileName = linkList.value[0].first.linkInfo.fileName
            var path = linkList.key.replace(fileName, "")
            if (path.endsWith("/")) {
                path = path.dropLast(1)
            }
            val file = add(node, "$fileName $path", false)
            for (links in linkList.value) {
                val link = add(file, "${links.first.linkInfo.linkText} ${links.first.linkInfo.linkPath}", false)
                for(i in 0 until links.second.afterPath.size) {
                    link.add(
                        DefaultMutableTreeNode(
                            "(${links.first.linkInfo.foundAtLineNumber}) ${links.second.afterPath[i]}"
                        )
                    )
                }
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
    fun add(parent: DefaultMutableTreeNode, text: String, checked: Boolean): DefaultMutableTreeNode {
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
        val links = getLinkNodes()
        for (link in links) {
            var selected  = false
            var allSelected = true
            var noSelected = true
            for ((key, value) in TreeView.nodesCheckingState) {
                if (key.pathCount == 3 && value == link.key && value.isChecked) {
                    selected = true
                } else if(key.pathCount == 4 && link.value.contains(value) && !value.isChecked) {
                    allSelected = false
                } else if(key.pathCount == 4 && link.value.contains(value) && value.isChecked) {
                    noSelected = false
                }
            }
            for ((key, value) in TreeView.nodesCheckingState) {
                if (key.pathCount == 3 && value == link.key && !selected && allSelected) {
                    value.isChecked = true
                } else if (key.pathCount == 3 && value == link.key && selected && noSelected) {
                    value.isChecked = false
                }
            }
        }
        var parentSelected = false
        var allOtherSelected = true
        var nonSelected = true
        for ((key, value) in TreeView.nodesCheckingState) {
            if (key.pathCount == 2 && value.isChecked) {
                parentSelected = true
            } else if (key.pathCount != 2 && !value.isChecked) {
                allOtherSelected = false
            } else if (key.pathCount != 2 && value.isChecked) {
                nonSelected = false
            }
        }
        for ((key, value) in TreeView.nodesCheckingState) {
            if (key.pathCount == 2 && !parentSelected && allOtherSelected) {
                value.isChecked = true
            } else if (key.pathCount == 2 && parentSelected && nonSelected) {
                value.isChecked = false
            }
        }
    }

    /**
     * Adds the change to accepted changes
     */
    fun addToAcceptedChangeList(changes: MutableList<Pair<Link, Change>>, path: TreePath) {
        for (pair in changes) {
            if (pair.first.path == path.lastPathComponent.toString()) TreeView.acceptedChangeList.add(pair)
        }
    }

    /**
     * Removes the changes from accepted changes
     */
    fun removeFromAcceptedChangeList(changes: MutableList<Pair<Link, Change>>, path: TreePath) {
        for (pair in changes) {
            if (pair.first.path == path.lastPathComponent.toString()) TreeView.acceptedChangeList.remove(pair)
        }
    }

    /**
     * Gets all files and respective nodes with their data
     */
    private fun getFileNodes(): MutableList<Pair<TreePath, CheckBoxNodeData>> {
        val result: MutableList<Pair<TreePath, CheckBoxNodeData>> = mutableListOf()
        val paths = mutableListOf<TreePath>()
        for ((key, value) in TreeView.nodesCheckingState) {
            if (key.pathCount == 3 && !paths.contains(key)) {
                result.add(Pair(key, value))
                paths.add(key)
            }
        }
        return result
    }

    /**
     * Gets all link nodes with their data
     */
    private fun getLinkNodes(): HashMap<CheckBoxNodeData, MutableList<CheckBoxNodeData>> {
        val result: HashMap<CheckBoxNodeData, MutableList<CheckBoxNodeData>> = HashMap()
        for (file in getFileNodes()) {
            val list = mutableListOf<CheckBoxNodeData>()
            for ((key, value) in TreeView.nodesCheckingState) {
                if (key.pathCount == 4 && key.toString().contains(file.first.toString().replace("]", ""))) {
                    list.add(value)
                }
            }
            result[file.second] = list
        }
        return result
    }
}