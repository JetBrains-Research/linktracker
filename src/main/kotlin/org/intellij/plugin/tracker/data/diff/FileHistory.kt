package org.intellij.plugin.tracker.data.diff

data class FileHistory(
    private val rev: String = "",
    val path: String,
    val fromWorkingTree: Boolean = false
) {
    val revision: String = rev
        get() {
            return if (field.isBlank()) field
            else field.split("Commit: ")[1]
        }
}
