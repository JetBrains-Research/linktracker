package org.intellij.plugin.tracker.data.links

import org.intellij.plugin.tracker.data.WebLinkReferenceTypeIsInvalidException
import org.intellij.plugin.tracker.data.changes.*
import org.intellij.plugin.tracker.services.ChangeTrackerService
import org.intellij.plugin.tracker.utils.GitOperationManager
import org.intellij.plugin.tracker.utils.LinkPatterns
import java.io.File
import java.util.regex.Pattern


data class WebLinkToDirectory(
    override val linkInfo: LinkInfo,
    override val pattern: Pattern = LinkPatterns.WebLinkToDirectory.pattern,
    override var commitSHA: String? = null
) : WebLink<DirectoryChange>(linkInfo, pattern) {
    override val lineReferenced: Int
        get() = -1

    override val referencedFileName: String
        get() = ""

    override val referencedStartingLine: Int
        get() = -1

    override val referencedEndingLine: Int
        get() = -1

    override val path: String
        get() = matcher.group(12)

    override fun visit(visitor: ChangeTrackerService): Change {
        if (correspondsToLocalProject(GitOperationManager(linkInfo.project).getRemoteOriginUrl())) {
            return visitor.getLocalDirectoryChanges(this)
        }
        if (platformName.contains("github")) {
            return visitor.getRemoteDirectoryChanges(this)
        }

        throw NotImplementedError()
    }

    override fun updateLink(change: DirectoryChange, commitSHA: String?): String? {
        var newPath: String = linkInfo.linkPath
        if (referenceType == WebLinkReferenceType.COMMIT) {
            if (commitSHA == null) return null

            newPath = newPath.replace(referencingName, commitSHA)
        }
        // attach link prefix and suffix if specified (e.g. for web links of type <link path>)
        if (linkInfo.linkPathPrefix != null) newPath = "${linkInfo.linkPathPrefix}$newPath"
        if (linkInfo.linkPathSuffix != null) newPath = "$newPath${linkInfo.linkPathSuffix}"
        return newPath.replace(path, change.afterPath)
    }

    override fun copyWithAfterPath(link: Link, afterPath: String): WebLinkToDirectory {
        val linkInfoCopy: LinkInfo = link.linkInfo.copy(linkPath = afterPath)
        return copy(linkInfo = linkInfoCopy)
    }
}

data class WebLinkToFile(
    override val linkInfo: LinkInfo,
    override val pattern: Pattern = LinkPatterns.WebLinkToFile.pattern
) : WebLink<FileChange>(linkInfo, pattern) {
    override val path: String
        get() {
            if (matcher.matches())
                return matcher.group(12)
            return linkInfo.linkPath
        }

    override val lineReferenced: Int
        get() = -1

    override val referencedFileName: String
        get() = File(path).name

    override val referencedStartingLine: Int
        get() = -1

    override val referencedEndingLine: Int
        get() = -1

    override fun visit(visitor: ChangeTrackerService): Change {
        if (correspondsToLocalProject(GitOperationManager(linkInfo.project).getRemoteOriginUrl())) {
            return when (referenceType) {
                WebLinkReferenceType.COMMIT -> visitor.getLocalFileChanges(link = this, specificCommit = referencingName)
                WebLinkReferenceType.BRANCH, WebLinkReferenceType.TAG ->
                    visitor.getLocalFileChanges(link = this, branchOrTagName = referencingName)
                else -> throw WebLinkReferenceTypeIsInvalidException()
            }
        }

        throw NotImplementedError("")
    }

    override fun updateLink(change: FileChange, commitSHA: String?): String? {
        var newPath: String = linkInfo.linkPath
        if (referenceType == WebLinkReferenceType.COMMIT) {
            if (commitSHA == null) return null
            newPath = newPath.replace(referencingName, commitSHA)
        }
        // attach link prefix and suffix if specified (e.g. for web links of type <link path>)
        if (linkInfo.linkPathPrefix != null) newPath = "${linkInfo.linkPathPrefix}$newPath"
        if (linkInfo.linkPathSuffix != null) newPath = "$newPath${linkInfo.linkPathSuffix}"
        return newPath.replace(path, change.afterPath)
    }

    override fun copyWithAfterPath(link: Link, afterPath: String): WebLinkToFile {
        val linkInfoCopy: LinkInfo = link.linkInfo.copy(linkPath = afterPath)
        return copy(linkInfo = linkInfoCopy)
    }
}

data class WebLinkToLine(
    override val linkInfo: LinkInfo,
    override val pattern: Pattern = LinkPatterns.WebLinkToLine.pattern
) : WebLink<LineChange>(linkInfo, pattern) {
    override val path: String
        get() {
            if (matcher.matches())
                return matcher.group(11)
            return linkInfo.linkPath
        }
    override val lineReferenced: Int
        get() = matcher.group(12).toInt()
    override val referencedFileName: String
        get() = File(path).name.replace("#L${matcher.group(12)}", "")
    override val referencedStartingLine: Int
        get() = -1
    override val referencedEndingLine: Int
        get() = -1

    override fun visit(visitor: ChangeTrackerService): Change {
        if (correspondsToLocalProject(GitOperationManager(linkInfo.project).getRemoteOriginUrl())) {
            return when (referenceType) {
                WebLinkReferenceType.COMMIT -> visitor.getLocalLineChanges(link = this, specificCommit = referencingName)
                WebLinkReferenceType.BRANCH, WebLinkReferenceType.TAG ->
                    visitor.getLocalLineChanges(link = this, branchOrTagName = referencingName)
                else -> throw WebLinkReferenceTypeIsInvalidException()
            }
        }

        throw NotImplementedError("")
    }

    override fun updateLink(change: LineChange, commitSHA: String?): String? {
        var newPath: String = linkInfo.linkPath
        if (referenceType == WebLinkReferenceType.COMMIT) {
            if (commitSHA == null) return null

            newPath = newPath.replace(referencingName, commitSHA)
        }
        if (change.newLine == null) return null

        // attach link prefix and suffix if specified (e.g. for web links of type <link path>)
        if (linkInfo.linkPathPrefix != null) newPath = "${linkInfo.linkPathPrefix}$newPath"
        if (linkInfo.linkPathSuffix != null) newPath = "$newPath${linkInfo.linkPathSuffix}"
        return newPath.replace("$path#L$lineReferenced", "${change.fileChange.afterPath}#L${change.newLine.lineNumber}")
    }

    override fun copyWithAfterPath(link: Link, afterPath: String): WebLinkToLine {
        val linkInfoCopy: LinkInfo = link.linkInfo.copy(linkPath = afterPath)
        return copy(linkInfo = linkInfoCopy)
    }
}


data class WebLinkToLines(
    override val linkInfo: LinkInfo,
    override val pattern: Pattern = LinkPatterns.WebLinkToLines.pattern
) : WebLink<LinesChange>(linkInfo, pattern) {
    override val path: String
        get() {
            if (matcher.matches())
                return matcher.group(11)
            return linkInfo.linkPath
        }
    override val lineReferenced: Int
        get() = -1
    override val referencedFileName: String
        get() = File(path).name.replace("#L$referencedStartingLine-L$referencedEndingLine", "")
    override val referencedStartingLine: Int
        get() = matcher.group(12).toInt()
    override val referencedEndingLine: Int
        get() = matcher.group(13).toInt()

    override fun visit(visitor: ChangeTrackerService): Change {
        TODO("not implemented")
    }

    override fun updateLink(change: LinesChange, commitSHA: String?): String? {
        TODO("not implemented")
    }

    override fun copyWithAfterPath(link: Link, afterPath: String): WebLinkToLines {
        val linkInfoCopy: LinkInfo = link.linkInfo.copy(linkPath = afterPath)
        return copy(linkInfo = linkInfoCopy)
    }
}