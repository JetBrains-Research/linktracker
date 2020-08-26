package org.intellij.plugin.tracker.utils

import org.intellij.plugin.tracker.data.diff.Line
import java.util.regex.Matcher
import kotlin.math.max
import kotlin.math.min

/**
 * This method processes the output of a git diff command by going through each line of this output,
 * determining the `added` lines, `deleted` lines, as well as the context lines for each of these lines
 * It will return a DiffOutput object, containing all the information described above.
 */
internal fun processDiffOutputLines(
    lines: List<String?>,
    contextLinesNumber: Int = 3
): Pair<MutableList<Line>, MutableList<Line>> {
    val addedLines: MutableList<Line> = mutableListOf()
    val deletedLines: MutableList<Line> = mutableListOf()
    val contextLinesDeleted: MutableList<Line> = mutableListOf()
    val contextLinesAdded: MutableList<Line> = mutableListOf()

    var startDeletedLine: Int
    var startAddedLine: Int
    var currentAddedLine = 0
    var currentDeletedLine = 0

    for (line: String? in lines) {
        if (line == null) {
            break
            // git hunk info header
        } else if (line.startsWith("@@ ")) {
            val info = line.split(" @@")[0]
            val matcher: Matcher = LinkPatterns.GitDiffChangedLines.pattern.matcher(info)
            if (matcher.matches()) {
                startDeletedLine = matcher.group(1).toInt()
                currentDeletedLine = startDeletedLine
                startAddedLine = matcher.group(6).toInt()
                currentAddedLine = startAddedLine
            }
            // added line
        } else if (line.startsWith("+")) {
            val addedLine = Line(
                currentAddedLine,
                line.replaceFirst("+", "")
            )
            addedLines.add(addedLine)
            contextLinesAdded.add(addedLine)
            currentAddedLine++
            // deleted line
        } else if (line.startsWith("-")) {
            val deletedLine = Line(
                currentDeletedLine,
                line.replaceFirst("-", "")
            )
            deletedLines.add(deletedLine)
            contextLinesDeleted.add(deletedLine)
            currentDeletedLine++
            // this is an unchanged line: just add it to the context lines lists and increment the indices
        } else {
            contextLinesDeleted.add(
                Line(
                    currentDeletedLine,
                    line
                )
            )
            contextLinesAdded.add(Line(currentAddedLine, line))
            currentAddedLine++
            currentDeletedLine++
        }
    }
    populateContextLines(addedLines, contextLinesAdded, contextLinesNumber)
    populateContextLines(deletedLines, contextLinesDeleted, contextLinesNumber)
    return Pair(addedLines, deletedLines)
}

/**
 * This auxiliary method will populate each line in `lines` with the corresponding context lines of this line
 *
 * It will take `contextLinesNumber` number of context lines before (above) and `contextLinesNumber`
 * number of context lines after (below) the target line.
 */
private fun populateContextLines(
    lines: MutableList<Line>,
    contextLinesList: MutableList<Line>,
    contextLinesNumber: Int
) {
    // populate the context lines properties of the added lines
    for (l: Line in lines) {
        val maxContextLineNumber: Int =
            contextLinesList.maxBy { line -> line.lineNumber }?.lineNumber ?: continue

        // get all of the context lines on the upper side of the line:
        // that is, the lines within [current_line_number - contextLinesNumber, current_line_number)
        // as well as all of the context line on the lower side of the line:
        // all of the lines within the interval (current_line_number, current_line-number+ contextLinesNumber)
        val contextLines: MutableList<Line> = contextLinesList.filter { line ->
            (line.lineNumber < l.lineNumber && line.lineNumber >= max(0, l.lineNumber - contextLinesNumber)) ||
                    (line.lineNumber > l.lineNumber && line.lineNumber <= min(
                        l.lineNumber + contextLinesNumber,
                        maxContextLineNumber
                    ))
        }.toMutableList()
        l.contextLines = contextLines
    }
}