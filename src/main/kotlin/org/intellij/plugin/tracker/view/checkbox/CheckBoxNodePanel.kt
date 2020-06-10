package org.intellij.plugin.tracker.view.checkbox

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Insets
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * Helper class defining Swing components for check box node renderer UI.
 */
class CheckBoxNodePanel : JPanel() {
    val label = JLabel()
    val check = JCheckBox()

    init {
        check.margin = Insets(0, 0, 0, 0)
        layout = BorderLayout()
        check.background = Color.WHITE
        add(check, BorderLayout.WEST)
        add(label, BorderLayout.CENTER)
    }
}
