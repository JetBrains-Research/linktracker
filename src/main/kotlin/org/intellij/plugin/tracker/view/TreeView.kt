package org.intellij.plugin.tracker.view

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.ui.SideBorder
import org.apache.commons.lang.StringUtils.substringBetween
import org.intellij.plugin.tracker.LinkTrackerAction
import org.intellij.plugin.tracker.data.ScanResult
import org.intellij.plugin.tracker.data.changes.ChangeType
import org.intellij.plugin.tracker.data.changes.LinkChange
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.services.LinkUpdaterService
import org.intellij.plugin.tracker.utils.GitOperationManager
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.*
import javax.swing.border.Border
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * Class creating tree view
 */
class TreeView : JPanel(BorderLayout()) {

    private var myTree: JTree
    private lateinit var myScanResult: ScanResult

    /**
     * Updating tree view
     */
    fun updateModel(scanResult: ScanResult) {
        myScanResult = scanResult
        val changes = scanResult.myLinkChanges
        val root = myTree.model.root as DefaultMutableTreeNode
        root.removeAllChildren()

        val changedOnes = changes.filter {
            it.second.changeType == ChangeType.DELETED
                    || it.second.changeType == ChangeType.MOVED
        }.groupBy { it.first.linkInfo.proveniencePath }
        val unchangedOnes = changes.filter {
            it.second.changeType == ChangeType.ADDED
                    || it.second.changeType == ChangeType.MODIFIED
        }.groupBy { it.first.linkInfo.proveniencePath }
        val invalidOnes = changes.filter { it.second.changeType == ChangeType.INVALID }
            .groupBy { it.first.linkInfo.proveniencePath }

        val changed = DefaultMutableTreeNode("Changed Links ${count(changedOnes)} links")
        val unchanged = DefaultMutableTreeNode("Unchanged Links ${count(unchangedOnes)} links")
        val invalid = DefaultMutableTreeNode("Invalid Links ${count(invalidOnes)} links")

        val info = changes.map {
            mutableListOf(
                it.first.linkInfo.linkPath, it.first.linkInfo.proveniencePath,
                it.first.linkInfo.foundAtLineNumber
            )
        }

        callListener(info)

        root.add(addNodeTree(changedOnes, changed))
        root.add(addNodeTree(unchangedOnes, unchanged))
        root.add(addNodeTree(invalidOnes, invalid))
        (myTree.model as DefaultTreeModel).reload()
    }

    private fun addNodeTree(changeList: Map<String, List<Pair<Link, LinkChange>>>, node: DefaultMutableTreeNode):
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
                if (links.second.changeType == ChangeType.MOVED || links.second.changeType == ChangeType.DELETED) {
                    link.add(DefaultMutableTreeNode(links.second.changeType.toString()))
                } else if (links.second.changeType == ChangeType.INVALID) {
                    link.add(DefaultMutableTreeNode("MESSAGE: " + links.second.errorMessage.toString()))
                }

                file.add(link)
            }
            node.add(file)
        }

        return node
    }

    private fun count(list: Map<String, List<Pair<Link, LinkChange>>>): Int {
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
                    if (SwingUtilities.isRightMouseButton(e) && changed && name != "MOVED" && name != "DELETED") {
                        myTree.selectionPath = selPath
                        val treePopup = TreePopup(myScanResult, info, name, line, path)
                        treePopup.show(e.component, e.x, e.y)
                        if (selRow > -1) {
                            myTree.setSelectionRow(selRow)
                        }
                    }
                    if (SwingUtilities.isLeftMouseButton(e) && name != "MOVED" && name != "DELETED") {
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
            }
        })
    }

    /**
     * Constructor of class
     */
    init {
        myTree = JTree(DefaultMutableTreeNode("markdown"))
        val root = myTree.model.root as DefaultMutableTreeNode
        root.removeAllChildren()
        root.add(DefaultMutableTreeNode("Changed Links"))
        root.add(DefaultMutableTreeNode("Unchanged Links"))
        root.add(DefaultMutableTreeNode("Invalid Links"))
        (myTree.model as DefaultTreeModel).reload()
        myTree.isRootVisible = false
        myTree.cellRenderer = CustomCellRenderer()
        val scrollPane = JScrollPane(myTree)
        val border: Border = SideBorder(Color.LIGHT_GRAY, SideBorder.LEFT, 1)
        scrollPane.border = border
        val actionManager: ActionManager = ActionManager.getInstance()
        val actionGroup = DefaultActionGroup("ACTION_GROUP", false)
        actionGroup.add(ActionManager.getInstance().getAction("LinkTracker"))
        actionGroup.add(ActionManager.getInstance().getAction("Settings"))
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

/**
 * A popup button prompting the user to update the link.
 */
class TreePopup(
    private val myScanResult: ScanResult,
    private val myInfo: List<MutableList<*>>,
    private val myName: String, line: String, path: String
) : JPopupMenu() {

    private val myProject: Project = myScanResult.myProject
    private val myLinkUpdaterService: LinkUpdaterService = LinkUpdaterService(myProject)
    private val headCommitSHA: String? = try {
        ProgressManager.getInstance()
            .runProcessWithProgressSynchronously<String?, VcsException>(
                { GitOperationManager(myProject).getHeadCommitSHA() },
                "Getting head commit SHA..",
                true,
                myProject
            )
    } catch (e: VcsException) {
        null
    }

    init {
        val changes = myScanResult.myLinkChanges
        val item = JMenuItem("Accept Change")
        item.addActionListener {
            for ((counter, information) in myInfo.withIndex()) {
                if (information[0].toString() == myName && information[1].toString() == path && information[2].toString() == line) {
                    val pair = changes[counter]
                    updateLink(pair.first, pair.second)
                }
            }
        }
        add(item)
    }

    /**
     * Checks if the link is still valid, if so updates the link, otherwise shows the refresh dialog.
     */
    private fun updateLink(link: Link, change: LinkChange) {

        if (myScanResult.isValid(link)) {
            ApplicationManager.getApplication().runWriteAction {
                WriteCommandAction.runWriteCommandAction(myProject) {
                    myLinkUpdaterService.updateLinks(mutableListOf(Pair(link, change)), headCommitSHA)
                }
            }
        } else {
            showRefreshDialog()
        }
    }

    /**
     * Show a popup warning the user that the chosen link is not valid.
     */
    private fun showRefreshDialog() {

        if (RefreshDialog().showAndGet()) {
            LinkTrackerAction.run(project = myScanResult.myProject)
        }
    }

    /**
     * A dialog popup warning the user about an out of date link
     * and asking whether them to rerun a scan to update the links.
     */
    inner class RefreshDialog : DialogWrapper(true) {

        private val text = "This file has changed, do you want to run a new scan to update the links' data?"

        init {
            super.init()
            title = "Link Out Of Date"
        }

        override fun createCenterPanel(): JComponent? {
            val dialogPanel = JPanel(BorderLayout())
            val label = JLabel(text)
            label.preferredSize = Dimension(100, 50)
            dialogPanel.add(label, BorderLayout.CENTER)
            return dialogPanel
        }
    }
}
