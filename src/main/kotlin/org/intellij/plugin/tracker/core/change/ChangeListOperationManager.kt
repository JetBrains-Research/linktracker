package org.intellij.plugin.tracker.core.change

import com.intellij.history.core.ChangeCollectingVisitor
import com.intellij.history.core.changes.*
import com.intellij.history.integration.IdeaGateway
import com.intellij.history.integration.LocalHistoryImpl
import com.intellij.openapi.project.Project
import com.intellij.util.diff.Diff
import org.intellij.plugin.tracker.data.changes.Change as LinkTrackerChange
import org.intellij.plugin.tracker.data.changes.CustomChange
import org.intellij.plugin.tracker.data.changes.CustomChangeType
import org.intellij.plugin.tracker.data.diff.DiffOutput
import org.intellij.plugin.tracker.data.diff.FileHistory
import org.intellij.plugin.tracker.data.diff.Line
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.services.HistoryService
import org.intellij.plugin.tracker.utils.ChangeSourceUtil
import org.intellij.plugin.tracker.utils.getLinesFromFileContents
import java.io.File

class ChangeListOperationManager(val project: Project) : ChangeSource() {

    private val changeSourceUtil = ChangeSourceUtil(project)

    private fun getLocalHistory(absoluteFilePath: String, timestamp: Long): MutableList<Change> {
        val vcs = LocalHistoryImpl.getInstanceImpl().facade
        val collector = ChangeCollectingVisitor(absoluteFilePath, project.locationHash, null)
        vcs!!.accept(collector)
        val timeStampFilteredChangeSets = collector.changes
            .filter { set -> set.timestamp > timestamp }
            .sortedBy { set -> set.timestamp }
        val localChangesList: MutableList<Change> = mutableListOf()
        for (changeSet in timeStampFilteredChangeSets) {
            for (change in changeSet.changes.iterator()) localChangesList.add(change)
        }
        return localChangesList
    }

    private fun handleCreateFileChange(change: CreateFileChange, linkPath: String, fileHistoryList: MutableList<FileHistory>): CustomChange {
        val fullPath = changeSourceUtil.getFilePath(change, linkPath)
        val trimmedPath = changeSourceUtil.removeProjectBasePath(fullPath)
        val actualFileContent = changeSourceUtil.getContentAtPath(fullPath)
        if (actualFileContent != null) fileHistoryList.add(FileHistory(path = trimmedPath, content = actualFileContent))
        return CustomChange(CustomChangeType.ADDED, trimmedPath, fileHistoryList = fileHistoryList)
    }

    private fun handleContentChange(
        it: MutableListIterator<Change>,
        previousPath: String,
        change: ContentChange,
        linkPath: String,
        fileHistoryList: MutableList<FileHistory>
    ): CustomChange? {
        val fileContent = IdeaGateway().stringFromBytes(change.oldContent.bytes, change.path)
        val trimmedPath = changeSourceUtil.removeProjectBasePath(change.path)
        if (previousPath == trimmedPath) {
            if (fileContent.isNotBlank() || (fileContent.isBlank() && fileHistoryList.isNotEmpty())) {
                fileHistoryList.add(FileHistory(path = trimmedPath, content = fileContent))
            }
            if (!it.hasNext()) {
                val actualFileContent = File(changeSourceUtil.getFilePath(change, linkPath)).readText()
                fileHistoryList.add(FileHistory(path = trimmedPath, content = actualFileContent))
                if (trimmedPath != linkPath) {
                    return CustomChange(CustomChangeType.MOVED, trimmedPath, fileHistoryList = fileHistoryList)
                }
                return CustomChange(CustomChangeType.MODIFIED, trimmedPath, fileHistoryList = fileHistoryList)
            }
        }
        return null
    }

    private fun handleMoveChange(
        it: MutableListIterator<Change>,
        previousPath: String,
        fullPath: String,
        fileHistoryList: MutableList<FileHistory>,
        newChangeList: MutableList<Change>
    ): CustomChange? {
        val trimmedPath = changeSourceUtil.removeProjectBasePath(fullPath)
        if (newChangeList.isEmpty() && !it.hasNext()) {
            val actualFileContent = changeSourceUtil.getContentAtPath(fullPath)
            if (actualFileContent != null)
                fileHistoryList.add(FileHistory(path = trimmedPath, content = actualFileContent))
            if (trimmedPath == previousPath) {
                return CustomChange(CustomChangeType.ADDED, trimmedPath, fileHistoryList = fileHistoryList)
            }
            return CustomChange(CustomChangeType.MOVED, trimmedPath, fileHistoryList = fileHistoryList)
        }
        addNewChangesToMutableList(it, newChangeList, fullPath)
        return null
    }

    private fun traverseLocalHistoryForFile(link: Link, timestamp: Long): LinkTrackerChange? {
        var changeList = getLocalHistory(changeSourceUtil.getAbsolutePathForLink(link), timestamp)
        val fileHistoryList: MutableList<FileHistory> = mutableListOf()
        var previousPath = link.path
        changeList = changeList.filter { ch -> ch.affectsPath(changeSourceUtil.getAbsolutePathForLink(link)) }.toMutableList()
        val it = changeList.listIterator()
        while (it.hasNext()) {
            when (val change = it.next()) {
                is CreateFileChange -> if (!it.hasNext()) return handleCreateFileChange(change, link.path, fileHistoryList)
                is DeleteChange -> if (!it.hasNext()) return CustomChange(CustomChangeType.DELETED)
                is ContentChange -> {
                    val result = handleContentChange(it, previousPath, change, link.path, fileHistoryList)
                    if (result != null) return result
                }
                is RenameChange, is MoveChange -> {
                    val fullPath = changeSourceUtil.getFilePath(change as StructuralChange, previousPath)
                    val newTempChangeList = getLocalHistory(fullPath, timestamp)
                    newTempChangeList.removeAll(changeList)
                    val result = handleMoveChange(it, previousPath, fullPath, fileHistoryList, newTempChangeList)
                    if (result != null) return result
                    previousPath = changeSourceUtil.removeProjectBasePath(fullPath)
                }
            }
        }
        return null
    }

    override fun getChangeForFile(link: Link): LinkTrackerChange {
        val timestamp = HistoryService.getInstance(project).getTimestamp(link.linkInfo.proveniencePath, link.linkInfo.linkPath)
        val change = traverseLocalHistoryForFile(link, timestamp)
        if (change == null) {
            if (File(changeSourceUtil.getAbsolutePathForLink(link)).exists()) {
                return CustomChange(CustomChangeType.ADDED, link.path)
            }
            return CustomChange(CustomChangeType.DELETED)
        }
        return change
    }

    override fun performDiffOutput(before: FileHistory, after: FileHistory): DiffOutput? {
        val addedLines: MutableList<Line> = mutableListOf()
        val deletedLines: MutableList<Line> = mutableListOf()
        val diffResult = Diff.buildChanges(before.content as CharSequence, after.content as CharSequence)
        if (diffResult != null) {
            for (diffChange in diffResult.toList()) {
                if (diffChange.inserted != 0) {
                    addedLines.addAll(
                        getLinesFromFileContents(
                            after.content,
                            diffChange.line1,
                            diffChange.line1 + diffChange.inserted
                        )
                    )
                }
                if (diffChange.deleted != 0) {
                    deletedLines.addAll(
                        getLinesFromFileContents(
                            before.content,
                            diffChange.line0,
                            diffChange.line0 + diffChange.deleted
                        )
                    )
                }
            }
        }
        return DiffOutput(before.path, addedLines, deletedLines)
    }

    override fun getLines(link: Link): List<String>? = try {
        (getChangeForFile(link) as CustomChange).fileHistoryList.first().content?.lines()
    } catch (e: NoSuchElementException) {
        File(changeSourceUtil.getAbsolutePathForLink(link)).readLines()
    }

    private fun addNewChangesToMutableList(it: MutableListIterator<Change>, changeList: List<Change>, path: String) {
        for (change in changeList.asReversed()) {
            it.add(change)
            it.previous()
        }
    }

    override fun getChangeForDirectory(link: Link): LinkTrackerChange {
        val timestamp = HistoryService.getInstance(project).getTimestamp(link.linkInfo.proveniencePath, link.linkInfo.linkPath)
        val changes = getLocalHistory(changeSourceUtil.getAbsolutePathForLink(link), timestamp)
        var previousPath = link.path
        val it = changes.listIterator()
        while (it.hasNext()) {
            when (val change = it.next()) {
                is DeleteChange -> if (!it.hasNext()) return CustomChange(CustomChangeType.DELETED)
                is CreateDirectoryChange -> if (!it.hasNext()) return CustomChange(CustomChangeType.ADDED, changeSourceUtil.removeProjectBasePath(change.path))
                is MoveChange, is RenameChange -> {
                    change as StructuralChange
                    if (previousPath.startsWith(changeSourceUtil.removeProjectBasePath(change.oldPath))) {
                        val newPath = changeSourceUtil.getNewPathUsingDirectoryChangePath(change, previousPath, true)
                        val trimmedPath = changeSourceUtil.removeProjectBasePath(newPath)
                        previousPath = trimmedPath

                        val newChanges = getLocalHistory(newPath, timestamp)
                        newChanges.removeAll(changes)
                        if (newChanges.isEmpty() && !it.hasNext()) {
                            return CustomChange(CustomChangeType.MOVED, trimmedPath)
                        }
                        addNewChangesToMutableList(it, newChanges, previousPath)
                    }
                }
            }
        }
        return CustomChange(CustomChangeType.DELETED)
    }
}