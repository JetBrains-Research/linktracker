package org.intellij.plugin.tracker.data

import com.intellij.openapi.project.Project

enum class WebLinkReferenceType(private val type: String) {
    COMMIT("COMMIT"),
    BRANCH("BRANCH"),
    TAG("TAG")
}

enum class LinkType(private val type: String) {
    LINE("LINE"),
    LINES("LINES"),
    FILE("FILE"),
    DIRECTORY("DIRECTORY"),
    USER("USER"),
    URL("URL")
}

abstract class Link(
    open var linkType: LinkType,
    open var linkText: String,
    open var linkPath: String,
    open var proveniencePath: String, // TODO what is proveniencePath
    open var foundAtLineNumber: Int
) {
    fun getMarkDownSyntaxString(): String {
        return "[$linkText]($linkPath)"
    }

    /**
     * Returns the relative path at which the referenced element is located.
     */
    abstract fun getPath(): String
}

data class WebLink(
    override var linkType: LinkType,
    override var linkText: String,
    override var linkPath: String,
    override var proveniencePath: String,
    override var foundAtLineNumber: Int,
    var platformName: String,
    var projectOwnerName: String,
    var projectName: String,
    var relativePath: String,
    var referenceType: WebLinkReferenceType,
    var referenceName: String,
    var lineReferenced: Int? = null,
    var startReferencedLine: Int? = null,
    var endReferencedLine: Int? = null
) : Link(linkType, linkText, linkPath, proveniencePath, foundAtLineNumber) {

    override fun getPath(): String {
        return relativePath
    }

    // TODO: Check whether the weblink corresponds to the currently open project
    fun correspondsToLocalProject(project: Project): Boolean = throw NotImplementedError("")

    fun isPermalink(): Boolean {
        if (referenceType.toString() == "COMMIT") return true
        return false
    }
}

data class RelativeLink(
    override var linkType: LinkType,
    override var linkText: String,
    override var linkPath: String,
    override var proveniencePath: String,
    override var foundAtLineNumber: Int
) : Link(linkType, linkText, linkPath, proveniencePath, foundAtLineNumber) {

    override fun getPath(): String {
        return linkPath
    }
}
