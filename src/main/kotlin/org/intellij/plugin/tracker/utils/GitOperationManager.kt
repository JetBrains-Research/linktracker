package org.intellij.plugin.tracker.utils

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitCommandResult
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import java.io.File
import kotlin.math.max
import kotlin.math.min
import org.intellij.plugin.tracker.data.ChangeTypeExtractionException
import org.intellij.plugin.tracker.data.OriginalLineContentsNotFoundException
import org.intellij.plugin.tracker.data.OriginalLinesContentsNotFoundException
import org.intellij.plugin.tracker.data.ReferencedFileNotFoundException
import org.intellij.plugin.tracker.data.ReferencedPathNotFoundException
import org.intellij.plugin.tracker.data.changes.CustomChange
import org.intellij.plugin.tracker.data.changes.CustomChangeType
import org.intellij.plugin.tracker.data.diff.FileHistory
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.services.LinkUpdaterService

/**
 * Class that handles the logic of executing all git commands needed throughout the project
 * processing the outputs (of the git commands) and returning it.
 */
class GitOperationManager(private val project: Project) {

    /**
     * Main, git service class. It is needed for running all of the git commands,
     * by using it's runCommand method
     */
    private val git: Git = Git.getInstance()

    /**
     * The git repository of the currently open project in the IDE
     */
    private val gitRepository: GitRepository

    /**
     * Initializes the git repository property of this class, by fetching all of the repositories
     * that exist in this project and getting the first one that appears in the list
     *
     * If this cannot ber done, throw an exception
     */
    init {
        val repositories: MutableList<GitRepository> = GitRepositoryManager.getInstance(project).repositories
        if (repositories.isNotEmpty()) {
            gitRepository = repositories[0]
        } else {
            throw VcsException("Could not find Git Repository")
        }
    }

    /**
     * Get the contents of a directory at a specific commit
     *
     * Runs git command `git ls-tree --name-only -r <commitSHA> -- <dirPath>`
     * Returns null if the contents cannot be fetched
     */
    fun getDirectoryContentsAtCommit(directoryPath: String, commitSHA: String): MutableList<String>? {
        val gitLineHandler = GitLineHandler(project, gitRepository.root, GitCommand.LS_TREE)
        gitLineHandler.addParameters("--name-only", "-r", commitSHA, "--", directoryPath)
        val result: GitCommandResult = git.runCommand(gitLineHandler)
        if (result.exitCode == 0) {
            return result.output
        }
        return null
    }

    /**
     * Gets the contents of a single line from a file, at a specified commit
     *
     * Runs git command `git show <commitSHA>:<path>` that retrieves the contents of the file
     * at <path> at <commitSHA>. Then the method tries to get the line at `lineNumber`.
     *
     * Throw an exception of the requested line do not exist in the version of file at <path> and <commitSHA>
     */
    @Throws(VcsException::class)
    fun getContentsOfLineInFileAtCommit(commitSHA: String, path: String, lineNumber: Int): String {
        val gitLineHandler = GitLineHandler(project, gitRepository.root, GitCommand.SHOW)
        gitLineHandler.addParameters("$commitSHA:$path")
        val result: GitCommandResult = git.runCommand(gitLineHandler)
        if (result.exitCode == 0) {
            val lines: List<String> = result.output
            if (lines.size >= lineNumber) return lines[lineNumber - 1]
        }
        throw OriginalLineContentsNotFoundException(fileChange = CustomChange(CustomChangeType.INVALID, ""))
    }

    /**
     * Gets the contents of multiple, consecutive lines from a file, at a specified commit
     *
     * Runs git command `git show <commitSHA>:<path>` that retrieves the contents of the file
     * at <path> at <commitSHA>. Then the method tries to get the lines starting (including)
     * `startLineNumber` and ending `endLineNumber` (included as well).
     *
     * Throw an exception of the requested lines do not exist in the version of file at <path> and <commitSHA>
     */
    @Throws(VcsException::class)
    fun getContentsOfLinesInFileAtCommit(
        commitSHA: String,
        path: String,
        startLineNumber: Int,
        endLineNumber: Int
    ): List<String> {
        val gitLineHandler = GitLineHandler(project, gitRepository.root, GitCommand.SHOW)
        gitLineHandler.addParameters("$commitSHA:$path")
        val result: GitCommandResult = git.runCommand(gitLineHandler)
        if (result.exitCode == 0) {
            val lines: List<String> = result.output
            if (lines.size >= endLineNumber) return lines.subList(startLineNumber - 1, endLineNumber)
        }
        throw OriginalLinesContentsNotFoundException(fileChange = CustomChange(CustomChangeType.INVALID, ""))
    }

    /**
     * Checks whether a reference name corresponds to a valid tag name (in the repository associated to the open project)
     *
     * This is done by listing all tags in the repository and checking whether `ref` is present in this list
     * Runs git command `git tag -l`
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
     * Checks whether a reference name corresponds to a valid branch name (in the repository associated to the open project)
     *
     * This is done by listing all branches in the repository and checking whether `ref` is present in this list
     * Runs git command `git branch -l`
     */
    @Throws(VcsException::class)
    fun isRefABranch(ref: String): Boolean {
        val gitLineHandler = GitLineHandler(project, gitRepository.root, GitCommand.BRANCH)
        // list all branch names for this project
        gitLineHandler.addParameters("-l")
        val output: GitCommandResult = git.runCommand(gitLineHandler)
        val outputString: String = output.getOutputOrThrow()
        var branchList: List<String> = outputString.split("\n")
        branchList = branchList.map { line -> line.trim() }
        branchList = branchList.map { line -> line.replace("* ", "") }
        return ref in branchList
    }

    /**
     * Checks whether a reference name corresponds to a valid commit SHA (in the repository associated to the open project)
     *
     * This is done by running git command `git rev-parse --verify -q <ref>^{commit}` and then checking the exit code
     * If the exit code is 0, it means that `ref` points to a valid commit SHA. Otherwise, it does not and false is returned
     */
    fun isRefACommit(ref: String): Boolean {
        val gitLineHandler = GitLineHandler(project, gitRepository.root, GitCommand.REV_PARSE)
        gitLineHandler.addParameters(
            "--verify",
            "-q",
            "$ref^{commit}"
        )
        val output: GitCommandResult = git.runCommand(gitLineHandler)
        if (output.exitCode == 0) return true
        return false
    }

    /**
     * Get the commit SHA which points to the current HEAD of the repository
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
     *
     * If the file does not exist at the found commit, then if `checkSurroundings` parameter is true,
     * check the commits within `maxCommitsSurrounding` behind and in front of this initially found start commit.
     * Then, return the first commit SHA at which the link path is found (it first tries to go backwards and then forwards).
     */
    fun getStartCommit(link: Link, checkSurroundings: Boolean = false, maxCommitsSurroundings: Int = 5): String? {
        val linkInfo = link.linkInfo
        val gitLineHandler = GitLineHandler(project, gitRepository.root, GitCommand.LOG)
        gitLineHandler.addParameters("--oneline", "-S${linkInfo.getMarkDownSyntaxString()}")
        val outputLog = git.runCommand(gitLineHandler)
        if (outputLog.exitCode == 0) {
            // return most recent finding
            if (outputLog.output.size != 0) {
                val commitSHA = outputLog.output[0].split(" ")[0]
                if (fileExistsAtCommit(commitSHA, link.path)) {
                    return outputLog.output[0].split(" ")[0]
                }

                // file does not exist at the found commit
                // find the most recent commit at which this file exists
                // starting at the initially found `start commit`.
                // go - maxCommitsSurroundings back and + maxCommitsSurroundings
                // after the found commit.
                if (checkSurroundings) {
                    if (commitSHA != getFirstCommitSHA()) {
                        val startCommit = getCommitsInRange(
                            until = commitSHA,
                            maxCommitsSurroundings = maxCommitsSurroundings,
                            path = link.path
                        )
                        if (startCommit != null) return startCommit
                    }
                    if (commitSHA != getHeadCommitSHA()) {
                        val startCommit = getCommitsInRange(
                            from = commitSHA,
                            maxCommitsSurroundings = maxCommitsSurroundings,
                            path = link.path
                        )
                        if (startCommit != null) return startCommit
                    }
                }
            }
        }
        return null
    }

    /**
     * Auxiliary function for method getStartCommit.
     * This function retrieves all the commit SHAs that are in between a specific range and checks whether the file/directory
     * (identified by the path parameter) exists at any of these retrieved commits.
     *
     * This method accepts either a `from` or `until` value, but not both. Also, when none of these is specified,
     * it is also considered as an invalid method call.
     *
     * If `from` is specified, then the method will fetch the commits starting at `from` (not including) plus
     * `maxCommitsSurrounding` in front of this commit.
     * If `until` is specified, then the method will fetch the commits ending at `until` (not including) minus
     * `maxCommitsSurrounding` behind this commit.
     *
     * With each of these commits, it will check whether `path` exists at that commit. If it exists,
     * return the first commit SHA at which the path exists.
     */
    private fun getCommitsInRange(
        from: String? = null,
        until: String? = null,
        maxCommitsSurroundings: Int,
        path: String
    ): String? {
        if (from == null && until == null || from != null && until != null) {
            throw VcsException("Illegal arguments")
        }
        val gitLineHandlerLog = GitLineHandler(project, gitRepository.root, GitCommand.LOG)
        if (from != null) {
            gitLineHandlerLog.addParameters("$from..", "--oneline")
        } else {
            gitLineHandlerLog.addParameters("$until^", "--oneline")
        }
        val resultLog: GitCommandResult = git.runCommand(gitLineHandlerLog)
        if (resultLog.exitCode == 0) {
            val commitList = resultLog.output
            var index = 0
            for (line in commitList) {
                if (index > maxCommitsSurroundings) break
                index++
                val commit = line.substring(0, 7)
                if (fileExistsAtCommit(commit, path)) {
                    return commit
                }
            }
        }
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
    private fun processWorkingTreeChanges(linkPath: String, changes: String): CustomChange? {
        val changeList: List<String> = changes.split("\n")
        changeList.forEach { line -> line.trim() }

        var change: String? = changeList.find { line -> line.split(" ".toRegex()).any { res -> res == linkPath }}
        if (change != null) {
            change = change.trim()
            when {
                change.startsWith("?") -> return CustomChange(CustomChangeType.ADDED, linkPath)
                change.startsWith("!") -> return CustomChange(CustomChangeType.ADDED, linkPath)
                change.startsWith("C") -> return CustomChange(CustomChangeType.ADDED, linkPath)
                change.startsWith("A") -> return CustomChange(CustomChangeType.ADDED, linkPath)
                change.startsWith("U") -> return CustomChange(CustomChangeType.ADDED, linkPath)
                change.startsWith("R") -> {
                    val lineSplit = change.split(" -> ")
                    assert(lineSplit.size == 2)
                    return CustomChange(CustomChangeType.MOVED, lineSplit[1])
                }
                change.startsWith("D") -> return CustomChange(CustomChangeType.DELETED, linkPath)
                change.startsWith("M") -> return CustomChange(CustomChangeType.MODIFIED, linkPath)
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
    fun checkWorkingTreeChanges(link: Link): CustomChange? {
        val gitLineHandler = GitLineHandler(project, gitRepository.root, GitCommand.STATUS)
        gitLineHandler.addParameters("--porcelain=v1")
        val outputLog: GitCommandResult = git.runCommand(gitLineHandler)
        return processWorkingTreeChanges(link.path, outputLog.getOutputOrThrow())
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
    ): CustomChange {
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
            "*${link.referencedFileName}"
        )

        val outputLog: GitCommandResult = git.runCommand(gitLineHandler)
        return processChangesForFile(link, link.path, outputLog.getOutputOrThrow(), specificCommit)
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
    private fun extractChangeType(linkPath: String, line: String): CustomChange {
        when {
            line.startsWith("A") -> {
                val lineSplit: List<String> = line.trim().split("\t".toPattern())
                assert(lineSplit.size == 2)
                if (lineSplit[1] != linkPath) return CustomChange(CustomChangeType.MOVED, lineSplit[1])

                return CustomChange(CustomChangeType.ADDED, lineSplit[1])
            }
            line.startsWith("M") -> {
                val lineSplit: List<String> = line.trim().split("\t".toPattern())
                assert(lineSplit.size == 2)
                if (lineSplit[1] != linkPath) return CustomChange(CustomChangeType.MOVED, lineSplit[1])

                return CustomChange(CustomChangeType.MODIFIED, lineSplit[1])
            }
            line.startsWith("D") -> return CustomChange(CustomChangeType.DELETED, linkPath)
            line.startsWith("R") -> {
                val lineSplit: List<String> = line.trim().split("\t".toPattern())
                assert(lineSplit.size == 3)
                if (lineSplit[2] == linkPath) return CustomChange(CustomChangeType.MODIFIED, lineSplit[2])
                return CustomChange(CustomChangeType.MOVED, lineSplit[2])
            }
            else -> throw ChangeTypeExtractionException()
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
                val lineSplit: List<String> = content.trim().split("\t".toPattern())
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
                val lineSplit: List<String> = content.trim().split("\t".toPattern())
                assert(lineSplit.size == 2)
                lineSplit[1]
            }
        }
    }

    /**
     * Checks whether a given file/directory (identified by a path) exists at a certain commit
     * in the repository associated with the open project
     *
     * Runs git command `git show <commitSHA>:<path> and checks the exit code of this command.
     */
    private fun fileExistsAtCommit(commitSHA: String, path: String): Boolean {
        val gitLineHandler = GitLineHandler(project, gitRepository.root, GitCommand.SHOW)
        gitLineHandler.addParameters("$commitSHA:$path")
        return git.runCommand(gitLineHandler).exitCode == 0
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
        link: Link,
        linkPath: String,
        changes: String,
        specificCommit: String?
    ): CustomChange {
        if (changes.isNotEmpty()) {
            val changeList: List<String> = changes.split("\n")
            var additionList: List<Pair<Int, String>> =
                changeList.withIndex()
                    .filter { (_: Int, line: String) -> line.startsWith("A") }
                    .distinctBy { pair -> pair.value }
                    .map { (i: Int, line: String) -> Pair(i, line) }

            // if a specific commit is specified as a starting point
            // we  only want to start tracking the path that corresponds to the
            // first entry in change list
            // if we can not find the link path we are looking for within
            // the traversal, throw an exception
            if (specificCommit != null) {
                var linkPathFound = false
                val fileHistoryList: MutableList<FileHistory> = mutableListOf()
                var lookUpIndex: Int = 1
                var lookUpContent = changeList[lookUpIndex]
                var subList: List<String> = changeList.subList(min(lookUpIndex + 1, changeList.size), changeList.size)

                var deletionsAndAdditions = 0

                // only add the FileHistory containing specificCommit as revision
                // and linkPath as path if it truly existed
                if (fileExistsAtCommit(specificCommit, linkPath)) {
                    fileHistoryList.add(FileHistory("Commit: $specificCommit", linkPath))
                }
                fileHistoryList.add(FileHistory(parseContent(changeList[lookUpIndex - 1]), parseContent(lookUpContent)))

                while (lookUpIndex != -1) {
                    val parsedLookUpContent: String = parseContent(lookUpContent).trim()

                    if (lookUpContent.contains(linkPath)) linkPathFound = true

                    // if the link path has been found during the traversal
                    // and we encounter a delete change type, that means that that file was deleted
                    // stop the search and return
                    if (linkPathFound && lookUpContent.startsWith("D")) deletionsAndAdditions++

                    // lookUpIndex will match the first line which is not a commit line and contains the
                    // parsedLookUpContent
                    lookUpIndex = subList.indexOfFirst { line ->
                        line.contains(parsedLookUpContent) && !parseContent(line).startsWith("Commit: ")
                    }

                    if (lookUpIndex != -1) {
                        lookUpContent = subList[lookUpIndex]
                        fileHistoryList.add(
                            FileHistory(
                                parseContent(subList[max(0, lookUpIndex - 1)]),
                                parseContent(lookUpContent)
                            )
                        )
                    }
                    subList = subList.subList(min(lookUpIndex + 1, subList.size), subList.size)
                }

                if (linkPathFound || lookUpContent.contains(linkPath)) {
                    val linkChange: CustomChange = extractChangeType(linkPath, lookUpContent)
                    linkChange.fileHistoryList = fileHistoryList
                    linkChange.deletionsAndAdditions = deletionsAndAdditions
                    return linkChange
                }
                throw ReferencedPathNotFoundException(linkPath)
            }

            additionList = additionList.reversed()
            for (pair: Pair<Int, String> in additionList) {
                var linkPathFound = false
                var lookUpIndex: Int = pair.first
                var lookUpContent: String = pair.second
                var subList: List<String> = changeList.subList(min(lookUpIndex + 1, changeList.size), changeList.size)

                var deletionsAndAdditions = 0

                val fileHistoryList: MutableList<FileHistory> = mutableListOf()
                fileHistoryList.add(FileHistory(parseContent(changeList[lookUpIndex - 1]), parseContent(lookUpContent)))

                while (lookUpIndex != -1) {
                    val parsedLookUpContent: String = parseContent(lookUpContent).trim()

                    if (lookUpContent.contains(linkPath)) linkPathFound = true

                    // if the link path has been found during the traversal
                    // and we encounter a delete change type, that means that that file was deleted
                    // stop the search and return
                    if (linkPathFound && lookUpContent.startsWith("D")) deletionsAndAdditions++

                    // lookUpIndex will match the first line which is not a commit line and contains the
                    // parsedLookUpContent
                    lookUpIndex = subList.indexOfFirst { line ->
                        line.contains(parsedLookUpContent) && !parseContent(line).startsWith("Commit: ")
                    }

                    if (lookUpIndex != -1) {
                        lookUpContent = subList[lookUpIndex]
                        fileHistoryList.add(
                            FileHistory(
                                parseContent(subList[max(0, lookUpIndex - 1)]),
                                parseContent(lookUpContent)
                            )
                        )
                    }
                    subList = subList.subList(min(lookUpIndex + 1, subList.size), subList.size)
                }

                if (linkPathFound || lookUpContent.contains(linkPath)) {
                    val linkChange: CustomChange = extractChangeType(linkPath, lookUpContent)
                    linkChange.fileHistoryList = fileHistoryList
                    linkChange.deletionsAndAdditions = deletionsAndAdditions
                    return linkChange
                }
            }

            // If this is a working tree change, finds the corresponding change of this link
            val paths = LinkUpdaterService.workingTreePaths
            for (path in paths) {
                if (path.linkInfo.fileName == link.linkInfo.fileName &&
                    path.linkInfo.proveniencePath == link.linkInfo.proveniencePath) {
                    return CustomChange(CustomChangeType.MOVED, path.path)
                }
            }
            throw ReferencedPathNotFoundException(linkPath)
        }

        if (specificCommit != null && File("${project.basePath}/$linkPath").exists()) {
            val fileChange = CustomChange(CustomChangeType.ADDED, afterPathString = linkPath)
            fileChange.fileHistoryList = mutableListOf(FileHistory("Commit: $specificCommit", linkPath))
            return fileChange
        }

        // If this is a working tree change, finds the corresponding change of this link
        val paths = LinkUpdaterService.workingTreePaths
        for (path in paths) {
            if (path.linkInfo.fileName == link.linkInfo.fileName &&
                path.linkInfo.proveniencePath == link.linkInfo.proveniencePath) {
                return CustomChange(CustomChangeType.MOVED, path.path)
            }
        }

        throw ReferencedFileNotFoundException()
    }

    /**
     * Get the remote origin url of a git repository (associated with the currently open project)
     *
     * Runs git command `git config --get remote.origin.url`
     */
    fun getRemoteOriginUrl(): String {
        val gitLineHandler = GitLineHandler(project, gitRepository.root, GitCommand.CONFIG)
        gitLineHandler.addParameters("--get", "remote.origin.url")
        val outputLog: GitCommandResult = git.runCommand(gitLineHandler)
        return outputLog.getOutputOrThrow()
    }

    /**
     * Gets the diff between the version of the file at the last commit
     * and the working version of the file
     *
     * Runs git command `git -U<context_lines_no> diff <path>`
     */
    @Throws(VcsException::class)
    fun getDiffWithWorkingVersionOfFile(
        beforePath: String,
        afterPath: String,
        contextLinesNumber: Int = 3
    ): String {
        val gitLineHandler = GitLineHandler(project, gitRepository.root, GitCommand.DIFF)
        gitLineHandler.addParameters(
            "-U$contextLinesNumber",
            "HEAD:$beforePath",
            afterPath
        )
        val output = git.runCommand(gitLineHandler)
        return output.getOutputOrThrow()
    }

    /**
     * Runs a git diff command between the version of the file at path `beforePath` at `commit1`
     * and the version of the file at path `afterPath` at `commit2`.
     *
     * Runs command `git diff -U<contextLinesNumber> <commit1>:<beforePath> <commit2:afterPath>`
     */
    @Throws(VcsException::class)
    fun getDiffBetweenCommits(
        commit1: String,
        commit2: String,
        beforePath: String,
        afterPath: String,
        contextLinesNumber: Int = 3
    ): String {
        val gitLineHandler = GitLineHandler(project, gitRepository.root, GitCommand.DIFF)
        gitLineHandler.addParameters("-U$contextLinesNumber", "$commit1:$beforePath", "$commit2:$afterPath")
        val output = git.runCommand(gitLineHandler)
        return output.getOutputOrThrow()
    }
}
