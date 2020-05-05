package org.intellij.plugin.tracker.data

enum class LinkType(private val type: String) {
    FILE("FILE"),
    LINE("LINE"),
    DIRECTORY("DIRECTORY"),
    URL("URL")
}

data class Link(val linkType: LinkType, var linkText: String, var linkPath: String)
