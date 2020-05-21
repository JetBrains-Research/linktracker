package org.intellij.plugin.tracker.view

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import org.intellij.plugin.tracker.utils.CredentialsManager
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridLayout
import java.awt.event.KeyEvent
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.ScrollPaneConstants


class PasswordManagingDialog internal constructor(
    canBeParent: Boolean,
    private val userInfoList: List<Pair<String, List<Pair<String, String>>>>
) : DialogWrapper(canBeParent) {
    private val tabbedPane = JBTabbedPane()

    public override fun createCenterPanel(): JComponent? {
        return tabbedPane
    }

    override fun doOKAction() {
        if (okAction.isEnabled) {
            val selectedIndex = tabbedPane.selectedIndex
            val currentUser = userInfoList[selectedIndex]
            val textField: JTextField? = tabbedPane.getClientProperty(currentUser.first) as JTextField?
            if (textField == null || textField.text.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Please enter a password")
            } else {
                CredentialsManager.storeCredentials(currentUser.first, textField.text)
                val options = arrayOf("Continue", "Exit")
                val response = JOptionPane.showOptionDialog(
                    null, "Password is saved. Do you want to continue managing passwords?", "Password Manager",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                    null, options, options[0]
                )
                if (response == 1) {
                    close(OK_EXIT_CODE)
                }
            }
        }
    }

    init {
        init()
        var password: JTextField
        for (i in userInfoList.indices) {
            val userInfo = userInfoList[i]
            val panel = JPanel()
            val height = JPanel(BorderLayout())
            height.add(JLabel("Enter the password for ${userInfo.first}: "), BorderLayout.LINE_START)
            password = JTextField()
            password.preferredSize = Dimension(100, 20)
            height.add(password, BorderLayout.LINE_END)
            panel.add(height, BorderLayout.CENTER)
            panel.layout = GridLayout(0, 1)
            tabbedPane.putClientProperty(userInfo.first, password)
            val infoList = userInfo.second
            var text = ""
            for (pair in infoList) {
                text += " Needed for accessing link ${pair.second} to project name: ${pair.first} \n"
            }
            panel.preferredSize = Dimension(350, 100)
            panel.maximumSize = panel.preferredSize
            panel.minimumSize = panel.preferredSize
            val display = JTextArea(100, 20)
            display.text = text
            display.isEditable = false
            val scroll = JBScrollPane(display)
            scroll.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
            panel.add(scroll)

            tabbedPane.addTab(
                "User ${i+1}", null, panel,
                "${userInfo.first}'s password"
            )
            tabbedPane.setMnemonicAt(i, KeyEvent.VK_1)
        }
    }
}