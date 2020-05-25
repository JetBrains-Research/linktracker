package org.intellij.plugin.tracker.view

import java.awt.Color
import java.awt.Component
import java.awt.Font.PLAIN
import java.awt.GridLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.TreeCellRenderer

internal class CustomCellRenderer : TreeCellRenderer {
    private var titleLabel = JLabel("")
    private var renderer: JPanel = JPanel()
    private var defaultRenderer = DefaultTreeCellRenderer()
    private var backgroundSelectionColor: Color
    private var backgroundNonSelectionColor: Color

    override fun getTreeCellRendererComponent(
            tree: JTree, value: Any, selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean
    ): Component {
        var returnValue: Component? = null
        if (value is DefaultMutableTreeNode) {
            val userObject = value.userObject
            titleLabel.text = userObject.toString()

            if (value.parent != null) {
                val text = value.parent.toString()
                if (text.contains("Changed Links") || text.contains("Unchanged Links") || text.contains("Invalid Links")) {
                    val texts = userObject.toString().split(" ")
                    titleLabel.text = "<html><font color='rgb(0, 142, 204)'>" + texts[0] + "</font> <font color='gray'>" + texts[1] + "</font></html>"
                }
            }

            if (value.parent != null && value.parent.parent != null && value.parent.parent.parent != null) {
                val text = value.parent.parent.parent.toString()
                if (text.contains("Changed Links") || text.contains("Unchanged Links") || text.contains("Invalid Links")) {
                    val texts = userObject.toString().split(") ")
                    titleLabel.text = "<html><font color='gray'>" + texts[0] + ")</font> <font color='rgb(247, 152, 98)'>" + texts[1] + "</font></html>"
                }
            }

            when {
                userObject.toString().contains("Changed Links") -> {
                    titleLabel.text = "<html>" + "<strong>Changed Links </strong></font> <font color='gray'>" +
                            userObject.toString().substring(13) + "</font></html>"
                }
                userObject.toString().contains("Unchanged Links") -> {
                    titleLabel.text = "<html>" + "<strong>Unchanged Links </strong></font> <font color='gray'>" +
                            userObject.toString().substring(15) + "</font></html>"
                }
                userObject.toString().contains("Invalid Links") -> {
                    titleLabel.text = "<html>" + "<strong>Invalid Links </strong></font> <font color='gray'>" +
                            userObject.toString().substring(13) + "</font></html>"
                }
                else -> { titleLabel.font = titleLabel.font.deriveFont(PLAIN) }
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