package org.intellij.plugin.tracker.data.diff

import org.intellij.plugin.tracker.data.changes.CustomChange

data class DiffOutputMultipleRevisions(
    val fileChange: CustomChange,
    val diffOutputList: MutableList<DiffOutput>,
    val originalLineContent: String = "",
    val originalLinesContents: List<String> = mutableListOf()
)