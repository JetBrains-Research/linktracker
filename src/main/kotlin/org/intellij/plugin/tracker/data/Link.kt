package org.intellij.plugin.tracker.data


enum class WebLinkReferenceType(private val type: String) {
    COMMIT("COMMIT"),
    BRANCH("BRANCH"),
    TAG("TAG")
}

enum class LinkType(private val type: String) {
    LINE("LINE"),
    LINES("LINES"),
    FILE("FILE"),
    DIRECTORY("DIRECTORY")
}

abstract class Link(
    open var linkType: LinkType,
    open var linkText: String,
    open var linkPath: String,
    open var proveniencePath: String,
    open var foundAtLineNumber: Int
) {
    fun getMarkDownSyntaxString(): String {
        return "[$linkText]($linkPath)"
    }
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
    var correspondsToLocalProject: Boolean,
    var isPermaLink: Boolean = false,
    var lineReferenced: Int? = null,
    var startReferencedLine: Int? = null,
    var endReferencedLine: Int? = null
) : Link(linkType, linkText, linkPath, proveniencePath, foundAtLineNumber)

data class RelativeLink(
    override var linkType: LinkType,
    override var linkText: String,
    override var linkPath: String,
    override var proveniencePath: String,
    override var foundAtLineNumber: Int
) : Link(linkType, linkText, linkPath, proveniencePath, foundAtLineNumber)
