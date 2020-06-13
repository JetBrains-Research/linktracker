package org.intellij.plugin.tracker.data.diff

import com.intellij.util.diff.Diff
import org.intellij.plugin.tracker.data.Line
import org.intellij.plugin.tracker.utils.GitOperationManager
import org.intellij.plugin.tracker.utils.LinkPatterns
import java.util.regex.Matcher
import kotlin.math.max
import kotlin.math.min

data class DiffOutput(
    val file: String?,
    val addedLines: MutableList<Line>,
    val deletedLines: MutableList<Line>,
    val beforeCommit: String,
    val afterCommit: String
) {
    override fun toString(): String {
        return "In file $file between commits $beforeCommit and $afterCommit added lines are $addedLines and deleted lines are $deletedLines"
    }

    companion object {

        private fun processDiffOutputLines(lines: List<String?>, contextLinesNumber: Int): Pair<MutableList<Line>, MutableList<Line>> {
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
                    val addedLine = Line(currentAddedLine, line.replaceFirst("+", ""))
                    addedLines.add(addedLine)
                    contextLinesAdded.add(addedLine)
                    currentAddedLine++
                    // deleted line
                } else if (line.startsWith("-")) {
                    val deletedLine = Line(currentDeletedLine, line.replaceFirst("-", ""))
                    deletedLines.add(deletedLine)
                    contextLinesDeleted.add(deletedLine)
                    currentDeletedLine++
                    // this is an unchanged line: just add it to the context lines lists and increment the indices
                } else {
                    contextLinesDeleted.add(Line(currentDeletedLine, line))
                    contextLinesAdded.add(Line(currentAddedLine, line))
                    currentAddedLine++
                    currentDeletedLine++
                }
            }

            populateContextLines(addedLines, contextLinesAdded, contextLinesNumber)
            populateContextLines(deletedLines, contextLinesDeleted, contextLinesNumber)

            return Pair(addedLines, deletedLines)
        }

        private fun populateContextLines(
            lines: MutableList<Line>,
            contextLinesList: MutableList<Line>,
            contextLinesNumber: Int
        ) {
            // populate the context lines properties of the added lines
            for (l: Line in lines) {
                val maxContextLineNumber: Int = contextLinesList.maxBy { line -> line.lineNumber }?.lineNumber ?: continue

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

        fun getDiffOutput(
            gitOperationManager: GitOperationManager,
            before: String,
            after: String,
            beforePath: String,
            afterPath: String,
            contextLinesNumber: Int = 3
        ): DiffOutput? {
            val output: String = if (after.isNotBlank()) {
                gitOperationManager.getDiffBetweenCommits(before, after, beforePath, afterPath, contextLinesNumber)
            } else {
                gitOperationManager.getDiffWithWorkingVersionOfFile(beforePath, afterPath, contextLinesNumber)
            }
            // skip the git diff header (first 4 lines)
            if (output.isEmpty()) return null
            // filter out all git-added "No newline at end of file" lines
            val lines: List<String?> = output.lines()
                .subList(4, output.lines().size)
                .filterNot { line -> line == "\\ No newline at end of file" }

            val diffOutputResult = processDiffOutputLines(lines, contextLinesNumber)

            return DiffOutput(beforePath, diffOutputResult.first, diffOutputResult.second, before, after)
        }
    }
}
