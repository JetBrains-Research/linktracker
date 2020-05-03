package org.intellij.plugin.tracker.services

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import git4idea.changes.GitChangeUtils
import git4idea.repo.GitRepositoryManager
import org.intellij.plugin.tracker.data.FileChange
import org.intellij.plugin.tracker.data.Link
import java.io.File

class ChangeTrackerService(private val project: Project) {

    private fun getStartCommit(link: Link): String = throw NotImplementedError("")

    private fun changeToFileChange(change: Change): FileChange? {
        val fileChangeBuilder = FileChange.Builder()
        fileChangeBuilder.changeType = change.type.toString()

        if (change.type.toString() == "MOVED") {
            fileChangeBuilder.moveRelativePath = change.getMoveRelativePath(project)
        }

        if (change.beforeRevision?.file?.path != null) {
            fileChangeBuilder.beforePath = change.beforeRevision?.file?.path
        }

        if (change.afterRevision?.file?.path != null) {
            fileChangeBuilder.afterPath = change.afterRevision?.file?.path
        }

        if (change.afterRevision?.file?.name != null) {
            fileChangeBuilder.fileName = change.afterRevision?.file?.name.toString()
        }
        return fileChangeBuilder.build()
    }

    private fun extractSpecificFileChanges(link: Link, changeList: MutableCollection<Change>): FileChange? {
        for (change in changeList) {
            if (change.affectsFile(File(link.linkPath))) return changeToFileChange(change)
        }
        return null
    }


    fun getFileChanges(linkList: MutableList<Link>, commitSHA: String? = null): MutableCollection<FileChange> {
        val repository = GitRepositoryManager.getInstance(project).repositories[0]
        val changes: MutableCollection<FileChange> = mutableListOf()

        if (commitSHA == null) {

            for (link in linkList) {
                // TODO: Add functionality for retrieving the commit SHA at which the line containing the link was added
                val startCommit = getStartCommit(link)
                val changeList = GitChangeUtils.getDiffWithWorkingTree(repository, startCommit, true)
                if (changeList != null) {
                    val fileChange = extractSpecificFileChanges(link, changeList = changeList)
                    if (fileChange != null) {
                        changes.add(fileChange)
                    } else {
                        // TODO: File was not found

                    }
                } else {
                    // TODO: Change list is null
                }
            }

        } else {
            // TODO: Get only the changes of the files which are referenced by links
            val changeList = GitChangeUtils.getDiffWithWorkingTree(repository, commitSHA, true)
            if (changeList != null) {
                for (link in linkList) {
                    val fileChange = extractSpecificFileChanges(link, changeList = changeList)
                    if (fileChange != null) {
                        changes.add(fileChange)
                    } else {
                        // TODO: File was not found
                    }
                }
            } else {
                // TODO: Change list is null
            }
        }
        return changes
    }

    companion object {
        fun getInstance(project: Project): ChangeTrackerService = ServiceManager.getService(project, ChangeTrackerService(project).javaClass)
    }
}