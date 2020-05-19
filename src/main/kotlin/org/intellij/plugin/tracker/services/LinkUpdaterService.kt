package org.intellij.plugin.tracker.services

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import org.intellij.plugin.tracker.data.UpdateResult
import org.intellij.plugin.tracker.data.changes.ChangeType
import org.intellij.plugin.tracker.data.changes.LinkChange
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.data.links.RelativeLinkToFile
import org.intellij.plugin.tracker.data.links.WebLink
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
    fun updateLinks(links: MutableCollection<Pair<Link, LinkChange>>): UpdateResult {
        val startTime = System.currentTimeMillis()
        val updated = mutableListOf<Link>()
        val failed = mutableListOf<Link>()
        val listWithElements = getLinkElements(links)
        for (triple in listWithElements) {
            try {
                if (triple.third == null) {
                    failed.add(triple.first)
                } else if (updateLink(triple.first, triple.second, triple.third!!)) {
                    updated.add(triple.first)
                } else {
                    failed.add(triple.first)
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
     * @param fileChange the FileChange according to which to update the link
     */
    private fun updateLink(link: Link, fileChange: LinkChange, element: PsiElement): Boolean {
        when (link) {
            is RelativeLinkToFile -> return updateRelativeLink(link, fileChange, element)
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
    private fun updateRelativeLink(link: RelativeLinkToFile, fileChange: LinkChange, element: PsiElement): Boolean {
        if (fileChange.changeType == ChangeType.MOVED) {
            var newPath = fileChange.afterPath
            // transform the path to the original format: this will mostly work for paths which
            // do not contain ../ or ./ in their original format
            newPath = link.linkInfo.getAfterPathToOriginalFormat(newPath)
            val newElement: MarkdownPsiElement = MarkdownPsiElementFactory.createTextElement(this.project, newPath)
            element.replace(newElement)
            return true
        } else {
            throw NotImplementedError()
        }
    }

    /**
     * Gets the PsiElement corresponding to each input Link.
     *
     * @return a List of Triple<Link, FileChange, PsiElement>
     */
    private fun getLinkElements(list: MutableCollection<Pair<Link, LinkChange>>):
            MutableCollection<Triple<Link, LinkChange, PsiElement?>> {
        return list.map { pair -> Triple(pair.first, pair.second, getLinkElement(pair.first)) }
                as MutableCollection<Triple<Link, LinkChange, PsiElement?>>
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
        return psiFile.findElementAt(link.linkInfo.textOffset)
    }
}