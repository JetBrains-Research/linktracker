package org.intellij.plugin.tracker.view.checkbox

/**
 * Data class for the check box
 */
class CheckBoxNodeData(var text: String, var isChecked: Boolean) {
    override fun toString(): String {
        return text
    }
}
