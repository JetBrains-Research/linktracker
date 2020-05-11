package org.intellij.plugin.tracker.services

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.intellij.util.indexing.FileBasedIndex
import org.intellij.plugin.tracker.data.changes.FileChange
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.data.links.RelativeLinkToFile
import org.intellij.plugin.tracker.data.links.WebLink
import java.util.*
import org.intellij.plugins.markdown.lang.psi.MarkdownPsiElementFactory as MarkdownPsiElementFactory


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
     * @param links the collection of link data necessary for the update
     */
    fun updateLinks(links: MutableCollection<Pair<Link, FileChange>>) {
        for (link in links) {
            updateLink(link.first, link.second)
        }
    }

    /**
     * Dispatches updating the link to the type specific function.
     *
     * @param link the Link object to be updated
     * @param fileChange the FileChange according to which to update the link
     */
    private fun updateLink(link: Link, fileChange: FileChange) {
        when (link) {
            is RelativeLinkToFile -> updateRelativeLink(link, fileChange)
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
    fun updateRelativeLink(link: RelativeLinkToFile, fileChange: FileChange): Boolean {
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
        val fileRelativePath = link.proveniencePath
        val relativePath =
            if (fileRelativePath.startsWith("/")) fileRelativePath else "/$fileRelativePath"
        val fileTypes: Set<FileType> =
            Collections.singleton(FileTypeManager.getInstance().getFileTypeByFileName(relativePath))
        val fileList: MutableList<VirtualFile> = ArrayList()
        FileBasedIndex.getInstance()
            .processFilesContainingAllKeys(FileTypeIndex.NAME, fileTypes,
                GlobalSearchScope.projectScope(project), null,
                Processor { virtualFile: VirtualFile ->
                    if (virtualFile.path.endsWith(relativePath)) {
                        fileList.add(virtualFile)
                    }
                    true
                }
            )

        // An alternative way of finding files can be by calling:
        // `FilenameIndex.getFilesByName(project, fileName, scope)`
        // and then checking the file's path against the relative path.

        // Assume only one valid result
        val virtualFile = fileList[0]
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile)!!
        return psiFile.findElementAt(link.textOffset)
    }
}