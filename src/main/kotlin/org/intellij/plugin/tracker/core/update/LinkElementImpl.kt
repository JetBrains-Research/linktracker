package org.intellij.plugin.tracker.utils

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

class LinkElementImpl(private val element: PsiElement) : LinkElement {

    /**
     * Replaces this PSI element (along with all its children)
     * with another element (along with the children).
     */
    override fun replace(newElement: PsiElement) = element.node.replaceAllChildrenToChildrenOf(newElement.node)

    /**
     * Returns the text of the PSI element.
     */
    override fun getText(): String? = element.text

    /**
     * Returns the AST node corresponding to the element.
     */
    override fun getNode(): ASTNode? = element.node
}
