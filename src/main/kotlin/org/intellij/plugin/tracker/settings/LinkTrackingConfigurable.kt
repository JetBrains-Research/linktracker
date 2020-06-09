package org.intellij.plugin.tracker.settings

import com.intellij.openapi.options.SearchableConfigurable
import javax.swing.JComponent

class LinkTrackingConfigurable : SearchableConfigurable {
    private var linkTrackingSettingsForm: LinkTrackingSettingsForm? = null

    private fun getForm(): LinkTrackingSettingsForm {
        if (linkTrackingSettingsForm == null) {
            linkTrackingSettingsForm = LinkTrackingSettingsForm()
        }
        return linkTrackingSettingsForm!!
    }

    override fun getId(): String = "Settings.LinkTracking"

    override fun getDisplayName(): String = "Link Tracking"

    override fun isModified(): Boolean = getForm().similarityThresholdSettings.isModified()

    override fun apply() = SimilarityThresholdSettings.saveSetValues(getForm().similarityThresholdSettings)

    override fun reset() = getForm().reset()

    override fun createComponent(): JComponent? = getForm().component
}
