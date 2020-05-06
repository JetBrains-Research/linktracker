package org.intellij.plugin.tracker

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.ui.Messages
import org.intellij.plugin.tracker.data.LinkType
import org.intellij.plugin.tracker.data.RelativeLink
import org.intellij.plugin.tracker.services.ChangeTrackerService

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

        val linkTrackerAction = ChangeTrackerService.getInstance(currentProject)

        // linkList is hardcoded for now. Should be retrieved from project files. Also commitSHA (should be retrieved from disk, if possible)
        // TODO: Implement logic for linkPath to have both relative paths + absolute paths
        val fileChanges = linkTrackerAction.getFileChanges(
            mutableListOf(
                RelativeLink(
                    linkType = LinkType.FILE,
                    linkText = "inline link",
                    linkPath = "build.gradle",
                    proveniencePath = "README.md",
                    foundAtLineNumber = 52
                )
            )
        )
        println(fileChanges)
        linkTrackerAction.updateView(currentProject, fileChanges)
        // println(linkTrackerAction.getFileChanges(mutableListOf(Link(linkType = LinkType.FILE, linkText = "", linkPath = "C:/Users/Tudor/Desktop/plugins/markdown-plugin/README.md", provinencePath = "README.md")), commitSHA = "2b474"))
    }
}
