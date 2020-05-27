package org.intellij.plugin.tracker.services

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import org.intellij.plugin.tracker.data.changes.ChangeType
import org.intellij.plugin.tracker.data.changes.DirectoryChange
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.data.links.WebLinkToDirectory
import org.intellij.plugin.tracker.utils.CredentialsManager
import org.kohsuke.github.*
import java.io.File
import java.io.IOException


class GitHubChangeTrackerService {


    fun getDirectoryChanges(link: WebLinkToDirectory, similarityThreshold: Int = 50): Pair<Link, DirectoryChange> {
        // initialize the GitHub client
        val githubBuilder = GitHubBuilder()

        val platformName: String = link.getPlatformName()
        val username: String = link.getProjectOwnerName()
        val token: String? = CredentialsManager.getCredentials(platformName, username)
        // see whether a token exists for this user: if so, use it
        // otherwise, proceed without a token being given
        // if the repo is private, it will fail
        // if the repo is public but too many requests are being made, it will also fail
        if (token != null) {
            githubBuilder.withOAuthToken(token)
        }

        val github: GitHub = githubBuilder.build()

        // list of all the files that have been added to this folder
        var addedFiles: MutableList<String> = mutableListOf()
        // list of all the files that have been moved out of this folder
        val deletedFiles: MutableList<String> = mutableListOf()
        // list of all the files deletes from this folder
        val movedFiles: MutableList<String> = mutableListOf()

        return try {
            val ghRepository: GHRepository = github.getRepository("${link.getProjectOwnerName()}/${link.getProjectName()}")
            val commitQueryBuilder: GHCommitQueryBuilder = ghRepository.queryCommits()
            // get only commits that affect the directory path
            commitQueryBuilder.path(link.getPath())
            val commitList: PagedIterable<GHCommit> = commitQueryBuilder.list()

            for (commit: GHCommit in commitList) {
                for (file: GHCommit.File in commit.files) {
                    val fileName: String = file.fileName
                    val fileStatus: String = file.status

                    when {
                        fileName.startsWith(link.getPath()) && fileStatus == "added" -> addedFiles.add(fileName)
                        fileName.startsWith(link.getPath()) && fileStatus == "removed" -> deletedFiles.add(fileName)
                        fileStatus == "renamed" -> {
                            if (!file.previousFilename.startsWith(link.getPath()) && fileName.startsWith(link.getPath()))
                                addedFiles.add(fileName)
                            if (file.previousFilename.startsWith(link.getPath()) && !fileName.startsWith(link.getPath()))
                                movedFiles.add(file.fileName)
                        }
                    }
                }
            }

            addedFiles = addedFiles.distinct().toMutableList()

            // can only happen when the directory did not exist
            if (addedFiles.size == 0) {
                return Pair(
                    link,
                    DirectoryChange(
                        ChangeType.INVALID,
                        link.getPath(),
                        errorMessage = "Directory ${link.getPath()} never existed"
                    )
                )
            }
            if (addedFiles.size == deletedFiles.size + movedFiles.size) {
                // if the directory we are looking for was deleted: look for the most common
                // part of path in the moved files paths
                // divide the # occurrences of that path by the total amount of added files to get the sim. threshold
                // if the similarity is above a certain settable number, declare the directory as moved
                // else, deleted

                val similarityPair: Pair<String, Int> = calculateSimilarity(movedFiles, addedFiles.size)

                if (similarityPair.second >= similarityThreshold) {
                    return Pair(link, DirectoryChange(ChangeType.MOVED, afterPath = similarityPair.first))
                }
                return Pair(link, DirectoryChange(ChangeType.DELETED, afterPath = link.getPath()))
            }

            // as long as there is something in the directory, we can declare it valid
            Pair(link, DirectoryChange(ChangeType.ADDED, afterPath = link.getPath()))
        } catch (e: IOException) {
            Pair(
                link,
                DirectoryChange(ChangeType.INVALID, afterPath = link.linkInfo.linkPath,
                errorMessage = e.message)
            )
        }
    }

    private fun calculateSimilarity(movedFiles: List<String>, addedFilesSize: Int): Pair<String, Int> {
        val countMap: Map<String, Int> = movedFiles.map { path -> path.replace(File(path).name, "") }
            .groupingBy { it }
            .eachCount()
        val maxPair: Map.Entry<String, Int>? = countMap.maxBy { it.value }
        return Pair(maxPair!!.key, (maxPair.value.toDouble() / addedFilesSize * 100).toInt())
    }

    /**
     * Used by IDEA to get a reference to the single instance of this class.
     */
    companion object {
        fun getInstance(project: Project): GitHubChangeTrackerService =
            ServiceManager.getService(project, GitHubChangeTrackerService::class.java)
    }
}
