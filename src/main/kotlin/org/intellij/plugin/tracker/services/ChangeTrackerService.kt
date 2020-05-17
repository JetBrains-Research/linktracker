package org.intellij.plugin.tracker.services

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import org.intellij.plugin.tracker.utils.GitOperationManager
import org.intellij.plugin.tracker.data.changes.ChangeType
import org.intellij.plugin.tracker.data.changes.DirectoryChange
import org.intellij.plugin.tracker.data.changes.LinkChange
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.data.links.LinkInfo
import org.intellij.plugin.tracker.data.links.RelativeLinkToFile
import org.intellij.plugin.tracker.data.links.WebLinkToFile


class ChangeTrackerService(project: Project) {

    private val gitOperationManager = GitOperationManager(project = project)

    /**
     * Main function for getting changes for a link to a file.
     */
    fun getFileChange(link: Link): Pair<Link, LinkChange> {
        val workingTreeChange: LinkChange? = gitOperationManager.checkWorkingTreeChanges(link)

        // this file has just been added and is not tracked by git, but the link is considered valid
        if (workingTreeChange != null && workingTreeChange.changeType == ChangeType.ADDED) {
            return Pair(link, workingTreeChange)
        }

        val change: LinkChange = gitOperationManager.getAllChangesForFile(link)
        when (change.changeType) {
            // this file's change type is invalid, return straight-away
            ChangeType.INVALID -> return Pair(link, change)
            else -> {
                // so far we have only checked `git log` with the commit that is pointing to HEAD.
                // but we want to also check non-committed changes for file changes.
                // at this point, link was found and a new change has been correctly identified.

                // working tree change can be null (might be because we have first calculated the working tree change
                // using the unchanged path that was retrieved from the markdown file -- this path might have been invalid
                // but now we have a new path that corresponds to the original retrieved path
                // we want to check whether there is any non-committed change that affects this new path
                if (workingTreeChange == null) {
                    var newLink: Link? = null
                    // only these 2 link types get this far (LinkProcessingRouter handles this logic)
                    when (link) {
                        is RelativeLinkToFile -> {
                            val linkInfoCopy: LinkInfo = link.linkInfo.copy(linkPath = change.afterPath)
                            newLink = link.copy(linkInfo = linkInfoCopy)
                        }
                        is WebLinkToFile -> {
                            val linkInfoCopy: LinkInfo = link.linkInfo.copy(linkPath = change.afterPath)
                            newLink = link.copy(linkInfo = linkInfoCopy)
                        }
                    }

                    // safe !!
                    newLink!!.linkInfo.linkPath = change.afterPath
                    val currentChange: LinkChange = gitOperationManager.checkWorkingTreeChanges(newLink) ?: return Pair(link, change)
                    // new change identified (from checking working tree). Use this newly-found change instead.
                    return Pair(link, currentChange)
                }

                // if the working tree change change type is either deleted or moved
                // (calculated using the unchanged path retrieved from the markdown files),
                // use this change instead of the one found from `git log` command (it overrides it).
                // Otherwise, return the change found from `git log` command.
                return when (workingTreeChange.changeType) {
                    ChangeType.DELETED, ChangeType.MOVED -> Pair(link, workingTreeChange)
                    else -> Pair(link, change)
                }
            }
        }
    }

    /**
     * Extract the directory we are looking for from a list of changes
     */
    private fun extractSpecificDirectoryChanges(changeList: MutableCollection<Change>): DirectoryChange {
        for (change in changeList) {
            val prevPath = change.beforeRevision?.file?.parentPath
            val currPath = change.afterRevision?.file?.parentPath
            if (prevPath != currPath) return DirectoryChange.changeToDirectoryChange(change)
        }
        return DirectoryChange(ChangeType.ADDED, "")
    }

    /**
     * Main function for getting changes for a directory.
     */
    fun getDirectoryChange(link: Link): Pair<Link, DirectoryChange> {
        val changeList = gitOperationManager.getDiffWithWorkingTree(link.commitSHA!!)
        return if (changeList != null) {
            val directoryChange = extractSpecificDirectoryChanges(changeList = changeList)
            Pair(link, directoryChange)
        } else {
            Pair(link, DirectoryChange(ChangeType.ADDED, ""))
        }
    }

    companion object {
        fun getInstance(project: Project): ChangeTrackerService =
            ServiceManager.getService(project, ChangeTrackerService::class.java)
    }
}
