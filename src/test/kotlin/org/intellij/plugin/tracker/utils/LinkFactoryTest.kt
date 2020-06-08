package org.intellij.plugin.tracker.utils

import com.nhaarman.mockitokotlin2.mock
import org.intellij.plugin.tracker.data.links.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test


class LinkFactoryTest {

    @Test
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

        val resultLink: Link = LinkFactory.createLink(linkInfo, null)
        Assertions.assertEquals(RelativeLinkToFile(linkInfo), resultLink)
    }

    @Test
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

        val resultLink: Link = LinkFactory.createLink(linkInfo, null)
        Assertions.assertEquals(RelativeLinkToLine(linkInfo), resultLink)
    }

    @Test
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

        val resultLink: Link = LinkFactory.createLink(linkInfo, null)
        Assertions.assertEquals(RelativeLinkToLines(linkInfo), resultLink)
    }

    @Test
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

        val resultLink: Link = LinkFactory.createLink(linkInfo, null)
        Assertions.assertEquals(WebLinkToFile(linkInfo), resultLink)
    }

    @Test
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

        val resultLink: Link = LinkFactory.createLink(linkInfo, null)
        Assertions.assertEquals(WebLinkToLine(linkInfo), resultLink)
    }

    @Test
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

        val resultLink: Link = LinkFactory.createLink(linkInfo, null)
        Assertions.assertEquals(WebLinkToLines(linkInfo), resultLink)
    }

    @Test
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

        val resultLink: Link = LinkFactory.createLink(linkInfo, null)
        Assertions.assertEquals(WebLinkToDirectory(linkInfo), resultLink)
    }

    @Test
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

        val resultLink: Link = LinkFactory.createLink(linkInfo, null)
        Assertions.assertEquals(NotSupportedLink(linkInfo, errorMessage = "This type of web link is not supported"), resultLink)
    }

    @Test
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

        val resultLink: Link = LinkFactory.createLink(linkInfo, null)
        Assertions.assertEquals(NotSupportedLink(linkInfo, errorMessage = "This type of web link is not supported"), resultLink)
    }

    @Test
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

        val resultLink: Link = LinkFactory.createLink(linkInfo, null)
        Assertions.assertEquals(NotSupportedLink(linkInfo, errorMessage = "This type of web link is not supported"), resultLink)
    }

    @Test
    fun testCreateValidWebLinkToFileStartsWithWWW() {
        val linkInfo = LinkInfo(
            linkText = "dummy text",
            linkPath = "www.github.com/tudorpopovici1/demo-plugin-jetbrains-project/tree/cf925c192b45c9310a2dcc874573f393024f3be2/src/main/java/actions/Mark",
            proveniencePath = "dummypath.md",
            foundAtLineNumber = 1,
            textOffset = 33,
            fileName = "dummypath.md",
            project = mock()
        )

        val resultLink: Link = LinkFactory.createLink(linkInfo, null)
        Assertions.assertEquals(WebLinkToDirectory(linkInfo), resultLink)
    }

}