package org.intellij.plugin.tracker.data.diff

import org.intellij.plugin.tracker.data.Line

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
    val deletedLines: MutableList<Line>,

    /**
     * The commit SHA of the `older` version of the file passed to the git diff command
     */
    val beforeCommit: String,

    /**
     * The commit SHA of the `newer` version of the file passed to the git diff command
     */
    val afterCommit: String
)
