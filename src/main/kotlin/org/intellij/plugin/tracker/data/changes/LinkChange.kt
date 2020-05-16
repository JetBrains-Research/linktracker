package org.intellij.plugin.tracker.data.changes

enum class ChangeType(change: String) {
    ADDED("ADDED"),
    MOVED("MOVED"),
    MODIFIED("MODIFIED"),
    DELETED("DELETED"),
    INVALID("INVALID")
}

open class LinkChange(
    open val changeType: ChangeType,
    open val afterPath: String,
    open val errorMessage: String? = null
)
