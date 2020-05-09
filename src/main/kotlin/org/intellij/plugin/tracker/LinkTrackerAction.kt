package org.intellij.plugin.tracker

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import org.intellij.plugin.tracker.data.changes.LinkChange
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.data.links.NotSupportedLink
import org.intellij.plugin.tracker.data.links.PotentialLink
import org.intellij.plugin.tracker.utils.LinkFactory
import org.intellij.plugin.tracker.utils.LinkProcessingRouter
import org.intellij.plugin.tracker.services.LinkRetrieverService
import org.intellij.plugin.tracker.services.UIService
import java.text.DecimalFormat


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
        var potentialLinkList: MutableList<PotentialLink> = mutableListOf()

        val start = System.currentTimeMillis()
        val start3 = System.currentTimeMillis()

        ApplicationManager.getApplication().runReadAction {
            potentialLinkList = linkService.getLinks()
        }

        val formatter = DecimalFormat("#0.00000")
        var end = System.currentTimeMillis()
        // println("Link retrieving Execution time is " + formatter.format((end - start3) / 1000.0) + " seconds")

        val start2 = System.currentTimeMillis()
        ProgressManager.getInstance().run(object : Task.Modal(currentProject, "Tracking links..", true) {
                override fun run(indicator: ProgressIndicator) {
                    for (potentialLink in potentialLinkList) {
                        val commitSHA = null
                        val link = LinkFactory.createLink(potentialLink, commitSHA, currentProject)

                        //println("$link")


                        if (link is NotSupportedLink) {
                            // println(link)
                            continue
                        }

                        indicator.text = "Tracking link with path ${link.linkPath}.."

                        try {
                            linksAndChangesList.add(
                                LinkProcessingRouter.getChangesForLink(
                                    link = link,
                                    project = currentProject
                                )
                            )
                        } catch (e: NotImplementedError) {
                            //println(e.message)
                            continue
                        }
                    }
                }})

        end = System.currentTimeMillis()
        // println("Git Execution time is " + formatter.format((end - start2) / 1000.0) + " seconds")

        end = System.currentTimeMillis()
        println("Total Execution time is " + formatter.format((end - start) / 1000.0) + " seconds")

        // TODO: for each link and change pair, pass it to the core to get final results before showing in the UI.


        val statistics =
            mutableListOf<Any>(linkService.noOfFiles, linkService.noOfLinks, linkService.noOfFilesWithLinks)

        uiService.updateView(currentProject, linksAndChangesList)
        uiService.updateStatistics(currentProject, statistics)
    }
}
