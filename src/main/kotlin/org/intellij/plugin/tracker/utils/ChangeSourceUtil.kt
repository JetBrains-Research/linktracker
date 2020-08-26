package org.intellij.plugin.tracker.utils

import com.intellij.history.core.Paths
import com.intellij.history.core.changes.*
import com.intellij.openapi.project.Project
import org.intellij.plugin.tracker.data.ReferencedFileNotFoundException
import org.intellij.plugin.tracker.data.changes.CustomChange
import org.intellij.plugin.tracker.data.changes.CustomChangeType
import org.intellij.plugin.tracker.data.diff.FileHistory
import org.intellij.plugin.tracker.data.links.Link
import java.io.File

class ChangeSourceUtil(val project: Project) {

    fun getContentAtPath(filePath: String): String? {
        if (File(filePath).exists() && File(filePath).isFile)
            return File(filePath).readText()
        return null
    }

    fun removeProjectBasePath(path: String) = path.removePrefix("${project.basePath}/")

    private fun replaceBackwardsDoubleSlashes(path: String) = path.replace("\\", "/")

    private fun getRenamedPath(path: String, actualPath: String, computeForDirChange: Boolean = false): String {
        var tempActualPath = actualPath
        val tempPath = removeProjectBasePath(path)
        val splitPaths = actualPath.split("/")
        for (i in tempPath.split("/").indices) {
            try {
                tempActualPath = if (!computeForDirChange)
                    tempActualPath.removePrefix("${tempActualPath.split("/")[0]}/")
                else {
                    tempActualPath.removePrefix(splitPaths[i]).removePrefix("/")
                }
            } catch (e: IndexOutOfBoundsException) {
                break
            }
        }
        return replaceBackwardsDoubleSlashes(File(path, tempActualPath).path)
    }

    private fun getMovedPath(path: String, actualPath: String): String {
        val name = Paths.getNameOf(path)
        val index = actualPath.indexOf(name) + name.length
        val remainingPath = actualPath.substring(index, actualPath.length).removePrefix("/")
        return replaceBackwardsDoubleSlashes(File(path, remainingPath).path)
    }

    fun getNewPathUsingDirectoryChangePath(change: StructuralChange, actualPath: String, computeForDirChange: Boolean = false): String {
        if (change is RenameChange) return getRenamedPath(change.path, actualPath, computeForDirChange)
        return getMovedPath(change.path, actualPath)
    }

    fun getFilePath(change: StructuralChange, actualPath: String): String {
        val path = change.path
        var isDirectory = false
        if (File(path).exists()) {
            if (File(path).isDirectory) isDirectory = true
        } else {
            // TODO: Ambiguous. Should be a better way to detect whether non-existent path is a directory
            if (path.lastIndexOf(".") == -1) isDirectory = true
        }
        if (isDirectory) return getNewPathUsingDirectoryChangePath(change, actualPath)
        return path
    }

    fun checkFileExistsOnDisk(linkPath: String, specificCommit: String?): CustomChange {
        if (File("${project.basePath}/$linkPath").exists()) {
            val fileChange = CustomChange(CustomChangeType.ADDED, afterPathString = linkPath)
            fileChange.fileHistoryList = mutableListOf(FileHistory("Commit: $specificCommit", linkPath))
            return fileChange
        }
        throw ReferencedFileNotFoundException()
    }

    fun getAbsolutePathForLink(link: Link) =
        replaceBackwardsDoubleSlashes(File("${project.basePath}/${link.path}").path)
}