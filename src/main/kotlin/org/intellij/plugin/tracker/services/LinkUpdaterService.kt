package org.intellij.plugin.tracker.services

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import org.intellij.plugin.tracker.data.UpdateResult
import org.intellij.plugin.tracker.data.changes.Change
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.data.links.RelativeLink
import org.intellij.plugin.tracker.data.links.WebLink
import org.intellij.plugin.tracker.utils.LinkElement
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
    fun updateLinks(links: MutableCollection<Pair<Link, Change>>, newCommit: String?): UpdateResult {
        val startTime = System.currentTimeMillis()
        val updated = mutableListOf<Link>()
        val failed = mutableListOf<Link>()
        val listWithElements = getLinkElements(links)
        for (triple in listWithElements) {
            try {
                when {
                    triple.third == null -> failed.add(triple.first)
                    updateLink(triple.first, triple.second, triple.third!!, newCommit) -> updated.add(triple.first)
                    else -> failed.add(triple.first)
                }
            } catch (e: NotImplementedError) {
                failed.add(triple.first)
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
    private fun updateLink(link: Link, change: Change, element: LinkElement, newCommit: String?): Boolean {
        // if the change comes from the working tree, do not update the link
        // let the user do it via the UI!
        if (change.hasWorkingTreeChanges()) return false
        if (change.requiresUpdate || change.afterPath.any { path -> link.markdownFileMoved(path) }) {
            var afterPath: String? = null
            if (link is RelativeLink<*>) {
                val castLink: RelativeLink<Change> = link as RelativeLink<Change>
                afterPath = castLink.updateLink(change, newCommit)
            } else if (link is WebLink<*>) {
                val castLink: WebLink<Change> = link as WebLink<Change>
                afterPath = castLink.updateLink(change, newCommit)
            }

            // calculated updated link is null -> something wrong must have happened, return false
            if (afterPath == null) return false

            println("is it coming ${element.getText()}")
            val newElement: MarkdownPsiElement = MarkdownPsiElementFactory.createTextElement(this.project, afterPath)
            element.replace(newElement)
            return true
        }
        // only change changes of type MOVED
        return false
    }

    /**
     * Gets the PsiElement corresponding to each input Link.
     *
     * @return a List of Triple<Link, FileChange, PsiElement>
     */
    private fun getLinkElements(list: MutableCollection<Pair<Link, Change>>):
            MutableCollection<Triple<Link, Change, LinkElement?>> {
        return list.map { pair -> Triple(pair.first, pair.second, pair.first.linkInfo.linkElement) }.toMutableList()
    }
}
