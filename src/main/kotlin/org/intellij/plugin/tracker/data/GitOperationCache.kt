package org.intellij.plugin.tracker.data

object GitOperationCache {

    /**
     * The commit pointing to the HEAD.
     */
    var myHeadCommitSHA: String? = null
        set(value) {
            headCommitSetCounter += 1
            println("[ GitoperationCache ] - headCommitSHA recomputed $headCommitSetCounter times")
            field = value
        }

    private var headCommitSetCounter = 0

    /**
     * The first commit on the current branch or tag.
     */
    var myFirstCommitSHA: String? = null

    /**
     * The outputs of a `git log` command with default settings for each branch or tag name parameter.
     */
    var myLogOutputs: HashMap<String, String> = HashMap()

    fun invalidate() {
        myHeadCommitSHA = null
        myFirstCommitSHA = null
        myLogOutputs = HashMap()
    }
}