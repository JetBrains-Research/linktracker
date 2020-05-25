package org.intellij.plugin.tracker.integration

import com.intellij.openapi.progress.ProgressManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.mockk.every
import io.mockk.mockkConstructor
import org.intellij.plugin.tracker.LinkTrackerAction
import org.intellij.plugin.tracker.data.changes.ChangeType
import org.intellij.plugin.tracker.data.changes.LinkChange
import org.intellij.plugin.tracker.data.links.RelativeLinkToDirectory
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

    private lateinit var gitOperationManager: GitOperationManager
    private lateinit var historyService: HistoryService
    private lateinit var linkService: LinkRetrieverService
    private lateinit var linkUpdateService: LinkUpdaterService
    private lateinit var uiService: UIService
    private lateinit var dataParsingTask: LinkTrackerAction.DataParsingTask
    private val files = arrayOf(
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
        myFixture.configureByFiles(*files)
        setupGitManager()
    }

    @BeforeEach
    fun init() {
        gitOperationManager = GitOperationManager(project)
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

        every { anyConstructed<GitOperationManager>().getAllChangesForFile(any(), any(), any(), any()) } returns gitFileChanges
        every { anyConstructed<GitOperationManager>().checkWorkingTreeChanges(any()) } returns null

        ProgressManager.getInstance().run(dataParsingTask)
        val links = dataParsingTask.getLinks()

        val pair = links.first{pair -> pair.first.linkInfo.linkText == "single - relative link to file"}
        val link = pair.first
        val change = pair.second
        Assertions.assertTrue(link is RelativeLinkToFile)
        Assertions.assertEquals("file.txt", link.linkInfo.linkPath)
        Assertions.assertEquals("/src/testParseRelativeLinks.md", link.linkInfo.proveniencePath)
        Assertions.assertEquals(ChangeType.MOVED, change.changeType)
        Assertions.assertEquals(afterPath, change.afterPath)
    }

//    @Test
//    fun parseRelativeLinkToLine() {
//
//        val afterPath = "src/main/file.txt#L1"
//        val gitFileChanges = Pair(
//            mutableListOf(Pair("Commit: edbb2f5", "file.txt")),
//            LinkChange(changeType = ChangeType.MOVED, afterPath = afterPath)
//        )
//
//        every { gitOperationManager.getAllChangesForFile(any(), any(), any(), any()) } returns gitFileChanges
//        every { gitOperationManager.checkWorkingTreeChanges(any()) } returns null
//
//        ProgressManager.getInstance().run(dataParsingTask)
//        val links = dataParsingTask.getLinks()
//
//        val pair = links.first{pair -> pair.first.linkInfo.linkText == "single - relative link to line"}
//        val link = pair.first
//        val change = pair.second
//        Assertions.assertTrue(link is RelativeLinkToLine)
//        Assertions.assertEquals("file.txt", link.linkInfo.linkPath)
//        Assertions.assertEquals("/src/testParseRelativeLinks.md", link.linkInfo.proveniencePath)
//        Assertions.assertEquals(ChangeType.MOVED, change.changeType)
//        Assertions.assertEquals(afterPath, change.afterPath)
//    }

    @Test
    fun parseRelativeLinkToDirectory() {

        val afterPath = "main"
        val linkChange = LinkChange(changeType = ChangeType.ADDED, afterPath = afterPath)
        val gitFileChanges = Pair(
            mutableListOf(Pair("Working tree", "file.txt")),
            linkChange
        )

        every { gitOperationManager.getAllChangesForFile(any(), any(), any(), any()) } returns gitFileChanges
        every { gitOperationManager.checkWorkingTreeChanges(any()) } returns linkChange

        ProgressManager.getInstance().run(dataParsingTask)
        val links = dataParsingTask.getLinks()

        val pair = links.first{pair -> pair.first.linkInfo.linkText == "single - relative link to directory"}
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

        ProgressManager.getInstance().run(dataParsingTask)
        val links = dataParsingTask.getLinks()

        val pair = links.first{pair -> pair.first.linkInfo.linkText == "single - web link to line"}
        val link = pair.first
        Assertions.assertTrue(link is WebLinkToLine)
        Assertions.assertEquals("https://github.com/tudorpopovici1/demo-plugin-jetbrains-project/blob/cf925c192b45c9310a2dcc874573f393024f3be2/src/main/java/actions/MarkdownAction.java#L55", link.linkInfo.linkPath)
        Assertions.assertEquals("/src/testParseWebLink.md", link.linkInfo.proveniencePath)
    }

    @Test
    fun parseMultipleLinks() {

        ProgressManager.getInstance().run(dataParsingTask)
        val links = dataParsingTask.getLinks()

        val multiLinks = links.filter{pair -> pair.first.linkInfo.fileName == "testParseMultipleLinks.md"}
        Assertions.assertEquals(3, multiLinks.size)
    }
}