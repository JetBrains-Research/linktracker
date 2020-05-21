package org.intellij.plugin.tracker.utils

import org.intellij.plugin.tracker.data.changes.ChangeType
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
                is RelativeLinkToFile -> {
//                    if(link.linkInfo.linkPath=="build.gradle") {
//                        println(changeTrackerService.getLinkChange(link))
//                    }
                    return changeTrackerService.getFileChange(link).second }
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
                is WebLinkToFile -> return when {
                    link.correspondsToLocalProject() -> {
                        val webLinkReferenceType: WebLinkReferenceType = link.getWebLinkReferenceType()

                        // match on the web link reference type and call the methods with the right parameters
                        when (webLinkReferenceType) {
                            WebLinkReferenceType.COMMIT -> changeTrackerService.getFileChange(link, specificCommit = link.getReferencingName()).second
                            WebLinkReferenceType.BRANCH, WebLinkReferenceType.TAG ->
                                changeTrackerService.getFileChange(link, branchOrTagName = link.getReferencingName()).second
                            WebLinkReferenceType.INVALID ->
                                Pair(link, LinkChange(ChangeType.INVALID, errorMessage = "Web link reference name is invalid", afterPath = link.linkInfo.linkPath))
                        }
                    }
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