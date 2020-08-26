package org.intellij.plugin.tracker.services

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import org.intellij.markdown.flavours.gfm.GFMTokenTypes.GFM_AUTOLINK
import org.intellij.plugin.tracker.data.links.LinkInfo
import org.intellij.plugins.markdown.lang.MarkdownElementType
import org.intellij.plugins.markdown.lang.MarkdownElementTypes.*
import org.intellij.plugins.markdown.lang.MarkdownFileType
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownFile
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownLinkDestinationImpl

class LinkRetrieverService(private val project: Project?) {

    /**
     * Function to get the list of links from MD files.
     */
    fun getLinksInProjectScope(): List<LinkInfo> {
        val linkInfoList: MutableList<LinkInfo> = mutableListOf()
        val virtualFiles = FileTypeIndex.getFiles(MarkdownFileType.INSTANCE, GlobalSearchScope.projectScope(project!!))
        for (virtualFile in virtualFiles) {
            val psiFile: MarkdownFile = PsiManager.getInstance(project).findFile(virtualFile!!) as MarkdownFile
            linkInfoList.addAll(getLinksInFileScope(psiFile))
        }
        return linkInfoList
    }

    private fun classifyLinkElementAndExtractInfo(element: PsiElement, document: Document, file: PsiFile): LinkInfo? =
        when {
            isElementGfmAutoLink(element) -> LinkInfo.constructLinkInfoGfmAutoLink(element, document, file, project!!)
            isElementMarkdownLinkDestination(element) -> LinkInfo.constructLinkInfoMarkdownLinkDestination(element, document, file, project!!)
            isElementAutoLink(element) -> LinkInfo.constructLinkInfoAutoLink(element, document, file, project!!)
            else -> null
        }

    fun getLinkInfoFromLinkElement(file: PsiFile, element: PsiElement): LinkInfo? {
        val document = PsiDocumentManager.getInstance(project!!).getDocument(file)!!
        return classifyLinkElementAndExtractInfo(element, document, file)
    }

    fun getLinksInFileScope(file: PsiFile): List<LinkInfo> {
        val linkInfoList: MutableList<LinkInfo> = mutableListOf()
        val document = PsiDocumentManager.getInstance(project!!).getDocument(file)!!
        file.accept(object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                val linkInfo = classifyLinkElementAndExtractInfo(element, document, file)
                if (linkInfo != null) linkInfoList.add(linkInfo)
                else super.visitElement(element)
            }
        })
        return linkInfoList
    }

    /**
     * Used by IDEA to get a reference to the single instance of this class.
     */
    companion object {
        fun getInstance(project: Project): LinkRetrieverService =
            ServiceManager.getService(project, LinkRetrieverService::class.java)

        fun isElementMarkdownLinkDestination(element: PsiElement): Boolean =
            element.javaClass == MarkdownLinkDestinationImpl::class.java && element.node.elementType === LINK_DESTINATION

        fun isElementGfmAutoLink(element: PsiElement): Boolean =
            element.javaClass == LeafPsiElement::class.java &&
                    (element.node.elementType === MarkdownElementType.platformType(GFM_AUTOLINK) && element.parent.node.elementType !== LINK_DESTINATION)

        fun isElementAutoLink(element: PsiElement): Boolean =
            element.javaClass == ASTWrapperPsiElement::class.java && element.node.elementType === AUTOLINK
    }
}
