package org.intellij.plugin.tracker.integration

import com.intellij.openapi.progress.ProgressManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.mockk.every
import io.mockk.mockkConstructor
import org.intellij.plugin.tracker.data.changes.ChangeType
import org.intellij.plugin.tracker.data.changes.LinkChange
import org.intellij.plugin.tracker.data.links.RelativeLinkToDirectory
import org.intellij.plugin.tracker.data.links.RelativeLinkToFile
import org.intellij.plugin.tracker.data.links.WebLinkToLine
import org.intellij.plugin.tracker.services.HistoryService
import org.intellij.plugin.tracker.services.LinkRetrieverService
import org.intellij.plugin.tracker.services.LinkUpdaterService
import org.intellij.plugin.tracker.services.UIService
import org.intellij.plugin.tracker.utils.DataParsingTask
import org.intellij.plugin.tracker.utils.GitOperationManager
import org.junit.jupiter.api.*

/**
 * @author Tommaso Brandirali
 *
 * This class tests the parsing of links and changes.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestParseData : BasePlatformTestCase() {

    private lateinit var myGitOperationManager: GitOperationManager
    private lateinit var myHistoryService: HistoryService
    private lateinit var myLinkService: LinkRetrieverService
    private lateinit var myLinkUpdateService: LinkUpdaterService
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
        myGitOperationManager = GitOperationManager(project)
        myHistoryService = HistoryService.getInstance(project)
        myLinkService = LinkRetrieverService.getInstance(project)
        myLinkUpdateService = LinkUpdaterService.getInstance(project)
        myUiService = UIService.getInstance(project)
        myDataParsingTask = DataParsingTask(
            currentProject = project,
            myLinkService = myLinkService,
            myHistoryService = myHistoryService,
            myGitOperationManager = myGitOperationManager,
            myLinkUpdateService = myLinkUpdateService,
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
    }

    @Test
    fun parseRelativeLinkToFile() {

        val afterPath = "src/main/file.txt"
        val gitFileChanges = Pair(
            mutableListOf(Pair("Commit: edbb2f5", "file.txt")),
            LinkChange(changeType = ChangeType.MOVED, afterPath = afterPath)
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
        every { anyConstructed<GitOperationManager>().getRemoteOriginUrl() } returns ""

        ProgressManager.getInstance().run(myDataParsingTask)
        val result = myDataParsingTask.getResult()
        val links = result.myLinkChanges

        val pair = links.first { it.first.linkInfo.linkText == "single - relative link to file" }
        val link = pair.first
        val change = pair.second
        Assertions.assertTrue(link is RelativeLinkToFile)
        Assertions.assertEquals("file.txt", link.linkInfo.linkPath)
        Assertions.assertEquals("/src/testParseRelativeLinks.md", link.linkInfo.proveniencePath)
        Assertions.assertEquals(ChangeType.MOVED, change.changeType)
        Assertions.assertEquals(afterPath, change.afterPath)
    }

    @Test
    fun parseRelativeLinkToDirectory() {

        val afterPath = "main"
        val linkChange = LinkChange(changeType = ChangeType.ADDED, afterPath = afterPath)
        val gitFileChanges = Pair(
            mutableListOf(Pair("Working tree", "file.txt")),
            linkChange
        )

        every { myGitOperationManager.getAllChangesForFile(any(), any(), any(), any()) } returns gitFileChanges
        every { myGitOperationManager.checkWorkingTreeChanges(any()) } returns linkChange


        ProgressManager.getInstance().run(myDataParsingTask)
        val result = myDataParsingTask.getResult()
        val links = result.myLinkChanges

        val pair = links.first { it.first.linkInfo.linkText == "single - relative link to directory" }
        val link = pair.first
        val change = pair.second
        Assertions.assertTrue(link is RelativeLinkToDirectory)
        Assertions.assertEquals("main", link.linkInfo.linkPath)
        Assertions.assertEquals("/src/testParseRelativeLinks.md", link.linkInfo.proveniencePath)
        //Assertions.assertEquals(ChangeType.ADDED, change.changeType)
        Assertions.assertEquals(afterPath, change.afterPath)
    }

    @Test
    fun parseWebLinkToLine() {


        ProgressManager.getInstance().run(myDataParsingTask)
        val result = myDataParsingTask.getResult()
        val links = result.myLinkChanges

        val pair = links.first { it.first.linkInfo.linkText == "single - web link to line" }
        val link = pair.first
        Assertions.assertTrue(link is WebLinkToLine)
        Assertions.assertEquals(
            "https://github.com/tudorpopovici1/demo-plugin-jetbrains-project/blob/cf925c192b45c9310a2dcc874573f393024f3be2/src/main/java/actions/MarkdownAction.java#L55",
            link.linkInfo.linkPath
        )
        Assertions.assertEquals("/src/testParseWebLink.md", link.linkInfo.proveniencePath)
    }

    @Test
    fun parseMultipleLinks() {

        ProgressManager.getInstance().run(myDataParsingTask)
        val result = myDataParsingTask.getResult()
        val links = result.myLinkChanges

        val multiLinks = links.filter { it.first.linkInfo.fileName == "testParseMultipleLinks.md" }
        Assertions.assertEquals(3, multiLinks.size)
    }
}