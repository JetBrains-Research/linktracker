package org.intellij.plugin.tracker.view

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.castSafelyTo
import org.intellij.plugin.tracker.utils.CredentialsManager
import java.awt.GridLayout
import javax.swing.*
import javax.swing.JPanel




class PasswordManagingDialog internal constructor(
    canBeParent: Boolean,
    private val userInfoList: List<Pair<String, List<Pair<String, String>>>>
) : DialogWrapper(canBeParent) {
    private val panel: JPanel = JPanel()


    public override fun createCenterPanel(): JComponent? {
        return panel
    }

    override fun doOKAction() {
        if (okAction.isEnabled) {
            for (userInfo in userInfoList) {
                val textField: JTextField? = panel.getClientProperty(userInfo.first).castSafelyTo<JTextField>()
                if (textField != null) {
                    val password: String = textField.text
                    if (!password.isEmpty()) CredentialsManager.storeCredentials(userInfo.first, password)
                }
            }
            close(OK_EXIT_CODE)
        }
    }

    init {
        init()
        var password: JTextField

        for (userInfo in userInfoList) {
            panel.add(JLabel("Enter the password for ${userInfo.first}:"))
            panel.layout = GridLayout(0, 1)
            password = JTextField(20)
            panel.add(password)
            panel.putClientProperty(userInfo.first, password)
            val infoList = userInfo.second
            for (pair in infoList) {
                val displayText = "Needed for accessing link ${pair.second} to project name: ${pair.first}"
                panel.add(JLabel(displayText))
            }
        }
    }
}