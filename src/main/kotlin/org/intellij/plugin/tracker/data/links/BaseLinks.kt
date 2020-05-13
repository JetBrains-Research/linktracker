package org.intellij.plugin.tracker.data.links

import com.intellij.openapi.project.Project
import org.intellij.plugin.tracker.utils.GitOperationManager
import java.util.regex.Matcher
import java.util.regex.Pattern


enum class WebLinkReferenceType(private val type: String) {
    COMMIT("COMMIT"),
    BRANCH("BRANCH"),
    TAG("TAG")
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
    open val commitSHA: String? = null,
    var beenCached: Boolean = false
) {

    /**
     * Get the matcher given by the pattern and linkInfo's linkPath property
     */
    val matcher: Matcher by lazy {
        val returnMatcher = pattern!!.matcher(linkInfo.linkPath)
        returnMatcher.matches()
        returnMatcher
    }


    /**
     * Gets the format in which the link appears in the markdown files
     */
    fun getMarkDownSyntaxString(): String {
        return "[${linkInfo.linkText}](${linkInfo.linkPath})"
    }


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
    override val commitSHA: String
) : Link(linkInfo, pattern, commitSHA) {

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
    override val commitSHA: String
) : Link(linkInfo, pattern, commitSHA) {

    fun getPlatformName(): String = matcher.group(4)

    fun getProjectOwnerName(): String = matcher.group(5)

    fun getProjectName(): String = matcher.group(6)

    fun getWebLinkReferenceType(): WebLinkReferenceType = throw NotImplementedError("")

    fun isPermalink(): Boolean {
        if (getWebLinkReferenceType().toString() == "COMMIT") return true
        return false
    }

    fun getReferencingName(): String = matcher.group(9) ?: matcher.group(11)

    // TODO: check the web reference type
    fun correspondsToLocalProject(project: Project): Boolean {
        val remoteOriginUrl = "https://${getPlatformName()}/${getProjectOwnerName()}/${getProjectName()}.git"
        return GitOperationManager(project).getRemoteOriginUrl() == remoteOriginUrl
    }
}

/**
 * Data class which encapsulates information about links which are not supported and the reasoning
 * why they are not supported
 */
data class NotSupportedLink(
    override val linkInfo: LinkInfo,
    override val pattern: Pattern? = null,
    override val commitSHA: String? = null,
    val errorMessage: String? = null
):Link(linkInfo, pattern, commitSHA) {

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
    val project: Project
){

    fun getAfterPathToOriginalFormat(afterPath: String): String{
        val newPath = afterPath.replace("${project.basePath!!}/", "")
        return newPath.replace(getMarkdownDirectoryPath(), "")
    }

    fun getMarkdownDirectoryPath(): String {
        return proveniencePath.replace(fileName, "")
    }

    fun getMarkdownDirectoryRelativeLinkPath(): String {
        return getMarkdownDirectoryPath() + linkPath
    }
}