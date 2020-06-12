package org.intellij.plugin.tracker.utils

import com.intellij.psi.PsiElement

interface LinkElement {

    fun replace(newElement: PsiElement): PsiElement?

    fun getText(): String?
}
