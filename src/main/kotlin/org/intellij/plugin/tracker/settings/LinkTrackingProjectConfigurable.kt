package org.intellij.plugin.tracker.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import jdk.jfr.Description
import jdk.jfr.Label
import org.intellij.plugin.tracker.data.UserInfo
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.data.links.LinkInfo
import org.intellij.plugin.tracker.data.links.WebLink
import org.intellij.plugin.tracker.services.LinkRetrieverService
import org.intellij.plugin.tracker.utils.CredentialsManager
import org.intellij.plugin.tracker.core.change.GitOperationManager
import org.intellij.plugin.tracker.utils.LinkFactory
import javax.swing.JComponent

/**
 * This class registers the secondary settings page of the plugin (for token management)
 * in the general settings of the project
 *
 * It corresponds to the page where a user can manage the tokens for multiple platforms.
 * This class also makes sure that these settings are saved on a project-level.
 */
@Label("Experimental")
@Description("Remote web links are not supported as of yet")
class LinkTrackingProjectConfigurable(val project: Project) : SearchableConfigurable {

    /**
     * Class bound to the form displaying the settings page for token management
     */
    private var tokenManagerForm: LinkTrackingTokenManagerForm? = null

    /**
     * The user info list (containing a pair of 2 string - representing platform and username)
     * fetched from the links that are present in the currently open project
     * This list will only containing info's of web-links that do not correspond to the open project
     */
    private lateinit var userInfoList: List<Pair<String, String>>

    /**
     * Creates the UI of the settings page.
     * It then fetches the web-links that do not correspond to the currently open project and
     * adds them to a list of pairs of strings (representing username and platform name).
     * It adds the userInfo to the UI and finally returns the component representing the UI for this page.
     */
    override fun createComponent(): JComponent? {
        val linkService: LinkRetrieverService = LinkRetrieverService.getInstance(project)
        val linkInfoList: MutableList<LinkInfo> = mutableListOf()

        ApplicationManager.getApplication().runReadAction {
            linkService.getLinks(linkInfoList)
        }

        val infoList: MutableList<Triple<String, String, String>> = mutableListOf()

        var remoteOriginUrl = ""

        ApplicationManager.getApplication().run {
            object : Task.Modal(project, "Retrieving remote origin url", true) {
                override fun run(indicator: ProgressIndicator) {
                    remoteOriginUrl = GitOperationManager(
                        project
                    ).getRemoteOriginUrl()
                }
            }
        }

        for (linkInfo: LinkInfo in linkInfoList) {
            val link: Link = LinkFactory.createLink(linkInfo)
            if (link is WebLink<*> && !link.correspondsToLocalProject(remoteOriginUrl)) {
                infoList.add(Triple(link.projectOwnerName, link.platformName, link.projectName))
            }
        }

        // group by project-owner-name and platform-name
        val result: List<Pair<String, String>> = infoList.groupBy { Pair(it.first, it.second) }
            .map { (e1, _) -> e1 }

        userInfoList = result
        val form: LinkTrackingTokenManagerForm = getForm()
        form.initializeTable(userInfoList)
        return form.component
    }

    /**
     * Get the class bound to the form displaying the settings page for token management
     */
    private fun getForm(): LinkTrackingTokenManagerForm {
        if (tokenManagerForm == null) {
            tokenManagerForm = LinkTrackingTokenManagerForm()
        }
        return tokenManagerForm!!
    }

    /**
     * Checks whether the settings page has any attributes that have been modified
     */
    override fun isModified(): Boolean {
        val form: LinkTrackingTokenManagerForm = getForm()
        return form.savedState != form.currentState
    }

    /**
     * Logic to run when the apply button is clicked in the settings page
     */
    override fun apply() {
        val form: LinkTrackingTokenManagerForm = getForm()
        val savedState: List<UserInfo> = form.savedState
        val currentState: List<UserInfo> = form.currentState
        for (userInfo: UserInfo in currentState) {
            if (userInfo.token != null) {
                if (userInfo.token.isNotBlank() || (userInfo.token.isBlank() && savedState.any { info ->
                        info.username == userInfo.username && info.platform == userInfo.platform &&
                                info.token != null && info.token.isNotBlank()
                    }))
                    CredentialsManager.storeCredentials(userInfo.platform, userInfo.username, userInfo.token)
            }
        }
    }

    /**
     * Logic to run when the reset button is clicked in the settings page
     */
    override fun reset() = getForm().updateTable()

    /**
     * Set the id of this settings page
     */
    override fun getId(): String = "TokenManager"

    /**
     * Set the display name of this settings page
     */
    override fun getDisplayName(): String = "Token Manager"
}
