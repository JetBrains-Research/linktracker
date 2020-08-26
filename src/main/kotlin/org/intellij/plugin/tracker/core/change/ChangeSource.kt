package org.intellij.plugin.tracker.core.change

import org.intellij.plugin.tracker.data.OriginalLineContentsNotFoundException
import org.intellij.plugin.tracker.data.OriginalLinesContentsNotFoundException
import org.intellij.plugin.tracker.data.changes.Change
import org.intellij.plugin.tracker.data.changes.CustomChange
import org.intellij.plugin.tracker.data.diff.DiffOutput
import org.intellij.plugin.tracker.data.diff.FileHistory
import org.intellij.plugin.tracker.data.links.Link

/**
 * Base class for sources of change. Implemented by GitOperationManager and ChangeListOperationManager
 */
abstract class ChangeSource {

    /**
     * Gets the change for a file in the environment it is implemented
     */
    abstract fun getChangeForFile(link: Link): Change

    /**
     * Gets the change for a directory in the environment it is implemented
     */
    abstract fun getChangeForDirectory(link: Link): Change

    /**
     * Method that retrieves a list of lines (strings) of a file given by a link
     */
    abstract fun getLines(link: Link): List<String>?

    /**
     * Method that performs a diff between the versions of a file and retrieves the diff output.
     * (environment specific).
     */
    abstract fun performDiffOutput(before: FileHistory, after: FileHistory): DiffOutput?

    /**
     * Template method for getting a list of diff outputs between versions of a file.
     * Calls the performDiffOutput abstract method, which defines the environmental specific operations
     * of performing the diff.
     */
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

    /**
     * Get the original line content of a line that is targeted by a link.
     */
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

    /**
     * Get the original lines contents of lines targeted by a link
     */
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