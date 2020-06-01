package org.intellij.plugin.tracker.data.diff

import org.intellij.plugin.tracker.data.Line

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
}