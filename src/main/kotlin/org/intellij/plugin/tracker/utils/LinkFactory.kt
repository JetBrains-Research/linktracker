package org.intellij.plugin.tracker.utils


import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import org.intellij.plugin.tracker.data.changes.LinkChange
import org.intellij.plugin.tracker.data.links.*

class LinkFactory {

    companion object {

        /**
         * Cache each link that is being identified for faster identification upon consecutive runs
         */
        private val cachedResults: HashMap<LinkInfo, Link> = hashMapOf()

        /**
         * Check whether a link is a valid link to file or directory.
         */
        private fun processAmbiguousLink(
            linkInfo: LinkInfo,
            commit: String,
            gitOperationManager: GitOperationManager
        ): Link {
            val projectRelativeLinkPath = linkInfo.getMarkdownDirectoryRelativeLinkPath()
            val link: Link

            val fileList: List<String> = gitOperationManager.getListOfFiles(commit)

            fileList.map { x -> x.trim() }
            if (fileList.any { x -> x == projectRelativeLinkPath }) {
                link = RelativeLinkToFile(linkInfo = linkInfo, commitSHA = commit)
                cachedResults[linkInfo] = link
                return link
            }

            val directoryList = gitOperationManager.getListOfDirectories(commit)

            directoryList.map { x -> x.trim() }
            if (directoryList.any { x -> x == projectRelativeLinkPath }) {
                link = RelativeLinkToDirectory(linkInfo = linkInfo, commitSHA = commit)
            } else {
                link = NotSupportedLink(linkInfo = linkInfo, errorMessage = "Not a valid link")
            }

            cachedResults[linkInfo] = link
            return link
        }

        /**
         * Factory method which creates the link according to its link path type.
         */
        fun createLink(
            linkInfo: LinkInfo,
            commitSHA: String?,
            currentProject: Project,
            cachedChanges: HashSet<LinkChange>
        ): Link {
            if (cachedResults.containsKey(linkInfo)) {
                return cachedResults[linkInfo]!!
            }

            val gitOperationManager = GitOperationManager(project = currentProject)
            val commit: String?
            try {
                commit = commitSHA ?: gitOperationManager.getStartCommit(linkInfo = linkInfo)
                if (commit == null) {

                    // check if this link has been updated by a previous run
                    val cachedChange = cachedChanges.find { change -> change.changeType == "MOVED"
                            && change.afterPath != null
                            && linkInfo.getAfterPathToOriginalFormat(change.afterPath!!) == linkInfo.linkPath }
                    if (cachedChange != null) {
                        linkInfo.linkPath = linkInfo.getAfterPathToOriginalFormat(cachedChange.beforePath!!)
                        val cachedLink = cachedResults[linkInfo]!!
                        cachedLink.beenCached = true
                        return cachedResults[linkInfo]!!
                    }

                    return NotSupportedLink(linkInfo = linkInfo, errorMessage = "Could not find start commit for this link - this link has just been added")
                }
            } catch (e: VcsException) {
                val link = NotSupportedLink(linkInfo = linkInfo, errorMessage = e.message)
                cachedResults[linkInfo] = link
                return link
            }

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
                            gitOperationManager
                    )
                }
            }
            cachedResults[linkInfo] = link
            return link
        }
    }
}