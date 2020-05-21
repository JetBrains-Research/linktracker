package org.intellij.plugin.tracker.data

data class Line(

    val lineNumber: Int,

    val content: String,

    var contextLines: MutableList<Line>
) {
    override fun toString(): String {
        return "line is \n" +
                "$content \n" +
                "with line number $lineNumber"
    }
}
