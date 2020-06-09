package org.intellij.plugin.tracker.utils

import java.util.regex.Matcher
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.data.links.LinkInfo
import org.intellij.plugin.tracker.data.links.NotSupportedLink
import org.intellij.plugin.tracker.data.links.RelativeLinkToDirectory
import org.intellij.plugin.tracker.data.links.RelativeLinkToFile
import org.intellij.plugin.tracker.data.links.RelativeLinkToLine
import org.intellij.plugin.tracker.data.links.RelativeLinkToLines
import org.intellij.plugin.tracker.data.links.WebLinkToDirectory
import org.intellij.plugin.tracker.data.links.WebLinkToFile
import org.intellij.plugin.tracker.data.links.WebLinkToLine
import org.intellij.plugin.tracker.data.links.WebLinkToLines

class LinkFactory {

    companion object {

        /**
         * Factory method which creates the link according to its link path type.
         */
        fun createLink(linkInfo: LinkInfo, commitSHA: String? = null): Link {
            // Web links matchers
            val webLinkToLinesMatcher: Matcher = LinkPatterns.WebLinkToLines.pattern.matcher(linkInfo.linkPath)
            val webLinkToLineMatcher: Matcher = LinkPatterns.WebLinkToLine.pattern.matcher(linkInfo.linkPath)
            val webLinkToFileMatcher: Matcher = LinkPatterns.WebLinkToFile.pattern.matcher(linkInfo.linkPath)
            val webLinkToDirectoryMatcher: Matcher = LinkPatterns.WebLinkToDirectory.pattern.matcher(linkInfo.linkPath)
            val genericWebLinksMatcher: Matcher = LinkPatterns.GenericWebLinks.pattern.matcher(linkInfo.linkPath)

            // Relative links matchers
            val relativeLinkToLinesMatcher: Matcher = LinkPatterns.RelativeLinkToLines.pattern.matcher(linkInfo.linkPath)
            val relativeLinkToLineMatcher: Matcher = LinkPatterns.RelativeLinkToLine.pattern.matcher(linkInfo.linkPath)

            val link: Link
            when {
                webLinkToLineMatcher.matches() -> {
                    link = WebLinkToLine(linkInfo = linkInfo)
                    if (link.lineReferenced > 0) return link
                    return NotSupportedLink(linkInfo = linkInfo, errorMessage = "Referenced line cannot be negative/zero")
                }
                webLinkToLinesMatcher.matches() -> {
                    val startLine = WebLinkToLines(linkInfo = linkInfo).referencedStartingLine
                    val endLine = WebLinkToLines(linkInfo = linkInfo).referencedEndingLine
                    link = if (startLine > endLine) {
                        NotSupportedLink(linkInfo = linkInfo, errorMessage = "Invalid start and end lines")
                    } else {
                        WebLinkToLines(linkInfo = linkInfo)
                    }
                }
                webLinkToFileMatcher.matches() -> {
                    link = WebLinkToFile(linkInfo = linkInfo)
                }
                webLinkToDirectoryMatcher.matches() -> {
                    link = WebLinkToDirectory(linkInfo = linkInfo)
                }
                genericWebLinksMatcher.matches() -> {
                    link =
                        NotSupportedLink(linkInfo = linkInfo, errorMessage = "This type of web link is not supported")
                }
                relativeLinkToLinesMatcher.matches() -> {
                    val startLine = RelativeLinkToLines(linkInfo = linkInfo).referencedStartingLine
                    val endLine = RelativeLinkToLines(linkInfo = linkInfo).referencedEndingLine
                    link = if (startLine > endLine) {
                        NotSupportedLink(linkInfo = linkInfo, errorMessage = "Invalid start and end lines")
                    } else {
                        RelativeLinkToLines(linkInfo = linkInfo)
                    }
                }
                relativeLinkToLineMatcher.matches() -> {
                    link = RelativeLinkToLine(linkInfo = linkInfo)
                    if (link.lineReferenced > 0) return link
                    return NotSupportedLink(linkInfo = linkInfo, errorMessage = "Referenced line cannot be negative/zero")
                }
                else -> {
                    // Ambiguous link: have to see whether it's a path to a file or directory
                    // not the best way to check for file/directory: directories can also have . in their names
                    // TODO: can be optimized
                    if (linkInfo.linkPath.lastIndexOf(".") == -1) {
                        return RelativeLinkToDirectory(linkInfo = linkInfo)
                    }
                    link = RelativeLinkToFile(linkInfo = linkInfo)
                }
            }
            return link
        }
    }
}
