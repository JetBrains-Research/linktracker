package org.intellij.plugin.tracker.services

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import org.intellij.markdown.flavours.gfm.GFMTokenTypes.GFM_AUTOLINK
import org.intellij.plugin.tracker.data.Link
import org.intellij.plugin.tracker.data.LinkType
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
                        val link = Link(getLinkType(linkText), linkText, linkText)
                        println(link)
                        links.add(link)
                    } else if (element.javaClass == MarkdownLinkDestinationImpl::class.java && elemType === LINK_DESTINATION) {
                        linkFound = true
                        noOfLinks++
                        val linkPath = element.node.text
                        val linkText = element.parent.firstChild.node.text.replace("[", "").replace("]", "")
                        val link = Link(getLinkType(linkPath), linkText, linkPath)
                        println(link)
                        links.add(link)
                    } else if (element.javaClass == ASTWrapperPsiElement::class.java && elemType === AUTOLINK) {
                        linkFound = true
                        noOfLinks++
                        val linkText = element.node.text.replace("<", "").replace(">", "")
                        val link = Link(getLinkType(linkText), linkText, linkText)
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
}
