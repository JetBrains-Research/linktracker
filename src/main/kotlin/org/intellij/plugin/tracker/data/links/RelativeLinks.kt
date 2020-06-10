package org.intellij.plugin.tracker.data.links

import java.io.File
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.intellij.plugin.tracker.data.changes.Change
import org.intellij.plugin.tracker.data.changes.CustomChange
import org.intellij.plugin.tracker.data.changes.LineChange
import org.intellij.plugin.tracker.data.changes.LinesChange
import org.intellij.plugin.tracker.services.ChangeTrackerService
import org.intellij.plugin.tracker.utils.LinkPatterns


/**
 * Data class that corresponds to a relative link to directory
 */
data class RelativeLinkToDirectory(
    override val linkInfo: LinkInfo,
    override val pattern: Pattern? = null
) : RelativeLink<CustomChange>(linkInfo, pattern) {

    /**
     * No lines referenced
     */
    override val lineReferenced: Int
        get() = -1

    /**
     * No lines referenced
     */
    override val referencedStartingLine: Int
        get() = -1


    /**
     * No lines referenced
     */
    override val referencedEndingLine: Int
        get() = -1

    /**
     * No file referenced
     */
    override val referencedFileName: String
        get() = ""

    /**
     * Call the right method in the implementation of ChangeTrackerService
     */
    override fun visit(visitor: ChangeTrackerService): Change = visitor.getLocalDirectoryChanges(this)

    /**
     * Generate a new equivalent link based on the passed in change
     */
    override fun updateLink(change: CustomChange, commitSHA: String?): String? = change.afterPathString

    /**
     * Deep copy this link and return the copied link with a new after path
     */
    override fun copyWithAfterPath(link: Link, afterPath: String): RelativeLinkToDirectory {
        val linkInfoCopy: LinkInfo = link.linkInfo.copy(linkPath = afterPath)
        return copy(linkInfo = linkInfoCopy)
    }

    override fun markdownFileMoved(afterPath: String): Boolean = false
}


/**
 * Data class that corresponds to a relative link to file
 */
data class RelativeLinkToFile(
    override val linkInfo: LinkInfo,
    override val pattern: Pattern? = null
) : RelativeLink<CustomChange>(linkInfo, pattern) {

    /**
     * No lines referenced
     */
    override val lineReferenced: Int
        get() = -1

    /**
     * Get the referenced file name only
     */
    override val referencedFileName: String
        get() {
            val file = File(relativePath)
            return file.name
        }

    /**
     * Get the path
     */
    override val path: String
        get() = relativePath

    /**
     * No lines referenced
     */
    override val referencedStartingLine: Int
        get() = -1

    /**
     * No lines referenced
     */
    override val referencedEndingLine: Int
        get() = -1

    /**
     * Call the right method in the implementation of ChangeTrackerService
     */
    override fun visit(visitor: ChangeTrackerService): Change = visitor.getLocalFileChanges(this)

    /**
     * Generate a new equivalent link based on the passed in change
     */
    override fun updateLink(change: CustomChange, commitSHA: String?): String? =
        linkInfo.getAfterPathToOriginalFormat(change.afterPathString)

    /**
     * Deep copy this link and return the copied link with a new after path
     */
    override fun copyWithAfterPath(link: Link, afterPath: String): RelativeLinkToFile {
        val linkInfoCopy: LinkInfo = link.linkInfo.copy(linkPath = afterPath)
        return copy(linkInfo = linkInfoCopy)
    }
}


/**
 * Data class that corresponds to a relative link to line
 */
data class RelativeLinkToLine(
    override val linkInfo: LinkInfo,
    override val pattern: Pattern = LinkPatterns.RelativeLinkToLine.pattern
) : RelativeLink<LineChange>(linkInfo, pattern) {

    /**
     * Get the referenced line part from the path
     */
    override val lineReferenced: Int
        get() = matcher.group(1).toInt()

    /**
     * Get the referenced file name part from the path
     */
    override val referencedFileName: String
        get() {
            val file = File(relativePath)
            return file.name.replace("#L${matcher.group(1)}", "")
        }

    /**
     * No multiple lines referenced
     */
    override val referencedStartingLine: Int
        get() = -1

    /**
     * No multiple lines referenced
     */
    override val referencedEndingLine: Int
        get() = -1

    /**
     * Get the path without the line referencing part
     */
    override val path: String
        get() {
            if (matcher.matches())
                return relativePath.replace("#L$lineReferenced", "")
            return relativePath
        }

    /**
     * Call the right method in the implementation of ChangeTrackerService
     */
    override fun visit(visitor: ChangeTrackerService): Change = visitor.getLocalLineChanges(this)


    /**
     * Deep copy this link and return the copied link with a new after path
     */
    override fun copyWithAfterPath(link: Link, afterPath: String): RelativeLinkToLine {
        val linkInfoCopy: LinkInfo = link.linkInfo.copy(linkPath = afterPath)
        return copy(linkInfo = linkInfoCopy)
    }

    /**
     * Generate a new equivalent link based on the passed in change
     */
    override fun updateLink(change: LineChange, commitSHA: String?): String? = change.afterPath[0]
}


/**
 * Data class that corresponds to a relative link to multiple lines
 */
data class RelativeLinkToLines(
    override val linkInfo: LinkInfo,
    override val pattern: Pattern = LinkPatterns.RelativeLinkToLines.pattern
) : RelativeLink<LinesChange>(linkInfo, pattern) {

    /**
     * No single line referenced
     */
    override val lineReferenced: Int
        get() = -1

    /**
     * Get the referenced file name part from the path, without the line range specifications
     */
    override val referencedFileName: String
        get() {
            val file = File(relativePath)
            return file.name.replace("#L${matcher.group(1)}-L${matcher.group(2)}", "")
        }

    /**
     * Get the first line referenced (inclusive) part from the path
     */
    override val referencedStartingLine: Int
        get() = matcher.group(1).toInt()

    /**
     * Get the last line referenced part (inclusive) from the path
     */
    override val referencedEndingLine: Int
        get() = matcher.group(2).toInt()

    /**
     * Get the full path without the line-range specifications
     */
    override val path: String
        get() {
            if (matcher.matches())
                return relativePath.replace(
                    "#L$referencedStartingLine-L$referencedEndingLine", ""
                )
            return relativePath
        }

    /**
     * Call the right method in the implementation of ChangeTrackerService
     */
    override fun visit(visitor: ChangeTrackerService): Change = visitor.getLocalLinesChanges(this)

    /**
     * Deep copy this link and return the copied link with a new after path
     */
    override fun copyWithAfterPath(link: Link, afterPath: String): RelativeLinkToLines {
        val linkInfoCopy: LinkInfo = link.linkInfo.copy(linkPath = afterPath)
        return copy(linkInfo = linkInfoCopy)
    }

    /**
     * Generate a new equivalent link based on the passed in change
     */
    override fun updateLink(change: LinesChange, commitSHA: String?): String? = change.afterPath[0]
}

fun checkRelativeLink(linkPath: String, filePath: String): String {
    val link = filePath.replace(filePath.split("/").last(), "") + linkPath
    return checkDoubleDots(checkSingleDot(link))
}

fun checkDoubleDots(link: String): String {
    var result = link
    while (result.contains("..")) {
        val matcher: Matcher = LinkPatterns.RelativeLinkWithDoubleDots.pattern.matcher(result)
        if (matcher.matches()) {
            val firstPart = matcher.group(2)
            val secondPart = matcher.group(6)
            result = if (firstPart == null) {
                secondPart
            } else {
                firstPart + secondPart
            }
        } else {
            val endMatcher: Matcher = LinkPatterns.RelativeLinkWithDoubleDotsAtEnd.pattern.matcher(result)
            if (endMatcher.matches()) {
                result = endMatcher.group(2)
            } else {
                val startMatcher: Matcher = LinkPatterns.RelativeLinkWithDoubleDotsAtStart.pattern.matcher(result)
                if (startMatcher.matches()) {
                    result = startMatcher.group(2)
                } else {
                    return result
                }
            }
        }
    }
    return result
}

fun checkSingleDot(link: String): String {
    var result = link
    while (result.contains("/.") || result.contains("./")) {
        val matcher: Matcher = LinkPatterns.RelativeLinkWithSingleDot.pattern.matcher(result)
        result = if (matcher.matches()) {
            val firstPart = matcher.group(2)
            val secondPart = matcher.group(3)
            "$firstPart/$secondPart"
        } else {
            val endMatcher: Matcher = LinkPatterns.RelativeLinkWithSingleDotAtEnd.pattern.matcher(result)
            if (endMatcher.matches()) {
                endMatcher.group(2)
            } else {
                val startMatcher: Matcher = LinkPatterns.RelativeLinkWithSingleDotAtStart.pattern.matcher(result)
                if (startMatcher.matches()) {
                    startMatcher.group(2)
                } else {
                    return result
                }
            }
        }
    }
    return result
}
