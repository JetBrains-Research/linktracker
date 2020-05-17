package org.intellij.plugin.tracker.data.links

import org.intellij.plugin.tracker.utils.LinkPatterns
import java.io.File
import java.util.regex.Pattern


data class WebLinkToDirectory(
    override val linkInfo: LinkInfo,
    override val pattern: Pattern = LinkPatterns.WebLinkToDirectory.pattern,
    override var commitSHA: String? = null
    ) : WebLink(linkInfo, pattern) {

    override fun getReferencedFileName(): String {
        return ""
    }

    override fun getPath(): String = matcher.group(12)
}

data class WebLinkToFile(
    override val linkInfo: LinkInfo,
    override val pattern: Pattern = LinkPatterns.WebLinkToFile.pattern
    ) : WebLink(linkInfo, pattern) {
    override fun getReferencedFileName(): String = File(getPath()).name

    override fun getPath(): String = matcher.group(12)
}

data class WebLinkToLine(
    override val linkInfo: LinkInfo,
    override val pattern: Pattern = LinkPatterns.WebLinkToLine.pattern
    ) : WebLink(linkInfo, pattern) {
    override fun getReferencedFileName(): String = File(getPath()).name.replace("#L${matcher.group(12)}", "")

    override fun getPath(): String {
        matcher.matches()
        return matcher.group(11)
    }

    fun getLineReferenced(): Int = matcher.group(12).toInt()
}


data class WebLinkToLines(
    override val linkInfo: LinkInfo,
    override val pattern: Pattern = LinkPatterns.WebLinkToLines.pattern
    ) : WebLink(linkInfo, pattern) {
    override fun getReferencedFileName(): String =
        File(getPath()).name.replace("#L${matcher.group(12)}-L${matcher.group(13)}", "")

    override fun getPath(): String = matcher.group(11)

    fun getReferencedStartingLine(): Int = matcher.group(12).toInt()

    fun getReferencedEndingLine(): Int = matcher.group(13).toInt()
}