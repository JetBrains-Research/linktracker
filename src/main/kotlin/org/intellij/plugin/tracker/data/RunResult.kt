package org.intellij.plugin.tracker.data

/**
 * @author Tommaso Brandirali
 *
 * A dat class to represents the results of a run of the plugin
 */
data class RunResult(

    /**
     * The SHA of the HEAD commit the plugin was ran on.
     */
    val currentCommit: String,

    /**
     * The links that were valid when scanned by the plugin.
     */
    val validLinks: ArrayList<Link>,

    /**
     * The links that were found to be invalid
     * and should have been correctly fixed by the plugin.
     */
    val fixedLinks: ArrayList<Link>,

    /**
     * The links that were found to be invalid
     * and couldn't be automatically fixed.
     */
    val invalidLinks: ArrayList<Link>,

    /**
     * Plugin run duration.
     */
    val timeElapsed: Int
)