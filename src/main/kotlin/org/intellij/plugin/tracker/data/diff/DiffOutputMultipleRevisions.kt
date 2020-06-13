package org.intellij.plugin.tracker.data.diff

import org.intellij.plugin.tracker.data.changes.CustomChange

/**
 * A data class that stores all the information needed
 * for the LineTracker class in order to map line(s) from an old location
 * to a new location the file.
 */
data class DiffOutputMultipleRevisions(

    /**
     * The change of the file in which the line(s) are located
     */
    val fileChange: CustomChange,

    /**
     * A list of diff outputs between the versions of the file
     * in which the line(s) are located
     */
    val diffOutputList: MutableList<DiffOutput>,

    /**
     * Original line content (in case of a single line referenced)
     * That is, the contents of the line in the file at the moment the link was created
     */
    val originalLineContent: String = "",

    /**
     * Original line content list (in case of multiple lines referenced)
     * That is, the contents of the lines in the file at the moment the link was created
     */
    val originalLinesContents: List<String> = mutableListOf()
)
