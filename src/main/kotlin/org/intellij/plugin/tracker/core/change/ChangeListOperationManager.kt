package org.intellij.plugin.tracker.core.change

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.util.diff.Diff
import org.intellij.plugin.tracker.data.FileNotFoundInChangeListsException
import org.intellij.plugin.tracker.data.LocalDirectoryNeverExistedException
import org.intellij.plugin.tracker.data.diff.Line
import org.intellij.plugin.tracker.data.OriginalLineContentsNotFoundException
import org.intellij.plugin.tracker.data.OriginalLinesContentsNotFoundException
import org.intellij.plugin.tracker.data.changes.Change
import org.intellij.plugin.tracker.data.changes.CustomChange
import org.intellij.plugin.tracker.data.changes.CustomChangeType
import org.intellij.plugin.tracker.data.diff.DiffOutput
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.utils.getContextLinesFromFileContents
import org.intellij.plugin.tracker.utils.getLinesFromFileContents
import java.io.File
import kotlin.math.abs

class ChangeListOperationManager(val project: Project): ChangeSource() {

    private val changeListManager: ChangeListManager = ChangeListManager.getInstance(project)

    override fun getChangeForFile(link: Link): Change {
        val filePath = link.path
        val change = getChangeCorrespondingToPath(filePath, changeListManager.allChanges)
        if (change == null) {
            if (File(project.basePath, filePath).exists()) return CustomChange(CustomChangeType.ADDED, filePath)
            throw FileNotFoundInChangeListsException()
        }
        return CustomChange.convertChangeToCustomChange(project, change)
    }

    private fun getChangeCorrespondingToPath(
        path: String,
        changeList: MutableCollection<com.intellij.openapi.vcs.changes.Change>
    ): com.intellij.openapi.vcs.changes.Change? {
        for (change in changeList) {
            val beforePath = change.beforeRevision?.file?.path?.removePrefix(project.basePath as CharSequence)
            if (beforePath == path) {
                return change
            }
            if (beforePath != null && beforePath.startsWith("/")) {
                if (beforePath.removePrefix("/") == path) {
                    return change
                }
            }
        }
        return null
    }

    override fun getDiffOutput(fileChange: CustomChange): MutableList<DiffOutput> {
        val diffOutputList: MutableList<DiffOutput> = mutableListOf()
        val addedLines: MutableList<Line> = mutableListOf()
        val deletedLines: MutableList<Line> = mutableListOf()
        if (fileChange.beforeContent != null && fileChange.afterContent != null) {
            val diffResult = Diff.buildChanges(fileChange.beforeContent, fileChange.afterContent)
            if (diffResult != null) {
                for (diffChange in diffResult.toList()) {
                    if (diffChange.inserted != 0) {
                        addedLines.addAll(
                            getLinesFromFileContents(
                                fileChange.afterContent as String,
                                diffChange.line1,
                                diffChange.line1 + diffChange.inserted
                            )
                        )
                    }
                    if (diffChange.deleted != 0) {
                        deletedLines.addAll(
                            getLinesFromFileContents(
                                fileChange.beforeContent as String,
                                diffChange.line0,
                                diffChange.line0 + diffChange.deleted
                            )
                        )
                    }
                }
            }
        }
        diffOutputList.add(DiffOutput(fileChange.afterPathString, addedLines, deletedLines))
        return diffOutputList
    }

    override fun getLines(link: Link): List<String>? {
        val fileChange = getChangeForFile(link) as CustomChange
        return if (fileChange.customChangeType == CustomChangeType.MODIFIED) {
            fileChange.beforeContent?.lines()
        } else {
            File(project.basePath, fileChange.afterPathString).readText().lines()
        }
    }

    override fun getDirectoryInfo(link: Link): DirectoryInfo {
        val directoryChanges = getChangesForDirectory(link.path)
        if (directoryChanges.isEmpty()) {
            throw LocalDirectoryNeverExistedException()
        }
        return DirectoryInfo(directoryChanges.map { change -> change.afterPath[0] }, directoryChanges.size)
    }

    private fun getChangesForDirectory(directoryPath: String): List<Change> {
        return getChangesCorrespondingToPath(directoryPath, changeListManager.allChanges)
            .map { change -> CustomChange.convertChangeToCustomChange(project, change) }
    }

    private fun getChangesCorrespondingToPath(
        path: String,
        changeList: MutableCollection<com.intellij.openapi.vcs.changes.Change>
    ): MutableCollection<com.intellij.openapi.vcs.changes.Change> {
        val filteredChangeList = mutableListOf<com.intellij.openapi.vcs.changes.Change>()
        for (change in changeList) {
            val beforePath = change.beforeRevision?.file?.path?.removePrefix(project.basePath as CharSequence)
            if (beforePath != null) {
                if (beforePath.startsWith(path)) {
                    filteredChangeList.add(change)
                } else if (beforePath.startsWith("/")) {
                    if (beforePath.removePrefix("/").startsWith(path)) {
                        filteredChangeList.add(change)
                    }
                }
            }
        }
        return filteredChangeList
    }
}