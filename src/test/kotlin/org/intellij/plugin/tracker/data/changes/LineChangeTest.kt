package org.intellij.plugin.tracker.data.changes

import junit.framework.TestCase
import org.intellij.plugin.tracker.data.Line
import org.intellij.plugin.tracker.data.diff.FileHistory
import org.junit.jupiter.api.Assertions

/**
 * This class tests the changes of a line.
 */
class LineChangeTest : TestCase() {

    private lateinit var fileChange: CustomChange
    private lateinit var lineChange: LineChange
    private lateinit var otherChange: LineChange

    override fun setUp() {
        super.setUp()
        fileChange = CustomChange(
            customChangeType = CustomChangeType.DELETED,
            afterPathString = "dummypath.md",
            errorMessage = "dummy message",
            fileHistoryList = mutableListOf(FileHistory("commit sha", "dummy path", true)),
            deletionsAndAdditions = 10
        )

        lineChange = LineChange(
            fileChange = fileChange,
            lineChangeType = LineChangeType.DELETED,
            errorMessage = "dummy message",
            newLine = Line(5, "line text")
        )

        otherChange = LineChange(
            fileChange = CustomChange(CustomChangeType.MODIFIED, "dummy path"),
            lineChangeType = LineChangeType.MOVED
        )
    }

    fun testRequiresUpdate() {
        val otherLineChange = LineChange(
            fileChange = CustomChange(CustomChangeType.INVALID, "dummy path"),
            lineChangeType = LineChangeType.INVALID
        )
        Assertions.assertEquals(lineChange.requiresUpdate, true)
        Assertions.assertEquals(otherChange.requiresUpdate, true)
        Assertions.assertEquals(otherLineChange.requiresUpdate, false)
    }

    fun testHasWorkingTreeChanges() {
        Assertions.assertEquals(lineChange.hasWorkingTreeChanges(), true)
        Assertions.assertEquals(otherChange.hasWorkingTreeChanges(), false)
    }

    fun testAfterPath() {
        Assertions.assertEquals(lineChange.afterPath, mutableListOf("dummypath.md#L5"))
        Assertions.assertEquals(otherChange.afterPath, mutableListOf(""))
    }

    fun testConstructor() {
        Assertions.assertEquals(lineChange.fileChange, fileChange)
        Assertions.assertEquals(lineChange.lineChangeType, LineChangeType.DELETED)
        Assertions.assertEquals(lineChange.errorMessage, "dummy message")
        Assertions.assertEquals(lineChange.newLine,
            Line(5, "line text")
        )
    }

    fun testGetters() {
        Assertions.assertEquals(lineChange.changes, mutableListOf(CustomChangeType.DELETED, LineChangeType.DELETED))
    }
}
