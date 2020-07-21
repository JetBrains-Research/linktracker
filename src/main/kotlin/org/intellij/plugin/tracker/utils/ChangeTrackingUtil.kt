package org.intellij.plugin.tracker.utils

import com.intellij.openapi.project.Project
import org.intellij.plugin.tracker.core.change.GitOperationManager
import org.intellij.plugin.tracker.data.InvalidFileChangeTypeException
import org.intellij.plugin.tracker.data.changes.Change
import org.intellij.plugin.tracker.data.changes.CustomChange
import org.intellij.plugin.tracker.data.changes.CustomChangeType
import org.intellij.plugin.tracker.data.diff.FileHistory
import org.intellij.plugin.tracker.data.links.*
import java.io.File

/**
 * Auxiliary function that processed an uncommitted  rename operation
 * performed on a file tracked by git.
 */
internal fun processUncommittedRename(workingTreeChange: CustomChange?, errorMessage: String?): CustomChange {
    if (workingTreeChange != null) {
        val fileHistory = FileHistory(path = workingTreeChange.afterPathString, fromWorkingTree = true)
        workingTreeChange.fileHistoryList = mutableListOf(fileHistory)
        return workingTreeChange
    }
    throw InvalidFileChangeTypeException(errorMessage)
}

internal fun checkCurrentDirectoryContents(project: Project, directoryPath: String): Boolean {
    if (File(project.basePath, directoryPath).exists()) return true
    return false
}

internal fun getLinkStartCommit(gitOperationManager: GitOperationManager, link: Link, throwable: Throwable): String {
    var branchOrTagName: String? = null
    if (link is WebLink<*>) {
        if (link.referenceType == WebLinkReferenceType.COMMIT) return link.referencingName
        if (link.referenceType != WebLinkReferenceType.INVALID) branchOrTagName = link.referencingName
    }
    return (gitOperationManager
        .getStartCommit(link, checkSurroundings = true, branchOrTagName = branchOrTagName)
        ?: throw throwable)
}

/**
 * If the working tree change change type is either deleted or moved
 * (calculated using the unchanged path retrieved from the markdown files),
 * use this change instead of the one found from `git log` command (it overrides it).
 * Otherwise, return the change found from `git log` command.
 */
internal fun matchAndGetOverridingWorkingTreeChange(
    workingTreeChange: CustomChange,
    change: CustomChange
): CustomChange {
    return when (workingTreeChange.customChangeType) {
        CustomChangeType.DELETED, CustomChangeType.MOVED, CustomChangeType.MODIFIED -> {
            change.fileHistoryList.add(
                FileHistory(
                    path = workingTreeChange.afterPathString,
                    fromWorkingTree = true
                )
            )
            workingTreeChange.fileHistoryList = change.fileHistoryList
            return workingTreeChange
        }
        else -> change
    }
}

internal fun convertDirectoryLinkToFileLink(link: Link, filePath: String): Link {
    return if (link is RelativeLinkToDirectory) {
        link.convertToFileLink(filePath)
    } else {
        link as WebLinkToDirectory
        link.convertToFileLink(filePath)
    }
}