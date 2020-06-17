package org.intellij.plugin.tracker.utils

import java.util.LinkedList
import java.util.regex.Pattern

/**
 * A collection of functions to filter and manipulate outputs of `git log` as provided by the
 * GitOperationManager::getLogOutputs() method.
 * These methods emulate the effect of adding various parameters to a `git log` call.
 */
object GitLogUtils {

    private val commitPattern: Pattern = Pattern.compile("[a-z0-9]{6}.*")

    /**
     * Emulates the behaviour of adding the --reverse parameter to a git log command.
     */
    fun reverseLog(log: String): String {

        if (log == "") return ""

        val logList = log.split("\n").map { it.trim() }
        val output = LinkedList<String>()

        // Iterate rows
        var currentCommitLine: String? = null
        val currentChangelines = mutableListOf<String>()
        for (line in logList) {

            val slices = line.split("\\s+".toRegex())
            val header = slices[0]

            // Check if row is commit row
            if (commitPattern.matcher(header).matches()) {
                // Add lines to output
                if (currentCommitLine != null) {

                    currentChangelines.map { output.push(it) }
                    output.push(currentCommitLine)
                    currentChangelines.clear()
                }
                currentCommitLine = line
            } else {
                // Throw exception if input log does not start wit commit line
                if (currentCommitLine == null) {
                    throw IllegalArgumentException("Log does not start with commit line")
                }
                currentChangelines.add(line)
            }
        }

        currentChangelines.map { output.push(it) }
        output.push(currentCommitLine)

        return output.joinToString("\n")
    }

    /**
     * Emulates the behaviour of adding the *{filename} parameter to a git log command.
     */
    fun filterByReferencedFileName(log: String, fileName: String): String {

        val logList = log.split("\n").map { it.trim() }
        val output = mutableListOf<String>()

        // Iterate rows
        var currentCommitLine: String? = null
        val currentChangelines = mutableListOf<String>()
        for (line in logList) {

            val slices = line.split("\\s+".toRegex())
            val header = slices[0]

            // Check if row is commit row
            if (commitPattern.matcher(header).matches()) {
                // Add lines to output
                if (currentCommitLine != null && currentChangelines.isNotEmpty()) {
                    output.add(currentCommitLine)
                    output.addAll(currentChangelines)
                    currentChangelines.clear()
                }
                currentCommitLine = line
            } else {
                // Throw exception if input log does not start wit commit line
                if (currentCommitLine == null) {
                    throw IllegalArgumentException("Log does not start with commit line")
                }
                // Save change line if it mentions the input filename
                if (slices[1].endsWith(fileName) or (slices.size > 2 && slices[2].endsWith(fileName))) {
                    currentChangelines.add(line)
                }
            }
        }

        if (currentChangelines.isNotEmpty()) {
            output.add(currentCommitLine!!)
            output.addAll(currentChangelines)
        }

        return output.joinToString("\n")
    }

    /**
     * Emulates the behavior of adding "{from}.." and "{until}^" params to a git log command.
     *
     * Expects the log to be made of commit lines only.
     * It is recommended to run filterCommitsOnly() on the input before using this function.
     *
     * Expects the log to be ordered from latest to first, as output by the Git CLI.
     * WARNING: Do not call this function after _reverseLog()_.
     */
    fun filterFromUntil(log: String, from: String, until: String): String {

        if (from == until) {
            throw IllegalArgumentException("from and until commits are the same")
        }

        var logList = log.split("\n").map { it.trim() }
        val output = mutableListOf<String>()

        // Iterate lines
        for (line in logList) {
            // Match on 'until' commit
            if (line.split("\\s+".toRegex())[0] == until) {
                if (output.isNotEmpty()) {
                    throw IllegalArgumentException("duplicated commit SHA")
                }
                output.add(line)
                // Match on 'from' commit
            } else if (line.split("\\s+".toRegex())[0] == from) {
                if (output.isEmpty()) {
                    throw IllegalArgumentException("'until' commit found before 'from'")
                }
                output.add(line)
                break
                // Add lines if between until and from
            } else {
                if (output.isNotEmpty()) {
                    output.add(line)
                }
            }
        }

        return output.joinToString("\n")
    }

    /**
     * Filter the input log and return a list of only the commit lines.
     */
    fun filterCommitsOnly(log: String): String = log
        .split("\n")
        .map { it.trim() }
        .filter { commitPattern.matcher(it.split(" ")[0]).matches() }
        .joinToString("\n")
}