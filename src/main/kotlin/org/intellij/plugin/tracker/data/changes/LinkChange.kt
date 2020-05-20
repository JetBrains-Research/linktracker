package org.intellij.plugin.tracker.data.changes

enum class ChangeType(change: String) {
    ADDED("ADDED"),
    MOVED("MOVED"),
    MODIFIED("MODIFIED"),
    DELETED("DELETED"),
    INVALID("INVALID")
}

open class LinkChange(
    open val changeType: ChangeType,
    open val afterPath: String,
    open val errorMessage: String? = null
) {
    override fun toString(): String {
        return "Change type is $changeType and after path is $afterPath"
    }
}

open class LineChange(
    open val file: String?,
    open val addedLines: MutableList<Int>,
    open val deletedLines: MutableList<Int>,
    open val beforeCommit: String,
    open val afterCommit: String
) {
    override fun toString(): String {
        return "In file $file between commits $beforeCommit and $afterCommit added lines are $addedLines and deleted lines are $deletedLines"
    }
}