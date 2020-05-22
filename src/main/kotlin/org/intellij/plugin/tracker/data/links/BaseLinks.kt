package org.intellij.plugin.tracker.data.links

import com.intellij.openapi.project.Project
import org.intellij.plugin.tracker.utils.GitOperationManager
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
        val returnMatcher = pattern!!.matcher(linkInfo.linkPath)
        returnMatcher.matches()
        returnMatcher
    }

    abstract fun getReferencedFileName(): String

    /**
     * Returns the relative path at which the referenced element is located.
     */
    abstract fun getPath(): String
}

/**
 * Abstract class for relative links
 */
abstract class RelativeLink(
    override val linkInfo: LinkInfo,
    override val pattern: Pattern? = null,
    override var commitSHA: String? = null
) : Link(linkInfo, pattern) {

    override fun getPath(): String {
        return linkInfo.linkPath
    }
}


/**
 * Abstract class for web links
 *
 * Includes common functions for web links
 */
abstract class WebLink(
    override val linkInfo: LinkInfo,
    override val pattern: Pattern,
    override var commitSHA: String? = null
) : Link(linkInfo, pattern) {

    var referenceType: WebLinkReferenceType? = null

    fun getPlatformName(): String = matcher.group(4)

    fun getProjectOwnerName(): String = matcher.group(5)

    fun getProjectName(): String = matcher.group(6)

    fun getWebLinkReferenceType(): WebLinkReferenceType {
        val ref: String = getReferencingName()
        val gitOperationManager = GitOperationManager(linkInfo.project)
        val result: WebLinkReferenceType = when {
            gitOperationManager.isRefABranch(ref) -> WebLinkReferenceType.BRANCH
            gitOperationManager.isRefATag(ref) -> WebLinkReferenceType.TAG
            gitOperationManager.isRefACommit(ref) -> WebLinkReferenceType.COMMIT
            else -> WebLinkReferenceType.INVALID
        }
        referenceType = result
        return result
    }

    fun isPermalink(): Boolean {
        if (getWebLinkReferenceType() == WebLinkReferenceType.COMMIT) return true
        return false
    }

    fun getReferencingName(): String = matcher.group(9) ?: matcher.group(11)

    // TODO: check the web reference type
    fun correspondsToLocalProject(): Boolean {
        val remoteOriginUrl = "https://${getPlatformName()}/${getProjectOwnerName()}/${getProjectName()}.git"
        return GitOperationManager(linkInfo.project).getRemoteOriginUrl() == remoteOriginUrl
    }

    abstract fun updateLink(
        afterPath: String,
        referencedLine: Int? = null,
        startLine: Int? = null,
        endLine: Int? = null
    ): String
}

/**
 * Data class which encapsulates information about links which are not supported and the reasoning
 * why they are not supported
 */
data class NotSupportedLink(
    override val linkInfo: LinkInfo,
    override val pattern: Pattern? = null,
    override var commitSHA: String? = null,
    val errorMessage: String? = null
) : Link(linkInfo, pattern) {
    override fun getReferencedFileName(): String {
        return ""
    }

    override fun getPath(): String {
        return linkInfo.linkPath
    }
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

    fun getAfterPathToOriginalFormat(afterPath: String): String {
        val targetPath = Paths.get(afterPath)
        val sourcePath = Paths.get(proveniencePath).parent ?: Paths.get(".")
        return sourcePath?.relativize(targetPath).toString().replace("\\", "/")
    }

    private fun getMarkdownDirectoryPath(): String {
        return proveniencePath.replace(fileName, "")
    }

    fun getMarkdownDirectoryRelativeLinkPath(): String {
        return getMarkdownDirectoryPath() + linkPath
    }
}