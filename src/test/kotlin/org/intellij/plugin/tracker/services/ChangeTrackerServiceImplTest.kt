package org.intellij.plugin.tracker.services

import com.intellij.openapi.vcs.Executor
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import org.junit.jupiter.api.Assertions
import org.intellij.plugin.tracker.data.changes.CustomChange
import org.intellij.plugin.tracker.data.changes.CustomChangeType
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.data.links.LinkInfo
import org.intellij.plugin.tracker.data.links.RelativeLinkToFile
import org.intellij.plugin.tracker.services.git4idea.test.GitSingleRepoTest
import org.intellij.plugin.tracker.services.git4idea.test.TestFile
import org.intellij.plugin.tracker.services.git4idea.test.delete
import org.intellij.plugin.tracker.services.git4idea.test.mv
import org.intellij.plugin.tracker.services.git4idea.test.add
import org.intellij.plugin.tracker.services.git4idea.test.commit

/**
 * This class is a template for testing parsing changes from the Git integration.
 * In order to create tests with a new project instance per test
 * it is necessary to create a different instance of this class for each test case.
 */
class ChangeTrackerServiceTest : GitSingleRepoTest() {

    private lateinit var changeTracker: ChangeTrackerServiceImpl
    private lateinit var defaultLink: Link

    override fun setUp() {
        super.setUp()

        changeTracker = ChangeTrackerServiceImpl.getInstance(project)
        defaultLink = RelativeLinkToFile(
            LinkInfo(
                fileName = "file.md",
                foundAtLineNumber = 1,
                linkPath = "file.txt",
                linkText = "link",
                project = project,
                proveniencePath = "file.md",
                textOffset = 7
            )
        )
    }

    fun testParseChangesCommittedAddedFile() {

        // Create and commit files
        createLinkedFile()
        createLinkingFile()

        refresh()
        updateChangeListManager()

        val change: CustomChange = changeTracker.getLocalFileChanges(defaultLink) as CustomChange

        Assertions.assertEquals(change.afterPathString, "file.txt")
        Assertions.assertEquals(change.customChangeType, CustomChangeType.ADDED)
        Assertions.assertEquals(change.requiresUpdate, false)
        Assertions.assertEquals(change.hasWorkingTreeChanges(), false)
    }

    fun testParseChangesCommittedMovedFile() {

        // Create and commit files
        val linkedFile = createLinkedFile()
        createLinkingFile()

        // Create new directory and move linked file to new directory
        val dir = Executor.mkdir("mydirectory")
        val mvFile = File(dir.path, "file.txt")
        repo.mv(linkedFile.file, mvFile)
        repo.commit("Move linked file to new directory")

        refresh()
        updateChangeListManager()

        val change: CustomChange = changeTracker.getLocalFileChanges(defaultLink) as CustomChange

        Assertions.assertEquals(change.afterPathString, "mydirectory/file.txt")
        Assertions.assertEquals(change.customChangeType, CustomChangeType.MOVED)
        Assertions.assertEquals(change.requiresUpdate, true)
        Assertions.assertEquals(change.hasWorkingTreeChanges(), false)
    }

    fun testParseChangesUncommittedMoved() {

        // Create and commit files
        val linkedFile = createLinkedFile()
        createLinkingFile()

        // Create new directory and move linked file to new directory
        val dir = Executor.mkdir("mydirectory")
        val mvFile = File(dir.path, "file.txt")
        repo.mv(linkedFile.file, mvFile)

        refresh()
        updateChangeListManager()

        val change: CustomChange = changeTracker.getLocalFileChanges(defaultLink) as CustomChange

        Assertions.assertEquals(change.afterPathString, "mydirectory/file.txt")
        Assertions.assertEquals(change.customChangeType, CustomChangeType.MOVED)
        Assertions.assertEquals(change.requiresUpdate, true)
        Assertions.assertEquals(change.hasWorkingTreeChanges(), true)
    }

    fun testParseChangesCommittedDeleted() {

        // Create and commit files
        val linkedFile = createLinkedFile()
        createLinkingFile()

        // Delete linked file
        repo.delete(linkedFile)
        repo.add()
        repo.commit("Delete linked file")

        refresh()

        val change: CustomChange = changeTracker.getLocalFileChanges(defaultLink) as CustomChange

        Assertions.assertEquals(change.afterPathString, "file.txt")
        Assertions.assertEquals(change.customChangeType, CustomChangeType.DELETED)
        Assertions.assertEquals(change.requiresUpdate, true)
        Assertions.assertEquals(change.hasWorkingTreeChanges(), false)
    }

    private fun createLinkedFile(): TestFile {

        val linkedFile = file("file.txt").create("Some content")
        repo.add()
        repo.commit("Create linked file")
        return linkedFile
    }

    private fun createLinkingFile(): TestFile {

        val linkingFile = file("file.md").create("[link](file.txt)")
        repo.add()
        repo.commit("Create linking file")
        return linkingFile
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
