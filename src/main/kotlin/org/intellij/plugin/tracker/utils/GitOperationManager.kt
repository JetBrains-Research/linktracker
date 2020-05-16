package org.intellij.plugin.tracker.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.changes.Change
import git4idea.changes.GitChangeUtils
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.history.GitHistoryUtils
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import org.intellij.plugin.tracker.data.changes.ChangeType
import org.intellij.plugin.tracker.data.changes.LinkChange
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.data.links.LinkInfo
import org.intellij.plugin.tracker.services.ChangeTrackerService


/**
 * Class that handles the logic of git operations
 */
class GitOperationManager(private val project: Project) {

    private val git: Git = Git.getInstance()
    private val gitRepository: GitRepository = GitRepositoryManager.getInstance(project).repositories[0]


    /**
     * Get the date of a commit in a timestamp format
     *
     */
    private fun getDateOfCommit(commitSHA: String): String {
        val gitLineHandler = GitLineHandler(project, gitRepository.root, GitCommand.SHOW)
        gitLineHandler.addParameters("-s", "--format=%ct", commitSHA)
        val timestampOutput = git.runCommand(gitLineHandler)
        return timestampOutput.getOutputOrThrow()
    }

    @Throws(VcsException::class)
    fun getHeadCommitSHA(): String {
        val gitLineHandler = GitLineHandler(project, gitRepository.root, GitCommand.REV_PARSE)
        gitLineHandler.addParameters("--short", "HEAD")
        val output = git.runCommand(gitLineHandler)
        return output.getOutputOrThrow()
    }

    /**
     * Get the commit at which a line containing the information in linkInfo was created
     *
     * Runs a git command of the form 'git -L32,+1:README.md', where README.md would be the project relative path
     * to the markdown file in which the link was found and 32 would be the line number at which that link was found
     */
    fun getStartCommit(linkInfo: LinkInfo): String? {
        val gitLineHandler = GitLineHandler(project, gitRepository.root, GitCommand.LOG)
        gitLineHandler.addParameters("--oneline", "-S${linkInfo.getMarkDownSyntaxString()}")
        val outputLog = git.runCommand(gitLineHandler)
        if (outputLog.exitCode == 0)
            // return most recent finding
            if (outputLog.output.size != 0) return outputLog.output[0].split(" ")[0]
        return null
    }

    private fun processWorkingTreeChanges(link: Link, changes: String): LinkChange? {
        val changeList: List<String> = changes.split("\n")
        val change: String? = changeList.find { line -> line.contains(link.linkInfo.linkPath) }

        if (change != null) {
            when {
                change.startsWith("??") -> return LinkChange(ChangeType.ADDED, link.linkInfo.linkPath)
                change.startsWith("RM") -> {
                    val lineSplit = change.split(" -> ")
                    println("LINE SPLIT: $lineSplit")
                    println("NEW PATH: ${lineSplit[1]}")
                    assert(lineSplit.size == 2)
                    return LinkChange(ChangeType.MOVED, lineSplit[1])
                }
                change.startsWith("D") -> return LinkChange(ChangeType.DELETED, link.linkInfo.linkPath)
                change.startsWith("M") -> return LinkChange(ChangeType.MODIFIED, link.linkInfo.linkPath)
            }
        }
        return null
    }

    @Throws(VcsException::class)
    fun checkWorkingTreeChanges(link: Link): LinkChange? {
        val gitLineHandler = GitLineHandler(project, gitRepository.root, GitCommand.STATUS)
        gitLineHandler.addParameters("--porcelain")
        val outputLog = git.runCommand(gitLineHandler)
        return processWorkingTreeChanges(link, outputLog.getOutputOrThrow())
    }

    @Throws(VcsException::class)
    fun getAllChangesForFile(link: Link): LinkChange {
        val gitLineHandler = GitLineHandler(project, gitRepository.root, GitCommand.LOG)
        gitLineHandler.addParameters(
            "--name-status",
            "--oneline",
            "--follow",
            "-p",
            "--reverse",
            "*${link.getReferencedFileName()}"
        )

        val outputLog = git.runCommand(gitLineHandler)
        println(processChangesForFile(link, outputLog.getOutputOrThrow()))
        return processChangesForFile(link, outputLog.getOutputOrThrow())
    }

    private fun processChangesForFile(link: Link, changes: String): LinkChange {
        if (changes.isNotEmpty()) {
            val changeList = changes.split("\n")
            println("CHANGE LIST IS: $changeList")
            val index: Int = changeList.indexOfFirst { line -> line.contains(link.linkInfo.linkPath) }
            if (index != -1) {
                val lastChange: String = changeList.last()
                when {
                    lastChange.startsWith("A") -> return LinkChange(ChangeType.ADDED, link.linkInfo.linkPath)
                    lastChange.startsWith("M") -> return LinkChange(ChangeType.MODIFIED, link.linkInfo.linkPath)
                    lastChange.startsWith("D") -> return LinkChange(ChangeType.DELETED, link.linkInfo.linkPath)
                    lastChange.startsWith("R") -> {
                        val lineSplit = lastChange.trim().split("\\s+".toPattern())
                        assert(lineSplit.size == 3)
                        return LinkChange(ChangeType.MOVED, lineSplit[2])
                    }
                }
            }
            return LinkChange(
                ChangeType.INVALID,
                link.linkInfo.linkPath,
                errorMessage = "File existed, but the path ${link.linkInfo.linkPath} to this file never existed in Git history.")
        }
        return LinkChange(
            ChangeType.INVALID,
            link.linkInfo.linkPath,
            errorMessage = "Referenced file ${link.getReferencedFileName()} never existed in Git history.")
    }


    /**
     * Get the remote origin url of a git repository
     *
     * Runs git command 'git config --get remote.origin.url'
     */
    fun getRemoteOriginUrl(): String {
        val gitLineHandler = GitLineHandler(project, gitRepository.root, GitCommand.CONFIG)
        gitLineHandler.addParameters("--get", "remote.origin.url")
        val outputLog = git.runCommand(gitLineHandler)
        return outputLog.getOutputOrThrow()
    }
}