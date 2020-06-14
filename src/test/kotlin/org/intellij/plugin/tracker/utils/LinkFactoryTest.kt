package org.intellij.plugin.tracker.utils

import com.nhaarman.mockitokotlin2.mock
import junit.framework.TestCase
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.data.links.LinkInfo
import org.intellij.plugin.tracker.data.links.NotSupportedLink
import org.intellij.plugin.tracker.data.links.RelativeLinkToFile
import org.intellij.plugin.tracker.data.links.RelativeLinkToLine
import org.intellij.plugin.tracker.data.links.RelativeLinkToLines
import org.intellij.plugin.tracker.data.links.WebLinkToDirectory
import org.intellij.plugin.tracker.data.links.WebLinkToFile
import org.intellij.plugin.tracker.data.links.WebLinkToLine
import org.intellij.plugin.tracker.data.links.WebLinkToLines
import org.junit.jupiter.api.Assertions

class LinkFactoryTest : TestCase() {

    fun testCreateRelativeLinkToFile() {
        val linkInfo = LinkInfo(
            linkText = "dummy text",
            linkPath = "file.txt",
            proveniencePath = "dummypath.md",
            foundAtLineNumber = 1,
            textOffset = 33,
            fileName = "dummypath.md",
            project = mock()
        )

        val resultLink: Link = LinkFactory.createLink(linkInfo)
        Assertions.assertEquals(RelativeLinkToFile(linkInfo), resultLink)
    }

    fun testCreateRelativeLinkToLine() {
        val linkInfo = LinkInfo(
            linkText = "dummy text",
            linkPath = "file.txt#L22",
            proveniencePath = "dummypath.md",
            foundAtLineNumber = 1,
            textOffset = 33,
            fileName = "dummypath.md",
            project = mock()
        )

        val resultLink: Link = LinkFactory.createLink(linkInfo)
        Assertions.assertEquals(RelativeLinkToLine(linkInfo), resultLink)
    }

    fun testCreateRelativeLinkToLines() {
        val linkInfo = LinkInfo(
            linkText = "dummy text",
            linkPath = "file.txt#L22-L25",
            proveniencePath = "dummypath.md",
            foundAtLineNumber = 1,
            textOffset = 33,
            fileName = "dummypath.md",
            project = mock()
        )

        val resultLink: Link = LinkFactory.createLink(linkInfo)
        Assertions.assertEquals(RelativeLinkToLines(linkInfo), resultLink)
    }

    fun testCreateWebLinkToFile() {
        val linkInfo = LinkInfo(
            linkText = "dummy text",
            linkPath = "https://github.com/tudorpopovici1/demo-plugin-jetbrains-project/blob/cf925c192b45c9310a2dcc874573f393024f3be2/src/main/java/actions/MarkdownAction.java",
            proveniencePath = "dummypath.md",
            foundAtLineNumber = 1,
            textOffset = 33,
            fileName = "dummypath.md",
            project = mock()
        )

        val resultLink: Link = LinkFactory.createLink(linkInfo)
        Assertions.assertEquals(WebLinkToFile(linkInfo), resultLink)
    }

    fun testCreateWebLinkToLine() {
        val linkInfo = LinkInfo(
            linkText = "dummy text",
            linkPath = "https://github.com/tudorpopovici1/demo-plugin-jetbrains-project/blob/cf925c192b45c9310a2dcc874573f393024f3be2/src/main/java/actions/MarkdownAction.java#L22",
            proveniencePath = "dummypath.md",
            foundAtLineNumber = 1,
            textOffset = 33,
            fileName = "dummypath.md",
            project = mock()
        )

        val resultLink: Link = LinkFactory.createLink(linkInfo)
        Assertions.assertEquals(WebLinkToLine(linkInfo), resultLink)
    }

    fun testCreateWebLinkToLines() {
        val linkInfo = LinkInfo(
            linkText = "dummy text",
            linkPath = "https://github.com/tudorpopovici1/demo-plugin-jetbrains-project/blob/cf925c192b45c9310a2dcc874573f393024f3be2/src/main/java/actions/MarkdownAction.java#L22-L25",
            proveniencePath = "dummypath.md",
            foundAtLineNumber = 1,
            textOffset = 33,
            fileName = "dummypath.md",
            project = mock()
        )

        val resultLink: Link = LinkFactory.createLink(linkInfo)
        Assertions.assertEquals(WebLinkToLines(linkInfo), resultLink)
    }

    fun testCreateWebLinkToDirectory() {
        val linkInfo = LinkInfo(
            linkText = "dummy text",
            linkPath = "https://github.com/tudorpopovici1/demo-plugin-jetbrains-project/tree/cf925c192b45c9310a2dcc874573f393024f3be2/src/main/java/actions/",
            proveniencePath = "dummypath.md",
            foundAtLineNumber = 1,
            textOffset = 33,
            fileName = "dummypath.md",
            project = mock()
        )

        val resultLink: Link = LinkFactory.createLink(linkInfo)
        Assertions.assertEquals(WebLinkToDirectory(linkInfo), resultLink)
    }

    fun testCreateWebLinkToLineInvalidLines() {
        val linkInfo = LinkInfo(
            linkText = "dummy text",
            linkPath = "https://github.com/tudorpopovici1/demo-plugin-jetbrains-project/tree/cf925c192b45c9310a2dcc874573f393024f3be2/src/main/java/actions/Mark.java#L25-L22",
            proveniencePath = "dummypath.md",
            foundAtLineNumber = 1,
            textOffset = 33,
            fileName = "dummypath.md",
            project = mock()
        )

        val resultLink: Link = LinkFactory.createLink(linkInfo)
        Assertions.assertEquals(
            NotSupportedLink(linkInfo, errorMessage = "This type of web link is not supported"),
            resultLink
        )
    }

    fun testCreateInvalidWebLink1() {
        val linkInfo = LinkInfo(
            linkText = "dummy text",
            linkPath = "https://github.com/tudorpopovici1/demo-plugin-jetbrains-project",
            proveniencePath = "dummypath.md",
            foundAtLineNumber = 1,
            textOffset = 33,
            fileName = "dummypath.md",
            project = mock()
        )

        val resultLink: Link = LinkFactory.createLink(linkInfo)
        Assertions.assertEquals(
            NotSupportedLink(linkInfo, errorMessage = "This type of web link is not supported"),
            resultLink
        )
    }

    fun testCreateInvalidWebLink2() {
        val linkInfo = LinkInfo(
            linkText = "dummy text",
            linkPath = "https://github.com/tudorpopovici1.com",
            proveniencePath = "dummypath.md",
            foundAtLineNumber = 1,
            textOffset = 33,
            fileName = "dummypath.md",
            project = mock()
        )

        val resultLink: Link = LinkFactory.createLink(linkInfo)
        Assertions.assertEquals(
            NotSupportedLink(linkInfo, errorMessage = "This type of web link is not supported"),
            resultLink
        )
    }

    fun testCreateWebLinkToFileWWWPrefix() {
        val linkInfo = LinkInfo(
            linkText = "dummy text",
            linkPath = "www.github.com/tudorpopovici1/demo-plugin-jetbrains-project/tree/cf925c192b45c9310a2dcc874573f393024f3be2/src/main/java/actions/Mark",
            proveniencePath = "dummypath.md",
            foundAtLineNumber = 1,
            textOffset = 33,
            fileName = "dummypath.md",
            project = mock()
        )

        val resultLink: Link = LinkFactory.createLink(linkInfo)
        Assertions.assertEquals(WebLinkToDirectory(linkInfo), resultLink)
    }
}
