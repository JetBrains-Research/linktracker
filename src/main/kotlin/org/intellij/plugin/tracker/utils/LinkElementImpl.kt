package org.intellij.plugin.tracker.utils

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.intellij.plugin.tracker.data.links.LinkInfo

class LinkElementImpl(val element: PsiElement) {

    val linkText: String = ""
    val linkPath: String = ""
    val lineNumber: Int = 0
    val proveniencePath: String = ""
    val fileName: String = ""
    val project: Project = element.project

    fun parseElement(): LinkInfo {
        return LinkInfo(linkText, linkText, proveniencePath, lineNumber, 0, fileName, project, "<", ">")
    }

    val elemType = element.node.elementType
//
//    val linkText: String
//    val linkPath: String
//    val textOffset: Int
//    val lineNumber: Int
//
//    if (element.javaClass == LeafPsiElement::class.java && (elemType === MarkdownElementType.platformType(
//                    GFM_AUTOLINK
//            ) &&
//                    element.parent.node.elementType !== LINK_DESTINATION)
//    ) {
//        linkText = element.node.text
//        textOffset = element.node.startOffset
//        lineNumber = document.getLineNumber(textOffset) + 1
//        linkInfoList.add(LinkInfo(linkText, linkText, proveniencePath, lineNumber, textOffset, fileName, currentProject))
//    } else if (element.javaClass == MarkdownLinkDestinationImpl::class.java && elemType === LINK_DESTINATION) {
//        linkText = element.parent.firstChild.node.text.replace("[", "").replace("]", "")
//        linkPath = element.node.text
//        textOffset = element.node.startOffset
//        lineNumber = document.getLineNumber(textOffset) + 1
//        linkInfoList.add(LinkInfo(linkText, linkPath, proveniencePath, lineNumber, textOffset, fileName, currentProject))
//    } else if (element.javaClass == ASTWrapperPsiElement::class.java && elemType === AUTOLINK) {
//        linkText = element.node.text.replace("<", "").replace(">", "")
//        textOffset = element.node.startOffset
//        lineNumber = document.getLineNumber(textOffset) + 1
//        linkInfoList.add(LinkInfo(linkText, linkText, proveniencePath, lineNumber, textOffset, fileName, currentProject, "<", ">"))
//    }
}
