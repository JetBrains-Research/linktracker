package org.intellij.plugin.tracker.utils


import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import org.intellij.plugin.tracker.data.links.*
import java.util.regex.Pattern

class LinkFactory {

    companion object {

        /**
         * Cache each link that is being identified
         */
        val cachedResults: HashMap<String, Link> = hashMapOf<String, Link>()

        /**
         * Function which creates the link according to its type.
         */
        fun createLink(potentialLink: PotentialLink, commitSHA: String?, currentProject: Project): Link {

            val linkText = potentialLink.linkText
            val linkPath = potentialLink.linkPath
            val proveniencePath = potentialLink.proveniencePath
            val foundAtLineNumber = potentialLink.foundAtLineNumber

            if (cachedResults.containsKey(linkPath))
                return cachedResults.get(linkPath)!!

            // Web links patterns
            val webLinkToLinesMatcher = Pattern.compile(LinkPatterns.WebLinkToLines.patternString).matcher(linkPath)
            val webLinkToLineMatcher = Pattern.compile(LinkPatterns.WebLinkToLine.patternString).matcher(linkPath)
            val webLinkToFileMatcher = Pattern.compile(LinkPatterns.WebLinkToFile.patternString).matcher(linkPath)
            val webLinkToDirectoryMatcher =
                Pattern.compile(LinkPatterns.WebLinkToDirectory.patternString).matcher(linkPath)
            val genericWebLinksMatcher = Pattern.compile(LinkPatterns.GenericWebLinks.patternString).matcher(linkPath)

            // Relative links patterns
            val relativeLinkToLinesMatcher =
                Pattern.compile(LinkPatterns.RelativeLinkToLines.patternString).matcher(linkPath)
            val relativeLinkToLineMatcher =
                Pattern.compile(LinkPatterns.RelativeLinkToLine.patternString).matcher(linkPath)

            val gitOperationManager = GitOperationManager(project = currentProject)
            var commit = ""
            try {
                commit =
                    commitSHA ?: gitOperationManager.getStartCommit(
                        foundAtLineNumber,
                        proveniencePath,
                        linkText,
                        linkPath
                    )
            } catch (e: VcsException) {
                val link =
                    NotSupportedLink(
                        linkText = linkText,
                        linkPath = linkPath,
                        proveniencePath = proveniencePath,
                        foundAtLineNumber = foundAtLineNumber,
                        errorMessage = e.message
                    )
                cachedResults.put(linkPath, link)
                return link
            }

            when {
                webLinkToLinesMatcher.matches() -> {
                    val link =
                        WebLinkToLines(
                            linkText = linkText,
                            linkPath = linkPath,
                            proveniencePath = proveniencePath,
                            foundAtLineNumber = foundAtLineNumber,
                            matcher = webLinkToLinesMatcher,
                            commitSHA = commit
                        )
                    cachedResults.put(linkPath, link)
                    return link
                }
                webLinkToLineMatcher.matches() -> {
                    val link =
                        WebLinkToLine(
                            linkText = linkText,
                            linkPath = linkPath,
                            proveniencePath = proveniencePath,
                            foundAtLineNumber = foundAtLineNumber,
                            matcher = webLinkToLineMatcher,
                            commitSHA = commit
                        )
                    cachedResults.put(linkPath, link)
                    return link
                }
                webLinkToFileMatcher.matches() -> {
                    val link =
                        WebLinkToFile(
                            linkText = linkText,
                            linkPath = linkPath,
                            proveniencePath = proveniencePath,
                            foundAtLineNumber = foundAtLineNumber,
                            matcher = webLinkToFileMatcher,
                            commitSHA = commit
                        )
                    cachedResults.put(linkPath, link)
                    return link
                }
                webLinkToDirectoryMatcher.matches() -> {
                    val link =
                        WebLinkToDirectory(
                            linkText = linkText,
                            linkPath = linkPath,
                            proveniencePath = proveniencePath,
                            foundAtLineNumber = foundAtLineNumber,
                            matcher = webLinkToDirectoryMatcher,
                            commitSHA = commit
                        )
                    cachedResults.put(linkPath, link)
                    return link
                }
                genericWebLinksMatcher.matches() -> {
                    val link =
                        NotSupportedLink(
                            linkText = linkText,
                            linkPath = linkPath,
                            proveniencePath = proveniencePath,
                            foundAtLineNumber = foundAtLineNumber,
                            errorMessage = "This type of web link is not supported"
                        )
                    cachedResults.put(linkPath, link)
                    return link
                }
                relativeLinkToLinesMatcher.matches() -> {
                    val link =
                        RelativeLinkToLines(
                            linkText = linkText,
                            linkPath = linkPath,
                            proveniencePath = proveniencePath,
                            foundAtLineNumber = foundAtLineNumber,
                            matcher = relativeLinkToLinesMatcher,
                            commitSHA = commit
                        )
                    cachedResults.put(linkPath, link)
                    return link
                }
                relativeLinkToLineMatcher.matches() -> {
                    val link =
                        RelativeLinkToLine(
                            linkText = linkText,
                            linkPath = linkPath,
                            proveniencePath = proveniencePath,
                            foundAtLineNumber = foundAtLineNumber,
                            matcher = relativeLinkToLineMatcher,
                            commitSHA = commit
                        )
                    cachedResults.put(linkPath, link)
                    return link
                }
                else -> {
                    // Ambiguous link: have to see whether it's a path to a file or directory

                    val linkValidity =
                        gitOperationManager.checkValidityOfLinkPathAtCommit(commit, linkPath)

                    if (!linkValidity) {
                        val link =
                            NotSupportedLink(
                                linkText = linkText,
                                linkPath = linkPath,
                                proveniencePath = proveniencePath,
                                foundAtLineNumber = foundAtLineNumber,
                                commitSHA = commit,
                                errorMessage = "Link was not referencing a valid element when it was created"
                            )
                        cachedResults.put(linkPath, link)
                        return link
                    }

                    val fileList = gitOperationManager.getListOfFiles(commit)

                    if (linkPath in fileList) {
                        val link =
                            RelativeLinkToFile(
                                linkText = linkText,
                                linkPath = linkPath,
                                proveniencePath = proveniencePath,
                                foundAtLineNumber = foundAtLineNumber,
                                commitSHA = commit
                            )
                        cachedResults.put(linkPath, link)
                        return link
                    }
                    val directoryList = gitOperationManager.getListOfDirectories(commit)
                    if (linkPath in directoryList) {
                        val link =
                            RelativeLinkToDirectory(
                                linkText = linkText,
                                linkPath = linkPath,
                                proveniencePath = proveniencePath,
                                foundAtLineNumber = foundAtLineNumber,
                                commitSHA = commit
                            )
                        cachedResults.put(linkPath, link)
                        return link
                    }
                    val link =
                        NotSupportedLink(
                            linkText = linkText,
                            linkPath = linkPath,
                            proveniencePath = proveniencePath,
                            foundAtLineNumber = foundAtLineNumber,
                            errorMessage = "Not a valid link"
                        )
                    cachedResults.put(linkPath, link)
                    return link
                }
            }
        }
    }
}