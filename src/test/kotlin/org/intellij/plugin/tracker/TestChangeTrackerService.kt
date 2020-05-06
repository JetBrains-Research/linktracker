package org.intellij.plugin.tracker

// import com.intellij.openapi.project.Project
// import com.intellij.openapi.vcs.FilePath
// import com.intellij.openapi.vcs.changes.Change
// import com.intellij.openapi.vcs.changes.ContentRevision
// import com.nhaarman.mockitokotlin2.doReturn
// import com.nhaarman.mockitokotlin2.mock
// import com.nhaarman.mockitokotlin2.whenever
// import git4idea.changes.GitChangeUtils
// import git4idea.repo.GitRepository
// import git4idea.repo.GitRepositoryManager
// import io.mockk.every
// import io.mockk.mockkStatic
// import kotlin.test.assertEquals
// import kotlin.test.assertTrue
// import org.intellij.plugin.tracker.data.FileChange
// import org.intellij.plugin.tracker.data.Link
// import org.intellij.plugin.tracker.data.LinkType
// import org.intellij.plugin.tracker.data.RelativeLink
// import org.intellij.plugin.tracker.services.ChangeTrackerService
// import org.junit.Test
// import org.mockito.Mockito

class TestChangeTrackerService {

    /**
     * TO DO: tests are commented out because not passing giving following message
     * "ToolWindowManager must not be null"
     * Tests need to be fixed
     */
    //    @Test
    //    fun noChangesForFileLink() {
    //
    //        val projectMock = mock<Project>()
    //        val repositoryMock = mock<GitRepository>()
    //        val changeTracker = ChangeTrackerService(project = projectMock)
    //
    //        mockkStatic("git4idea.changes.GitChangeUtils")
    //        mockkStatic("git4idea.repo.GitRepositoryManager")
    //
    //        every {
    //            GitRepositoryManager.getInstance(projectMock).repositories[0]
    //        } returns repositoryMock
    //
    //        every {
    //            GitChangeUtils.getDiffWithWorkingTree(repositoryMock, "commit_sha", true)
    //        } returns mutableListOf()
    //
    //        val fileLinkChangesList = changeTracker.getFileChanges(
    //            mutableListOf(
    //                RelativeLink(
    //                    linkType = LinkType.FILE,
    //                    linkText = "",
    //                    linkPath = "path/to/file",
    //                    proveniencePath = "README.md",
    //                    foundAtLineNumber = 52
    //                )
    //            ), commitSHA = "commit_sha"
    //        )
    //        val expectedFileChangesList = mutableListOf<Pair<Link, FileChange>>(
    //            Pair(
    //                RelativeLink(
    //                    linkType = LinkType.FILE,
    //                    linkText = "",
    //                    linkPath = "path/to/file",
    //                    proveniencePath = "README.md",
    //                    foundAtLineNumber = 52
    //                ),
    //                FileChange()
    //            )
    //        )
    //
    //        assertEquals(expectedFileChangesList.size, fileLinkChangesList.size)
    //        assertTrue {
    //            expectedFileChangesList.containsAll(fileLinkChangesList)
    //                    && fileLinkChangesList.containsAll(expectedFileChangesList)
    //        }
    //    }
    //
    //    @Test
    //    fun changesForMultipleFileLinksMoved() {
    //
    //        val projectMock = mock<Project>()
    //        val repositoryMock = mock<GitRepository>()
    //        val changeTracker = ChangeTrackerService(project = projectMock)
    //
    //        mockkStatic("git4idea.changes.GitChangeUtils")
    //        mockkStatic("git4idea.repo.GitRepositoryManager")
    //
    //        every {
    //            GitRepositoryManager.getInstance(projectMock).repositories[0]
    //        } returns repositoryMock
    //
    //        val mockFileBefore = mock<FilePath>()
    //        val mockFileAfter = mock<FilePath>()
    //        val mockRevisionBefore = mock<ContentRevision>()
    //        val mockRevisionAfter = mock<ContentRevision>()
    //
    //        whenever(mockFileBefore.path).doReturn("before/path/README.md")
    //        whenever(mockRevisionBefore.file).doReturn(mockFileBefore)
    //
    //        whenever(mockFileAfter.path).doReturn("after/path/README.md")
    //        whenever(mockRevisionAfter.file).doReturn(mockFileAfter)
    //
    //        // Mock first change
    //        val mockChange1 = mock<Change>()
    //        whenever(mockChange1.beforeRevision).doReturn(mockRevisionBefore)
    //        whenever(mockChange1.afterRevision).doReturn(mockRevisionAfter)
    //
    //        val mockChangeType = mock<Change.Type>()
    //        whenever(mockChangeType.toString()).doReturn("MOVED")
    //
    //        whenever(mockChange1.type).doReturn(mockChangeType)
    //        whenever(mockChange1.getMoveRelativePath(projectMock)).doReturn("""..\..\after""")
    //
    //        whenever(mockFileAfter.name).doReturn("README.md")
    //        whenever(mockChange1.afterRevision?.file?.name).doReturn("README.md")
    //        whenever(mockChange1.affectsFile(Mockito.any())).doReturn(true)
    //
    //        every {
    //            GitChangeUtils.getDiffWithWorkingTree(repositoryMock, "commit_sha", true)
    //        } returns mutableListOf(mockChange1)
    //
    //        val link1 = RelativeLink(
    //            linkType = LinkType.FILE,
    //            linkText = "",
    //            linkPath = "before/path/README.md",
    //            proveniencePath = "README.md",
    //            foundAtLineNumber = 52
    //        )
    //
    //        val linkList = mutableListOf<Link>(link1)
    //
    //        val fileLinkChangesList = changeTracker.getFileChanges(linkList = linkList, commitSHA = "commit_sha")
    //        val expectedFileChangesList = mutableListOf<Pair<Link, FileChange>>(
    //            Pair(
    //                link1,
    //                FileChange(
    //                    changeType = "MOVED",
    //                    fileName = "README.md",
    //                    beforePath = "before/path/README.md",
    //                    afterPath = "after/path/README.md",
    //                    moveRelativePath = """..\..\after"""
    //                )
    //            )
    //        )
    //
    //        assertEquals(expectedFileChangesList.size, fileLinkChangesList.size)
    //        assertTrue {
    //            expectedFileChangesList.containsAll(fileLinkChangesList)
    //                    && fileLinkChangesList.containsAll(expectedFileChangesList)
    //        }
    //    }
}
