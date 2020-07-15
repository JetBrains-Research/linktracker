package org.intellij.plugin.tracker.data.diff

/**
 * A class that defines the characteristics of a line
 */
data class Line(

    /**
     * The number at which this line appears in the file
     */
    val lineNumber: Int,

    /**
     * The content of this line
     */
    val content: String,

    /**
     * A list of context lines of this line
     */
    var contextLines: MutableList<Line>? = null
) {
    override fun toString(): String = "($lineNumber, $content)"
}
