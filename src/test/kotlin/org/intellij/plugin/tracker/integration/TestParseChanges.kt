package org.intellij.plugin.tracker.integration

import org.intellij.plugin.tracker.integration.git4idea.test.GitSingleRepoTest
import org.intellij.plugin.tracker.utils.GitOperationManager
import org.junit.jupiter.api.*

/**
 * This class is a template for testing parsing changes from the Git integration.
 * In order to create tests with a new project instance per test
 * it is necessary to create a different instance of this class for each test case.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestParseChanges : GitSingleRepoTest() {

    private lateinit var gitOperationManager: GitOperationManager

    @BeforeAll
    override fun setUp() {
        super.setUp()
    }

    @AfterAll
    override fun tearDown() {
        super.tearDown()
    }

    @BeforeEach
    fun config() {
        gitOperationManager = GitOperationManager(project)
    }

    @Test
    fun test() {

        val linkedFile = file("file.txt").create("Some content")
        linkedFile.addCommit("Commit 1")

        val linkingFile = file("file.md").create("[link](file.txt)")
        linkingFile.addCommit("Commit 2")

        refresh()
        updateChangeListManager()

        println(gitOperationManager.getHeadCommitSHA())
    }
}