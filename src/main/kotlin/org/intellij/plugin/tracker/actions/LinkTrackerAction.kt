package org.intellij.plugin.tracker.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import org.intellij.plugin.tracker.services.*
import org.intellij.plugin.tracker.settings.FeatureSwitchSettings
import org.intellij.plugin.tracker.core.DataParsingTask
import org.intellij.plugin.tracker.core.change.GitOperationManager

/**
 * Main action of the plugin.
 */
class LinkTrackerAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val currentProject: Project? = event.getData(PlatformDataKeys.PROJECT)

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
            val gitOperationManager =
                GitOperationManager(project)

            // Initialize task
            val dataParsingTask = DataParsingTask(
                currentProject = project,
                myLinkService = linkService,
                myHistoryService = historyService,
                myGitOperationManager = gitOperationManager,
                myLinkUpdateService = linkUpdateService,
                myChangeTrackerService = ChangeTrackerServiceImpl(
                    project,
                    FeatureSwitchSettings.getCorrespondentChangeTrackingPolicy()
                ),
                myUiService = uiService,
                dryRun = true
            )

            FileDocumentManager.getInstance().saveAllDocuments()
            // Run task
            ProgressManager.getInstance().run(dataParsingTask)
        }
    }
}