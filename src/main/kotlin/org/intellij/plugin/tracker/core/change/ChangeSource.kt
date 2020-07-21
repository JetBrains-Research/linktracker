package org.intellij.plugin.tracker.core.change

import org.intellij.plugin.tracker.data.OriginalLineContentsNotFoundException
import org.intellij.plugin.tracker.data.OriginalLinesContentsNotFoundException
import org.intellij.plugin.tracker.data.changes.Change
import org.intellij.plugin.tracker.data.changes.CustomChange
import org.intellij.plugin.tracker.data.diff.DiffOutput
import org.intellij.plugin.tracker.data.links.Link

abstract class ChangeSource {

    abstract fun getChangeForFile(link: Link): Change

    abstract fun getDiffOutput(fileChange: CustomChange): List<DiffOutput>

    abstract fun getLines(link: Link): List<String>?

    abstract fun getDirectoryInfo(link: Link): DirectoryInfo

    fun getOriginalLineContents(link: Link): String {
        val lines = getLines(link)
        val lineNumber = link.lineReferenced
        if (lines != null) {
            if (lineNumber <= lines.size) {
                return lines[lineNumber - 1]
            }
        }
        throw OriginalLineContentsNotFoundException()
    }

    fun getMultipleOriginalLinesContents(link: Link): List<String> {
        val lines = getLines(link)
        val startLine = link.referencedStartingLine
        val endLine = link.referencedEndingLine
        if (lines != null) {
            if (startLine <= lines.size && endLine <= lines.size) {
                return lines.subList(startLine - 1, endLine)
            }
        }
        throw OriginalLinesContentsNotFoundException()
    }

    data class DirectoryInfo (val movedFiles: List<String>, val directorySize: Int)

}