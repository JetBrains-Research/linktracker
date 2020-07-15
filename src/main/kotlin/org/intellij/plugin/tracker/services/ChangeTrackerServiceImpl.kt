package org.intellij.plugin.tracker.services

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import org.intellij.plugin.tracker.core.change.ChangeListOperationManager
import org.intellij.plugin.tracker.core.change.GitOperationManager
import org.intellij.plugin.tracker.core.line.LineTracker
import org.intellij.plugin.tracker.data.*
import org.intellij.plugin.tracker.data.changes.Change
import org.intellij.plugin.tracker.data.changes.CustomChange
import org.intellij.plugin.tracker.data.changes.CustomChangeType
import org.intellij.plugin.tracker.data.diff.DiffOutput
import org.intellij.plugin.tracker.data.diff.FileHistory
import org.intellij.plugin.tracker.data.links.*
import org.intellij.plugin.tracker.utils.*
import org.intellij.plugin.tracker.utils.calculateDirectorySimilarityAndDetermineChange
import org.intellij.plugin.tracker.utils.checkCurrentDirectoryContents
import org.intellij.plugin.tracker.utils.getLinkStartCommit
import org.intellij.plugin.tracker.utils.processUncommittedRename
import java.io.File

/**
 * Implementation of ChangeTrackerService interface, adapted to work in IntelliJ IDEA environment
 */
class ChangeTrackerServiceImpl(project: Project, policy: ChangeTrackingPolicy) : ChangeTrackerService {

    private val myProject = project

    private val myPolicy = policy

    private val myChangeListOperationManager =
        ChangeListOperationManager(project)

    private val myGitOperationManager =
        GitOperationManager(project)

    override fun getLocalFileChanges(link: Link): Change {
        if (myPolicy == ChangeTrackingPolicy.LOCAL) {
            return myChangeListOperationManager.getChangeForFile(link.path)
        }
        return getFileChangeFromGit(link)
    }

    override fun getLocalDirectoryChanges(link: Link): Change {
        val linkPath = link.path
        if (checkCurrentDirectoryContents(myProject, link.path)) {
            return CustomChange(CustomChangeType.ADDED, link.path)
        }
        val movedFiles: List<String>
        val directoryContentsSize: Int
        var deletedFilesCount = -1
        if (myPolicy == ChangeTrackingPolicy.LOCAL) {
            val directoryChanges = myChangeListOperationManager.getChangesForDirectory(linkPath)
            if (directoryChanges.isEmpty()) {
                throw LocalDirectoryNeverExistedException()
            }
            movedFiles = directoryChanges.map { change -> change.afterPath[0] }
            directoryContentsSize = directoryChanges.size
        } else {
            val startCommit = getLinkStartCommit(myGitOperationManager, link, CommitSHAIsNullDirectoryException())
            link.specificCommit = startCommit
            val directoryContents = myGitOperationManager.getDirectoryContentsAtCommit(
                linkPath,
                startCommit
            )
            if (directoryContents == null || directoryContents.size == 0) {
                throw LocalDirectoryNeverExistedException()
            }
            val trackingResult = trackDirectoryContents(link, directoryContents)
            directoryContentsSize = directoryContents.size
            movedFiles = trackingResult.first
            deletedFilesCount = trackingResult.second
        }
        return calculateDirectorySimilarityAndDetermineChange(
            movedFiles,
            linkPath,
            directoryContentsSize,
            myPolicy,
            deletedFilesCount
        )
    }

    /**
     * Get the changes for a single line
     *
     * If a specific commit is not specified (the link is not a permalink), then
     * we need to fetch the start commit of the line containing the link. Otherwise, use `specificCommit`
     * as start commit.
     * Using the start commit, we want to get the original line contents from the file at commit `startCommit`.
     *
     * After that is done, we will retrieve the changes for the file in which the lines are located.
     * Using the file history list of the file change object returned, we will take the git diff
     * of this file's versions, generating a list of diff output objects which contain deleted and added lines.
     *
     * As the final step, the method will call the LineTracker class method `trackLine`, which maps
     * the line to it's new location using the generated diff outputs as input.
     */
    override fun getLocalLineChanges(link: Link): Change {
        val fileChange: CustomChange
        val diffOutputList: MutableList<DiffOutput>
        val originalLineContent: String
        if (myPolicy == ChangeTrackingPolicy.LOCAL) {
            fileChange = getFileChangeForLink(link, FileHasBeenDeletedException())
            diffOutputList = myChangeListOperationManager.getDiffOutput(fileChange)
            originalLineContent = myChangeListOperationManager.getOriginalLineContents(fileChange, link.lineReferenced)
        } else {
            val startCommit = getLinkStartCommit(myGitOperationManager, link, CommitSHAIsNullLineException())
            link.specificCommit = startCommit
            fileChange = getFileChangeForLink(link, FileHasBeenDeletedException())
            diffOutputList = myGitOperationManager.getDiffOutput(fileChange)
            originalLineContent = myGitOperationManager.getContentsOfLineInFileAtCommit(
                startCommit,
                link.path,
                link.lineReferenced
            )
        }
        return LineTracker.trackLine(link, fileChange, originalLineContent, diffOutputList)
    }

    /**
     * Get the changes for multiple lines
     *
     * Ifa specific commit is not specified (the link is not a permalink), then
     * we need to fetch the start commit of the line containing the link. Otherwise, use `specificCommit`
     * as start commit.
     * Using the start commit, we want to get the original lines contents from the file at commit `startCommit`.
     *
     * After that is done, we will retrieve the changes for the file in which the lines are located.
     * Using the file history list of the file change object returned, we will take the git diff
     * of this file's versions, generating a list of diff output objects which contain deleted and added lines.
     *
     * As the final step, the method will call the LineTracker class method `trackLines`, which maps
     * the lines to their new locations using the generated diff outputs as input.
     */
    override fun getLocalLinesChanges(link: Link): Change {
        val fileChange: CustomChange
        val diffOutputList: MutableList<DiffOutput>
        val originalLinesContents: List<String>
        if (myPolicy == ChangeTrackingPolicy.LOCAL) {
            fileChange = getFileChangeForLink(link, FileHasBeenDeletedLinesException())
            diffOutputList = myChangeListOperationManager.getDiffOutput(fileChange)
            originalLinesContents = myChangeListOperationManager.getMultipleOriginalLinesContents(
                fileChange,
                link.referencedStartingLine,
                link.referencedEndingLine
            )
        } else {
            val startCommit = getLinkStartCommit(myGitOperationManager, link, CommitSHAIsNullLinesException())
            link.specificCommit = startCommit
            fileChange = getFileChangeForLink(link, FileHasBeenDeletedLinesException())
            diffOutputList = myGitOperationManager.getDiffOutput(fileChange)
            originalLinesContents = myGitOperationManager.getContentsOfLinesInFileAtCommit(
                startCommit,
                link.path,
                link.referencedStartingLine,
                link.referencedEndingLine
            )
        }
        return LineTracker.trackLines(link, diffOutputList, fileChange, originalLinesContents)
    }

    private fun getFileChangeForLink(link: Link, throwable: Throwable): CustomChange {
        val fileChange = getLocalFileChanges(link) as CustomChange
        if (fileChange.customChangeType == CustomChangeType.DELETED) throw throwable
        return fileChange
    }

    private fun getFileChangeFromGit(link: Link): Change {
        val workingTreeChange: CustomChange? = myGitOperationManager.checkWorkingTreeChanges(link)
        // this file has just been added and is not tracked by git, but the link is considered valid
        if (workingTreeChange != null && workingTreeChange.customChangeType == CustomChangeType.ADDED) {
            val fileHistory = FileHistory(path = workingTreeChange.afterPathString, fromWorkingTree = true)
            workingTreeChange.fileHistoryList = mutableListOf(fileHistory)
            return workingTreeChange
        }
        val change: CustomChange = myGitOperationManager.getGitHistoryChangeForFile(link)
        try {
            if (workingTreeChange == null) return getWorkingTreeChangeOfNewPath(myGitOperationManager, link, change)
            return matchAndGetOverridingWorkingTreeChange(workingTreeChange, change)
        } catch (e: FileChangeGatheringException) {
            // this might be the case when a link corresponds to an uncommitted rename
            // git log history will have no changes when using the new name
            // but the working tree change will capture the rename, so we want to return it
            return processUncommittedRename(workingTreeChange, e.message)
        }
    }

    /**
     * For each file identified as content of a directory at at specific commit,
     * track that file individually through-out git history, identifying thus
     * the set of moved out-files from the directory and the set of deleted files from the directory.
     */
    private fun trackDirectoryContents(
        link: Link,
        directoryContents: List<String>
    ): Pair<MutableList<String>, Int> {
        var deletedFiles = 0
        val movedFiles: MutableList<String> = mutableListOf()
        for (filePath: String in directoryContents) {
            val fileLink: Link = if (link is RelativeLinkToDirectory) {
                link.convertToFileLink(filePath)
            } else {
                link as WebLinkToDirectory
                link.convertToFileLink(filePath)
            }
            try {
                val fileChange = getLocalFileChanges(fileLink) as CustomChange
                if (fileChange.customChangeType == CustomChangeType.MOVED) {
                    movedFiles.add(fileChange.afterPathString)
                }
                if (fileChange.customChangeType == CustomChangeType.DELETED) {
                    deletedFiles++
                }
            } catch (e: FileChangeGatheringException) {
                throw UnableToFetchLocalDirectoryChangesException("Error while fetching file changes: ${e.message}")
            } catch (e: VcsException) {
                throw UnableToFetchLocalDirectoryChangesException("Error while fetching file changes: ${e.message}")
            } catch (e: Exception) {
                throw UnableToFetchLocalDirectoryChangesException("Error while fetching file changes: ${e.message}")
            }
        }
        return Pair(movedFiles, deletedFiles)
    }
}
