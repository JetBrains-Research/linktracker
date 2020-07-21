package org.intellij.plugin.tracker.core.change

import com.intellij.openapi.project.Project
import org.intellij.plugin.tracker.core.line.LineTracker
import org.intellij.plugin.tracker.data.*
import org.intellij.plugin.tracker.data.changes.Change
import org.intellij.plugin.tracker.data.changes.CustomChange
import org.intellij.plugin.tracker.data.changes.CustomChangeType
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.utils.calculateDirectorySimilarityAndDetermineChange
import org.intellij.plugin.tracker.utils.checkCurrentDirectoryContents
import org.intellij.plugin.tracker.utils.getLinkStartCommit

/**
 * Implementation of ChangeTrackerService interface, adapted to work in IntelliJ IDEA environment
 */
class ChangeTrackerImpl(project: Project, policy: ChangeTrackingPolicy) : ChangeTracker {

    private val changeSource: ChangeSource = if (policy == ChangeTrackingPolicy.HISTORY) {
        GitOperationManager(project)
    } else {
        ChangeListOperationManager(project)
    }

    private val myProject = project
    private val myPolicy = policy

    override fun getLocalFileChanges(link: Link): Change = changeSource.getChangeForFile(link)

    override fun getLocalDirectoryChanges(link: Link): Change {
        val linkPath = link.path
        if (checkCurrentDirectoryContents(myProject, link.path)) {
            return CustomChange(CustomChangeType.ADDED, link.path)
        }
        return calculateDirectorySimilarityAndDetermineChange(linkPath, changeSource.getDirectoryInfo(link))
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
        val fileChange = getFileChangeForLink(link, CommitSHAIsNullLineException(), FileHasBeenDeletedException())
        val diffOutputList = changeSource.getDiffOutput(fileChange)
        val originalLineContent = changeSource.getOriginalLineContents(link)
        return LineTracker.trackLine(link, fileChange, originalLineContent, diffOutputList)
    }

    private fun getFileChangeForLink(
        link: Link,
        commitNotFoundException: Throwable,
        fileDeletedException: Throwable
    ): CustomChange {
        if (myPolicy == ChangeTrackingPolicy.HISTORY) {
            changeSource as GitOperationManager
            link.specificCommit = getLinkStartCommit(changeSource, link, commitNotFoundException)
        }
        val fileChange = getLocalFileChanges(link) as CustomChange
        if (fileChange.customChangeType == CustomChangeType.DELETED) throw fileDeletedException
        return fileChange
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
        val fileChange = getFileChangeForLink(link, CommitSHAIsNullLinesException(), FileHasBeenDeletedLinesException())
        val diffOutputList = changeSource.getDiffOutput(fileChange)
        val multipleOriginalLinesContents = changeSource.getMultipleOriginalLinesContents(link)
        return LineTracker.trackLines(link, diffOutputList, fileChange, multipleOriginalLinesContents)
    }
}
