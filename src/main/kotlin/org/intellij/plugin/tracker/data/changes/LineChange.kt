package org.intellij.plugin.tracker.data.changes

import org.intellij.plugin.tracker.data.Line

enum class LineChangeType(val change: String) {
    DELETED("DELETED"),
    UNCHANGED("UNCHANGED"),
    CHANGED("CHANGED")
}

data class LineChange(
    val fileChange: FileChange,
    val lineChangeType: LineChangeType? = null,
    override val errorMessage: String? = null,
    val newLine: Line? = null
) : Change {
    override val changeType: ChangeType
        get() = fileChange.changeType

    override fun hasWorkingTreeChanges(): Boolean = fileChange.hasWorkingTreeChanges()

    override val afterPath: String
        get() {
            if (newLine == null) return ""
            return "${fileChange.afterPath}#L${newLine.lineNumber}"
        }
}
