package org.intellij.plugin.tracker.utils

import java.util.regex.Pattern

/**
 * Class containing patterns needed for identifying links based on their link path type
 */
enum class LinkPatterns(val pattern: Pattern) {

    /**
     * Pattern for a web-link to multiple lines (web-hosted git repositories)
     */
    WebLinkToLines(Pattern.compile("((https?://)|(www.))([a-zA-Z.]+)/([a-zA-Z0-9-_./]+)/([a-zA-Z]+[a-zA-Z0-9-_.]+)/((blob/([a-z0-9]+))|(-/blob))/([a-zA-Z0-9-_./]+)#L([0-9]+)-L([0-9]+)")),

    /**
     * Pattern for a web-link to a single line (web-hosted git repositories)
     */
    WebLinkToLine(Pattern.compile("((https?://)|(www\\.))([a-zA-Z.]+)/([a-zA-Z0-9-_./]+)/([a-zA-Z]+[a-zA-Z0-9-_.]+)/((blob/([a-z0-9]+))|(-/blob))/([a-zA-Z0-9-_./]+)#L([0-9]+)")),

    /**
     * Pattern for a web-link to a file (web-hosted git repositories)
     */
    WebLinkToFile(Pattern.compile("((https?://)|(www.))([a-zA-Z.]+)/([a-zA-Z0-9-_./]+)/([a-zA-Z]+[a-zA-Z0-9-_.]+)/((blob/([a-zA-Z-_.0-9]+))|(-/blob/([a-zA-Z-_.0-9]+)))/([a-zA-Z0-9-_./]+\\.[a-zA-Z0-9-_./]+)")),

    /**
     * Pattern for a web-link to a directory (web-hosted git repositories)
     */
    WebLinkToDirectory(Pattern.compile("((https?://)|(www.))([a-zA-Z.]+)/([a-zA-Z0-9-_./]+)/([a-zA-Z]+[a-zA-Z0-9-_.]+)/((tree/([a-zA-Z-_.0-9]+))|(-/tree/([a-zA-Z-_.0-9]+)))/([a-zA-Z0-9-_./]+)")),

    /**
     * Pattern for a generic web-link (not a link to code)
     */
    GenericWebLinks(Pattern.compile("https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&//=]*)")),

    /**
     * Pattern for a relative link to multiple lines
     */
    RelativeLinkToLines(Pattern.compile(".*[^\\\\\\.\\/\\:\\*\\?\\\"\\<\\>]+\\.[^\\\\\\.\\/\\:\\*\\?\\\"\\<\\>]+#L([0-9]+)-L([0-9]+)\$")),

    /**
     * Pattern for a relative link to a single line
     */
    RelativeLinkToLine(Pattern.compile(".*[^\\\\\\.\\/\\:\\*\\?\\\"\\<\\>]+\\.[^\\\\\\.\\/\\:\\*\\?\\\"\\<\\>]+#L([0-9]+)\$")),

    /**
     * Pattern for the git diff output line that corresponds to the git hunk info
     */
    GitDiffChangedLines(Pattern.compile("@@ -([0-9]+)(((,)([0-9]+))*) \\+([0-9]+)(((,)([0-9]+))*)"))
}
