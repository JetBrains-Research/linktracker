package org.intellij.plugin.tracker.data.links

import com.nhaarman.mockitokotlin2.mock
import junit.framework.TestCase
import org.intellij.plugin.tracker.data.Line
import org.intellij.plugin.tracker.data.changes.CustomChange
import org.intellij.plugin.tracker.data.changes.CustomChangeType
import org.intellij.plugin.tracker.data.changes.LineChange
import org.intellij.plugin.tracker.data.changes.LineChangeType
import org.intellij.plugin.tracker.data.changes.LinesChange
import org.intellij.plugin.tracker.data.changes.LinesChangeType
import org.intellij.plugin.tracker.utils.LinkElementImpl
import org.intellij.plugin.tracker.utils.LinkPatterns
import org.junit.jupiter.api.Assertions

/**
 * This class tests the relative links.
 */
class RelativeLinksTest : TestCase() {

    private lateinit var linkInfo: LinkInfo
    private lateinit var relativeLinkToFile: RelativeLinkToFile
    private lateinit var relativeLinkToDirectory: RelativeLinkToDirectory
    private lateinit var relativeLinkToLine: RelativeLinkToLine
    private lateinit var relativeLinkToLines: RelativeLinkToLines

    override fun setUp() {
        super.setUp()
        linkInfo = LinkInfo(
            linkText = "dummy text",
            linkPath = "dummy/file.md",
            proveniencePath = "dummy/../path/./dummypath.md",
            foundAtLineNumber = 1,
            linkElement = LinkElementImpl(mock()),
            fileName = "dummypath.md",
            project = mock()
        )

        relativeLinkToFile = RelativeLinkToFile(linkInfo)
        relativeLinkToDirectory = RelativeLinkToDirectory(linkInfo.copy(linkPath = "dummy/new%20directory"))
        relativeLinkToLine = RelativeLinkToLine(linkInfo.copy(linkPath = "dummy/file.md#L7"), LinkPatterns.RelativeLinkToLine.pattern)
        relativeLinkToLines = RelativeLinkToLines(linkInfo.copy(linkPath = "dummy/file.md#L7-L12"), LinkPatterns.RelativeLinkToLines.pattern)
    }

    fun testUpdateLink() {
        val customChange = CustomChange(
            customChangeType = CustomChangeType.ADDED,
            afterPathString = "after path"
        )
        Assertions.assertEquals(relativeLinkToFile.updateLink(customChange, "sha"), "../after path")
        Assertions.assertEquals(relativeLinkToDirectory.updateLink(customChange, "sha"), "after path")

        val lineChange = LineChange(
            fileChange = customChange,
            lineChangeType = LineChangeType.MOVED,
            newLine = Line(5, "line text")
        )
        Assertions.assertEquals(relativeLinkToLine.updateLink(lineChange, "sha"), "after path#L5")

        val linesChange = LinesChange(
            fileChange = customChange,
            linesChangeType = LinesChangeType.PARTIAL,
            newLines = mutableListOf(mutableListOf(Line(3, "line text"), Line(5, "text")))
        )
        Assertions.assertEquals(relativeLinkToLines.updateLink(linesChange, "sha"), "after path#L3-L5")
    }

    fun testLineReferenced() {
        Assertions.assertEquals(relativeLinkToFile.lineReferenced, -1)
        Assertions.assertEquals(relativeLinkToDirectory.lineReferenced, -1)
        Assertions.assertEquals(relativeLinkToLine.lineReferenced, 7)
        Assertions.assertEquals(relativeLinkToLines.lineReferenced, -1)
    }

    fun testReferencedStartingLine() {
        Assertions.assertEquals(relativeLinkToFile.referencedStartingLine, -1)
        Assertions.assertEquals(relativeLinkToDirectory.referencedStartingLine, -1)
        Assertions.assertEquals(relativeLinkToLine.referencedStartingLine, -1)
        Assertions.assertEquals(relativeLinkToLines.referencedStartingLine, 7)
    }

    fun testReferencedEndingLine() {
        Assertions.assertEquals(relativeLinkToFile.referencedEndingLine, -1)
        Assertions.assertEquals(relativeLinkToDirectory.referencedEndingLine, -1)
        Assertions.assertEquals(relativeLinkToLine.referencedEndingLine, -1)
        Assertions.assertEquals(relativeLinkToLines.referencedEndingLine, 12)
    }

    fun testReferencedFileName() {
        Assertions.assertEquals(relativeLinkToFile.referencedFileName, "file.md")
        Assertions.assertEquals(relativeLinkToDirectory.referencedFileName, "")
        Assertions.assertEquals(relativeLinkToLine.referencedFileName, "file.md")
        Assertions.assertEquals(relativeLinkToLines.referencedFileName, "file.md")
    }

    fun testPath() {
        Assertions.assertEquals(relativeLinkToFile.path, "path/dummy/file.md")
        Assertions.assertEquals(relativeLinkToDirectory.path, "path/dummy/new directory")

        Assertions.assertEquals(relativeLinkToLine.path, "path/dummy/file.md")

        Assertions.assertEquals(relativeLinkToLines.path, "path/dummy/file.md")
        Assertions.assertEquals(RelativeLinkToLines(linkInfo.copy(linkPath = "invalid")).path, "path/invalid")
    }

    fun testCopyWithAfterPath() {
        Assertions.assertEquals(relativeLinkToFile.copyWithAfterPath(relativeLinkToDirectory,
            "test"), RelativeLinkToFile(linkInfo.copy(linkPath = "test")))
        Assertions.assertEquals(relativeLinkToDirectory.copyWithAfterPath(relativeLinkToFile,
            "test"), RelativeLinkToDirectory(linkInfo.copy(linkPath = "test")))
        Assertions.assertEquals(relativeLinkToLine.copyWithAfterPath(relativeLinkToLine,
            "test"), RelativeLinkToLine(linkInfo.copy(linkPath = "test")))
        Assertions.assertEquals(relativeLinkToLines.copyWithAfterPath(relativeLinkToLines,
            "test"), RelativeLinkToLines(linkInfo.copy(linkPath = "test")))
    }

    fun testMarkdownFileMoved() {
        Assertions.assertEquals(relativeLinkToFile.markdownFileMoved("../../test"), true)
        Assertions.assertEquals(relativeLinkToDirectory.markdownFileMoved("test"), false)
    }
}
