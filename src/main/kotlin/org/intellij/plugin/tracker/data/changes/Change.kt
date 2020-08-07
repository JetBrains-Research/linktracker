package org.intellij.plugin.tracker.data.changes

/**
 * Base interface of ChangeType enum classes
 */
interface ChangeType {
    val changeTypeString: String
}

/**
 * Interface that corresponds to a Change object
 * This interface defines the common properties of all change classes
 */
interface Change {

    /**
     * Indicates whether the change requires to be updated
     */
    val requiresUpdate: Boolean

    /**
     * A list of possible after paths
     */
    val afterPath: MutableList<String>

    /**
     * Optional error message - in case something went wrong while
     * gathering the change
     */
    val errorMessage: String?

    /**
     * A list of ChangeType enum objects, which will show
     * all changes that affected a certain link
     */
    val changes: MutableList<ChangeType>

    /**
     * Function that indicates whether a change contains any working tree changes
     */
    fun hasWorkingTreeChanges(): Boolean

    fun isChangeDelete(): Boolean = changes.any { ch -> ch.changeTypeString.contains("DELETED") }
}
