package org.intellij.plugin.tracker.view

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTabbedPane
import org.intellij.plugin.tracker.utils.CredentialsManager
import java.awt.GridLayout
import java.awt.event.KeyEvent
import javax.swing.*


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
            panel.add(JLabel("Enter the password for ${userInfo.first}:"))
            panel.layout = GridLayout(0, 1)
            password = JTextField(20)
            panel.add(password)
            tabbedPane.putClientProperty(userInfo.first, password)
            val infoList = userInfo.second
            for (pair in infoList) {
                panel.add(JLabel("Needed for accessing link ${pair.second} to project name: ${pair.first}\n"))
            }
            tabbedPane.addTab(
                "User ${i+1}", null, panel,
                "${userInfo.first}'s password"
            )
            tabbedPane.setMnemonicAt(i, KeyEvent.VK_1)
        }
    }
}