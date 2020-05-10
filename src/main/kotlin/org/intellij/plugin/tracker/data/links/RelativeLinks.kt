package org.intellij.plugin.tracker.data.links

import org.intellij.plugin.tracker.utils.LinkPatterns
import java.util.regex.Pattern


data class RelativeLinkToDirectory(
    override val linkInfo: LinkInfo,
    override val pattern: Pattern? = null,
    override val commitSHA: String
) : RelativeLink(linkInfo, pattern, commitSHA) {
    override fun getPath(): String {
        return linkInfo.linkPath
    }
}


data class RelativeLinkToFile(
    override val linkInfo: LinkInfo,
    override val pattern: Pattern? = null,
    override val commitSHA: String
) : RelativeLink(linkInfo, pattern, commitSHA) {
    override fun getPath(): String {
        return linkInfo.linkPath
    }
}


data class RelativeLinkToLine(
    override val linkInfo: LinkInfo,
    override val pattern: Pattern = LinkPatterns.RelativeLinkToLine.pattern,
    override val commitSHA: String
) : RelativeLink(linkInfo, pattern, commitSHA) {

    fun getLineReferenced(): Int =  matcher.group(1).toInt()
}


data class RelativeLinkToLines(
    override val linkInfo: LinkInfo,
    override val pattern: Pattern = LinkPatterns.RelativeLinkToLines.pattern,
    override val commitSHA: String
) : RelativeLink(linkInfo, pattern, commitSHA) {

    fun getStartLineReferenced(): Int = matcher.group(1).toInt()

    fun getEndLineReferenced(): Int = matcher.group(2).toInt()
}