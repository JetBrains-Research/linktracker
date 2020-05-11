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
import org.intellij.plugin.tracker.data.links.LinkInfo


/**
 * Class that handles the logic of git operations
 */
class GitOperationManager(private val project: Project) {

    private val git: Git = Git.getInstance()
    private val gitRepository: GitRepository = GitRepositoryManager.getInstance(project).repositories[0]


    /**
     * Auxiliary function that processes the outputs of a git log -L command
     *
     * Checks between the diffs of the line to see when the line containing the link was introduced
     * and returns the corresponding commit SHA.
     */
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


    /**
     * Method that retrieves the list of directories in a git repository, at a certain commit
     *
     * Runs git command 'git ls-tree -d -r --name-only COMMITSHA'
     */
    fun getListOfDirectories(
        commitSHA: String
    ): List<String> {
        val gitLineHandler = GitLineHandler(project, gitRepository.root, GitCommand.LS_TREE)
        gitLineHandler.addParameters("-d", "-r", "--name-only", commitSHA)
        val outputDirectories = git.runCommand(gitLineHandler)
        return outputDirectories.getOutputOrThrow().split("\n")
    }


    /**
     * Method that retrieves the list of files in a git repository, at a certain commit
     *
     * Runs git command 'git ls -r --name-only COMMITSHA'
     */
    fun getListOfFiles(
        commitSHA: String
    ): List<String> {
        val gitLineHandler = GitLineHandler(project, gitRepository.root, GitCommand.LS_TREE)
        gitLineHandler.addParameters("-r", "--name-only", commitSHA)
        val outputFiles = git.runCommand(gitLineHandler)
        return outputFiles.getOutputOrThrow().split("\n")
    }


    /**
     * Method that retrieves a list of changes between the project version at commit commitSHA
     * and the current working tree (includes also uncommitted files)
     */
    fun getDiffWithWorkingTree(commitSHA: String): MutableCollection<Change>? =
        GitChangeUtils.getDiffWithWorkingTree(gitRepository, commitSHA, true)


    /**
     * Get the commit at which a line containing the information in linkInfo was created
     *
     * Runs a git command of the form 'git -L32,+1:README.md', where README.md would be the project relative path
     * to the markdown file in which the link was found and 32 would be the line number at which that link was found
     */
    fun getStartCommit(linkInfo: LinkInfo): String {
        val gitLineHandler = GitLineHandler(project, gitRepository.root, GitCommand.LOG)
        gitLineHandler.addParameters("-L${linkInfo.foundAtLineNumber},+1:${linkInfo.proveniencePath}", "--reverse")
        val outputLog = git.runCommand(gitLineHandler)
        return processOutputLog(
            outputLog.getOutputOrThrow(),
            linkText = linkInfo.linkText,
            linkPath = linkInfo.linkPath
        )
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