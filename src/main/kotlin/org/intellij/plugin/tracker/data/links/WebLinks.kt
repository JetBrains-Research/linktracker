package org.intellij.plugin.tracker.data.links

import org.intellij.plugin.tracker.data.WebLinkReferenceTypeIsInvalidException
import org.intellij.plugin.tracker.data.changes.Change
import org.intellij.plugin.tracker.data.changes.CustomChange
import org.intellij.plugin.tracker.data.changes.LineChange
import org.intellij.plugin.tracker.data.changes.LinesChange
import org.intellij.plugin.tracker.services.ChangeTrackerService
import org.intellij.plugin.tracker.utils.GitOperationManager
import org.intellij.plugin.tracker.utils.LinkPatterns
import java.io.File
import java.util.regex.Pattern

/**
 * Data class that corresponds to a web link to directory
 */
data class WebLinkToDirectory(
    override val linkInfo: LinkInfo,
    override val pattern: Pattern = LinkPatterns.WebLinkToDirectory.pattern
) : WebLink<CustomChange>(linkInfo, pattern) {

    /**
     * No lines referenced
     */
    override val lineReferenced: Int
        get() = -1

    /**
     * No file referenced
     */
    override val referencedFileName: String
        get() = ""

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
     * Get the path from the URL
     */
    override val path: String
        get() = matcher.group(12)

    /**
     * Call the right method in the implementation of ChangeTrackerService
     */
    override fun visit(visitor: ChangeTrackerService): Change {
        if (correspondsToLocalProject(GitOperationManager(linkInfo.project).getRemoteOriginUrl())) {
            return when (referenceType) {
                WebLinkReferenceType.COMMIT -> visitor.getLocalDirectoryChanges(
                    link = this,
                    specificCommit = referencingName
                )
                WebLinkReferenceType.BRANCH, WebLinkReferenceType.TAG ->
                    visitor.getLocalDirectoryChanges(link = this, branchOrTagName = referencingName)
                else -> throw WebLinkReferenceTypeIsInvalidException()
            }
        }

        throw NotImplementedError()
    }

    /**
     * Generates a new, equivalent path, based on the change object passed in as a parameter
     */
    override fun generateNewPath(change: CustomChange, newPath: String): String? =
        newPath.replace(path, change.afterPathString)

    /**
     * Deep copy this link and return the copied link with a new after path
     */
    override fun copyWithAfterPath(link: Link, afterPath: String): WebLinkToDirectory {
        val linkInfoCopy: LinkInfo = link.linkInfo.copy(linkPath = afterPath)
        return copy(linkInfo = linkInfoCopy)
    }

    /**
     * Converts this link to a link to a file, containing the parameterized file path as link path
     */
    fun convertToFileLink(filePath: String): WebLinkToFile {
        val linkInfoCopy: LinkInfo = linkInfo.copy(linkPath = filePath)
        return WebLinkToFile(
            linkInfo = linkInfoCopy
        )
    }
}

/**
 * Data class that corresponds to a web link to file
 */
data class WebLinkToFile(
    override val linkInfo: LinkInfo,
    override val pattern: Pattern = LinkPatterns.WebLinkToFile.pattern
) : WebLink<CustomChange>(linkInfo, pattern) {

    /**
     * Get the part part from the URL
     */
    override val path: String
        get() {
            if (matcher.matches())
                return matcher.group(12)
            return linkInfo.linkPath
        }

    /**
     * No line referenced
     */
    override val lineReferenced: Int
        get() = -1

    /**
     * Get only the name of the file from the path
     */
    override val referencedFileName: String
        get() = File(path).name

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
     * Generates a new, equivalent path, based on the change object passed in as a parameter
     */
    override fun generateNewPath(change: CustomChange, newPath: String): String? =
        newPath.replace(path, change.afterPathString)

    /**
     * Call the right method in the implementation of ChangeTrackerService
     */
    override fun visit(visitor: ChangeTrackerService): Change {
        if (correspondsToLocalProject(GitOperationManager(linkInfo.project).getRemoteOriginUrl())) {
            return when (referenceType) {
                WebLinkReferenceType.COMMIT -> visitor.getLocalFileChanges(
                    link = this,
                    specificCommit = referencingName
                )
                WebLinkReferenceType.BRANCH, WebLinkReferenceType.TAG ->
                    visitor.getLocalFileChanges(link = this, branchOrTagName = referencingName)
                else -> throw WebLinkReferenceTypeIsInvalidException()
            }
        }

        throw NotImplementedError("")
    }

    /**
     * Deep copy this link and return the copied link with a new after path
     */
    override fun copyWithAfterPath(link: Link, afterPath: String): WebLinkToFile {
        val linkInfoCopy: LinkInfo = link.linkInfo.copy(linkPath = afterPath)
        return copy(linkInfo = linkInfoCopy)
    }
}

/**
 * Data class that corresponds to a relative link to line
 */
data class WebLinkToLine(
    override val linkInfo: LinkInfo,
    override val pattern: Pattern = LinkPatterns.WebLinkToLine.pattern
) : WebLink<LineChange>(linkInfo, pattern) {

    /**
     * Get the path part from the URL
     */
    override val path: String
        get() {
            if (matcher.matches())
                return matcher.group(11)
            return linkInfo.linkPath
        }

    /**
     * Get the line referenced specification from the URL
     */
    override val lineReferenced: Int
        get() = matcher.group(12).toInt()

    /**
     * Get the name of the file without the line specification
     */
    override val referencedFileName: String
        get() = File(path).name.replace("#L${matcher.group(12)}", "")

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
     * Generates a new, equivalent path, based on the change object passed in as a parameter
     */
    override fun generateNewPath(change: LineChange, newPath: String): String? {
        if (change.newLine == null) return null
        return newPath.replace(
            "$path#L$lineReferenced",
            "${change.fileChange.afterPath[0]}#L${change.newLine.lineNumber}"
        )
    }

    /**
     * Call the right method in the implementation of ChangeTrackerService
     */
    override fun visit(visitor: ChangeTrackerService): Change {
        if (correspondsToLocalProject(GitOperationManager(linkInfo.project).getRemoteOriginUrl())) {
            return when (referenceType) {
                WebLinkReferenceType.COMMIT -> visitor.getLocalLineChanges(
                    link = this,
                    specificCommit = referencingName
                )
                WebLinkReferenceType.BRANCH, WebLinkReferenceType.TAG ->
                    visitor.getLocalLineChanges(link = this, branchOrTagName = referencingName)
                else -> throw WebLinkReferenceTypeIsInvalidException()
            }
        }

        throw NotImplementedError("")
    }

    /**
     * Deep copy this link and return the copied link with a new after path
     */
    override fun copyWithAfterPath(link: Link, afterPath: String): WebLinkToLine {
        val linkInfoCopy: LinkInfo = link.linkInfo.copy(linkPath = afterPath)
        return copy(linkInfo = linkInfoCopy)
    }
}

/**
 * Data class that corresponds to a relative link to lines
 */
data class WebLinkToLines(
    override val linkInfo: LinkInfo,
    override val pattern: Pattern = LinkPatterns.WebLinkToLines.pattern
) : WebLink<LinesChange>(linkInfo, pattern) {

    /**
     * Get the path without line-range specifications
     */
    override val path: String
        get() {
            if (matcher.matches())
                return matcher.group(11)
            return linkInfo.linkPath
        }

    /**
     * No single line referenced
     */
    override val lineReferenced: Int
        get() = -1

    /**
     * Get the referenced file name part from the path, without the line range specifications
     */
    override val referencedFileName: String
        get() = File(path).name.replace("#L$referencedStartingLine-L$referencedEndingLine", "")

    /**
     * Get the first line referenced part (inclusive) from the path
     */
    override val referencedStartingLine: Int
        get() = matcher.group(12).toInt()

    /**
     * Get the last line referenced part (inclusive) from the path
     */
    override val referencedEndingLine: Int
        get() = matcher.group(13).toInt()

    /**
     * Call the right method in the implementation of ChangeTrackerService
     */
    override fun visit(visitor: ChangeTrackerService): Change {
        if (correspondsToLocalProject(GitOperationManager(linkInfo.project).getRemoteOriginUrl())) {
            return when (referenceType) {
                WebLinkReferenceType.COMMIT -> visitor.getLocalLinesChanges(
                    link = this,
                    specificCommit = referencingName
                )
                WebLinkReferenceType.BRANCH, WebLinkReferenceType.TAG ->
                    visitor.getLocalLinesChanges(link = this, branchOrTagName = referencingName)
                else -> throw WebLinkReferenceTypeIsInvalidException()
            }
        }

        throw NotImplementedError("")
    }

    /**
     * Generates a new, equivalent path, based on the change object passed in as a parameter
     */
    override fun generateNewPath(change: LinesChange, newPath: String): String? {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Deep copy this link and return the copied link with a new after path
     */
    override fun copyWithAfterPath(link: Link, afterPath: String): WebLinkToLines {
        val linkInfoCopy: LinkInfo = link.linkInfo.copy(linkPath = afterPath)
        return copy(linkInfo = linkInfoCopy)
    }
}
