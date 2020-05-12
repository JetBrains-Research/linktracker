package org.intellij.plugin.tracker.data.links

import org.intellij.plugin.tracker.utils.LinkPatterns
import java.util.regex.Matcher
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

fun checkRelativeLink(link: String): String {
    var newLink = link;
    while(newLink.contains("..")) {
        val pattern = Pattern.compile("((([a-zA-Z./]+)/)*)(([a-zA-Z]+)/../)([a-zA-Z./]+)")
        val matcher: Matcher = pattern.matcher(newLink)
        if(matcher.matches()) {
            val firstPart = matcher.group(2)
            val secondPart = matcher.group(6)
            newLink = if(firstPart==null) {
                secondPart
            } else {
                firstPart+secondPart
            }
        } else {
            val lastPattern = Pattern.compile("(([a-zA-Z/]+)/([a-zA-Z]+)/..)")
            val lastMatcher: Matcher = lastPattern.matcher(newLink)
            if(lastMatcher.matches()) {
                newLink = lastMatcher.group(2)
            }
        }
    }
    while(newLink.contains("/.")) {
        val pattern = Pattern.compile("(([a-zA-Z/]+)/./([a-zA-Z/.]+))")
        val matcher: Matcher = pattern.matcher(newLink)
        if(matcher.matches()) {
            val firstPart = matcher.group(2)
            val secondPart = matcher.group(3)
            newLink = "$firstPart/$secondPart"
        } else {
            val lastPattern = Pattern.compile("(([a-zA-Z/]+)/.)")
            val lastMatcher: Matcher = lastPattern.matcher(newLink)
            if(lastMatcher.matches()) {
                newLink = lastMatcher.group(2)
            }
        }
    }
    return newLink
}