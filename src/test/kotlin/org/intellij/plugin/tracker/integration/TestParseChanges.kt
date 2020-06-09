package org.intellij.plugin.tracker.integration

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vcs.AbstractVcsHelper
import com.intellij.openapi.vcs.VcsTestUtil
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.testFramework.replaceService
import com.intellij.vcs.test.VcsPlatformTest
import git4idea.GitUtil
import git4idea.GitVcs
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepositoryManager
import org.junit.jupiter.api.*

/**
 * This class is a template for testing parsing changes from the Git integration.
 * In order to create tests with a new project instance per test
 * it is necessary to create a different instance of this class for each test case.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestParseChanges : VcsPlatformTest() {

    protected lateinit var vcs: GitVcs
    protected lateinit var commitContext: CommitContext
    protected lateinit var repositoryManager: GitRepositoryManager
    protected lateinit var vcsHelper: MockVcsHelper
    protected lateinit var git: TestGitImpl

    private val commitCommand = GitCommand.COMMIT
    private val addCommad = GitCommand.ADD
    private lateinit var addAllHandler: GitLineHandler
    private lateinit var commitHandler: GitLineHandler

    @BeforeAll
    override fun setUp() {
        super.setUp()

        vcsHelper = MockVcsHelper(myProject)
        project.replaceService(AbstractVcsHelper::class.java, vcsHelper, testRootDisposable)

        git = TestGitImpl()
        ApplicationManager.getApplication().replaceService(Git::class.java, git, testRootDisposable)
        repositoryManager = GitUtil.getRepositoryManager(project)
        vcs = GitVcs.getInstance(project)
        vcs.doActivate()
        commitContext = CommitContext()
    }

    @AfterAll
    override fun tearDown() {
        super.tearDown()
    }

    @BeforeEach
    fun config() {
        git.init(project, testRootFile)
        addAllHandler = GitLineHandler(project, testRootFile, addCommad)
        addAllHandler.addParameters(".")
        commitHandler = GitLineHandler(project, testRootFile, commitCommand)
    }

    @Test
    fun test() {

        // Setup
        val testFile = VcsTestUtil.createFile(project, testRootFile, "TestFile", "Some content")
        val addResult = git.runCommand(addAllHandler)
        Assertions.assertTrue(addResult.success())
        val commitResult = git.runCommand(commitHandler)
        println(commitResult.errorOutput)
        Assertions.assertTrue(commitResult.success())
    }
}