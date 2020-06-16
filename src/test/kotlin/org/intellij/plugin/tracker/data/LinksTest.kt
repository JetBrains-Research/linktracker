package org.intellij.plugin.tracker.data

import com.nhaarman.mockitokotlin2.mock
import junit.framework.TestCase
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.intellij.plugin.tracker.data.changes.CustomChange
import org.intellij.plugin.tracker.data.changes.CustomChangeType
import org.intellij.plugin.tracker.data.changes.LineChange
import org.intellij.plugin.tracker.data.changes.LineChangeType
import org.intellij.plugin.tracker.data.links.LinkInfo
import org.intellij.plugin.tracker.data.links.NotSupportedLink
import org.intellij.plugin.tracker.data.links.RelativeLinkToDirectory
import org.intellij.plugin.tracker.data.links.RelativeLinkToFile
import org.intellij.plugin.tracker.data.links.RelativeLinkToLine
import org.intellij.plugin.tracker.data.links.RelativeLinkToLines
import org.intellij.plugin.tracker.data.links.WebLinkReferenceType
import org.intellij.plugin.tracker.data.links.WebLinkToDirectory
import org.intellij.plugin.tracker.data.links.WebLinkToFile
import org.intellij.plugin.tracker.data.links.WebLinkToLine
import org.intellij.plugin.tracker.data.links.WebLinkToLines
import org.intellij.plugin.tracker.utils.LinkElementImpl
import org.intellij.plugin.tracker.utils.LinkPatterns
import org.junit.jupiter.api.Assertions
import java.lang.UnsupportedOperationException

class LinksTest : TestCase() {

    private val linkInfo = LinkInfo(
        linkText = "dummy text",
        linkPath = "dummy/file.txt",
        proveniencePath = "dummy/../path/./dummypath.md",
        foundAtLineNumber = 1,
        linkElement = LinkElementImpl(mock()),
        fileName = "dummypath.md",
        project = mock()
    )

    fun testRelativeLinkToFile() {
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
        val myLinkInfo = linkInfo
        myLinkInfo.linkPath = "dummy/new%20directory"
        val link = RelativeLinkToDirectory(myLinkInfo)
        Assertions.assertEquals(link.lineReferenced, -1)
        Assertions.assertEquals(link.referencedStartingLine, -1)
        Assertions.assertEquals(link.referencedEndingLine, -1)
        Assertions.assertEquals(link.referencedFileName, "")
        Assertions.assertEquals(link.path, "path/dummy/new directory")
        Assertions.assertEquals(link.relativePath, "path/dummy/new directory")
        Assertions.assertEquals(link.markdownFileMoved(""), false)
        Assertions.assertEquals(link.copyWithAfterPath(link, link.linkInfo.linkPath), link)
    }

    fun testRelativeLinkToLine() {
        val myLinkInfo = linkInfo
        myLinkInfo.linkPath = "dummy/file.md#L7"
        val link = RelativeLinkToLine(myLinkInfo, LinkPatterns.RelativeLinkToLine.pattern)
        Assertions.assertEquals(link.lineReferenced, 7)
        Assertions.assertEquals(link.referencedFileName, "file.md")
        Assertions.assertEquals(link.referencedStartingLine, -1)
        Assertions.assertEquals(link.referencedEndingLine, -1)
        Assertions.assertEquals(link.path, "path/dummy/file.md")
        Assertions.assertEquals(link.copyWithAfterPath(link, link.linkInfo.linkPath), link)
    }

    fun testRelativeLinkToLines() {
        val myLinkInfo = linkInfo
        myLinkInfo.linkPath = "dummy/file.md#L7-L12"
        val link = RelativeLinkToLines(myLinkInfo, LinkPatterns.RelativeLinkToLines.pattern)
        Assertions.assertEquals(link.lineReferenced, -1)
        Assertions.assertEquals(link.referencedFileName, "file.md")
        Assertions.assertEquals(link.referencedStartingLine, 7)
        Assertions.assertEquals(link.referencedEndingLine, 12)
        Assertions.assertEquals(link.path, "path/dummy/file.md")
        Assertions.assertEquals(link.copyWithAfterPath(link, link.linkInfo.linkPath), link)
    }

    fun testNotSupportedLink() {
        val myLinkInfo = linkInfo
        myLinkInfo.linkPath = "invalid link"
        val link = NotSupportedLink(myLinkInfo)
        Assertions.assertEquals(link.lineReferenced, -1)
        Assertions.assertEquals(link.referencedFileName, "")
        Assertions.assertEquals(link.referencedStartingLine, -1)
        Assertions.assertEquals(link.referencedEndingLine, -1)
        Assertions.assertEquals(link.path, "invalid link")
        Assertions.assertEquals(link.copyWithAfterPath(link, link.linkInfo.linkPath), link)
        assertThatExceptionOfType(UnsupportedOperationException::class.java).isThrownBy {
            link.markdownFileMoved("")
        }
    }

    fun testWebLinkToFile() {
        val myLinkInfo = linkInfo.copy()
        myLinkInfo.linkPath = "https://gitlab.ewi.tudelft.nl/cse2000-software-project/2019-2020-q4/cluster-0/" +
                "tracking-changes-in-links-to-code/tracking-changes-in-links-to-code/-/blob/add_regex_to_link_parsing/README.md"
        val otherLinkInfo = linkInfo.copy()
        otherLinkInfo.linkPath = "other style"
        val link = WebLinkToFile(myLinkInfo, LinkPatterns.WebLinkToFile.pattern)
        val otherLink = WebLinkToFile(otherLinkInfo, LinkPatterns.WebLinkToFile.pattern)
        Assertions.assertEquals(link.lineReferenced, -1)
        Assertions.assertEquals(link.path, "README.md")
        Assertions.assertEquals(otherLink.path, "other style")
        Assertions.assertEquals(link.referencedFileName, "README.md")
        Assertions.assertEquals(link.referencedStartingLine, -1)
        Assertions.assertEquals(link.referencedEndingLine, -1)
        Assertions.assertEquals(link.generateNewPath(CustomChange(CustomChangeType.MOVED,
            "dummy.md"), "dummy/README.md"), "dummy/dummy.md")
        Assertions.assertEquals(link.copyWithAfterPath(link, link.linkInfo.linkPath), link)
    }

    fun testWebLinkToLine() {
        val myLinkInfo = linkInfo.copy()
        myLinkInfo.linkPath = "https://github.com/tudorpopovici1/demo-plugin-jetbrains-project/blob/" +
                "cf925c192b45c9310a2dcc874573f393024f3be2/src/main/java/actions/MarkdownAction.java#L55"
        val otherLinkInfo = linkInfo.copy()
        otherLinkInfo.linkPath = "other style"
        val link = WebLinkToLine(myLinkInfo, LinkPatterns.WebLinkToLine.pattern)
        val otherLink = WebLinkToLine(otherLinkInfo, LinkPatterns.WebLinkToLine.pattern)
        Assertions.assertEquals(link.lineReferenced, 55)
        Assertions.assertEquals(link.path, "src/main/java/actions/MarkdownAction.java")
        Assertions.assertEquals(otherLink.path, "other style")
        Assertions.assertEquals(link.referencedFileName, "MarkdownAction.java")
        Assertions.assertEquals(link.referencedStartingLine, -1)
        Assertions.assertEquals(link.referencedEndingLine, -1)
        Assertions.assertEquals(link.generateNewPath(LineChange(CustomChange(CustomChangeType.MOVED,
            "dummy.md"), LineChangeType.DELETED), "new path"), null)
        Assertions.assertEquals(link.generateNewPath(LineChange(CustomChange(CustomChangeType.MOVED,
            "dummy.md"), LineChangeType.DELETED, "error", Line(3, "line content")),
            "new path"), "new path")
        Assertions.assertEquals(link.copyWithAfterPath(link, link.linkInfo.linkPath), link)
    }

    fun testWebLinkToLines() {
        val myLinkInfo = linkInfo.copy()
        myLinkInfo.linkPath = "https://github.com/tudorpopovici1/demo-plugin-jetbrains-project/blob/" +
                "cf925c192b45c9310a2dcc874573f393024f3be2/src/main/java/actions/MarkdownAction.java#L52-L71"
        val otherLinkInfo = linkInfo.copy()
        otherLinkInfo.linkPath = "other style"
        val link = WebLinkToLines(myLinkInfo, LinkPatterns.WebLinkToLines.pattern)
        val otherLink = WebLinkToLines(otherLinkInfo, LinkPatterns.WebLinkToLines.pattern)
        Assertions.assertEquals(link.lineReferenced, -1)
        Assertions.assertEquals(link.path, "src/main/java/actions/MarkdownAction.java")
        Assertions.assertEquals(otherLink.path, "other style")
        Assertions.assertEquals(link.referencedFileName, "MarkdownAction.java")
        Assertions.assertEquals(link.referencedStartingLine, 52)
        Assertions.assertEquals(link.referencedEndingLine, 71)
        Assertions.assertEquals(link.copyWithAfterPath(link, link.linkInfo.linkPath), link)
        link.referenceType = WebLinkReferenceType.BRANCH
        Assertions.assertEquals(link.isPermalink(), false)
    }

    fun testWebLinkToDirectory() {
        val myLinkInfo = linkInfo
        myLinkInfo.linkPath = "https://gitlab.ewi.tudelft.nl/project_owner/tracking-changes-in-links-to-code/-/tree/branch/dummy/directory"
        val link = WebLinkToDirectory(myLinkInfo, LinkPatterns.WebLinkToDirectory.pattern)

        Assertions.assertEquals(link.lineReferenced, -1)
        Assertions.assertEquals(link.path, "dummy/directory")
        Assertions.assertEquals(link.referencedFileName, "")
        Assertions.assertEquals(link.referencedStartingLine, -1)
        Assertions.assertEquals(link.referencedEndingLine, -1)
        Assertions.assertEquals(link.copyWithAfterPath(link, link.linkInfo.linkPath), link)
        Assertions.assertEquals(link.generateNewPath(CustomChange(CustomChangeType.MOVED,
            "new directory"), "new/dummy/directory"), "new/new directory")
        Assertions.assertEquals(link.platformName, "gitlab.ewi.tudelft.nl")
        Assertions.assertEquals(link.projectName, "tracking-changes-in-links-to-code")
        Assertions.assertEquals(link.projectOwnerName, "project_owner")
        Assertions.assertEquals(link.referencingName, "branch")
        Assertions.assertEquals(link.correspondsToLocalProject("github.com"), false)
        Assertions.assertEquals(link.markdownFileMoved(""), false)
        link.referenceType = WebLinkReferenceType.COMMIT
        Assertions.assertEquals(link.isPermalink(), true)
    }
}
