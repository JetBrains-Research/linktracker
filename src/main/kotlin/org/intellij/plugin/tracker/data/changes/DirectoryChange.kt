package org.intellij.plugin.tracker.data.changes

enum class DirectoryChangeType(val change: String): ChangeType {
    ADDED("DIRECTORY ADDED") {
        override val changeTypeString: String
            get() = change
    },
    MOVED("DIRECTORY MOVED") {
        override val changeTypeString: String
            get() = change
    },
    MODIFIED("DIRECTORY MODIFIED") {
        override val changeTypeString: String
            get() = change
    },
    DELETED("DIRECTORY DELETED") {
        override val changeTypeString: String
            get() = change
    },
    INVALID("DIRECTORY INVALID") {
        override val changeTypeString: String
            get() = change
    }
}

data class DirectoryChange(
        val directoryChangeType: DirectoryChangeType,
        val afterPathString: String,
        override val errorMessage: String? = null
) : Change {
    override val afterPath: MutableList<String>
        get() = mutableListOf(afterPathString)

    override val changes: MutableList<ChangeType>
        get() = mutableListOf(directoryChangeType)

    override val requiresUpdate: Boolean
        get() {
            if (directoryChangeType == DirectoryChangeType.MOVED || directoryChangeType == DirectoryChangeType.DELETED)
                return true
            return false
        }

    override fun hasWorkingTreeChanges(): Boolean {
        // returns false for now
        return false
    }

    override fun toString(): String {
        return "Change type is $directoryChangeType and after path is $afterPath with error message $errorMessage"
    }
}
