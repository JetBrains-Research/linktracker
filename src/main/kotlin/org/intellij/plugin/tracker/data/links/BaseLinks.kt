package org.intellij.plugin.tracker.data.links

import com.intellij.openapi.project.Project
import org.intellij.plugin.tracker.utils.GitOperationManager
import java.util.regex.Matcher


enum class WebLinkReferenceType(private val type: String) {
    COMMIT("COMMIT"),
    BRANCH("BRANCH"),
    TAG("TAG")
}

abstract class Link(
    open val linkText: String,
    open val linkPath: String,
    open val proveniencePath: String, // TODO what is proveniencePath
    open val foundAtLineNumber: Int,
    open val textOffset: Int,
    open val matcher: Matcher? = null,
    open val commitSHA: String? = null
) {
    fun getMarkDownSyntaxString(): String {
        return "[$linkText]($linkPath)"
    }

    /**
     * Returns the relative path at which the referenced element is located.
     */
    abstract fun getPath(): String
}


abstract class WebLink(
    override val linkText: String,
    override val linkPath: String,
    override val proveniencePath: String,
    override val foundAtLineNumber: Int,
    override val textOffset: Int,
    override val matcher: Matcher,
    override val commitSHA: String
) : Link(linkText, linkPath, proveniencePath, foundAtLineNumber, textOffset, matcher, commitSHA) {


    fun getPlatformName(): String {
        //println("platform name is ${matcher.group(4)}")
        return matcher.group(4)
    }

    fun getProjectOwnerName(): String {
        //println("project owner name is ${matcher.group(5)}")
        return matcher.group(5)
    }

    fun getProjectName(): String {
        //println("project name is ${matcher.group(6)}")
        return matcher.group(6)
    }

    fun getWebLinkReferenceType(): WebLinkReferenceType = throw NotImplementedError("")

    fun isPermalink(): Boolean {
        if (getWebLinkReferenceType().toString() == "COMMIT") return true
        return false
    }

    fun getReferencingName(): String {
        //println("referencing name is ${matcher.group(9)} or ${matcher.group(11)}")
        return matcher.group(9) ?: matcher.group(11)
    }

    fun correspondsToLocalProject(project: Project): Boolean {
        val remoteOriginUrl = "https://${getPlatformName()}/${getProjectOwnerName()}/${getProjectName()}.git"
        return GitOperationManager(project).getRemoteOriginUrl() == remoteOriginUrl
    }
}

data class PotentialLink(
    override val linkText: String,
    override val linkPath: String,
    override val proveniencePath: String,
    override val foundAtLineNumber: Int,
    override val textOffset: Int
): Link(linkText, linkPath, proveniencePath, foundAtLineNumber, textOffset) {

    override fun getPath(): String {
        return linkPath
    }
}

data class NotSupportedLink(
    override val linkText: String,
    override val linkPath: String,
    override val proveniencePath: String,
    override val foundAtLineNumber: Int,
    override val textOffset: Int,
    override val matcher: Matcher? = null,
    override val commitSHA: String? = null,
    val errorMessage: String? = null
):Link(linkText, linkPath, proveniencePath, foundAtLineNumber, textOffset, matcher, commitSHA) {
    override fun getPath(): String {
        return linkPath
    }
}