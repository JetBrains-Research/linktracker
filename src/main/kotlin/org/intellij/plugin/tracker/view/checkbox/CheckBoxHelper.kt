package org.intellij.plugin.tracker.view.checkbox

import java.util.HashMap
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath
import org.intellij.plugin.tracker.data.changes.Change
import org.intellij.plugin.tracker.data.changes.ChangeType
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.view.TreeView

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
                val link = DefaultMutableTreeNode("${links.first.linkInfo.linkText} ${links.first.linkInfo.linkPath}")
                file.add(link)
                for (i in 0 until links.second.afterPath.size) {
                    add(link, "(${links.first.linkInfo.foundAtLineNumber}) ${links.second.afterPath[i]}", false)
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
                    link.add(DefaultMutableTreeNode("MESSAGE: ${links.second.errorMessage}"))
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
            var selected = false
            var allSelected = true
            var noSelected = true
            for ((key, value) in TreeView.nodesCheckingState) {
                if (key.pathCount == 3 && value == link.key && value.isChecked) {
                    selected = true
                } else if (key.pathCount == 5 && link.value.contains(value) && !value.isChecked) {
                    allSelected = if(getSiblings(key).size > 0) {
                        oneSiblingChecked(key)
                    } else {
                        false
                    }
                } else if (key.pathCount == 5 && link.value.contains(value) && value.isChecked) {
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
                if(getSiblings(key).size > 0) {
                    if(!oneSiblingChecked(key)) {
                        allOtherSelected = false
                    }
                } else {
                    allOtherSelected = false
                }
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
        println("add $path")
        for (pair in changes) {
            val afterPath = pair.second.afterPath[0]
            val p = "(${pair.first.linkInfo.foundAtLineNumber}) $afterPath"
            println("path $path")
            println("p $p")
            if (path.lastPathComponent.toString() == p || path.lastPathComponent.toString().contains(">$afterPath<")) {
                println("add this $p")
                TreeView.acceptedChangeList.add(pair)
            }
        }
    }

    /**
     * Removes the changes from accepted changes
     */
    fun removeFromAcceptedChangeList(changes: MutableList<Pair<Link, Change>>, path: TreePath) {
        println("remove $path")
        for (pair in changes) {
            val afterPath = pair.second.afterPath[0]
            val p = "(${pair.first.linkInfo.foundAtLineNumber}) $afterPath"
            if (path.lastPathComponent.toString() == p || path.lastPathComponent.toString().contains(">$afterPath<")) {
                println("remove this $p")
                TreeView.acceptedChangeList.remove(pair)
            }        }
    }


    private fun oneSiblingChecked(path: TreePath) : Boolean {
        val siblingNodes = getSiblings(path)
        for(s in siblingNodes) {
            if(s.isChecked) {
                return true
            }
        }
        return false
    }

    fun notSiblingChecked(path: TreePath) : Boolean {
        val siblingNodes = getSiblings(path)
        return if(siblingNodes.size == 0) {
            true
        } else {
            var checkedOther = false
            for(s in siblingNodes) {
                if(s.isChecked) {
                    checkedOther = true
                }
            }
            !checkedOther
        }
    }

    fun getSiblings(path: TreePath): MutableList<CheckBoxNodeData> {
        val siblings = mutableListOf<CheckBoxNodeData>()
        val last = path.lastPathComponent.toString()
        val common = path.toString().replace(last, "").replace("]", "")
        for(node in TreeView.nodesCheckingState) {
            if (node.key.toString().contains(common) && node.key!=path) {
                siblings.add(node.value)
            }
        }
        return siblings
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
                if (key.pathCount == 5 && key.toString().contains(file.first.toString().replace("]", ""))) {
                    list.add(value)
                }
            }
            result[file.second] = list
        }
        return result
    }
}
