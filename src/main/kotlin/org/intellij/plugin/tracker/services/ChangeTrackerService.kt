package org.intellij.plugin.tracker.services

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

        val result: Pair<MutableList<Pair<String, String>>, LinkChange> =
            gitOperationManager.getAllChangesForFile(link, branchOrTagName = branchOrTagName, specificCommit = specificCommit)
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

    /**
     * Function for getting LineChange for a link between each commits of it
     * Optionally you can give a linNo param to see changes just in that line
     */
    fun getLinkChange(link: Link, lineNo:Int=0): MutableList<LineChange> {

        val fileChange: Pair<MutableList<Pair<String, String>>, Pair<Link, LinkChange>>? = getFileChange(link)

        val result = mutableListOf<LineChange>()

        val changeList: MutableList<Pair<String, String>> = fileChange!!.first

        for (x in 0 until changeList.size-1) {
            val before = changeList.get(x).first.split("Commit: ").get(1)
            val after = changeList.get(x+1).first.split("Commit: ").get(1)
            val file  = changeList.get(x).second
            val output = getDiffOutput(before, after, file, lineNo)
            result.add(output)
        }
        return result
    }

    /**
     * Helper function to get LineChange between two commits
     */
    private fun getDiffOutput(before: String, after: String, file: String, lineNo: Int): LineChange {
        val output = gitOperationManager.getDiffBetweenCommits(before, after, file)
        val lines: List<String?> = output.lines()
        val addedLines = mutableListOf<Line>()
        val deletedLines = mutableListOf<Line>()
        var secondStartLine: Int
        var currentLine = 0
        for(line in lines) {
            if (line == null) {
                break
            }
            if(line.startsWith("@@ ")) {
                val info = line.split(" @@").get(0)
                val matcher: Matcher = LinkPatterns.GitDiffChangedLines.pattern.matcher(info)
                if(matcher.matches()) {
                    secondStartLine = matcher.group(6).toInt()
                    currentLine = secondStartLine-1
                }
            }
            if(line.startsWith("+") && !line.startsWith("+++")) {
                val addedLine = Line(currentLine, line.split("+").get(1), addedLines)
                if(lineNo==0 || (lineNo!=0 && currentLine==lineNo)) {
                    addedLines.add(addedLine)
                }
            }
            if(line.startsWith("-") && !line.startsWith("---")) {
                val deletedLine = Line(currentLine, line.split("-").get(1), deletedLines)
                if(lineNo==0 || (lineNo!=0 && currentLine==lineNo)) {
                    deletedLines.add(deletedLine)
                }
                currentLine--
            }
            currentLine++
        }
        for (l in addedLines) {
            l.contextLines = addedLines
        }

        for (l in deletedLines) {
            l.contextLines = deletedLines
        }
        println(LineChange(file, addedLines, deletedLines, before, after))
        return LineChange(file, addedLines, deletedLines, before, after)
    }

    companion object {
        fun getInstance(project: Project): ChangeTrackerService =
            ServiceManager.getService(project, ChangeTrackerService::class.java)
    }
}
