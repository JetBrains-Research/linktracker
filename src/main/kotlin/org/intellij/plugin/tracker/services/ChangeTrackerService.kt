package org.intellij.plugin.tracker.services

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import org.intellij.plugin.tracker.data.changes.FileChange
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.utils.GitOperationManager
import java.io.File


class ChangeTrackerService(private val project: Project) {

    private val gitOperationManager = GitOperationManager(project = project)


    /**
     * Extract the link we are looking for from a list of changes
     */
    private fun extractSpecificFileChanges(link: Link, changeList: MutableCollection<Change>): FileChange {
        val fullPath = "${project.basePath}/${link.getPath()}"
        for (change in changeList) {
            if (change.affectsFile(File(fullPath))) return FileChange.changeToFileChange(project, change)
        }
        return FileChange()
    }


    /**
     * Main function for getting changes for a link to a file.
     */
    fun getFileChange(
        link: Link
    ): Pair<Link, FileChange> {
        val changeList = gitOperationManager.getDiffWithWorkingTree(link.commitSHA!!)
        return if (changeList != null) {
            val fileChange = extractSpecificFileChanges(
                link = link,
                changeList = changeList
            )
            Pair(link, fileChange)
        } else {
            Pair(link, FileChange())
        }
    }

    companion object {
        fun getInstance(project: Project): ChangeTrackerService =
            ServiceManager.getService(project, ChangeTrackerService::class.java)
    }
}
