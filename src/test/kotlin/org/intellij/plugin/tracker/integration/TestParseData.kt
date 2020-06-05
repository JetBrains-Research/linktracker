package org.intellij.plugin.tracker.integration

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vcs.FilePath
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
import org.intellij.plugin.tracker.data.ScanResult
import org.intellij.plugin.tracker.data.links.*
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
    private lateinit var retrievedLinks: List<Link>

    @Override
    override fun getTestDataPath(): String {
        return "src/test/kotlin/org/intellij/plugin/tracker/integration/testdata"
    }

    @BeforeAll
    override fun setUp() {
        super.setUp()
        myFixture.configureByFiles(*myFiles)
        setupGitManager()
        performMocks()
        retrievedLinks = runDataParsingTask()
    }

    @AfterAll
    override fun tearDown() {
        super.tearDown()
    }

    fun performMocks() {
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
        every { anyConstructed<GitOperationManager>().getRemoteOriginUrl() } returns
                "https://github.com/tudorpopovici1/demo-plugin-jetbrains-project.git"
        every { anyConstructed<GitOperationManager>().checkWorkingTreeChanges(any()) } returns null
        every { anyConstructed<GitOperationManager>().getDiffWithWorkingTree(any()) } returns mutableListOf()
        every { anyConstructed<GitOperationManager>().getDirectoryCommits(any()) } returns mutableListOf(
                mutableListOf("main"), mutableListOf<String>(), mutableMapOf<String, String>())
        every { anyConstructed<GitOperationManager>().getMoveCommits(any(), any()) } returns ""
        every { anyConstructed<GitOperationManager>().getContentsOfLineInFileAtCommit(any(), any(), any()) } returns ""
        every { anyConstructed<GitOperationManager>().getContentsOfLinesInFileAtCommit(any(), any(), any(), any()) } returns mock()
        every {
            anyConstructed<GitOperationManager>().getAllChangesForFile(
                any(),
                any(),
                any(),
                any()
            )
        } returns mockk(relaxed = true)
    }

    private fun runDataParsingTask(): List<Link> {
        ProgressManager.getInstance().run(myDataParsingTask)
        val result: ScanResult = myDataParsingTask.getResult()
        return result.myLinkChanges.map { pair -> pair.first }
    }

    private fun getLinkWithText(linkText: String) = retrievedLinks.first { link -> link.linkInfo.linkText == linkText }

    @Test
    fun parseRelativeLinkToFile() {
        val expectedLink = RelativeLinkToFile(
            linkInfo = LinkInfo(
                linkText = "single - relative link to file",
                linkPath = "file.txt",
                proveniencePath = "/src/testParseRelativeLinks.md",
                foundAtLineNumber = 1,
                textOffset = 33,
                fileName = "testParseRelativeLinks.md",
                project = ProjectManager.getInstance().openProjects[0]
            )
        )
        ProgressManager.getInstance().run(myDataParsingTask)
        val result: ScanResult = myDataParsingTask.getResult()
        val links = result.myLinkChanges

        val pair = links.first { pair -> pair.first.linkInfo.linkText == "single - relative link to file" }
        val resultedLink: Link = pair.first
        Assertions.assertEquals(expectedLink, resultedLink)
    }

    @Test
    fun parseRelativeLinkToDirectory() {
        val expectedLink = RelativeLinkToDirectory(
            linkInfo = LinkInfo(
                linkText = "single - relative link to directory",
                linkPath = "main",
                proveniencePath = "/src/testParseRelativeLinks.md",
                foundAtLineNumber = 3,
                textOffset = 127,
                fileName = "testParseRelativeLinks.md",
                project = ProjectManager.getInstance().openProjects[0]
            ),
            commitSHA = "edbb2f5"
        )
        val resultedLink: Link = getLinkWithText(linkText = "single - relative link to directory")
        Assertions.assertEquals(expectedLink, resultedLink)
    }

    @Test
    fun parseRelativeLinkToLine() {
        val expectedLink = RelativeLinkToLine(
            linkInfo = LinkInfo(
                linkText = "single - relative link to line",
                linkPath = "file.txt#L1",
                proveniencePath = "/src/testParseRelativeLinks.md",
                foundAtLineNumber = 2,
                textOffset = 76,
                fileName = "testParseRelativeLinks.md",
                project = ProjectManager.getInstance().openProjects[0]
            )
        )
        val resultedLink: Link = getLinkWithText(linkText = "single - relative link to line")
        Assertions.assertEquals(expectedLink, resultedLink)
    }

    @Test
    fun parseWebLinkToFile() {
        val expectedLink = WebLinkToFile(
            linkInfo = LinkInfo(
                linkText = "single - web link to file",
                linkPath = "https://github.com/tudorpopovici1/demo-plugin-jetbrains-project/blob/cf925c192b45c9310a2dcc874573f393024f3be2/src/main/java/actions/MarkdownAction.java",
                proveniencePath = "/src/testParseWebLink.md",
                foundAtLineNumber = 2,
                textOffset = 213,
                fileName = "testParseWebLink.md",
                project = ProjectManager.getInstance().openProjects[0]
            )
        )
        val resultedLink: Link = getLinkWithText(linkText = "single - web link to file")
        Assertions.assertEquals(expectedLink, resultedLink)
    }

    @Test
    fun parseWebLinkToLine() {
        val expectedLink = WebLinkToLine(
            linkInfo = LinkInfo(
                linkText = "single - web link to line",
                linkPath = "https://github.com/tudorpopovici1/demo-plugin-jetbrains-project/blob/cf925c192b45c9310a2dcc874573f393024f3be2/src/main/java/actions/MarkdownAction.java#L55",
                proveniencePath = "/src/testParseWebLink.md",
                foundAtLineNumber = 1,
                textOffset = 28,
                fileName = "testParseWebLink.md",
                project = ProjectManager.getInstance().openProjects[0]
            )
        )
        val resultedLink: Link = getLinkWithText(linkText = "single - web link to line")
        Assertions.assertEquals(expectedLink, resultedLink)
    }

    @Test
    fun parseMultipleLinks() {
        ProgressManager.getInstance().run(myDataParsingTask)
        val multiLinks = retrievedLinks.filter { link -> link.linkInfo.fileName == "testParseMultipleLinks.md" }
        Assertions.assertEquals(3, multiLinks.size)
    }
}