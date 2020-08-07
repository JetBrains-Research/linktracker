package org.intellij.plugin.tracker.services

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import org.intellij.plugin.tracker.data.results.UpdateResult
import org.intellij.plugin.tracker.data.changes.Change
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.data.links.RelativeLink
import org.intellij.plugin.tracker.data.links.WebLink
import org.intellij.plugins.markdown.lang.psi.MarkdownPsiElement
import org.intellij.plugins.markdown.lang.psi.MarkdownPsiElementFactory
import java.sql.Timestamp

/**
 * A service to update broken links.
 */
class LinkUpdaterService(val project: Project) {

    /**
     * Used by IDEA to get a reference to the single instance of this class.
     */
    companion object {

        fun getInstance(project: Project): LinkUpdaterService = ServiceManager.getService(project, LinkUpdaterService::class.java)

        @Suppress("UNCHECKED_CAST")
        fun getNewLinkPath(link: Link, change: Change, index: Int, newCommit: String? = null): String? {
            var afterPath: String? = null
            if (link is RelativeLink<*>) {
                val castLink: RelativeLink<Change> = link as RelativeLink<Change>
                afterPath = castLink.updateLink(change, index, newCommit)
            } else if (link is WebLink<*>) {
                val castLink: WebLink<Change> = link as WebLink<Change>
                afterPath = castLink.updateLink(change, index, newCommit)
            }
            return afterPath
        }
    }

    /**
     * Update a batch of broken links according to their paired file changes.
     *
     * WARNING: This function can only be called inside a WriteCommandAction, like:
     *
     * `ApplicationManager.getApplication().invokeLater {
     *   WriteCommandAction.runWriteCommandAction(currentProject, Runnable {
     *      LinkUpdaterService.getInstance(project).updateLinks(links)
     *  }}`
     *
     * @param links the collection of link data necessary for the update
     */
    fun batchUpdateLinks(links: MutableCollection<Pair<Link, Change>>, newCommit: String? = null): UpdateResult {
        val startTime = System.currentTimeMillis()
        val updated = mutableListOf<Link>()
        val failed = mutableListOf<Link>()
        for (pair in links) {
            val link = pair.first
            val change = pair.second
            try {
                when {
                    updateSingleLink(link, change, newCommit = newCommit) -> updated.add(link)
                    else -> failed.add(link)
                }
            } catch (e: NotImplementedError) {
                failed.add(link)
            }
        }
        return UpdateResult(updated, failed, System.currentTimeMillis() - startTime)
    }

    /**
     * Dispatches updating the link to the type specific function.
     *
     * @param link the Link object to be updated
     * @param change the LinkChange object according to which to update the link
     */
    fun updateSingleLink(link: Link, change: Change, index: Int = 0, newCommit: String? = null): Boolean {
        if (change.isChangeDelete()) {
            link.linkInfo.linkElement.delete()
            return true
        }
        // calculated updated link is null -> something wrong must have happened, return false
        val afterPath: String = getNewLinkPath(link, change, index, newCommit) ?: return false
        val newElement: MarkdownPsiElement = MarkdownPsiElementFactory.createTextElement(this.project, afterPath)
        link.linkInfo.linkElement.replace(newElement)
        HistoryService.getInstance(project).saveTimestamp(link.linkInfo.proveniencePath, afterPath, Timestamp(System.currentTimeMillis()).time)
        return true
    }
}
