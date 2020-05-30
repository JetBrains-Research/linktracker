package org.intellij.plugin.tracker.data.changes

data class LinesChange(
    override val changeType: ChangeType,
    override val afterPath: String,
    override val errorMessage: String?
): Change {
    override fun hasWorkingTreeChanges(): Boolean {
        TODO("not implemented")
    }
}
