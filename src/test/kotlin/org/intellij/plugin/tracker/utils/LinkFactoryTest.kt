package org.intellij.plugin.tracker.utils

import com.nhaarman.mockitokotlin2.mock
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
import org.junit.jupiter.api.Test

class LinkFactoryTest {

    @Test
    fun `create a relative link to a file`() {
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
    fun `create a relative link to a line`() {
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
    fun `create a relative link to multiple lines`() {
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
    fun `create a web link to a file`() {
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
    fun `create a web link to a line`() {
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
    fun `create a web link to multiple lines`() {
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
    fun `create a web link to a directory`() {
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
    fun `create a web link to a line with invalid lines`() {
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
    fun `create an invalid web link 1`() {
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
    fun `create an invalid web link 2`() {
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
    fun `create a web link to a file with 'www' prefix`() {
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
