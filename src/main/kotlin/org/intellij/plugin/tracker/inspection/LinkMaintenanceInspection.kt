package org.intellij.plugin.tracker.inspection

import com.intellij.codeInspection.*
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.util.IncorrectOperationException
import org.intellij.plugin.tracker.core.change.ChangeTrackerImpl
import org.intellij.plugin.tracker.core.change.ChangeTrackingPolicy
import org.intellij.plugin.tracker.core.change.GitOperationManager
import org.intellij.plugin.tracker.data.ChangeGatheringException
import org.intellij.plugin.tracker.data.changes.Change
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.data.links.NotSupportedLink
import org.intellij.plugin.tracker.data.links.WebLink
import org.intellij.plugin.tracker.data.links.WebLinkReferenceType
import org.intellij.plugin.tracker.services.LinkRetrieverService
import org.intellij.plugin.tracker.services.LinkUpdaterService
import org.intellij.plugin.tracker.utils.LinkFactory
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownFile


class LinkMaintenanceInspection: LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (holder.file is MarkdownFile) {
                    val linkRetrieverService = LinkRetrieverService.getInstance(holder.project)
                    val linkInfo = linkRetrieverService.getLinkInfoFromLinkElement(holder.file, element)
                    if (linkInfo != null) {
                        val link = LinkFactory.createLink(linkInfo)
                        if (link !is NotSupportedLink) {
                            try {
                                val change = link.visit(ChangeTrackerImpl(holder.project, ChangeTrackingPolicy.LOCAL))
                                if (change.requiresUpdate) {
                                    for (index in change.afterPath.indices) {
                                        holder.registerProblem(element, "My description", QuickLinkFix(link, change, index))
                                    }
                                }
                            } catch (e: ChangeGatheringException) {

                            } catch (e: VcsException) {

                            }
                        }
                    }
                    super.visitElement(element)
                }
            }
        }
    }

    internal class QuickLinkFix(link: Link, change: Change, index: Int): LocalQuickFix {

        private val myToFixLink = link
        private val myFixingChange = change
        private val myIndex = index

        override fun getFamilyName(): String = "Update link to " + myFixingChange.afterPath[myIndex]

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            try {
                var headCommitSHA: String? = null
                if (myToFixLink is WebLink<*> && myToFixLink.referenceType == WebLinkReferenceType.COMMIT) {
                    headCommitSHA = calculateHEADCommitSHA(project)
                }
                LinkUpdaterService.getInstance(project).updateSingleLink(myToFixLink, myFixingChange, headCommitSHA, myIndex)
            } catch (e: IncorrectOperationException) {

            } catch (e: VcsException) {

            }
        }

        private fun calculateHEADCommitSHA(project: Project): String? {
            return try {
                ProgressManager.getInstance().runProcessWithProgressSynchronously<String?, VcsException>(
                    { GitOperationManager(project).getHeadCommitSHA() },
                    "Getting head commit SHA..",
                    true,
                    project
                )
            } catch (e: VcsException) {
                null
            }
        }
    }
}