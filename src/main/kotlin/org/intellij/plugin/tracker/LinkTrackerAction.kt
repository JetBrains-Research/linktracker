package org.intellij.plugin.tracker

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.ui.Messages
import org.intellij.plugin.tracker.data.Link
import org.intellij.plugin.tracker.data.LinkType
import org.intellij.plugin.tracker.data.RelativeLink
import org.intellij.plugin.tracker.services.ChangeTrackerService
import org.intellij.plugin.tracker.services.LinkRetrieverService

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

        // linkList is hardcoded for now. Should be retrieved from project files. Also commitSHA (should be retrieved from disk, if possible)
        // TODO: Implement logic for linkPath to have both relative paths + absolute paths

        val changeTrackerService = ChangeTrackerService.getInstance(currentProject)
        val linkService= LinkRetrieverService.getInstance(currentProject)
        val links = linkService.getLinks()

        // TODO: Commit SHA needs to be given to following method to retrieve changes
        val fileChanges = changeTrackerService.getFileChanges(links as MutableList<Link>)
        val statistics = mutableListOf<Any>(linkService.noOfFiles, linkService.noOfLinks, linkService.noOfFilesWithLinks)
        changeTrackerService.updateView(currentProject, fileChanges)
        changeTrackerService.updateStatistics(currentProject, statistics)
        // println(linkTrackerAction.getFileChanges(mutableListOf(Link(linkType = LinkType.FILE, linkText = "", linkPath = "C:/Users/Tudor/Desktop/plugins/markdown-plugin/README.md", provinencePath = "README.md")), commitSHA = "2b474"))
    }
}
