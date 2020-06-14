package org.intellij.plugin.tracker.data

import com.intellij.openapi.project.Project
import org.intellij.plugin.tracker.data.changes.Change
import org.intellij.plugin.tracker.data.links.Link

/**
 * @author Tommaso Brandirali
 *
 * A data class to represents the results of scanning the links throughout the project.
 */
class ScanResult(

    /**
     * The list of link changes.
     */
    var myLinkChanges: MutableList<Pair<Link, Change>>,

    /**
     * The project referred to by the class' instance.
     */
    val myProject: Project
) {

    /**
     * Tracks whether the current scan values have been invalidated by virtual file changes.
     * Should be updated by a change listener in case of changes to the files tracked in the linksAndChangesList.
     */
    var myIsValid: Boolean = true

    /**
     * The list of files which contains links.
     */
    val myFiles: List<String>
        get() = myLinkChanges.map { it.first.linkInfo.proveniencePath }.distinct()

    /**
     * The hash map representing which links are valid.
     */
    private lateinit var myValidityMap: HashMap<Link, Boolean>

    /**
     * Checks whether the given link is considered valid in the validity map.
     */
    fun isValid(link: Link): Boolean {
        return myValidityMap[link] ?: false
    }

    /**
     * Signals the completion of the scan and inits the validity map.
     */
    fun lockResults() {
        myValidityMap = HashMap()
        myLinkChanges.map { myValidityMap.put(it.first, true) }
    }

    /**
     * Updates the validity map invalidating all links that have the input provenience path.
     */
    fun invalidateByFile(proveniencePath: String) {
        val matches = myLinkChanges
            .filter { proveniencePath.endsWith(it.first.linkInfo.proveniencePath) }
        matches.map { myValidityMap.put(it.first, false) }
    }
}
