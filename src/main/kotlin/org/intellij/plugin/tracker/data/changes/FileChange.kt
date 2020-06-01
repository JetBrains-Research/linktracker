package org.intellij.plugin.tracker.data.changes

import org.intellij.plugin.tracker.data.diff.FileHistory

enum class FileChangeType(val change: String): ChangeType {
    ADDED("FILE ADDED") {
        override val changeTypeString: String
            get() = change
    },
    MOVED("FILE MOVED") {
        override val changeTypeString: String
            get() = change
    },
    MODIFIED("FILE MODIFIED") {
        override val changeTypeString: String
            get() = change
    },
    DELETED("FILE DELETED") {
        override val changeTypeString: String
            get() = change
    },
    INVALID("FILE INVALID") {
        override val changeTypeString: String
            get() = change
    }
}

data class FileChange(
    val fileChangeType: FileChangeType,
    override val afterPath: String,
    override val errorMessage: String? = null,
    var fileHistoryList: MutableList<FileHistory> = mutableListOf(),
    var deletionsAndAdditions: Int = 0
) : Change {
    override val changes: MutableList<ChangeType>
        get() {
            return mutableListOf(fileChangeType)
        }

    override val requiresUpdate: Boolean
        get() {
            if (fileChangeType == FileChangeType.MOVED || fileChangeType == FileChangeType.DELETED)
                return true
            return false
        }

    override fun hasWorkingTreeChanges(): Boolean {
        return try {
            fileHistoryList.last().fromWorkingTree
        } catch (e: NoSuchElementException) {
            false
        }
    }

    override fun toString(): String {
        return "Change type is $fileChangeType and after path is $afterPath with error message $errorMessage"
    }
}
