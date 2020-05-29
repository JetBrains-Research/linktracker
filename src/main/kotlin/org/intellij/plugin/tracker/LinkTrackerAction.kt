package org.intellij.plugin.tracker

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import org.intellij.plugin.tracker.services.HistoryService
import org.intellij.plugin.tracker.services.LinkRetrieverService
import org.intellij.plugin.tracker.services.LinkUpdaterService
import org.intellij.plugin.tracker.services.UIService
import org.intellij.plugin.tracker.utils.DataParsingTask
import org.intellij.plugin.tracker.utils.GitOperationManager

/**
 * Main action of the plugin.
 */
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

        run(currentProject)
    }

    companion object {

        /**
         * Runs the plugin logic.
         * Static execution is needed to be able to trigger scanning from other classes.
         */
        fun run(project: Project) {

            // Initialize all services
            val historyService: HistoryService = HistoryService.getInstance(project)
            val linkService: LinkRetrieverService = LinkRetrieverService.getInstance(project)
            val linkUpdateService: LinkUpdaterService = LinkUpdaterService.getInstance(project)
            val uiService: UIService = UIService.getInstance(project)
            val gitOperationManager = GitOperationManager(project)

            // Initialize task
            val dataParsingTask = DataParsingTask(
                currentProject = project,
                myLinkService = linkService,
                myHistoryService = historyService,
                myGitOperationManager = gitOperationManager,
                myLinkUpdateService = linkUpdateService,
                myUiService = uiService,
                dryRun = true
            )

            // Run task
            ProgressManager.getInstance().run(dataParsingTask)
        }
    }
}
