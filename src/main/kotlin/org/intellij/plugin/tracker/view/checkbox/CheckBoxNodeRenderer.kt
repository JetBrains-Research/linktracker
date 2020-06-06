package org.intellij.plugin.tracker.view.checkbox

import com.intellij.icons.AllIcons
import icons.MarkdownIcons
import org.intellij.plugin.tracker.view.CustomCellRenderer
import org.intellij.plugin.tracker.view.TreeView
import java.awt.Color
import java.awt.Component
import java.awt.Font
import javax.swing.JTree
import javax.swing.UIManager
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.TreeCellRenderer
import javax.swing.tree.TreePath

/**
 * A [TreeCellRenderer] for check box tree nodes.
 */
class CheckBoxNodeRenderer : TreeCellRenderer {
    val panel = CheckBoxNodePanel()
    private val defaultRenderer = DefaultTreeCellRenderer()
    private val selectionForeground: Color
    private val selectionBackground: Color

    // -- TreeCellRenderer methods --
    override fun getTreeCellRendererComponent(
        tree: JTree,
        value: Any, selected: Boolean, expanded: Boolean,
        leaf: Boolean, row: Int, hasFocus: Boolean
    ): Component {
        var data: CheckBoxNodeData? = null
        var path : TreePath? = null
        if (value is DefaultMutableTreeNode) {
            path = TreePath(value.path)
            val userObject = value.userObject
            if (userObject is CheckBoxNodeData) {
                data = userObject
            }
            panel.check.isSelected = false
            panel.isEnabled = tree.isEnabled

            CustomCellRenderer().renderComponents(panel.label, value)

            defaultRenderer.isEnabled = tree.isEnabled
        }
        if (data == null) {
            // not a check box node; return default custom cell renderer
            val renderer = CustomCellRenderer()
            return renderer.getTreeCellRendererComponent(
                tree, value,
                selected, expanded, leaf, row, hasFocus
            )
        }
        var selected: Boolean = true
        val nodes  = TreeView.nodesCheckingState
        for(node in nodes) {
            if(path == node.key) {
                selected = node.value.isChecked
            }
        }
        panel.check.isSelected = selected
        return panel
    }

    init {
        val fontValue = UIManager.getFont("Tree.font")
        if (fontValue != null) panel.label.font = fontValue
        val focusPainted = UIManager.get("Tree.drawsFocusBorderAroundIcon") as Boolean
        panel.check.isFocusPainted = focusPainted
        selectionForeground = defaultRenderer
            .backgroundNonSelectionColor
        selectionBackground = defaultRenderer.backgroundSelectionColor
    }
}