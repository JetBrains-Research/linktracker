package org.intellij.plugin.tracker.utils


import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import org.intellij.plugin.tracker.data.links.*
import org.intellij.plugin.tracker.utils.GitOperationManager
import org.intellij.plugin.tracker.utils.LinkPatterns
import java.util.regex.Pattern

class LinkFactory {

    companion object {

        /**
         * Function which creates the link according to its type.
         */
        fun createLink(potentialLink: PotentialLink, commitSHA: String?, currentProject: Project): Link {

            val linkText = potentialLink.linkText
            val linkPath = potentialLink.linkPath
            val proveniencePath = potentialLink.proveniencePath
            val foundAtLineNumber = potentialLink.foundAtLineNumber

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
            var commit  = ""
            try {
                commit =
                    commitSHA ?: gitOperationManager.getStartCommit(foundAtLineNumber, proveniencePath, linkText, linkPath)
            } catch (e: VcsException) {
                return NotSupportedLink(
                    linkText = linkText,
                    linkPath = linkPath,
                    proveniencePath = proveniencePath,
                    foundAtLineNumber = foundAtLineNumber,
                    errorMessage = e.message
                )
            }

            when {
                webLinkToLinesMatcher.matches() -> {
                    return WebLinkToLines(
                        linkText = linkText,
                        linkPath = linkPath,
                        proveniencePath = proveniencePath,
                        foundAtLineNumber = foundAtLineNumber,
                        matcher = webLinkToLinesMatcher,
                        commitSHA = commit
                    )
                }
                webLinkToLineMatcher.matches() -> {
                    return WebLinkToLine(
                        linkText = linkText,
                        linkPath = linkPath,
                        proveniencePath = proveniencePath,
                        foundAtLineNumber = foundAtLineNumber,
                        matcher = webLinkToLineMatcher,
                        commitSHA = commit
                    )
                }
                webLinkToFileMatcher.matches() -> {
                    return WebLinkToFile(
                        linkText = linkText,
                        linkPath = linkPath,
                        proveniencePath = proveniencePath,
                        foundAtLineNumber = foundAtLineNumber,
                        matcher = webLinkToFileMatcher,
                        commitSHA = commit
                    )
                }
                webLinkToDirectoryMatcher.matches() -> {
                    return WebLinkToDirectory(
                        linkText = linkText,
                        linkPath = linkPath,
                        proveniencePath = proveniencePath,
                        foundAtLineNumber = foundAtLineNumber,
                        matcher = webLinkToDirectoryMatcher,
                        commitSHA = commit
                    )
                }
                genericWebLinksMatcher.matches() -> {
                    return NotSupportedLink(
                        linkText = linkText,
                        linkPath = linkPath,
                        proveniencePath = proveniencePath,
                        foundAtLineNumber = foundAtLineNumber,
                        errorMessage = "This type of web link is not supported"
                    )
                }
                relativeLinkToLinesMatcher.matches() -> {
                    return RelativeLinkToLines(
                        linkText = linkText,
                        linkPath = linkPath,
                        proveniencePath = proveniencePath,
                        foundAtLineNumber = foundAtLineNumber,
                        matcher = relativeLinkToLinesMatcher,
                        commitSHA = commit
                    )
                }
                relativeLinkToLineMatcher.matches() -> {
                    return RelativeLinkToLine(
                        linkText = linkText,
                        linkPath = linkPath,
                        proveniencePath = proveniencePath,
                        foundAtLineNumber = foundAtLineNumber,
                        matcher = relativeLinkToLineMatcher,
                        commitSHA = commit
                    )
                }
                else -> {
                    // Ambiguous link: have to see whether it's a path to a file or directory

                    val linkValidity =
                        gitOperationManager.checkValidityOfLinkPathAtCommit(commit, linkPath)

                    if (!linkValidity) {
                        return NotSupportedLink(
                            linkText = linkText,
                            linkPath = linkPath,
                            proveniencePath = proveniencePath,
                            foundAtLineNumber = foundAtLineNumber,
                            errorMessage = "Link was not referencing a valid element when it was created"
                        )
                    }

                    val fileList = gitOperationManager.getListOfFiles(commit)

                    if (linkPath in fileList) {
                        return RelativeLinkToFile(
                            linkText = linkText,
                            linkPath = linkPath,
                            proveniencePath = proveniencePath,
                            foundAtLineNumber = foundAtLineNumber,
                            commitSHA = commit
                        )
                    }
                    val directoryList = gitOperationManager.getListOfDirectories(commit)
                    if (linkPath in directoryList) {
                        return RelativeLinkToDirectory(
                            linkText = linkText,
                            linkPath = linkPath,
                            proveniencePath = proveniencePath,
                            foundAtLineNumber = foundAtLineNumber,
                            commitSHA = commit
                        )
                    }
                    return NotSupportedLink(
                        linkText = linkText,
                        linkPath = linkPath,
                        proveniencePath = proveniencePath,
                        foundAtLineNumber = foundAtLineNumber,
                        errorMessage = "Not a valid link"
                    )
                }
            }
        }
    }
}