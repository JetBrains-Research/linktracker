package org.intellij.plugin.tracker.core

import com.google.common.collect.HashMultiset
import org.simmetrics.metrics.CosineSimilarity
import org.simmetrics.metrics.Levenshtein
import java.util.*
import kotlin.collections.ArrayList


class LineTracker {

    companion object {

        fun trackLine(deletedLine: String, addedLines: List<String>, contextLines: List<List<String>>): Int {
            val deletedLineContextSimHash: Long = getSimHash(line = getContext(contextLines = contextLines[0]))
            val deletedLineContentSimHash: Long = getSimHash(line = deletedLine)
            val possibleList: ArrayList<Triple<String, String, Float>> = arrayListOf()
            var bestScore: Float = -1f

            for ((index: Int, line: String) in addedLines.withIndex()) {
                val simHashContextAddedLine: Long = getSimHash(line = getContext(contextLines[index + 1]))
                val simHashContentAddedLine: Long = getSimHash(line = line)
                val scoreOfContext: Int = hamming(deletedLineContextSimHash, simHashContextAddedLine)
                val scoreOfContent: Int = hamming(deletedLineContentSimHash, simHashContentAddedLine)
                val overallScore: Float = 0.40f * scoreOfContext + 0.60f * scoreOfContent

                if (overallScore >= bestScore) {
                    bestScore = overallScore
                    possibleList.add(Triple(deletedLine, line, overallScore))
                }
            }

            // TODO: find k closest neighbours.
            if (possibleList.size > 0) {
                val result: Int = mapLine(deletedLine, contextLines, possibleList)
                if (result != -1) return result
            } else {
                return -1
            }

            // no match until now.
            // try to see whether the line was split.
            return detectLineSplit(deletedLine, addedLines, contextLines, possibleList)
        }

        private fun detectLineSplit(
            deletedLine: String,
            addedLines: List<String>,
            contextLines: List<List<String>>,
            possibleList: List<Triple<String, String, Float>>
        ): Int {
            val thresholdValue = 0.85f
            val maxConcatenationLines = 8
            var bestScore = -1f
            var bestIndex = -1

            for ((index: Int, line: String) in addedLines.withIndex()) {
                val previousScore: Float = Levenshtein().compare(deletedLine, line)
                var concatenateString: String = line

                for (i: Int in 0 until contextLines[index + 1].size) {
                    concatenateString += contextLines[index + 1][i]
                    val currentScore: Float = Levenshtein().compare(
                        deletedLine, concatenateString
                    )

                    if (currentScore >= bestScore && currentScore >= thresholdValue) {
                        bestScore = currentScore
                        bestIndex = index
                    }

                    if (currentScore <= previousScore) {
                        break
                    }
                }
            }
            return bestIndex
        }

        private fun mapLine(
            deletedLine: String,
            contextLines: List<List<String>>,
            possibleList: List<Triple<String, String, Float>>
        ): Int {
            var mappingFound = false
            var bestScore = 0.45F
            var bestIndex = -1

            Collections.sort(possibleList, object : Comparator<Triple<String, String, Float>> {
                override fun compare(o1: Triple<String, String, Float>?, o2: Triple<String, String, Float>?): Int {
                    if (o2!!.third > o1!!.third) return -1
                    else if (o2.third < o1.third) return 1
                    return 0
                }
            })

            for (i: Int in 0 until possibleList.size) {
                val scoreContent: Float = Levenshtein().compare(deletedLine, possibleList[i].second)

                val multiSet1: HashMultiset<String> = HashMultiset.create()
                for (el: String in contextLines[0]) multiSet1.add(el)
                val multiSet2: HashMultiset<String> = HashMultiset.create()
                for (el: String in contextLines[i + 1]) multiSet2.add(el)

                val scoreContext: Float = CosineSimilarity<String>().compare(multiSet1, multiSet2)
                val score: Double = 0.6 * scoreContent + 0.4 * scoreContext
                if (score >= bestScore) {
                    mappingFound = true
                    bestIndex = i
                    bestScore = score.toFloat()
                }
            }
            return if (mappingFound) {
                bestIndex
            } else {
                -1
            }
        }

        private fun getContext(contextLines: List<String>): String {
            val stringBuilder = StringBuilder()
            for (j: Int in 0 until contextLines.size)
                if (!contextLines[j].matches(Regex("\\s+")) && !contextLines[j].trim().matches(Regex("\\{ | \\( | \\)| \\}")))
                    stringBuilder.append(contextLines[j])
            return stringBuilder.toString()
        }

        private fun hamming(first: Long, second: Long): Int {
            var x: Long = first xor second
            var setBits = 0L

            while (x > 0) {
                setBits += x and 1L
                x = x shr 1
            }
            return setBits.toInt()
        }

        private fun getSimHash(line: String, hashSize: Int = 32): Long {
            val bitArray = IntArray(hashSize) { 0 }
            val shingleList: ArrayList<String> = createShingles(line)
            var simHashValue: Long = 0

            for (shingle in shingleList) {
                val hashedShingle = JenkinsHash().hash(shingle.toByteArray())
                for (i in 0 until hashSize) {
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

        private fun ithBitSet(hashedShingle: Long, position: Int): Boolean {
            if (hashedShingle and (1L shl position) == 1L)
                return true
            return false
        }

        private fun createShingles(line: String, shingleSize: Int = 2): ArrayList<String> {
            val tokenList = ArrayList<String>()
            var replacedLine: String = line.replace(Regex("\\r\\n|\\n|\\r"), " ")
            replacedLine = replacedLine.replace(Regex("\\s+"), "")
            val stringBuilder: StringBuilder = StringBuilder().append(replacedLine)
            if (line.length % shingleSize != 0)
                replacedLine = stringBuilder.append("_".repeat(line.length % shingleSize)).toString()

            var i = 0
            while (i + shingleSize < replacedLine.length) {
                tokenList.add(replacedLine.substring(i, i + shingleSize))
                i++
            }
            return tokenList
        }
    }
}
