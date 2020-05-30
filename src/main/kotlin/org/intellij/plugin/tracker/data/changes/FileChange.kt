package org.intellij.plugin.tracker.data.changes

import org.intellij.plugin.tracker.data.diff.FileHistory

enum class ChangeType(change: String) {
    ADDED("ADDED"),
    MOVED("MOVED"),
    MODIFIED("MODIFIED"),
    DELETED("DELETED"),
    INVALID("INVALID")
}

data class FileChange(
    override val changeType: ChangeType,
    override val afterPath: String,
    override val errorMessage: String? = null,
    var fileHistoryList: MutableList<FileHistory> = mutableListOf()
) : Change {

    override fun hasWorkingTreeChanges(): Boolean {
        return try {
            fileHistoryList.last().fromWorkingTree
        } catch (e: NoSuchElementException) {
            false
        }
    }

    override fun toString(): String {
        return "Change type is $changeType and after path is $afterPath with error message $errorMessage"
    }
}
