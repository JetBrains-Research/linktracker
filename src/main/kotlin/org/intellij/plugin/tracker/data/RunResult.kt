package org.intellij.plugin.tracker.data

import com.intellij.util.ui.update.Update
import org.intellij.plugin.tracker.data.links.Link

/**
 * @author Tommaso Brandirali
 *
 * A data class to represents the results of a run of the plugin.
 */
data class RunResult(

    /**
     * The SHA of the HEAD commit the plugin was ran on.
     */
    val currentCommit: String,

    /**
     * The links that were valid when scanned by the plugin.
     */
    val validLinks: MutableList<Link>,

    /**
     * The links that were found to be invalid.
     */
    val invalidLinks: MutableList<Link>,

    /**
     * The results from the LinkUpdaterService's run.
     */
    var updateResult: UpdateResult,

    /**
     * Plugin run duration.
     */
    val timeElapsed: Long
)
