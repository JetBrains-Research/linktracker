package org.intellij.plugin.tracker.services

import org.intellij.plugin.tracker.data.changes.Change
import org.intellij.plugin.tracker.data.links.Link

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
    fun getLocalFileChanges(
        link: Link,
        branchOrTagName: String? = null,
        specificCommit: String? = null
    ): Change

    /**
     * Get the changes for (relative) links to directories that correspond to the currently open project
     */
    fun getLocalDirectoryChanges(
        link: Link,
        branchOrTagName: String? = null,
        specificCommit: String? = null
    ): Change

    /**
     * Get the changes for (relative) links to a single line that correspond to the currently open project
     */
    fun getLocalLineChanges(
        link: Link,
        branchOrTagName: String? = null,
        specificCommit: String? = null
    ): Change

    /**
     * Get the changes for (relative) links to multiple lines that correspond to the currently open project
     */
    fun getLocalLinesChanges(
        link: Link,
        branchOrTagName: String? = null,
        specificCommit: String? = null
    ): Change

    /**
     * Get the changes for (web) links to files that do not correspond to the currently open project
     *
     * Has to resort to using the API of the platform hosting the code
     */
    fun getRemoteFileChanges(link: Link): Change

    /**
     * Get the changes for (web) links to directories that do not correspond to the currently open project
     *
     * Has to resort to using the API of the platform hosting the code
     */
    fun getRemoteDirectoryChanges(link: Link): Change


    /**
     * Get the changes for (web) links to a single line that does not correspond to the currently open project
     *
     * Has to resort to using the API of the platform hosting the code
     */
    fun getRemoteLineChanges(link: Link): Change

    /**
     * Get the changes for (web) links to multiple lines that do not correspond to the currently open project
     *
     * Has to resort to using the API of the platform hosting the code
     */
    fun getRemoteLinesChanges(link: Link): Change
}
