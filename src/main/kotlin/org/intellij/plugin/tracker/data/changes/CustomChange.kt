package org.intellij.plugin.tracker.data.changes

import org.intellij.plugin.tracker.data.diff.FileHistory

enum class CustomChangeType(val change: String) : ChangeType {
    ADDED("ADDED") {
        override val changeTypeString: String
            get() = change
    },
    MOVED("MOVED") {
        override val changeTypeString: String
            get() = change
    },
    MODIFIED("MODIFIED") {
        override val changeTypeString: String
            get() = change
    },
    DELETED("DELETED") {
        override val changeTypeString: String
            get() = change
    },
    INVALID("INVALID") {
        override val changeTypeString: String
            get() = change
    }
}

data class CustomChange(
    val customChangeType: CustomChangeType,
    val afterPathString: String,
    override val errorMessage: String? = null,
    var fileHistoryList: MutableList<FileHistory> = mutableListOf(),
    var deletionsAndAdditions: Int = 0
) : Change {
    override val afterPath: MutableList<String>
        get() = mutableListOf(afterPathString)

    override val changes: MutableList<ChangeType>
        get() = mutableListOf(customChangeType)

    override val requiresUpdate: Boolean
        get() {
            if (customChangeType == CustomChangeType.MOVED || customChangeType == CustomChangeType.DELETED)
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
        return "Change type is $customChangeType and after path is $afterPath with error message $errorMessage"
    }
}
