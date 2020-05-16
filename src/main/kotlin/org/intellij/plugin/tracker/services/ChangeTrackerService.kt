package org.intellij.plugin.tracker.services

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import org.intellij.plugin.tracker.data.changes.ChangeType
import org.intellij.plugin.tracker.data.changes.LinkChange
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.utils.GitOperationManager
import java.io.File


class ChangeTrackerService(private val project: Project) {

    private val gitOperationManager = GitOperationManager(project = project)
    var lastCommitRunOn: String? = null


    /**
     * Extract the link we are looking for from a list of changes
     */


    /**
     * Main function for getting changes for a link to a file.
     */
    fun getFileChange(
            link: Link
    ): Pair<Link, LinkChange> {
        val workingTreeChange: LinkChange? = gitOperationManager.checkWorkingTreeChanges(link)

        // this file has just been added and is not tracked by git, but the link is valid
        if (workingTreeChange != null && workingTreeChange.changeType.toString() == "ADDED") {
            return Pair(link, workingTreeChange)
        }

        val change = gitOperationManager.getAllChangesForFile(link)

        println("CHANGE IS: $change")

        when (change.changeType) {
            ChangeType.INVALID -> return Pair(link, change)
            else -> {
                // so far we have only checked `git log` with the commit that is pointing to HEAD.
                // but we want to also check non-committed changes for file changes.

                if (workingTreeChange == null) return Pair(link, change)

                return when (workingTreeChange.changeType) {
                    ChangeType.DELETED, ChangeType.MOVED -> Pair(link, workingTreeChange)
                    else -> Pair(link, change)
                }
            }
        }
    }

    companion object {
        fun getInstance(project: Project): ChangeTrackerService =
                ServiceManager.getService(project, ChangeTrackerService::class.java)
    }
}
