package org.intellij.plugin.tracker.data.links

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import org.intellij.plugin.tracker.data.changes.Change
import org.intellij.plugin.tracker.services.ChangeTrackerService
import org.intellij.plugin.tracker.utils.GitOperationManager
import java.lang.IllegalArgumentException
import java.nio.file.Paths
import java.util.regex.Matcher
import java.util.regex.Pattern


enum class WebLinkReferenceType(private val type: String) {
    COMMIT("COMMIT"),
    BRANCH("BRANCH"),
    TAG("TAG"),
    INVALID("INVALID")
}


/**
 * Base class for links
 *
 * @linkInfo: information about the link that has been retrieved from processing the markdown files
 * @pattern: pattern that corresponds to a certain type of link (e.g. can be a WebLinkToFile pattern)
 * @commitSHA: commit SHA at which the line containing the link was created / commit SHA at which the plugin has been
 *             last run on
 */
abstract class Link(
    open val linkInfo: LinkInfo,
    open val pattern: Pattern? = null,
    open var commitSHA: String? = null
) {

    /**
     * Get the matcher given by the pattern and linkInfo's linkPath property
     */
    val matcher: Matcher by lazy {
        val returnMatcher: Matcher = pattern!!.matcher(linkInfo.linkPath)
        returnMatcher.matches()
        returnMatcher
    }

    /**
     * Returns the relative path at which the referenced element is located.
     */
    abstract val path: String

    abstract val lineReferenced: Int

    abstract val referencedFileName: String

    abstract val referencedStartingLine: Int

    abstract val referencedEndingLine: Int

    abstract fun visit(visitor: ChangeTrackerService): Change

    abstract fun copyWithAfterPath(link: Link, afterPath: String): Link

    abstract fun markdownFileMoved(afterPath: String): Boolean
}

/**
 * Abstract class for relative links
 */
abstract class RelativeLink<in T : Change>(
    override val linkInfo: LinkInfo,
    override val pattern: Pattern? = null,
    override var commitSHA: String? = null
) : Link(linkInfo, pattern) {

    override val path: String
        get() = linkInfo.linkPath

    override fun markdownFileMoved(afterPath: String): Boolean = checkRelativeLink(linkInfo
        .getAfterPathToOriginalFormat(afterPath)!!, linkInfo.proveniencePath) != afterPath

    abstract fun updateLink(change: T, commitSHA: String?): String?
}


/**
 * Abstract class for web links
 *
 * Includes common functions for web links
 */
abstract class WebLink<in T : Change>(
    override val linkInfo: LinkInfo,
    override val pattern: Pattern,
    override var commitSHA: String? = null
) : Link(linkInfo, pattern) {
    var referenceType: WebLinkReferenceType? = null
        get() {
            if (field == null) {
                val ref: String = referencingName
                val gitOperationManager = GitOperationManager(linkInfo.project)
                try {
                    val result: WebLinkReferenceType = when {
                        gitOperationManager.isRefABranch(ref) -> WebLinkReferenceType.BRANCH
                        gitOperationManager.isRefATag(ref) -> WebLinkReferenceType.TAG
                        gitOperationManager.isRefACommit(ref) -> WebLinkReferenceType.COMMIT
                        else -> WebLinkReferenceType.INVALID
                    }
                    field = result
                } catch (e: VcsException) {
                    return field
                }
            }
            return field
        }

    val platformName: String
        get() = matcher.group(4)

    val projectOwnerName: String
        get() = matcher.group(5)

    val projectName: String
        get() = matcher.group(6)

    val referencingName: String
        get() = matcher.group(9) ?: matcher.group(11)

    fun isPermalink(): Boolean {
        if (referenceType == WebLinkReferenceType.COMMIT) return true
        return false
    }

    fun correspondsToLocalProject(gitRemoteOriginUrl: String): Boolean {
        val remoteOriginUrl = "https://$platformName/$projectOwnerName/$projectName.git"
        return gitRemoteOriginUrl == remoteOriginUrl
    }

    fun updateLink(change: T, commitSHA: String?): String? {
        var newPath: String = linkInfo.linkPath
        if (referenceType == WebLinkReferenceType.COMMIT) {
            if (commitSHA == null) return null

            newPath = newPath.replace(referencingName, commitSHA)
        }
        // attach link prefix and suffix if specified (e.g. for web links of type <link path>)
        if (linkInfo.linkPathPrefix != null) newPath = "${linkInfo.linkPathPrefix}$newPath"
        if (linkInfo.linkPathSuffix != null) newPath = "$newPath${linkInfo.linkPathSuffix}"
        return generateNewPath(change, newPath)
    }

    abstract fun generateNewPath(change: T, newPath: String): String?

    override fun markdownFileMoved(afterPath: String): Boolean = false
}

/**
 * Data class which encapsulates information about links which are not supported and the reasoning
 * why they are not supported
 */
data class NotSupportedLink (
    override val linkInfo: LinkInfo,
    override val pattern: Pattern? = null,
    override var commitSHA: String? = null,
    val errorMessage: String? = null
) : Link(linkInfo, pattern) {
    override val lineReferenced: Int
        get() = -1

    override val referencedStartingLine: Int
        get() = -1

    override val referencedEndingLine: Int
        get() = -1

    override val referencedFileName: String
        get() = ""

    override val path: String
        get() = linkInfo.linkPath

    override fun visit(visitor: ChangeTrackerService): Change {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun copyWithAfterPath(link: Link, afterPath: String): NotSupportedLink {
        val linkInfoCopy: LinkInfo = link.linkInfo.copy(linkPath = afterPath)
        return copy(linkInfo = linkInfoCopy)
    }

    override fun markdownFileMoved(afterPath: String): Boolean = false
}


/**
 * Data class containing information about the link, which was retrieved from markdown files
 *
 */
data class LinkInfo(
    val linkText: String,
    var linkPath: String,
    val proveniencePath: String,
    val foundAtLineNumber: Int,
    val textOffset: Int,
    val fileName: String,
    val project: Project,
    val linkPathPrefix: String? = null,
    val linkPathSuffix: String? = null
) {

    /**
     * Gets the format in which the link appears in the markdown files
     */
    fun getMarkDownSyntaxString(): String {
        return "[$linkText]($linkPath)"
    }

    fun getAfterPathToOriginalFormat(afterPath: String): String? {
        val targetPath = Paths.get(afterPath)
        val sourcePath = Paths.get(proveniencePath).parent ?: Paths.get(".")
        return try {
            sourcePath?.relativize(targetPath).toString().replace("\\", "/")
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private fun getMarkdownDirectoryPath(): String {
        return proveniencePath.replace(fileName, "")
    }

    fun getMarkdownDirectoryRelativeLinkPath(): String {
        return getMarkdownDirectoryPath() + linkPath
    }
}