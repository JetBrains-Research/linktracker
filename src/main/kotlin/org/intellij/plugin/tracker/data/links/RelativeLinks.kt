package org.intellij.plugin.tracker.data.links

import java.util.regex.Matcher

data class RelativeLinkToDirectory(
    override val linkText: String,
    override val linkPath: String,
    override val proveniencePath: String,
    override val foundAtLineNumber: Int,
    override val textOffset: Int,
    override val matcher: Matcher? = null,
    override val commitSHA: String
) : Link(linkText, linkPath, proveniencePath, foundAtLineNumber, textOffset, matcher, commitSHA) {
    override fun getPath(): String {
        return linkPath
    }
}

data class RelativeLinkToFile(
    override val linkText: String,
    override val linkPath: String,
    override val proveniencePath: String,
    override val foundAtLineNumber: Int,
    override val textOffset: Int,
    override val matcher: Matcher? = null,
    override val commitSHA: String
) : Link(linkText, linkPath, proveniencePath, foundAtLineNumber, textOffset, matcher, commitSHA) {
    override fun getPath(): String {
        return linkPath
    }
}

data class RelativeLinkToLine(
    override val linkText: String,
    override val linkPath: String,
    override val proveniencePath: String,
    override val foundAtLineNumber: Int,
    override val textOffset: Int,
    override val matcher: Matcher,
    override val commitSHA: String
) : Link(linkText, linkPath, proveniencePath, foundAtLineNumber, textOffset, matcher, commitSHA) {

    override fun getPath(): String {
        return linkPath
    }

    fun getLineReferenced(): Int = matcher.group(1).toInt()
}

data class RelativeLinkToLines(
    override val linkText: String,
    override val linkPath: String,
    override val proveniencePath: String,
    override val foundAtLineNumber: Int,
    override val textOffset: Int,
    override val matcher: Matcher,
    override val commitSHA: String
) : Link(linkText, linkPath, proveniencePath, foundAtLineNumber, textOffset, matcher, commitSHA) {

    override fun getPath(): String {
        return linkPath
    }

    fun getStartLineReferenced(): Int = matcher.group(1).toInt()

    fun getEndLineReferenced(): Int = matcher.group(2).toInt()
}