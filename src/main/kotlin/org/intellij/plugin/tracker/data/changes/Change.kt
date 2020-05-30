package org.intellij.plugin.tracker.data.changes

interface Change {
    val changeType: ChangeType
    val afterPath: String
    val errorMessage: String?

    fun hasWorkingTreeChanges(): Boolean
}