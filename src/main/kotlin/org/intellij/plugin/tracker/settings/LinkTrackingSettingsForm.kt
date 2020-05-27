package org.intellij.plugin.tracker.settings

import javax.swing.*

class LinkTrackingSettingsForm {
    private val mainPanel: JPanel? = null
    private val fileSimilaritySlider: JSlider? = null
    private val similarityThresholdLabel: JLabel? = null
    private val fileSimilarityLabel: JLabel? = null
    private val directorySimilarityLabel: JLabel? = null
    private val directorySimilaritySlider: JSlider? = null
    private val lineSimilarityLabel: JLabel? = null
    private val lineSimilaritySlider: JSlider? = null

    val component: JComponent?
        get() {
            initializeSliderValues(SimilarityThresholdSettings.getCurrentSimilarityThresholdSettings())
            return mainPanel
        }

    val similarityThresholdSettings: SimilarityThresholdSettings
        get() = SimilarityThresholdSettings(
            fileSimilaritySlider!!.value,
            directorySimilaritySlider!!.value,
            lineSimilaritySlider!!.value
        )

    private fun initializeSliderValues(similarityThresholdSettings: SimilarityThresholdSettings) {
        fileSimilaritySlider!!.value = similarityThresholdSettings.fileSimilarity
        directorySimilaritySlider!!.value = similarityThresholdSettings.directorySimilarity
        lineSimilaritySlider!!.value = similarityThresholdSettings.lineSimilarity
    }

    fun reset() {
        initializeSliderValues(SimilarityThresholdSettings.getCurrentSimilarityThresholdSettings())
    }
}
