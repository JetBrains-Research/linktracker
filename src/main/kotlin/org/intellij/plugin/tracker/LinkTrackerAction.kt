package org.intellij.plugin.tracker

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import org.intellij.plugin.tracker.data.changes.LinkChange
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.data.links.LinkInfo
import org.intellij.plugin.tracker.data.links.NotSupportedLink
import org.intellij.plugin.tracker.services.ChangeTrackerService
import org.intellij.plugin.tracker.services.LinkRetrieverService
import org.intellij.plugin.tracker.services.LinkUpdaterService
import org.intellij.plugin.tracker.services.UIService
import org.intellij.plugin.tracker.utils.LinkFactory
import org.intellij.plugin.tracker.utils.LinkProcessingRouter


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
        val linkUpdateService = LinkUpdaterService.getInstance(currentProject)
        val uiService = UIService.getInstance(currentProject)

        val linksAndChangesList: MutableList<Pair<Link, LinkChange>> = mutableListOf()
        val linkInfoList: MutableList<LinkInfo> = mutableListOf()

        ApplicationManager.getApplication().runReadAction {
            linkService.getLinks(linkInfoList)
        }

        var running = true
        ProgressManager.getInstance().run(object : Task.Modal(currentProject, "Tracking links..", true) {
            override fun run(indicator: ProgressIndicator) {
                running = true
                for (linkInfo in linkInfoList) {
                    // TODO: Get commit SHA from disk if possible
                    val commitSHA = null
                    val link = LinkFactory.createLink(
                        linkInfo,
                        commitSHA,
                        currentProject,
                        ChangeTrackerService.getInstance(project).cachedChanges
                    )

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
                // Debug
                println("Link tracking finished!")
                running = false
            }
        })

        // TODO: Commit SHA needs to be given to following method to retrieve changes
        val statistics =
                mutableListOf<Any>(linkService.noOfFiles, linkService.noOfLinks, linkService.noOfFilesWithLinks)

        // Run linkUpdater thread
        // There should be a better way to wait for the Tracking Links task to finish
        ApplicationManager.getApplication().invokeLater {
            WriteCommandAction.runWriteCommandAction(currentProject, Runnable {
                while (running) {
                    Thread.sleep(100L)
                }
                // Debug
                println("Finished waiting")
                if (linksAndChangesList.size != 0) {
                    // Debug
                    println("All changes: ")
                    // Debug
                    linksAndChangesList.map { pair -> println(pair) }
                    val result = linkUpdateService.updateLinks(linksAndChangesList)
                    // Debug
                    println("Update result: $result")
                } else {
                    // Debug
                    println("No links to update...")
                }
            })
        }

        uiService.updateView(currentProject, linksAndChangesList)
        uiService.updateStatistics(currentProject, statistics)
    }
}
