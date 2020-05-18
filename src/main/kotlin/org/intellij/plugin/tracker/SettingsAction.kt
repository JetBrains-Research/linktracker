package org.intellij.plugin.tracker

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.intellij.plugin.tracker.view.CustomDialog

class SettingsAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val dialog = CustomDialog(true);
        dialog.title = "Settings"
        dialog.createCenterPanel()
        dialog.show()
    }
}

