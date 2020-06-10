package org.intellij.plugin.tracker.integration

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vcs.VcsTestUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFileMoveEvent
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import git4idea.GitVcs
import org.intellij.plugin.tracker.data.changes.Change
import org.intellij.plugin.tracker.data.changes.ChangeType
import org.intellij.plugin.tracker.data.changes.CustomChange
import org.intellij.plugin.tracker.data.changes.CustomChangeType
import org.intellij.plugin.tracker.data.links.LinkInfo
import org.intellij.plugin.tracker.data.links.RelativeLinkToFile
import org.intellij.plugin.tracker.integration.git4idea.test.*
import org.intellij.plugin.tracker.integration.git4idea.test.TestFile
import org.intellij.plugin.tracker.services.ChangeTrackerServiceImpl
import org.intellij.plugin.tracker.utils.GitOperationManager
import org.junit.jupiter.api.*

/**
 * This class is a template for testing parsing changes from the Git integration.
 * In order to create tests with a new project instance per test
 * it is necessary to create a different instance of this class for each test case.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestParseChanges : GitSingleRepoTest() {

    private lateinit var changeTracker: ChangeTrackerServiceImpl

    @BeforeAll
    override fun setUp() {
        super.setUp()
    }

    @AfterAll
    override fun tearDown() {
        super.tearDown()
    }

    @BeforeEach
    fun config() {
        changeTracker = ChangeTrackerServiceImpl.getInstance(project)
    }

    @Disabled
    @Test
    fun test() {

        // Create and commit linked file
        val linkedFile = file("file.txt").create("Some content")
        val linkedVirtualFile = getVirtualFile(linkedFile.file)
        linkedFile.addCommit("First commit")

        // Create and commit linking file
        val linkingFile = file("file.md").create("[link](file.txt)")
        val linkingVirtualFile = getVirtualFile(linkingFile.file)
        linkingFile.addCommit("Second commit")

        // Move linked file
        val mainDir = createChildDirectory(projectRoot, "main")
        move(linkedVirtualFile, mainDir)
        refresh()
        updateChangeListManager()
        printVfsTree(projectRoot)

        // Create link
        val linkInfo = LinkInfo(
            fileName = "file.md",
            foundAtLineNumber = 1,
            linkPath = "file.txt",
            linkText = "link",
            project = project,
            proveniencePath = "file.md",
            textOffset = 7
        )
        val link = RelativeLinkToFile(linkInfo)

        val changes = changeTracker.getLocalFileChanges(link)
        changes.changes.map{ println(it.changeTypeString) }
        Assertions.assertTrue(changes.changes.contains(CustomChangeType.MOVED))
    }

    /**
     * Prints the test directory structure, for debug purposes.
     */
    private fun printVfsTree(root: VirtualFile) {
        printVfsTree(root, "", true)
    }

    private fun printVfsTree(node: VirtualFile, prefix: String, excludeGit: Boolean) {
        val nodePath = prefix + "/" + node.name
        println(nodePath)
        val newPrefix = prefix + "\t"
        for (child in node.children) {
            if (excludeGit && child.name == ".git") {
                continue
            }

            printVfsTree(child, newPrefix, excludeGit)
        }
    }
}