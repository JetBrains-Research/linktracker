package org.intellij.plugin.tracker.data

import org.intellij.plugin.tracker.data.changes.CustomChange

open class FileChangeGatheringException(override val message: String?) : Exception(message)
open class DirectoryChangeGatheringException(override val message: String?) : Exception(message)
open class LineChangeGatheringException(override val message: String?, open val fileChange: CustomChange) :
    Exception(message)
open class LinesChangeGatheringException(override val message: String?, open val fileChange: CustomChange) :
    Exception(message)

class CommitSHAIsNullLineException(
    override val message: String? =
        "Could not find the start commit of the line containing this link, " +
                "please try to commit the file containing the link and run the plugin again.",
    override val fileChange: CustomChange
) : LineChangeGatheringException(message, fileChange)

class CommitSHAIsNullLinesException(
    override val message: String? =
        "Could not find the start commit of the line containing this link, " +
                "please try to commit the file containing the link and run the plugin again.",
    override val fileChange: CustomChange
) : LinesChangeGatheringException(message, fileChange)

class CommitSHAIsNullDirectoryException(
    override val message: String? =
        "Could not find the start commit of the line containing this link, " +
                "please try to commit the file containing the link and run the plugin again."
) : DirectoryChangeGatheringException(message)

class OriginalLineContentsNotFoundException(
    override val message: String? = "Could not find the contents of the line specified in the link",
    override val fileChange: CustomChange
) : LineChangeGatheringException(message, fileChange)

class OriginalLinesContentsNotFoundException(
    override val message: String? = "Could not find the contents of the lines specified in the link",
    override val fileChange: CustomChange
) : LineChangeGatheringException(message, fileChange)

class InvalidFileChangeTypeException(
    override val message: String?
) : FileChangeGatheringException(message)

class FileHasBeenDeletedException(
    override val message: String? = "File has been deleted. Line can not be tracked.",
    override val fileChange: CustomChange
) : LineChangeGatheringException(message, fileChange)

class InvalidFileChangeException(
    override val message: String? = "There was an error gathering the changes for the file",
    override val fileChange: CustomChange
) : LineChangeGatheringException(message, fileChange)

class ReferencedPathNotFoundException(
    val linkPath: String,
    override val message: String? =
        "File existed, but the path $linkPath to this file never existed in Git history."
) : FileChangeGatheringException(message)

class WebLinkReferenceTypeIsInvalidException(
    override val message: String? = "The web link reference type is invalid."
) : FileChangeGatheringException(message)

class ReferencedFileNotFoundException(
    override val message: String? = "Referenced file never existed in Git history."
) : FileChangeGatheringException(message)

class ChangeTypeExtractionException(
    override val message: String? = "Could not parse this file"
) : FileChangeGatheringException(message)

class RemoteDirectoryNeverExistedException(
    override val message: String? = "Directory never existed"
) : DirectoryChangeGatheringException(message)

class UnableToFetchRemoteDirectoryChangesException(
    override val message: String?
) : DirectoryChangeGatheringException(
    "There was a problem in gathering the directory changes from remote repository: $message"
)

class LocalDirectoryNeverExistedException(
    override val message: String? = "Could not track this directory"
) : DirectoryChangeGatheringException(message)

class UnableToFetchLocalDirectoryChangesException(
    override val message: String?
) : DirectoryChangeGatheringException(
    "There was a problem in gathering the directory changes from local directory: $message"
)
