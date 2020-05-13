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

    fun getLineReferenced(): Int = matcher.group(1).toInt()
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
    return checkSingleDot(checkDoubleDots(link))
}

fun checkDoubleDots(link: String): String {
    var result = link
    while(result.contains("..")) {
        val matcher: Matcher = LinkPatterns.RelativeLinkWithDoubleDots.pattern.matcher(result)
        if(matcher.matches()) {
            val firstPart = matcher.group(2)
            val secondPart = matcher.group(6)
            result = if(firstPart==null) {
                secondPart
            } else {
                firstPart+secondPart
            }
        } else {
            val endMatcher: Matcher = LinkPatterns.RelativeLinkWithDoubleDotsAtEnd.pattern.matcher(result)
            if(endMatcher.matches()) {
                result = endMatcher.group(2)
            }
        }
    }
    return result
}

fun checkSingleDot(link: String): String {
    var result = link
    while(result.contains("/.")) {
        val matcher: Matcher = LinkPatterns.RelativeLinkWithSingleDot.pattern.matcher(result)
        if(matcher.matches()) {
            val firstPart = matcher.group(2)
            val secondPart = matcher.group(3)
            result = "$firstPart/$secondPart"
        } else {
            val endMatcher: Matcher = LinkPatterns.RelativeLinkWithSingleDotAtEnd.pattern.matcher(result)
            if(endMatcher.matches()) {
                result = endMatcher.group(2)
            }
        }
    }
    return result
}