package org.intellij.plugin.tracker.data.changes

import junit.framework.TestCase
import org.intellij.plugin.tracker.data.diff.Line
import org.intellij.plugin.tracker.data.diff.FileHistory
import org.junit.jupiter.api.Assertions

/**
 * This class tests the changes of lines.
 */
class LinesChangeTest : TestCase() {

    private lateinit var fileChange: CustomChange
    private lateinit var linesChange: LinesChange
    private lateinit var otherChange: LinesChange

    override fun setUp() {
        super.setUp()
        fileChange = CustomChange(
            customChangeType = CustomChangeType.DELETED,
            afterPathString = "dummypath.md",
            errorMessage = "dummy message",
            fileHistoryList = mutableListOf(FileHistory("commit sha", "dummy path", true)),
            deletionsAndAdditions = 10
        )

        linesChange = LinesChange(
            fileChange = fileChange,
            linesChangeType = LinesChangeType.PARTIAL,
            errorMessage = "dummy message",
            newLines = mutableListOf(mutableListOf(
                Line(
                    5,
                    "line text"
                )
            ),
                mutableListOf(Line(3, "other text")), mutableListOf())
        )

        otherChange = LinesChange(
            fileChange = CustomChange(CustomChangeType.MODIFIED, "dummy path"),
            linesChangeType = LinesChangeType.INVALID,
            newLines = mutableListOf()
        )
    }

    fun testRequiresUpdate() {
        val otherLinesChange = LinesChange(
            fileChange = CustomChange(CustomChangeType.INVALID, "dummy path"),
            linesChangeType = LinesChangeType.DELETED
        )
        Assertions.assertEquals(linesChange.requiresUpdate, true)
        Assertions.assertEquals(otherChange.requiresUpdate, false)
        Assertions.assertEquals(otherLinesChange.requiresUpdate, true)
    }

    fun testHasWorkingTreeChanges() {
        Assertions.assertEquals(linesChange.hasWorkingTreeChanges(), true)
        Assertions.assertEquals(otherChange.hasWorkingTreeChanges(), false)
    }

    fun testAfterPath() {
        val otherLinesChange = LinesChange(
            fileChange = fileChange,
            linesChangeType = LinesChangeType.FULL,
            newLines = mutableListOf(mutableListOf(
                Line(5, "line text"),
                Line(3, "other text")
            ))
        )
        Assertions.assertEquals(linesChange.afterPath, mutableListOf("dummypath.md#L5", "dummypath.md#L3"))
        Assertions.assertEquals(otherChange.afterPath, mutableListOf<String>())
        Assertions.assertEquals(otherLinesChange.afterPath, mutableListOf("dummypath.md#L5-L3"))
    }

    fun testConstructor() {
        Assertions.assertEquals(linesChange.fileChange, fileChange)
        Assertions.assertEquals(linesChange.linesChangeType, LinesChangeType.PARTIAL)
        Assertions.assertEquals(linesChange.errorMessage, "dummy message")
        Assertions.assertEquals(linesChange.newLines, mutableListOf(mutableListOf(
            Line(
                5,
                "line text"
            )
        ),
            mutableListOf(Line(3, "other text")), mutableListOf()))
    }

    fun testGetters() {
        Assertions.assertEquals(linesChange.changes, mutableListOf(CustomChangeType.DELETED, LinesChangeType.PARTIAL))
    }

    fun testChangeTypeString() {
        val partialChange = linesChange.copy(linesChangeType = LinesChangeType.PARTIAL)
        Assertions.assertEquals(partialChange.linesChangeType.changeTypeString, "LINES PARTIALLY MOVED")

        val fullyChange = linesChange.copy(linesChangeType = LinesChangeType.FULL)
        Assertions.assertEquals(fullyChange.linesChangeType.changeTypeString, "LINES FULLY MOVED")

        val invalidChange = linesChange.copy(linesChangeType = LinesChangeType.INVALID)
        Assertions.assertEquals(invalidChange.linesChangeType.changeTypeString, "LINES INVALID")

        val unchangedChange = linesChange.copy(linesChangeType = LinesChangeType.UNCHANGED)
        Assertions.assertEquals(unchangedChange.linesChangeType.changeTypeString, "LINES UNCHANGED")

        val deletedChange = linesChange.copy(linesChangeType = LinesChangeType.DELETED)
        Assertions.assertEquals(deletedChange.linesChangeType.changeTypeString, "LINES DELETED")
    }
}
