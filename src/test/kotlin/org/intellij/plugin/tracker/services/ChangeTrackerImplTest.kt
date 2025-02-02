package org.intellij.plugin.tracker.services

import com.intellij.openapi.vcs.Executor
import com.intellij.openapi.vfs.VirtualFile
import com.nhaarman.mockitokotlin2.mock
import org.intellij.plugin.tracker.core.change.ChangeTrackerImpl
import org.intellij.plugin.tracker.core.change.ChangeTrackingPolicy
import org.intellij.plugin.tracker.data.CommitSHAIsNullDirectoryException
import org.intellij.plugin.tracker.data.CommitSHAIsNullLineException
import org.intellij.plugin.tracker.data.CommitSHAIsNullLinesException
import org.intellij.plugin.tracker.data.FileChangeGatheringException
import org.intellij.plugin.tracker.data.diff.Line
import org.intellij.plugin.tracker.data.ReferencedFileNotFoundException
import org.intellij.plugin.tracker.data.changes.CustomChange
import org.intellij.plugin.tracker.data.changes.CustomChangeType
import org.intellij.plugin.tracker.data.changes.LineChange
import org.intellij.plugin.tracker.data.changes.LineChangeType
import org.intellij.plugin.tracker.data.changes.LinesChange
import org.intellij.plugin.tracker.data.changes.LinesChangeType
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.data.links.LinkInfo
import org.intellij.plugin.tracker.data.links.RelativeLinkToDirectory
import org.intellij.plugin.tracker.data.links.RelativeLinkToFile
import org.intellij.plugin.tracker.data.links.RelativeLinkToLine
import org.intellij.plugin.tracker.data.links.RelativeLinkToLines
import org.intellij.plugin.tracker.services.git4idea.test.GitSingleRepoTest
import org.intellij.plugin.tracker.services.git4idea.test.TestFile
import org.intellij.plugin.tracker.services.git4idea.test.add
import org.intellij.plugin.tracker.services.git4idea.test.addCommit
import org.intellij.plugin.tracker.services.git4idea.test.commit
import org.intellij.plugin.tracker.services.git4idea.test.delete
import org.intellij.plugin.tracker.services.git4idea.test.mv
import org.intellij.plugin.tracker.core.update.LinkElementImpl
import org.junit.jupiter.api.Assertions
import java.io.File
import kotlin.test.assertFailsWith

/**
 * This class is a template for testing parsing changes from the Git integration.
 * In order to create tests with a new project instance per test
 * it is necessary to create a different instance of this class for each test case.
 */
class ChangeTrackerImplTest : GitSingleRepoTest() {

    private lateinit var changeTracker: ChangeTrackerImpl
    private lateinit var defaultLink: Link

    override fun setUp() {
        super.setUp()

        changeTracker = ChangeTrackerImpl(
            project,
            ChangeTrackingPolicy.HISTORY
        )
        defaultLink = RelativeLinkToFile(
            LinkInfo(
                fileName = "file.md",
                foundAtLineNumber = 1,
                linkPath = "file.txt",
                linkText = "link",
                project = project,
                proveniencePath = "file.md",
                linkElement = LinkElementImpl(mock())
            )
        )
    }

    fun `test parse changes committed added file`() {

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

    fun `test parse changes uncommitted added file`() {

        // Create files
        val linkedFile: TestFile = file("file.txt")
        linkedFile.create("Some content")

        val linkingFile: TestFile = file("file.md")
        linkingFile.create("[link](file.txt)")

        refresh()
        updateChangeListManager()

        val change: CustomChange = changeTracker.getLocalFileChanges(defaultLink) as CustomChange

        Assertions.assertEquals(change.afterPathString, "file.txt")
        Assertions.assertEquals(change.customChangeType, CustomChangeType.ADDED)
        Assertions.assertEquals(change.requiresUpdate, false)
        Assertions.assertEquals(change.hasWorkingTreeChanges(), true)
    }

    fun `test parse changes committed moved file`() {

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

    fun `test parse changes uncommitted moved`() {

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

    fun `test parse changes committed and then uncommitted moved`() {

        // Create and commit files
        val linkedFile = createLinkedFile()
        createLinkingFile()

        // Create new directory and move linked file to new directory
        val dir = Executor.mkdir("mydirectory")
        val mvFile = File(dir.path, "file.txt")
        repo.mv(linkedFile.file, mvFile)
        repo.add()
        repo.commit("move file to new dir")

        // Create new directory and move linked file to new directory
        val dir2 = Executor.mkdir("newdirectory")
        val mvFile2 = File(dir2.path, "file.txt")
        repo.mv(mvFile, mvFile2)

        refresh()
        updateChangeListManager()

        val change: CustomChange = changeTracker.getLocalFileChanges(defaultLink) as CustomChange

        Assertions.assertEquals(change.afterPathString, "newdirectory/file.txt")
        Assertions.assertEquals(change.customChangeType, CustomChangeType.MOVED)
        Assertions.assertEquals(change.requiresUpdate, true)
        Assertions.assertEquals(change.hasWorkingTreeChanges(), true)
    }

    fun `test parse changes uncommitted renamed`() {

        // Create and commit files
        val linkedFile = createLinkedFile()
        createLinkingFile()

        // Rename the file
        val renameFile = File("renamed.txt")
        repo.mv(linkedFile.file, renameFile)

        refresh()
        updateChangeListManager()

        val change: CustomChange = changeTracker.getLocalFileChanges(defaultLink) as CustomChange

        Assertions.assertEquals(change.afterPathString, "renamed.txt")
        Assertions.assertEquals(change.customChangeType, CustomChangeType.MOVED)
        Assertions.assertEquals(change.requiresUpdate, true)
        Assertions.assertEquals(change.hasWorkingTreeChanges(), true)
    }

    fun `test parse changes uncommitted deleted`() {

        // Create and commit files
        val linkedFile = createLinkedFile()
        createLinkingFile()

        // Delete linked file
        repo.delete(linkedFile)
        repo.add()
        repo.commit("Delete linked file")

        refresh()
        updateChangeListManager()

        val change: CustomChange = changeTracker.getLocalFileChanges(defaultLink) as CustomChange

        Assertions.assertEquals(change.afterPathString, "file.txt")
        Assertions.assertEquals(change.customChangeType, CustomChangeType.DELETED)
        Assertions.assertEquals(change.requiresUpdate, true)
        Assertions.assertEquals(change.hasWorkingTreeChanges(), false)
    }

    fun `test parse changes not existing file`() {

        val link = createDummyLinkToFile("file.md", "file.md", "notexistingfile.txt")

        createLinkingFile(content = "[link](notexistingfile.txt)")

        refresh()
        updateChangeListManager()

        assertFailsWith<ReferencedFileNotFoundException> {
            changeTracker.getLocalFileChanges(link) as CustomChange
        }
    }

    fun `test parse changes file exception`() {
        val link = RelativeLinkToFile(
            LinkInfo(
                fileName = "file.md",
                foundAtLineNumber = 1,
                linkPath = "none.txt",
                linkText = "link",
                project = project,
                proveniencePath = "file.md",
                linkElement = LinkElementImpl(mock())
            )
        )
        val linkingFile = file("file.md")
        linkingFile.create("[link](none.txt)")
        repo.add()
        repo.commit("Create linking file")

        refresh()
        updateChangeListManager()

        assertFailsWith<FileChangeGatheringException> {
            changeTracker.getLocalFileChanges(link) as CustomChange
        }
    }

    fun `test parse changes committed added directory`() {

        val link = createDummyLinkToDirectory("file.md", "file.md", "dir")

        createLinkedDirectory()
        createLinkingFile(content = "[link](dir)")

        refresh()
        updateChangeListManager()

        val change: CustomChange = changeTracker.getLocalDirectoryChanges(link) as CustomChange

        Assertions.assertEquals(change.afterPathString, "dir")
        Assertions.assertEquals(change.customChangeType, CustomChangeType.ADDED)
        Assertions.assertEquals(change.requiresUpdate, false)
        Assertions.assertEquals(change.hasWorkingTreeChanges(), false)
    }

    fun `test parse changes uncommitted renamed directory`() {

        val link = createDummyLinkToDirectory("file.md", "file.md", "dir")

        val linkedDirectory = createLinkedDirectory()
        createLinkingFile(content = "[link](dir)")

        // Create new directory and move linked file to new directory
        val dir = File("mydirectory")
        repo.mv(linkedDirectory.file, dir)
        repo.add()
        repo.commit("rename the directory")

        refresh()
        updateChangeListManager()

        val change: CustomChange = changeTracker.getLocalDirectoryChanges(link) as CustomChange

        Assertions.assertEquals(change.afterPathString, "mydirectory")
        Assertions.assertEquals(change.customChangeType, CustomChangeType.MOVED)
        Assertions.assertEquals(change.requiresUpdate, true)
        Assertions.assertEquals(change.hasWorkingTreeChanges(), false)
    }

    fun `test parse changes committed moved directory`() {

        val link = createDummyLinkToDirectory("file.md", "file.md", "dir")

        val linkedDirectory = createLinkedDirectory()
        createLinkingFile(content = "[link](dir)")

        // Create new directory and move linked file to new directory
        val dir = Executor.mkdir("mydirectory")
        repo.mv(linkedDirectory.file, dir)
        repo.commit("Move linked directory to new directory")

        refresh()
        updateChangeListManager()

        val change: CustomChange = changeTracker.getLocalDirectoryChanges(link) as CustomChange

        Assertions.assertEquals(change.afterPathString, "mydirectory/dir")
        Assertions.assertEquals(change.customChangeType, CustomChangeType.MOVED)
        Assertions.assertEquals(change.requiresUpdate, true)
        Assertions.assertEquals(change.hasWorkingTreeChanges(), false)
    }

    fun `test parse changes committed deleted directory`() {

        val link = createDummyLinkToDirectory("file.md", "file.md", "dir")

        val linkedDirectory = createLinkedDirectory()
        createLinkingFile(content = "[link](dir)")

        // Delete linked file
        repo.delete(linkedDirectory)
        repo.add()
        repo.commit("Delete linked directory")

        refresh()
        updateChangeListManager()

        val change: CustomChange = changeTracker.getLocalDirectoryChanges(link) as CustomChange

        Assertions.assertEquals(change.afterPathString, "dir")
        Assertions.assertEquals(change.customChangeType, CustomChangeType.DELETED)
        Assertions.assertEquals(change.requiresUpdate, true)
        Assertions.assertEquals(change.hasWorkingTreeChanges(), false)
    }

    fun `test parse changes not existing directory`() {

        val link = createDummyLinkToDirectory("file.md", "file.md", "notexistingdir")

        createLinkingFile(content = "[link](notexistingdir)")

        refresh()
        updateChangeListManager()

        assertFailsWith<CommitSHAIsNullDirectoryException> {
            changeTracker.getLocalDirectoryChanges(link) as CustomChange
        }
    }

    fun `test single line moved with uncommitted file changes`() {
        val link = createDummyLinkToLine("file.md", "file.md", "file.txt#L1")

        createLinkingFile(content = "[link](file.txt#L1)")
        val initialFileContent = """
           public static void dummyMethod() {
                this.value = value;
                return;
            }
        """.trimIndent()
        val linkedFile = createLinkedFile(content = initialFileContent)

        linkedFile.write(
            """
            
            
            
        public static void dummyMethod() {
            this.value = value;
            return;
        }
        """.trimIndent()
        )

        val change = changeTracker.getLocalLineChanges(link) as LineChange

        Assertions.assertEquals(change.lineChangeType, LineChangeType.MOVED)
        Assertions.assertEquals(change.newLine,
            Line(4, "public static void dummyMethod() {")
        )
        Assertions.assertEquals(change.requiresUpdate, true)
        Assertions.assertEquals(change.hasWorkingTreeChanges(), true)
        Assertions.assertEquals(change.fileChange.customChangeType, CustomChangeType.MODIFIED)
        Assertions.assertEquals(change.fileChange.afterPathString, "file.txt")
        Assertions.assertEquals(change.fileChange.deletionsAndAdditions, 0)
    }

    fun `test parse changes directory exception`() {
        val link = RelativeLinkToDirectory(
            LinkInfo(
                fileName = "test.md",
                foundAtLineNumber = 1,
                linkPath = "none",
                linkText = "link",
                project = project,
                proveniencePath = "test.md",
                linkElement = LinkElementImpl(mock())
            )
        )
        val linkingFile = file("test.md")
        linkingFile.create("[link](none)")
        repo.add()
        repo.commit("Create linking file")

        assertFailsWith<CommitSHAIsNullDirectoryException> {
            changeTracker.getLocalDirectoryChanges(link) as CustomChange
        }
    }

    fun `test single line deleted with uncommitted file changes`() {
        val link = createDummyLinkToLine("file.md", "file.md", "file.txt#L1")

        createLinkingFile(content = "[link](file.txt#L1)")
        val initialFileContent = """
           public static void dummyMethod() {
                this.value = value;
                return;
            }
        """.trimIndent()
        val linkedFile = createLinkedFile(content = initialFileContent)

        linkedFile.write(
            """
            
            
            
            this.value = value;
            return;
        }
        """.trimIndent()
        )

        val change = changeTracker.getLocalLineChanges(link) as LineChange
        Assertions.assertEquals(change.lineChangeType, LineChangeType.DELETED)
        Assertions.assertEquals(change.newLine?.lineNumber, 1)
        Assertions.assertEquals(change.newLine?.content, "")
        Assertions.assertEquals(change.requiresUpdate, true)
        Assertions.assertEquals(change.hasWorkingTreeChanges(), true)
        Assertions.assertEquals(change.fileChange.customChangeType, CustomChangeType.MODIFIED)
        Assertions.assertEquals(change.fileChange.afterPathString, "file.txt")
        Assertions.assertEquals(change.fileChange.deletionsAndAdditions, 0)
    }

    fun `test single line unchanged with uncommitted file changes`() {
        val link = createDummyLinkToLine("file.md", "file.md", "file.txt#L1")

        createLinkingFile(content = "[link](file.txt#L1)")
        val initialFileContent = """
           public static void dummyMethod() {
                this.value = value;
                return;
            }
        """.trimIndent()
        val linkedFile = createLinkedFile(content = initialFileContent)

        linkedFile.write(
            """
          public static void dummyMethod() {
                this.value = value;
                return;
            }
            
          public void newMethodAddedHere() {
                doNothing();
           }
        """.trimIndent()
        )

        val change = changeTracker.getLocalLineChanges(link) as LineChange
        Assertions.assertEquals(change.lineChangeType, LineChangeType.UNCHANGED)
        Assertions.assertEquals(change.newLine,
            Line(1, "public static void dummyMethod() {")
        )
        Assertions.assertEquals(change.requiresUpdate, false)
        Assertions.assertEquals(change.hasWorkingTreeChanges(), true)
        Assertions.assertEquals(change.fileChange.customChangeType, CustomChangeType.MODIFIED)
        Assertions.assertEquals(change.fileChange.afterPathString, "file.txt")
        Assertions.assertEquals(change.fileChange.deletionsAndAdditions, 0)
    }

    fun `test single line unchanged with multiple uncommitted file changes`() {
        val link = createDummyLinkToLine("file.md", "file.md", "file.txt#L1")

        createLinkingFile(content = "[link](file.txt#L1)")
        val initialFileContent = """
           public static void dummyMethod() {
                this.value = value;
                return;
            }
        """.trimIndent()
        val linkedFile = createLinkedFile(content = initialFileContent)

        linkedFile.write(
            """
           public void newMethodAddedHere() {
                doNothing();
           }
            
          public static void dummyMethod() {
                this.value = value;
                return;
            }

        """.trimIndent()
        )

        var change = changeTracker.getLocalLineChanges(link) as LineChange
        Assertions.assertEquals(change.lineChangeType, LineChangeType.MOVED)
        Assertions.assertEquals(change.newLine,
            Line(5, "public static void dummyMethod() {")
        )
        Assertions.assertEquals(change.requiresUpdate, true)
        Assertions.assertEquals(change.hasWorkingTreeChanges(), true)
        Assertions.assertEquals(change.fileChange.customChangeType, CustomChangeType.MODIFIED)
        Assertions.assertEquals(change.fileChange.afterPathString, "file.txt")
        Assertions.assertEquals(change.fileChange.deletionsAndAdditions, 0)

        linkedFile.write(
            """
           public static void renameMethod() {
                this.value = value;
                return;
            }
            
          /**
          * Adding some
          * more
          * lines
          */
          public void myMethod() {
                return this;
            }
        """.trimIndent()
        )

        change = changeTracker.getLocalLineChanges(link) as LineChange

        Assertions.assertEquals(change.lineChangeType, LineChangeType.UNCHANGED)
        Assertions.assertEquals(change.newLine?.lineNumber, 1)
        Assertions.assertEquals(change.newLine?.content, "public static void renameMethod() {")
        Assertions.assertEquals(change.hasWorkingTreeChanges(), true)
        Assertions.assertEquals(change.fileChange.customChangeType, CustomChangeType.MODIFIED)
        Assertions.assertEquals(change.fileChange.afterPathString, "file.txt")
        Assertions.assertEquals(change.fileChange.deletionsAndAdditions, 0)
    }

    fun `test single line moved with uncommitted file changes and file moved`() {
        val link = createDummyLinkToLine("file.md", "file.md", "file.txt#L1")

        createLinkingFile(content = "[link](file.txt#L1)")
        val initialFileContent = """
           public static void dummyMethod() {
                this.value = value;
                return;
            }
        """.trimIndent()
        val linkedFile = createLinkedFile(content = initialFileContent)

        linkedFile.write(
            """
            
          public void newMethodAddedHere() {
                doNothing();
           }
           
           /**
           * New text
           * here
           * here and here
           * dummy text
           */
           public static void dummyMethod() {
                this.value = value;
                return;
            }
        """.trimIndent()
        )

        // Create new directory and move linked file to new directory
        val dir = Executor.mkdir("mydirectory")
        val mvFile = File(dir.path, "file.txt")
        repo.mv(linkedFile.file, mvFile)

        refresh()
        updateChangeListManager()

        val expectedLine =
            Line(12, "public static void dummyMethod() {")

        val change = changeTracker.getLocalLineChanges(link) as LineChange
        Assertions.assertEquals(change.lineChangeType, LineChangeType.MOVED)
        Assertions.assertEquals(change.newLine?.lineNumber, expectedLine.lineNumber)
        Assertions.assertEquals(change.newLine?.content, expectedLine.content)
        Assertions.assertEquals(change.requiresUpdate, true)
        Assertions.assertEquals(change.hasWorkingTreeChanges(), true)
        Assertions.assertEquals(change.fileChange.customChangeType, CustomChangeType.MOVED)
        Assertions.assertEquals(change.fileChange.afterPathString, "mydirectory/file.txt")
        Assertions.assertEquals(change.fileChange.deletionsAndAdditions, 0)
    }

    fun `test single line null commit sha`() {
        val link = createDummyLinkToLine("file.md", "file.md", "file.txt#L1")

        createLinkingFile(content = "[link](file.txt#L1)")

        assertFailsWith<CommitSHAIsNullLineException> {
            changeTracker.getLocalLineChanges(link) as LineChange
        }
    }

    fun `test multiple lines fully moved with uncommitted file`() {
        val link = createDummyLinkToLines("file.md", "file.md", "file.txt#L1-L5")

        createLinkingFile(content = "[link](file.txt#L1-L5)")
        val initialFileContent = """
           public static void dummyMethod() {
                this.value = value;
                this.name = name;
                doSomethingMore();
            }
        """.trimIndent()
        val linkedFile = createLinkedFile(content = initialFileContent)

        linkedFile.write(
            """
          
         class DummyClass {
            
            public DummyClass() {
            }
            
            
           /**
           * Adding documentation lines
           * One more lines
           */
           public static void dummyMethod() {
                this.value = value;
                this.name = name;
                doSomethingMore();
            }
        """.trimIndent()
        )

        val change = changeTracker.getLocalLinesChanges(link) as LinesChange

        val expectedLines = (12..16).toList()

        Assertions.assertEquals(LinesChangeType.FULL, change.linesChangeType)

        Assertions.assertNotNull(change.newLines)
        Assertions.assertEquals(1, change.newLines?.size)
        for ((index, line) in change.newLines!![0].withIndex()) {
            Assertions.assertEquals(expectedLines[index], line.lineNumber)
        }
        Assertions.assertEquals(change.requiresUpdate, true)
        Assertions.assertEquals(change.hasWorkingTreeChanges(), true)
        Assertions.assertEquals(change.fileChange.customChangeType, CustomChangeType.MODIFIED)
        Assertions.assertEquals(change.fileChange.afterPathString, "file.txt")
        Assertions.assertEquals(change.fileChange.deletionsAndAdditions, 0)
    }

    fun `test multiple lines fully moved with multiple changes with uncommitted file`() {
        val link = createDummyLinkToLines("file.md", "file.md", "file.txt#L1-L5")

        createLinkingFile(content = "[link](file.txt#L1-L5)")
        val initialFileContent = """
           public static void dummyMethod() {
                this.value = value;
                this.name = name;
                doSomethingMore();
            }
        """.trimIndent()
        val linkedFile = createLinkedFile(content = initialFileContent)

        linkedFile.write(
            """
          
         class DummyClass {
            
            public DummyClass() {
            }
            
            
           /**
           * Adding documentation lines
           * One more lines
           */
           public void dummyMethod() {
                this.value = otherValue;
                doSomethingMuchMuchMore();
            }
        """.trimIndent()
        )

        var change = changeTracker.getLocalLinesChanges(link) as LinesChange

        var expectedLines = (12..15).toList()

        Assertions.assertEquals(LinesChangeType.FULL, change.linesChangeType)

        Assertions.assertNotNull(change.newLines)
        Assertions.assertEquals(1, change.newLines?.size)
        for ((index, line) in change.newLines!![0].withIndex()) {
            Assertions.assertEquals(expectedLines[index], line.lineNumber)
        }
        Assertions.assertEquals(change.requiresUpdate, true)
        Assertions.assertEquals(change.hasWorkingTreeChanges(), true)
        Assertions.assertEquals(change.fileChange.customChangeType, CustomChangeType.MODIFIED)
        Assertions.assertEquals(change.fileChange.afterPathString, "file.txt")
        Assertions.assertEquals(change.fileChange.deletionsAndAdditions, 0)

        linkedFile.write(
            """
            
           /**
           * Adding documentation lines
           /*
           public void dummyMethod() {
                this.value = otherValue;
                doSomethingMuchMuchMore();
            }
            
            
            public inner class MyClass {
                late init var myVar: String }
        """.trimIndent()
        )

        change = changeTracker.getLocalLinesChanges(link) as LinesChange
        expectedLines = (5..8).toList()

        Assertions.assertEquals(LinesChangeType.FULL, change.linesChangeType)

        Assertions.assertNotNull(change.newLines)
        Assertions.assertEquals(1, change.newLines?.size)
        for ((index, line) in change.newLines!![0].withIndex()) {
            Assertions.assertEquals(expectedLines[index], line.lineNumber)
        }

        Assertions.assertEquals(change.requiresUpdate, true)
        Assertions.assertEquals(change.hasWorkingTreeChanges(), true)
        Assertions.assertEquals(change.fileChange.customChangeType, CustomChangeType.MODIFIED)
        Assertions.assertEquals(change.fileChange.afterPathString, "file.txt")
        Assertions.assertEquals(change.fileChange.deletionsAndAdditions, 0)
    }

    fun `test multiple lines fully moved with structure changes with uncommitted file`() {
        val link = createDummyLinkToLines("file.md", "file.md", "file.txt#L1-L5")

        createLinkingFile(content = "[link](file.txt#L1-L5)")
        val initialFileContent = """
           public static void dummyMethod() {
                this.value = value;
                this.name = name;
                doSomethingMore();
            }
        """.trimIndent()
        val linkedFile = createLinkedFile(content = initialFileContent)

        linkedFile.write(
            """
          
         class DummyClass {
            
            public DummyClass() {
            }
            
            
           /**
           * Adding documentation lines
           * One more lines
           */
           public void dummyMethod() {
                this.value = otherValue;
                doSomethingMuchMuchMore();
            }
        """.trimIndent()
        )

        val change = changeTracker.getLocalLinesChanges(link) as LinesChange
        val expectedLines = (12..15).toList()

        Assertions.assertEquals(LinesChangeType.FULL, change.linesChangeType)

        Assertions.assertNotNull(change.newLines)
        Assertions.assertEquals(1, change.newLines?.size)
        for ((index, line) in change.newLines!![0].withIndex()) {
            Assertions.assertEquals(expectedLines[index], line.lineNumber)
        }

        Assertions.assertEquals(change.requiresUpdate, true)
        Assertions.assertEquals(change.hasWorkingTreeChanges(), true)
        Assertions.assertEquals(change.fileChange.customChangeType, CustomChangeType.MODIFIED)
        Assertions.assertEquals(change.fileChange.afterPathString, "file.txt")
        Assertions.assertEquals(change.fileChange.deletionsAndAdditions, 0)
    }

    fun `test multiple lines deleted with uncommitted file`() {
        val link = createDummyLinkToLines("file.md", "file.md", "file.txt#L1-L5")

        createLinkingFile(content = "[link](file.txt#L1-L5)")
        val initialFileContent = """
           public static void dummyMethod() {
                this.value = value;
                this.name = name;
                doSomethingMore();
            }
        """.trimIndent()
        val linkedFile = createLinkedFile(content = initialFileContent)

        linkedFile.write(
            """
          
         class DummyClass {
            
            public DummyClass() {
            }
            
            
           /**
           * Adding documentation lines
           * One more lines
           */
           public void noMatchMethod() {}
        """.trimIndent()
        )

        val change = changeTracker.getLocalLinesChanges(link) as LinesChange
        Assertions.assertEquals(LinesChangeType.DELETED, change.linesChangeType)
        Assertions.assertEquals(change.requiresUpdate, true)
        Assertions.assertEquals(change.hasWorkingTreeChanges(), true)
        Assertions.assertEquals(change.fileChange.customChangeType, CustomChangeType.MODIFIED)
        Assertions.assertEquals(change.fileChange.afterPathString, "file.txt")
        Assertions.assertEquals(change.fileChange.deletionsAndAdditions, 0)
    }

    fun `test multiple lines partially moved with uncommitted file`() {
        val link = createDummyLinkToLines("file.md", "file.md", "file.txt#L1-L5")

        createLinkingFile(content = "[link](file.txt#L1-L5)")
        val initialFileContent = """
           public static void dummyMethod() {
                this.value = value;
                this.name = name;
                doSomethingMore();
            }
        """.trimIndent()
        val linkedFile = createLinkedFile(content = initialFileContent)

        linkedFile.write(
            """
          
         class DummyClass {
            
            public DummyClass() {
            }
            
            
           /**
           * Adding documentation lines
           * One more lines
           */
          public static void dummyMethod() {
                this.value = value;
          }
          
          public void myVeryDifferentMethod() {
                this.name = name;
                doSomethingMore();
            }
          
        """.trimIndent()
        )

        val change = changeTracker.getLocalLinesChanges(link) as LinesChange

        val expectedLines1 = mutableListOf(12, 13, 14)
        val expectedLines2 = mutableListOf(17, 18)

        Assertions.assertEquals(LinesChangeType.PARTIAL, change.linesChangeType)

        Assertions.assertNotNull(change.newLines)
        Assertions.assertEquals(2, change.newLines?.size)
        for ((index, line) in change.newLines!![0].withIndex()) {
            Assertions.assertEquals(expectedLines1[index], line.lineNumber)
        }

        for ((index, line) in change.newLines!![1].withIndex()) {
            Assertions.assertEquals(expectedLines2[index], line.lineNumber)
        }

        Assertions.assertEquals(change.requiresUpdate, true)
        Assertions.assertEquals(change.hasWorkingTreeChanges(), true)
        Assertions.assertEquals(change.fileChange.customChangeType, CustomChangeType.MODIFIED)
        Assertions.assertEquals(change.fileChange.afterPathString, "file.txt")
        Assertions.assertEquals(change.fileChange.deletionsAndAdditions, 0)
    }

    fun `test multiple lines fully moved with structure changes with uncommitted file and file moved`() {
        val link = createDummyLinkToLines("file.md", "file.md", "file.txt#L4-L8")

        createLinkingFile(content = "[link](file.txt#L4-L8)")
        val initialFileContent = """
            
            
            
           public static void dummyMethod() {
                this.value = value;
                this.name = name;
                doSomethingMore();
            }
        """.trimIndent()
        val linkedFile = createLinkedFile(content = initialFileContent)

        linkedFile.write(
            """
          
         class DummyClass {
            
            public DummyClass() {
            }
            
            
           /**
           * Adding documentation lines
           * One more lines
           */
           public void dummyMethod() {
                this.value = otherValue;
                doSomethingMuchMuchMore();
            }
        """.trimIndent()
        )

        // Create new directory and move linked file to new directory
        val dir = Executor.mkdir("mydirectory")
        val mvFile = File(dir.path, "file.txt")
        repo.mv(linkedFile.file, mvFile)

        val change = changeTracker.getLocalLinesChanges(link) as LinesChange
        val expectedLines = (12..15).toList()

        Assertions.assertEquals(LinesChangeType.FULL, change.linesChangeType)

        Assertions.assertNotNull(change.newLines)
        Assertions.assertEquals(1, change.newLines?.size)
        for ((index, line) in change.newLines!![0].withIndex()) {
            Assertions.assertEquals(expectedLines[index], line.lineNumber)
        }

        Assertions.assertEquals(change.requiresUpdate, true)
        Assertions.assertEquals(change.hasWorkingTreeChanges(), true)
        Assertions.assertEquals(change.fileChange.customChangeType, CustomChangeType.MOVED)
        Assertions.assertEquals(change.fileChange.afterPathString, "mydirectory/file.txt")
        Assertions.assertEquals(change.fileChange.deletionsAndAdditions, 0)
    }

    fun `test multiple lines null commit sha`() {
        val link = createDummyLinkToLine("file.md", "file.md", "file.txt#L1-L3")

        createLinkingFile(content = "[link](file.txt#L1-L3)")

        assertFailsWith<CommitSHAIsNullLinesException> {
            changeTracker.getLocalLinesChanges(link) as LinesChange
        }
    }

    private fun createDummyLinkToLine(
        proveniencePath: String = "file.md",
        fileName: String = "file.md",
        linkPath: String
    ): Link {
        return RelativeLinkToLine(
            LinkInfo(
                fileName = fileName,
                foundAtLineNumber = 1,
                linkPath = linkPath,
                linkText = "link",
                project = project,
                proveniencePath = proveniencePath,
                linkElement = LinkElementImpl(mock())
            )
        )
    }

    private fun createDummyLinkToLines(
        proveniencePath: String = "file.md",
        fileName: String = "file.md",
        linkPath: String
    ): Link {
        return RelativeLinkToLines(
            LinkInfo(
                fileName = fileName,
                foundAtLineNumber = 1,
                linkPath = linkPath,
                linkText = "link",
                project = project,
                proveniencePath = proveniencePath,
                linkElement = LinkElementImpl(mock())
            )
        )
    }

    private fun createDummyLinkToDirectory(
        proveniencePath: String = "file.md",
        fileName: String = "file.md",
        linkPath: String
    ): Link {
        return RelativeLinkToDirectory(
            LinkInfo(
                fileName = fileName,
                foundAtLineNumber = 1,
                linkPath = linkPath,
                linkText = "link",
                project = project,
                proveniencePath = proveniencePath,
                linkElement = LinkElementImpl(mock())
            )
        )
    }

    private fun createDummyLinkToFile(
        proveniencePath: String = "file.md",
        fileName: String = "file.md",
        linkPath: String
    ): Link {
        return RelativeLinkToFile(
            LinkInfo(
                fileName = fileName,
                foundAtLineNumber = 1,
                linkPath = linkPath,
                linkText = "link",
                project = project,
                proveniencePath = proveniencePath,
                linkElement = LinkElementImpl(mock())
            )
        )
    }

    private fun createLinkedFile(fileName: String? = null, content: String? = null): TestFile {
        val linkedFile: TestFile = if (fileName != null)
            file(fileName)
        else
            file("file.txt")

        if (content != null)
            linkedFile.create(content)
        else
            linkedFile.create("Some content")
        repo.add()
        repo.commit("Create linked file")
        return linkedFile
    }

    private fun createLinkedDirectory(fileName: String? = null, content: String? = null): TestFile {
        val linkedFile = createLinkedFile("dirFile.txt")

        // Create directory and commit files
        val dir = Executor.mkdir("dir")
        val mvFile = File(dir.path, "dirFile.txt")
        repo.mv(linkedFile.file, mvFile)
        repo.add()
        repo.commit("Create linked file in new directory")
        return TestFile(repo, dir)
    }

    private fun createLinkingFile(fileName: String? = null, content: String? = null): TestFile {
        val linkingFile: TestFile = if (fileName != null)
            file(fileName)
        else
            file("file.md")

        if (content != null)
            linkingFile.create(content)
        else
            linkingFile.create("[link](file.txt)")
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
        val newPrefix = prefix + "\t"
        for (child in node.children) {
            if (excludeGit && child.name == ".git") {
                continue
            }
            printVfsTree(child, newPrefix, excludeGit)
        }
    }
}
