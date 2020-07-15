package org.intellij.plugin.tracker.settings

import com.intellij.openapi.options.SearchableConfigurable
import javax.swing.JComponent

/**
 * This class registers the main settings page of the plugin in the general settings of the project
 *
 * It corresponds to the page where a user sets the similarity threshold for certain elements being referenced,
 * as well as where the user can enable/disable the history traversal behaviour of the plugin or start fetch commit one.
 * This class also makes sure that these settings are saved on an application-level.
 */
class LinkTrackingConfigurable : SearchableConfigurable {

    /**
     * Class bound to the form displaying the settings page
     */
    private var linkTrackingSettingsForm: LinkTrackingSettingsForm? = null

    /**
     * Get the class bound to the form displaying the settings page
     */
    private fun getForm(): LinkTrackingSettingsForm {
        if (linkTrackingSettingsForm == null) {
            linkTrackingSettingsForm = LinkTrackingSettingsForm()
        }
        return linkTrackingSettingsForm!!
    }

    /**
     * Set the id name of this settings page
     */
    override fun getId(): String = "Settings.LinkTracking"

    /**
     * Set the display name of this settings page
     */
    override fun getDisplayName(): String = "Link Tracking"

    /**
     * Checks whether the settings page has any attributes that have been modified
     */
    override fun isModified(): Boolean = getForm().isModified

    /**
     * Logic to run when the apply button is clicked in the settings page
     */
    override fun apply() = getForm().saveValues()

    /**
     * Logic to run when the reset button is clicked in the settings page
     */
    override fun reset() = getForm().reset()

    /**
     * Creates the UI for this settings page
     */
    override fun createComponent(): JComponent? = getForm().component
}
