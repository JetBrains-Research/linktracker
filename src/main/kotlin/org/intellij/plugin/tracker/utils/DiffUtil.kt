package org.intellij.plugin.tracker.utils

import org.intellij.plugin.tracker.data.diff.Line
import kotlin.math.abs

internal fun getLinesFromFileContents(fileContents: String, startLine: Int, endLine: Int): List<Line> =
    fileContents.lines().subList(startLine, endLine)
        .mapIndexed { index, s ->
            Line(
                index + startLine + 1,
                s,
                contextLines = getContextLinesFromFileContents(fileContents, index + startLine)
            )
        }

internal fun getContextLinesFromFileContents(
    fileContents: String,
    lineIndex: Int,
    contextLines: Int = 3
): MutableList<Line> {
    val lines = fileContents.lines()
    val contextLinesList: MutableList<Line> = mutableListOf()
    val differenceUpperSide = lineIndex - contextLines
    val differenceUnderSide = lines.size - lineIndex

    val upperSideRange: Int
    val underSideRange: Int
    if ((differenceUnderSide < contextLines && differenceUpperSide < 0)
        || (differenceUnderSide > contextLines && differenceUpperSide >= 0)
    ) {
        underSideRange = contextLines
        upperSideRange = contextLines
    } else if (differenceUnderSide <= contextLines && differenceUpperSide >= 0) {
        underSideRange = contextLines
        upperSideRange = (contextLines + (contextLines - differenceUnderSide) + 1)
    } else {
        underSideRange = contextLines + abs(differenceUpperSide)
        upperSideRange = contextLines
    }
    populateUpperContextLines(contextLinesList, lines, lineIndex, upperSideRange)
    populateUnderContextLines(contextLinesList, lines, lineIndex, underSideRange)
    return contextLinesList
}

internal fun populateUpperContextLines(
    contextLinesList: MutableList<Line>,
    lines: List<String>,
    startLine: Int,
    range: Int
) {
    var tempStartLine = startLine - 1
    var tempRange = range
    while (tempRange != 0 && tempStartLine >= 0) {
        contextLinesList.add(
            Line(
                tempStartLine + 1,
                lines[tempStartLine]
            )
        )
        tempStartLine--
        tempRange--
    }
}

internal fun populateUnderContextLines(
    contextLinesList: MutableList<Line>,
    lines: List<String>,
    startLine: Int,
    range: Int
) {
    var tempStartLine = startLine + 1
    var tempRange = range
    while (tempRange != 0 && tempStartLine < lines.size) {
        contextLinesList.add(
            Line(
                tempStartLine + 1,
                lines[tempStartLine]
            )
        )
        tempStartLine++
        tempRange--
    }
}