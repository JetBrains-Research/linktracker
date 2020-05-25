package org.intellij.plugin.tracker.data.links

import org.intellij.plugin.tracker.utils.LinkPatterns
import java.io.File
import java.util.regex.Matcher
import java.util.regex.Pattern

data class RelativeLinkToDirectory(
        override val linkInfo: LinkInfo,
        override val pattern: Pattern? = null,
        override var commitSHA: String? = null
        ) : RelativeLink(linkInfo, pattern) {

    override fun getReferencedFileName(): String {
        return ""
    }
}

data class RelativeLinkToFile(
        override val linkInfo: LinkInfo,
        override val pattern: Pattern? = null
) : RelativeLink(linkInfo, pattern) {
    override fun getReferencedFileName(): String {
        val file = File(linkInfo.linkPath)
        return file.name
    }
}


data class RelativeLinkToLine(
        override val linkInfo: LinkInfo,
        override val pattern: Pattern = LinkPatterns.RelativeLinkToLine.pattern
        ): RelativeLink(linkInfo, pattern) {
    override fun getReferencedFileName(): String {
        val file = File(linkInfo.linkPath)
        return file.name.replace("#L${matcher.group(1)}", "")
    }

    override fun getPath(): String {
        if (matcher.matches())
            return linkInfo.linkPath.replace("#L${getLineReferenced()}", "")
        return linkInfo.linkPath
    }

    fun getLineReferenced(): Int  = matcher.group(1).toInt()
}

data class RelativeLinkToLines(
        override val linkInfo: LinkInfo,
        override val pattern: Pattern = LinkPatterns.RelativeLinkToLines.pattern
        ) : RelativeLink(linkInfo, pattern) {
    override fun getReferencedFileName(): String {
        val file = File(linkInfo.linkPath)
        return file.name.replace("#L${matcher.group(1)}-L${matcher.group(2)}", "")
    }

    override fun getPath(): String {
        if (matcher.matches())
            return linkInfo.linkPath.replace(
                "#L${getStartLineReferenced()}-L${getEndLineReferenced()}", "")
        return linkInfo.linkPath
    }

    fun getStartLineReferenced(): Int = matcher.group(1).toInt()

    fun getEndLineReferenced(): Int = matcher.group(2).toInt()
}

fun checkRelativeLink(link: String): String {
    return checkSingleDot(checkDoubleDots(link))
}

fun checkDoubleDots(link: String): String {
    println("double dot $link")
    var result = link
    while(result.contains("..")) {
        if(result.startsWith("..")) {
            println("not valid link $result")
            return result
        }
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
    println("single dot $link")
    var result = link
    while(result.contains("/./") || result.endsWith("/.")) {
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