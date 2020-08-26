package org.intellij.plugin.tracker.core.line

import info.debatty.java.stringsimilarity.Cosine
import org.intellij.plugin.tracker.data.diff.Line
import org.intellij.plugin.tracker.data.changes.*
import org.intellij.plugin.tracker.data.diff.DiffOutput
import org.intellij.plugin.tracker.data.links.Link
import org.intellij.plugin.tracker.utils.*
import org.intellij.plugin.tracker.utils.calculateCosineSimilarityOfContext
import org.intellij.plugin.tracker.utils.getJoinedStringContextLines
import org.intellij.plugin.tracker.utils.getSimHash
import org.intellij.plugin.tracker.utils.hamming
import org.simmetrics.metrics.Levenshtein

class LineTracker {

    companion object {

        /**
         * This method calls the LHDiff algorithm (`trackLine`)  on each single line of the group of lines
         * and matches on the  return of this method.
         */
        private fun trackEachLineIndividually(
            link: Link,
            linesToTrack: MutableList<Int>,
            fileChange: CustomChange,
            diffOutputList: List<DiffOutput>,
            originalLinesContents: List<String>
        ) {
            for ((index: Int, line: Int) in linesToTrack.withIndex()) {
                val result: LineChange =
                    trackLine(link, fileChange, originalLinesContents[index], diffOutputList, givenLineToTrack = line)
                when (result.lineChangeType) {
                    LineChangeType.DELETED -> linesToTrack[index] = -1
                    LineChangeType.MOVED -> linesToTrack[index] = result.newLine!!.lineNumber
                    // unchanged, do nothing
                    else -> Unit
                }
            }
        }

        /**
         * Tracks multiple lines across versions of a file.
         *
         * This is done by calling the `trackLine` method on each individual line and checking the output
         * of this method call.
         * If all of the outputs result in a new (single) group of consecutive lines, then classify this change
         * as a full move of the lines and return only a group of lines.
         *
         * There can also be the case where the lines have been split into multiple groups. In this case, the method
         * groups each consecutive sequence of lines an returns these groups, along with a partially moved change type.
         *
         * If all of the lines have been found to be deleted, then return a change type of deleted.
         */

        fun trackLines(
            link: Link,
            diffOutputList: List<DiffOutput>,
            fileChange: CustomChange,
            originalLinesContents: List<String>
        ): LinesChange {
            var linesToTrack: MutableList<Int> =
                (link.referencedStartingLine..link.referencedEndingLine).toMutableList()
            val originalLineNumbers: MutableList<Int> =
                (link.referencedStartingLine..link.referencedEndingLine).toMutableList()
            val contentHashMap: HashMap<Int, String> = hashMapOf()

            for (i: Int in 0 until originalLineNumbers.size) {
                contentHashMap[originalLineNumbers[i]] = originalLinesContents[i]
            }

            trackEachLineIndividually(link, linesToTrack, fileChange, diffOutputList, originalLinesContents)

            val changeMap: HashMap<Int, Int> = constructChangeMap(originalLineNumbers, linesToTrack)

            linesToTrack = linesToTrack.filter { lineNo -> lineNo != -1 }.toMutableList()
            linesToTrack = linesToTrack.distinct().toMutableList()
            linesToTrack.sort()

            if (linesToTrack.size == 0) {
                return LinesChange(fileChange, LinesChangeType.DELETED)
            }
            val trackResults: MutableList<MutableList<Int>> = groupConsecutiveNumbers(linesToTrack)
            val newLines: MutableList<MutableList<Line>> = groupTrackResults(trackResults, contentHashMap, changeMap)
            val linesChangeType: LinesChangeType = determineLinesChangeType(trackResults, changeMap)
            return LinesChange(fileChange, linesChangeType, newLines = newLines)
        }

        /**
         * Track a single line, throughout versions of a file.
         *
         * This method makes use of the LHDiff algorithm to be able to map a deleted line to an added line.
         *
         * This method will first calculate the sim-hash of the deleted line and of each added line, also the sim-hash
         * of the the context lines of these lines, it will calculate then the hamming distance of these sim-hashes
         * and create a candidate list of lines, which will then be passed to the `mapLine` method and
         * `detectLineSplit` method respectively.
         */

        fun trackLine(
            link: Link,
            fileChange: CustomChange,
            originalLineContent: String,
            diffOutputList: List<DiffOutput>,
            givenLineToTrack: Int = -1
        ): LineChange {
            val trimmedOriginalContent: String = originalLineContent.trim()
            var lineToTrack: Int
            lineToTrack = if (givenLineToTrack != -1) {
                givenLineToTrack
            } else {
                link.lineReferenced
            }
            var addedLines: MutableList<Line> = mutableListOf()
            var modifications = 0
            var lineIsDeleted = false

            for (diffOutput: DiffOutput in diffOutputList) {
                // remove leading / trailing spaces from line contents
                val deletedLines = removeTrailingSpacesFromLinesContents(diffOutput.deletedLines)
                addedLines = removeTrailingSpacesFromLinesContents(diffOutput.addedLines)
                // try to find from the deleted lines list a line which has the same line number
                // which we are looking for
                val deletedLine: Line? = deletedLines.find { line -> line.lineNumber == lineToTrack }

                // we found the line
                if (deletedLine != null) {
                    val result =
                        mapADeletedLineToAnAddedLine(modifications, lineToTrack, deletedLine, addedLines)
                    lineToTrack = result.first
                    modifications = result.second
                    lineIsDeleted = result.third
                    if (lineIsDeleted) break
                } else {
                    // line has not been deleted, but we still need to calculate the new location of the line
                    // based on the lines added / deleted before `lineToTrack`
                    val previousLineToTrack: Int = lineToTrack
                    lineToTrack = determineNewLocationOfUnchangedLine(deletedLines, addedLines, lineToTrack)
                    if (previousLineToTrack != lineToTrack) modifications++
                }
            }

            var line: Line? = addedLines.find { line -> line.lineNumber == lineToTrack }
            val lineChangeType: LineChangeType = when {
                lineIsDeleted -> LineChangeType.DELETED
                modifications == 0 || lineToTrack == link.lineReferenced -> LineChangeType.UNCHANGED
                else -> LineChangeType.MOVED
            }

            if (line == null) line = Line(lineToTrack, trimmedOriginalContent)
            return LineChange(fileChange, lineChangeType, newLine = line)
        }

        /**
         * For a deleted line from the before version of the file, constructs
         * a candidate list of the added lines, by calculating the hamming distance
         * between the sim-hashes of the two lines and of the context lines of each of the 2 lines.
         */
        private fun constructCandidateListForDeletedLine(deletedLine: Line, addedLines: MutableList<Line>): ArrayList<Pair<Line, Float>> {
            // join its context lines together in a string
            val joinedStringContextLines: String = getJoinedStringContextLines(line = deletedLine)

            // calculate the SimHash value for both the line contents and concatenated context lines
            val deletedLineContextSimHash: Long = getSimHash(line = joinedStringContextLines)
            val deletedLineContentSimHash: Long = getSimHash(line = deletedLine.content)
            // list of the added lines together with the calculated overall
            // hamming score
            val candidateList: ArrayList<Pair<Line, Float>> = arrayListOf()

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
                candidateList.add(Pair(line, overallScore))
            }
            return candidateList
        }

        /**
         * Maps a deleted line from the before version of the file to an added line
         * in the after version of the file. Loops over the candidate list constructed earlier,
         * and tries to find the best matching line to the deleted line, according to the Levenshtein distance
         * and to the Cosine distance of the context lines. If the best matching score exceeds a pre-defined threshold,
         * then a new mapped line is identified.
         *
         * The candidate list is then sorted in increasing order, as the lower the hamming distance
         * the greater the similarity between the lines.
         */
        private fun mapADeletedLineToAnAddedLine(
            modificationParameter: Int,
            lineToTrackParameter: Int,
            deletedLine: Line,
            addedLines: MutableList<Line>
        ): Triple<Int, Int, Boolean> {
            var lineIsDeleted = false
            var lineIsFound = false
            var modifications: Int = modificationParameter
            var lineToTrack: Int = lineToTrackParameter
            val candidateList = constructCandidateListForDeletedLine(deletedLine, addedLines)

            if (candidateList.size > 0) {
                // sort by the overall score in ascending order
                // lower scores mean that the two strings are more similar
                // e.g. a small hamming distance between 2 strings
                // will show that the 2 strings are similar
                candidateList.sortBy { pair -> pair.second }
                val result: Line? = mapLine(deletedLine, candidateList)
                if (result != null) {
                    modifications++
                    lineIsFound = true
                    // found a match in candidateList! Update `lineToTrack` to the matching
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
            }
            return Triple(lineToTrack, modifications, lineIsDeleted)
        }

        /**
         * For an unchanged line between the before version after version of a file,
         * calculate the new location of this line based on the amount of lines deleted and added
         * before it.
         */
        private fun determineNewLocationOfUnchangedLine(
            deletedLines: MutableList<Line>,
            addedLines: MutableList<Line>,
            lineToTrackParameter: Int
        ): Int {
            var lineToTrack: Int = lineToTrackParameter
            val useList: MutableList<Pair<String, Line>> = mutableListOf()
            val annotatedAddedLines = addedLines.map { el -> Pair("a", el) }.toMutableList()
            val annotatedDeletedLines = deletedLines.map { el -> Pair("d", el) }.toMutableList()
            annotatedAddedLines.addAll(annotatedDeletedLines)
            annotatedAddedLines.sortBy { el -> el.second.lineNumber }

            if (annotatedAddedLines.size == 1) {
                useList.add(annotatedAddedLines[0])
            } else if (annotatedAddedLines.size >= 2) {
                for (i: Int in 0 until annotatedAddedLines.size - 1 step 2) {
                    if (annotatedAddedLines[i].first == "a" && annotatedAddedLines[i + 1].first == "d"
                        && annotatedAddedLines[i + 1].second.lineNumber == annotatedAddedLines[i].second.lineNumber
                    ) {
                        continue
                    } else {
                        useList.add(annotatedAddedLines[i])
                        useList.add(annotatedAddedLines[i + 1])
                    }
                }
                if (annotatedAddedLines.size % 2 != 0) {
                    useList.add(annotatedAddedLines[annotatedAddedLines.size - 1])
                }
            }

            val previousLineToTrack: Int = lineToTrack
            for (changedLine in useList) {
                if (changedLine.first == "a" && changedLine.second.lineNumber <= lineToTrack) {
                    lineToTrack++
                } else if (changedLine.first == "d" && changedLine.second.lineNumber <= previousLineToTrack) {
                    lineToTrack--
                }
            }
            return lineToTrack
        }

        /**
         * Method that detects whether a deleted line has been split over a set of added lines.
         *
         * This is done by taking each added line, concatenating them into a joined string one-by-one
         * and calculating the Levenshtein similarity between the deleted line and this concatenated string,
         * as long as this similarity keeps increasing with each new concatenation.
         *
         * If a score over the `thresholdValue` parameter is found, then a deleted line is considered
         * as split over multiple lines.
         */
        private fun detectLineSplit(deletedLine: String, addedLines: List<Line>, thresholdValue: Float = 0.85f): Pair<Line?, Int> {
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

        /**
         * Main function that maps a deleted line to a set of added lines,
         * by using the parameterized threshold value as a value under which lines are considered delete,
         * and over which lines are consideres mapped (moved) respectively.
         */
        private fun mapLine(deletedLine: Line, candidateList: List<Pair<Line, Float>>, thresholdValue: Float = 0.65f): Line? {
            // initialize helper variables
            var mappingFound = false
            var bestMatch = 0.45F
            var bestLine: Line? = null

            // go over each pair in candidateList
            // this pair contains the added line accompanied by the hamming distance overall score
            // between the deleted line and this added line
            // candidateList is ordered according to overall score in INCREASING order!
            for (i: Int in candidateList.indices) {
                // calculate the levenshtein distance between the deleted line and currently inspected added line
                val scoreContent: Float = Levenshtein().compare(deletedLine.content, candidateList[i].first.content)
                val scoreContext: Float = calculateCosineSimilarityOfContext(i, deletedLine, candidateList)
                val score: Double = Math.round((0.6 * scoreContent + 0.4 * scoreContext) * 20.0) / 20.0

                // if the overall score is best so far, save the details
                if (score > bestMatch) {
                    mappingFound = true
                    bestLine = candidateList[i].first
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
    }
}
