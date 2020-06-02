package org.intellij.plugin.tracker.integration

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.changes.Change
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import org.intellij.plugin.tracker.data.changes.DirectoryChange
import org.intellij.plugin.tracker.data.changes.FileChange
import org.intellij.plugin.tracker.data.changes.FileChangeType
import org.intellij.plugin.tracker.data.diff.FileHistory
import org.intellij.plugin.tracker.data.links.RelativeLinkToDirectory
import org.intellij.plugin.tracker.data.links.RelativeLinkToFile
import org.intellij.plugin.tracker.data.links.WebLinkToLine
import org.intellij.plugin.tracker.services.*
import org.intellij.plugin.tracker.utils.DataParsingTask
import org.intellij.plugin.tracker.utils.GitOperationManager
import org.junit.jupiter.api.*

/**
 * This class tests the parsing of links and changes.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestParseData : BasePlatformTestCase() {

    private lateinit var myGitOperationManager: GitOperationManager
    private lateinit var myHistoryService: HistoryService
    private lateinit var myLinkService: LinkRetrieverService
    private lateinit var myLinkUpdateService: LinkUpdaterService
    private lateinit var myChangeTrackerService: ChangeTrackerService
    private lateinit var myUiService: UIService
    private lateinit var myDataParsingTask: DataParsingTask
    private val myFiles = arrayOf(
        "testParseRelativeLinks.md",
        "main/file.txt",
        "testParseWebLink.md",
        "testParseMultipleLinks.md"
    )

    @Override
    override fun getTestDataPath(): String {
        return "src/test/kotlin/org/intellij/plugin/tracker/integration/testdata"
    }

    @BeforeAll
    override fun setUp() {
        super.setUp()
        myFixture.configureByFiles(*myFiles)
        setupGitManager()
    }

    @AfterAll
    override fun tearDown() {
        super.tearDown()
    }

    @BeforeEach
    fun init() {
        val repositoryMock: GitRepository = mock()
        whenever(repositoryMock.root).doReturn(mock())
        mockkStatic("git4idea.repo.GitRepositoryManager")
        every { GitRepositoryManager.getInstance(project).repositories } returns mutableListOf(repositoryMock)

        myGitOperationManager = GitOperationManager(project)
        myHistoryService = HistoryService.getInstance(project)
        myLinkService = LinkRetrieverService.getInstance(project)
        myLinkUpdateService = LinkUpdaterService.getInstance(project)
        myChangeTrackerService = ChangeTrackerServiceImpl.getInstance(project)
        myUiService = mockk(relaxed = true)
        myDataParsingTask = DataParsingTask(
            currentProject = project,
            myLinkService = myLinkService,
            myHistoryService = myHistoryService,
            myGitOperationManager = myGitOperationManager,
            myLinkUpdateService = myLinkUpdateService,
            myChangeTrackerService = myChangeTrackerService,
            myUiService = myUiService,
            dryRun = true
        )
    }

    private fun setupGitManager() {
        mockkConstructor(GitOperationManager::class)
        every { anyConstructed<GitOperationManager>().isRefABranch(any()) } returns false
        every { anyConstructed<GitOperationManager>().isRefATag(any()) } returns false
        every { anyConstructed<GitOperationManager>().isRefACommit(any()) } returns false
        every { anyConstructed<GitOperationManager>().getHeadCommitSHA() } returns "edbb2f5"
        every { anyConstructed<GitOperationManager>().getStartCommit(any()) } returns "edbb2f5"
        every { anyConstructed<GitOperationManager>().getRemoteOriginUrl() } returns "github.com/owner/project/src.git"
    }

    @Test
    fun parseRelativeLinkToFile() {

        val afterPath = "src/main/file.txt"
        val gitFileChanges = FileChange(
            fileChangeType = FileChangeType.MOVED,
            afterPathString = afterPath,
            fileHistoryList = mutableListOf(FileHistory("Commit: edbb2f5", "file.txt"))
        )

        every {
            anyConstructed<GitOperationManager>().getAllChangesForFile(
                any(),
                any(),
                any(),
                any()
            )
        } returns gitFileChanges
        every { anyConstructed<GitOperationManager>().checkWorkingTreeChanges(any()) } returns null
        every { anyConstructed<GitOperationManager>().getDiffWithWorkingTree(any()) } returns mutableListOf()
        every { anyConstructed<GitOperationManager>().getContentsOfLineInFileAtCommit(any(), any(), any()) } returns ""

        ProgressManager.getInstance().run(myDataParsingTask)
        val result = myDataParsingTask.getResult()
        val links = result.myLinkChanges

        val pair = links.first { pair -> pair.first.linkInfo.linkText == "single - relative link to file" }
        val link = pair.first
        val change = pair.second
        Assertions.assertTrue(link is RelativeLinkToFile)
        Assertions.assertEquals("file.txt", link.linkInfo.linkPath)
        Assertions.assertEquals("/src/testParseRelativeLinks.md", link.linkInfo.proveniencePath)
        Assertions.assertEquals(FileChangeType.MOVED, change.changes[0])
        Assertions.assertEquals(mutableListOf(afterPath), change.afterPath)
    }

    @Test
    fun parseRelativeLinkToDirectory() {

        val beforePath = mockk<FilePath>()
        val afterPath = mockk<FilePath>()
        every { beforePath.getPath() } returns "."
        every { afterPath.getPath() } returns "main"

        val change = mockk<Change>(relaxed = true)
        every { change.beforeRevision?.file?.parentPath } returns null
        every { change.afterRevision?.file?.parentPath } returns afterPath
        every { change.afterRevision?.file?.parentPath?.name } returns "main"
        every { change.afterRevision?.file?.parentPath?.toString() } returns "main"
        every { change.type } returns Change.Type.NEW

        every { anyConstructed<GitOperationManager>().getContentsOfLineInFileAtCommit(any(), any(), any()) } returns ""
        every { anyConstructed<GitOperationManager>().getDiffWithWorkingTree(any()) } returns mutableListOf(change)
        every { anyConstructed<GitOperationManager>().checkWorkingTreeChanges(any()) } returns null
        every { anyConstructed<GitOperationManager>().getAllChangesForFile(any(), any(), any(), any()) } returns mockk(
            relaxed = true
        )

        ProgressManager.getInstance().run(myDataParsingTask)
        val result = myDataParsingTask.getResult()
        val links = result.myLinkChanges

        val pair = links.first { pair -> pair.first.linkInfo.linkText == "single - relative link to directory" }
        val link = pair.first
        val dirChange = pair.second as DirectoryChange
        Assertions.assertTrue(link is RelativeLinkToDirectory)
        Assertions.assertEquals("main", link.linkInfo.linkPath)
        Assertions.assertEquals("/src/testParseRelativeLinks.md", link.linkInfo.proveniencePath)
        Assertions.assertEquals(FileChangeType.ADDED, dirChange.changes[0])
        Assertions.assertEquals(afterPath.path, dirChange.afterPathString)
    }

    @Disabled
    @Test
    fun parseWebLinkToLine() {

        every { anyConstructed<GitOperationManager>().getContentsOfLineInFileAtCommit(any(), any(), any()) } returns ""
        every { anyConstructed<GitOperationManager>().checkWorkingTreeChanges(any()) } returns null
        every { anyConstructed<GitOperationManager>().getDiffWithWorkingTree(any()) } returns mutableListOf()
        every { anyConstructed<GitOperationManager>().getAllChangesForFile(any(), any(), any(), any()) } returns mockk(
            relaxed = true
        )

        ProgressManager.getInstance().run(myDataParsingTask)
        val result = myDataParsingTask.getResult()
        val links = result.myLinkChanges

        val pair = links.first { pair -> pair.first.linkInfo.linkText == "single - web link to line" }

        val link = pair.first
        Assertions.assertTrue(link is WebLinkToLine)
        Assertions.assertEquals(
            "https://github.com/tudorpopovici1/demo-plugin-jetbrains-project/blob/cf925c192b45c9310a2dcc874573f393024f3be2/src/main/java/actions/MarkdownAction.java#L55",
            link.linkInfo.linkPath
        )
        Assertions.assertEquals("/src/testParseWebLink.md", link.linkInfo.proveniencePath)
    }

    @Disabled
    @Test
    fun parseMultipleLinks() {

        every { anyConstructed<GitOperationManager>().checkWorkingTreeChanges(any()) } returns null
        every { anyConstructed<GitOperationManager>().getDiffWithWorkingTree(any()) } returns mutableListOf()
        every { anyConstructed<GitOperationManager>().getContentsOfLineInFileAtCommit(any(), any(), any()) } returns ""
        every {
            anyConstructed<GitOperationManager>().getAllChangesForFile(
                any(),
                any(),
                any(),
                any()
            )
        } returns mockk(relaxed = true)

        ProgressManager.getInstance().run(myDataParsingTask)
        val result = myDataParsingTask.getResult()
        val links = result.myLinkChanges

        val multiLinks = links.filter { pair -> pair.first.linkInfo.fileName == "testParseMultipleLinks.md" }
        Assertions.assertEquals(3, multiLinks.size)
    }
}