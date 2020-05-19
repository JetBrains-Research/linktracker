package org.intellij.plugin.tracker.view

import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.*

class CustomPanel : JPanel(), ActionListener {
    private val panel: JPanel = JPanel()
    private val threshold: JTextField

    private val thresholdText: String get() = threshold.text

    override fun actionPerformed(ae: ActionEvent) {
        throw UnsupportedOperationException("Not supported yet.")
    }

    init {
        panel.add(JLabel("Enter your threshold value:"))
        threshold = JTextField(20)
        panel.add(threshold)
        add(panel)
        val b2 = JButton("Save")
        add(b2, BorderLayout.SOUTH)
        b2.actionCommand = "save"
        b2.addActionListener { e ->
            if ("save" == e.actionCommand) {

                //!! call public method on ButtonFrame object
                JOptionPane.showMessageDialog(null, thresholdText)
            }
        }
    }
}