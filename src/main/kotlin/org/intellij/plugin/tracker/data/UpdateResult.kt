package org.intellij.plugin.tracker.data

import org.intellij.plugin.tracker.data.links.Link

/**
 * A data class to hold results from the LinkUpdaterService's run.
 */
data class UpdateResult(
    /**
     * The links that were found to be invalid
     * and should have been correctly fixed by the plugin.
     */
    val updatedLinks: MutableList<Link>,

    /**
     * The links that were found to be invalid
     * and couldn't be automatically fixed.
     */
    val failedLinks: MutableList<Link>,

    /**
     * Service run duration.
     */
    val timeElapsed: Long
)
