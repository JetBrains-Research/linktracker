package org.intellij.plugin.tracker.integration

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.vcs.VcsRoot
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import org.intellij.plugin.tracker.LinkTrackerAction
import org.intellij.plugin.tracker.data.changes.ChangeType
import org.intellij.plugin.tracker.data.changes.FileChange
import org.intellij.plugin.tracker.data.changes.FileChangeType
import org.intellij.plugin.tracker.data.diff.FileHistory
import org.intellij.plugin.tracker.data.links.RelativeLinkToDirectory
import org.intellij.plugin.tracker.data.links.RelativeLinkToFile
import org.intellij.plugin.tracker.data.links.WebLinkToLine
import org.intellij.plugin.tracker.services.*
import org.intellij.plugin.tracker.utils.GitOperationManager
import org.junit.jupiter.api.*
import org.mockito.Mockito.mock

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
    private lateinit var myDataParsingTask: LinkTrackerAction.DataParsingTask
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
        myUiService = UIService.getInstance(project)
        myDataParsingTask = LinkTrackerAction.DataParsingTask(
            currentProject = project,
            linkService = myLinkService,
            historyService = myHistoryService,
            gitOperationManager = myGitOperationManager,
            linkUpdateService = myLinkUpdateService,
            changeTrackerService = myChangeTrackerService,
            uiService = myUiService,
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
        val links = myDataParsingTask.getLinks()

        val pair = links.first { pair -> pair.first.linkInfo.linkText == "single - relative link to file" }
        val link = pair.first
        val change = pair.second
        Assertions.assertTrue(link is RelativeLinkToFile)
        Assertions.assertEquals("file.txt", link.linkInfo.linkPath)
        Assertions.assertEquals("/src/testParseRelativeLinks.md", link.linkInfo.proveniencePath)
        Assertions.assertEquals(FileChangeType.MOVED, change.changes[0])
        Assertions.assertEquals(mutableListOf(afterPath), change.afterPath)
    }

    @Disabled
    @Test
    fun parseRelativeLinkToDirectory() {

        val afterPath = "main"
        val fileChange = FileChange(fileChangeType = FileChangeType.ADDED, afterPathString = afterPath)

        every { myGitOperationManager.getContentsOfLineInFileAtCommit(any(), any(), any()) } returns ""
        every { myGitOperationManager.getDiffWithWorkingTree(any()) } returns mutableListOf()
        every { myGitOperationManager.getAllChangesForFile(any(), any(), any(), any()) } returns fileChange
        every { myGitOperationManager.checkWorkingTreeChanges(any()) } returns fileChange

        ProgressManager.getInstance().run(myDataParsingTask)
        val links = myDataParsingTask.getLinks()

        val pair = links.first { pair -> pair.first.linkInfo.linkText == "single - relative link to directory" }
        val link = pair.first
        val change = pair.second
        Assertions.assertTrue(link is RelativeLinkToDirectory)
        Assertions.assertEquals("main", link.linkInfo.linkPath)
        Assertions.assertEquals("/src/testParseRelativeLinks.md", link.linkInfo.proveniencePath)
        Assertions.assertEquals(FileChangeType.ADDED, change.changes[0])
        Assertions.assertEquals(afterPath, change.afterPath)
    }

    @Disabled
    @Test
    fun parseWebLinkToLine() {

        ProgressManager.getInstance().run(myDataParsingTask)
        val links = myDataParsingTask.getLinks()

        val pair = links.first { pair -> pair.first.linkInfo.linkText == "single - web link to line" }

        every { anyConstructed<GitOperationManager>().getContentsOfLineInFileAtCommit(any(), any(), any()) } returns ""

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

        ProgressManager.getInstance().run(myDataParsingTask)
        val links = myDataParsingTask.getLinks()

        every {
            anyConstructed<GitOperationManager>().getAllChangesForFile(
                any(),
                any(),
                any(),
                any()
            )
        } returns mock()
        every { anyConstructed<GitOperationManager>().checkWorkingTreeChanges(any()) } returns null
        every { anyConstructed<GitOperationManager>().getDiffWithWorkingTree(any()) } returns mutableListOf()
        every { anyConstructed<GitOperationManager>().getContentsOfLineInFileAtCommit(any(), any(), any()) } returns ""

        val multiLinks = links.filter { pair -> pair.first.linkInfo.fileName == "testParseMultipleLinks.md" }
        Assertions.assertEquals(3, multiLinks.size)
    }
}