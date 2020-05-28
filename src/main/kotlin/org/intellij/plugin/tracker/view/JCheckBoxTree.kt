package org.intellij.plugin.tracker.view

import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.util.*
import javax.swing.JTree
import javax.swing.event.EventListenerList
import javax.swing.tree.*


class JCheckBoxTree : JTree() {
    var selfPointer = this

    inner class CheckedNode(
        var isSelected: Boolean,
        var hasChildren: Boolean,
        var allChildrenSelected: Boolean
    )

    var nodesCheckingState: HashMap<TreePath, CheckedNode>? = null
    var checkedPaths = HashSet<TreePath>()
    var list = EventListenerList()

    class CheckChangeEvent(source: Any?) : EventObject(source)

    interface CheckChangeEventListener : EventListener {
        fun checkStateChanged(event: CheckChangeEvent?)
    }

    fun addCheckChangeEventListener(listener: CheckChangeEventListener?) {
        list.add(CheckChangeEventListener::class.java, listener)
    }

    fun removeCheckChangeEventListener(listener: CheckChangeEventListener?) {
        list.remove(CheckChangeEventListener::class.java, listener)
    }

    fun fireCheckChangeEvent(evt: CheckChangeEvent?) {
        val listeners: Array<Any> = listenerList.listenerList
        for (i in listeners.indices) {
            if (listeners[i] === CheckChangeEventListener::class.java) {
                (listeners[i + 1] as CheckChangeEventListener).checkStateChanged(evt)
            }
        }
    }

    override fun setModel(newModel: TreeModel) {
        super.setModel(newModel)
        resetCheckingState()
    }

    fun getCheckedPaths(): Array<TreePath> {
        return checkedPaths.toTypedArray()
    }

    fun isSelectedPartially(path: TreePath): Boolean {
        val cn = nodesCheckingState!![path]
        return cn!!.isSelected && cn.hasChildren && !cn.allChildrenSelected
    }

    private fun resetCheckingState() {
        nodesCheckingState = HashMap()
        checkedPaths = HashSet()
        val node = model.root as DefaultMutableTreeNode
        addSubtreeToCheckingStateTracking(node)
    }

    private fun addSubtreeToCheckingStateTracking(node: DefaultMutableTreeNode) {
        val path = node.path
        val tp = TreePath(path)
        val cn = CheckedNode(false, node.childCount > 0, false)
        nodesCheckingState!![tp] = cn
        for (i in 0 until node.childCount) {
            addSubtreeToCheckingStateTracking(
                tp.pathByAddingChild(node.getChildAt(i)).lastPathComponent as DefaultMutableTreeNode
            )
        }
    }

    private fun updatePredecessorsWithCheckMode(tp: TreePath, check: Boolean) {
        val parentPath = tp.parentPath ?: return
        val parentCheckedNode = nodesCheckingState!![parentPath]
        val parentNode = parentPath.lastPathComponent as DefaultMutableTreeNode
        parentCheckedNode!!.allChildrenSelected = true
        parentCheckedNode.isSelected = false
        for (i in 0 until parentNode.childCount) {
            val childPath = parentPath.pathByAddingChild(parentNode.getChildAt(i))
            val childCheckedNode = nodesCheckingState!![childPath]
            if (!childCheckedNode!!.allChildrenSelected) {
                parentCheckedNode.allChildrenSelected = false
            }
            if (childCheckedNode.isSelected) {
                parentCheckedNode.isSelected = true
            }
        }
        if (parentCheckedNode.isSelected) {
            checkedPaths.add(parentPath)
        } else {
            checkedPaths.remove(parentPath)
        }
        updatePredecessorsWithCheckMode(parentPath, check)
    }

    private fun checkSubTree(tp: TreePath, check: Boolean) {
        val cn = nodesCheckingState!![tp]
        cn!!.isSelected = check
        val node = tp.lastPathComponent as DefaultMutableTreeNode
        for (i in 0 until node.childCount) {
            checkSubTree(tp.pathByAddingChild(node.getChildAt(i)), check)
        }
        cn.allChildrenSelected = check
        if (check) {
            checkedPaths.add(tp)
        } else {
            checkedPaths.remove(tp)
        }
    }

    init {
        setToggleClickCount(0)
        val cellRenderer = nodesCheckingState?.let { CheckBoxCellRenderer(it) }
        setCellRenderer(cellRenderer)

        val dtsm: DefaultTreeSelectionModel = object : DefaultTreeSelectionModel() {
            override fun setSelectionPath(path: TreePath) {}
            override fun addSelectionPath(path: TreePath) {}
            override fun removeSelectionPath(path: TreePath) {}
            override fun setSelectionPaths(pPaths: Array<TreePath>) {}
        }

        addMouseListener(object : MouseListener {
            override fun mouseClicked(arg0: MouseEvent) {
                val tp = selfPointer.getPathForLocation(arg0.x, arg0.y) ?: return
                val checkMode = !nodesCheckingState!![tp]!!.isSelected
                checkSubTree(tp, checkMode)
                updatePredecessorsWithCheckMode(tp, checkMode)
                fireCheckChangeEvent(CheckChangeEvent(Any()))
                selfPointer.repaint()
            }

            override fun mouseEntered(arg0: MouseEvent) {}
            override fun mouseExited(arg0: MouseEvent) {}
            override fun mousePressed(arg0: MouseEvent) {}
            override fun mouseReleased(arg0: MouseEvent) {}
        })
        setSelectionModel(dtsm)
    }
}