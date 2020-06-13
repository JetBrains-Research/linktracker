package org.intellij.plugin.tracker.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.intellij.plugin.tracker.data.Line
import org.intellij.plugin.tracker.data.changes.Change
import org.intellij.plugin.tracker.data.changes.CustomChange
import org.intellij.plugin.tracker.data.changes.CustomChangeType
import org.intellij.plugin.tracker.data.changes.LineChange
import org.intellij.plugin.tracker.data.changes.LineChangeType
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.data.links.LinkInfo
import org.intellij.plugin.tracker.data.links.RelativeLinkToDirectory
import org.intellij.plugin.tracker.data.links.RelativeLinkToFile
import org.intellij.plugin.tracker.data.links.RelativeLinkToLine

/**
 * This class is a template for testing updating links.
 * In order to create tests with a new project instance per test
 * it is necessary to create a different instance of this class for each test case.
 */
abstract class TestUpdateLinks : BasePlatformTestCase() {

    protected lateinit var myLinkUpdateService: LinkUpdaterService
    private val myFiles = arrayOf(
        "testUpdateLinks.md",
        "testUpdateRelativeLinks.md",
        "main/file.txt",
        "main/directory/file1.txt",
        "main/directory/file2.txt"
    )

    override fun getTestDataPath(): String {
        return "src/test/kotlin/org/intellij/plugin/tracker/services/testdata"
    }

    override fun setUp() {
        super.setUp()
        myFixture.configureByFiles(*myFiles)
        myLinkUpdateService = LinkUpdaterService.getInstance(project)
    }
}

/**
 * This class tests updating a single link.
 * Simulates the effect of moving test file "file.txt"
 * from the root directory to a new directory "main".
 */
class TestRelativeLinkToFile : TestUpdateLinks() {

    fun testUpdateRelativeLinkToFile() {
        val linkInfo = LinkInfo(
            linkText = "relative link to file",
            linkPath = "file.txt",
            proveniencePath = "testUpdateLinks.md",
            foundAtLineNumber = 1,
            textOffset = 24,
            fileName = "testUpdateLinks.md",
            project = project
        )
        val link = RelativeLinkToFile(
            linkInfo = linkInfo
        )
        val change = CustomChange(
            customChangeType = CustomChangeType.MOVED,
            afterPathString = "main/file.txt"
        )
        val list = mutableListOf<Pair<Link, Change>>(
            Pair(link, change)
        )
        WriteCommandAction.runWriteCommandAction(project) {
            myLinkUpdateService.updateLinks(list, null)
        }
        myFixture.configureByFile("expected/expectedTestUpdateRelativeLinkToFile.md")
        ApplicationManager.getApplication().runReadAction {
            val fileExpected = myFixture.findFileInTempDir("expected/expectedTestUpdateRelativeLinkToFile.md")
            val fileActual = myFixture.findFileInTempDir("testUpdateLinks.md")
            PlatformTestUtil.assertFilesEqual(fileExpected, fileActual)
        }
    }
}

/**
 * This class tests updating a single link.
 * Simulates the effect of moving test file "file.txt"
 * from the root directory to a new directory "main".
 */
class TestRelativeLinkToLine : TestUpdateLinks() {

    fun testUpdateRelativeLinkToLine() {
        val linkInfo = LinkInfo(
            linkText = "relative link to line",
            linkPath = "file.txt#L1",
            proveniencePath = "testUpdateLinks.md",
            foundAtLineNumber = 2,
            textOffset = 63,
            fileName = "testUpdateLinks.md",
            project = project
        )
        val link = RelativeLinkToFile(
            linkInfo = linkInfo
        )
        val change = CustomChange(
            customChangeType = CustomChangeType.MOVED,
            afterPathString = "main/file.txt#L1"
        )
        val list = mutableListOf<Pair<Link, Change>>(
            Pair(link, change)
        )
        WriteCommandAction.runWriteCommandAction(project) {
            myLinkUpdateService.updateLinks(list, null)
        }
        myFixture.configureByFile("expected/expectedTestUpdateRelativeLinkToLine.md")
        ApplicationManager.getApplication().runReadAction {
            val fileExpected = myFixture.findFileInTempDir("expected/expectedTestUpdateRelativeLinkToLine.md")
            val fileActual = myFixture.findFileInTempDir("testUpdateLinks.md")
            PlatformTestUtil.assertFilesEqual(fileExpected, fileActual)
        }
    }
}

/**
 * This class tests updating multiple relative links within a single file.
 * Simulates the effect of moving test file "file.txt" from the root directory
 * to a new directory "main", and moving files "file1.txt" and "file2.txt"
 * from the root directory to a new directory "main/directory".
 */
class TestRelativeLinks : TestUpdateLinks() {

    fun testUpdateRelativeLinks() {
        val linkInfo1 = LinkInfo(
            linkText = "relative link 1",
            linkPath = "file.txt",
            proveniencePath = "testUpdateRelativeLinks.md",
            foundAtLineNumber = 1,
            textOffset = 18,
            fileName = "testUpdateRelativeLinks.md",
            project = project
        )
        val linkInfo2 = LinkInfo(
            linkText = "relative link 2",
            linkPath = "file1.txt",
            proveniencePath = "testUpdateRelativeLinks.md",
            foundAtLineNumber = 2,
            textOffset = 46,
            fileName = "testUpdateRelativeLinks.md",
            project = project
        )
        val linkInfo3 = LinkInfo(
            linkText = "relative link 3",
            linkPath = "file2.txt",
            proveniencePath = "testUpdateRelativeLinks.md",
            foundAtLineNumber = 3,
            textOffset = 75,
            fileName = "testUpdateRelativeLinks.md",
            project = project
        )
        val link1 = RelativeLinkToFile(
            linkInfo = linkInfo1
        )
        val link2 = RelativeLinkToFile(
            linkInfo = linkInfo2
        )
        val link3 = RelativeLinkToFile(
            linkInfo = linkInfo3
        )
        val change1 = CustomChange(
            customChangeType = CustomChangeType.MOVED,
            afterPathString = "main/file.txt"
        )
        val change2 = CustomChange(
            customChangeType = CustomChangeType.MOVED,
            afterPathString = "main/directory/file1.txt"
        )
        val change3 = CustomChange(
            customChangeType = CustomChangeType.MOVED,
            afterPathString = "main/directory/file2.txt"
        )
        val list = mutableListOf<Pair<Link, Change>>(
            Pair(link1, change1),
            Pair(link2, change2),
            Pair(link3, change3)
        )
        WriteCommandAction.runWriteCommandAction(project) {
            myLinkUpdateService.updateLinks(list, null)
        }
        myFixture.configureByFile("expected/expectedTestUpdateRelativeLinks.md")
        ApplicationManager.getApplication().runReadAction {
            val fileExpected = myFixture.findFileInTempDir("expected/expectedTestUpdateRelativeLinks.md")
            val fileActual = myFixture.findFileInTempDir("testUpdateRelativeLinks.md")
            PlatformTestUtil.assertFilesEqual(fileExpected, fileActual)
        }
    }
}

/**
 * This class tests updating multiple links of different types within a single file.
 * Simulates the effect of moving test file "file.txt"
 * from the root directory to a new directory "main".
 */
class TestMultipleLinks : TestUpdateLinks() {

    // This test case is disabled because one of the tested features
    // (updating links to directories) is not yet ready.
    // Do not remove the test.
    fun testUpdateVariousLinks() {
        val linkInfoToFile = LinkInfo(
            linkText = "relative link to file",
            linkPath = "file.txt",
            proveniencePath = "testUpdateLinks.md",
            foundAtLineNumber = 1,
            textOffset = 24,
            fileName = "testUpdateLinks.md",
            project = project
        )
        val linkInfoToLine = LinkInfo(
            linkText = "relative link to line",
            linkPath = "file.txt#L1",
            proveniencePath = "testUpdateLinks.md",
            foundAtLineNumber = 2,
            textOffset = 63,
            fileName = "testUpdateLinks.md",
            project = project
        )
        val linkInfoToDir = LinkInfo(
            linkText = "relative link to directory",
            linkPath = ".",
            proveniencePath = "testUpdateLinks.md",
            foundAtLineNumber = 3,
            textOffset = 110,
            fileName = "testUpdateLinks.md",
            project = project
        )
        val linkToFile = RelativeLinkToFile(
            linkInfo = linkInfoToFile
        )
        val linkToLine = RelativeLinkToLine(
            linkInfo = linkInfoToLine
        )
        val linkToDir = RelativeLinkToDirectory(
            linkInfo = linkInfoToDir
        )
        val linkChangeToFile = CustomChange(
            customChangeType = CustomChangeType.MOVED,
            afterPathString = "main/file.txt"
        )
        val linkChangeToLine = LineChange(
            fileChange = CustomChange(CustomChangeType.MOVED, afterPathString = "main/file.txt"),
            lineChangeType = LineChangeType.MOVED,
            newLine = Line(lineNumber = 1, content = "dummy line")
        )
        val linkChangeToDir = CustomChange(
            customChangeType = CustomChangeType.MOVED,
            afterPathString = "main"
        )
        val list = mutableListOf<Pair<Link, Change>>(
            Pair(linkToFile, linkChangeToFile),
            Pair(linkToLine, linkChangeToLine),
            Pair(linkToDir, linkChangeToDir)
        )
        WriteCommandAction.runWriteCommandAction(project) {
            myLinkUpdateService.updateLinks(list, null)
        }
        myFixture.configureByFile("expected/expectedTestUpdateMultipleLinks.md")
        ApplicationManager.getApplication().runReadAction {
            val fileExpected = myFixture.findFileInTempDir("expected/expectedTestUpdateMultipleLinks.md")
            val fileActual = myFixture.findFileInTempDir("testUpdateLinks.md")
            PlatformTestUtil.assertFilesEqual(fileExpected, fileActual)
        }
    }
}
