package org.intellij.plugin.tracker.data.diff

/**
 * Data class representing the output of a `git diff` command
 */
data class DiffOutput(

    /**
     * The name of the file on which the git diff command is executed
     */
    val file: String?,

    /**
     * A list containing the added lines (annotated with a + prefix)
     * from the output of `git diff` command
     */
    val addedLines: MutableList<Line>,

    /**
     * A list containing the deleted lines (annotated with a - prefix)
     * from the output of git diff command
     */
    val deletedLines: MutableList<Line>
) {
    override fun toString(): String {
        return "In file $file added lines are $addedLines and deleted lines are $deletedLines"
    }
}
