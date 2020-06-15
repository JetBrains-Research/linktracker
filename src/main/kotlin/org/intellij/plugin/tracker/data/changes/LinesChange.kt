package org.intellij.plugin.tracker.data.changes

import org.intellij.plugin.tracker.data.Line


/**
 * Enum class for change types of multiple lines
 */
enum class LinesChangeType(val change: String) : ChangeType {

    /**
     * The lines have not changed their line numbers or contents
     */
    UNCHANGED("LINES UNCHANGED") {
        override val changeTypeString: String
            get() = change
    },

    /**
     * The lines have partially moved
     *
     * Multiple subgroups of lines out of the original group of lines
     * has been identified
     */
    PARTIAL("LINES PARTIALLY MOVED") {
        override val changeTypeString: String
            get() = change
    },

    /**
     * Lines have been fully moved
     *
     * That is, we have identified only 1 group of lines
     */
    FULL("LINES FULLY MOVED") {
        override val changeTypeString: String
            get() = change
    },

    /**
     * All of the referenced lines have been deleted
     */
    DELETED("LINES DELETED") {
        override val changeTypeString: String
            get() = change
    },

    /**
     * An error occurred while gathering the changes of those lines
     */
    INVALID("LINES INVALID") {
        override val changeTypeString: String
            get() = change
    }
}

/**
 * Change class for links to multiple lines
 *
 * Implements the Change interface and implements it's own logic
 * for the properties defined in the interface
 */
data class LinesChange(

    /**
     * Change object of the file in which the lines are located
     */
    val fileChange: CustomChange,

    /**
     * Lines change type property
     */
    val linesChangeType: LinesChangeType,

    /**
     * Error message if something wrong happened while retrieving the change
     */
    override val errorMessage: String? = null,

    /**
     * A list of lists of new, mapped line objects
     * Each list represents a newly identified group of lines based on the initial group of lines
     *
     * For example, a link referencing lines 21-25 over time could have changed into 2 groups of lines
     * 33-35 and 40-42.
     */
    val newLines: MutableList<MutableList<Line>>? = null
) : Change {

    /**
     * Changes are made up of the change of the file in which the lines are located
     * together with the change of the lines
     */
    override val changes: MutableList<ChangeType>
        get() = mutableListOf(fileChange.customChangeType, linesChangeType)

    /**
     * This change requires an update if the file in which the lines are located require an update
     * or if the lines change type is either partially moved or fully moved
     */
    override val requiresUpdate: Boolean
        get() {
            if (fileChange.requiresUpdate) return true
            if (linesChangeType == LinesChangeType.INVALID || linesChangeType == LinesChangeType.UNCHANGED) return false
            return true
        }

    /**
     * There can be multiple possible after paths for a link to multiple lines
     *
     * If the new lines list contains multiple groups, then there will be a separate
     * new after path for each group
     */
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

    /**
     * Checks whether the file in which these lines are located has working tree changes
     */
    override fun hasWorkingTreeChanges(): Boolean = fileChange.hasWorkingTreeChanges()
}
