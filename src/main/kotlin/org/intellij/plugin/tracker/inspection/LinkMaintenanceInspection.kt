package org.intellij.plugin.tracker.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
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
import java.util.logging.Level
import java.util.logging.Logger


class LinkMaintenanceInspection : LocalInspectionTool() {

    private val logger = Logger.getLogger(LinkMaintenanceInspection::class::simpleName.name)

    private val REFERENCE_TYPE_BY_LINK: HashMap<String, WebLinkReferenceType> = hashMapOf()
    // TODO: Add listener for branch checkout
    private var BRANCH_NAME: String? = null

    private var REMOTE_ORIGIN_URL: String? = null

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {

            override fun visitFile(file: PsiFile) {
                ApplicationManager.getApplication().invokeLater(
                    { FileDocumentManager.getInstance().saveAllDocuments() },
                    ModalityState.defaultModalityState()
                )
                super.visitFile(file)
            }

            override fun visitElement(element: PsiElement) {
                if (holder.file is MarkdownFile) {
                    val linkInfo = LinkRetrieverService.getInstance(holder.project).getLinkInfoFromLinkElement(holder.file, element)
                    if (linkInfo != null) {
                        val link = LinkFactory.createLink(linkInfo)
                        if (doContinueLinkFixing(link)) {
                            try {
                                val change = link.visit(ChangeTrackerImpl(holder.project, ChangeTrackingPolicy.LOCAL))
                                if (change.requiresUpdate) {
                                    for (index in change.afterPath.indices) {
                                        holder.registerProblem(
                                            element,
                                            getDescription(link.linkInfo.linkPath),
                                            QuickLinkFix(link, change, index)
                                        )
                                    }
                                }
                            } catch (e: ChangeGatheringException) {
                                logger.log(Level.WARNING, e.message)
                            } catch (e: VcsException) {
                                logger.log(Level.WARNING, e.message)
                            }
                        }
                    }
                    super.visitElement(element)
                }
            }

            private fun doContinueLinkFixing(link: Link): Boolean {
                if (link is NotSupportedLink) return false
                if (link is WebLink<*>) {
                    if (getWebLinkReferenceType(link) == WebLinkReferenceType.COMMIT) return false
                    if (!link.correspondsToLocalProject(getRemoteOriginUrl())) return false
                    if (getCurrentBranchName() != link.referencingName) return false
                }
                return true
            }

            private fun getCurrentBranchName(): String? {
                if (BRANCH_NAME != null) return BRANCH_NAME
                var branchName: String? = null
                ProgressManager.getInstance().executeProcessUnderProgress({
                    branchName = GitOperationManager(holder.project).getCurrentBranchName()
                    BRANCH_NAME = branchName
                }, EmptyProgressIndicator())
                return branchName
            }

            private fun getRemoteOriginUrl(): String {
                if (REMOTE_ORIGIN_URL != null) return REMOTE_ORIGIN_URL!!
                var remoteOriginUrl = ""
                ProgressManager.getInstance().executeProcessUnderProgress({
                    remoteOriginUrl = GitOperationManager(holder.project).getRemoteOriginUrl()
                    REMOTE_ORIGIN_URL = remoteOriginUrl
                }, EmptyProgressIndicator())
                return remoteOriginUrl
            }

            private fun getWebLinkReferenceType(link: WebLink<*>): WebLinkReferenceType? {
                if (REFERENCE_TYPE_BY_LINK.containsKey(link.linkInfo.linkPath)) {
                    return REFERENCE_TYPE_BY_LINK[link.linkInfo.linkPath]
                }
                var res: WebLinkReferenceType? = null
                ProgressManager.getInstance().executeProcessUnderProgress({
                    res = link.referenceType
                    if (res != null) REFERENCE_TYPE_BY_LINK[link.linkInfo.linkPath] = res!!
                }, EmptyProgressIndicator())
                return res
            }

            private fun getDescription(originalPath: String) =
                "The link path $originalPath is outdated and requires to be updated"
        }
    }

    internal class QuickLinkFix(link: Link, change: Change, index: Int) : LocalQuickFix {
        private val logger = Logger.getLogger(QuickLinkFix::class::simpleName.name)
        private val myToFixLink = link
        private val myFixingChange = change
        private val myIndex = index

        override fun getFamilyName(): String {
            if (myFixingChange.isChangeDelete()) {
                return "The linked resource has been deleted. Link can be removed"
            }
            val newPath = LinkUpdaterService.getNewLinkPath(myToFixLink, myFixingChange, myIndex)
            return "Update link to $newPath"
        }

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            try {
                LinkUpdaterService.getInstance(project).updateSingleLink(myToFixLink, myFixingChange, myIndex)
            } catch (e: VcsException) {
                logger.log(Level.WARNING, e.message)
            }
        }
    }
}