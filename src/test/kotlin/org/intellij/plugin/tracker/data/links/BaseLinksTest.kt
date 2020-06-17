package org.intellij.plugin.tracker.data.links

import com.nhaarman.mockitokotlin2.mock
import junit.framework.TestCase
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.intellij.plugin.tracker.utils.LinkElementImpl
import org.junit.jupiter.api.Assertions
import java.lang.UnsupportedOperationException

/**
 * This class tests the not supported links and link info.
 */
class BaseLinksTest : TestCase() {

    private lateinit var linkInfo: LinkInfo
    private lateinit var link: NotSupportedLink

    override fun setUp() {
        super.setUp()
        linkInfo = LinkInfo(
            linkText = "dummy text",
            linkPath = "invalid link",
            proveniencePath = "",
            foundAtLineNumber = 1,
            linkElement = LinkElementImpl(mock()),
            fileName = "dummypath.md",
            project = mock()
        )

        link = NotSupportedLink(linkInfo)
    }

    fun testNotSupportedLink() {
        Assertions.assertEquals(link.lineReferenced, -1)
        Assertions.assertEquals(link.referencedFileName, "")
        Assertions.assertEquals(link.referencedStartingLine, -1)
        Assertions.assertEquals(link.referencedEndingLine, -1)
        Assertions.assertEquals(link.path, "invalid link")
        Assertions.assertEquals(link.copyWithAfterPath(link, link.linkInfo.linkPath), link)
        assertThatExceptionOfType(UnsupportedOperationException::class.java).isThrownBy {
            link.markdownFileMoved("")
        }
    }

    fun testLinkInfo() {
        Assertions.assertEquals(linkInfo.getAfterPathToOriginalFormat("/./././"), null)
        Assertions.assertEquals(linkInfo.proveniencePath, "")
        Assertions.assertEquals(linkInfo.foundAtLineNumber, 1)
    }
}
