package org.intellij.plugin.tracker.settings

import com.intellij.ide.util.PropertiesComponent

/**
 * Data class responsible for the logic associated with the similarity threshold settings values, checking whether
 * the saved values are different from the ones currently present in the settings page and also saving them.
 */
data class SimilarityThresholdSettings(

    /**
     * File similarity threshold in the range from 0-100 (inclusive).
     */
    val fileSimilarity: Int,

    /**
     * Directory similarity threshold in the range from 0-100 (inclusive).
     */
    val directorySimilarity: Int,

    /**
     * Line(s) similarity threshold in the range from 0-100 (inclusive).
     */
    val lineSimilarity: Int
) {
    companion object {
        /**
         * Properties component service, which saves and also gets the saved similarity threshold values
         */
        private val propertiesComponent: PropertiesComponent = PropertiesComponent.getInstance()

        private const val FILE_SIMILARITY = "FILE_SIMILARITY"
        private const val DIRECTORY_SIMILARITY = "DIRECTORY_SIMILARITY"
        private const val LINE_SIMILARITY = "LINE_SIMILARITY"
        private const val DEFAULT_VALUE = "60"

        /**
         * Get the currently stored similarity threshold values and construct a new object of this class
         * that contains these values
         */
        fun getCurrentSimilarityThresholdSettings(): SimilarityThresholdSettings {
            val fileSimilarity: Int = propertiesComponent.getValue(FILE_SIMILARITY, DEFAULT_VALUE).toInt()
            val directorySimilarity: Int = propertiesComponent.getValue(DIRECTORY_SIMILARITY, DEFAULT_VALUE).toInt()
            val lineSimilarity: Int = propertiesComponent.getValue(LINE_SIMILARITY, DEFAULT_VALUE).toInt()
            return SimilarityThresholdSettings(fileSimilarity, directorySimilarity, lineSimilarity)
        }

        /**
         * This method is called when the apply button in the settings page is pressed
         *
         * It then saves each of the similarity threshold settings values.
         */
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

    /**
     * Checks whether the saved similarity threshold settings is different than
     * the ones that are currently present in the settings page
     */
    fun isModified(): Boolean = this != (getCurrentSimilarityThresholdSettings())
}
