package org.intellij.plugin.tracker.services

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import org.intellij.plugin.tracker.data.UpdateResult
import org.intellij.plugin.tracker.data.changes.FileChange
import org.intellij.plugin.tracker.data.changes.LinkChange
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.data.links.RelativeLinkToFile
import org.intellij.plugin.tracker.data.links.WebLink
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
    fun updateLinks(links: MutableCollection<Pair<Link, LinkChange>>): UpdateResult {
        val startTime = System.currentTimeMillis()
        val updated = mutableListOf<Link>()
        val failed = mutableListOf<Link>()
        for (link in links) {
            try {
                if (updateLink(link.first, link.second)) {
                    updated.add(link.first)
                } else {
                    failed.add(link.first)
                }
            } catch (e: NotImplementedError) {
                failed.add(link.first)
            }
        }
        val timeElapsed = System.currentTimeMillis() - startTime
        return UpdateResult(updated, failed, timeElapsed)
    }

    /**
     * Dispatches updating the link to the type specific function.
     *
     * @param link the Link object to be updated
     * @param fileChange the FileChange according to which to update the link
     */
    private fun updateLink(link: Link, fileChange: LinkChange): Boolean {
        when (link) {
            is RelativeLinkToFile -> return updateRelativeLink(link, fileChange as FileChange)
            is WebLink -> throw NotImplementedError()
            else -> throw NotImplementedError()
        }
    }

    /**
     * Updates a broken relative link.
     *
     * @param link the Link object to be updated
     * @param fileChange the FileChange according to which to update the link
     * @return true if update succeeded, false otherwise
     */
    private fun updateRelativeLink(link: RelativeLinkToFile, fileChange: FileChange): Boolean {
        val linkElement = getLinkElement(link) ?: return false
        if (fileChange.changeType == "MOVED") {
            val newPath = fileChange.afterPath ?: return false
            val newElement = MarkdownPsiElementFactory.createTextElement(this.project, newPath)
            linkElement.replace(newElement)
            return true
        } else {
            throw NotImplementedError()
        }
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

        val matchingFiles = FilenameIndex.getFilesByName(project,
                link.linkInfo.fileName,
                GlobalSearchScope.projectScope(project))
        matchingFiles.filter({ file -> file.virtualFile.path.endsWith(relativePath) })

        // Assume only one valid result
        assert(matchingFiles.size == 1)
        val psiFile = matchingFiles[0]
//        val psiFile = PsiManager.getInstance(project).findFile(virtualFile)!!
        return psiFile.findElementAt(link.linkInfo.textOffset)
    }
}