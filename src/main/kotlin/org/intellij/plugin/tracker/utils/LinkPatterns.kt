package org.intellij.plugin.tracker.utils

import java.util.regex.Pattern


/**
 * Class containing patterns needed for identifying links based on their link path type
 */
enum class LinkPatterns(val pattern: Pattern) {

    /**
     * WebLink patterns
     */
    WebLinkToLines(Pattern.compile("((https?://)|(www.))([a-zA-Z.]+)/([a-zA-Z0-9-_./]+)/([a-zA-Z]+[a-zA-Z0-9-_.]+)/((blob/([a-z0-9]+))|(-/blob))/([a-zA-Z0-9-_./]+)#L([0-9]+)-L([0-9]+)")),
    WebLinkToLine(Pattern.compile("((https?://)|(www\\.))([a-zA-Z.]+)/([a-zA-Z0-9-_./]+)/([a-zA-Z]+[a-zA-Z0-9-_.]+)/((blob/([a-z0-9]+))|(-/blob))/([a-zA-Z0-9-_./]+)#L([0-9]+)")),
    WebLinkToFile(Pattern.compile("((https?://)|(www.))([a-zA-Z.]+)/([a-zA-Z0-9-_./]+)/([a-zA-Z]+[a-zA-Z0-9-_.]+)/((blob/([a-zA-Z-_.0-9]+))|(-/blob/([a-zA-Z-_.0-9]+)))/([a-zA-Z0-9-_./]+\\.[a-zA-Z0-9-_./]+)")),
    WebLinkToDirectory(Pattern.compile("((https?://)|(www.))([a-zA-Z.]+)/([a-zA-Z0-9-_./]+)/([a-zA-Z]+[a-zA-Z0-9-_.]+)/((tree/([a-zA-Z-_.0-9]+))|(-/tree/([a-zA-Z-_.0-9]+)))/([a-zA-Z0-9-_./]+)")),
    GenericWebLinks(Pattern.compile("https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&//=]*)")),

    /**
     * Relative link patterns
     */
    RelativeLinkToLines(Pattern.compile(".*[^\\\\\\.\\/\\:\\*\\?\\\"\\<\\>]+\\.[^\\\\\\.\\/\\:\\*\\?\\\"\\<\\>]+#L([0-9]+)-L([0-9]+)\$")),
    RelativeLinkToLine(Pattern.compile(".*[^\\\\\\.\\/\\:\\*\\?\\\"\\<\\>]+\\.[^\\\\\\.\\/\\:\\*\\?\\\"\\<\\>]+#L([0-9]+)\$")),
    RelativeLinkWithDoubleDots(Pattern.compile("((([a-zA-Z0-9%./\\- ]+)/)*)(([a-zA-Z0-9%\\- ]+)/../)([a-zA-Z0-9%./\\- ]+)")),
    RelativeLinkWithDoubleDotsAtEnd(Pattern.compile("(([a-zA-Z0-9%/\\- ]+)/([a-zA-Z0-9%\\- ]+)/..)")),
    RelativeLinkWithSingleDot(Pattern.compile("(([a-zA-Z0-9%/\\- ]+)/./([a-zA-Z0-9%/.\\- ]+))")),
    RelativeLinkWithSingleDotAtEnd(Pattern.compile("(([a-zA-Z0-9%/\\- ]+)/.)"))

}