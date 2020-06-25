package org.intellij.plugin.tracker.data.changes

import org.intellij.plugin.tracker.data.diff.FileHistory

/**
 * Enum class for change types of files and directories
 */
enum class CustomChangeType(val change: String) : ChangeType {

    /**
     * File / directory is added
     */
    ADDED("ADDED") {
        override val changeTypeString: String
            get() = change
    },

    /**
     * File / directory is moved
     */
    MOVED("MOVED") {
        override val changeTypeString: String
            get() = change
    },

    /**
     * File / directory is modified
     */
    MODIFIED("MODIFIED") {
        override val changeTypeString: String
            get() = change
    },

    /**
     * File / directory is deleted
     */
    DELETED("DELETED") {
        override val changeTypeString: String
            get() = change
    },

    /**
     * Invalid corresponds to the case where an exception has been thrown
     * while gathering the change
     */
    INVALID("INVALID") {
        override val changeTypeString: String
            get() = change
    }
}

/**
 * Change class for links to files and directories
 *
 * Implements the Change interface and implements it's own logic
 * for the properties defined in the interface
 */
data class CustomChange(

    /**
     * The change type of this change class.
     */
    val customChangeType: CustomChangeType,

    /**
     * The after path
     */
    val afterPathString: String,

    /**
     * Error message in case something went wrong while gathering the changes
     */
    override val errorMessage: String? = null,

    /**
     * A list of FileHistory objects for a linked file, containing the paths and revisions
     * discovered of a file during the traversal of git history.
     */
    var fileHistoryList: MutableList<FileHistory> = mutableListOf(),

    /**
     * Number of deletions and additions that have been encountered for the file
     * during the traversal of Git history.
     */
    var deletionsAndAdditions: Int = 0
) : Change {

    /**
     * One possible after path, given by the afterPathString.
     */
    override val afterPath: MutableList<String>
        get() = mutableListOf(afterPathString)

    /**
     * List containing one element: the change type of this change.
     */
    override val changes: MutableList<ChangeType>
        get() = mutableListOf(customChangeType)

    /**
     * A change of this type requires an update if the change type is either moved or deleted
     */
    override val requiresUpdate: Boolean
        get() {
            if (customChangeType == CustomChangeType.MOVED || customChangeType == CustomChangeType.DELETED)
                return true
            return false
        }

    /**
     * Checks the last element in the file history list
     * Checks whether this element is from the working tree
     */
    override fun hasWorkingTreeChanges(): Boolean {
        return try {
            fileHistoryList.last().fromWorkingTree
        } catch (e: NoSuchElementException) {
            false
        }
    }

    override fun toString(): String {
        return "Change type is $customChangeType and after path is $afterPath with error message $errorMessage"
    }
}
