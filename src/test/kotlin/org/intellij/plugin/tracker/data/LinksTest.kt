package org.intellij.plugin.tracker.data

import com.nhaarman.mockitokotlin2.mock
import junit.framework.TestCase
import org.intellij.plugin.tracker.data.links.LinkInfo
import org.intellij.plugin.tracker.data.links.RelativeLinkToDirectory
import org.intellij.plugin.tracker.data.links.RelativeLinkToFile
import org.intellij.plugin.tracker.data.links.RelativeLinkToLine
import org.intellij.plugin.tracker.data.links.RelativeLinkToLines
import org.intellij.plugin.tracker.data.links.WebLinkToFile
import org.intellij.plugin.tracker.utils.LinkElementImpl
import org.intellij.plugin.tracker.utils.LinkPatterns
import org.junit.jupiter.api.Assertions

class LinksTest : TestCase() {

    fun testRelativeLinkToFile() {
        val linkInfo = LinkInfo(
            linkText = "dummy text",
            linkPath = "dummy/file.txt",
            proveniencePath = "dummy/../path/./dummypath.md",
            foundAtLineNumber = 1,
            linkElement = LinkElementImpl(mock()),
            fileName = "dummypath.md",
            project = mock()
        )
        val link = RelativeLinkToFile(linkInfo)
        Assertions.assertEquals(link.lineReferenced, -1)
        Assertions.assertEquals(link.referencedFileName, "file.txt")
        Assertions.assertEquals(link.path, "path/dummy/file.txt")
        Assertions.assertEquals(link.referencedStartingLine, -1)
        Assertions.assertEquals(link.referencedEndingLine, -1)
        Assertions.assertEquals(link.copyWithAfterPath(link, link.linkInfo.linkPath), link)
        Assertions.assertEquals(link.markdownFileMoved(""), false)
    }

    fun testRelativeLinkToDirectory() {
        val linkInfo = LinkInfo(
            linkText = "dummy text",
            linkPath = "dummy/directory",
            proveniencePath = "dummy/../path/./dummypath.md",
            foundAtLineNumber = 5,
            linkElement = LinkElementImpl(mock()),
            fileName = "dummypath.md",
            project = mock()
        )
        val link = RelativeLinkToDirectory(linkInfo)
        Assertions.assertEquals(link.lineReferenced, -1)
        Assertions.assertEquals(link.referencedStartingLine, -1)
        Assertions.assertEquals(link.referencedEndingLine, -1)
        Assertions.assertEquals(link.referencedFileName, "")
        Assertions.assertEquals(link.path, "path/dummy/directory")
        Assertions.assertEquals(link.markdownFileMoved(""), false)
        Assertions.assertEquals(link.copyWithAfterPath(link, link.linkInfo.linkPath), link)
    }

    fun testRelativeLinkToLine() {
        val linkInfo = LinkInfo(
            linkText = "dummy text",
            linkPath = "dummy/file.md#L7",
            proveniencePath = "dummy/../path/./dummypath.md",
            foundAtLineNumber = 5,
            linkElement = LinkElementImpl(mock()),
            fileName = "dummypath.md",
            project = mock()
        )
        val link = RelativeLinkToLine(linkInfo, LinkPatterns.RelativeLinkToLine.pattern)
        Assertions.assertEquals(link.lineReferenced, 7)
        Assertions.assertEquals(link.referencedFileName, "file.md")
        Assertions.assertEquals(link.referencedStartingLine, -1)
        Assertions.assertEquals(link.referencedEndingLine, -1)
        Assertions.assertEquals(link.path, "path/dummy/file.md")
        Assertions.assertEquals(link.copyWithAfterPath(link, link.linkInfo.linkPath), link)
    }

    fun testRelativeLinkToLines() {
        val linkInfo = LinkInfo(
            linkText = "dummy text",
            linkPath = "dummy/file.md#L7-L12",
            proveniencePath = "dummy/../path/./dummypath.md",
            foundAtLineNumber = 5,
            linkElement = LinkElementImpl(mock()),
            fileName = "dummypath.md",
            project = mock()
        )
        val link = RelativeLinkToLines(linkInfo, LinkPatterns.RelativeLinkToLines.pattern)
        Assertions.assertEquals(link.lineReferenced, -1)
        Assertions.assertEquals(link.referencedFileName, "file.md")
        Assertions.assertEquals(link.referencedStartingLine, 7)
        Assertions.assertEquals(link.referencedEndingLine, 12)
        Assertions.assertEquals(link.path, "path/dummy/file.md")
        Assertions.assertEquals(link.copyWithAfterPath(link, link.linkInfo.linkPath), link)
    }

    fun testWebLinkToFile() {
        val linkInfo = LinkInfo(
            linkText = "dummy text",
            linkPath = "https://gitlab.ewi.tudelft.nl/cse2000-software-project/2019-2020-q4/cluster-0/tracking-changes-in-links-to-code/tracking-changes-in-links-to-code/-/blob/add_regex_to_link_parsing/README.md",
            proveniencePath = "dummy/../path/./dummypath.md",
            foundAtLineNumber = 5,
            linkElement = LinkElementImpl(mock()),
            fileName = "dummypath.md",
            project = mock()
        )
        val link = WebLinkToFile(linkInfo, LinkPatterns.WebLinkToFile.pattern)
        Assertions.assertEquals(link.lineReferenced, -1)
        Assertions.assertEquals(link.referencedFileName, "README.md")
        Assertions.assertEquals(link.referencedStartingLine, -1)
        Assertions.assertEquals(link.referencedEndingLine, -1)
        Assertions.assertEquals(link.path, "README.md")
        Assertions.assertEquals(link.copyWithAfterPath(link, link.linkInfo.linkPath), link)
    }
}
