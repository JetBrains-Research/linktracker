package org.intellij.plugin.tracker.view

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.ui.DialogWrapper
import java.awt.GridLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JTextField

class CustomDialog internal constructor(canBeParent: Boolean) : DialogWrapper(canBeParent) {

    private val panel: JPanel = JPanel()
    private val threshold: JTextField
    private val thresholdText: String get() = threshold.text
    private val prop = PropertiesComponent.getInstance()

    public override fun createCenterPanel(): JComponent? {
        return panel
    }

    override fun doOKAction() {
        if (okAction.isEnabled) {
            try {
                val input = thresholdText.toInt()
                if (input in 101 downTo 29) {
                    prop.setValue("threshold", input.toString())
                    close(OK_EXIT_CODE)
                } else {
                    JOptionPane.showMessageDialog(null, "Please enter a number between 30-100")
                }

            } catch (e: Exception) {
                JOptionPane.showMessageDialog(null, "Please enter a valid number")
            }
        }
    }

    init {
        init()
        panel.add(JLabel("Enter your threshold value:"))
        panel.layout = GridLayout(0, 1)
        threshold = JTextField(20)
        panel.add(threshold)
        panel.add(JLabel("Current threshold: ${prop.getValue("threshold")}"))
    }
}