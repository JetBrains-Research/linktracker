package org.intellij.plugin.tracker.data.changes


interface ChangeType {
    val changeTypeString: String
}


interface Change {
    val requiresUpdate: Boolean
    val afterPath: MutableList<String>
    val errorMessage: String?
    val changes: MutableList<ChangeType>

    fun hasWorkingTreeChanges(): Boolean
}