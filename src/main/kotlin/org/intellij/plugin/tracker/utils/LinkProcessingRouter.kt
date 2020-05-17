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
        fun getChangesForLink(link: Link): Pair<Link, LinkChange> {
            val changeTrackerService: ChangeTrackerService = ChangeTrackerService.getInstance(link.linkInfo.project)

            when(link) {
                is RelativeLinkToDirectory -> return changeTrackerService.getDirectoryChange(link)
                is RelativeLinkToFile -> return changeTrackerService.getFileChange(link).second
                is RelativeLinkToLine -> {
                    val result = changeTrackerService.getFileChange(link)
                    println("FILE HISTORY LIST: ${result.first}")
                    // TODO: get the versions of the file using the file history list
                    // result.first will contains a list of Pair<String, String>, where the first element
                    // represents the commitSHA and the project relative path to the file
                    throw NotImplementedError("")
                }
                is RelativeLinkToLines -> {
                    val result = changeTrackerService.getFileChange(link)
                    println("FILE HISTORY LIST: ${result.first}")
                    throw NotImplementedError("")
                }
                is WebLinkToDirectory-> when {
                    link.correspondsToLocalProject() -> return changeTrackerService.getDirectoryChange(link)
                    else -> throw NotImplementedError("$link is not yet supported")
                }
                is WebLinkToFile -> when {
                    link.correspondsToLocalProject() -> return changeTrackerService.getFileChange(link).second
                    else -> throw NotImplementedError("$link is not yet supported")
                }
                is WebLinkToLine -> when {
                    link.correspondsToLocalProject() -> {
                        val result = changeTrackerService.getFileChange(link)
                        println("FILE HISTORY LIST: ${result.first}")
                        // TODO: get the versions of the file using the file history list
                        // result.first will contains a list of Pair<String, String>, where the first element
                        // represents the commitSHA and the project relative path to the file
                        throw NotImplementedError("")
                    }
                    else -> throw NotImplementedError("$link is not yet supported")
                }
                is WebLinkToLines -> when {
                    link.correspondsToLocalProject() -> {
                        val result = changeTrackerService.getFileChange(link)
                        println("FILE HISTORY LIST: ${result.first}")
                        throw NotImplementedError("")
                    }
                    else -> throw NotImplementedError("$link is not yet supported")
                }
                is NotSupportedLink -> throw NotImplementedError("$link is not yet supported")
                else -> throw NotImplementedError("$link is not yet supported")
            }
        }
    }
}