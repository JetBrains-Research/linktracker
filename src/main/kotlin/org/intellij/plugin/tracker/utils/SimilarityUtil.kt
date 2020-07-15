package org.intellij.plugin.tracker.utils

import org.intellij.plugin.tracker.data.changes.CustomChange
import org.intellij.plugin.tracker.data.changes.CustomChangeType
import org.intellij.plugin.tracker.services.ChangeTrackingPolicy
import org.intellij.plugin.tracker.settings.SimilarityThresholdSettings
import java.io.File

/**
 * Method that takes in a list of paths of the moved files and the size of the
 * added files list as parameters. It then tries to split each path in the moved files
 * into separate sub-paths, adding each to a counting map
 *
 * It then fetches the most numerous sub-path amongst all the moved files paths
 * and divides the number of occurrences to the added files list size.
 */
internal fun calculateSimilarity(movedFiles: List<String>, addedFilesSize: Int): Pair<String, Int>? {
    val countMap: HashMap<String, Int> = hashMapOf()
    for (path in movedFiles) {
        val usePath = path.replace(File(path).name, "")
        val splitPaths: List<String> = usePath.split("/")
        var pathStart = ""
        for (splitPath in splitPaths) {
            if (splitPath.isNotBlank()) {
                pathStart += "$splitPath/"
                if (countMap.containsKey(pathStart)) countMap[pathStart] = countMap[pathStart]!! + 1
                else countMap[pathStart] = 1
            }
        }
    }
    val maxValue: Int = countMap.maxBy { it.value }?.value ?: return null
    val maxPair =
        countMap.filter { entry -> entry.value == maxValue }.maxBy { it.key.length }
    return Pair(maxPair!!.key.removeSuffix("/"), (maxPair.value.toDouble() / addedFilesSize * 100).toInt())
}

internal fun calculateDirectorySimilarityAndDetermineChange(
    movedFiles: List<String>,
    linkPath: String,
    directoryContentsSize: Int,
    policy: ChangeTrackingPolicy,
    deletedFilesCount: Int
): CustomChange {
    if (policy == ChangeTrackingPolicy.HISTORY) {
        if (deletedFilesCount + movedFiles.size != directoryContentsSize) {
            return CustomChange(CustomChangeType.ADDED, linkPath)
        }
    }
    val similarityPair: Pair<String, Int>? = calculateSimilarity(movedFiles, directoryContentsSize)
    if (similarityPair != null && similarityPair.second >= SimilarityThresholdSettings.getSavedDirectorySimilarity()) {
        return CustomChange(CustomChangeType.MOVED, afterPathString = similarityPair.first)
    }
    return CustomChange(CustomChangeType.DELETED, afterPathString = linkPath)
}