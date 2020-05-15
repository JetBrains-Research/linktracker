package org.intellij.plugin.tracker.view

import java.awt.Color
import java.awt.Component
import java.awt.GridLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.TreeCellRenderer


internal class CustomCellRenderer : TreeCellRenderer {
    var titleLabel = JLabel("")
    var renderer: JPanel = JPanel()
    var defaultRenderer = DefaultTreeCellRenderer()
    var backgroundSelectionColor: Color
    var backgroundNonSelectionColor: Color

    override fun getTreeCellRendererComponent(
            tree: JTree, value: Any, selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean
    ): Component {
        var returnValue: Component? = null
        if (value is DefaultMutableTreeNode) {
            val userObject = value.userObject
            titleLabel.text = userObject.toString()
            titleLabel.foreground = Color.BLUE

            if (selected && userObject.toString() == "Accept Change ") {
                titleLabel.foreground = Color.GREEN
            }

            if (selected && userObject.toString() == "Deny Change ") {
                titleLabel.foreground = Color.RED
            }
            renderer.isEnabled = tree.isEnabled
            returnValue = renderer
        }
        if (null == returnValue) {
            returnValue = defaultRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)
        }
        return returnValue!!
    }

    init {
        renderer = JPanel(GridLayout(0, 1))
        titleLabel = JLabel(" ")
        renderer.add(titleLabel)
        backgroundSelectionColor = defaultRenderer
                .backgroundSelectionColor
        backgroundNonSelectionColor = defaultRenderer
                .backgroundNonSelectionColor
    }
}