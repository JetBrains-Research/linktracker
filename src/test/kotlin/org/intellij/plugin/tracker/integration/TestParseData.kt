package org.intellij.plugin.tracker.integration

import com.intellij.openapi.progress.ProgressManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.intellij.plugin.tracker.LinkTrackerAction
import org.intellij.plugin.tracker.data.changes.ChangeType
import org.intellij.plugin.tracker.data.changes.LinkChange
import org.intellij.plugin.tracker.data.links.RelativeLinkToFile
import org.intellij.plugin.tracker.data.links.WebLinkToLine
import org.intellij.plugin.tracker.services.*
import org.intellij.plugin.tracker.utils.GitOperationManager
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.Assertions

/**
 * This class tests the parsing of links and changes.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestParseData: BasePlatformTestCase() {

    @MockK
    private lateinit var gitOperationManager: GitOperationManager
    private lateinit var historyService: HistoryService
    private lateinit var linkService: LinkRetrieverService
    private lateinit var linkUpdateService: LinkUpdaterService
    private lateinit var uiService: UIService
    private lateinit var dataParsingTask: LinkTrackerAction.DataParsingTask
    private val files = arrayOf(
        "testParseRelativeLink.md",
        "main/file.txt",
        "testParseWebLink.md"
    )

    @Override
    override fun getTestDataPath(): String {
        return "src/test/kotlin/org/intellij/plugin/tracker/integration/testdata"
    }

    @BeforeAll
    override fun setUp() {
        super.setUp()
        myFixture.configureByFiles(*files)
        MockKAnnotations.init(this)
    }

    @BeforeEach
    fun init() {
        historyService = HistoryService.getInstance(project)
        linkService = LinkRetrieverService.getInstance(project)
        linkUpdateService = LinkUpdaterService.getInstance(project)
        uiService = UIService.getInstance(project)
        dataParsingTask = LinkTrackerAction.DataParsingTask(
            currentProject = project,
            linkService = linkService,
            historyService = historyService,
            gitOperationManager = gitOperationManager,
            linkUpdateService = linkUpdateService,
            uiService = uiService,
            dryRun = true
        )
    }

    private fun setupGitManagerForRelativeLinks() {
        every { gitOperationManager.isRefABranch(any()) } returns false
        every { gitOperationManager.isRefATag(any()) } returns false
        every { gitOperationManager.isRefACommit(any()) } returns false
        every { gitOperationManager.getHeadCommitSHA() } returns "edbb2f5"
        every { gitOperationManager.getStartCommit(any()) } returns "edbb2f5"
        ChangeTrackerService.getInstance(project).injectGitOperationManager(gitOperationManager)
    }

    @Test
    fun parseRelativeLinkCommittedMoved() {

        setupGitManagerForRelativeLinks()
        val gitFileChanges = Pair(
            mutableListOf(Pair("Commit: edbb2f5", "file.txt")),
            LinkChange(changeType = ChangeType.MOVED, afterPath = "src/main/file.txt")
        )

        every { gitOperationManager.getAllChangesForFile(any(), any(), any(), any()) } returns gitFileChanges
        every { gitOperationManager.checkWorkingTreeChanges(any()) } returns null

        ProgressManager.getInstance().run(dataParsingTask)
        val links = dataParsingTask.getLinks()

        val link = links.first{pair -> pair.first.linkInfo.linkText == "a relative link to a file"}.first
        Assertions.assertTrue(link is RelativeLinkToFile)
        Assertions.assertEquals("file.txt", link.linkInfo.linkPath)
        Assertions.assertEquals("/src/testParseRelativeLink.md", link.linkInfo.proveniencePath)
    }

    @Test
    fun parseRelativeLinkUncommittedAdded() {

        setupGitManagerForRelativeLinks()
        val linkChange = LinkChange(changeType = ChangeType.ADDED, afterPath = "src/main/file.txt")
        val gitFileChanges = Pair(
            mutableListOf(Pair("Working tree", "file.txt")),
            linkChange
        )

        every { gitOperationManager.getAllChangesForFile(any(), any(), any(), any()) } returns gitFileChanges
        every { gitOperationManager.checkWorkingTreeChanges(any()) } returns linkChange

        ProgressManager.getInstance().run(dataParsingTask)
        val links = dataParsingTask.getLinks()

        val link = links.first{pair -> pair.first.linkInfo.linkText == "a relative link to a file"}.first
        Assertions.assertTrue(link is RelativeLinkToFile)
        Assertions.assertEquals("file.txt", link.linkInfo.linkPath)
        Assertions.assertEquals("/src/testParseRelativeLink.md", link.linkInfo.proveniencePath)
    }

    @Test
    fun parseWebLinkToLine() {

        setupGitManagerForRelativeLinks()
        ProgressManager.getInstance().run(dataParsingTask)
        val links = dataParsingTask.getLinks()

        val link = links.first{pair -> pair.first.linkInfo.linkText == "a web link to a line"}.first
        Assertions.assertTrue(link is WebLinkToLine)
        Assertions.assertEquals("https://github.com/tudorpopovici1/demo-plugin-jetbrains-project/blob/cf925c192b45c9310a2dcc874573f393024f3be2/src/main/java/actions/MarkdownAction.java#L55", link.linkInfo.linkPath)
        Assertions.assertEquals("/src/testParseWebLink.md", link.linkInfo.proveniencePath)
    }
}