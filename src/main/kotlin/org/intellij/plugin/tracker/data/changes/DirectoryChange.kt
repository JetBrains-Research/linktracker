package org.intellij.plugin.tracker.data.changes

import com.intellij.openapi.vcs.changes.Change as Change1


data class DirectoryChange(
    override var changeType: ChangeType,
    override val afterPath: String = "",
    override val errorMessage: String? = null,
    val directoryName: String? = null
) : Change {
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

            val changeType: ChangeType = when (change.type) {
                Change1.Type.MOVED -> ChangeType.MOVED
                Change1.Type.MODIFICATION -> ChangeType.MODIFIED
                Change1.Type.NEW -> ChangeType.ADDED
                Change1.Type.DELETED -> ChangeType.DELETED
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
        var changeType: ChangeType = ChangeType.INVALID
        var afterPath: String = ""
        var directoryName: String? = null
        var errorMessage: String? = null

        fun build() = DirectoryChange(this)
    }
}
