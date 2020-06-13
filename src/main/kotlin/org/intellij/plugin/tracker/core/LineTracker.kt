package org.intellij.plugin.tracker.core

import info.debatty.java.stringsimilarity.Cosine
import org.intellij.plugin.tracker.data.Line
import org.intellij.plugin.tracker.data.changes.CustomChange
import org.intellij.plugin.tracker.data.changes.LineChange
import org.intellij.plugin.tracker.data.changes.LineChangeType
import org.intellij.plugin.tracker.data.changes.LinesChange
import org.intellij.plugin.tracker.data.changes.LinesChangeType
import org.intellij.plugin.tracker.data.diff.DiffOutput
import org.intellij.plugin.tracker.data.diff.DiffOutputMultipleRevisions
import org.intellij.plugin.tracker.data.links.Link
import org.simmetrics.metrics.Levenshtein

class LineTracker {

    companion object {

        fun trackLines(link: Link, diffOutputMultipleRevisions: DiffOutputMultipleRevisions): LinesChange {
            var linesToTrack: MutableList<Int> =
                (link.referencedStartingLine..link.referencedEndingLine).toMutableList()

            val originalLineNumbers: MutableList<Int> =
                (link.referencedStartingLine..link.referencedEndingLine).toMutableList()

            val contentHashMap: HashMap<Int, String> = hashMapOf()

            for (i: Int in 0 until originalLineNumbers.size) {
                contentHashMap[originalLineNumbers[i]] = diffOutputMultipleRevisions.originalLinesContents[i]
            }

            for ((index: Int, line: Int) in linesToTrack.withIndex()) {
                val diffOutputMultipleRevisionTransformed = DiffOutputMultipleRevisions(
                    diffOutputMultipleRevisions.fileChange,
                    diffOutputMultipleRevisions.diffOutputList,
                    diffOutputMultipleRevisions.originalLinesContents[index]
                )
                val result: LineChange = trackLine(
                    link,
                    diffOutputMultipleRevisionTransformed,
                    givenLineToTrack = line
                )
                when (result.lineChangeType) {
                    LineChangeType.DELETED -> linesToTrack[index] = -1
                    LineChangeType.MOVED -> linesToTrack[index] = result.newLine!!.lineNumber
                    // unchanged, do nothing
                    else -> Unit
                }
            }
            val changeMap: HashMap<Int, Int> = hashMapOf()

            for (i: Int in 0 until linesToTrack.size) {
                if (linesToTrack[i] == -1) {
                    continue
                } else {
                    changeMap[linesToTrack[i]] = originalLineNumbers[i]
                }
            }

            linesToTrack = linesToTrack.filter { lineNo -> lineNo != -1 }.toMutableList()
            linesToTrack = linesToTrack.distinct().toMutableList()
            linesToTrack.sort()

            if (linesToTrack.size == 0) {
                return LinesChange(
                    diffOutputMultipleRevisions.fileChange,
                    LinesChangeType.DELETED
                )
            }

            val result: MutableList<MutableList<Int>> = groupConsecutiveNumbers(linesToTrack)
            val newLines: MutableList<MutableList<Line>> = mutableListOf()

            for (group in result) {
                val lineGroup: MutableList<Line> = mutableListOf()
                for (lineNumber in group) {
                    lineGroup.add(Line(lineNumber, contentHashMap[changeMap[lineNumber]]!!))
                }
                newLines.add(lineGroup)
            }

            if (result.size == 1) {
                var changed = false
                for (entry in changeMap) {
                    if (entry.key != entry.value) {
                        changed = true
                        break
                    }
                }
                if (!changed) {
                    return LinesChange(
                        diffOutputMultipleRevisions.fileChange,
                        LinesChangeType.UNCHANGED,
                        newLines = newLines
                    )
                }
                return LinesChange(
                    diffOutputMultipleRevisions.fileChange,
                    LinesChangeType.FULL,
                    newLines = newLines
                )
            }

            return LinesChange(
                diffOutputMultipleRevisions.fileChange,
                LinesChangeType.PARTIAL,
                newLines = newLines
            )
        }

        private fun groupConsecutiveNumbers(list: MutableList<Int>): MutableList<MutableList<Int>> {
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

        fun trackLine(
            link: Link,
            diffOutputMultipleRevisions: DiffOutputMultipleRevisions,
            givenLineToTrack: Int = -1
        ): LineChange {
            val fileChange: CustomChange = diffOutputMultipleRevisions.fileChange
            val originalLineContent: String = diffOutputMultipleRevisions.originalLineContent.trim()
            var lineToTrack: Int
            if (givenLineToTrack != -1) {
                lineToTrack = givenLineToTrack
            } else {
                lineToTrack = link.lineReferenced
            }
            var addedLines: MutableList<Line> = mutableListOf()
            var modifications = 0
            var lineIsDeleted = false

            for (diffOutput: DiffOutput in diffOutputMultipleRevisions.diffOutputList) {
                // get the deleted line between the commits
                var deletedLines: MutableList<Line> = diffOutput.deletedLines
                // get the added lines between the commits
                addedLines = diffOutput.addedLines
                // remove leading / trailing spaces from line contents
                deletedLines = deletedLines.map {
                        line -> Line(line.lineNumber, line.content.trim(), line.contextLines)
                }.toMutableList()
                addedLines = addedLines.map {
                        line -> Line(line.lineNumber, line.content.trim(), line.contextLines)
                }.toMutableList()

                // try to find from the deleted lines list a line which has the same line number
                // which we are looking for
                val deletedLine: Line? = deletedLines.find { line -> line.lineNumber == lineToTrack }
                var lineIsFound = false

                // we found the line
                if (deletedLine != null) {
                    // join its context lines together in a string
                    val joinedStringContextLines: String = getJoinedStringContextLines(line = deletedLine)

                    // calculate the SimHash value for both the line contents and concatenated context lines
                    val deletedLineContextSimHash: Long = getSimHash(line = joinedStringContextLines)
                    val deletedLineContentSimHash: Long = getSimHash(line = deletedLine.content)
                    // list of the added lines together with the calculated overall
                    // hamming score
                    val possibleList: ArrayList<Pair<Line, Float>> = arrayListOf()

                    for (line: Line in addedLines) {
                        val joinedStringContextLines1: String = getJoinedStringContextLines(line = line)

                        // calculate the SimHash values of added lines joined context lines
                        // and added line contents respectively
                        val simHashContextAddedLine: Long = getSimHash(line = joinedStringContextLines1)
                        val simHashContentAddedLine: Long = getSimHash(line = line.content)

                        // calculate the hamming distance between the previously SimHash values
                        val scoreOfContext: Int = hamming(deletedLineContextSimHash, simHashContextAddedLine)
                        val scoreOfContent: Int = hamming(deletedLineContentSimHash, simHashContentAddedLine)

                        val overallScore: Float = 0.40f * scoreOfContext + 0.60f * scoreOfContent
                        possibleList.add(Pair(line, overallScore))
                    }

                    if (possibleList.size > 0) {
                        // sort by the overall score in ascending order
                        // lower scores mean that the two strings are more similar
                        // e.g. a small hamming distance between 2 strings
                        // will show that the 2 strings are similar
                        possibleList.sortBy { pair -> pair.second }
                        val result: Line? = mapLine(deletedLine, possibleList)
                        if (result != null) {
                            modifications++
                            lineIsFound = true
                            // found a match in possibleList! Update `lineToTrack` to the matching
                            // line's line number.
                            lineToTrack = result.lineNumber
                        }
                    }

                    // try to see whether the line was split.
                    val lineSplit: Pair<Line?, Int> = detectLineSplit(deletedLine.content, addedLines)

                    // for now, use the split line number for further calculations instead
                    // TODO: track the group of lines further
                    if (!lineIsFound && lineSplit.first != null) {
                        modifications++
                        lineIsFound = true
                        lineToTrack = lineSplit.first!!.lineNumber
                    }

                    if (!lineIsFound) {
                        lineIsDeleted = true
                        break
                    }
                } else {
                    // see how many lines were deleted before the line to track
                    var deletedLinesBefore: Int = deletedLines.count { line -> line.lineNumber < lineToTrack }
                    // see how many lines were added before the line to track
                    // these lines that are deleted/added before would influence the current location of `lineToTrack`
                    var addedLinesBefore: Int = addedLines.count { line -> line.lineNumber < lineToTrack }
                    // check whether there is an added line which has the same line number as `lineToTrack`
                    val find: Int = addedLines.indexOfFirst { line -> line.lineNumber == lineToTrack }

                    if (find != -1) addedLinesBefore++

                    // update the new location of the line according to the deleted/added lines
                    // before it
                    if (addedLinesBefore - deletedLinesBefore != 0) modifications++

                    var previousLineToTrack = lineToTrack
                    while (addedLinesBefore != 0 || deletedLinesBefore != 0) {
                        lineToTrack += addedLinesBefore - deletedLinesBefore
                        deletedLinesBefore = deletedLines.count { line -> line.lineNumber in previousLineToTrack + 1 until lineToTrack }
                        addedLinesBefore = addedLines.count { line -> line.lineNumber in previousLineToTrack + 1 until lineToTrack }
                        val find1: Int = addedLines.indexOfFirst { line -> line.lineNumber == lineToTrack }
                        if (find1 != -1) addedLinesBefore++
                        previousLineToTrack = lineToTrack
                    }
                }
            }

            var line: Line? = addedLines.find { line -> line.lineNumber == lineToTrack }
            val lineChangeType: LineChangeType = when {
                lineIsDeleted -> LineChangeType.DELETED
                modifications == 0 || lineToTrack == link.lineReferenced -> LineChangeType.UNCHANGED
                else -> LineChangeType.MOVED
            }

            if (line == null) line = Line(lineToTrack, originalLineContent)
            return LineChange(fileChange, lineChangeType, newLine = line)
        }

        private fun getJoinedStringContextLines(line: Line): String {
            return if (line.contextLines == null) ""
            else line.contextLines!!.joinToString()
        }

        private fun detectLineSplit(
            deletedLine: String,
            addedLines: List<Line>,
            thresholdValue: Float = 0.85f
        ): Pair<Line?, Int> {
            // initialize helper variables
            var bestMatch = -1f
            var bestLine: Line? = null
            var concatenatedLines = 0
            var bestConcatenatedLines: Int = concatenatedLines

            // go over each added line
            for (i: Int in 0 until addedLines.size - 1) {
                concatenatedLines = 1
                // calculate the levenshtein distance between the deleted line and this added line
                var previousScore: Float = Levenshtein().compare(deletedLine, addedLines[i].content)
                var concatenateString: String = addedLines[i].content

                for (j: Int in i + 1 until addedLines.size) {
                    // if the next added line is blank, break out
                    if (addedLines[j].content.isBlank()) break
                    // concatenate the next added line
                    concatenateString += addedLines[j].content

                    // increment the # of concatenated lines
                    concatenatedLines++

                    // compare the current score of the deleted line (which is the original line) contents
                    // with the concatenated string
                    val currentScore: Float = Levenshtein().compare(deletedLine, concatenateString)

                    // if the score gets getting better with each concatenation
                    // save it. It might mean that the line is being built-up to its original format
                    // with each concatenation that we are doing.
                    if (currentScore >= bestMatch) {
                        bestMatch = currentScore
                        bestLine = addedLines[i]
                        bestConcatenatedLines = concatenatedLines
                    }

                    // if a concatenation would cause the score to go below the previous score
                    // just break. It means that going further with the loop won't make a difference
                    if (currentScore <= previousScore) break
                    previousScore = currentScore
                }
            }

            // if the best score goes over a pre-defined threshold, return the index in addedLines
            // at which the line split begins, accompanied with the number of lines over which the line
            // is split
            if (bestMatch >= thresholdValue && bestConcatenatedLines > 1) return Pair(bestLine, bestConcatenatedLines)
            // no line split found, return null and 0 respectively
            return Pair(null, 0)
        }

        private fun mapLine(
            deletedLine: Line,
            possibleList: List<Pair<Line, Float>>,
            thresholdValue: Float = 0.65f
        ): Line? {
            // initialize helper variables
            var mappingFound = false
            var bestMatch = 0.45F
            var bestLine: Line? = null

            // go over each pair in possibleList
            // this pair contains the added line accompanied by the hamming distance overall score
            // between the deleted line and this added line
            // possibleList is ordered according to overall score in INCREASING order!
            for (i: Int in possibleList.indices) {
                // calculate the levenshtein distance between the deleted line and currently inspected added line
                val scoreContent: Float = Levenshtein().compare(deletedLine.content, possibleList[i].first.content)

                // initialize a hash multiset containing the context lines for the deleted line
                // and a hash multiset for the context lines of the currently inspected added line
                var contextLinesFirst = ""
                var contextLinesSecond = ""

                if (deletedLine.contextLines != null)
                    for (el: Line in deletedLine.contextLines!!) contextLinesFirst += el.content
                if (possibleList[i].first.contextLines != null)
                    for (el: Line in possibleList[i].first.contextLines!!.iterator()) contextLinesSecond += el.content.trim()

                // calculate the cosine similarity between the context lines of the deleted line
                // and the context lines of the currently inspected added line
                val scoreContext: Float = 1 - Cosine().distance(contextLinesFirst, contextLinesSecond).toFloat()

                // 1 - CosineSimilarity<String>().compare(multiSet1, multiSet2)
                val score: Double = 0.6 * scoreContent + 0.4 * scoreContext

                // if the overall score is best so far, save the details
                if (score > bestMatch) {
                    mappingFound = true
                    bestLine = possibleList[i].first
                    bestMatch = score.toFloat()
                }
            }

            // if we found a mapping so far and this mapping is bigger than the threshold value
            // return the matching line
            // otherwise, return null
            return if (mappingFound) {
                if (bestMatch >= thresholdValue) return bestLine
                return null
            } else {
                null
            }
        }

        /**
         * Does a bit-wise hamming distance between two long values.
         * Returns the hamming distance
         */
        private fun hamming(first: Long, second: Long): Int {
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
        private fun getSimHash(line: String, hashSize: Int = 32): Long {
            val bitArray = IntArray(hashSize) { 0 }
            val shingleList: ArrayList<String> = createShingles(line)
            var simHashValue: Long = 0

            for (shingle: String in shingleList) {
                val hashedShingle: Long = JenkinsHash().hash(shingle.toByteArray())
                for (i: Int in 0 until hashSize) {
                    bitArray[i] += if (ithBitSet(hashedShingle, i)) 1 else -1
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
        private fun ithBitSet(hashedShingle: Long, position: Int): Boolean {
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
        private fun createShingles(line: String, shingleSize: Int = 2): ArrayList<String> {
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
    }
}
