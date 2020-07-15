package org.intellij.plugin.tracker.services

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.elementType
import org.intellij.markdown.flavours.gfm.GFMTokenTypes.GFM_AUTOLINK
import org.intellij.plugin.tracker.data.links.LinkInfo
import org.intellij.plugin.tracker.core.update.LinkElement
import org.intellij.plugin.tracker.core.update.LinkElementImpl
import org.intellij.plugins.markdown.lang.MarkdownElementType
import org.intellij.plugins.markdown.lang.MarkdownElementTypes.*
import org.intellij.plugins.markdown.lang.MarkdownFileType
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownFile
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownLinkDestinationImpl

class LinkRetrieverService(private val project: Project?) {

    /**
     * Function to get the list of links from MD files.
     */
    fun getLinks(linkInfoList: MutableList<LinkInfo>) {
        val currentProject = project
        val virtualFiles =
            FileTypeIndex.getFiles(MarkdownFileType.INSTANCE, GlobalSearchScope.projectScope(currentProject!!))

        val psiDocumentManager = PsiDocumentManager.getInstance(project!!)

        for (virtualFile in virtualFiles) {
            val proveniencePath = virtualFile.path.replace("${currentProject.basePath!!}/", "")
            val psiFile: MarkdownFile = PsiManager.getInstance(currentProject).findFile(virtualFile!!) as MarkdownFile
            val document = psiDocumentManager.getDocument(psiFile)!!
            val fileName = psiFile.name

            psiFile.accept(object : PsiRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    val elemType = element.node.elementType

                    val linkText: String
                    val linkPath: String
                    val lineNumber: Int
                    val linkElement: LinkElement

                    if (element.javaClass == LeafPsiElement::class.java && (elemType === MarkdownElementType
                            .platformType(GFM_AUTOLINK) && element.parent.node.elementType !== LINK_DESTINATION)) {
                        linkText = element.node.text
                        lineNumber = document.getLineNumber(element.node.startOffset) + 1
                        linkElement =
                            LinkElementImpl(element)
                        linkInfoList.add(LinkInfo(linkText, linkText, proveniencePath, lineNumber, linkElement, fileName, currentProject))
                    } else if (element.javaClass == MarkdownLinkDestinationImpl::class.java && elemType === LINK_DESTINATION) {
                        var inlineLink = true
                        if (element.parent.elementType == LINK_DEFINITION) inlineLink = false
                        linkText = element.parent.firstChild.node.text.replace("[", "").replace("]", "")
                        linkPath = element.node.text
                        lineNumber = document.getLineNumber(element.node.startOffset) + 1
                        linkElement =
                            LinkElementImpl(element)
                        linkInfoList.add(LinkInfo(linkText, linkPath, proveniencePath, lineNumber, linkElement, fileName, currentProject, inlineLink = inlineLink))
                    } else if (element.javaClass == ASTWrapperPsiElement::class.java && elemType === AUTOLINK) {
                        linkText = element.node.text.replace("<", "").replace(">", "")
                        lineNumber = document.getLineNumber(element.node.startOffset) + 1
                        linkElement =
                            LinkElementImpl(element)
                        linkInfoList.add(LinkInfo(linkText, linkText, proveniencePath, lineNumber, linkElement, fileName, currentProject, "<", ">"))
                    }
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
