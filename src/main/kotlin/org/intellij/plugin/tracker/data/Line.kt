package org.intellij.plugin.tracker.data

data class Line(

    val lineNumber: Int,

    val content: String,

    var contextLines: MutableList<Line>
) {
    override fun toString(): String {
        return "Line($lineNumber, $content ${contextLines.map { l->l.lineNumber }})"
    }
}
