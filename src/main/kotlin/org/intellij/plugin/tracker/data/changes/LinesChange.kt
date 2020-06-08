package org.intellij.plugin.tracker.data.changes

import org.intellij.plugin.tracker.data.Line

enum class LinesChangeType(val change: String) : ChangeType {
    UNCHANGED("LINES UNCHANGED") {
        override val changeTypeString: String
            get() = change
    },
    PARTIAL("LINES PARTIALLY MOVED") {
        override val changeTypeString: String
            get() = change
    },
    FULL("LINES FULLY MOVED") {
        override val changeTypeString: String
            get() = change
    },
    DELETED("LINES DELETED") {
        override val changeTypeString: String
            get() = change
    },
    INVALID("LINES INVALID") {
        override val changeTypeString: String
            get() = change
    }
}


data class LinesChange(
    val fileChange: CustomChange,
    val linesChangeType: LinesChangeType,
    override val errorMessage: String? = null,
    val newLines: MutableList<MutableList<Line>>? = null
) : Change {
    override val changes: MutableList<ChangeType>
        get() = mutableListOf(fileChange.customChangeType, linesChangeType)

    override val requiresUpdate: Boolean
        get() {
            if (fileChange.requiresUpdate) return true
            if (linesChangeType == LinesChangeType.INVALID || linesChangeType == LinesChangeType.UNCHANGED) return false
            return true
        }

    override val afterPath: MutableList<String>
        get() {
            if (newLines == null) return mutableListOf()

            val afterPathList: MutableList<String> = mutableListOf()
            for (group: MutableList<Line> in newLines) {
                if (group.size == 0) {
                    continue
                } else if (group.size == 1) {
                    afterPathList.add(
                        "${fileChange.afterPathString}#L${group[0].lineNumber}"
                    )
                } else if (group.size > 1) {
                    afterPathList.add(
                        "${fileChange.afterPathString}#L${group[0].lineNumber}-L${group[group.size - 1].lineNumber}"
                    )
                }
            }
            return afterPathList
        }

    override fun hasWorkingTreeChanges(): Boolean = fileChange.hasWorkingTreeChanges()
}
