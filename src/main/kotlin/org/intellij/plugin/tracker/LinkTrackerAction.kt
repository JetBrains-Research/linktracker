package org.intellij.plugin.tracker

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import org.intellij.plugin.tracker.data.changes.LinkChange
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.data.links.LinkInfo
import org.intellij.plugin.tracker.data.links.NotSupportedLink
import org.intellij.plugin.tracker.utils.LinkFactory
import org.intellij.plugin.tracker.utils.LinkProcessingRouter
import org.intellij.plugin.tracker.services.LinkRetrieverService
import org.intellij.plugin.tracker.services.UIService


class LinkTrackerAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val currentProject = event.getData(PlatformDataKeys.PROJECT)

        if (currentProject == null) {
            Messages.showErrorDialog(
                "Please open a project to run the link tracking plugin.",
                "Link Tracker"
            )
            return
        }

        val linkService = LinkRetrieverService.getInstance(currentProject)
        val uiService = UIService.getInstance(currentProject)

        val linksAndChangesList = mutableListOf<Pair<Link, LinkChange>>()
        var linkInfoList: MutableList<LinkInfo> = mutableListOf()

        ApplicationManager.getApplication().runReadAction {
            linkInfoList = linkService.getLinks()
        }

        ProgressManager.getInstance().run(object : Task.Modal(currentProject, "Tracking links..", true) {
                override fun run(indicator: ProgressIndicator) {
                    for (linkInfo in linkInfoList) {
                        // TODO: Get commit SHA from disk if possible
                        val commitSHA = null
                        val link = LinkFactory.createLink(linkInfo, commitSHA, currentProject)

                        if (link is NotSupportedLink) {
                            continue
                        }
                        indicator.text = "Tracking link with path ${link.linkInfo.linkPath}.."
                        try {
                            linksAndChangesList.add(
                                LinkProcessingRouter.getChangesForLink(
                                    link = link,
                                    project = currentProject
                                )
                            )
                        } catch (e: NotImplementedError) {
                            continue
                        }
                        // TODO: for each link and change pair, pass it to the core to get final results before showing in the UI.
                    }
                }})

        val statistics =
            mutableListOf<Any>(linkService.noOfFiles, linkService.noOfLinks, linkService.noOfFilesWithLinks)

        uiService.updateView(currentProject, linksAndChangesList)
        uiService.updateStatistics(currentProject, statistics)
    }
}
