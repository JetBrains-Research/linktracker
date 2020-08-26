package org.intellij.plugin.tracker.services

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.intellij.markdown.MarkdownElementTypes.INLINE_LINK
import org.intellij.plugin.tracker.data.diff.Line
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
import org.intellij.plugin.tracker.core.update.LinkElementImpl
import org.intellij.plugins.markdown.lang.MarkdownElementType

/**
 * This class is a template for testing updating links.
 * In order to create tests with a new project instance per test
 * it is necessary to create a different instance of this class for each test case.
 * WARNING: All filenames of test files should be unique project-wide,
 * and all link texts should be unique file-wide.
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

    protected fun refresh(dir: VirtualFile = project.baseDir) {
        VfsUtil.markDirtyAndRefresh(false, true, false, dir)
    }

    /**
     * Finds the Psi link destination element in the given file with the given link text.
     * Assumes that the filename is unique in the project's file tree and that the element's text
     * is unique in its file.
     */
    protected fun findElement(fileName: String, text: String): PsiElement {
        val files = FilenameIndex.getFilesByName(
            project, fileName,
            GlobalSearchScope.projectScope(project)
        )
        assert(files.size == 1)
        val file = files[0]
        val matches = PsiTreeUtil
            .findChildrenOfType(file, ASTWrapperPsiElement::class.java)
            .toList()
            .filter { it.elementType == MarkdownElementType.platformType(INLINE_LINK) }
            .filter { it.children[0].text == "[$text]" }
        assert(matches.size == 1)
        return matches[0].children[1]
    }
}

/**
 * This class tests updating a single link.
 * Simulates the effect of moving test file "file.txt"
 * from the root directory to a new directory "main".
 */
class TestRelativeLinkToFile : TestUpdateLinks() {

    fun testUpdateRelativeLinkToFile() {
        val linkText = "relative link to file"
        val fileName = "testUpdateLinks.md"
        val linkElement =
            LinkElementImpl(findElement(fileName, linkText))
        val linkInfo = LinkInfo(
            linkText = linkText,
            linkPath = "file.txt",
            proveniencePath = fileName,
            foundAtLineNumber = 1,
            fileName = fileName,
            project = project,
            linkElement = linkElement
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
            myLinkUpdateService.batchUpdateLinks(list, null)
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
        val linkText = "relative link to line"
        val fileName = "testUpdateLinks.md"
        val linkElement =
            LinkElementImpl(findElement(fileName, linkText))
        val linkInfo = LinkInfo(
            linkText = linkText,
            linkPath = "file.txt#L1",
            proveniencePath = fileName,
            foundAtLineNumber = 2,
            fileName = fileName,
            project = project,
            linkElement = linkElement
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
            myLinkUpdateService.batchUpdateLinks(list, null)
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
        val fileName = "testUpdateRelativeLinks.md"
        val linkText1 = "relative link 1"
        val linkElement1 =
            LinkElementImpl(findElement(fileName, linkText1))
        val linkInfo1 = LinkInfo(
            linkText = linkText1,
            linkPath = "file.txt",
            proveniencePath = fileName,
            foundAtLineNumber = 1,
            fileName = fileName,
            project = project,
            linkElement = linkElement1
        )
        val linkText2 = "relative link 2"
        val linkElement2 =
            LinkElementImpl(findElement(fileName, linkText2))
        val linkInfo2 = LinkInfo(
            linkText = linkText2,
            linkPath = "file1.txt",
            proveniencePath = fileName,
            foundAtLineNumber = 2,
            fileName = fileName,
            project = project,
            linkElement = linkElement2
        )
        val linkText3 = "relative link 3"
        val linkElement3 =
            LinkElementImpl(findElement(fileName, linkText3))
        val linkInfo3 = LinkInfo(
            linkText = linkText3,
            linkPath = "file2.txt",
            proveniencePath = fileName,
            foundAtLineNumber = 3,
            fileName = fileName,
            project = project,
            linkElement = linkElement3
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
            myLinkUpdateService.batchUpdateLinks(list, null)
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
        val fileName = "testUpdateLinks.md"
        val linkText1 = "relative link to file"
        val linkElement1 =
            LinkElementImpl(findElement(fileName, linkText1))
        val linkInfoToFile = LinkInfo(
            linkText = linkText1,
            linkPath = "file.txt",
            proveniencePath = fileName,
            foundAtLineNumber = 1,
            fileName = fileName,
            project = project,
            linkElement = linkElement1
        )
        val linkText2 = "relative link to line"
        val linkElement2 =
            LinkElementImpl(findElement(fileName, linkText2))
        val linkInfoToLine = LinkInfo(
            linkText = linkText2,
            linkPath = "file.txt#L1",
            proveniencePath = fileName,
            foundAtLineNumber = 2,
            fileName = fileName,
            project = project,
            linkElement = linkElement2
        )
        val linkText3 = "relative link to directory"
        val linkElement3 =
            LinkElementImpl(findElement(fileName, linkText3))
        val linkInfoToDir = LinkInfo(
            linkText = linkText3,
            linkPath = ".",
            proveniencePath = fileName,
            foundAtLineNumber = 3,
            fileName = fileName,
            project = project,
            linkElement = linkElement3
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
            newLine = Line(
                lineNumber = 1,
                content = "dummy line"
            )
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
            myLinkUpdateService.batchUpdateLinks(list, null)
        }
        myFixture.configureByFile("expected/expectedTestUpdateMultipleLinks.md")
        ApplicationManager.getApplication().runReadAction {
            val fileExpected = myFixture.findFileInTempDir("expected/expectedTestUpdateMultipleLinks.md")
            val fileActual = myFixture.findFileInTempDir("testUpdateLinks.md")
            PlatformTestUtil.assertFilesEqual(fileExpected, fileActual)
        }
    }
}
