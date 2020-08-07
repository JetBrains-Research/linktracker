package org.intellij.plugin.tracker.data.changes

import junit.framework.TestCase
import org.intellij.plugin.tracker.data.diff.FileHistory
import org.junit.jupiter.api.Assertions

/**
 * This class tests the custom changes such as file, directory change.
 */
class CustomChangeTest : TestCase() {

    private lateinit var change: CustomChange
    private lateinit var otherChange: CustomChange

    override fun setUp() {
        super.setUp()
        change = CustomChange(
            customChangeType = CustomChangeType.MOVED,
            afterPathString = "dummypath.md",
            errorMessage = "dummy message",
            fileHistoryList = mutableListOf(FileHistory("commit sha", "dummy path", fromWorkingTree = true)),
            deletionsAndAdditions = 10
        )

        otherChange = CustomChange(
            customChangeType = CustomChangeType.ADDED,
            afterPathString = "dummypath.md",
            fileHistoryList = mutableListOf()
        )
    }

    fun testToString() {
        Assertions.assertEquals(change.toString(), "Change type is MOVED and " +
                "after path is [dummypath.md] with error message dummy message")
    }

    fun testHasWorkingTreeChanges() {
        Assertions.assertEquals(change.hasWorkingTreeChanges(), true)
        Assertions.assertEquals(otherChange.hasWorkingTreeChanges(), false)
    }

    fun testRequiresUpdate() {
        Assertions.assertEquals(change.requiresUpdate, true)
        Assertions.assertEquals(otherChange.requiresUpdate, false)
    }

    fun testConstructor() {
        Assertions.assertEquals(change.customChangeType, CustomChangeType.MOVED)
        Assertions.assertEquals(change.errorMessage, "dummy message")
        Assertions.assertEquals(change.afterPathString, "dummypath.md")
        Assertions.assertEquals(change.fileHistoryList, mutableListOf(FileHistory("commit sha", "dummy path", fromWorkingTree = true)))
        Assertions.assertEquals(change.deletionsAndAdditions, 10)
    }

    fun testGetters() {
        Assertions.assertEquals(change.afterPath, mutableListOf("dummypath.md"))
        Assertions.assertEquals(change.changes, mutableListOf(CustomChangeType.MOVED))
    }

    fun testChangeTypeString() {
        val deletedChange = change.copy(customChangeType = CustomChangeType.DELETED)
        Assertions.assertEquals(deletedChange.customChangeType.changeTypeString, "DELETED")

        val invalidChange = change.copy(customChangeType = CustomChangeType.INVALID)
        Assertions.assertEquals(invalidChange.customChangeType.changeTypeString, "INVALID")

        val movedChange = change.copy(customChangeType = CustomChangeType.MOVED)
        Assertions.assertEquals(movedChange.customChangeType.changeTypeString, "MOVED")

        val modifiedChange = change.copy(customChangeType = CustomChangeType.MODIFIED)
        Assertions.assertEquals(modifiedChange.customChangeType.changeTypeString, "MODIFIED")

        val addedChange = change.copy(customChangeType = CustomChangeType.ADDED)
        Assertions.assertEquals(addedChange.customChangeType.changeTypeString, "ADDED")
    }
}
