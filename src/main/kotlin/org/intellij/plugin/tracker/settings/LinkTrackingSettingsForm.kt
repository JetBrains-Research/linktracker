package org.intellij.plugin.tracker.settings

import javax.swing.*

class LinkTrackingSettingsForm {
    private var mainPanel: JPanel? = null
    private var fileSimilaritySlider: JSlider? = null
    private var similarityThresholdLabel: JLabel? = null
    private var fileSimilarityLabel: JLabel? = null
    private var directorySimilarityLabel: JLabel? = null
    private var directorySimilaritySlider: JSlider? = null
    private var lineSimilarityLabel: JLabel? = null
    private var lineSimilaritySlider: JSlider? = null

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
