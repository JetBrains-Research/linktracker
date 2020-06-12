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
import com.intellij.ui.treeStructure.Tree
import java.awt.BorderLayout
import java.awt.Color
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTree
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.border.Border
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import org.apache.commons.lang.StringUtils.substringBetween
import org.intellij.plugin.tracker.data.ScanResult
import org.intellij.plugin.tracker.data.changes.Change
import org.intellij.plugin.tracker.data.changes.ChangeType
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.utils.GitOperationManager
import org.intellij.plugin.tracker.view.checkbox.CheckBoxHelper
import org.intellij.plugin.tracker.view.checkbox.CheckBoxNodeData
import org.intellij.plugin.tracker.view.checkbox.CheckBoxNodeEditor
import org.intellij.plugin.tracker.view.checkbox.CheckBoxNodeRenderer

/**
 * Class creating tree view
 */
class TreeView : JPanel(BorderLayout()) {

    private var myTree: JTree = Tree(DefaultMutableTreeNode("markdown"))
    private val checkBoxHelper = CheckBoxHelper()
    private var callListenerInfo: List<MutableList<*>> = listOf()

    companion object {
        var checkedPaths = HashSet<TreePath>()
        var nodesCheckingState = HashMap<TreePath, CheckBoxNodeData>()
        var acceptedChangeList: MutableList<Pair<Link, Change>> = mutableListOf()
        lateinit var ourScanResult: ScanResult
        var myCommitSHA: String? = null
    }

    /**
     * Updates the tree model and adds required nodes for links
     */
    fun updateModel(currentScanResult: ScanResult) {
        acceptedChangeList = mutableListOf()
        checkedPaths = HashSet<TreePath>()
        nodesCheckingState = HashMap()

        // Parse data from result
        ourScanResult = currentScanResult
        val changes = currentScanResult.myLinkChanges
        val project = currentScanResult.myProject
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
        calculateCommitSHA()

        val root = myTree.model.root as DefaultMutableTreeNode
        root.removeAllChildren()

        // groups the changes to show in the ui
        val changedOnes = changes.filter {
            (it.second.requiresUpdate || it.second.afterPath.any { path -> it.first.markdownFileMoved(path) }) && it.second.errorMessage == null
        }.groupBy { it.first.linkInfo.proveniencePath }
        val unchangedOnes = changes.filter {
            !it.second.requiresUpdate && it.second.errorMessage == null && !it.second.afterPath.any { path ->
                it.first.markdownFileMoved(
                    path
                )
            }
        }.groupBy { it.first.linkInfo.proveniencePath }
        val invalidOnes = changes.filter { it.second.errorMessage != null }
            .groupBy { it.first.linkInfo.proveniencePath }

        val changed = checkBoxHelper.add(root, "Changed Links " +
                "${changedOnes.map { it.value }.sumBy { it.size }} links", false
        )
        val unchanged = DefaultMutableTreeNode("Unchanged Links " +
                "${unchangedOnes.map { it.value }.sumBy { it.size }} links"
        )
        val invalid = DefaultMutableTreeNode("Invalid Links " +
                "${invalidOnes.map { it.value }.sumBy { it.size }} links"
        )

        val info = changes.map {
            mutableListOf(
                it.first.linkInfo.linkPath, it.first.linkInfo.proveniencePath,
                it.first.linkInfo.foundAtLineNumber
            )
        }

        callListenerInfo = info
        callListener()

        // adds created nodes to tree according to their groups
        root.add(checkBoxHelper.addCheckBoxNodeTree(changedOnes, changed))
        root.add(addNodeTree(unchangedOnes, unchanged))
        root.add(addNodeTree(invalidOnes, invalid))
        (myTree.model as DefaultTreeModel).reload()
    }

    /**
     * Method which calculates the commitSHA
     */
    private fun calculateCommitSHA() {
        myCommitSHA = try {
            ProgressManager.getInstance()
                .runProcessWithProgressSynchronously<String?, VcsException>(
                    { ourScanResult.myProject.let { GitOperationManager(it).getHeadCommitSHA() } },
                    "Getting head commit SHA..",
                    true,
                    ourScanResult.myProject
                )
        } catch (e: VcsException) {
            null
        }
    }

    /**
     * Adds a new node to the tree
     */
    private fun addNodeTree(
        changeList: Map<String, List<Pair<Link, Change>>>,
        node: DefaultMutableTreeNode
    ): DefaultMutableTreeNode {
        for (linkList in changeList) {
            val fileName = linkList.value[0].first.linkInfo.fileName
            var path = linkList.key.replace(fileName, "")
            if (path.endsWith("/")) {
                path = path.dropLast(1)
            }
            val file = DefaultMutableTreeNode("$fileName $path")

            // for each link adds nodes to the tree
            for (links in linkList.value) {
                val link = DefaultMutableTreeNode("${links.first.linkInfo.linkText} ${links.first.linkInfo.linkPath}")
                link.add(
                    DefaultMutableTreeNode(
                        "(${links.first.linkInfo.foundAtLineNumber})"
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
                    link.add(DefaultMutableTreeNode("MESSAGE: ${links.second.errorMessage}"))
                }
                file.add(link)
            }
            node.add(file)
        }
        return node
    }

    /**
     * Adds mouse listener for left and right click
     */
    private fun callListener() {

        // adds the mouse listener if it is the first time
        if (myTree.mouseListeners.size < 2) {

            // mouse listener for selection in tree
            myTree.addMouseListener(object : MouseAdapter() {
                override fun mouseReleased(e: MouseEvent) {
                    val selRow = myTree.getRowForLocation(e.x, e.y)
                    val selPath = myTree.getPathForLocation(e.x, e.y)
                    if (selPath != null && selPath.pathCount == 5) {
                        val changed = selPath.getPathComponent(1).toString().contains("Changed Links")
                        val name = selPath.parentPath.lastPathComponent.toString().split(" ").last()
                        val line = substringBetween(selPath.toString(), "(", ")")
                        val paths = selPath.parentPath.parentPath.lastPathComponent.toString().split(" ")
                        var path = paths[0]
                        if (paths[1].toCharArray().isNotEmpty()) {
                            path = paths[1] + "/" + paths[0]
                        }

                        /**
                         * if right mouse button is clicked in this certain level of the tree
                         * shows the tree popup
                         */
                        if (SwingUtilities.isRightMouseButton(e) && changed && !name.contains("MOVED") && !name.contains(
                                "DELETED"
                            )
                        ) {
                            myTree.selectionPath = selPath
                            val treePopup = TreePopup(ourScanResult, callListenerInfo, name, line, path, myCommitSHA)
                            treePopup.show(e.component, e.x, e.y)
                            if (selRow > -1) {
                                myTree.setSelectionRow(selRow)
                            }
                        }

                        /**
                         * if left mouse button clicked in this certain level of the tree
                         * shows the mentioned line in the editor
                         */
                        if (SwingUtilities.isLeftMouseButton(e) && !name.contains("MOVED") && !name.contains("DELETED")) {
                            for (information in callListenerInfo) {
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
                    if (selPath != null) checkCheckBoxes(selPath)
                }
            })
        }
    }

    /**
     * Checks the situation of checkboxes and makes required updates
     */
    private fun checkCheckBoxes(selPath: TreePath) {
        /**
         * if the clicked node is one of our link's node
         * call the relevant methods and make it checked/unchecked
         * and add it to the [checkedPaths] and/or [acceptedChangeList]
         */
        if (nodesCheckingState.keys.contains(selPath)) {
            val data = nodesCheckingState[selPath]
            if (!data!!.isChecked) {
                when (selPath.pathCount) {
                    // case for the first level parent node
                    2 -> {
                        for (node in nodesCheckingState) {
                            if (!node.value.isChecked) {
                                node.value.isChecked = true
                                checkedPaths.add(node.key)
                                if (node.key.pathCount == 5) {
                                    val newPath = selPath.toString().split(",")[3].split(" ").last()
                                    checkBoxHelper.addToAcceptedChangeList(ourScanResult.myLinkChanges, newPath)
                                }
                            }
                        }
                    }
                    // case for second level nodes
                    3 -> {
                        for (node in nodesCheckingState) {
                            if (!node.value.isChecked && node.key.toString()
                                    .contains(selPath.toString().replace("]", ""))
                            ) {
                                node.value.isChecked = true
                                checkedPaths.add(node.key)
                                if (node.key.pathCount == 5) {
                                    val newPath = node.key.toString().split(",")[3].split(" ").last()
                                    checkBoxHelper.addToAcceptedChangeList(ourScanResult.myLinkChanges, newPath)
                                }
                            }
                        }
                    }
                    // case for the fourth level nodes
                    else -> {
                        if(CheckBoxHelper().notSiblingChecked(selPath, data)) {
                            data.isChecked = true
                            checkedPaths.add(selPath)
                            val newPath = selPath.toString().split(",")[3].split(" ").last()
                            checkBoxHelper.addToAcceptedChangeList(ourScanResult.myLinkChanges, newPath)
                        }
                    }
                }
            } else {
                when (selPath.pathCount) {
                    // case of the first level parent node
                    2 -> {
                        for (node in nodesCheckingState) {
                            if (node.value.isChecked) {
                                node.value.isChecked = false
                                checkedPaths.remove(node.key)
                                if (node.key.pathCount == 5) {
                                    val newPath = node.key.toString().split(",")[3].split(" ").last()
                                    checkBoxHelper.removeFromAcceptedChangeList(ourScanResult.myLinkChanges, newPath)
                                }
                            }
                        }
                    }
                    // case for second level nodes
                    3 -> {
                        for (node in nodesCheckingState) {
                            if (node.value.isChecked && node.key.toString()
                                    .contains(selPath.toString().replace("]", ""))
                            ) {
                                node.value.isChecked = false
                                checkedPaths.remove(node.key)
                                if (node.key.pathCount == 5) {
                                    val newPath = selPath.toString().split(",")[3].split(" ").last()
                                    checkBoxHelper.removeFromAcceptedChangeList(ourScanResult.myLinkChanges, newPath)
                                }
                            }
                        }
                    }
                    // case for the fourth level nodes
                    else -> {
                        data.isChecked = false
                        checkedPaths.remove(selPath)
                        val newPath = selPath.toString().split(",")[3].split(" ").last()
                        checkBoxHelper.removeFromAcceptedChangeList(ourScanResult.myLinkChanges, newPath)
                    }
                }
            }
            // call @checkChildren method to make parents/children of the respective node selected/unselected
            checkBoxHelper.checkChildren()
            println("nodes checking state $nodesCheckingState")
            println("checked paths $checkedPaths")
            println("accepted $acceptedChangeList")
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

        (myTree.model as DefaultTreeModel).reload()
        myTree.isRootVisible = false

        checkedPaths = HashSet()
        acceptedChangeList = mutableListOf()

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
}
