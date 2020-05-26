package org.intellij.plugin.tracker.settings;

import javax.swing.*;

public class LinkTrackingSettingsForm {
    private JPanel mainPanel;
    private JSlider fileSimilaritySlider;
    private JLabel similarityThresholdLabel;
    private JLabel fileSimilarityLabel;
    private JLabel directorySimilarityLabel;
    private JSlider directorySimilaritySlider;
    private JLabel lineSimilarityLabel;
    private JSlider lineSimilaritySlider;

    public JComponent getComponent() {
        initializeSliderValues(SimilarityThresholdSettings.Companion.getCurrentSimilarityThresholdSettings());
        return mainPanel;
    }

    private void initializeSliderValues(SimilarityThresholdSettings similarityThresholdSettings) {
        fileSimilaritySlider.setValue(similarityThresholdSettings.getFileSimilarity());
        directorySimilaritySlider.setValue(similarityThresholdSettings.getDirectorySimilarity());
        lineSimilaritySlider.setValue(similarityThresholdSettings.getLineSimilarity());
    }

    public void reset() {
        initializeSliderValues(SimilarityThresholdSettings.Companion.getCurrentSimilarityThresholdSettings());
    }

    public SimilarityThresholdSettings getSimilarityThresholdSettings() {
        return new SimilarityThresholdSettings(
                fileSimilaritySlider.getValue(),
                directorySimilaritySlider.getValue(),
                lineSimilaritySlider.getValue());
    }
}
