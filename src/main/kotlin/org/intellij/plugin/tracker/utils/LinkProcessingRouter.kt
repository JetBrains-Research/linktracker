package org.intellij.plugin.tracker.utils

import com.intellij.openapi.project.Project
import org.intellij.plugin.tracker.data.changes.LinkChange
import org.intellij.plugin.tracker.data.links.*
import org.intellij.plugin.tracker.services.ChangeTrackerService

class LinkProcessingRouter {

    companion object {

        /**
         * Takes in the link and calls ChangeTrackerService API methods depending on the link type
         */
        fun getChangesForLink(link: Link, project: Project): Pair<Link, LinkChange> {
            val changeTrackerService = ChangeTrackerService(project)

            when (link) {
                is RelativeLinkToDirectory -> throw NotImplementedError("")
                is RelativeLinkToFile -> return changeTrackerService.getFileChange(link)
                is RelativeLinkToLine -> throw NotImplementedError("")
                is RelativeLinkToLines -> throw NotImplementedError("")
                is WebLinkToDirectory -> when {
                    link.correspondsToLocalProject(project) -> throw NotImplementedError("$link is not yet supported")
                    else -> throw NotImplementedError("$link is not yet supported")
                }
                is WebLinkToFile -> when {
                    link.correspondsToLocalProject(project) -> return changeTrackerService.getFileChange(link)
                    else -> throw NotImplementedError("$link is not yet supported")
                }
                is WebLinkToLine -> when {
                    link.correspondsToLocalProject(project) -> throw NotImplementedError("$link is not yet supported")
                    else -> throw NotImplementedError("$link is not yet supported")
                }
                is WebLinkToLines -> when {
                    link.correspondsToLocalProject(project) -> throw NotImplementedError("$link is not yet supported")
                    else -> throw NotImplementedError("$link is not yet supported")
                }
                is NotSupportedLink -> throw NotImplementedError("$link is not yet supported")
                else -> throw NotImplementedError("$link is not yet supported")
            }
        }
    }
}