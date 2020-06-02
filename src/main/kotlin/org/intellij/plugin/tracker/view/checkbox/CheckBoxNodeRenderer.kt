package org.intellij.plugin.tracker.view.checkbox

import com.intellij.icons.AllIcons
import icons.MarkdownIcons
import org.intellij.plugin.tracker.view.CustomCellRenderer
import java.awt.Color
import java.awt.Component
import java.awt.Font
import javax.swing.JTree
import javax.swing.UIManager
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.TreeCellRenderer

/**
 * A [TreeCellRenderer] for check box tree nodes.
 */
class CheckBoxNodeRenderer : TreeCellRenderer {
    val panel = CheckBoxNodePanel()
    private val defaultRenderer = DefaultTreeCellRenderer()
    private val selectionForeground: Color
    private val selectionBackground: Color
    private val textForeground: Color
    private val textBackground: Color

    // -- TreeCellRenderer methods --
    override fun getTreeCellRendererComponent(
        tree: JTree,
        value: Any, selected: Boolean, expanded: Boolean,
        leaf: Boolean, row: Int, hasFocus: Boolean
    ): Component {
        var data: CheckBoxNodeData? = null
        if (value is DefaultMutableTreeNode) {
            val userObject = value.userObject
            if (userObject is CheckBoxNodeData) {
                data = userObject
            }
            //val stringValue = tree.convertValueToText(value, selected, expanded, leaf, row, false)
            panel.label.text = userObject.toString()
            panel.label.icon = MarkdownIcons.EditorActions.Link
            panel.check.isSelected = false
            panel.isEnabled = tree.isEnabled
            if (selected) {
                panel.foreground = selectionForeground
                panel.background = selectionBackground
                panel.label.foreground = selectionForeground
                panel.label.background = selectionBackground
            } else {
                panel.foreground = textForeground
                panel.background = textBackground
                panel.label.foreground = textForeground
                panel.label.background = textBackground
            }

            if (panel.label.text.contains("DELETED") || panel.label.text.contains("MOVED")) {
                panel.label.icon = AllIcons.General.BalloonInformation
            } else if (panel.label.text.contains("MESSAGE: ")) {
                panel.label.icon = AllIcons.General.BalloonWarning
            } else {
                if (value.parent != null) {
                    val text = value.parent.toString()
                    if (text.contains("Changed Links") || text.contains("Unchanged Links") || text.contains("Invalid Links")) {
                        val texts = userObject.toString().split(" ")
                        panel.label.text = "<html><font color='rgb(0, 142, 204)'>" + texts[0] + "</font> <font color='gray'>" + texts[1] + "</font></html>"
                        panel.label.icon = MarkdownIcons.MarkdownPlugin
                    }
                }

                if (value.parent != null && value.parent.parent != null && value.parent.parent.parent != null) {
                    val text = value.parent.parent.parent.toString()
                    if (text.contains("Changed Links") || text.contains("Unchanged Links") || text.contains("Invalid Links")) {
                        val texts = userObject.toString().split(") ")
                        panel.label.text = "<html><font color='gray'>" + texts[0] + ")</font> <font color='rgb(162, 33, 147)'>" + texts[1] + "</font></html>"
                        panel.label.icon = AllIcons.General.TodoDefault
                    }
                }
            }

            when {
                userObject.toString().contains("Changed Links") -> {
                    panel.label.text = "<html>" + "<strong>Changed Links </strong></font> <font color='gray'>" +
                            userObject.toString().substring(13) + "</font></html>"
                    panel.label.icon = null
                }
                userObject.toString().contains("Unchanged Links") -> {
                    panel.label.text = "<html>" + "<strong>Unchanged Links </strong></font> <font color='gray'>" +
                            userObject.toString().substring(15) + "</font></html>"
                    panel.label.icon = null
                }
                userObject.toString().contains("Invalid Links") -> {
                    panel.label.text = "<html>" + "<strong>Invalid Links </strong></font> <font color='gray'>" +
                            userObject.toString().substring(13) + "</font></html>"
                    panel.label.icon = null
                }
                else -> { panel.label.font = panel.label.font.deriveFont(Font.PLAIN) }
            }
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
        //panel.label.text = data.text
        panel.check.isSelected = data.isChecked
        return panel
    }

    init {
        val fontValue = UIManager.getFont("Tree.font")
        if (fontValue != null) panel.label.font = fontValue
        val focusPainted = UIManager.get("Tree.drawsFocusBorderAroundIcon") as Boolean
        panel.check.isFocusPainted = focusPainted != null && focusPainted
        selectionForeground = UIManager.getColor("Tree.selectionForeground")
        selectionBackground = UIManager.getColor("Tree.selectionBackground")
        textForeground = UIManager.getColor("Tree.textForeground")
        textBackground = UIManager.getColor("Tree.textBackground")
    }
}