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

    override fun generateNewPath(change: DirectoryChange, newPath: String): String? =
        newPath.replace(path, change.afterPathString)

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

    override fun generateNewPath(change: FileChange, newPath: String): String? =
        newPath.replace(path, change.afterPathString)

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

    override fun generateNewPath(change: LineChange, newPath: String): String? {
        if (change.newLine == null) return null
        return newPath.replace("$path#L$lineReferenced", "${change.fileChange.afterPath}#L${change.newLine.lineNumber}")
    }

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

    override fun generateNewPath(change: LinesChange, newPath: String): String? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun copyWithAfterPath(link: Link, afterPath: String): WebLinkToLines {
        val linkInfoCopy: LinkInfo = link.linkInfo.copy(linkPath = afterPath)
        return copy(linkInfo = linkInfoCopy)
    }
}