package org.intellij.plugin.tracker.services

import com.intellij.openapi.project.ProjectManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.nhaarman.mockitokotlin2.mock
import org.intellij.plugin.tracker.data.links.LinkInfo
import org.intellij.plugin.tracker.core.update.LinkElementImpl
import org.junit.jupiter.api.Assertions

/**
 * This class tests the parsing of links and changes.
 */
class LinkRetrieverServiceTest : BasePlatformTestCase() {

    private val myFiles = arrayOf(
        "testParseRelativeLinks.md",
        "main/file.txt",
        "testParseWebLink.md",
        "testParseMultipleLinks.md"
    )
    private var myRetrievedInfo = mutableListOf<LinkInfo>()

    override fun getTestDataPath(): String {
        return "src/test/kotlin/org/intellij/plugin/tracker/services/testdata"
    }

    override fun setUp() {
        super.setUp()
        myFixture.configureByFiles(*myFiles)
        scan()
    }

    private fun scan() {
        val linkRetrieverService = LinkRetrieverService.getInstance(project)
        linkRetrieverService.getLinks(myRetrievedInfo)
    }

    private fun getInfoByText(linkText: String) = myRetrievedInfo.first { it.linkText == linkText }

    fun testParseRelativeLinkToFile() {

        val expectedInfo = LinkInfo(
            linkText = "single - relative link to file",
            linkPath = "file.txt",
            proveniencePath = "/src/testParseRelativeLinks.md",
            foundAtLineNumber = 1,
            linkElement = LinkElementImpl(mock()),
            fileName = "testParseRelativeLinks.md",
            project = ProjectManager.getInstance().openProjects[0]
        )

        val linkInfo = myRetrievedInfo.first { it.linkText == "single - relative link to file" }
        expectedInfo.linkElement = linkInfo.linkElement
        Assertions.assertEquals(expectedInfo, linkInfo)
    }

    fun testParseRelativeLinkToDirectory() {

        val expectedInfo = LinkInfo(
            linkText = "single - relative link to directory",
            linkPath = "main",
            proveniencePath = "/src/testParseRelativeLinks.md",
            foundAtLineNumber = 3,
            linkElement = LinkElementImpl(mock()),
            fileName = "testParseRelativeLinks.md",
            project = ProjectManager.getInstance().openProjects[0]
        )

        val resultedInfo = getInfoByText(linkText = "single - relative link to directory")
        expectedInfo.linkElement = resultedInfo.linkElement
        Assertions.assertEquals(expectedInfo, resultedInfo)
    }

    fun testParseRelativeLinkToLine() {

        val expectedInfo = LinkInfo(
            linkText = "single - relative link to line",
            linkPath = "file.txt#L1",
            proveniencePath = "/src/testParseRelativeLinks.md",
            foundAtLineNumber = 2,
            linkElement = LinkElementImpl(mock()),
            fileName = "testParseRelativeLinks.md",
            project = ProjectManager.getInstance().openProjects[0]
        )

        val resultedInfo = getInfoByText(linkText = "single - relative link to line")
        expectedInfo.linkElement = resultedInfo.linkElement
        Assertions.assertEquals(expectedInfo, resultedInfo)
    }

    fun testParseWebLinkToFile() {
        val expectedInfo = LinkInfo(
            linkText = "single - web link to file",
            linkPath = "https://github.com/tudorpopovici1/demo-plugin-jetbrains-project/blob/cf925c192b45c9310a2dcc874573f393024f3be2/src/main/java/actions/MarkdownAction.java",
            proveniencePath = "/src/testParseWebLink.md",
            foundAtLineNumber = 2,
            linkElement = LinkElementImpl(mock()),
            fileName = "testParseWebLink.md",
            project = ProjectManager.getInstance().openProjects[0]
        )

        val resultedInfo = getInfoByText(linkText = "single - web link to file")
        expectedInfo.linkElement = resultedInfo.linkElement
        Assertions.assertEquals(expectedInfo, resultedInfo)
    }

    fun testParseWebLinkToLine() {
        val expectedInfo = LinkInfo(
            linkText = "single - web link to line",
            linkPath = "https://github.com/tudorpopovici1/demo-plugin-jetbrains-project/blob/cf925c192b45c9310a2dcc874573f393024f3be2/src/main/java/actions/MarkdownAction.java#L55",
            proveniencePath = "/src/testParseWebLink.md",
            foundAtLineNumber = 1,
            linkElement = LinkElementImpl(mock()),
            fileName = "testParseWebLink.md",
            project = ProjectManager.getInstance().openProjects[0]
        )

        val resultedInfo = getInfoByText(linkText = "single - web link to line")
        expectedInfo.linkElement = resultedInfo.linkElement
        Assertions.assertEquals(expectedInfo, resultedInfo)
    }

    fun testParseMultipleLinks() {

        val multiLinks = myRetrievedInfo.filter { it.fileName == "testParseMultipleLinks.md" }
        Assertions.assertEquals(3, multiLinks.size)
    }
}
