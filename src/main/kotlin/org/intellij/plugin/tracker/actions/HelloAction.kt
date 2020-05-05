package org.intellij.plugin.tracker.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import org.intellij.plugin.tracker.services.LinkRetrieverService

class HelloAction : DumbAwareAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.getData(PlatformDataKeys.PROJECT)
        val service = LinkRetrieverService(project)
        val message = service.getLinks()
        Messages.showMessageDialog(project, message.toString(), "Greeting", Messages.getInformationIcon())
    }
}
