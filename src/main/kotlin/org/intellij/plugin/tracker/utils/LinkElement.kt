package org.intellij.plugin.tracker.utils

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

interface LinkElement {

    fun replace(newElement: PsiElement)

    fun getText(): String?

    fun getNode(): ASTNode?
}
