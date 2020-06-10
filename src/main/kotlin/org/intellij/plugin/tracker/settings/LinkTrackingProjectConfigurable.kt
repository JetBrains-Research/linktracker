package org.intellij.plugin.tracker.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import javax.swing.JComponent
import org.intellij.plugin.tracker.data.UserInfo
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.data.links.LinkInfo
import org.intellij.plugin.tracker.data.links.WebLink
import org.intellij.plugin.tracker.services.LinkRetrieverService
import org.intellij.plugin.tracker.utils.CredentialsManager
import org.intellij.plugin.tracker.utils.GitOperationManager
import org.intellij.plugin.tracker.utils.LinkFactory

class LinkTrackingProjectConfigurable(val project: Project) : SearchableConfigurable {
    private var tokenManagerForm: LinkTrackingTokenManagerForm? = null
    private lateinit var userInfoList: List<Pair<String, String>>

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
                    remoteOriginUrl = GitOperationManager(project).getRemoteOriginUrl()
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

    private fun getForm(): LinkTrackingTokenManagerForm {
        if (tokenManagerForm == null) {
            tokenManagerForm = LinkTrackingTokenManagerForm()
        }
        return tokenManagerForm!!
    }

    override fun isModified(): Boolean {
        val form: LinkTrackingTokenManagerForm = getForm()
        return form.savedState != form.currentState
    }

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

    override fun reset() = getForm().updateTable()

    override fun getId(): String = "TokenManager"

    override fun getDisplayName(): String = "Token Manager"
}
