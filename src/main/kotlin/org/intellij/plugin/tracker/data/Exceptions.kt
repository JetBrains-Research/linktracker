package org.intellij.plugin.tracker.data

import org.intellij.plugin.tracker.data.changes.*

/**
 * Generic, base exception class for change gathering exceptions
 */
open class ChangeGatheringException(
    override val message: String?,
    open val change: Change
) : Exception(message)

/**
 * Base exception for errors that occur during the process of retrieving changes for a file
 */
open class FileChangeGatheringException(
    override val message: String?,
    override val change: Change = CustomChange(CustomChangeType.INVALID, "", message)
) : ChangeGatheringException(message, change)

/**
 * Base exception for errors that occur during the process of retrieving changes for a directory
 */
open class DirectoryChangeGatheringException(
    override val message: String?,
    override val change: Change = CustomChange(CustomChangeType.INVALID, "", message)
) : ChangeGatheringException(message, change)

/**
 * Base exception for errors that occur during the process of retrieving changes for a single line
 */
open class LineChangeGatheringException(
    override val message: String?,
    open val fileChange: CustomChange,
    override val change: Change = LineChange(fileChange, LineChangeType.INVALID, message)
) : ChangeGatheringException(message, change)

/**
 * Base exception for errors that occur during the process of retrieving changes for multiple lines
 */
open class LinesChangeGatheringException(
    override val message: String?,
    open val fileChange: CustomChange,
    override val change: Change = LinesChange(fileChange, LinesChangeType.INVALID, message)
) : ChangeGatheringException(message, change)

/**
 * Exception thrown when the start commit SHA of a link to a line cannot be retrieved
 */
class CommitSHAIsNullLineException(
    override val message: String? =
        "Could not find the start commit of the line containing this link, " +
                "please try to commit the file containing the link and run the plugin again.",
    override val fileChange: CustomChange = CustomChange(CustomChangeType.INVALID, "")
) : LineChangeGatheringException(message, fileChange)

/**
 * Exception thrown when the start commit SHA of a link to multiple lines cannot be retrieved
 */
class CommitSHAIsNullLinesException(
    override val message: String? =
        "Could not find the start commit of the line containing this link, " +
                "please try to commit the file containing the link and run the plugin again.",
    override val fileChange: CustomChange = CustomChange(CustomChangeType.INVALID, "")
) : LinesChangeGatheringException(message, fileChange)

/**
 * Exception thrown when the start commit SHA of a link to a line cannot be retrieved
 */
class CommitSHAIsNullDirectoryException(
    override val message: String? =
        "Could not find the start commit of the line containing this link, " +
                "please try to commit the file containing the link and run the plugin again."
) : DirectoryChangeGatheringException(message)

/**
 * Exception thrown when the contents of a line, in a file, at a specified commit cannot be retrieved
 */
class OriginalLineContentsNotFoundException(
    override val message: String? = "Could not find the contents of the line specified in the link",
    override val fileChange: CustomChange = CustomChange(CustomChangeType.INVALID, "")
) : LineChangeGatheringException(message, fileChange)

/**
 * Exception thrown when the contents of multiple lines, in a file, at a specified commit cannot be retrieved
 */
class OriginalLinesContentsNotFoundException(
    override val message: String? = "Could not find the contents of the lines specified in the link",
    override val fileChange: CustomChange = CustomChange(CustomChangeType.INVALID, "")
) : LineChangeGatheringException(message, fileChange)

/**
 * The file change that has been retrieved has an invalid file change type while retrieving file changes.
 */
class InvalidFileChangeTypeException(override val message: String?) : FileChangeGatheringException(message)

class FileWebLinkNotCorrespondingToLocalProjectException(
    override val message: String? = "The web link to file does not correspond to the currently open project"
) : FileChangeGatheringException(message)

/**
 * The file in the path existed in git history, but the full path has not.
 */
class ReferencedPathNotFoundException(
    val linkPath: String,
    override val message: String? =
        "File existed, but the path $linkPath to this file never existed in Git history."
) : FileChangeGatheringException(message)

/**
 * The reference part of the web link points to something that does not exist
 *
 * Be it either a commit SHA that does not exist, a branch or a tag name.
 * This applies of web links that correspond to the currently open project.
 */
class WebLinkReferenceTypeIsInvalidException(
    override val message: String? = "The web link reference type is invalid."
) : FileChangeGatheringException(message)

/**
 * Neither the file nor the full path is present in git history
 */
class ReferencedFileNotFoundException(
    override val message: String? = "Referenced file never existed in Git history."
) : FileChangeGatheringException(message)

/**
 * Error encountered while parsing the output of a git command, while gathering changes for a file
 */
class ChangeTypeExtractionException(
    override val message: String? = "Could not parse this file"
) : FileChangeGatheringException(message)

/**
 * Exception thrown in the case where a referenced directory never existed in the
 * currently open project
 */
class LocalDirectoryNeverExistedException(
    override val message: String? = "Could not track this directory"
) : DirectoryChangeGatheringException(message)

/**
 * Exception encountered while fetching directory changes from the local project
 */
class UnableToFetchLocalDirectoryChangesException(
    override val message: String?
) : DirectoryChangeGatheringException(
    "There was a problem in gathering the directory changes from local directory: $message")
