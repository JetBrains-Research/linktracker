package org.intellij.plugin.tracker.services

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import org.intellij.plugin.tracker.data.results.UpdateResult
import org.intellij.plugin.tracker.data.changes.Change
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.data.links.RelativeLink
import org.intellij.plugin.tracker.data.links.WebLink
import org.intellij.plugin.tracker.core.update.LinkElement
import org.intellij.plugins.markdown.lang.psi.MarkdownPsiElement
import org.intellij.plugins.markdown.lang.psi.MarkdownPsiElementFactory

/**
 * A service to update broken links.
 */
class LinkUpdaterService(val project: Project) {

    /**
     * Used by IDEA to get a reference to the single instance of this class.
     */
    companion object {
        var workingTreePaths = mutableListOf<RelativeLink<*>>()

        fun getInstance(project: Project): LinkUpdaterService =
            ServiceManager.getService(project, LinkUpdaterService::class.java)
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
    fun batchUpdateLinks(links: MutableCollection<Pair<Link, Change>>, newCommit: String?): UpdateResult {
        val startTime = System.currentTimeMillis()
        val updated = mutableListOf<Link>()
        val failed = mutableListOf<Link>()
        for (pair in links) {
            val link = pair.first
            val change = pair.second
            try {
                when {
                    updateSingleLink(link, change, newCommit) -> updated.add(link)
                    else -> failed.add(link)
                }
            } catch (e: NotImplementedError) {
                failed.add(link)
            }
        }
        val timeElapsed = System.currentTimeMillis() - startTime
        return UpdateResult(updated, failed, timeElapsed)
    }

    /**
     * Dispatches updating the link to the type specific function.
     *
     * @param link the Link object to be updated
     * @param change the LinkChange object according to which to update the link
     */
    @Suppress("UNCHECKED_CAST")
    fun updateSingleLink(link: Link, change: Change, newCommit: String?, index: Int = 0): Boolean {
        var afterPath: String? = null
        if (link is RelativeLink<*>) {
            val castLink: RelativeLink<Change> = link as RelativeLink<Change>
            afterPath = castLink.updateLink(change, index, newCommit)
        } else if (link is WebLink<*>) {
            val castLink: WebLink<Change> = link as WebLink<Change>
            afterPath = castLink.updateLink(change, index, newCommit)
        }

        println("AFTER PATH IS: $afterPath")

        println("HEAD COMMIT SHA IS: $newCommit")

        // calculated updated link is null -> something wrong must have happened, return false
        if (afterPath == null) return false

        val newElement: MarkdownPsiElement = MarkdownPsiElementFactory.createTextElement(this.project, afterPath)
        link.linkInfo.linkElement.replace(newElement)
        return true
    }
}
