package org.intellij.plugin.tracker.view

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.ui.SideBorder
import org.apache.commons.lang.StringUtils.substringBetween
import org.intellij.plugin.tracker.data.ScanResult
import org.intellij.plugin.tracker.data.changes.Change
import org.intellij.plugin.tracker.data.changes.ChangeType
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.utils.GitOperationManager
import org.intellij.plugin.tracker.view.checkbox.CheckBoxNodeData
import org.intellij.plugin.tracker.view.checkbox.CheckBoxNodeEditor
import org.intellij.plugin.tracker.view.checkbox.CheckBoxNodeRenderer
import java.awt.BorderLayout
import java.awt.Color
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.util.HashMap
import java.util.HashSet
import javax.swing.*
import javax.swing.border.Border
import javax.swing.event.TreeModelEvent
import javax.swing.event.TreeModelListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

/**
 * Class creating tree view
 */
class TreeView : JPanel(BorderLayout()) {

    private var myTree: JTree = JTree(DefaultMutableTreeNode("markdown"))
    private var myCommitSHA: String? = null
    private lateinit var myScanResult: ScanResult

    companion object {
        var checkedPaths = HashSet<TreePath>()
        var nodesCheckingState = HashMap<TreePath, CheckBoxNodeData>()
        var acceptedChangeList : MutableList<Pair<Link, Change>> = mutableListOf()
        var scanResults : MutableList<ScanResult> = mutableListOf()
    }

    /**
     * Updating tree view
     */
    fun updateModel(scanResult: ScanResult) {
        // Parse data from result
        myScanResult = scanResult
        scanResults.add(myScanResult)
        val changes = scanResult.myLinkChanges
        val project = myScanResult.myProject
        myCommitSHA = try {
            ProgressManager.getInstance()
                .runProcessWithProgressSynchronously<String?, VcsException>(
                    { GitOperationManager(project).getHeadCommitSHA() },
                    "Getting head commit SHA..",
                    true,
                    project
                )
        } catch (e: VcsException) {
            null
        }

        val root = myTree.model.root as DefaultMutableTreeNode
        root.removeAllChildren()


        val changedOnes = changes.filter {
            (it.second.requiresUpdate || it.second.afterPath.any { path -> it.first.markdownFileMoved(path) }) && it.second.errorMessage == null
        }.groupBy { it.first.linkInfo.proveniencePath }
        val unchangedOnes = changes.filter {
            !it.second.requiresUpdate && it.second.errorMessage == null && !it.second.afterPath.any { path -> it.first.markdownFileMoved(path) }
        }.groupBy { it.first.linkInfo.proveniencePath }
        val invalidOnes = changes.filter { it.second.errorMessage != null }
            .groupBy { it.first.linkInfo.proveniencePath }

        val changed = add(root, "Changed Links ${count(changedOnes)} links", false)
        val unchanged = DefaultMutableTreeNode("Unchanged Links ${count(unchangedOnes)} links")
        val invalid = DefaultMutableTreeNode("Invalid Links ${count(invalidOnes)} links")

        val info = changes.map {
            mutableListOf(
                it.first.linkInfo.linkPath, it.first.linkInfo.proveniencePath,
                it.first.linkInfo.foundAtLineNumber
            )
        }

        callListener(info)

        root.add(addCheckBoxNodeTree(changedOnes, changed))
        root.add(addNodeTree(unchangedOnes, unchanged))
        root.add(addNodeTree(invalidOnes, invalid))
        (myTree.model as DefaultTreeModel).reload()
    }

    private fun addNodeTree(changeList: Map<String, List<Pair<Link, Change>>>, node: DefaultMutableTreeNode):
            DefaultMutableTreeNode {
        for (linkList in changeList) {
            val fileName = linkList.value[0].first.linkInfo.fileName
            var path = linkList.key.replace(fileName, "")
            if (path.endsWith("/")) {
                path = path.dropLast(1)
            }
            val file = DefaultMutableTreeNode("$fileName $path")
            for (links in linkList.value) {
                val link = DefaultMutableTreeNode(links.first.linkInfo.linkPath)
                link.add(
                    DefaultMutableTreeNode(
                        "(${links.first.linkInfo.foundAtLineNumber}) " +
                                links.first.linkInfo.linkText
                    )
                )
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
                    link.add(DefaultMutableTreeNode("MESSAGE: ${links.second.errorMessage.toString()}"))
                }

                file.add(link)
            }
            node.add(file)
        }

        return node
    }

    private fun addCheckBoxNodeTree(changeList: Map<String, List<Pair<Link, Change>>>, node: DefaultMutableTreeNode):
            DefaultMutableTreeNode {
        for (linkList in changeList) {
            val fileName = linkList.value[0].first.linkInfo.fileName
            var path = linkList.key.replace(fileName, "")
            if (path.endsWith("/")) {
                path = path.dropLast(1)
            }
            val file = add(node, "$fileName $path", false)
            for (links in linkList.value) {
                val link = add(file, links.first.linkInfo.linkPath, false)
                link.add(
                    DefaultMutableTreeNode(
                        "(${links.first.linkInfo.foundAtLineNumber}) " +
                                links.first.linkInfo.linkText
                    )
                )
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
                    link.add(DefaultMutableTreeNode("MESSAGE: ${links.second.errorMessage.toString()}"))
                }

                file.add(link)
            }
            node.add(file)
        }

        return node
    }

    private fun count(list: Map<String, List<Pair<Link, Change>>>): Int {
        var count = 0
        for (el in list) {
            count += el.value.size
        }
        return count
    }

    private fun callListener(info: List<MutableList<*>>) {
        myTree.addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent) {
                val selRow = myTree.getRowForLocation(e.x, e.y)
                val selPath = myTree.getPathForLocation(e.x, e.y)
                if (selPath != null && selPath.pathCount == 5) {
                    val changed = selPath.getPathComponent(1).toString().contains("Changed Links")
                    val name = selPath.parentPath.lastPathComponent.toString()
                    val line = substringBetween(selPath.toString(), "(", ")")
                    val paths = selPath.parentPath.parentPath.lastPathComponent.toString().split(" ")
                    var path = paths[0]
                    if (paths[1].toCharArray().isNotEmpty()) {
                        path = paths[1] + "/" + paths[0]
                    }
                    if (SwingUtilities.isRightMouseButton(e) && changed && !name.contains("MOVED") && !name.contains("DELETED")) {
                        myTree.selectionPath = selPath
                        val treePopup = TreePopup(myScanResult, info, name, line, path, myCommitSHA)
                        treePopup.show(e.component, e.x, e.y)
                        if (selRow > -1) {
                            myTree.setSelectionRow(selRow)
                        }
                    }
                    if (SwingUtilities.isLeftMouseButton(e) && !name.contains("MOVED") && !name.contains("DELETED")) {
                        for (information in info) {
                            if (information[0].toString() == name && information[1].toString() == path && information[2].toString() == line) {
                                val project = ProjectManager.getInstance().openProjects[0]
                                val file = File(project.basePath + "/" + information[1])
                                val virtualFile = VfsUtil.findFileByIoFile(file, true)
                                OpenFileDescriptor(
                                    project, virtualFile!!,
                                    information[2] as Int - 1, 0
                                ).navigate(true)
                            }
                        }
                    }
                }

                // check checkboxes
                if(nodesCheckingState.keys.contains(selPath)) {
                    println("contains")
                    val data = nodesCheckingState[selPath]
                    println(data!!.isChecked)
                    if(!data!!.isChecked) {
                        data!!.isChecked = true
                        checkedPaths.add(selPath)
                        addToAcceptedChangeList(myScanResult.myLinkChanges, selPath)
                    } else {
                        data!!.isChecked = false
                        checkedPaths.remove(selPath)
                        removeFromAcceptedChangeList(myScanResult.myLinkChanges, selPath)
                    }
                }
                println("node checking state $nodesCheckingState")
                println("checked paths $checkedPaths")
                println("accepted changes $acceptedChangeList")
            }
        })
    }

    private fun addToAcceptedChangeList(changes : MutableList<Pair<Link, Change>>, path : TreePath) {
        for( pair in changes) {
            if(pair.first.path == path.lastPathComponent.toString()) {
                acceptedChangeList.add(pair)
            }
        }
    }

    private fun removeFromAcceptedChangeList(changes : MutableList<Pair<Link, Change>>, path : TreePath) {
        for(pair in changes) {
            if(pair.first.path == path.lastPathComponent.toString()) {
                acceptedChangeList.remove(pair)
            }
        }
    }

    /**
     * Constructor of class
     */
    init {

        val root = DefaultMutableTreeNode("Root")
        root.removeAllChildren()

        val treeModel = DefaultTreeModel(root)
        myTree = JTree(treeModel)

        val renderer = CheckBoxNodeRenderer()
        myTree.cellRenderer = renderer

        val editor = CheckBoxNodeEditor(myTree)
        myTree.cellEditor = editor
        myTree.isEditable = true

        // listen for changes in the selection
        myTree.addTreeSelectionListener { println(System.currentTimeMillis().toString() + ": selection changed") }

        // listen for changes in the model (including check box toggles)
        treeModel.addTreeModelListener(object : TreeModelListener {
            override fun treeNodesChanged(e: TreeModelEvent) {
                println(System.currentTimeMillis().toString() + ": nodes changed")
            }

            override fun treeNodesInserted(e: TreeModelEvent) {
                println(System.currentTimeMillis().toString() + ": nodes inserted")
            }

            override fun treeNodesRemoved(e: TreeModelEvent) {
                println(System.currentTimeMillis().toString() + ": nodes removed")
            }

            override fun treeStructureChanged(e: TreeModelEvent) {
                println(System.currentTimeMillis().toString() + ": structure changed")
            }
        })

        (myTree.model as DefaultTreeModel).reload()
        myTree.isRootVisible = false

        val scrollPane = JScrollPane(myTree)
        val border: Border = SideBorder(Color.LIGHT_GRAY, SideBorder.LEFT, 1)
        scrollPane.border = border
        val actionManager: ActionManager = ActionManager.getInstance()
        val actionGroup = DefaultActionGroup("ACTION_GROUP", false)
        actionGroup.add(ActionManager.getInstance().getAction("LinkTracker"))
        actionGroup.add(ActionManager.getInstance().getAction("Settings"))
        actionGroup.add(ActionManager.getInstance().getAction("AcceptChanges"))
        val actionToolbar: ActionToolbar = actionManager.createActionToolbar("ACTION_TOOLBAR", actionGroup, true)
        actionToolbar.setOrientation(SwingConstants.VERTICAL)
        add(actionToolbar.component, BorderLayout.PAGE_START)
        val contentPane = JPanel()
        contentPane.layout = BorderLayout()
        contentPane.add(actionToolbar.component, BorderLayout.WEST)
        contentPane.add(scrollPane, BorderLayout.CENTER)
        add(contentPane, BorderLayout.CENTER)
    }

    private fun add(
        parent: DefaultMutableTreeNode, text: String,
        checked: Boolean
    ): DefaultMutableTreeNode {
        val data = CheckBoxNodeData(text, checked)
        val node = DefaultMutableTreeNode(data)
        parent.add(node)
        nodesCheckingState[TreePath(node.path)] = data
        if(checked) {
            checkedPaths.add(TreePath(node.path))
        }
        return node
    }
}
