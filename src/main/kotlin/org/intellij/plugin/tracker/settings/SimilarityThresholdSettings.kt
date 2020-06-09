package org.intellij.plugin.tracker.settings

import com.intellij.ide.util.PropertiesComponent

data class SimilarityThresholdSettings(
    val fileSimilarity: Int,
    val directorySimilarity: Int,
    val lineSimilarity: Int
) {
    companion object {
        private val propertiesComponent: PropertiesComponent = PropertiesComponent.getInstance()
        private const val FILE_SIMILARITY = "FILE_SIMILARITY"
        private const val DIRECTORY_SIMILARITY = "DIRECTORY_SIMILARITY"
        private const val LINE_SIMILARITY = "LINE_SIMILARITY"
        private const val DEFAULT_VALUE = "60"

        fun getCurrentSimilarityThresholdSettings(): SimilarityThresholdSettings {
            val fileSimilarity: Int = propertiesComponent.getValue(FILE_SIMILARITY, DEFAULT_VALUE).toInt()
            val directorySimilarity: Int = propertiesComponent.getValue(DIRECTORY_SIMILARITY, DEFAULT_VALUE).toInt()
            val lineSimilarity: Int = propertiesComponent.getValue(LINE_SIMILARITY, DEFAULT_VALUE).toInt()
            return SimilarityThresholdSettings(fileSimilarity, directorySimilarity, lineSimilarity)
        }

        fun saveSetValues(similarityThresholdSettings: SimilarityThresholdSettings) {
            propertiesComponent.setValue(
                FILE_SIMILARITY,
                "${similarityThresholdSettings.fileSimilarity}"
            )
            propertiesComponent.setValue(
                DIRECTORY_SIMILARITY,
                "${similarityThresholdSettings.directorySimilarity}"
            )
            propertiesComponent.setValue(
                LINE_SIMILARITY,
                "${similarityThresholdSettings.lineSimilarity}"
            )
        }
    }

    fun isModified(): Boolean = this != (getCurrentSimilarityThresholdSettings())
}
