package org.intellij.plugin.tracker.data

import org.intellij.plugin.tracker.data.changes.LinkChange
import org.intellij.plugin.tracker.data.links.Link

/**
 * @author Tommaso Brandirali
 *
 * A data class to represents the results of scanning the links throughout the project.
 */
data class ScanResult(

    /**
     * The list of link changes.
     */
    val linkChanges: MutableList<Pair<Link, LinkChange>>,

    /**
     * Tracks whether the current scan values have been invalidated by virtual file changes.
     * Should be updated by a change listener in case of changes to the files tracked in the linksAndChangesList.
     */
    var isValid: Boolean
) {

    /**
     * The list of files which contains links.
     */
    val files: List<String>
        get() = linkChanges.map { pair -> pair.first.linkInfo.proveniencePath }.distinct()
}