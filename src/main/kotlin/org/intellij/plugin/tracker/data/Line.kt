package org.intellij.plugin.tracker.data

data class Line(

    val lineNumber: Int,

    val content: String,

    var contextLines: MutableList<Line>? = null
) {
    override fun toString(): String {
        return "line is " +
                "$content and " +
                "with line number $lineNumber"
    }
}
