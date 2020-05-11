package org.intellij.plugin.tracker.utils


import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import org.intellij.plugin.tracker.data.links.*

class LinkFactory {

    companion object {

        /**
         * Cache each link that is being identified for faster identification upon consecutive runs
         */
        private val cachedResults: HashMap<Pair<String, Int>, Link> = hashMapOf<Pair<String, Int>, Link>()

        /**
         * Check whether a link is a valid link to file or directory.
         */
        private fun processAmbiguousLink(
            linkInfo: LinkInfo,
            commit: String,
            gitOperationManager: GitOperationManager
        ): Link {
            val key = Pair(linkInfo.linkPath, linkInfo.foundAtLineNumber)
            val projectRelativeLinkPath = linkInfo.getProjectRelativePath()
            val linkValidity =
                gitOperationManager.checkValidityOfLinkPathAtCommit(commit, projectRelativeLinkPath)

            val link: Link

            if (!linkValidity) {
                link = NotSupportedLink(
                    linkInfo = linkInfo,
                    commitSHA = commit,
                    errorMessage = "Link was not referencing a valid element when it was created"
                )
                cachedResults[key] = link
                return link
            }

            val fileList = gitOperationManager.getListOfFiles(commit)
            if (projectRelativeLinkPath in fileList) {
                link = RelativeLinkToFile(linkInfo = linkInfo, commitSHA = commit)
                cachedResults[key] = link
                return link
            }

            val directoryList = gitOperationManager.getListOfDirectories(commit)
            if (projectRelativeLinkPath in directoryList) {
                link = RelativeLinkToDirectory(linkInfo = linkInfo, commitSHA = commit)
            } else {
                link = NotSupportedLink(linkInfo = linkInfo, errorMessage = "Not a valid link")
            }
            cachedResults[key] = link
            return link
        }

        /**
         * Factory method which creates the link according to its link path type.
         */
        fun createLink(linkInfo: LinkInfo, commitSHA: String?, currentProject: Project): Link {
            val gitOperationManager = GitOperationManager(project = currentProject)
            val key = Pair(linkInfo.linkPath, linkInfo.foundAtLineNumber)

            if (cachedResults.containsKey(key))
                return cachedResults[key]!!

            val commit: String
            try {
                commit = commitSHA ?: gitOperationManager.getStartCommit(linkInfo = linkInfo)
            } catch (e: VcsException) {
                val link = NotSupportedLink(linkInfo = linkInfo, errorMessage = e.message)
                cachedResults[key] = link
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
                    link = processAmbiguousLink(linkInfo, commit, gitOperationManager)
                }
            }
            cachedResults[key] = link
            return link
        }
    }
}