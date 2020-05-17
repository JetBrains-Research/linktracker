package org.intellij.plugin.tracker.data.changes

import com.intellij.openapi.vcs.changes.Change


data class DirectoryChange(
        override var changeType: ChangeType,
        override val afterPath: String,
        override val errorMessage: String? = null,
        val directoryName: String? = null
): LinkChange(changeType, afterPath, errorMessage) {
    private constructor(builder:Builder):this(
        builder.changeType,
        builder.afterPath,
        builder.errorMessage,
        builder.directoryName
        )

    companion object {
        fun changeToDirectoryChange(change:Change): DirectoryChange {
            val directoryChangeBuilder = Builder()

            val changeType: ChangeType = when (change.type) {
                Change.Type.MOVED -> ChangeType.MOVED
                Change.Type.MODIFICATION -> ChangeType.MODIFIED
                Change.Type.NEW -> ChangeType.ADDED
                Change.Type.DELETED -> ChangeType.DELETED
            }

            directoryChangeBuilder.changeType = changeType

            if(change.afterRevision?.file?.path != null){
                directoryChangeBuilder.afterPath = change.afterRevision?.file?.parentPath.toString()
            }

            if(change.afterRevision?.file?.name != null){
                directoryChangeBuilder.directoryName = change.afterRevision?.file?.parentPath?.name
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
