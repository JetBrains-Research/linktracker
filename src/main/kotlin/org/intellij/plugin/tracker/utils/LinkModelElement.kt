package org.intellij.plugin.tracker.utils

import com.intellij.lang.ASTNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.openapi.util.UserDataHolder
import com.intellij.psi.PsiInvalidElementAccessException
import com.intellij.util.ArrayFactory
import com.intellij.util.IncorrectOperationException
import org.jetbrains.annotations.Contract
import org.jetbrains.annotations.NonNls

/**
 * The interface for link elements.
 */
interface LinkModelElement : UserDataHolder, Iconable {
    /**
     * Returns the project to which the PSI element belongs.
     *
     * @return the project instance.
     * @throws PsiInvalidElementAccessException if this element is invalid
     */
    @get:Throws(PsiInvalidElementAccessException::class)
    @get:Contract(pure = true)
    val project: Project

    /**
     * Returns the offset in the file to which the caret should be placed
     * when performing the navigation to the element. (For classes implementing
     * [PsiNamedElement], this should return the offset in the file of the
     * name identifier.)
     *
     * @return the offset of the PSI element.
     */
    @get:Contract(pure = true)
    val textOffset: Int

    /**
     * Returns the text of the PSI element.
     *
     *
     * Note: This call requires traversing whole subtree, so it can be expensive for composite elements, and should be avoided if possible.
     *
     * @return the element text.
     * @see .textMatches
     *
     * @see .textContains
     */
    @get:Contract(pure = true)
    @get:NonNls
    val text: String?

    /**
     * Replaces this PSI element (along with all its children) with another element
     * (along with the children).
     *
     * @param newElement the element to replace this element with.
     * @return the element which was actually inserted in the tree (either `newElement` or its copy)
     * @throws IncorrectOperationException if the modification is not supported or not possible for some reason.
     */
    @Throws(IncorrectOperationException::class)
    fun replace(newElement: LinkModelElement): LinkModelElement?

    /**
     * Returns the AST node corresponding to the element.
     *
     * @return the AST node instance.
     */
    @get:Contract(pure = true)
    val node: ASTNode?

    companion object {
        /**
         * The empty array of PSI elements which can be reused to avoid unnecessary allocations.
         */
        val EMPTY_ARRAY = arrayOfNulls<LinkModelElement>(0)
        val ARRAY_FACTORY =
            ArrayFactory { count: Int ->
                if (count == 0) EMPTY_ARRAY else arrayOfNulls(
                    count
                )
            }
    }
}
