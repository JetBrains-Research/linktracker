package org.intellij.plugin.tracker.utils

import org.intellij.plugin.tracker.core.change.ChangeSource
import org.intellij.plugin.tracker.data.changes.CustomChange
import org.intellij.plugin.tracker.data.changes.CustomChangeType
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
    linkPath: String,
    directoryInfo: ChangeSource.DirectoryInfo,
    directorySimilarity: Int = 60
): CustomChange {
    val similarityPair: Pair<String, Int>? = calculateSimilarity(directoryInfo.movedFiles, directoryInfo.directorySize)
    if (similarityPair != null && similarityPair.second >= directorySimilarity) {
        return CustomChange(CustomChangeType.MOVED, afterPathString = similarityPair.first)
    }
    return CustomChange(CustomChangeType.DELETED, afterPathString = linkPath)
}