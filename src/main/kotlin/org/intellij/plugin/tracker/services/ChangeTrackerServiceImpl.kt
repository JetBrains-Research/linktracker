package org.intellij.plugin.tracker.services

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import org.intellij.plugin.tracker.core.LineTracker
import org.intellij.plugin.tracker.data.*
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
import org.kohsuke.github.*
import java.io.File
import java.io.IOException

/**
 * Implementation of ChangeTrackerService interface, adapted to work in IntelliJ IDEA environment
 */
class ChangeTrackerServiceImpl(val project: Project) : ChangeTrackerService {

    /**
     * This property handles the execution of all git commands needed by this class
     */
    private val gitOperationManager = GitOperationManager(project = project)

    /**
     * Get the change for a link to a file that corresponds to the currently open project in the IDE
     *
     * First, it checks whether the file referenced is un-tracked - if so, return
     * Otherwise, it tries to fetch all the changes that have affected the file throughout git history
     *
     * If a new path is found, then try to see whether there is any working tree changes that would affect
     * this new path. If there are changes, then use this new change as the final change.
     * Otherwise, return the previous change.
     */
    override fun getLocalFileChanges(link: Link, branchOrTagName: String?, specificCommit: String?): Change {
        val workingTreeChange: CustomChange? = gitOperationManager.checkWorkingTreeChanges(link)

        // this file has just been added and is not tracked by git, but the link is considered valid
        if (workingTreeChange != null && workingTreeChange.customChangeType == CustomChangeType.ADDED) {
            val fileHistory = FileHistory(path = workingTreeChange.afterPathString, fromWorkingTree = true)
            workingTreeChange.fileHistoryList = mutableListOf(fileHistory)
            return workingTreeChange
        }

        /**
         * If a working tree change is  updated, its link path and after path will be same.
         * Following functionality is written to move it from Changed Links to Unchanged Links.
         */
        if (workingTreeChange != null && link.path == workingTreeChange.afterPathString && (workingTreeChange.
            customChangeType == CustomChangeType.MOVED || workingTreeChange.customChangeType == CustomChangeType.DELETED)) {
            val fileHistory = FileHistory(path = workingTreeChange.afterPathString, fromWorkingTree = true)
            workingTreeChange.fileHistoryList = mutableListOf(fileHistory)
            return CustomChange(CustomChangeType.ADDED, workingTreeChange.afterPathString)
        }

        val similarityThresholdSettings: SimilarityThresholdSettings =
            SimilarityThresholdSettings.getCurrentSimilarityThresholdSettings()
        val threshold: Int = similarityThresholdSettings.fileSimilarity

        val change: CustomChange =
            gitOperationManager.getAllChangesForFile(
                link, threshold,
                branchOrTagName = branchOrTagName, specificCommit = specificCommit
            )

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
                CustomChangeType.DELETED, CustomChangeType.MOVED, CustomChangeType.MODIFIED -> {
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
     * Get change that has affected a directory that is referenced by a link
     *
     * This method first tries to see whether the folder exists at the commit that is pointing to HEAD.
     * If so, return a change that this directory is not changed.
     *
     * Otherwise, try to get the start commit of the line containing this link in the project. If it cannot be found,
     * raise an appropriate exception.
     * Then, the method tries to fetch the directory contents at this start commit. After that, using these contents (files),
     * it will track each file individually throughout git history.
     * The method then checks whether the size of the directory contents at start commit is equal to the size of
     * the files found deleted + the files found to be moved. If so, it means that the directory has either been deleted/moved.
     *
     * In that case, if we can find a common sub-path in the moved files, and if the number of occurrences of this common sub-path
     * exceeds a given threshold value, we can say that the directory has been moved. Otherwise, it is deleted.
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
                if (fileChange.customChangeType == CustomChangeType.DELETED) {
                    throw FileHasBeenDeletedException(fileChange = fileChange)
                }
                getDiffOutputs(fileHistoryList, diffOutputList)
            }
            val diffOutputMultipleRevisions =
                DiffOutputMultipleRevisions(
                    fileChange,
                    diffOutputList,
                    originalLineContent = originalLineContent
                )
            return LineTracker.trackLine(link, diffOutputMultipleRevisions)
        } catch (e: FileChangeGatheringException) {
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
                    throw FileHasBeenDeletedLinesException(fileChange = fileChange)
                }
                getDiffOutputs(fileHistoryList, diffOutputList)
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
        val movedFiles: MutableList<String> = mutableListOf()

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
     * and divides the number of occurrences to the added files list size.
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

    /**
     * Goes over, commit-by-commit (from the list of file history) and calls auxiliary methods of getting git diff
     * between the file at a commit and a path (before) and the file at another commit and path (after)
     * It then adds the result to a git diff output list, which is going to be passed to the line tracking module.
     */
    private fun getDiffOutputs(fileHistoryList: List<FileHistory>, diffOutputList: MutableList<DiffOutput>) {
        for (x: Int in 0 until fileHistoryList.size - 1) {
            val beforeCommitSHA: String = fileHistoryList[x].revision
            val beforePath: String = fileHistoryList[x].path

            val afterCommitSHA: String = fileHistoryList[x + 1].revision
            val afterPath: String = fileHistoryList[x + 1].path

            val output: DiffOutput? =
                DiffOutput.getDiffOutput(gitOperationManager, beforeCommitSHA, afterCommitSHA, beforePath, afterPath)
            if (output != null) {
                diffOutputList.add(output)
            }
        }
    }

    /**
     * Get the changes for (web) links to a file that does not correspond to the currently open project
     *
     * Has to resort to using the API of the platform hosting the code
     */
    override fun getRemoteFileChanges(link: Link): Change {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Get the changes for (web) links to a single line that does not correspond to the currently open project
     *
     * Has to resort to using the API of the platform hosting the code
     */
    override fun getRemoteLineChanges(link: Link): Change {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Get the changes for (web) links to multiple lines that do not correspond to the currently open project
     *
     * Has to resort to using the API of the platform hosting the code
     */
    override fun getRemoteLinesChanges(link: Link): Change {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    companion object {

        /**
         * Used by IDEA to get a reference to the single instance of this class.
         */
        fun getInstance(project: Project): ChangeTrackerServiceImpl =
            ServiceManager.getService(
                project,
                ChangeTrackerServiceImpl::class.java
            )
    }
}
