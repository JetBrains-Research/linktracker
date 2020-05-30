package org.intellij.plugin.tracker.data.changes

data class LinesChange(
    val changeType: ChangeType,
    override val afterPath: String,
    override val errorMessage: String?
): Change {
    override val changes: MutableList<ChangeType>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val requiresUpdate: Boolean
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun hasWorkingTreeChanges(): Boolean {
        TODO("not implemented")
    }
}
