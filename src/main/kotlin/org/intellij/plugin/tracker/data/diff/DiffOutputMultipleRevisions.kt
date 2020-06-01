package org.intellij.plugin.tracker.data.diff

import org.intellij.plugin.tracker.data.changes.FileChange

data class DiffOutputMultipleRevisions(
    val fileChange: FileChange,
    val diffOutputList: MutableList<DiffOutput>,
    val originalLineContent: String = "",
    val originalLinesContents: List<String> = mutableListOf()
)