package org.intellij.plugin.tracker.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import git4idea.changes.GitChangeUtils
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.history.GitHistoryUtils
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

    private val cachedFilesAtCommits: HashMap<String, MutableList<String>> = hashMapOf()
    private val cachedDirectoriesAtCommits: HashMap<String, MutableList<String>> = hashMapOf()


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
    ): String? {
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
        return if (commitFound) {
            lastCommitSHA
        } else {
            null
        }
    }

    /**
     * Checks whether the link, when it was created, was referencing something that truly existed
     */
    fun checkValidityOfLinkPathAtCommit(
        commitSHA: String,
        linkPath: String
    ): Boolean {
        val gitLineHandler = GitLineHandler(project, gitRepository.root, GitCommand.CAT_FILE)
        gitLineHandler.addParameters("-e", "$commitSHA:$linkPath")
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
     * Method that retrieves the commit at which a link path representing a directory appears (starting at a commit
     * and following along all commits within a 30 minute interval of the first commit)
     *
     * Runs git command 'git ls-tree -d -r --name-only COMMITSHA' on the list of commits, which is a list of commits
     * between a start commit and the commits that follow 30 minutes after it
     *
     *
     * Returns `null` if no match found.
     */
    fun getCommitForDirectories(
        linkPath: String,
        commitSHA: String
    ): String? {
        val timestampSince: String = getDateOfCommit(commitSHA)
        // get all commits within 30 minutes of the `start` commit
        val timestampUntil = (timestampSince.toLong() +   30 * 60).toString()

        val listOfCommitsBetweenTimestamps =
            GitHistoryUtils.collectTimedCommits(project, gitRepository.root, "--since", timestampSince, "--until", timestampUntil)
        for (commit in listOfCommitsBetweenTimestamps) {
            val gitLineHandler = GitLineHandler(project, gitRepository.root, GitCommand.LS_TREE)
            val commitId = commit.id.toString()

            // check if directories are cached for this commit id
            if (cachedDirectoriesAtCommits.containsKey(commitId)) {
                if (cachedDirectoriesAtCommits[commitId]!!.contains(linkPath)) return commitId

                // skip over running git commands
                else continue
            }

            gitLineHandler.addParameters("-d", "-r", "--name-only", commitId)
            val outputDirectories = git.runCommand(gitLineHandler)

            val directoryList = outputDirectories.getOutputOrThrow().split("\n")
            cachedDirectoriesAtCommits[commitId] = mutableListOf()
            cachedDirectoriesAtCommits[commitId]!!.addAll(directoryList)

            if (directoryList.contains(linkPath)) return commitId
        }

        return null
    }

    /**
     * Get the date of a commit in a timestamp format
     *
     */
    fun getDateOfCommit(commitSHA: String): String {
        val gitLineHandler = GitLineHandler(project, gitRepository.root, GitCommand.SHOW)
        gitLineHandler.addParameters("-s", "--format=%ct", commitSHA)
        val timestampOutput = git.runCommand(gitLineHandler)
        return timestampOutput.getOutputOrThrow()
    }


    /**
     * Method that retrieves the commit at which a link path representing a file appears (starting at a commit
     * and following along all commits within a 30 minute interval of the first commit)
     *
     * Runs git command 'git ls -r --name-only COMMITSHA' on the list of commits, which is a list of commits
     * between a start commit and the commits that follow 30 minutes after it
     *
     * Returns `null` if no match found.
     */
    fun getCommitForFiles(
        linkPath: String,
        commitSHA: String
    ): String? {
        val timestampSince: String = getDateOfCommit(commitSHA)
        // get all commits within 30 minutes of the `start` commit
        val timestampUntil = (timestampSince.toLong() +   30 * 60).toString()

        val listOfCommitsBetweenTimestamps =
            GitHistoryUtils.collectTimedCommits(project, gitRepository.root, "--since", timestampSince, "--until", timestampUntil)
        for (commit in listOfCommitsBetweenTimestamps) {
            val gitLineHandler = GitLineHandler(project, gitRepository.root, GitCommand.LS_TREE)
            val commitId = commit.id.toString()

            // check if directories are cached for this commit id
            if (cachedFilesAtCommits.containsKey(commitId)) {
                if (cachedFilesAtCommits[commitId]!!.contains(linkPath)) return commitId

                // skip over running git commands
                else continue
            }

            gitLineHandler.addParameters("-r", "--name-only", commitId)
            val outputFiles = git.runCommand(gitLineHandler)

            val fileList = outputFiles.getOutputOrThrow().split("\n")
            cachedFilesAtCommits[commitId] = mutableListOf()
            cachedFilesAtCommits[commitId]!!.addAll(fileList)

            if (fileList.contains(linkPath)) return commitId
        }

        return null
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
    fun getStartCommit(linkInfo: LinkInfo): String? {
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