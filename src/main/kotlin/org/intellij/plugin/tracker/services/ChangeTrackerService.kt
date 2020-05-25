package org.intellij.plugin.tracker.services

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import org.intellij.plugin.tracker.data.Line
import org.intellij.plugin.tracker.data.changes.ChangeType
import org.intellij.plugin.tracker.data.changes.DirectoryChange
import org.intellij.plugin.tracker.data.changes.LineChange
import org.intellij.plugin.tracker.data.changes.LinkChange
import org.intellij.plugin.tracker.data.links.*
import org.intellij.plugin.tracker.utils.GitOperationManager
import org.intellij.plugin.tracker.utils.LinkPatterns
import java.util.regex.Matcher
import kotlin.math.max
import kotlin.math.min


class ChangeTrackerService(project: Project) {

    private val gitOperationManager = GitOperationManager(project = project)

    /**
     * Main function for getting changes for a link to a file.
     */
    fun getFileChange(
        link: Link,
        branchOrTagName: String? = null,
        specificCommit: String? = null
    ): Pair<MutableList<Pair<String, String>>, Pair<Link, LinkChange>> {
        val workingTreeChange: LinkChange? = gitOperationManager.checkWorkingTreeChanges(link)

        // this file has just been added and is not tracked by git, but the link is considered valid
        if (workingTreeChange != null && workingTreeChange.changeType == ChangeType.ADDED) {
            return Pair(mutableListOf(Pair("Working tree", workingTreeChange.afterPath)), Pair(link, workingTreeChange))
        }

        val prop = PropertiesComponent.getInstance()
        val threshold = prop.getValue("threshold", "60").toInt()
        val result: Pair<MutableList<Pair<String, String>>, LinkChange> =
            gitOperationManager.getAllChangesForFile(link, threshold,
                    branchOrTagName = branchOrTagName, specificCommit = specificCommit)
        val change: LinkChange = result.second
        when (change.changeType) {
            // this file's change type is invalid
            ChangeType.INVALID -> {
                // this might be the case when a link corresponds to an uncommitted rename
                // git log history will have no changes when using the new name
                // but the working tree change will capture the rename, so we want to return it
                if (workingTreeChange != null) return Pair(
                    mutableListOf(
                        Pair(
                            "Working tree",
                            workingTreeChange.afterPath
                        )
                    ), Pair(link, workingTreeChange)
                )
                return Pair(mutableListOf(), Pair(link, change))
            }
            else -> {
                // so far we have only checked `git log` with the commit that is pointing to HEAD.
                // but we want to also check non-committed changes for file changes.
                // at this point, link was found and a new change has been correctly identified.

                // working tree change can be null (might be because we have first calculated the working tree change
                // using the unchanged path that was retrieved from the markdown file -- this path might have been invalid
                // but now we have a new path that corresponds to the original retrieved path
                // we want to check whether there is any non-committed change that affects this new path
                if (workingTreeChange == null) {
                    var newLink: Link? = null
                    // only these 2 link types get this far (LinkProcessingRouter handles this logic)
                    when (link) {
                        is RelativeLinkToFile -> {
                            val linkInfoCopy: LinkInfo = link.linkInfo.copy(linkPath = change.afterPath)
                            newLink = link.copy(linkInfo = linkInfoCopy)
                        }
                        is WebLinkToFile -> {
                            val linkInfoCopy: LinkInfo = link.linkInfo.copy(linkPath = change.afterPath)
                            newLink = link.copy(linkInfo = linkInfoCopy)
                        }
                        is RelativeLinkToLine -> {
                            val linkInfoCopy: LinkInfo = link.linkInfo.copy(linkPath = change.afterPath)
                            newLink = link.copy(linkInfo = linkInfoCopy)
                        }
                        is WebLinkToLine -> {
                            val linkInfoCopy: LinkInfo = link.linkInfo.copy(linkPath = change.afterPath)
                            newLink = link.copy(linkInfo = linkInfoCopy)
                        }
                        is RelativeLinkToLines -> {
                            val linkInfoCopy: LinkInfo = link.linkInfo.copy(linkPath = change.afterPath)
                            newLink = link.copy(linkInfo = linkInfoCopy)
                        }
                        is WebLinkToLines -> {
                            val linkInfoCopy: LinkInfo = link.linkInfo.copy(linkPath = change.afterPath)
                            newLink = link.copy(linkInfo = linkInfoCopy)
                        }
                    }

                    // safe !!
                    newLink!!.linkInfo.linkPath = change.afterPath
                    val currentChange: LinkChange = gitOperationManager.checkWorkingTreeChanges(newLink) ?: return Pair(
                        result.first,
                        Pair(link, change)
                    )
                    // new change identified (from checking working tree). Use this newly-found change instead.
                    result.first.add(Pair("Working tree", currentChange.afterPath))
                    return Pair(result.first, Pair(link, currentChange))
                }

                // if the working tree change change type is either deleted or moved
                // (calculated using the unchanged path retrieved from the markdown files),
                // use this change instead of the one found from `git log` command (it overrides it).
                // Otherwise, return the change found from `git log` command.
                return when (workingTreeChange.changeType) {
                    ChangeType.DELETED, ChangeType.MOVED -> {
                        result.first.add(Pair("Working tree", workingTreeChange.afterPath))
                        Pair(
                            result.first,
                            Pair(link, workingTreeChange)
                        )
                    }
                    else -> Pair(result.first, Pair(link, change))
                }
            }
        }
    }

    /**
     * Extract the directory we are looking for from a list of changes
     */
    private fun extractSpecificDirectoryChanges(changeList: MutableCollection<Change>): DirectoryChange {
        for (change in changeList) {
            val prevPath = change.beforeRevision?.file?.parentPath
            val currPath = change.afterRevision?.file?.parentPath
            if (prevPath != currPath) return DirectoryChange.changeToDirectoryChange(change)
        }
        return DirectoryChange(ChangeType.ADDED, "")
    }

    /**
     * Main function for getting changes for a directory.
     */
    fun getDirectoryChange(link: Link): Pair<Link, DirectoryChange> {
        val changeList = gitOperationManager.getDiffWithWorkingTree(link.commitSHA!!)
        return if (changeList != null) {
            val directoryChange = extractSpecificDirectoryChanges(changeList = changeList)
            Pair(link, directoryChange)
        } else {
            Pair(link, DirectoryChange(ChangeType.ADDED, ""))
        }
    }

    fun getLinkChange(link: Link): MutableList<LineChange> {

        val fileChange: Pair<MutableList<Pair<String, String>>, Pair<Link, LinkChange>> = getFileChange(link)

        val result = mutableListOf<LineChange>()

        // if the file change type is deleted, return immediately.
        // There is no need to track the lines in a file.
        if(fileChange.second.second.changeType==ChangeType.DELETED) {
            return result
        }

        val changeList: MutableList<Pair<String, String>> = fileChange.first

        for (x in 0 until changeList.size - 1) {
            val before = changeList[x].first.split("Commit: ")[1]
            val beforePath = changeList[x].second
            val after = changeList[x + 1].first.split("Commit: ")[1]
            val afterPath = changeList[x + 1].second
            val file = changeList[x].second
            val output = getDiffOutput(before, after, beforePath, afterPath, file)
            result.add(output)
        }
        return result
    }

    private fun getDiffOutput(
        before: String,
        after: String,
        beforePath: String,
        afterPath: String,
        file: String,
        contextLinesNumber: Int = 3
    ): LineChange {
        val output = gitOperationManager.getDiffBetweenCommits(before, after, beforePath, afterPath, contextLinesNumber)
        // skip the git diff header (first 4 lines)
        val lines: List<String?> = output.lines().subList(4, output.lines().size)
        val addedLines: MutableList<Line> = mutableListOf()
        val deletedLines: MutableList<Line> = mutableListOf()
        var startDeletedLine: Int
        var startAddedLine: Int
        var currentAddedLine = 0
        var currentDeletedLine = 0
        var contextLinesDeleted: MutableList<Line> = mutableListOf()
        var contextLinesAdded: MutableList<Line> = mutableListOf()

        for (line: String? in lines) {
            if (line == null) {
                break
            // git hunk info header
            } else if (line.startsWith("@@ ")) {
                val info = line.split(" @@")[0]
                val matcher: Matcher = LinkPatterns.GitDiffChangedLines.pattern.matcher(info)
                if (matcher.matches()) {
                    startDeletedLine = matcher.group(1).toInt()
                    currentDeletedLine = startDeletedLine
                    startAddedLine = matcher.group(6).toInt()
                    currentAddedLine = startAddedLine
                }
            // added line
            } else if (line.startsWith("+")) {
                val addedLine = Line(currentAddedLine, line.split("+")[1])
                addedLines.add(addedLine)
                contextLinesAdded.add(addedLine)
                currentAddedLine++
            // deleted line
            } else if (line.startsWith("-")) {
                val deletedLine = Line(currentDeletedLine, line.split("-")[1])
                deletedLines.add(deletedLine)
                contextLinesDeleted.add(deletedLine)
                currentDeletedLine++
            // this is an unchanged line: just add it to the context lines lists and increment the indices
            } else {
                contextLinesDeleted.add(Line(currentDeletedLine, line))
                contextLinesAdded.add(Line(currentAddedLine, line))
                currentAddedLine++
                currentDeletedLine++
            }
        }

        // remove git-added warning lines
        while (contextLinesAdded.last().content == "\\ No newline at end of file") {
            contextLinesAdded = contextLinesAdded.subList(0, contextLinesAdded.size - 1)
        }

        // remove git-added warning lines
        while (contextLinesDeleted.last().content == "\\ No newline at end of file") {
            contextLinesDeleted = contextLinesDeleted.subList(0, contextLinesDeleted.size - 1)
        }

        // populate the context lines properties of the added lines
        for (l: Line in addedLines) {
            val maxContextLineNumber: Int = contextLinesAdded.maxBy { line -> line.lineNumber }?.lineNumber ?: continue

            // get all of the context lines on the upper side of the line:
            // that is, the lines within [current_line_number - contextLinesNumber, current_line_number)
            // as well as all of the context line on the lower side of the line:
            // all of the lines within the interval (current_line_number, current_line-number+ contextLinesNumber)
            val contextLines: MutableList<Line> = contextLinesAdded.filter { line ->
                (line.lineNumber < l.lineNumber && line.lineNumber >= max(0, l.lineNumber - contextLinesNumber))
                 || (line.lineNumber > l.lineNumber && line.lineNumber <= min(l.lineNumber + contextLinesNumber, maxContextLineNumber))
             }.toMutableList()
            l.contextLines = contextLines
        }

        // populate the context lines properties of the deleted lines
        for (l: Line in deletedLines) {
            val maxContextLineNumber: Int = contextLinesDeleted.maxBy { line -> line.lineNumber }?.lineNumber ?: continue

            // get all of the context lines on the upper side of the line:
            // that is, the lines within [current_line_number - contextLinesNumber, current_line_number)
            // as well as all of the context line on the lower side of the line:
            // all of the lines within the interval (current_line_number, current_line-number+ contextLinesNumber)
            val contextLines: MutableList<Line> = contextLinesDeleted.filter { line ->
                (line.lineNumber < l.lineNumber && line.lineNumber >= max(0, l.lineNumber - contextLinesNumber))
                        || (line.lineNumber > l.lineNumber && line.lineNumber <= min(l.lineNumber + contextLinesNumber, maxContextLineNumber))
            }.toMutableList()
            l.contextLines = contextLines
        }

        return LineChange(file, addedLines, deletedLines, before, after)
    }

    private fun getLineChange(start: String, change: String?): MutableList<Int> {
        val startNum = start.toInt()
        val result = mutableListOf(startNum)
        if (change == null) {
            return result
        } else {
            val changeNum = change.toInt()
            if (changeNum == 0) {
                return mutableListOf()
            } else {
                for (i in (startNum + 1) until (startNum + changeNum)) {
                    result.add(i)
                }
                return result
            }
        }
    }

    companion object {
        fun getInstance(project: Project): ChangeTrackerService =
            ServiceManager.getService(project, ChangeTrackerService::class.java)
    }
}
