package org.intellij.plugin.tracker.services

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import org.intellij.markdown.flavours.gfm.GFMTokenTypes.GFM_AUTOLINK
import org.intellij.plugin.tracker.data.Link
import org.intellij.plugin.tracker.data.LinkType
import org.intellij.plugin.tracker.data.RelativeLink
import org.intellij.plugin.tracker.data.WebLink
import org.intellij.plugin.tracker.data.WebLinkReferenceType
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
    var links: ArrayList<Link> = arrayListOf<Link>()

    /**
     * Function to get the list of links.
     */
    fun getLinks(): List<Link> {
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
            val psiFile: MarkdownFile = PsiManager.getInstance(currentProject).findFile(virtualFile!!) as MarkdownFile
            val document = psiDocumentManager.getDocument(psiFile)!!
            val fileName = psiFile.name
            psiFile.accept(object : PsiRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    val elemType = element.node.elementType
                    if (element.javaClass == LeafPsiElement::class.java && (elemType === MarkdownElementType.platformType(GFM_AUTOLINK) &&
                                element.parent.node.elementType !== LINK_DESTINATION)) {
                        val linkText = element.node.text
                        addLink(element, document, linkText, linkText, fileName)
                    } else if (element.javaClass == MarkdownLinkDestinationImpl::class.java && elemType === LINK_DESTINATION) {
                        val linkPath = element.node.text
                        val linkText = element.parent.firstChild.node.text.replace("[", "").replace("]", "")
                        addLink(element, document, linkText, linkPath, fileName)
                    } else if (element.javaClass == ASTWrapperPsiElement::class.java && elemType === AUTOLINK) {
                        val linkText = element.node.text.replace("<", "").replace(">", "")
                        addLink(element, document, linkText, linkText, fileName)
                    }
                    super.visitElement(element)
                }
            })
            if (linkFound) {
                noOfFilesWithLinks++
            }
        }
        return links
    }

    /**
     * Function which gives the type of the link.
     */
    fun getLinkType(link: String): LinkType {
        if (link.contains("https") || link.contains("http") || link.contains("www")) {
            return LinkType.URL
        } else if (link.contains(".")) {
            return LinkType.FILE
        } else {
            return LinkType.DIRECTORY
        }
    }

    /**
     * Function which creates the link according to its type.
     */
    fun createLink(linkText: String, linkPath: String, proveniencePath: String, lineNo: Int, textOffset: Int): Link {
        if (getLinkType(linkPath) == LinkType.URL) {
            if (linkPath.contains("github") || linkPath.contains("gitlab")) {
                // commit
                if (linkPath.contains("blob")) {
                    val parts = linkPath.split("//").get(1).split("/blob/")
                    val projectName = parts.get(0).split("/").get(parts.get(0).split("/").lastIndex)
                    val platformName = parts.get(0).split("/").get(0)
                    val projectOwnerName = parts.get(0).split(platformName + "/").get(1).split("/" + projectName).get(0)
                    val relativePath = parts.get(1).split(parts.get(1).split("/").get(0) + "/").get(1).split("#L").get(0)
                    if (parts.get(1).split("#L").get(1).contains("-L")) {
                        val start = parts.get(1).split("#L").get(1).split("-L").get(0).toInt()
                        val end = parts.get(1).split("#L").get(1).split("-L").get(1).toInt()
                        return WebLink(LinkType.LINES, linkText, linkPath, proveniencePath, lineNo, textOffset, platformName, projectOwnerName, projectName, relativePath, WebLinkReferenceType.COMMIT, linkText, start, start, end)
                    } else {
                        val lineReferenced = parts.get(1).split("#L").get(1).toInt()
                        return WebLink(LinkType.LINE, linkText, linkPath, proveniencePath, lineNo, textOffset, platformName, projectOwnerName, projectName, relativePath, WebLinkReferenceType.COMMIT, linkText, lineReferenced, lineReferenced, lineReferenced)
                    }
                } else {
                    val userName = linkPath.split("//").get(1).split("/").get(1)
                    val platformName = linkPath.split("//").get(1).split("/").get(0)
                    return WebLink(LinkType.USER, linkText, linkPath, proveniencePath, lineNo, textOffset, platformName, userName, "", "", WebLinkReferenceType.TAG, linkText, 0, 0, 0)
                }
            } else {
                return WebLink(LinkType.URL, linkText, linkPath, proveniencePath, lineNo, textOffset, "", "", "", "", WebLinkReferenceType.COMMIT, "", 0, 0, 0)
            }
        } else {
            return RelativeLink(getLinkType(linkPath), linkText, linkPath, proveniencePath, lineNo, textOffset)
        }
    }

    /**
     * Function to add links to our list.
     */
    fun addLink(element: PsiElement, document: Document, linkText: String, linkPath: String, fileName: String) {
        linkFound = true
        noOfLinks++
        val textOffset: Int = element.node.startOffset
        val lineNumber: Int = document.getLineNumber(textOffset) + 1
        val link = createLink(linkText, linkPath, fileName, lineNumber, textOffset)
        links.add(link)
    }

    /**
     * Used by IDEA to get a reference to the single instance of this class.
     */
    companion object {
        fun getInstance(project: Project): LinkRetrieverService =
            ServiceManager.getService(project, LinkRetrieverService::class.java)
    }
}
