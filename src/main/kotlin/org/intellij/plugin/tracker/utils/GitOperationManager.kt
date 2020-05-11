package org.intellij.plugin.tracker.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import git4idea.changes.GitChangeUtils
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import org.intellij.plugin.tracker.data.links.Link
import java.text.DecimalFormat

class GitOperationManager(private val project: Project) {

    private val git: Git = Git.getInstance()
    private val gitRepository: GitRepository = GitRepositoryManager.getInstance(project).repositories[0]

    private fun processOutputLog(
        outputLog: String,
        link: Link? = null,
        linkText: String? = null,
        linkPath: String? = null
    ): String {
        val outputLogLines = outputLog.split("\n")

        /**
         * Necessary regex for matching
         */
        val commitLineRegex = Regex("commit .*")
        val deletedLineRegex = Regex("^-.*")
        val addedLineRegex = Regex("^[+].*")
        val infoLineRegex = Regex("^@@.*@@$")
        val markDownSyntaxString: String?
        markDownSyntaxString = link?.getMarkDownSyntaxString() ?: "[$linkText]($linkPath)"
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

    /**
     * Checks whether the link, when it was created, was referencing something that truly existed
     */
    fun checkValidityOfLinkPathAtCommit(
        commitSHA: String,
        linkPath: String
    ): Boolean {
        val gitLineHandler = GitLineHandler(project, gitRepository.root, GitCommand.REV_PARSE)
        gitLineHandler.addParameters(commitSHA, "--is-inside-work-tree", "-- $linkPath")
        val outputRevParse = git.runCommand(gitLineHandler)
        if (outputRevParse.exitCode == 0) return true
        return false
    }

    fun getListOfDirectories(
        commitSHA: String
    ): List<String> {
        val gitLineHandler = GitLineHandler(project, gitRepository.root, GitCommand.LS_TREE)
        gitLineHandler.addParameters("-d", "-r", "--name-only", commitSHA)
        val outputDirectories = git.runCommand(gitLineHandler)
        return outputDirectories.getOutputOrThrow().split("\n")
    }

    fun getListOfFiles(
        commitSHA: String
    ): List<String> {
        val gitLineHandler = GitLineHandler(project, gitRepository.root, GitCommand.LS_TREE)
        gitLineHandler.addParameters("-r", "--name-only", commitSHA)
        val outputFiles = git.runCommand(gitLineHandler)
        return outputFiles.getOutputOrThrow().split("\n")
    }

    fun getDiffWithWorkingTree(commitSHA: String): MutableCollection<Change>? {
        //val start = System.currentTimeMillis()
        val ret = GitChangeUtils.getDiffWithWorkingTree(gitRepository, commitSHA, true)
        //val formatter = DecimalFormat("#0.00000")
        //var end = System.currentTimeMillis()
        //println("diff with working tree execution time is " + formatter.format((end - start) / 1000.0) + " seconds")
        return ret
    }


    fun getStartCommit(lineNumber: Int, proveniencePath: String, linkText: String, linkPath: String): String {
        //val start = System.currentTimeMillis()
        val gitLineHandler = GitLineHandler(project, gitRepository.root, GitCommand.LOG)
        gitLineHandler.addParameters("-L$lineNumber,+1:$proveniencePath", "--reverse")
        val outputLog = git.runCommand(gitLineHandler)
        val ret_val = processOutputLog(
            outputLog.getOutputOrThrow(),
            linkText = linkText,
            linkPath = linkPath
        )
        //val formatter = DecimalFormat("#0.00000")
        // var end = System.currentTimeMillis()
        //println("start commit Execution time is " + formatter.format((end - start) / 1000.0) + " seconds")

        return ret_val
    }

    fun getStartCommit(link: Link): String {
        val gitLineHandler = GitLineHandler(project, gitRepository.root, GitCommand.LOG)
        gitLineHandler.addParameters("-L${link.foundAtLineNumber},+1:${link.proveniencePath}", "--reverse")
        val outputLog = git.runCommand(gitLineHandler)
        return processOutputLog(outputLog.getOutputOrThrow(), link = link)
    }

    fun getRemoteOriginUrl(): String {
        //val start = System.currentTimeMillis()
        val gitLineHandler = GitLineHandler(project, gitRepository.root, GitCommand.CONFIG)
        gitLineHandler.addParameters("--get", "remote.origin.url")
        val outputLog = git.runCommand(gitLineHandler)

        //val formatter = DecimalFormat("#0.00000")
        //var end = System.currentTimeMillis()
        //println("get remote origin execution time is " + formatter.format((end - start) / 1000.0) + " seconds")
        return outputLog.getOutputOrThrow()
    }
}