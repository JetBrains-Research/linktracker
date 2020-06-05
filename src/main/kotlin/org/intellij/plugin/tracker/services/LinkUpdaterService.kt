package org.intellij.plugin.tracker.services

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.elementType
import org.intellij.plugin.tracker.data.UpdateResult
import org.intellij.plugin.tracker.data.changes.Change
import org.intellij.plugin.tracker.data.links.*
import org.intellij.plugins.markdown.lang.MarkdownElementTypes.LINK_DESTINATION
import org.intellij.plugins.markdown.lang.MarkdownTokenTypes.GFM_AUTOLINK
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
        fun getInstance(project: Project) =
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
    private fun updateLink(link: Link, change: Change, element: PsiElement, newCommit: String?): Boolean {
        // if the change comes from the working tree, do not update the link
        // let the user do it via the UI!
        if (change.hasWorkingTreeChanges()) return false
        if (change.requiresUpdate || change.afterPath.any { path -> link.markdownFileMoved(path) }) {
            var afterPath: String? = null
            if (link is RelativeLink<*>) {
                val castLink: RelativeLink<Change> = link as RelativeLink<Change>
                afterPath = castLink.updateLink(change, newCommit)
            }
            else if (link is WebLink<*>) {
                val castLink: WebLink<Change> = link as WebLink<Change>
                afterPath = castLink.updateLink(change, newCommit)
            }

            // calculated updated link is null -> something wrong must have happened, return false
            if (afterPath == null) return false

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
            MutableCollection<Triple<Link, Change, PsiElement?>> {
        return list.map { pair -> Triple(pair.first, pair.second, getLinkElement(pair.first)) }.toMutableList()
    }

    /**
     * Gets the Psi element corresponding to the given Link object.
     *
     * @param link the Link object to search for.
     * @return an instance of PsiElement, or null if none found.
     */
    private fun getLinkElement(link: Link): PsiElement? {
        val fileRelativePath = link.linkInfo.proveniencePath
        val relativePath =
            if (fileRelativePath.startsWith("/")) fileRelativePath else "/$fileRelativePath"
        val matchingFiles = FilenameIndex.getFilesByName(
            project,
            link.linkInfo.fileName,
            GlobalSearchScope.projectScope(project)
        )
        matchingFiles.filter { file -> file.virtualFile.path.endsWith(relativePath) }
        // Assume only one valid result
        assert(matchingFiles.size == 1)
        val psiFile: PsiFile = matchingFiles[0]

        val parent: PsiElement? = psiFile.findElementAt(link.linkInfo.textOffset)?.parent
        val child: PsiElement? = psiFile.findElementAt(link.linkInfo.textOffset)
        return when (link) {
            is WebLink<*> -> when {
                parent.elementType == LINK_DESTINATION && child.elementType == GFM_AUTOLINK -> child
                else -> parent
            }
            else -> child
        }
    }
}