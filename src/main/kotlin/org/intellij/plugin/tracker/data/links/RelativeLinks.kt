package org.intellij.plugin.tracker.data.links

import java.io.File
import java.util.Stack
import java.util.regex.Pattern
import org.intellij.plugin.tracker.data.changes.Change
import org.intellij.plugin.tracker.data.changes.CustomChange
import org.intellij.plugin.tracker.data.changes.LineChange
import org.intellij.plugin.tracker.data.changes.LinesChange
import org.intellij.plugin.tracker.services.ChangeTrackerService
import org.intellij.plugin.tracker.utils.LinkPatterns

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

    override val path: String
        get() = relativePath

    override fun visit(visitor: ChangeTrackerService): Change = visitor.getLocalDirectoryChanges(this)

    override fun updateLink(change: CustomChange, commitSHA: String?): String? {
        if (change.afterPathString.endsWith('/')) {
            return change.afterPathString.removeSuffix("/")
        }
        return change.afterPathString
    }

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
            val file = File(relativePath)
            return file.name
        }

    override val path: String
        get() = relativePath

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
            val file = File(relativePath)
            return file.name.replace("#L${matcher.group(1)}", "")
        }

    override val referencedStartingLine: Int
        get() = -1
    override val referencedEndingLine: Int
        get() = -1

    override val path: String
        get() {
            if (matcher.matches())
                return relativePath.replace("#L$lineReferenced", "")
            return relativePath
        }

    override fun visit(visitor: ChangeTrackerService): Change = visitor.getLocalLineChanges(this)

    override fun copyWithAfterPath(link: Link, afterPath: String): RelativeLinkToLine {
        val linkInfoCopy: LinkInfo = link.linkInfo.copy(linkPath = afterPath)
        return copy(linkInfo = linkInfoCopy)
    }

    override fun updateLink(change: LineChange, commitSHA: String?): String? = change.afterPath[0]
}

data class RelativeLinkToLines(
    override val linkInfo: LinkInfo,
    override val pattern: Pattern = LinkPatterns.RelativeLinkToLines.pattern
) : RelativeLink<LinesChange>(linkInfo, pattern) {
    override val lineReferenced: Int
        get() = -1
    override val referencedFileName: String
        get() {
            val file = File(relativePath)
            return file.name.replace("#L${matcher.group(1)}-L${matcher.group(2)}", "")
        }
    override val referencedStartingLine: Int
        get() = matcher.group(1).toInt()
    override val referencedEndingLine: Int
        get() = matcher.group(2).toInt()

    override val path: String
        get() {
            if (matcher.matches())
                return relativePath.replace(
                    "#L$referencedStartingLine-L$referencedEndingLine", ""
                )
            return relativePath
        }

    override fun visit(visitor: ChangeTrackerService): Change = visitor.getLocalLinesChanges(this)

    override fun copyWithAfterPath(link: Link, afterPath: String): RelativeLinkToLines {
        val linkInfoCopy: LinkInfo = link.linkInfo.copy(linkPath = afterPath)
        return copy(linkInfo = linkInfoCopy)
    }

    override fun updateLink(change: LinesChange, commitSHA: String?): String? = change.afterPath[0]
}

/**
 * Firstly, finds the absolute path of the given link using the link path and the file path
 * then simplifies and returns this absolute path
 */
fun checkRelativeLink(linkPath: String, filePath: String): String {
    var link = linkPath
    if (filePath.contains("/")) {
        link = filePath.replace(filePath.split("/").last(), "") + linkPath
    }
    return simplifyLink(link)
}

/**
 * Converts the given link path which is containing single or double dots
 * to a simple (without dots) version.
 */
fun simplifyLink(link: String): String {
    val st: Stack<String> = Stack<String>()
    val st1: Stack<String> = Stack<String>()
    var result = ""
    var i = 0
    while (i < link.length) {
        var dir = ""
        // skip all the multiple '/' eg. "/////""
        while (i < link.length && link[i] == '/') i++

        // stores directory's name("a", "b" etc.) or commands("."/"..") into dir
        while (i < link.length && link[i] != '/') {
            dir += link[i]
            i++
        }

        // if dir has ".." just pop the topmost element if
        // the stack is not empty otherwise ignore
        if (dir == "..") {
            if (!st.empty()) st.pop()
        } else if (dir == ".") {
            // if dir has "." then simply continue with the process
            i++
            continue
        } else if (dir.isNotEmpty()) st.push(dir)
        i++
    }
    while (!st.empty()) { st1.push(st.pop()) }
    while (!st1.empty()) {
        result += if (st1.size != 1) st1.pop().toString() + "/" else st1.pop()
    }
    return result
}
