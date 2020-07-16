package org.intellij.plugin.tracker.utils

import org.intellij.plugin.tracker.data.ChangeTypeExtractionException
import org.intellij.plugin.tracker.data.changes.CustomChange
import org.intellij.plugin.tracker.data.changes.CustomChangeType

/**
 * Auxiliary function to parse the content for the traversal that takes place
 * at method processChangesForFiles()
 *
 * If a line starts with R, it will have a before path and an after path. This method
 * retrieves the after path
 * Otherwise, a line will only have 1 non-changed path. We want to retrieve this path in this case.
 */
internal fun parseContent(content: String): String {
    return when {
        content.startsWith("R") -> {
            val lineSplit: List<String> = content.trim().split("\t".toPattern())
            assert(lineSplit.size == 3)
            lineSplit[2]
        }
        // line containing a commit along with the commit description
        content.matches("[a-z0-9]{6}.*".toRegex()) -> {
            val lineSplit: List<String> = content.trim().split("\\s+".toPattern())
            assert(lineSplit.isNotEmpty())
            "Commit: ${lineSplit[0]}"
        }
        else -> {
            val lineSplit: List<String> = content.trim().split("\t".toPattern())
            assert(lineSplit.size == 2)
            lineSplit[1]
        }
    }
}

/**
 * Based on a line that is being retrieved from a git command, convert this line into a ChangeType object.
 *
 * All lines retrieved from git will be of the type: <CHANGE_TYPE> <PATH> <PATH>(optional)
 *
 * Where CHANGE_TYPE can be R -> Renamed, A -> Added, M -> Modified etc.
 * First PATH is the before-path for the renamed change type and the non-changed path for other change types.
 * Second PATH is optional and corresponds to the after path for renamed change types.
 */
internal fun extractChangeType(linkPath: String, line: String): CustomChange {
    when {
        line.startsWith("A") -> {
            val lineSplit: List<String> = line.trim().split("\t".toPattern())
            assert(lineSplit.size == 2)
            if (lineSplit[1] != linkPath) return CustomChange(CustomChangeType.MOVED, lineSplit[1])

            return CustomChange(CustomChangeType.ADDED, lineSplit[1])
        }
        line.startsWith("M") -> {
            val lineSplit: List<String> = line.trim().split("\t".toPattern())
            assert(lineSplit.size == 2)
            if (lineSplit[1] != linkPath) return CustomChange(CustomChangeType.MOVED, lineSplit[1])

            return CustomChange(CustomChangeType.MODIFIED, lineSplit[1])
        }
        line.startsWith("D") -> return CustomChange(CustomChangeType.DELETED, linkPath)
        line.startsWith("R") -> {
            val lineSplit: List<String> = line.trim().split("\t".toPattern())
            assert(lineSplit.size == 3)
            if (lineSplit[2] == linkPath) return CustomChange(CustomChangeType.MODIFIED, lineSplit[2])
            return CustomChange(CustomChangeType.MOVED, lineSplit[2])
        }
        else -> throw ChangeTypeExtractionException()
    }
}

/**
 * Auxiliary function that processes the output of `git status --porcelain=v1`
 *
 * Splits the output into lines and checks the first letter of each line - based on this first letter
 * we can determine the type of change that affected a file
 *
 * We are checking whether there exists a line in the output which contains \
 * the link path that we are looking for.
 */
internal fun processWorkingTreeChanges(linkPath: String, changes: String): CustomChange? {
    val changeList: List<String> = changes.split("\n")
    changeList.forEach { line -> line.trim() }

    var change: String? = changeList.find { line -> line.split(" ".toRegex()).any { res -> res == linkPath } }
    if (change != null) {
        change = change.trim()
        when {
            change.startsWith("?") -> return CustomChange(CustomChangeType.ADDED, linkPath)
            change.startsWith("!") -> return CustomChange(CustomChangeType.ADDED, linkPath)
            change.startsWith("C") -> return CustomChange(CustomChangeType.ADDED, linkPath)
            change.startsWith("A") -> return CustomChange(CustomChangeType.ADDED, linkPath)
            change.startsWith("U") -> return CustomChange(CustomChangeType.ADDED, linkPath)
            change.startsWith("R") -> {
                val lineSplit = change.split(" -> ")
                assert(lineSplit.size == 2)
                if (lineSplit[1] == linkPath) return CustomChange(CustomChangeType.ADDED, lineSplit[1])
                return CustomChange(CustomChangeType.MOVED, lineSplit[1])
            }
            change.startsWith("D") -> return CustomChange(CustomChangeType.DELETED, linkPath)
            change.startsWith("M") -> return CustomChange(CustomChangeType.MODIFIED, linkPath)
        }
    }
    return null
}