package org.intellij.plugin.tracker.data.changes

import com.intellij.openapi.vcs.changes.Change as Change1


data class DirectoryChange(
    var changeType: FileChangeType,
    val afterPathString: String = "",
    override val errorMessage: String? = null,
    val directoryName: String? = null
) : Change {
    override val changes: MutableList<ChangeType>
        get() = mutableListOf(changeType)

    override val requiresUpdate: Boolean
        get() {
            if (changeType == FileChangeType.MOVED || changeType == FileChangeType.DELETED)
                return true
            return false
        }
    override val afterPath: MutableList<String>
        get() = mutableListOf(afterPathString)

    override fun hasWorkingTreeChanges(): Boolean {
        // for now return false
        return false
    }

    private constructor(builder: Builder) : this(
        builder.changeType,
        builder.afterPath,
        builder.errorMessage,
        builder.directoryName
    )

    companion object {
        fun changeToDirectoryChange(change: Change1): DirectoryChange {
            val directoryChangeBuilder = Builder()

            val changeType: FileChangeType = when (change.type) {
                Change1.Type.MOVED -> FileChangeType.MOVED
                Change1.Type.MODIFICATION -> FileChangeType.MODIFIED
                Change1.Type.NEW -> FileChangeType.ADDED
                Change1.Type.DELETED -> FileChangeType.DELETED
            }

            directoryChangeBuilder.changeType = changeType

            if (change.afterRevision?.file?.path != null) {
                directoryChangeBuilder.afterPath = change.afterRevision!!.file.parentPath.toString()
            }

            if (change.afterRevision?.file?.name != null) {
                directoryChangeBuilder.directoryName = change.afterRevision!!.file.parentPath!!.name
            }
            return directoryChangeBuilder.build()
        }
    }

    class Builder {
        var changeType: FileChangeType = FileChangeType.INVALID
        var afterPath: String = ""
        var directoryName: String? = null
        var errorMessage: String? = null

        fun build() = DirectoryChange(this)
    }
}
