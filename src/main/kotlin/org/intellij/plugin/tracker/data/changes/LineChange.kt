package org.intellij.plugin.tracker.data.changes

import org.intellij.plugin.tracker.data.Line

open class LineChange(
    open val file: String?,
    open val addedLines: MutableList<Line>,
    open val deletedLines: MutableList<Line>,
    open val beforeCommit: String,
    open val afterCommit: String
) {
    override fun toString(): String {
        return "In file $file between commits $beforeCommit and $afterCommit added lines are $addedLines and deleted lines are $deletedLines"
    }
}