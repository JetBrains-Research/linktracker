package org.intellij.plugin.tracker.services

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import java.util.Objects
import kotlin.collections.ArrayList
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
    var noOfFIlesWithLinks = 0
    var linkFound = false

    /**
     * function to get the list of links
     */
    public fun getLinks(): List<Link> {
        var links: ArrayList<Link> = arrayListOf<Link>()
        val currentProject = project
        val document = Objects.requireNonNull(
            FileEditorManager.getInstance(currentProject!!)
                .selectedTextEditor
        )!!.document
        val virtualFiles =
            FileTypeIndex.getFiles(MarkdownFileType.INSTANCE, GlobalSearchScope.projectScope(currentProject!!))
        noOfLinks = 0
        noOfFiles = 0
        noOfFIlesWithLinks = 0
        for (virtualFile in virtualFiles) {
            linkFound = false
            noOfFiles++
            val psiFile: MarkdownFile? = PsiManager.getInstance(currentProject).findFile(virtualFile!!) as MarkdownFile?
            psiFile?.accept(object : PsiRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    val elemType = element.node.elementType
                    if (element.javaClass == LeafPsiElement::class.java && (elemType === MarkdownElementType.platformType(
                            GFM_AUTOLINK
                        ) &&
                                element.parent.node.elementType !== LINK_DESTINATION)
                    ) {
                        linkFound = true
                        noOfLinks++
                        val linkText = element.node.text
                        val textOffset: Int = element.node.startOffset
                        val lineNumber: Int = document.getLineNumber(textOffset) + 1
                        val link = createLink(linkText, linkText, "", lineNumber)
                        println(link)
                        links.add(link)
                    } else if (element.javaClass == MarkdownLinkDestinationImpl::class.java && elemType === LINK_DESTINATION) {
                        linkFound = true
                        noOfLinks++
                        val linkPath = element.node.text
                        val linkText = element.parent.firstChild.node.text.replace("[", "").replace("]", "")
                        val textOffset: Int = element.node.startOffset
                        val lineNumber: Int = document.getLineNumber(textOffset) + 1
                        val link = createLink(linkText, linkPath, "", lineNumber)
                        println(link)
                        links.add(link)
                    } else if (element.javaClass == ASTWrapperPsiElement::class.java && elemType === AUTOLINK) {
                        linkFound = true
                        noOfLinks++
                        val linkText = element.node.text.replace("<", "").replace(">", "")
                        val textOffset: Int = element.node.startOffset
                        val lineNumber: Int = document.getLineNumber(textOffset) + 1
                        val link = createLink(linkText, linkText, "", lineNumber)
                        println(link)
                        links.add(link)
                    }
                    super.visitElement(element)
                }
            })
            if (linkFound) {
                noOfFIlesWithLinks++
            }
        }
        println(noOfFiles)
        println(noOfFIlesWithLinks)
        println(noOfLinks)
        return links
    }

    /**
     * function which gives the type of the link
     */
    public fun getLinkType(link: String): LinkType {
        if (link.contains("https") || link.contains("http") || link.contains("www")) {
            return LinkType.URL
        } else if (link.contains(".")) {
            return LinkType.FILE
        } else {
            return LinkType.DIRECTORY
        }
    }

    public fun createLink(linkText: String, linkPath: String, proveniencePath: String, lineNo: Int): Link {
        if (getLinkType(linkPath) == LinkType.URL) {
            // TODO implement weblink functionality
            return WebLink(LinkType.URL, linkText, linkPath, proveniencePath, lineNo, "", "", "", "", WebLinkReferenceType.COMMIT, "", 0, 0, 0)
        } else {
            return RelativeLink(getLinkType(linkPath), linkText, linkPath, proveniencePath, lineNo)
        }
    }
}
