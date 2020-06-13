package org.intellij.plugin.tracker.data.changes

import org.intellij.plugin.tracker.data.Line

/**
 * Enum class for change types of a single line
 */
enum class LineChangeType(val change: String) : ChangeType {

    /**
     * Line is deleted
     */
    DELETED("LINE DELETED") {
        override val changeTypeString: String
            get() = change
    },

    /**
     * Line is unchanged -- it has the same line number, but the contents
     * might or might not have changed
     * (if changed, it is within the line similarity threshold).
     */
    UNCHANGED("LINE UNCHANGED") {
        override val changeTypeString: String
            get() = change
    },

    /**
     * Line is moved - it has another line number but similar, if not same contents
     */
    MOVED("LINE MOVED") {
        override val changeTypeString: String
            get() = change
    },

    /**
     * An error occurred while gathering the changes of that line
     */
    INVALID("LINE INVALID") {
        override val changeTypeString: String
            get() = change
    }
}

/**
 * Change class for links to a single line
 *
 * Implements the Change interface and implements it's own logic
 * for the properties defined in the interface
 */
data class LineChange(

    /**
     * Change object of the file in which the line is located
     */
    val fileChange: CustomChange,

    /**
     * Line change type property
     */
    val lineChangeType: LineChangeType,

    /**
     * Error message if something wrong happened while retrieving the change
     */
    override val errorMessage: String? = null,

    /**
     * The new, mapped line object
     */
    val newLine: Line? = null
) : Change {

    /**
     * Changes are made up of the change of the file in which the line is located
     * together with the change of the line
     */
    override val changes: MutableList<ChangeType>
        get() = mutableListOf(fileChange.customChangeType, lineChangeType)

    /**
     * This change requires an update if the file in which the line is located requires an updated
     * or if the line change type is either moved or deleted
     */
    override val requiresUpdate: Boolean
        get() {
            if (fileChange.requiresUpdate)
                return true
            if (lineChangeType == LineChangeType.MOVED || lineChangeType == LineChangeType.DELETED)
                return true
            return false
        }

    /**
     * Check whether the file change property has working tree changes
     */
    override fun hasWorkingTreeChanges(): Boolean = fileChange.hasWorkingTreeChanges()

    /**
     * The one and only possible after path is given by the after path of the file change property
     * and the new line's line number.
     */
    override val afterPath: MutableList<String>
        get() {
            if (newLine == null) return mutableListOf("")
            return mutableListOf("${fileChange.afterPathString}#L${newLine.lineNumber}")
        }
}
