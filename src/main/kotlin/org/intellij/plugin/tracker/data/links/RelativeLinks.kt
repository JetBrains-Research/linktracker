package org.intellij.plugin.tracker.data.links

import org.intellij.plugin.tracker.data.changes.*
import org.intellij.plugin.tracker.services.ChangeTrackerService
import org.intellij.plugin.tracker.utils.LinkPatterns
import java.io.File
import java.util.regex.Matcher
import java.util.regex.Pattern


data class RelativeLinkToDirectory(
    override val linkInfo: LinkInfo,
    override val pattern: Pattern? = null
) : RelativeLink<CustomChange>(linkInfo, pattern) {
    override val lineReferenced: Int
        get() = -1

    override val referencedStartingLine: Int
        get() = -1

    override val referencedEndingLine: Int
        get() = -1

    override val referencedFileName: String
        get() = ""

    override fun visit(visitor: ChangeTrackerService): Change = visitor.getLocalDirectoryChanges(this)

    override fun updateLink(change: CustomChange, commitSHA: String?): String? = change.afterPathString

    override fun copyWithAfterPath(link: Link, afterPath: String): RelativeLinkToDirectory {
        val linkInfoCopy: LinkInfo = link.linkInfo.copy(linkPath = afterPath)
        return copy(linkInfo = linkInfoCopy)
    }

    override fun markdownFileMoved(afterPath: String): Boolean = false
}

data class RelativeLinkToFile(
    override val linkInfo: LinkInfo,
    override val pattern: Pattern? = null
) : RelativeLink<CustomChange>(linkInfo, pattern) {
    override val lineReferenced: Int
        get() = -1
    override val referencedFileName: String
        get() {
            val file = File(linkInfo.linkPath)
            return file.name
        }

    override val referencedStartingLine: Int
        get() = -1

    override val referencedEndingLine: Int
        get() = -1

    override fun visit(visitor: ChangeTrackerService): Change = visitor.getLocalFileChanges(this)

    override fun updateLink(change: CustomChange, commitSHA: String?): String? =
        linkInfo.getAfterPathToOriginalFormat(change.afterPathString)

    override fun copyWithAfterPath(link: Link, afterPath: String): RelativeLinkToFile {
        val linkInfoCopy: LinkInfo = link.linkInfo.copy(linkPath = afterPath)
        return copy(linkInfo = linkInfoCopy)
    }
}


data class RelativeLinkToLine(
    override val linkInfo: LinkInfo,
    override val pattern: Pattern = LinkPatterns.RelativeLinkToLine.pattern
) : RelativeLink<LineChange>(linkInfo, pattern) {
    override val lineReferenced: Int
        get() = matcher.group(1).toInt()
    override val referencedFileName: String
        get() {
            val file = File(linkInfo.linkPath)
            return file.name.replace("#L${matcher.group(1)}", "")
        }

    override val referencedStartingLine: Int
        get() = -1
    override val referencedEndingLine: Int
        get() = -1

    override val path: String
        get() {
            if (matcher.matches())
                return linkInfo.linkPath.replace("#L$lineReferenced", "")
            return linkInfo.linkPath
        }

    override fun visit(visitor: ChangeTrackerService): Change = visitor.getLocalLineChanges(this)

    override fun copyWithAfterPath(link: Link, afterPath: String): RelativeLinkToLine {
        val linkInfoCopy: LinkInfo = link.linkInfo.copy(linkPath = afterPath)
        return copy(linkInfo = linkInfoCopy)
    }

    override fun updateLink(change: LineChange, commitSHA: String?): String? {
        TODO("not implemented")
    }
}

data class RelativeLinkToLines(
    override val linkInfo: LinkInfo,
    override val pattern: Pattern = LinkPatterns.RelativeLinkToLines.pattern
) : RelativeLink<LinesChange>(linkInfo, pattern) {
    override val lineReferenced: Int
        get() = -1
    override val referencedFileName: String
        get() {
            val file = File(linkInfo.linkPath)
            return file.name.replace("#L${matcher.group(1)}-L${matcher.group(2)}", "")
        }
    override val referencedStartingLine: Int
        get() = matcher.group(1).toInt()
    override val referencedEndingLine: Int
        get() = matcher.group(2).toInt()

    override val path: String
        get() {
            if (matcher.matches())
                return linkInfo.linkPath.replace(
                    "#L$referencedStartingLine-L$referencedEndingLine", ""
                )
            return linkInfo.linkPath
        }

    override fun visit(visitor: ChangeTrackerService): Change = visitor.getLocalLinesChanges(this)

    override fun copyWithAfterPath(link: Link, afterPath: String): RelativeLinkToLines {
        val linkInfoCopy: LinkInfo = link.linkInfo.copy(linkPath = afterPath)
        return copy(linkInfo = linkInfoCopy)
    }

    override fun updateLink(change: LinesChange, commitSHA: String?): String? {
        TODO("not implemented")
    }
}

fun checkRelativeLink(link: String): String {
    return checkSingleDot(checkDoubleDots(link))
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
            }
        }
    }
    return result
}

fun checkSingleDot(link: String): String {
    var result = link
    while (result.contains("/.")) {
        val matcher: Matcher = LinkPatterns.RelativeLinkWithSingleDot.pattern.matcher(result)
        if (matcher.matches()) {
            val firstPart = matcher.group(2)
            val secondPart = matcher.group(3)
            result = "$firstPart/$secondPart"
        } else {
            val endMatcher: Matcher = LinkPatterns.RelativeLinkWithSingleDotAtEnd.pattern.matcher(result)
            if (endMatcher.matches()) {
                result = endMatcher.group(2)
            }
        }
    }
    return result
}