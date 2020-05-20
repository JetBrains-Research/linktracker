package org.intellij.plugin.tracker.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.data.links.LinkInfo
import org.intellij.plugin.tracker.data.links.WebLink
import org.intellij.plugin.tracker.services.LinkRetrieverService
import org.intellij.plugin.tracker.utils.LinkFactory
import org.intellij.plugin.tracker.view.PasswordManagingDialog


class PasswordManagingAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val currentProject = event.getData(PlatformDataKeys.PROJECT)

        if (currentProject == null) {
            Messages.showErrorDialog(
                "Please open a project to run the link tracking plugin.",
                "Link Tracker"
            )
            return
        }

        val linkService: LinkRetrieverService = LinkRetrieverService.getInstance(currentProject)
        val linkInfoList: MutableList<LinkInfo> = mutableListOf()

        ApplicationManager.getApplication().runReadAction {
            linkService.getLinks(linkInfoList)
        }

        val userInfoList: MutableList<Triple<String, String, String>> = mutableListOf()

        ProgressManager.getInstance().run(object : Task.Modal(currentProject, "Retrieving info on links..", true) {
            override fun run(indicator: ProgressIndicator) {
                for (linkInfo: LinkInfo in linkInfoList) {
                    val link: Link = LinkFactory.createLink(linkInfo)
                    if (link is WebLink && !link.correspondsToLocalProject()) {
                        userInfoList.add(Triple(link.getProjectOwnerName(), link.getProjectName(), link.linkInfo.linkPath))
                    }
                }
            }})

        val result: List<Pair<String, List<Pair<String, String>>>> = userInfoList.groupBy { it.first }
            .map { (e1, e2) -> Pair(e1, e2.map { element -> Pair(element.second, element.third) }) }

        val dialog = PasswordManagingDialog(true, result);

        dialog.title = "Manage Passwords"
        dialog.createCenterPanel()
        dialog.show()
    }
}

