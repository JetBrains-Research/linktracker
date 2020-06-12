package org.intellij.plugin.tracker.utils

import com.intellij.psi.PsiElement

class LinkElementImpl(private val element: PsiElement) : LinkElement {

    /**
     * Replaces this PSI element (along with all its children)
     * with another element (along with the children).
     */
    override fun replace(newElement: PsiElement): PsiElement? = element.replace(newElement)

    /**
     * Returns the text of the PSI element.
     */
    override fun getText(): String? = element.text
}
