package org.intellij.plugin.tracker.data.changes

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change

data class DirectoryChange(
        override var changeType: String = "NONE",
        val directoryName: String? = null,
        override val beforePath: String? = null,
        override val afterPath: String? = null,
        val moveRelativePath: String? = null,
        val errorMessage: String? = null
): LinkChange() {
    private constructor(builder:Builder):this(
            builder.changeType,
            builder.directoryName,
            builder.beforePath,
            builder.afterPath,
            builder.moveRelativePath,
            builder.errorMessage
    )

    companion object {
        fun changeToDirectoryChange(project:Project,change:Change): DirectoryChange {
            val directoryChangeBuilder = Builder()
            directoryChangeBuilder.changeType=change.type.toString()

            if(change.type.toString() == "MOVED"){
                directoryChangeBuilder.moveRelativePath=change.getMoveRelativePath(project)
            }

            if(change.beforeRevision?.file?.parentPath != null){
                directoryChangeBuilder.beforePath=change.beforeRevision?.file?.parentPath.toString()
            }

            if(change.afterRevision?.file?.path != null){
                directoryChangeBuilder.afterPath=change.afterRevision?.file?.parentPath.toString()
            }

            if(change.afterRevision?.file?.name != null){
                directoryChangeBuilder.directoryName= change.afterRevision?.file?.parentPath?.name
            }
            return directoryChangeBuilder.build()
        }
    }

    class Builder {
        var changeType:String ="NONE"
        var directoryName:String?=null
        var beforePath:String?=null
        var afterPath:String?=null
        var moveRelativePath:String?=null
        var errorMessage:String?=null

        fun build() = DirectoryChange(this)
    }
}
