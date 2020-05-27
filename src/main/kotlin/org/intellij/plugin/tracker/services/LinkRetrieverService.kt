package org.intellij.plugin.tracker.services

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import org.intellij.markdown.flavours.gfm.GFMTokenTypes.GFM_AUTOLINK
import org.intellij.plugin.tracker.data.links.LinkInfo
import org.intellij.plugins.markdown.lang.MarkdownElementType
import org.intellij.plugins.markdown.lang.MarkdownElementTypes.AUTOLINK
import org.intellij.plugins.markdown.lang.MarkdownElementTypes.LINK_DESTINATION
import org.intellij.plugins.markdown.lang.MarkdownFileType
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownFile
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownLinkDestinationImpl


class LinkRetrieverService(private val project: Project?) {

    var noOfLinks = 0
    var noOfFiles = 0
    var noOfFilesWithLinks = 0
    var linkFound = false

    /**
     * Function to get the list of links from MD files.
     */
    fun getLinks(linkInfoList: MutableList<LinkInfo>) {
        val currentProject = project
        val virtualFiles =
                FileTypeIndex.getFiles(MarkdownFileType.INSTANCE, GlobalSearchScope.projectScope(currentProject!!))

        val psiDocumentManager = PsiDocumentManager.getInstance(project!!)
        noOfLinks = 0
        noOfFiles = 0
        noOfFilesWithLinks = 0

        for (virtualFile in virtualFiles) {

            linkFound = false
            noOfFiles++

            val proveniencePath = virtualFile.path.replace("${currentProject.basePath!!}/", "")
            val psiFile: MarkdownFile = PsiManager.getInstance(currentProject).findFile(virtualFile!!) as MarkdownFile
            val document = psiDocumentManager.getDocument(psiFile)!!
            val fileName = psiFile.name

            psiFile.accept(object : PsiRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    val elemType = element.node.elementType

                    val linkText: String
                    val linkPath: String
                    val textOffset: Int
                    val lineNumber: Int

                    if (element.javaClass == LeafPsiElement::class.java && (elemType === MarkdownElementType.platformType(
                                    GFM_AUTOLINK
                            ) &&
                                    element.parent.node.elementType !== LINK_DESTINATION)
                    ) {
                        linkText = element.node.text
                        textOffset = element.node.startOffset
                        lineNumber = document.getLineNumber(textOffset) + 1
                        linkInfoList.add(LinkInfo(linkText, linkText, proveniencePath, lineNumber, textOffset, fileName, currentProject))

                    } else if (element.javaClass == MarkdownLinkDestinationImpl::class.java && elemType === LINK_DESTINATION) {
                        linkText = element.parent.firstChild.node.text.replace("[", "").replace("]", "")
                        linkPath = element.node.text
                        textOffset = element.node.startOffset
                        lineNumber = document.getLineNumber(textOffset) + 1
                        linkInfoList.add(LinkInfo(linkText, linkPath, proveniencePath, lineNumber, textOffset, fileName, currentProject))

                    } else if (element.javaClass == ASTWrapperPsiElement::class.java && elemType === AUTOLINK) {
                        linkText = element.node.text.replace("<", "").replace(">", "")
                        textOffset = element.node.startOffset
                        lineNumber = document.getLineNumber(textOffset) + 1
                        linkInfoList.add(LinkInfo(linkText, linkText, proveniencePath, lineNumber, textOffset, fileName, currentProject, "<", ">"))
                    }
                    super.visitElement(element)
                }
            })
            if (linkFound) {
                noOfFilesWithLinks++
            }
        }
    }

    /**
     * Function to get the list of links from javadoc comments.
     */
    fun getCommentLinks(linkInfoList: MutableList<LinkInfo>) {
        val currentProject = project

        ProjectFileIndex.SERVICE.getInstance(project).iterateContent {
            val proveniencePath = it.path.replace("${currentProject!!.basePath!!}/", "")
            val psiFile = PsiManager.getInstance(currentProject).findFile(it)
            val fileName = psiFile!!.name
            psiFile.accept(object : PsiRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    if (element.node != null) {
                        val elemType = element.node.elementType.toString()
                        if (elemType == "KDOC_TEXT" || elemType == "EOL_COMMENT" || elemType == "comment" || elemType == "line comment" || elemType == "<comment>") {
//                            println(element.text)
                        }
                    }
                    super.visitElement(element)
                }
            })
            true
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
