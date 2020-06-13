package org.intellij.plugin.tracker.data.links

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import java.lang.IllegalArgumentException
import java.nio.file.Paths
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.intellij.plugin.tracker.data.changes.Change
import org.intellij.plugin.tracker.services.ChangeTrackerService
import org.intellij.plugin.tracker.utils.GitOperationManager


/**
 * An enum class for web link reference types
 */
enum class WebLinkReferenceType(private val type: String) {
    COMMIT("COMMIT"),
    BRANCH("BRANCH"),
    TAG("TAG"),
    INVALID("INVALID")
}

/**
 * Base class for links
 */
abstract class Link(

    /**
     * Information about the link that has been retrieved from processing the markdown files
     */
    open val linkInfo: LinkInfo,

    /**
     * Pattern that corresponds to a certain type of link (e.g. can be a WebLinkToFile pattern)
     */
    open val pattern: Pattern? = null
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

    /**
     * The line number in case of a link to a line
     */
    abstract val lineReferenced: Int

    /**
     * The file name of the element being referenced by the link
     * Valid in case of links to files and lines.
     */
    abstract val referencedFileName: String

    /**
     * The start line (inclusive) in case of links to multiple lines
     */
    abstract val referencedStartingLine: Int

    /**
     * The end line (inclusive) in case of links to multiple lines
     */
    abstract val referencedEndingLine: Int

    /**
     * Retrieve the changes of a specific link by passing in an
     * implementation of the ChangeTrackerService interface
     */
    abstract fun visit(visitor: ChangeTrackerService): Change

    /**
     * Performs a deep copy of the link and changes the after path of
     * the copied link to be the one that is passed as a parameter
     */
    abstract fun copyWithAfterPath(link: Link, afterPath: String): Link

    /**
     * Method that indicates whether the markdown file in which the link
     * is located has been moved
     */
    abstract fun markdownFileMoved(afterPath: String): Boolean
}

/**
 * Abstract class for relative links
 * Each link is bound to a specific change
 */
abstract class RelativeLink<in T : Change>(
    override val linkInfo: LinkInfo,
    override val pattern: Pattern? = null
) : Link(linkInfo, pattern) {

    override val path: String
        get() = linkInfo.linkPath

    val relativePath: String
        get() {
            var path = linkInfo.linkPath
            if (path.contains("%20")) {
                path = path.replaceFirst("%20", " ")
            }
            return checkRelativeLink(path, linkInfo.proveniencePath)
        }

    /**
     * Checks whether the markdown file in which this link is located has been moved
     */
    override fun markdownFileMoved(afterPath: String): Boolean = checkRelativeLink(linkInfo
        .getAfterPathToOriginalFormat(afterPath)!!, linkInfo.proveniencePath) != afterPath

    /**
     * Method that, given
     */
    abstract fun updateLink(change: T, commitSHA: String?): String?
}

/**
 * Abstract class for web links
 *
 * Includes common functions for web links
 * Each link is bound to a specific change
 */
abstract class WebLink<in T : Change>(
    override val linkInfo: LinkInfo,
    override val pattern: Pattern
) : Link(linkInfo, pattern) {

    /**
     * Retrieves the reference type of this web link
     * Be it a commit, tag or branch.
     *
     * This property is calculated only once per class instance
     * due to it's expensive nature.
     */
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

    /**
     * Get the platform name part from the URL
     */
    val platformName: String
        get() = matcher.group(4)

    /**
     * Get the project owner name part from the URL
     */
    val projectOwnerName: String
        get() = matcher.group(5)

    /**
     * Get the project name part from the URL
     */
    val projectName: String
        get() = matcher.group(6)

    /**
     * Get the referencing name part from the URL
     */
    val referencingName: String
        get() = matcher.group(9) ?: matcher.group(11)


    /**
     * Checks whether this web link is a permalink
     */
    fun isPermalink(): Boolean {
        if (referenceType == WebLinkReferenceType.COMMIT) return true
        return false
    }

    /**
     * Checks whether this link corresponds to the currently open project
     */
    fun correspondsToLocalProject(gitRemoteOriginUrl: String): Boolean {
        val remoteOriginUrl = "https://$platformName/$projectOwnerName/$projectName.git"
        return gitRemoteOriginUrl == remoteOriginUrl
    }

    /**
     * This method generates a new link based on the change object passed in
     *
     * If the reference type is a commit, then change the reference part of the link
     * to point to the HEAD commit SHA
     *
     * Each sub-type of WebLink implements it's own new path generation method
     */
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

    /**
     * Generates a new, equivalent path, based on the change object passed in as a parameter
     */
    abstract fun generateNewPath(change: T, newPath: String): String?

    /**
     * Always return false, web links do not have relative paths and therefore
     * are not affected by the markdown file being moved
     */
    override fun markdownFileMoved(afterPath: String): Boolean = false
}

/**
 * Data class which encapsulates information about links which are not supported and the reasoning
 * why they are not supported
 */
data class NotSupportedLink(
    override val linkInfo: LinkInfo,
    override val pattern: Pattern? = null,
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


    override fun copyWithAfterPath(link: Link, afterPath: String): NotSupportedLink {
        val linkInfoCopy: LinkInfo = link.linkInfo.copy(linkPath = afterPath)
        return copy(linkInfo = linkInfoCopy)
    }

    override fun visit(visitor: ChangeTrackerService): Change = throw UnsupportedOperationException()

    override fun markdownFileMoved(afterPath: String): Boolean = throw UnsupportedOperationException()
}

/**
 * Data class containing information about the link, which was retrieved from markdown files
 *
 */
data class LinkInfo(
    /**
     * The link text of the link, as it appears in the markdown file
     */
    val linkText: String,

    /**
     * The path, which could be a relative path or a web URL to a web-hosted repository
     */
    var linkPath: String,

    /**
     * The path, relative to the project root directory, to the markdown file
     * in which the link is located
     */
    val proveniencePath: String,

    /**
     * The line number at which this link is found in the markdown file
     */
    val foundAtLineNumber: Int,

    /**
     * The text offset at which this link is found in the markdown file
     */
    val textOffset: Int,

    /**
     * The name of the file in which the link is located
     */
    val fileName: String,

    /**
     * The project in which the markdown file containing this link is located
     */
    val project: Project,

    /**
     * Any link path prefix of the type '<' that accompanies the link path
     */
    val linkPathPrefix: String? = null,

    /**
     * Any link path prefix of the type '>' that accompanies the link path
     */
    val linkPathSuffix: String? = null
) {

    /**
     * Gets the format in which the link appears in the markdown files
     */
    fun getMarkDownSyntaxString(): String {
        return "[$linkText]($linkPath)"
    }

    /**
     * Get the after path (the newly generated path) into a path
     * that is relative to the directory containing the markdown file, which in
     * turn contains this link
     */
    fun getAfterPathToOriginalFormat(afterPath: String): String? {
        val targetPath = Paths.get(afterPath)
        val sourcePath = Paths.get(proveniencePath).parent ?: Paths.get(".")
        return try {
            sourcePath?.relativize(targetPath).toString().replace("\\", "/")
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /**
     * Gets the path of the markdown file in which the link is located
     * without the name of the markdown file part.
     */
    private fun getMarkdownDirectoryPath(): String {
        return proveniencePath.replace(fileName, "")
    }

    /**
     * Get the path composed of the path of the markdown file in which the link is located
     * (relative to the project root directory) plus the link path part of the link
     */
    fun getMarkdownDirectoryRelativeLinkPath(): String {
        return getMarkdownDirectoryPath() + linkPath
    }
}
