package org.intellij.plugin.tracker.services

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import org.intellij.plugin.tracker.core.LineTracker
import org.intellij.plugin.tracker.data.*
import org.intellij.plugin.tracker.data.changes.Change
import org.intellij.plugin.tracker.data.changes.CustomChange
import org.intellij.plugin.tracker.data.changes.CustomChangeType
import org.intellij.plugin.tracker.data.diff.DiffOutput
import org.intellij.plugin.tracker.data.diff.DiffOutputMultipleRevisions
import org.intellij.plugin.tracker.data.diff.FileHistory
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.data.links.RelativeLinkToDirectory
import org.intellij.plugin.tracker.data.links.WebLinkToDirectory
import org.intellij.plugin.tracker.settings.SimilarityThresholdSettings
import org.intellij.plugin.tracker.utils.*
import java.io.File

enum class ChangeTrackingPolicy { HISTORY, LOCAL }

/**
 * Implementation of ChangeTrackerService interface, adapted to work in IntelliJ IDEA environment
 */
class ChangeTrackerService(project: Project, changeTrackingPolicy: ChangeTrackingPolicy) {

    private val changeRetriever: ChangeRetriever = if (changeTrackingPolicy == ChangeTrackingPolicy.HISTORY) {
        HistoryChangeRetriever(project = project)
    } else {
        LocalChangeRetriever(project = project)
    }

    /**
     * Get the change for a link to a file that corresponds to the currently open project in the IDE
     *
     * First, it checks whether the file referenced is un-tracked - if so, return
     * Otherwise, it tries to fetch all the changes that have affected the file throughout git history
     *
     * If a new path is found, then try to see whether there is any working tree changes that would affect
     * this new path. If there are changes, then use this new change as the final change.
     * Otherwise, return the previous change.
     */
    override fun getLocalFileChanges(link: Link): Change = changeRetriever.getFileChange(link)


    /**
     * Get change that has affected a directory that is referenced by a link
     *
     * This method first tries to see whether the folder exists at the commit that is pointing to HEAD.
     * If so, return a change that this directory is not changed.
     *
     * Otherwise, try to get the start commit of the line containing this link in the project. If it cannot be found,
     * raise an appropriate exception.
     * Then, the method tries to fetch the directory contents at this start commit. After that, using these contents (files),
     * it will track each file individually throughout git history.
     * The method then checks whether the size of the directory contents at start commit is equal to the size of
     * the files found deleted + the files found to be moved. If so, it means that the directory has either been deleted/moved.
     *
     * In that case, if we can find a common sub-path in the moved files, and if the number of occurrences of this common sub-path
     * exceeds a given threshold value, we can say that the directory has been moved. Otherwise, it is deleted.
     */
    override fun getLocalDirectoryChanges(link: Link): Change = changeRetriever.getDirectoryChange(link)

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
    override fun getLocalLineChanges(
        link: Link,
        branchOrTagName: String?,
        specificCommit: String?
    ): Change {
        val startCommit: String = specificCommit
            ?: (gitOperationManager
                .getStartCommit(link, checkSurroundings = true, branchOrTagName = branchOrTagName)
                ?: throw CommitSHAIsNullLineException(fileChange = CustomChange(CustomChangeType.INVALID, "")))

        try {
            val fileChange: CustomChange = getLocalFileChanges(link, specificCommit = startCommit) as CustomChange
            val fileHistoryList: MutableList<FileHistory> = fileChange.fileHistoryList
            val diffOutputList: MutableList<DiffOutput> = mutableListOf()
            var originalLineContent = ""

            if (fileHistoryList.isNotEmpty()) {
                originalLineContent =
                    gitOperationManager.getContentsOfLineInFileAtCommit(startCommit, link.path, link.lineReferenced)
                // if the file change type is deleted, throw an exception.
                // There is no need to track the lines in this file.
                if (fileChange.customChangeType == CustomChangeType.DELETED) {
                    throw FileHasBeenDeletedException(fileChange = fileChange)
                }
                DiffOutput.getDiffOutputs(fileHistoryList, diffOutputList, gitOperationManager)
            }

            val diffOutputMultipleRevisions =
                DiffOutputMultipleRevisions(
                    fileChange,
                    diffOutputList,
                    originalLineContent = originalLineContent
                )
            return LineTracker.trackLine(link, diffOutputMultipleRevisions)
        } catch (e: FileChangeGatheringException) {
            throw InvalidFileChangeException(fileChange = CustomChange(CustomChangeType.INVALID, "", e.message))
        }
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
    override fun getLocalLinesChanges(
        link: Link,
        branchOrTagName: String?,
        specificCommit: String?
    ): Change {
        val startCommit: String = specificCommit
            ?: (gitOperationManager
                .getStartCommit(link, checkSurroundings = true, branchOrTagName = branchOrTagName)
                ?: throw CommitSHAIsNullLinesException(fileChange = CustomChange(CustomChangeType.INVALID, "")))

        try {
            val fileChange: CustomChange = getLocalFileChanges(link, specificCommit = startCommit) as CustomChange
            val fileHistoryList: MutableList<FileHistory> = fileChange.fileHistoryList
            val diffOutputList: MutableList<DiffOutput> = mutableListOf()
            val originalLineBlockContents: MutableList<String> = mutableListOf()

            if (fileHistoryList.isNotEmpty()) {
                originalLineBlockContents.addAll(
                    gitOperationManager.getContentsOfLinesInFileAtCommit(
                        fileHistoryList.first().revision,
                        link.path,
                        link.referencedStartingLine,
                        link.referencedEndingLine
                    )
                )
                // if the file change type is deleted, throw an exception.
                // There is no need to track the lines in this file.
                if (fileChange.customChangeType == CustomChangeType.DELETED) {
                    throw FileHasBeenDeletedLinesException(fileChange = fileChange)
                }
                DiffOutput.getDiffOutputs(fileHistoryList, diffOutputList, gitOperationManager)
            }

            val diffOutputMultipleRevisions =
                DiffOutputMultipleRevisions(
                    fileChange,
                    diffOutputList,
                    originalLinesContents = originalLineBlockContents
                )

            return LineTracker.trackLines(link, diffOutputMultipleRevisions)
        } catch (e: FileChangeGatheringException) {
            throw InvalidFileChangeException(fileChange = CustomChange(CustomChangeType.INVALID, "", e.message))
        }
    }

    companion object {
        /**
         * Used by IDEA to get a reference to the single instance of this class.
         */
        fun getInstance(project: Project): ChangeTrackerServiceImpl =
            ServiceManager.getService(
                project,
                ChangeTrackerServiceImpl::class.java
            )
    }
}
