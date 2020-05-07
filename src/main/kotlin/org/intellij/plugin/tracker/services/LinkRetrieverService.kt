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
import org.intellij.plugin.tracker.data.*
import org.intellij.plugins.markdown.lang.MarkdownElementType
import org.intellij.plugins.markdown.lang.MarkdownElementTypes.AUTOLINK
import org.intellij.plugins.markdown.lang.MarkdownElementTypes.LINK_DESTINATION
import org.intellij.plugins.markdown.lang.MarkdownFileType
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownFile
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownLinkDestinationImpl
import java.util.regex.Matcher
import java.util.regex.Pattern

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
        val basePath = project?.basePath
        val virtualFiles =
            FileTypeIndex.getFiles(MarkdownFileType.INSTANCE, GlobalSearchScope.projectScope(currentProject!!))
        val psiDocumentManager = PsiDocumentManager.getInstance(project!!)
        noOfLinks = 0
        noOfFiles = 0
        noOfFilesWithLinks = 0
        for (virtualFile in virtualFiles) {
            linkFound = false
            noOfFiles++
            val proveniencePath = virtualFile.path.replace(basePath!!+"/", "")
            val psiFile: MarkdownFile = PsiManager.getInstance(currentProject).findFile(virtualFile!!) as MarkdownFile
            val document = psiDocumentManager.getDocument(psiFile)!!
            psiFile.accept(object : PsiRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    val elemType = element.node.elementType
                    if (element.javaClass == LeafPsiElement::class.java && (elemType === MarkdownElementType.platformType(GFM_AUTOLINK) &&
                                element.parent.node.elementType !== LINK_DESTINATION)) {
                        val linkText = element.node.text
                        addLink(element, document, linkText, linkText, proveniencePath)
                    } else if (element.javaClass == MarkdownLinkDestinationImpl::class.java && elemType === LINK_DESTINATION) {
                        val linkPath = element.node.text
                        val linkText = element.parent.firstChild.text.substring(1, element.parent.firstChild.text.length-1)
                        addLink(element, document, linkText, linkPath, proveniencePath)
                    } else if (element.javaClass == ASTWrapperPsiElement::class.java && elemType === AUTOLINK) {
                        val linkText = element.node.text.substring(1, element.node.text.length-1)
                        addLink(element, document, linkText, linkText, proveniencePath)
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
    fun createLink(linkText: String, linkPath: String, proveniencePath: String, lineNo: Int): Link {
        if (getLinkType(linkPath) == LinkType.URL) {
            if (linkPath.contains("github") || linkPath.contains("gitlab")) {
                val patternLine = Pattern.compile("https://([a-zA-Z.]+)/([a-zA-Z0-9-_./]+)/([a-zA-Z]+[a-zA-Z0-9-_.]+)/((blob/([a-z0-9]+))|(-/blob))/([a-zA-Z0-9-_./]+)#L([0-9]+)")
                val matcherLine: Matcher = patternLine.matcher(linkPath)
                if(matcherLine.matches()) {
                    val platformName = matcherLine.group(1)
                    val projectOwnerName = matcherLine.group(2)
                    val projectName = matcherLine.group(3)
                    val relativePath = matcherLine.group(8)
                    val start = matcherLine.group(9).toInt()
                    return WebLink(LinkType.LINE, linkText, linkPath, proveniencePath, lineNo, platformName, projectOwnerName, projectName, relativePath, WebLinkReferenceType.COMMIT, linkText, start, start, start)
                }
                val patternLines = Pattern.compile("https://([a-zA-Z.]+)/([a-zA-Z0-9-_./]+)/([a-zA-Z]+[a-zA-Z0-9-_.]+)/((blob/([a-z0-9]+))|(-/blob))/([a-zA-Z0-9-_./]+)#L([0-9]+)-L([0-9]+)")
                val matcherLines: Matcher = patternLines.matcher(linkPath)
                if(matcherLines.matches()) {
                    val platformName = matcherLines.group(1)
                    val projectOwnerName = matcherLines.group(2)
                    val projectName = matcherLines.group(3)
                    val relativePath = matcherLines.group(8)
                    val start = matcherLines.group(9).toInt()
                    val end = matcherLines.group(10).toInt()
                    return WebLink(LinkType.LINES, linkText, linkPath, proveniencePath, lineNo, platformName, projectOwnerName, projectName, relativePath, WebLinkReferenceType.COMMIT, linkText, start, start, end)
                }
                val patternUser = Pattern.compile("https://([a-zA-Z.]+)/([a-zA-Z0-9-_.]+)")
                val matcherUser: Matcher = patternUser.matcher(linkPath)
                if(matcherUser.matches()) {
                    val platformName = matcherUser.group(1)
                    val userName = matcherUser.group(2)
                    return WebLink(LinkType.USER, linkText, linkPath, proveniencePath, lineNo, platformName, userName, "", "", WebLinkReferenceType.TAG, linkText, 0, 0, 0)
                } else {
                    return WebLink(LinkType.URL, linkText, linkPath, proveniencePath, lineNo, "", "", "", "", WebLinkReferenceType.COMMIT, "", 0, 0, 0)
                }
            } else {
                return WebLink(LinkType.URL, linkText, linkPath, proveniencePath, lineNo, "", "", "", "", WebLinkReferenceType.COMMIT, "", 0, 0, 0)
            }
        } else {
            return RelativeLink(getLinkType(linkPath), linkText, linkPath, proveniencePath, lineNo)
        }
    }

    /**
     * Function to add links to our list.
     */
    fun addLink(element: PsiElement, document: Document, linkText: String, linkPath: String, proveniencePath: String) {
        linkFound = true
        noOfLinks++
        val textOffset: Int = element.node.startOffset
        val lineNumber: Int = document.getLineNumber(textOffset) + 1
        val link = createLink(linkText, linkPath, proveniencePath, lineNumber)
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
