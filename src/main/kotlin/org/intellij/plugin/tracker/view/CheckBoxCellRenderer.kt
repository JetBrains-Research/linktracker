package org.intellij.plugin.tracker.view

import java.awt.BorderLayout
import java.awt.Component
import java.util.HashMap
import javax.swing.JCheckBox
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeCellRenderer
import javax.swing.tree.TreePath

class CheckBoxCellRenderer(private val nodesCheckingState: HashMap<TreePath, JCheckBoxTree.CheckedNode>) : JPanel(), TreeCellRenderer {
    var checkBox: JCheckBox
    override fun getTreeCellRendererComponent(
        tree: JTree, value: Any,
        selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int,
        hasFocus: Boolean
    ): Component {
        val node = value as DefaultMutableTreeNode
        val obj = node.userObject
        val tp = TreePath(node.path)
        val cn = nodesCheckingState[tp] ?: return this
        checkBox.isSelected = cn.isSelected
        checkBox.text = obj.toString()
        checkBox.isOpaque = cn.isSelected && cn.hasChildren && !cn.allChildrenSelected
        return this
    }

    init {
        this.layout = BorderLayout()
        checkBox = JCheckBox()
        add(checkBox, BorderLayout.CENTER)
        isOpaque = false
    }
}


