package org.intellij.plugin.tracker.services

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import git4idea.changes.GitChangeUtils
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import org.intellij.plugin.tracker.data.FileChange
import org.intellij.plugin.tracker.data.Link
import org.intellij.plugin.tracker.view.MDView
import java.io.File

class ChangeTrackerService(private val project: Project) {

    private val view: MDView = MDView()

    private fun processOutputLog(outputLog: String, link: Link): String {

        val outputLogLines = outputLog.split("\n")

        /**
         * Necessary regex for matching
         */
        val commitLineRegex = Regex("commit .*")
        val deletedLineRegex = Regex("^-.*")
        val addedLineRegex = Regex("^[+].*")
        val infoLineRegex = Regex("^@@.*@@$")

        val markDownSyntaxString = link.getMarkDownSyntaxString()
        var lastCommitSHA = ""
        var diffLine = false
        var lastLineNotContainingLinkString = true
        var commitFound = false

        for (line in outputLogLines) {
            when {
                commitLineRegex.matches(line) -> lastCommitSHA = line.replaceFirst("commit ", "")
                infoLineRegex.matches(line) -> diffLine = true
                deletedLineRegex.matches(line) && diffLine -> {
                    val trimmedLine = line.replaceFirst("-", "")
                    if (!trimmedLine.contains(markDownSyntaxString)) lastLineNotContainingLinkString = true
                }
                addedLineRegex.matches(line) && diffLine -> {
                    val trimmedLine = line.replaceFirst("+", "")
                    if (lastLineNotContainingLinkString && trimmedLine.contains(markDownSyntaxString)) {
                        commitFound = true
                    }
                    diffLine = false
                    lastLineNotContainingLinkString = false
                }
                else -> Unit
            }
            if (commitFound) break
        }
        return lastCommitSHA
    }

    private fun getStartCommit(link: Link): String {

        val repository = GitRepositoryManager.getInstance(project).repositories[0]
        val git = Git.getInstance()
        val gitLineHandler = GitLineHandler(project, repository.root, GitCommand.LOG)
        gitLineHandler.addParameters("-L${link.foundAtLineNumber},+1:${link.proveniencePath}", "--reverse")
        val outputLog = git.runCommand(gitLineHandler)
        return processOutputLog(outputLog.getOutputOrThrow(), link = link)
    }

    private fun extractSpecificFileChanges(link: Link, changeList: MutableCollection<Change>): FileChange {
        val fullPath = "${project.basePath}/${link.getPath()}"
        for (change in changeList) {
            if (change.affectsFile(File(fullPath))) return FileChange.changeToFileChange(project, change)
        }
        return FileChange()
    }

    private fun processFileChanges(
        linkList: MutableList<Link>,
        changes: MutableCollection<Pair<Link, FileChange>>,
        commitSHA: String,
        repository: GitRepository
    ) {
        val changeList = GitChangeUtils.getDiffWithWorkingTree(repository, commitSHA, true)
        if (changeList != null) {
            for (link in linkList) {
                val fileChange = extractSpecificFileChanges(
                    link = link,
                    changeList = changeList
                )
                changes.add(Pair(link, fileChange))
            }
        } else {
            for (link in linkList) {
                changes.add(Pair(link, FileChange()))
            }
        }
    }

    private fun processFileChangesNoCommitSHA(
        linkList: MutableList<Link>,
        changes: MutableCollection<Pair<Link, FileChange>>,
        repository: GitRepository
    ) {
        for (link in linkList) {
            try {
                val startCommit = getStartCommit(link)

                // TODO: Verify if at this commit the link points to something valid

                val changeList = GitChangeUtils.getDiffWithWorkingTree(repository, startCommit, true)

                if (changeList != null) {
                    val fileChange = extractSpecificFileChanges(
                        link = link,
                        changeList = changeList
                    )
                    changes.add(Pair(link, fileChange))
                } else {
                    changes.add(Pair(link, FileChange()))
                }
            } catch (e: VcsException) {
                val fileChangeBuilder = FileChange.Builder()
                fileChangeBuilder.errorMessage = "Could not retrieve new link for this link: ${e.message}"
                changes.add(Pair(link, fileChangeBuilder.build()))
            }
        }
    }

    fun getFileChanges(
        linkList: MutableList<Link>,
        commitSHA: String? = null
    ): MutableCollection<Pair<Link, FileChange>> {
        val repository = GitRepositoryManager.getInstance(project).repositories[0]
        val changes: MutableCollection<Pair<Link, FileChange>> = mutableListOf()

        if (commitSHA == null) {
            processFileChangesNoCommitSHA(
                linkList = linkList,
                changes = changes,
                repository = repository
            )
        } else {
            processFileChanges(
                linkList = linkList,
                changes = changes,
                repository = repository,
                commitSHA = commitSHA
            )
        }
        return changes
    }

    /**
     * Update the view.
     * @param project the currently open project
     * @param changes changes in the currently open MD file
     */
    fun updateView(project: Project?, fileChanges: MutableCollection<Pair<Link, FileChange>>) {
        val toolWindow =
            ToolWindowManager.getInstance(project!!).getToolWindow("Statistics")
        view.updateModel(fileChanges)
        toolWindow!!.hide(null)
    }

    companion object {
        fun getInstance(project: Project): ChangeTrackerService =
            ServiceManager.getService(project, ChangeTrackerService::class.java)
    }

    /**
     * Class constructor
     */
    init {
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow = toolWindowManager.registerToolWindow("Statistics", false, ToolWindowAnchor.BOTTOM)
        val contentFactory = ContentFactory.SERVICE.getInstance()
        val content = contentFactory.createContent(view, null, true)
        toolWindow.contentManager.addContent(content)
    }
}
