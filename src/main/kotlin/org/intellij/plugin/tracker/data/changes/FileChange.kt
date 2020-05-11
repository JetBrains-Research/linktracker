package org.intellij.plugin.tracker.data.changes

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change

data class FileChange(
    val changeType: String = "NONE",
    val fileName: String? = null,
    val beforePath: String? = null,
    val afterPath: String? = null,
    val moveRelativePath: String? = null,
    val errorMessage: String? = null
): LinkChange() {
    private constructor(builder: Builder) : this(
        builder.changeType,
        builder.fileName,
        builder.beforePath,
        builder.afterPath,
        builder.moveRelativePath,
        builder.errorMessage
    )

    companion object {
        fun changeToFileChange(project: Project, change: Change): FileChange {
            val fileChangeBuilder = Builder()
            fileChangeBuilder.changeType = change.type.toString()

            if (change.type.toString() == "MOVED") {
                fileChangeBuilder.moveRelativePath = change.getMoveRelativePath(project)
            }

            if (change.beforeRevision?.file?.path != null) {
                fileChangeBuilder.beforePath = change.beforeRevision?.file?.path
            }

            if (change.afterRevision?.file?.path != null) {
                fileChangeBuilder.afterPath = change.afterRevision?.file?.path
            }

            if (change.afterRevision?.file?.name != null) {
                fileChangeBuilder.fileName = change.afterRevision?.file?.name.toString()
            }
            return fileChangeBuilder.build()
        }
    }

    class Builder {
        var changeType: String = "NONE"
        var fileName: String? = null
        var beforePath: String? = null
        var afterPath: String? = null
        var moveRelativePath: String? = null
        var errorMessage: String? = null

        fun build() = FileChange(this)
    }
}
