package org.intellij.plugin.tracker.core.change

import com.intellij.openapi.project.Project
import org.intellij.plugin.tracker.core.line.LineTracker
import org.intellij.plugin.tracker.data.*
import org.intellij.plugin.tracker.data.changes.*
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.utils.checkCurrentDirectoryContents
import org.intellij.plugin.tracker.utils.getLinkStartCommit

/**
 * Implementation of ChangeTrackerService interface, adapted to work in IntelliJ IDEA environment
 */
class ChangeTrackerImpl(private val project: Project, private val policy: ChangeTrackingPolicy) : ChangeTracker {

    private val changeSource: ChangeSource = if (policy == ChangeTrackingPolicy.HISTORY) {
        GitOperationManager(project)
    } else {
        ChangeListOperationManager(project)
    }

    override fun getLocalFileChanges(link: Link): Change = changeSource.getChangeForFile(link)

    override fun getLocalDirectoryChanges(link: Link): Change {
        if (checkCurrentDirectoryContents(project, link.path)) {
            return CustomChange(CustomChangeType.ADDED, link.path)
        }
        return changeSource.getChangeForDirectory(link)
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
        val fileChange = getFileChangeForLink(link, CommitSHAIsNullLineException())
        if (fileChange.customChangeType != CustomChangeType.DELETED) {
            val diffOutputList = changeSource.getDiffOutput(fileChange)
            val originalLineContent = changeSource.getOriginalLineContents(link)
            return LineTracker.trackLine(link, fileChange, originalLineContent, diffOutputList)
        }
        return LineChange(fileChange, LineChangeType.DELETED)
    }

    private fun getFileChangeForLink(link: Link, commitNotFoundException: Throwable): CustomChange {
        if (policy == ChangeTrackingPolicy.HISTORY) {
            link.specificCommit = getLinkStartCommit(changeSource as GitOperationManager, link, commitNotFoundException)
        }
        return getLocalFileChanges(link) as CustomChange
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
        val fileChange = getFileChangeForLink(link, CommitSHAIsNullLinesException())
        if (fileChange.customChangeType != CustomChangeType.DELETED) {
            val diffOutputList = changeSource.getDiffOutput(fileChange)
            val multipleOriginalLinesContents = changeSource.getMultipleOriginalLinesContents(link)
            return LineTracker.trackLines(link, diffOutputList, fileChange, multipleOriginalLinesContents)
        }
        return LinesChange(fileChange, LinesChangeType.DELETED)
    }
}
