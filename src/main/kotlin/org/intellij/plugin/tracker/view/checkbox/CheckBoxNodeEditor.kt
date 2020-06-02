package org.intellij.plugin.tracker.view.checkbox

import java.awt.Component
import java.awt.event.ItemListener
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.AbstractCellEditor
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeCellEditor

/**
 * A [TreeCellEditor] for check box tree nodes.
 */
class CheckBoxNodeEditor(private val theTree: JTree) : AbstractCellEditor(), TreeCellEditor {
    private val renderer = CheckBoxNodeRenderer()
    override fun getCellEditorValue(): Any {
        val panel = renderer.panel
        return CheckBoxNodeData(panel.label.text, panel.check.isSelected)
    }

    override fun isCellEditable(event: EventObject): Boolean {
        if (event !is MouseEvent) return false
        val mouseEvent = event
        val path = theTree.getPathForLocation(mouseEvent.x, mouseEvent.y) ?: return false
        val node = path.lastPathComponent as? DefaultMutableTreeNode ?: return false
        val userObject = node.userObject
        return userObject is CheckBoxNodeData
    }

    override fun getTreeCellEditorComponent(
        tree: JTree,
        value: Any, selected: Boolean, expanded: Boolean,
        leaf: Boolean, row: Int
    ): Component {
        val editor = renderer.getTreeCellRendererComponent(
            tree, value, true, expanded, leaf,
            row, true
        )

        // editor always selected / focused
        val itemListener = ItemListener {
            if (stopCellEditing()) {
                fireEditingStopped()
            }
        }
        if (editor is CheckBoxNodePanel) {
            editor.check.addItemListener(itemListener)
        }
        return editor
    }

}