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
) {
    override fun toString(): String {
        return "Change type is $changeType and after path is $afterPath with error message $errorMessage"
    }
}
