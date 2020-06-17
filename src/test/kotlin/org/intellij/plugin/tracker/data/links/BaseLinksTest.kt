package org.intellij.plugin.tracker.data.links

import com.nhaarman.mockitokotlin2.mock
import org.intellij.plugin.tracker.services.git4idea.test.GitSingleRepoTest
import org.intellij.plugin.tracker.utils.LinkElementImpl
import org.intellij.plugin.tracker.utils.LinkPatterns
import org.junit.jupiter.api.Assertions

/**
 * This class tests the not supported links and link info.
 */
class BaseLinksTest : GitSingleRepoTest() {

    private lateinit var linkInfo: LinkInfo

    override fun setUp() {
        super.setUp()
        linkInfo = LinkInfo(
            linkText = "dummy text",
            linkPath = "invalid link",
            proveniencePath = "",
            foundAtLineNumber = 1,
            linkElement = LinkElementImpl(mock()),
            fileName = "dummypath.md",
            project = project
        )
    }

    fun testLinkInfo() {
        Assertions.assertEquals(linkInfo.getAfterPathToOriginalFormat("/./././"), null)
        Assertions.assertEquals(linkInfo.proveniencePath, "")
        Assertions.assertEquals(linkInfo.foundAtLineNumber, 1)
    }

    fun testReferenceType() {
        val webLinkToFile = WebLinkToFile(linkInfo.copy(linkPath = "https://gitlab.ewi.tudelft.nl/project_owner/" +
                "project/-/blob/branch/README.md"), LinkPatterns.WebLinkToFile.pattern)
        Assertions.assertEquals(webLinkToFile.referenceType, WebLinkReferenceType.INVALID)
    }
}
