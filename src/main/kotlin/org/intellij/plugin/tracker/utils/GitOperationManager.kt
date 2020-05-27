package org.intellij.plugin.tracker.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import git4idea.changes.GitChangeUtils
import git4idea.commands.*
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import org.intellij.plugin.tracker.data.changes.ChangeType
import org.intellij.plugin.tracker.data.changes.LinkChange
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.data.links.LinkInfo
import kotlin.math.min


/**
 * Class that handles the logic of git operations
 */
class GitOperationManager(private val project: Project) {

    private val git: Git = Git.getInstance()
    private val gitRepository: GitRepository = GitRepositoryManager.getInstance(project).repositories[0]


    /**
     * Checks whether a reference name corresponds to a valid tag name
     *
     */
    fun isRefATag(ref: String): Boolean {
        val gitLineHandler = GitLineHandler(project, gitRepository.root, GitCommand.TAG)
        // list all tags for this project
        gitLineHandler.addParameters("-l")
        val output: GitCommandResult = git.runCommand(gitLineHandler)
        val outputString: String = output.getOutputOrThrow()
        var tagList: List<String> = outputString.split("\n")
        tagList = tagList.map { line -> line.trim() }
        return ref in tagList
    }

    /**
     * Checks whether a reference name corresponds to a valid branch name
     *
     */
    @Throws(VcsException::class)
    fun isRefABranch(ref: String): Boolean {
        val gitLineHandler = GitLineHandler(project, gitRepository.root, GitCommand.BRANCH)
        // list all branch names for this project
        gitLineHandler.addParameters("-l")
        val gitCommandResult: GitCommandResult = git.runCommand(gitLineHandler)
        val outputString: String = gitCommandResult.getOutputOrThrow()
        var branchList: List<String> = outputString.split("\n")
        branchList = branchList.map { line -> line.trim() }
        branchList = branchList.map { line -> line.replace("* ", "") }
        return ref in branchList
    }

    /**
     * Checks whether a reference name corresponds to a valid commit SHA
     */
    fun isRefACommit(ref: String): Boolean {
        val gitLineHandler = GitLineHandler(project, gitRepository.root, GitCommand.REV_PARSE)
        gitLineHandler.addParameters(
            "--verify",
            "-q",
            "$ref^{commit}"
        )
        val gitCommandResult: GitCommandResult = git.runCommand(gitLineHandler)
        if (gitCommandResult.exitCode == 0) return true
        return false
    }


    /**
     * Get the date of a commit in a timestamp format
     *
     * Runs git command `git show -s --format=%ct <commitSHA>`
     *
     */
    @Throws(VcsException::class)
    private fun getDateOfCommit(commitSHA: String): String {
        val gitLineHandler = GitLineHandler(project, gitRepository.root, GitCommand.SHOW)
        gitLineHandler.addParameters("-s", "--format=%ct", commitSHA)
        val timestampOutput: GitCommandResult = git.runCommand(gitLineHandler)
        return timestampOutput.getOutputOrThrow()
    }

    /**
     * Get the commit SHA which points to the current HEAD
     *
     * Runs git command `git rev-parse --short HEAD`
     */
    @Throws(VcsException::class)
    fun getHeadCommitSHA(): String {
        val gitLineHandler = GitLineHandler(project, gitRepository.root, GitCommand.REV_PARSE)
        gitLineHandler.addParameters("HEAD")
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

    /**
     * Auxiliary function that processes the output of `git status --porcelain=v1`
     *
     * Splits the output into lines and checks the first letter of each line - based on this first letter
     * we can determine the type of change that affected a file
     *
     * We are checking whether there exists a line in the output which contains \
     * the link path that we are looking for.
     */
    private fun processWorkingTreeChanges(linkPath: String, changes: String): LinkChange? {
        val changeList: List<String> = changes.split("\n")
        changeList.forEach { line -> line.trim() }

        val change: String? = changeList.find { line -> line.contains(linkPath) }
        if (change != null) {
            when {
                change.startsWith("?") -> return LinkChange(ChangeType.ADDED, linkPath, fromWorkingTree = true)
                change.startsWith("!") -> return LinkChange(ChangeType.ADDED, linkPath, fromWorkingTree = true)
                change.startsWith("C") -> return LinkChange(ChangeType.ADDED, linkPath, fromWorkingTree = true)
                change.startsWith("A") -> return LinkChange(ChangeType.ADDED, linkPath, fromWorkingTree = true)
                change.startsWith("U") -> return LinkChange(ChangeType.ADDED, linkPath, fromWorkingTree = true)
                change.startsWith("R") -> {
                    val lineSplit = change.split(" -> ")
                    assert(lineSplit.size == 2)
                    return LinkChange(ChangeType.MOVED, lineSplit[1], fromWorkingTree = true)
                }
                change.startsWith("D") -> return LinkChange(ChangeType.DELETED, linkPath, fromWorkingTree = true)
                change.startsWith("M") -> return LinkChange(ChangeType.MODIFIED, linkPath, fromWorkingTree = true)
            }
        }
        return null
    }

    /**
     * Gets the commit SHA of the first commit in the currently checked out branch
     *
     * Runs git command `git rev-list --max-parents=0 HEAD`
     */
    @Throws(VcsException::class)
    fun getFirstCommitSHA(): String {
        val gitLineHandler = GitLineHandler(project, gitRepository.root, GitCommand.REV_LIST)
        gitLineHandler.addParameters("--max-parents=0")
        gitLineHandler.addParameters("HEAD")
        val output: GitCommandResult = git.runCommand(gitLineHandler)
        if (output.exitCode == 0) return output.outputAsJoinedString
        throw VcsException("Could not find first commit SHA of the currently checked-out branch")
    }

    /**
     * Get all working tree changes by calling git command `git status --porcelain=v1`
     *
     * The --porcelain=v1 parameter ensures that the output of git status will be static, despite
     * updates between git versions
     *
     * Hands the output to be processed by processWorkingTreeChanges()
     */
    @Throws(VcsException::class)
    fun checkWorkingTreeChanges(link: Link): LinkChange? {
        val gitLineHandler = GitLineHandler(project, gitRepository.root, GitCommand.STATUS)
        gitLineHandler.addParameters("--porcelain=v1")
        val outputLog: GitCommandResult = git.runCommand(gitLineHandler)
        return processWorkingTreeChanges(link.getPath(), outputLog.getOutputOrThrow())
    }

    /**
     * Get all working tree changes by calling git command
     * `git log --name-status --oneline --find-renames=<sim_threshold> --reverse <*file_name>`
     *
     * This method gets all of the changes that affected <filename> throughout git history
     *
     * Hands the output to be processed by processChangesForFile()
     *
     * Optional parameters:
     * @param similarityThreshold: if the contents of a file between two changes have a calculated similarity above
     * similarityThreshold, those two changes will be identified as a move/rename; otherwise, the changes are identified as
     * a deletion and an addition respectively.
     * @param branchOrTagName: specific branch or tag name on which to execute the git command.
     * @param specificCommit: specific commit SHA which to use as a starting point for the git command.
     */
    @Throws(VcsException::class)
    fun getAllChangesForFile(
        link: Link,
        similarityThreshold: Int,
        branchOrTagName: String? = null,
        specificCommit: String? = null
    ): Pair<MutableList<Pair<String, String>>, LinkChange> {
        val gitLineHandler = GitLineHandler(project, gitRepository.root, GitCommand.LOG)
        // add a specific branch or tag on which to execute the `git log` command
        // this branch/tag name exists (it has been previously checked in the LinkProcessingRouter
        if (branchOrTagName != null) gitLineHandler.addParameters(branchOrTagName)
        // if a specific commit is given, use that commit as a starting point for the log and compare it
        // to the HEAD commit
        // check also that the commit SHA is different than the first commit SHA that corresponds to the currently
        // checked-out branch; if they are the same, use a simple `git log` command without specifying commit range
        if (specificCommit != null && !getFirstCommitSHA().startsWith(specificCommit)) {
            gitLineHandler.addParameters("$specificCommit^..HEAD")
        }
        // misuse of the method: can not specify both branch/tag name and commit
        if (branchOrTagName != null && specificCommit != null) throw VcsException("Can not specify both branch/tag name and commit")
        gitLineHandler.addParameters(
            "--name-status",
            "--oneline",
            "--find-renames=$similarityThreshold",
            "--reverse",
            "*${link.getReferencedFileName()}"
        )

        val outputLog: GitCommandResult = git.runCommand(gitLineHandler)
        return processChangesForFile(link.getPath(), outputLog.getOutputOrThrow())
    }

    /**
     * Based on a line that is being retrieved from a git command, convert this line into a ChangeType object.
     *
     * All lines retrieved from git will be of the type: <CHANGE_TYPE> <PATH> <PATH>(optional)
     *
     * Where CHANGE_TYPE can be R -> Renamed, A -> Added, M -> Modified etc.
     * First PATH is the before-path for the renamed change type and the non-changed path for other change types.
     * Second PATH is optional and corresponds to the after path for renamed change types.
     */
    private fun extractChangeType(linkPath: String, line: String): LinkChange {
        when {
            line.startsWith("A") -> {
                val lineSplit: List<String> = line.trim().split("\\s+".toPattern())
                assert(lineSplit.size == 2)
                if (lineSplit[1] != linkPath) return LinkChange(ChangeType.MOVED, lineSplit[1])

                return LinkChange(ChangeType.ADDED, lineSplit[1])
            }
            line.startsWith("M") -> {
                val lineSplit: List<String> = line.trim().split("\\s+".toPattern())
                assert(lineSplit.size == 2)
                if (lineSplit[1] != linkPath) return LinkChange(ChangeType.MOVED, lineSplit[1])

                return LinkChange(ChangeType.MODIFIED, lineSplit[1])
            }
            line.startsWith("D") -> return LinkChange(ChangeType.DELETED, linkPath)
            line.startsWith("R") -> {
                val lineSplit: List<String> = line.trim().split("\\s+".toPattern())
                assert(lineSplit.size == 3)
                return LinkChange(ChangeType.MOVED, lineSplit[2])
            }
            else -> {
                return LinkChange(
                    ChangeType.INVALID,
                    linkPath,
                    errorMessage = "Could not track this link."
                )
            }
        }
    }

    /**
     * Auxiliary function to parse the content for the traversal that takes place
     * at method processChangesForFiles()
     *
     * If a line starts with R, it will have a before path and an after path. This method
     * retrieves the after path
     * Otherwise, a line will only have 1 non-changed path. We want to retrieve this path in this case.
     */
    private fun parseContent(content: String): String {
        return when {
            content.startsWith("R") -> {
                val lineSplit: List<String> = content.trim().split("\\s+".toPattern())
                assert(lineSplit.size == 3)
                lineSplit[2]
            }
            // line containing a commit along with the commit description
            content.matches("[a-z0-9]{6}.*".toRegex()) -> {
                val lineSplit: List<String> = content.trim().split("\\s+".toPattern())
                assert(lineSplit.isNotEmpty())
                "Commit: ${lineSplit[0]}"
            }
            else -> {
                val lineSplit: List<String> = content.trim().split("\\s+".toPattern())
                assert(lineSplit.size == 2)
                lineSplit[1]
            }
        }
    }

    /**
     * Auxiliary function that processes the output of
     * `git log --name-status --oneline --find-renames=<sim_threshold> --reverse <*file_name>`
     *
     * Splits the output of the command into a list of lines. Then it tries retrieve all of the lines
     * which represent additions of a the file that is being tracked.
     *
     * It then follows, starting from the last addition, each change until it gets to the last change
     * for that addition of the file. The final result would be the new location of that file.
     * The method checks the link path is being found throughout the traversal from the addition to the last change.
     *
     * If all additions have been traversed and the link path has not been found in any of these traversals,
     * we can say that the link path never existed.
     *
     * If the output of the git command that is processed is the empty string, then that file never existed in git history.
     */
    private fun processChangesForFile(
        linkPath: String,
        changes: String
    ): Pair<MutableList<Pair<String, String>>, LinkChange> {
        if (changes.isNotEmpty()) {
            val changeList: List<String> = changes.split("\n")
            val additionList: List<Pair<Int, String>> =
                changeList.withIndex()
                    .filter { (_: Int, line: String) -> line.startsWith("A") }
                    .distinctBy { pair -> pair.value }
                    .map { (i: Int, line: String) -> Pair(i, line) }
                    .reversed()

            for (pair: Pair<Int, String> in additionList) {
                var linkPathFound = false
                var lookUpIndex: Int = pair.first
                var lookUpContent: String = pair.second
                var subList: List<String> = changeList.subList(min(lookUpIndex + 1, changeList.size), changeList.size)

                val fileHistoryList: MutableList<Pair<String, String>> = mutableListOf()
                fileHistoryList.add(Pair(parseContent(changeList[lookUpIndex - 1]), parseContent(lookUpContent)))

                while (lookUpIndex != -1) {
                    val parsedLookUpContent: String = parseContent(lookUpContent).trim()

                    if (lookUpContent.contains(linkPath)) linkPathFound = true

                    // if the link path has been found during the traversal
                    // and we encounter a delete change type, that means that that file was deleted
                    // stop the search and return
                    if (linkPathFound && lookUpContent.startsWith("D")) break

                    // lookUpIndex will match the first line which is not a commit line and contains the
                    // parsedLookUpContent
                    lookUpIndex = subList.indexOfFirst { line ->
                        line.contains(parsedLookUpContent) && !parseContent(line).startsWith("Commit: ")
                    }

                    if (lookUpIndex != -1) {
                        lookUpContent = subList[lookUpIndex]
                        fileHistoryList.add(Pair(parseContent(subList[lookUpIndex - 1]), parseContent(lookUpContent)))
                    }
                    subList = subList.subList(min(lookUpIndex + 1, subList.size), subList.size)
                }

                if (linkPathFound || lookUpContent.contains(linkPath)) {
                    return Pair(fileHistoryList, extractChangeType(linkPath, lookUpContent))
                }
            }

            return Pair(
                mutableListOf(), LinkChange(
                    ChangeType.INVALID,
                    linkPath,
                    errorMessage = "File existed, but the path $linkPath to this file never existed in Git history."
                )
            )
        }
        return Pair(
            mutableListOf(), LinkChange(
                ChangeType.INVALID,
                linkPath,
                errorMessage = "Referenced file never existed in Git history."
            )
        )
    }

    /**
     * Get the remote origin url of a git repository
     *
     * Runs git command `git config --get remote.origin.url`
     */
    fun getRemoteOriginUrl(): String {
        val gitLineHandler = GitLineHandler(project, gitRepository.root, GitCommand.CONFIG)
        gitLineHandler.addParameters("--get", "remote.origin.url")

        val gitCommandResult: GitCommandResult = git.runCommand(gitLineHandler)
        return gitCommandResult.getOutputOrThrow()
    }

    /**
     * Method that retrieves a list of changes between the project version at commit commitSHA
     * and the current working tree (includes also uncommitted files)
     */
    fun getDiffWithWorkingTree(commitSHA: String): MutableCollection<com.intellij.openapi.vcs.changes.Change>? =
        GitChangeUtils.getDiffWithWorkingTree(gitRepository, commitSHA, true)

    fun getDiffBetweenCommits(commit1: String, commit2: String, beforePath: String, afterPath: String, contextLinesNumber: Int = 3): String {
        val gitLineHandler = GitLineHandler(project, gitRepository.root, GitCommand.DIFF)
        gitLineHandler.addParameters("-U$contextLinesNumber", "$commit1:$beforePath", "$commit2:$afterPath")
        val output = git.runCommand(gitLineHandler)
        return output.getOutputOrThrow()
    }

}