package org.intellij.plugin.tracker.data.diff

/**
 * Data class that stores a path that is associated with a commit SHA
 * This path can also come from the working tree, in which case it is not associated
 * with any commit SHA.
 */
data class FileHistory(

    /**
     * A string representing a commit SHA
     * It has a prefix of 'Commit: ' followed by the commit SHA
     */
    private val rev: String = "",

    /**
     * The path of the file at the commit given by `rev` property
     */
    val path: String,

    /**
     * Boolean indicating whether the path comes from the working tree or not
     */
    val fromWorkingTree: Boolean = false
) {
    /**
     * Get the commit SHA from the rev field in case the object
     * is not coming from the working tree (in that case it has no commit SHA)
     *
     */
    val revision: String = rev
        get() {
            return if (field.isBlank()) field
            else field.split("Commit: ")[1]
        }
}
