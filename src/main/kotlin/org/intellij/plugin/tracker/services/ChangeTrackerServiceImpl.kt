package org.intellij.plugin.tracker.services

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import java.io.File
import java.io.IOException
import java.util.regex.Matcher
import kotlin.math.max
import kotlin.math.min
import org.intellij.plugin.tracker.core.LineTracker
import org.intellij.plugin.tracker.data.CommitSHAIsNullDirectoryException
import org.intellij.plugin.tracker.data.CommitSHAIsNullLineException
import org.intellij.plugin.tracker.data.CommitSHAIsNullLinesException
import org.intellij.plugin.tracker.data.FileChangeGatheringException
import org.intellij.plugin.tracker.data.FileHasBeenDeletedException
import org.intellij.plugin.tracker.data.InvalidFileChangeException
import org.intellij.plugin.tracker.data.InvalidFileChangeTypeException
import org.intellij.plugin.tracker.data.Line
import org.intellij.plugin.tracker.data.LocalDirectoryNeverExistedException
import org.intellij.plugin.tracker.data.RemoteDirectoryNeverExistedException
import org.intellij.plugin.tracker.data.UnableToFetchLocalDirectoryChangesException
import org.intellij.plugin.tracker.data.UnableToFetchRemoteDirectoryChangesException
import org.intellij.plugin.tracker.data.changes.Change
import org.intellij.plugin.tracker.data.changes.CustomChange
import org.intellij.plugin.tracker.data.changes.CustomChangeType
import org.intellij.plugin.tracker.data.diff.DiffOutput
import org.intellij.plugin.tracker.data.diff.DiffOutputMultipleRevisions
import org.intellij.plugin.tracker.data.diff.FileHistory
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.data.links.WebLinkToDirectory
import org.intellij.plugin.tracker.settings.SimilarityThresholdSettings
import org.intellij.plugin.tracker.utils.CredentialsManager
import org.intellij.plugin.tracker.utils.GitOperationManager
import org.intellij.plugin.tracker.utils.LinkPatterns
import org.kohsuke.github.GHCommit
import org.kohsuke.github.GHCommitQueryBuilder
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder
import org.kohsuke.github.PagedIterable

class ChangeTrackerServiceImpl(project: Project) : ChangeTrackerService {

    private val gitOperationManager = GitOperationManager(project = project)

    /**
     * Get change for a local file
     */
    override fun getLocalFileChanges(link: Link, branchOrTagName: String?, specificCommit: String?): Change {
        val workingTreeChange: CustomChange? = gitOperationManager.checkWorkingTreeChanges(link)

        // this file has just been added and is not tracked by git, but the link is considered valid
        if (workingTreeChange != null && workingTreeChange.customChangeType == CustomChangeType.ADDED) {
            val fileHistory = FileHistory(path = workingTreeChange.afterPathString, fromWorkingTree = true)
            workingTreeChange.fileHistoryList = mutableListOf(fileHistory)
            return workingTreeChange
        }

        val similarityThresholdSettings: SimilarityThresholdSettings =
            SimilarityThresholdSettings.getCurrentSimilarityThresholdSettings()
        val threshold: Int = similarityThresholdSettings.fileSimilarity

        val change: CustomChange = if (branchOrTagName != null) {
            gitOperationManager.getAllChangesForFile(
                link, threshold,
                branchOrTagName = branchOrTagName, specificCommit = specificCommit
            )
        } else {
            gitOperationManager.getAllChangesForFile(
                link, threshold, specificCommit = specificCommit
            )
        }

        try {
            // so far we have only checked `git log` with the commit that is pointing to HEAD.
            // but we want to also check non-committed changes for file changes.
            // at this point, link was found and a new change has been correctly identified.
            // working tree change can be null (might be because we have first calculated the working tree change
            // using the unchanged path that was retrieved from the markdown file -- this path might have been invalid
            // but now we have a new path that corresponds to the original retrieved path
            // we want to check whether there is any non-committed change that affects this new path
            if (workingTreeChange == null) {
                val newLink: Link = link.copyWithAfterPath(link, change.afterPathString)
                val currentChange: CustomChange =
                    gitOperationManager.checkWorkingTreeChanges(newLink) ?: return change

                // new change identified (from checking working tree). Use this newly-found change instead.
                change.fileHistoryList.add(FileHistory(path = currentChange.afterPathString, fromWorkingTree = true))
                currentChange.fileHistoryList = change.fileHistoryList
                return currentChange
            }

            // if the working tree change change type is either deleted or moved
            // (calculated using the unchanged path retrieved from the markdown files),
            // use this change instead of the one found from `git log` command (it overrides it).
            // Otherwise, return the change found from `git log` command.
            return when (workingTreeChange.customChangeType) {
                CustomChangeType.DELETED, CustomChangeType.MOVED -> {
                    change.fileHistoryList.add(
                        FileHistory(
                            path = workingTreeChange.afterPathString,
                            fromWorkingTree = true
                        )
                    )
                    workingTreeChange.fileHistoryList = change.fileHistoryList
                    return workingTreeChange
                }
                else -> change
            }
        } catch (e: FileChangeGatheringException) {
            // this might be the case when a link corresponds to an uncommitted rename
            // git log history will have no changes when using the new name
            // but the working tree change will capture the rename, so we want to return it
            if (workingTreeChange != null) {
                val fileHistory = FileHistory(path = workingTreeChange.afterPathString, fromWorkingTree = true)
                workingTreeChange.fileHistoryList = mutableListOf(fileHistory)
                return workingTreeChange
            }
            throw InvalidFileChangeTypeException(e.message)
        }
    }

    /**
     * Get change for a local directory
     */
    override fun getLocalDirectoryChanges(link: Link): Change {
        val similarityThresholdSettings: SimilarityThresholdSettings =
            SimilarityThresholdSettings.getCurrentSimilarityThresholdSettings()
        val similarityThreshold: Int = similarityThresholdSettings.directorySimilarity

        val linkPath: String = link.path

        val currentContents: Boolean? = gitOperationManager.getDirectoryContentsAtCommit(linkPath, "HEAD")?.isNotEmpty()

        if (currentContents == null || currentContents) {
            return CustomChange(CustomChangeType.ADDED, link.linkInfo.linkPath)
        }

        val startCommit: String = gitOperationManager
            .getStartCommit(link, checkSurroundings = true) ?: throw CommitSHAIsNullDirectoryException()

        val directoryContents: MutableList<String>? =
            gitOperationManager.getDirectoryContentsAtCommit(linkPath, startCommit)

        if (directoryContents == null || directoryContents.size == 0) {
            throw LocalDirectoryNeverExistedException()
        }

        val movedFiles: MutableList<String> = mutableListOf()
        var deletedFiles = 0

        for (filePath: String in directoryContents) {
            val fileLink: Link = link.copyWithAfterPath(link, filePath)

            try {
                val fileChange: CustomChange = gitOperationManager.getAllChangesForFile(
                    fileLink,
                    similarityThreshold
                )

                if (fileChange.customChangeType == CustomChangeType.MOVED) {
                    movedFiles.add(fileChange.afterPathString)
                }
                if (fileChange.customChangeType == CustomChangeType.DELETED) {
                    deletedFiles++
                }
            } catch (e: FileChangeGatheringException) {
                throw UnableToFetchLocalDirectoryChangesException("Error while fetching file changes: ${e.message}")
            } catch (e: VcsException) {
                throw UnableToFetchLocalDirectoryChangesException("Error while fetching file changes: ${e.message}")
            } catch (e: Exception) {
                throw UnableToFetchLocalDirectoryChangesException("Error while fetching file changes: ${e.message}")
            }
        }

        if (deletedFiles + movedFiles.size == directoryContents.size) {
            val similarityPair: Pair<String, Int>? = calculateSimilarity(movedFiles, directoryContents.size)

            if (similarityPair != null && similarityPair.second >= similarityThreshold) {
                return CustomChange(CustomChangeType.MOVED, afterPathString = similarityPair.first)
            }
            return CustomChange(CustomChangeType.DELETED, afterPathString = linkPath)
        }
        return CustomChange(CustomChangeType.ADDED, linkPath)
    }

    /**
     * Get change for a local line
     */
    override fun getLocalLineChanges(
        link: Link,
        branchOrTagName: String?,
        specificCommit: String?
    ): Change {
        // if we cannot get the start commit, return
        val startCommit: String = gitOperationManager.getStartCommit(link, checkSurroundings = true)
            ?: throw CommitSHAIsNullLineException(fileChange = CustomChange(CustomChangeType.INVALID, ""))

        try {
            val fileChange: CustomChange = getLocalFileChanges(link, specificCommit = startCommit) as CustomChange
            val fileHistoryList: MutableList<FileHistory> = fileChange.fileHistoryList
            val diffOutputList: MutableList<DiffOutput> = mutableListOf()

            var originalLineContent = ""
            if (fileHistoryList.isNotEmpty()) {
                originalLineContent =
                    gitOperationManager.getContentsOfLineInFileAtCommit(startCommit, link.path, link.lineReferenced)
                // if the file change type is deleted, throw an exception.
                // There is no need to track the lines in this file.
                if (fileChange.customChangeType == CustomChangeType.DELETED)
                    throw FileHasBeenDeletedException(fileChange = fileChange)

                if (!fileChange.hasWorkingTreeChanges()) {
                    for (x: Int in 0 until fileHistoryList.size - 1) {
                        val beforeCommitSHA: String = fileHistoryList[x].revision
                        val beforePath: String = fileHistoryList[x].path

                        val afterCommitSHA: String = fileHistoryList[x + 1].revision
                        val afterPath: String = fileHistoryList[x + 1].path

                        println("[ ChangeTrackerServiceImpl ][ getLocalLineChanges() ] - beforeCommitSHA = $beforeCommitSHA")
                        println("[ ChangeTrackerServiceImpl ][ getLocalLineChanges() ] - afterCommitSHA = $afterCommitSHA")

                        val output: DiffOutput? = getDiffOutput(beforeCommitSHA, afterCommitSHA, beforePath, afterPath)
                        if (output != null) {
                            diffOutputList.add(output)
                        }
                    }
                } else {
                    throw NotImplementedError("TODO: get diff with working tree version of a file")
                }
            }
            val diffOutputMultipleRevisions =
                DiffOutputMultipleRevisions(
                    fileChange,
                    diffOutputList,
                    originalLineContent = originalLineContent
                )
            return LineTracker.trackLine(link, diffOutputMultipleRevisions)
        } catch (e: FileChangeGatheringException) {
            e.printStackTrace()
            throw InvalidFileChangeException(fileChange = CustomChange(CustomChangeType.INVALID, "", e.message))
        }
    }

    /**
     * Get change for multiple local lines
     */
    override fun getLocalLinesChanges(
        link: Link,
        branchOrTagName: String?,
        specificCommit: String?
    ): Change {
        // if we cannot get the start commit, throw an exception
        val startCommit: String = gitOperationManager.getStartCommit(link, checkSurroundings = true)
            ?: throw CommitSHAIsNullLinesException(fileChange = CustomChange(CustomChangeType.INVALID, ""))

        try {
            val fileChange: CustomChange = getLocalFileChanges(link, specificCommit = startCommit) as CustomChange

            val fileHistoryList: MutableList<FileHistory> = fileChange.fileHistoryList
            val diffOutputList: MutableList<DiffOutput> = mutableListOf()
            val originalLineBlockContents: MutableList<String> = mutableListOf()
            if (fileHistoryList.isNotEmpty()) {
                originalLineBlockContents.addAll(
                    gitOperationManager.getContentsOfLinesInFileAtCommit(
                        fileHistoryList.first().revision,
                        link.path,
                        link.referencedStartingLine,
                        link.referencedEndingLine
                    )
                )

                // if the file change type is deleted, throw an exception.
                // There is no need to track the lines in this file.
                if (fileChange.customChangeType == CustomChangeType.DELETED) {
                    throw FileHasBeenDeletedException(fileChange = fileChange)
                }

                if (!fileChange.hasWorkingTreeChanges()) {
                    for (x: Int in 0 until fileHistoryList.size - 1) {
                        val beforeCommitSHA: String = fileHistoryList[x].revision
                        val beforePath: String = fileHistoryList[x].path

                        val afterCommitSHA: String = fileHistoryList[x + 1].revision
                        val afterPath: String = fileHistoryList[x + 1].path

                        val output: DiffOutput? = getDiffOutput(beforeCommitSHA, afterCommitSHA, beforePath, afterPath)
                        if (output != null) {
                            diffOutputList.add(output)
                        }
                    }
                } else {
                    throw NotImplementedError("TODO: get diff with working tree version of a file")
                }
            }

            val diffOutputMultipleRevisions =
                DiffOutputMultipleRevisions(
                    fileChange,
                    diffOutputList,
                    originalLinesContents = originalLineBlockContents
                )

            return LineTracker.trackLines(link, diffOutputMultipleRevisions)
        } catch (e: FileChangeGatheringException) {
            throw InvalidFileChangeException(fileChange = CustomChange(CustomChangeType.INVALID, "", e.message))
        }
    }

    override fun getRemoteFileChanges(link: Link): Change {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun getRemoteLineChanges(link: Link): Change {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun getRemoteLinesChanges(link: Link): Change {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    /***
     * Get change for a remote directory
     */
    override fun getRemoteDirectoryChanges(link: Link): Change {
        link as WebLinkToDirectory
        val similarityThreshold = 50

        // initialize the GitHub client
        val githubBuilder = GitHubBuilder()

        val platformName: String = link.platformName
        val username: String = link.projectOwnerName
        val token: String? = CredentialsManager.getCredentials(platformName, username)
        // see whether a token exists for this user: if so, use it
        // otherwise, proceed without a token being given
        // if the repo is private, it will fail
        // if the repo is public but too many requests are being made, it will also fail
        if (token != null) {
            githubBuilder.withOAuthToken(token)
        }

        // list of all the files that have been added to this folder
        var addedFiles: MutableList<String> = mutableListOf()
        // list of all the files that have been delete from this folder
        val deletedFiles: MutableList<String> = mutableListOf()
        // list of all the files that have been moved out out this folder
        var movedFiles: MutableList<String> = mutableListOf()

        return try {
            val github: GitHub = githubBuilder.build()
            val ghRepository: GHRepository = github.getRepository("$link.projectOwnerName/$link.projectName")
            val commitQueryBuilder: GHCommitQueryBuilder = ghRepository.queryCommits()
            // get only commits that affect the directory path
            commitQueryBuilder.path(link.path)
            val commitList: PagedIterable<GHCommit> = commitQueryBuilder.list()

            for (commit: GHCommit in commitList) {
                for (file: GHCommit.File in commit.files) {
                    val fileName: String = file.fileName
                    val fileStatus: String = file.status

                    when {
                        fileName.startsWith(link.path) && fileStatus == "added" -> addedFiles.add(fileName)
                        fileName.startsWith(link.path) && fileStatus == "removed" -> deletedFiles.add(fileName)
                        fileStatus == "renamed" -> {
                            if (!file.previousFilename.startsWith(link.path) && fileName.startsWith(link.path))
                                addedFiles.add(fileName)
                            if (file.previousFilename.startsWith(link.path) && !fileName.startsWith(link.path))
                                movedFiles.add(file.fileName)
                        }
                    }
                }
            }

            addedFiles = addedFiles.distinct().toMutableList()

            // can only happen when the directory did not exist
            if (addedFiles.size == 0) {
                throw RemoteDirectoryNeverExistedException()
            }
            if (addedFiles.size == deletedFiles.size + movedFiles.size) {
                // if the directory we are looking for was deleted: look for the most common
                // part of path in the moved files paths
                // divide the # occurrences of that path by the total amount of added files to get the sim. threshold
                // if the similarity is above a certain settable number, declare the directory as moved
                // else, deleted

                val similarityPair: Pair<String, Int>? = calculateSimilarity(movedFiles, addedFiles.size)

                if (similarityPair != null && similarityPair.second >= similarityThreshold) {
                    return CustomChange(CustomChangeType.MOVED, afterPathString = similarityPair.first)
                }
                return CustomChange(CustomChangeType.DELETED, afterPathString = link.path)
            }

            // as long as there is something in the directory, we can declare it valid
            CustomChange(CustomChangeType.ADDED, afterPathString = link.path)
        } catch (e: IOException) {
            throw UnableToFetchRemoteDirectoryChangesException(e.message)
        }
    }

    /**
     * Method that takes in a list of paths of the moved files and the size of the
     * added files list as parameters. It then tries to split each path in the moved files
     * into separate sub-paths, adding each to a counting map
     *
     * It then fetches the most numerous sub-path amongst all the moved files paths
     * and divides the number of occurences to the added files list size.
     */
    private fun calculateSimilarity(movedFiles: List<String>, addedFilesSize: Int): Pair<String, Int>? {
        val countMap: HashMap<String, Int> = hashMapOf()
        for (path in movedFiles) {
            val usePath = path.replace(File(path).name, "")
            val splitPaths: List<String> = usePath.split("/")
            var pathStart = ""
            for (splitPath in splitPaths) {
                if (splitPath.isNotBlank()) {
                    pathStart += "$splitPath/"
                    if (countMap.containsKey(pathStart)) countMap[pathStart] = countMap[pathStart]!! + 1
                    else countMap[pathStart] = 1
                }
            }
        }
        val maxValue: Int = countMap.maxBy { it.value }?.value ?: return null
        val maxPair =
            countMap.filter { entry -> entry.value == maxValue }.maxBy { it.key.length }
        return Pair(maxPair!!.key.removeSuffix("/"), (maxPair.value.toDouble() / addedFilesSize * 100).toInt())
    }

    private fun getDiffOutput(
        before: String,
        after: String,
        beforePath: String,
        afterPath: String,
        contextLinesNumber: Int = 3
    ): DiffOutput? {
        val output: String =
            gitOperationManager.getDiffBetweenCommits(before, after, beforePath, afterPath, contextLinesNumber)
        // skip the git diff header (first 4 lines)
        if (output.isEmpty()) return null
        // filter out all git-added "No newline at end of file" lines
        val lines: List<String?> = output.lines()
            .subList(4, output.lines().size)
            .filterNot { line -> line == "\\ No newline at end of file" }
        val addedLines: MutableList<Line> = mutableListOf()
        val deletedLines: MutableList<Line> = mutableListOf()
        var startDeletedLine: Int
        var startAddedLine: Int
        var currentAddedLine = 0
        var currentDeletedLine = 0
        var contextLinesDeleted: MutableList<Line> = mutableListOf()
        var contextLinesAdded: MutableList<Line> = mutableListOf()

        for (line: String? in lines) {
            if (line == null) {
                break
                // git hunk info header
            } else if (line.startsWith("@@ ")) {
                val info = line.split(" @@")[0]
                val matcher: Matcher = LinkPatterns.GitDiffChangedLines.pattern.matcher(info)
                if (matcher.matches()) {
                    startDeletedLine = matcher.group(1).toInt()
                    currentDeletedLine = startDeletedLine
                    startAddedLine = matcher.group(6).toInt()
                    currentAddedLine = startAddedLine
                }
                // added line
            } else if (line.startsWith("+")) {
                val addedLine = Line(currentAddedLine, line.replaceFirst("+", ""))
                addedLines.add(addedLine)
                contextLinesAdded.add(addedLine)
                currentAddedLine++
                // deleted line
            } else if (line.startsWith("-")) {
                val deletedLine = Line(currentDeletedLine, line.replaceFirst("-", ""))
                deletedLines.add(deletedLine)
                contextLinesDeleted.add(deletedLine)
                currentDeletedLine++
                // this is an unchanged line: just add it to the context lines lists and increment the indices
            } else {
                contextLinesDeleted.add(Line(currentDeletedLine, line))
                contextLinesAdded.add(Line(currentAddedLine, line))
                currentAddedLine++
                currentDeletedLine++
            }
        }

        // remove git-added warning lines
        while (contextLinesAdded.last().content == "\\ No newline at end of file") {
            contextLinesAdded = contextLinesAdded.subList(0, contextLinesAdded.size - 1)
        }

        // remove git-added warning lines
        while (contextLinesDeleted.last().content == "\\ No newline at end of file") {
            contextLinesDeleted = contextLinesDeleted.subList(0, contextLinesDeleted.size - 1)
        }

        // populate the context lines properties of the added lines
        for (l: Line in addedLines) {
            val maxContextLineNumber: Int = contextLinesAdded.maxBy { line -> line.lineNumber }?.lineNumber ?: continue

            // get all of the context lines on the upper side of the line:
            // that is, the lines within [current_line_number - contextLinesNumber, current_line_number)
            // as well as all of the context line on the lower side of the line:
            // all of the lines within the interval (current_line_number, current_line-number+ contextLinesNumber)
            val contextLines: MutableList<Line> = contextLinesAdded.filter { line ->
                (line.lineNumber < l.lineNumber && line.lineNumber >= max(0, l.lineNumber - contextLinesNumber)) ||
                        (line.lineNumber > l.lineNumber && line.lineNumber <= min(l.lineNumber + contextLinesNumber,
                            maxContextLineNumber
                ))
            }.toMutableList()
            l.contextLines = contextLines
        }

        // populate the context lines properties of the deleted lines
        for (l: Line in deletedLines) {
            val maxContextLineNumber: Int =
                contextLinesDeleted.maxBy { line -> line.lineNumber }?.lineNumber ?: continue

            // get all of the context lines on the upper side of the line:
            // that is, the lines within [current_line_number - contextLinesNumber, current_line_number)
            // as well as all of the context line on the lower side of the line:
            // all of the lines within the interval (current_line_number, current_line-number+ contextLinesNumber)
            val contextLines: MutableList<Line> = contextLinesDeleted.filter { line ->
                (line.lineNumber < l.lineNumber && line.lineNumber >= max(0, l.lineNumber - contextLinesNumber)) ||
                        (line.lineNumber > l.lineNumber && line.lineNumber <= min(l.lineNumber + contextLinesNumber,
                            maxContextLineNumber
                ))
            }.toMutableList()
            l.contextLines = contextLines
        }

        return DiffOutput(beforePath, addedLines, deletedLines, before, after)
    }

    companion object {
        fun getInstance(project: Project): ChangeTrackerServiceImpl =
            ServiceManager.getService(
                project,
                ChangeTrackerServiceImpl::class.java
            )
    }
}
