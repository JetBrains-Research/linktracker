package org.intellij.plugin.tracker.utils


enum class LinkPatterns(val patternString: String) {

    /**
     * WebLink patterns
     */
    WebLinkToLines("((https?://)|(www.))([a-zA-Z.]+)/([a-zA-Z0-9-_./]+)/([a-zA-Z]+[a-zA-Z0-9-_.]+)/((blob/([a-z0-9]+))|(-/blob))/([a-zA-Z0-9-_./]+)#L([0-9]+-L[0-9])"),
    WebLinkToLine("((https?://)|(www\\.))([a-zA-Z.]+)/([a-zA-Z0-9-_./]+)/([a-zA-Z]+[a-zA-Z0-9-_.]+)/((blob/([a-z0-9]+))|(-/blob))/([a-zA-Z0-9-_./]+)#L([0-9]+)"),
    WebLinkToFile("((https?://)|(www.))([a-zA-Z.]+)/([a-zA-Z0-9-_./]+)/([a-zA-Z]+[a-zA-Z0-9-_.]+)/((blob/([a-zA-Z-_.0-9]+))|(-/blob/([a-zA-Z-_.0-9]+)))/([a-zA-Z0-9-_./]+\\.[a-zA-Z0-9-_./]+)"),
    WebLinkToDirectory("((https?://)|(www.))([a-zA-Z.]+)/([a-zA-Z0-9-_./]+)/([a-zA-Z]+[a-zA-Z0-9-_.]+)/((tree/([a-zA-Z-_.0-9]+))|(-/tree/([a-zA-Z-_.0-9]+)))/([a-zA-Z0-9-_./]+)"),
    GenericWebLinks("https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&//=]*)"),

    /**
     * Relative link patterns
     */
    RelativeLinkToLines(".*[^\\\\\\.\\/\\:\\*\\?\\\"\\<\\>]+\\.[^\\\\\\.\\/\\:\\*\\?\\\"\\<\\>]+#L([0-9]+)-L([0-9]+)\$"),
    RelativeLinkToLine(".*[^\\\\\\.\\/\\:\\*\\?\\\"\\<\\>]+\\.[^\\\\\\.\\/\\:\\*\\?\\\"\\<\\>]+#L([0-9]+)\$")

}