package org.intellij.plugin.tracker.utils

import com.intellij.openapi.project.Project
import org.intellij.plugin.tracker.core.change.GitOperationManager
import org.intellij.plugin.tracker.data.InvalidFileChangeTypeException
import org.intellij.plugin.tracker.data.changes.Change
import org.intellij.plugin.tracker.data.changes.CustomChange
import org.intellij.plugin.tracker.data.changes.CustomChangeType
import org.intellij.plugin.tracker.data.diff.FileHistory
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.data.links.WebLink
import org.intellij.plugin.tracker.data.links.WebLinkReferenceType
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

/**
 * So far we have only checked `git log` with the commit that is pointing to HEAD.
 * but we want to also check non-committed changes for file changes.
 * at this point, link was found and a new change has been correctly identified.
 * working tree change can be null (might be because we have first calculated the working tree change
 * using the unchanged path that was retrieved from the markdown file -- this path might have been invalid
 * but now we have a new path that corresponds to the original retrieved path
 * we want to check whether there is any non-committed change that affects this new path
 */
internal fun getWorkingTreeChangeOfNewPath(
    gitOperationManager: GitOperationManager,
    link: Link,
    change: CustomChange
): Change {
    val newLink: Link = link.copyWithAfterPath(link, change.afterPathString)
    val currentChange: CustomChange =
        gitOperationManager.checkWorkingTreeChanges(newLink) ?: return change

    // new change identified (from checking working tree). Use this newly-found change instead.
    change.fileHistoryList.add(
        FileHistory(
            path = currentChange.afterPathString,
            fromWorkingTree = true
        )
    )
    currentChange.fileHistoryList = change.fileHistoryList
    return currentChange
}