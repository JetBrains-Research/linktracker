package org.intellij.plugin.tracker.integration

import com.intellij.openapi.progress.ProgressManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import org.intellij.plugin.tracker.LinkTrackerAction
import org.intellij.plugin.tracker.data.changes.ChangeType
import org.intellij.plugin.tracker.data.changes.LinkChange
import org.intellij.plugin.tracker.services.*
import org.intellij.plugin.tracker.utils.GitOperationManager
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito

/**
 * This class tests the parsing of links and changes.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestParseData: BasePlatformTestCase() {

    private var historyService: HistoryService? = null
    private var linkService: LinkRetrieverService? = null
    private var linkUpdateService: LinkUpdaterService? = null
    private var uiService: UIService? = null
    private var gitOperationManager: GitOperationManager? = null

    @Override
    override fun getTestDataPath(): String {
        return "src/test/kotlin/org/intellij/plugin/tracker/integration/testdata"
    }

    @BeforeAll
    override fun setUp() {
        super.setUp()
        val files = arrayOf(
            "TEST.md",
            "src/file.txt"
        )
        myFixture.configureByFiles(*files)
    }

    @BeforeEach
    fun init() {
        gitOperationManager = Mockito.mock(GitOperationManager::class.java)
        historyService = HistoryService.getInstance(project)
        linkService = LinkRetrieverService.getInstance(project)
        linkUpdateService = LinkUpdaterService.getInstance(project)
        uiService = UIService.getInstance(project)
        Mockito.`when`(gitOperationManager!!.checkWorkingTreeChanges(any())).thenReturn(null)
    }

    fun setupGitManagerForRelativeLinks() {
        Mockito.`when`(gitOperationManager!!.isRefABranch(any())).thenReturn(false)
        Mockito.`when`(gitOperationManager!!.isRefATag(any())).thenReturn(false)
        Mockito.`when`(gitOperationManager!!.isRefACommit(any())).thenReturn(false)
        Mockito.`when`(gitOperationManager!!.getHeadCommitSHA()).thenReturn("edbb2f5")
        Mockito.`when`(gitOperationManager!!.getStartCommit(any())).thenReturn("edbb2f5")
    }

    @Test
    fun testParseLinks() {
        setupGitManagerForRelativeLinks()
        val gitFileChanges = Pair(
            mutableListOf(Pair("Commit: edbb2f5", "file.txt")),
            LinkChange(changeType = ChangeType.MOVED, afterPath = "src/file.txt")
        )
        Mockito.`when`(gitOperationManager!!.getAllChangesForFile(anyOrNull(), anyInt(), anyString(), anyString())).thenReturn(gitFileChanges)
        // Substitute the original gitOperationManager in ChangeTrackerService with the mocked version
        ChangeTrackerService.getInstance(project).injectGitOperationManager(gitOperationManager!!)
        val dataParsingTask = LinkTrackerAction.DataParsingTask(
            currentProject = project,
            linkService = linkService!!,
            historyService = historyService!!,
            gitOperationManager = gitOperationManager!!,
            linkUpdateService = linkUpdateService!!,
            uiService = uiService!!,
            dryRun = true
        )
        ProgressManager.getInstance().run(dataParsingTask)
        val links = dataParsingTask.getLinks()
        println(links)
        // This should return gitFileChanges, but doesn't
        println(ChangeTrackerService.getInstance(project).getGitManager().getAllChangesForFile(
            links[0].first,
            60,
            null,
            null
        ))
    }
}