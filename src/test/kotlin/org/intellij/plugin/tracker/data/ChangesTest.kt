package org.intellij.plugin.tracker.data

import junit.framework.TestCase
import org.intellij.plugin.tracker.data.changes.CustomChange
import org.intellij.plugin.tracker.data.changes.CustomChangeType
import org.intellij.plugin.tracker.data.changes.LineChange
import org.intellij.plugin.tracker.data.changes.LineChangeType
import org.intellij.plugin.tracker.data.changes.LinesChange
import org.intellij.plugin.tracker.data.changes.LinesChangeType
import org.intellij.plugin.tracker.data.diff.FileHistory
import org.junit.jupiter.api.Assertions

class ChangesTest : TestCase() {
    private val fileChange = CustomChange(
        customChangeType = CustomChangeType.MOVED,
        afterPathString = "dummypath.md",
        errorMessage = "dummy message",
        fileHistoryList = mutableListOf(FileHistory("commit sha", "dummy path", true)),
        deletionsAndAdditions = 10
    )

    private val otherFileChange = CustomChange(
        customChangeType = CustomChangeType.ADDED,
        afterPathString = "dummypath.md",
        fileHistoryList = mutableListOf()
    )

    fun testCustomChange() {
        Assertions.assertEquals(fileChange.afterPath, mutableListOf("dummypath.md"))
        Assertions.assertEquals(fileChange.changes, mutableListOf(CustomChangeType.MOVED))
        Assertions.assertEquals(fileChange.requiresUpdate, true)
        Assertions.assertEquals(otherFileChange.requiresUpdate, false)
        Assertions.assertEquals(fileChange.hasWorkingTreeChanges(), true)
        Assertions.assertEquals(otherFileChange.hasWorkingTreeChanges(), false)
        Assertions.assertEquals(fileChange.toString(), "Change type is MOVED and after path is [dummypath.md] " +
                "with error message dummy message")
    }

    fun testLineChange() {
        val otherChange = LineChange(
            fileChange = fileChange,
            lineChangeType = LineChangeType.DELETED,
            errorMessage = "dummy message",
            newLine = Line(5, "line text")
        )

        val lineChange = LineChange(
            fileChange = otherFileChange,
            lineChangeType = LineChangeType.MOVED
        )

        val otherLineChange = LineChange(
            fileChange = otherFileChange,
            lineChangeType = LineChangeType.INVALID
        )

        Assertions.assertEquals(otherChange.changes, mutableListOf(CustomChangeType.MOVED, LineChangeType.DELETED))
        Assertions.assertEquals(otherChange.requiresUpdate, true)
        Assertions.assertEquals(lineChange.requiresUpdate, true)
        Assertions.assertEquals(otherLineChange.requiresUpdate, false)
        Assertions.assertEquals(otherChange.hasWorkingTreeChanges(), true)
        Assertions.assertEquals(lineChange.hasWorkingTreeChanges(), false)
        Assertions.assertEquals(otherChange.afterPath, mutableListOf("dummypath.md#L5"))
        Assertions.assertEquals(lineChange.afterPath, mutableListOf(""))
    }

    fun testLinesChange() {
        val otherChange = LinesChange(
            fileChange = fileChange,
            linesChangeType = LinesChangeType.PARTIAL,
            errorMessage = "dummy message",
            newLines = mutableListOf(mutableListOf(Line(5, "line text")),
                mutableListOf(Line(3, "other text")), mutableListOf())
        )

        val linesChange = LinesChange(
            fileChange = otherFileChange,
            linesChangeType = LinesChangeType.INVALID,
            newLines = mutableListOf()
        )

        val otherLinesChange = LinesChange(
            fileChange = otherFileChange,
            linesChangeType = LinesChangeType.FULL,
            newLines = mutableListOf(mutableListOf(Line(5, "line text"), Line(3, "other text")))
        )

        Assertions.assertEquals(otherChange.changes, mutableListOf(CustomChangeType.MOVED, LinesChangeType.PARTIAL))
        Assertions.assertEquals(otherChange.requiresUpdate, true)
        Assertions.assertEquals(linesChange.requiresUpdate, false)
        Assertions.assertEquals(otherLinesChange.requiresUpdate, true)
        Assertions.assertEquals(otherChange.hasWorkingTreeChanges(), true)
        Assertions.assertEquals(linesChange.hasWorkingTreeChanges(), false)
        Assertions.assertEquals(otherChange.afterPath, mutableListOf("dummypath.md#L5", "dummypath.md#L3"))
        Assertions.assertEquals(linesChange.afterPath, mutableListOf<String>())
        Assertions.assertEquals(otherLinesChange.afterPath, mutableListOf("dummypath.md#L5-L3"))
    }
}
