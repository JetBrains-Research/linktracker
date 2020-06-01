package org.intellij.plugin.tracker.data.diff

import org.intellij.plugin.tracker.data.changes.FileChange

data class DiffOutputMultipleRevisions(
    val fileChange: FileChange,
    val originalLineContent: String,
    val diffOutputList: MutableList<DiffOutput>
)