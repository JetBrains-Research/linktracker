package org.intellij.plugin.tracker.services

import org.intellij.plugin.tracker.data.changes.Change
import org.intellij.plugin.tracker.data.links.Link

enum class ChangeTrackingPolicy { HISTORY, LOCAL }

/**
 * Generic interface for a change tracker class
 *
 * This interface declares common behaviour of a service that retrieves changes for links
 * depending on the environment in which the software is running
 */
interface ChangeTrackerService {

    /**
     * Get the changes for (relative) links to files that correspond to the currently open project
     */
    fun getLocalFileChanges(link: Link): Change

    /**
     * Get the changes for (relative) links to directories that correspond to the currently open project
     */
    fun getLocalDirectoryChanges(link: Link): Change

    /**
     * Get the changes for (relative) links to a single line that correspond to the currently open project
     */
    fun getLocalLineChanges(link: Link): Change

    /**
     * Get the changes for (relative) links to multiple lines that correspond to the currently open project
     */
    fun getLocalLinesChanges(link: Link): Change
}