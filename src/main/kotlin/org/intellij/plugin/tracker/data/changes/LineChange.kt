package org.intellij.plugin.tracker.data.changes

import org.intellij.plugin.tracker.data.Line

enum class LineChangeType(val change: String): ChangeType {
    DELETED("LINE DELETED") {
        override val changeTypeString: String
            get() = change
    },
    UNCHANGED("LINE UNCHANGED") {
        override val changeTypeString: String
            get() = change
    },
    MOVED("LINE MOVED") {
        override val changeTypeString: String
            get() = change
    },
    INVALID("LINE INVALID") {
        override val changeTypeString: String
            get() = change
    }
}

data class LineChange(
    val fileChange: CustomChange,
    val lineChangeType: LineChangeType,
    override val errorMessage: String? = null,
    val newLine: Line? = null
) : Change {
    override val changes: MutableList<ChangeType>
        get() = mutableListOf(fileChange.customChangeType, lineChangeType)

    override val requiresUpdate: Boolean
        get() {
            if (fileChange.requiresUpdate)
                return true
            if (lineChangeType == LineChangeType.MOVED || lineChangeType == LineChangeType.DELETED)
                return true
            return false
        }

    override fun hasWorkingTreeChanges(): Boolean = fileChange.hasWorkingTreeChanges()

    override val afterPath: MutableList<String>
        get() {
            if (newLine == null) return mutableListOf("")
            return mutableListOf("${fileChange.afterPathString}#L${newLine.lineNumber}")
        }
}
