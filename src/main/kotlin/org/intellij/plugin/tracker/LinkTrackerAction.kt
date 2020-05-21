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
import com.intellij.openapi.vcs.VcsException
import org.intellij.plugin.tracker.data.changes.ChangeType
import org.intellij.plugin.tracker.data.changes.LinkChange
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.data.links.LinkInfo
import org.intellij.plugin.tracker.data.links.NotSupportedLink
import org.intellij.plugin.tracker.services.*
import org.intellij.plugin.tracker.utils.GitOperationManager
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

        // initialize all services
        val historyService: HistoryService = HistoryService.getInstance(currentProject)
        val linkService: LinkRetrieverService = LinkRetrieverService.getInstance(currentProject)
        val linkUpdateService: LinkUpdaterService = LinkUpdaterService.getInstance(currentProject)
        val uiService: UIService = UIService.getInstance(currentProject)

        // initialize lists
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
                    indicator.text = "Tracking link with path ${linkInfo.linkPath}.."
                    val link: Link = LinkFactory.createLink(linkInfo, historyService.stateObject.commitSHA)

                    println("LINK IS: $link")

                    if (link is NotSupportedLink) {
                        continue
                    }

                    try {
                        linksAndChangesList.add(LinkProcessingRouter.getChangesForLink(link = link))
                    // temporary solution to ignoring not implemented stuff
                    } catch (e: NotImplementedError) {
                        continue
                    // catch any errors that might result from using vcs commands (git).
                    } catch(e: VcsException) {
                        linksAndChangesList.add(
                            Pair(link, LinkChange(ChangeType.INVALID, errorMessage = e.message, afterPath = link.linkInfo.linkPath)))
                    // horrible generic exception catch: just in case.
                    } catch(e: Exception) {
                        linksAndChangesList.add(
                            Pair(link, LinkChange(ChangeType.INVALID, errorMessage = e.message, afterPath = link.linkInfo.linkPath)))
                    }
                    // TODO: for each link and change pair, pass it to the core to get final results before showing in the UI.
                }
                // Debug
                println("Link tracking finished!")
                running = false

                historyService.saveCommitSHA(GitOperationManager(currentProject).getHeadCommitSHA())
            }
        })

        // Run linkUpdater thread
        // There should be a better way to wait for the Tracking Links task to finish
        ApplicationManager.getApplication().invokeLater {
            WriteCommandAction.runWriteCommandAction(currentProject) {
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
            }
        }

        uiService.updateView(currentProject, linksAndChangesList)
    }
}
