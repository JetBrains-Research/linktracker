package org.intellij.plugin.tracker.utils

import info.debatty.java.stringsimilarity.Cosine
import org.intellij.plugin.tracker.core.line.JenkinsHash
import org.intellij.plugin.tracker.data.changes.LinesChangeType
import org.intellij.plugin.tracker.data.diff.Line


/**
 * Does a bit-wise hamming distance between two long values.
 * Returns the hamming distance
 */
internal fun hamming(first: Long, second: Long): Int {
    var x: Long = first xor second
    var setBits = 0L

    while (x > 0) {
        setBits += x and 1L
        x = x shr 1
    }
    return setBits.toInt()
}

/**
 * Method that calculates the sim hash value of a given string (line)
 *
 * Breaks the string into shingles (list of fragments of the string)
 * and hashes each shingle using a hashing algorithm (in this case Jenkins).
 *
 * If the i-th bit of the hashed shingle is set, add a 1 to the bit-array
 * in position i, else subtract 1 from position i of that bit-array
 *
 * Sim hash bit i will be 1 if the bit array at position i is > 0, 0 otherwise
 */
internal fun getSimHash(line: String, hashSize: Int = 32): Long {
    val bitArray = IntArray(hashSize) { 0 }
    val shingleList: ArrayList<String> = createShingles(line)
    var simHashValue: Long = 0

    for (shingle: String in shingleList) {
        val hashedShingle: Long = JenkinsHash().hash(shingle.toByteArray())
        for (i: Int in 0 until hashSize) {
            bitArray[i] += if (ithBitSet(
                    hashedShingle,
                    i
                )
            ) 1 else -1
        }
    }
    for (i: Int in 0 until hashSize) {
        val addingBitMask: Long = if (bitArray[i] > 0) {
            1L shl i
        } else {
            0L shl i
        }
        simHashValue = simHashValue or addingBitMask
    }
    return simHashValue
}

/**
 * Verifies whether the i-th bit of a long number in binary format is set
 */
internal fun ithBitSet(hashedShingle: Long, position: Int): Boolean {
    if (hashedShingle and (1L shl position) == 1L)
        return true
    return false
}

/**
 * Creates shingles (a list of fragments) out of a given string (line)
 * using a specified size for the shingles.
 *
 * Shingles are created by taking substrings of `shingle size` each
 * out of the initial string and appending them to a list
 */
internal fun createShingles(line: String, shingleSize: Int = 2): ArrayList<String> {
    val tokenList = ArrayList<String>()
    // remove all white spaces from the line
    val replacedLine: String = line.replace(Regex("\\s+"), "")
    var i = 0
    while (i + shingleSize < replacedLine.length) {
        tokenList.add(replacedLine.substring(i, i + shingleSize))
        i++
    }
    return tokenList
}

/**
 * Calculates the Cosine similarity between the context lines of a deleted line
 * and the context lines of the an added lines from a candidate list at a specific index,
 * returning the cosine similarity value between these 2.
 */
internal fun calculateCosineSimilarityOfContext(index: Int, deletedLine: Line, candidateList: List<Pair<Line, Float>>): Float {
    // initialize a hash multiset containing the context lines for the deleted line
    // and a hash multiset for the context lines of the currently inspected added line
    var contextLinesFirst = ""
    var contextLinesSecond = ""

    if (deletedLine.contextLines != null)
        for (el: Line in deletedLine.contextLines!!) contextLinesFirst += el.content
    if (candidateList[index].first.contextLines != null)
        for (el: Line in candidateList[index].first.contextLines!!.iterator()) contextLinesSecond += el.content.trim()

    // calculate the cosine similarity between the context lines of the deleted line
    // and the context lines of the currently inspected added line
    return 1 - Cosine().distance(contextLinesFirst, contextLinesSecond).toFloat()
}

/**
 * Auxiliary method that constructs a change map, i.e.
 * it maps each original line number (before tracking) to the final result
 * line number, all of this in a map data structure.
 */
internal fun constructChangeMap(originalLineNumbers: MutableList<Int>, linesToTrack: MutableList<Int>): HashMap<Int, Int> {
    val changeMap: HashMap<Int, Int> = hashMapOf()
    for (i: Int in 0 until linesToTrack.size) {
        if (linesToTrack[i] == -1) {
            continue
        } else {
            changeMap[linesToTrack[i]] = originalLineNumbers[i]
        }
    }
    return changeMap
}

/**
 * Constructs a joined string representation of the context lines of a specific line.
 */
internal fun getJoinedStringContextLines(line: Line): String {
    return if (line.contextLines == null) ""
    else line.contextLines!!.joinToString()
}

/**
 * Based on the list of sequences of consecutive line numbers,
 * constructs a lists of lists of lines that correspond to those
 * consecutive line numbers.
 */
internal fun groupTrackResults(
    trackResults: MutableList<MutableList<Int>>,
    contentHashMap: HashMap<Int, String>,
    changeMap: HashMap<Int, Int>
): MutableList<MutableList<Line>> {
    val newLines: MutableList<MutableList<Line>> = mutableListOf()
    for (group in trackResults) {
        val lineGroup: MutableList<Line> = mutableListOf()
        for (lineNumber in group) {
            lineGroup.add(Line(lineNumber, contentHashMap[changeMap[lineNumber]]!!))
        }
        newLines.add(lineGroup)
    }
    return newLines
}

/**
 * Auxiliary function that determines the final lines change type result,
 * based on the track results that were obtained from performing LHDiff on each
 * individual line from the group of lines.
 */
internal fun determineLinesChangeType(
    trackResults: MutableList<MutableList<Int>>,
    changeMap: HashMap<Int, Int>
): LinesChangeType {
    val linesChangeType: LinesChangeType
    if (trackResults.size == 1) {
        var changed = false
        for (entry in changeMap) {
            if (entry.key != entry.value) {
                changed = true
                break
            }
        }
        linesChangeType = if (!changed) {
            LinesChangeType.UNCHANGED
        } else {
            LinesChangeType.FULL
        }
    } else {
        linesChangeType = LinesChangeType.PARTIAL
    }
    return linesChangeType
}

/**
 * Groups a sequence of integer numbers into multiple sequences constituting
 * consecutive groups of numbers that occur in the list.
 */
internal fun groupConsecutiveNumbers(list: MutableList<Int>): MutableList<MutableList<Int>> {
    val result: MutableList<MutableList<Int>> = mutableListOf()
    var index = 0
    while (index < list.size) {
        val range: MutableList<Int> = mutableListOf()
        range.add(list[index])
        var index2: Int = index + 1
        while (index2 < list.size) {
            if (list[index] + 1 == list[index2]) {
                range.add(list[index2])
            } else {
                break
            }
            index = index2
            index2++
        }
        result.add(range)
        index = index2
    }
    return result
}

internal fun removeTrailingSpacesFromLinesContents(lines: List<Line>) =
    lines.map { line -> Line(line.lineNumber, line.content.trim(), line.contextLines) }
        .toMutableList()