package org.intellij.plugin.tracker.services

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import org.intellij.plugin.tracker.data.links.LinkInfo
import org.intellij.plugin.tracker.utils.LinkElementImpl
import org.intellij.plugins.markdown.lang.MarkdownFileType
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownFile

class LinkRetrieverService(private val project: Project?) {

    /**
     * Function to get the list of links from MD files.
     */
    fun getLinks(linkInfoList: MutableList<LinkInfo>) {
        val currentProject = project
        val virtualFiles =
                FileTypeIndex.getFiles(MarkdownFileType.INSTANCE, GlobalSearchScope.projectScope(currentProject!!))

        // val psiDocumentManager = PsiDocumentManager.getInstance(project!!)

        for (virtualFile in virtualFiles) {
            // val proveniencePath = virtualFile.path.replace("${currentProject.basePath!!}/", "")
            val psiFile: MarkdownFile = PsiManager.getInstance(currentProject).findFile(virtualFile!!) as MarkdownFile
            // val document = psiDocumentManager.getDocument(psiFile)!!
            // val fileName = psiFile.name

            psiFile.accept(object : PsiRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    val linkElement: LinkInfo = LinkElementImpl(element).parseElement()
                    linkInfoList.add(linkElement)
                    super.visitElement(element)
                }
            })
        }
    }

    /**
     * Used by IDEA to get a reference to the single instance of this class.
     */
    companion object {
        fun getInstance(project: Project): LinkRetrieverService =
                ServiceManager.getService(project, LinkRetrieverService::class.java)
    }
}
