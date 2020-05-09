package org.intellij.plugin.tracker.data.links

import java.util.regex.Matcher

data class WebLinkToDirectory(
    override val linkText: String,
    override val linkPath: String,
    override val proveniencePath: String,
    override val foundAtLineNumber: Int,
    override val matcher: Matcher,
    override val commitSHA: String
) : WebLink(linkText, linkPath, proveniencePath, foundAtLineNumber, matcher, commitSHA) {

    override fun getPath(): String = matcher.group(12)
}

data class WebLinkToFile(
    override val linkText: String,
    override val linkPath: String,
    override val proveniencePath: String,
    override val foundAtLineNumber: Int,
    override val matcher: Matcher,
    override val commitSHA: String
) : WebLink(linkText, linkPath, proveniencePath, foundAtLineNumber, matcher, commitSHA) {

    override fun getPath(): String = matcher.group(12)
}

data class WebLinkToLine(
    override val linkText: String,
    override val linkPath: String,
    override val proveniencePath: String,
    override val foundAtLineNumber: Int,
    override val matcher: Matcher,
    override val commitSHA: String
) : WebLink(linkText, linkPath, proveniencePath, foundAtLineNumber, matcher, commitSHA) {

    override fun getPath(): String = matcher.group(11)

    fun getLineReferenced(): Int = matcher.group(12).toInt()
}


data class WebLinkToLines(
    override val linkText: String,
    override val linkPath: String,
    override val proveniencePath: String,
    override val foundAtLineNumber: Int,
    override val matcher: Matcher,
    override val commitSHA: String
) : WebLink(linkText, linkPath, proveniencePath, foundAtLineNumber, matcher, commitSHA) {

    override fun getPath(): String = matcher.group(11)

    fun getReferencedStartingLine(): Int = matcher.group(12).toInt()

    fun getReferencedEndingLine(): Int = matcher.group(13).toInt()

}