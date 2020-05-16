package org.intellij.plugin.tracker.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import org.intellij.plugin.tracker.data.changes.FileChange
import org.intellij.plugin.tracker.data.links.*
import java.io.File

class LinkFactory {

    companion object {

        /**
         * Cache each link that is being identified for faster identification upon consecutive runs
         */
        private val cachedResults: HashMap<LinkInfo, Link> = hashMapOf()
        var firstRun = true

        private fun processAmbiguousLinkOnFirstRun(
            linkInfo: LinkInfo,
            commit: String,
            gitOperationManager: GitOperationManager
        ): Link {

            val commitToUse: String = gitOperationManager.checkValidityOfLinkPath(
                commit, linkInfo.linkPath
            ) ?: return NotSupportedLink(linkInfo = linkInfo, errorMessage = "Link containing an invalid link path")

            val link: Link = RelativeLinkToFile(linkInfo = linkInfo, commitSHA = commitToUse)
            cachedResults[linkInfo] = link
            return link
        }

        private fun processAmbiguousLinkNextRuns(
            linkInfo: LinkInfo,
            commit: String,
            cachedChanges: HashSet<FileChange>
        ): Link {

/*            val commitToUse = gitOperationManager.checkValidityOfLinkPath(commit, linkInfo.linkPath)

            if (commitToUse != null) return RelativeLinkToFile(linkInfo = linkInfo, commitSHA = commitToUse)

            println("CACHED CHANGES HERE: $cachedChanges")
            // check if this link has been updated by a previous run
            val cachedChange = cachedChanges.find { change ->
                change.changeType == "MOVED"
                        && change.afterPath != null
                        && linkInfo.getAfterPathToOriginalFormat(change.afterPath!!) == linkInfo.linkPath
            }
            if (cachedChange != null) {
                linkInfo.linkPath = linkInfo.getAfterPathToOriginalFormat(cachedChange.beforePath!!)
                println("Cached results: $cachedResults")

                if (cachedResults.containsKey(linkInfo)) {
                    val cachedLink = cachedResults[linkInfo]!!
                    cachedLink.beenCached = true
                    return cachedLink
                }
            }
            return NotSupportedLink(linkInfo = linkInfo, errorMessage = "Not supported")*/
            return RelativeLinkToFile(linkInfo = linkInfo, commitSHA = commit)
        }

        /**
         * Check whether a link is a valid link to file or directory.
         */
        private fun processAmbiguousLink(
            linkInfo: LinkInfo,
            commit: String,
            cachedChanges: HashSet<FileChange>
        ): Link {
/*            if (firstEverRun) {
                return processAmbiguousLinkOnFirstRun(linkInfo, commit, gitOperationManager)
            }*/
            return processAmbiguousLinkNextRuns(linkInfo, commit, cachedChanges)
        }

        /**
         * Factory method which creates the link according to its link path type.
         */
        fun createLink(
            linkInfo: LinkInfo,
            commitSHA: String?,
            currentProject: Project,
            cachedChanges: HashSet<FileChange>
        ): Link {
/*            if (cachedResults.containsKey(linkInfo)) {
                println("${linkInfo.linkPath} is CACHED")
                return cachedResults[linkInfo]!!
            }*/

/*            println("USING COMMIT SHA: $commitSHA")

            val gitOperationManager = GitOperationManager(project = currentProject)
            var firstEverRun = false
            val commit: String?
            if (commitSHA == null) firstEverRun = true
            try {
                commit = commitSHA ?: gitOperationManager.getStartCommit(linkInfo = linkInfo)
                if (commit == null) {


                    // TODO: this link has just been added. Just a simple check File.exists / Directory.exists()
                    // TODO: would suffice - sanity check for lines (whether the line/lines exist in the specified file)
                    val fullPath = "${currentProject.basePath}/${linkInfo.linkPath}"

                    if (File(fullPath).isDirectory) {
                        return NotSupportedLink(
                            linkInfo = linkInfo,
                            errorMessage = "This link was just added and directory exists!!"
                        )
                    }

                    if (File(fullPath).exists()) {
                        return NotSupportedLink(
                            linkInfo = linkInfo,
                            errorMessage = "This link was just added and file exists!!"
                        )
                    }

                    return NotSupportedLink(
                        linkInfo = linkInfo,
                        errorMessage = "Could not find start commit for this link - this link has just been added and does not reference anything valid."
                    )
                }
            } catch (e: VcsException) {
                val link = NotSupportedLink(linkInfo = linkInfo, errorMessage = e.message)
                // cachedResults[linkInfo] = link
                return link
            }*/

            val commit = "commit"

            // Web links matchers
            val webLinkToLinesMatcher = LinkPatterns.WebLinkToLines.pattern.matcher(linkInfo.linkPath)
            val webLinkToLineMatcher = LinkPatterns.WebLinkToLine.pattern.matcher(linkInfo.linkPath)
            val webLinkToFileMatcher = LinkPatterns.WebLinkToFile.pattern.matcher(linkInfo.linkPath)
            val webLinkToDirectoryMatcher = LinkPatterns.WebLinkToDirectory.pattern.matcher(linkInfo.linkPath)
            val genericWebLinksMatcher = LinkPatterns.GenericWebLinks.pattern.matcher(linkInfo.linkPath)

            // Relative links matchers
            val relativeLinkToLinesMatcher = LinkPatterns.RelativeLinkToLines.pattern.matcher(linkInfo.linkPath)
            val relativeLinkToLineMatcher = LinkPatterns.RelativeLinkToLine.pattern.matcher(linkInfo.linkPath)

            val link: Link
            when {
                webLinkToLineMatcher.matches() -> {
                    link = WebLinkToLine(linkInfo = linkInfo, commitSHA = commit)
                }
                webLinkToLinesMatcher.matches() -> {
                    link = WebLinkToLines(linkInfo = linkInfo, commitSHA = commit)
                }
                webLinkToFileMatcher.matches() -> {
                    link = WebLinkToFile(linkInfo = linkInfo, commitSHA = commit)
                }
                webLinkToDirectoryMatcher.matches() -> {
                    link = WebLinkToDirectory(linkInfo = linkInfo, commitSHA = commit)
                }
                genericWebLinksMatcher.matches() -> {
                    link =
                        NotSupportedLink(linkInfo = linkInfo, errorMessage = "This type of web link is not supported")
                }
                relativeLinkToLinesMatcher.matches() -> {
                    link = RelativeLinkToLines(linkInfo = linkInfo, commitSHA = commit)
                }
                relativeLinkToLineMatcher.matches() -> {
                    link = RelativeLinkToLine(linkInfo = linkInfo, commitSHA = commit)
                }
                else -> {
                    // Ambiguous link: have to see whether it's a path to a file or directory
                    link = processAmbiguousLink(
                        linkInfo,
                        commit,
                        cachedChanges
                    )
                }
            }
            cachedResults[linkInfo] = link
            return link
        }
    }
}