package org.intellij.plugin.tracker.core.change

import org.intellij.plugin.tracker.data.OriginalLineContentsNotFoundException
import org.intellij.plugin.tracker.data.OriginalLinesContentsNotFoundException
import org.intellij.plugin.tracker.data.changes.Change
import org.intellij.plugin.tracker.data.changes.CustomChange
import org.intellij.plugin.tracker.data.diff.DiffOutput
import org.intellij.plugin.tracker.data.diff.FileHistory
import org.intellij.plugin.tracker.data.links.Link

abstract class ChangeSource {

    abstract fun getChangeForFile(link: Link): Change

    abstract fun getChangeForDirectory(link: Link): Change

    abstract fun getLines(link: Link): List<String>?

    abstract fun performDiffOutput(before: FileHistory, after: FileHistory): DiffOutput?

    fun getDiffOutput(fileChange: CustomChange): List<DiffOutput> {
        val diffOutputList: MutableList<DiffOutput> = mutableListOf()
        val fileHistoryList = fileChange.fileHistoryList
        if (fileHistoryList.size >= 2) {
            for (x: Int in 0 until fileHistoryList.size - 1) {
                val diffOutput = performDiffOutput(fileHistoryList[x], fileHistoryList[x + 1])
                if (diffOutput != null) diffOutputList.add(diffOutput)
            }
        }
        return diffOutputList
    }

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

    data class DirectoryInfo(val movedFiles: List<String>, val directorySize: Int)

}