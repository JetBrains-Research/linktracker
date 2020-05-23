package org.intellij.plugin.tracker.view

import java.awt.Color
import java.awt.Component
import java.awt.GridLayout
import javax.swing.Icon
import javax.swing.ImageIcon
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
            var check = false

            if (value.parent?.parent?.toString() == "Markdown Files") {
                val children = value.children()
                for (child in children) {
                    if (child.toString() == "Change") {
                        check = true
                    }
                }
            }

            if (check) titleLabel.foreground = Color.BLUE else titleLabel.foreground = Color.DARK_GRAY

            when {
                userObject.toString() == "Accept " -> {
                    titleLabel.foreground = Color.GREEN
                    val checkIcon: Icon = ImageIcon(javaClass.getResource("/images/check.png"))
                    titleLabel.icon = checkIcon
                }
                userObject.toString() == "Deny " -> {
                    titleLabel.foreground = Color.RED
                    val crossIcon: Icon = ImageIcon(javaClass.getResource("/images/cross.png"))
                    titleLabel.icon = crossIcon
                }
                else -> { titleLabel.icon = defaultRenderer.openIcon }
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