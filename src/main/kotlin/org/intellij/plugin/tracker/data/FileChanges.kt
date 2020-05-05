package org.intellij.plugin.tracker.data


data class FileChange(
    val changeType: String,
    val fileName: String?,
    val beforePath: String?,
    val afterPath: String?,
    val moveRelativePath: String?
) {
    private constructor(builder: Builder) : this(
        builder.changeType,
        builder.fileName,
        builder.beforePath,
        builder.afterPath,
        builder.moveRelativePath
    )

    class Builder {
        var changeType: String = "NONE"
        var fileName: String? = null
        var beforePath: String? = null
        var afterPath: String? = null
        var moveRelativePath: String? = null

        fun build() = FileChange(this)
    }
}