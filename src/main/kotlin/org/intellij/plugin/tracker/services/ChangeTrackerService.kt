package org.intellij.plugin.tracker.services

import org.intellij.plugin.tracker.data.changes.Change
import org.intellij.plugin.tracker.data.links.Link

interface ChangeTrackerService {

    fun getLocalFileChanges(
        link: Link,
        branchOrTagName: String? = null,
        specificCommit: String? = null
    ): Change

    fun getLocalDirectoryChanges(
        link: Link,
        branchOrTagName: String? = null,
        specificCommit: String? = null
    ): Change

    fun getLocalLineChanges(
        link: Link,
        branchOrTagName: String? = null,
        specificCommit: String? = null
    ): Change

    fun getLocalLinesChanges(
        link: Link,
        branchOrTagName: String? = null,
        specificCommit: String? = null
    ): Change

    fun getRemoteFileChanges(link: Link): Change

    fun getRemoteDirectoryChanges(link: Link): Change

    fun getRemoteLineChanges(link: Link): Change

    fun getRemoteLinesChanges(link: Link): Change
}
