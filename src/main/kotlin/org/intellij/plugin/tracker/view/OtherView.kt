package org.intellij.plugin.tracker.view

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.ui.SideBorder
import org.intellij.plugin.tracker.view.checkbox.CheckBoxNodeData
import org.intellij.plugin.tracker.view.checkbox.CheckBoxNodeEditor
import org.intellij.plugin.tracker.view.checkbox.CheckBoxNodeRenderer
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTree
import javax.swing.SwingConstants
import javax.swing.border.Border
import javax.swing.event.TreeModelEvent
import javax.swing.event.TreeModelListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath


/**
 * Class creating other view
 */
class OtherView : JPanel(BorderLayout()) {

//    init {
//        val root = DefaultMutableTreeNode("Root")
//
//        val changed = add(root, "Changed Links", false)
//        add(changed, "Link 1", false)
//        add(changed, "Link 2", false)
//        root.add(changed)
//
//        val unchanged = DefaultMutableTreeNode("Unchanged Links")
//        root.add(unchanged)
//
//        val invalid = DefaultMutableTreeNode("Invalid Links")
//        root.add(invalid)
//
//        val treeModel = DefaultTreeModel(root)
//        val tree = JTree(treeModel)
//
//        val renderer = CheckBoxNodeRenderer()
//        tree.cellRenderer = renderer
//
//        val editor = CheckBoxNodeEditor(tree)
//        tree.cellEditor = editor
//        tree.isEditable = true
//
//        // listen for changes in the selection
//
//        // listen for changes in the selection
//        tree.addTreeSelectionListener { println(System.currentTimeMillis().toString() + ": selection changed") }
//
//        // listen for changes in the model (including check box toggles)
//
//        // listen for changes in the model (including check box toggles)
//        treeModel.addTreeModelListener(object : TreeModelListener {
//            override fun treeNodesChanged(e: TreeModelEvent) {
//                println(System.currentTimeMillis().toString() + ": nodes changed")
//            }
//
//            override fun treeNodesInserted(e: TreeModelEvent) {
//                println(System.currentTimeMillis().toString() + ": nodes inserted")
//            }
//
//            override fun treeNodesRemoved(e: TreeModelEvent) {
//                println(System.currentTimeMillis().toString() + ": nodes removed")
//            }
//
//            override fun treeStructureChanged(e: TreeModelEvent) {
//                println(System.currentTimeMillis().toString() + ": structure changed")
//            }
//        })
//
//        (tree.model as DefaultTreeModel).reload()
//        tree.isRootVisible = false
//
//        val scrollPane = JScrollPane(tree)
//        val border: Border = SideBorder(Color.LIGHT_GRAY, SideBorder.LEFT, 1)
//        scrollPane.border = border
//        val actionManager: ActionManager = ActionManager.getInstance()
//        val actionGroup = DefaultActionGroup("ACTION_GROUP", false)
//        actionGroup.add(ActionManager.getInstance().getAction("LinkTracker"))
//        actionGroup.add(ActionManager.getInstance().getAction("Settings"))
//        val actionToolbar: ActionToolbar = actionManager.createActionToolbar("ACTION_TOOLBAR", actionGroup, true)
//        actionToolbar.setOrientation(SwingConstants.VERTICAL)
//        add(actionToolbar.component, BorderLayout.PAGE_START)
//        val contentPane = JPanel()
//        contentPane.layout = BorderLayout()
//        contentPane.add(actionToolbar.component, BorderLayout.WEST)
//        contentPane.add(scrollPane, BorderLayout.CENTER)
//        add(contentPane, BorderLayout.CENTER)
//    }
//
//    private fun add(
//        parent: DefaultMutableTreeNode, text: String,
//        checked: Boolean
//    ): DefaultMutableTreeNode {
//        val data = CheckBoxNodeData(text, checked)
//        val node = DefaultMutableTreeNode(data)
//        parent.add(node)
//        return node
//    }

        init {
            val contentPane = JPanel()
            val cbt = JCheckBoxTree()
            cbt.addCheckChangeEventListener(object : JCheckBoxTree.CheckChangeEventListener {
                override fun checkStateChanged(event: JCheckBoxTree.CheckChangeEvent?) {
                    val paths: Array<TreePath> = cbt.getCheckedPaths()
                    for (tp in paths) {
                        for (pathPart in tp.getPath()) {
                            print("$pathPart,")
                        }
                    }
                }
            })
            contentPane.layout = BorderLayout()
            contentPane.add(cbt)
            add(contentPane, BorderLayout.CENTER)
        }








}

