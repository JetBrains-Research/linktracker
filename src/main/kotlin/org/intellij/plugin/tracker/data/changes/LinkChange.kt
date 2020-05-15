package org.intellij.plugin.tracker.data.changes

abstract class LinkChange(
    open var changeType: String = "NONE",
    open val fileName: String? = null,
    open val beforePath: String? = null,
    open val afterPath: String? = null
)