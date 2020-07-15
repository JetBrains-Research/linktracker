package org.intellij.plugin.tracker.data.links

import com.nhaarman.mockitokotlin2.mock
import junit.framework.TestCase
import org.intellij.plugin.tracker.data.diff.Line
import org.intellij.plugin.tracker.data.changes.CustomChange
import org.intellij.plugin.tracker.data.changes.CustomChangeType
import org.intellij.plugin.tracker.data.changes.LineChange
import org.intellij.plugin.tracker.data.changes.LineChangeType
import org.intellij.plugin.tracker.core.update.LinkElementImpl
import org.intellij.plugin.tracker.utils.LinkPatterns
import org.junit.jupiter.api.Assertions

/**
 * This class tests the web links.
 */
class WebLinksTest : TestCase() {

    private lateinit var linkInfo: LinkInfo
    private lateinit var webLinkToFile: WebLinkToFile
    private lateinit var webLinkToDirectory: WebLinkToDirectory
    private lateinit var webLinkToLine: WebLinkToLine
    private lateinit var webLinkToLines: WebLinkToLines

    override fun setUp() {
        super.setUp()
        linkInfo = LinkInfo(
            linkText = "dummy text",
            linkPath = "https://gitlab.ewi.tudelft.nl/project_owner/project/-/blob/branch/README.md",
            proveniencePath = "dummy/../path/./dummypath.md",
            foundAtLineNumber = 1,
            linkElement = LinkElementImpl(mock()),
            fileName = "dummypath.md",
            project = mock()
        )

        webLinkToFile = WebLinkToFile(linkInfo, LinkPatterns.WebLinkToFile.pattern)
        webLinkToDirectory = WebLinkToDirectory(linkInfo.copy(linkPath = "https://gitlab.ewi.tudelft.nl/project_owner/" +
                "project/-/tree/branch/dummy/directory"), LinkPatterns.WebLinkToDirectory.pattern)
        webLinkToLine = WebLinkToLine(linkInfo.copy(linkPath = "https://github.com/project_owner/project/blob/sha/src/" +
                "main/java/actions/MarkdownAction.java#L55"), LinkPatterns.WebLinkToLine.pattern)
        webLinkToLines = WebLinkToLines(linkInfo.copy(linkPath = "https://github.com/project_owner/project/blob/sha/src/" +
                "main/java/actions/MarkdownAction.java#L52-L71"), LinkPatterns.WebLinkToLines.pattern)
    }

    fun testUpdateLink() {
        val customChange = CustomChange(
            customChangeType = CustomChangeType.ADDED,
            afterPathString = "after path"
        )
        webLinkToFile.referenceType = WebLinkReferenceType.COMMIT
        Assertions.assertEquals(webLinkToFile.updateLink(customChange, null), null)
        Assertions.assertEquals(webLinkToFile.updateLink(customChange, "sha"), "https://gitlab.ewi.tudelft.nl/project_owner/project/-/blob/sha/after path")
    }

    fun testLineReferenced() {
        Assertions.assertEquals(webLinkToFile.lineReferenced, -1)
        Assertions.assertEquals(webLinkToDirectory.lineReferenced, -1)
        Assertions.assertEquals(webLinkToLine.lineReferenced, 55)
        Assertions.assertEquals(webLinkToLines.lineReferenced, -1)
    }

    fun testReferencedFileName() {
        Assertions.assertEquals(webLinkToFile.referencedFileName, "README.md")
        Assertions.assertEquals(webLinkToDirectory.referencedFileName, "")
        Assertions.assertEquals(webLinkToLine.referencedFileName, "MarkdownAction.java")
        Assertions.assertEquals(webLinkToLines.referencedFileName, "MarkdownAction.java")
    }

    fun testReferencedStartingLine() {
        Assertions.assertEquals(webLinkToFile.referencedStartingLine, -1)
        Assertions.assertEquals(webLinkToDirectory.referencedStartingLine, -1)
        Assertions.assertEquals(webLinkToLine.referencedStartingLine, -1)
        Assertions.assertEquals(webLinkToLines.referencedStartingLine, 52)
    }

    fun testReferencedEndingLine() {
        Assertions.assertEquals(webLinkToFile.referencedEndingLine, -1)
        Assertions.assertEquals(webLinkToDirectory.referencedEndingLine, -1)
        Assertions.assertEquals(webLinkToLine.referencedEndingLine, -1)
        Assertions.assertEquals(webLinkToLines.referencedEndingLine, 71)
    }

    fun testPath() {
        Assertions.assertEquals(webLinkToFile.path, "README.md")
        Assertions.assertEquals(WebLinkToFile(linkInfo.copy(linkPath = "invalid")).path, "invalid")

        Assertions.assertEquals(webLinkToDirectory.path, "dummy/directory")

        Assertions.assertEquals(webLinkToLine.path, "src/main/java/actions/MarkdownAction.java")
        Assertions.assertEquals(WebLinkToLine(linkInfo.copy(linkPath = "invalid")).path, "invalid")

        Assertions.assertEquals(webLinkToLines.path, "src/main/java/actions/MarkdownAction.java")
        Assertions.assertEquals(WebLinkToLines(linkInfo.copy(linkPath = "invalid")).path, "invalid")
    }

    fun testGenerateNewPath() {
        Assertions.assertEquals(webLinkToFile.generateNewPath(CustomChange(CustomChangeType.MOVED,
            "dummy.md"), "dummy/README.md"), "dummy/dummy.md")
        Assertions.assertEquals(webLinkToDirectory.generateNewPath(CustomChange(CustomChangeType.MOVED,
            "new directory"), "new/dummy/directory"), "new/new directory")
        Assertions.assertEquals(webLinkToLine.generateNewPath(LineChange(CustomChange(CustomChangeType.MOVED,
            "dummy.md"), LineChangeType.DELETED), "new path"), null)
        Assertions.assertEquals(webLinkToLine.generateNewPath(LineChange(CustomChange(CustomChangeType.MOVED, "dummy.md"),
            LineChangeType.DELETED, "error",
            Line(3, "line content")
        ), "new path"), "new path")
    }

    fun testCopyWithAfterPath() {
        Assertions.assertEquals(webLinkToFile.copyWithAfterPath(webLinkToDirectory,
            "test"), WebLinkToFile(linkInfo.copy(linkPath = "test")))
        Assertions.assertEquals(webLinkToDirectory.copyWithAfterPath(webLinkToFile,
            "test"), WebLinkToDirectory(linkInfo.copy(linkPath = "test")))
        Assertions.assertEquals(webLinkToLine.copyWithAfterPath(webLinkToLine,
            "test"), WebLinkToLine(linkInfo.copy(linkPath = "test")))
        Assertions.assertEquals(webLinkToLines.copyWithAfterPath(webLinkToLines,
            "test"), WebLinkToLines(linkInfo.copy(linkPath = "test")))
    }

    fun testIsPermalink() {
        webLinkToFile.referenceType = WebLinkReferenceType.COMMIT
        Assertions.assertEquals(webLinkToFile.isPermalink(), true)
        webLinkToDirectory.referenceType = WebLinkReferenceType.BRANCH
        Assertions.assertEquals(webLinkToDirectory.isPermalink(), false)
    }

    fun testGetter() {
        Assertions.assertEquals(webLinkToFile.platformName, "gitlab.ewi.tudelft.nl")
        Assertions.assertEquals(webLinkToFile.projectName, "project")
        Assertions.assertEquals(webLinkToFile.projectOwnerName, "project_owner")
        Assertions.assertEquals(webLinkToFile.referencingName, "branch")
        Assertions.assertEquals(webLinkToFile.correspondsToLocalProject("github.com"), false)
        Assertions.assertEquals(webLinkToFile.markdownFileMoved(""), false)
    }
}
