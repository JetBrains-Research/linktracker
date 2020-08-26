package org.intellij.plugin.tracker.settings

import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.ui.components.CheckBox
import com.intellij.ui.layout.panel
import org.intellij.plugin.tracker.settings.FeatureSwitchSettings.Companion.getCurrentFeatureSwitchSettings
import org.intellij.plugin.tracker.settings.FeatureSwitchSettings.Companion.saveFeatureSwitchSettings
import javax.swing.JCheckBox
import javax.swing.JComponent

/**
 * This class registers the main settings page of the plugin in the general settings of the project
 *
 * It corresponds to the page where a user sets the similarity threshold for certain elements being referenced,
 * as well as where the user can enable/disable the history traversal behaviour of the plugin or start fetch commit one.
 * This class also makes sure that these settings are saved on an application-level.
 */
class LinkTrackingConfigurable : SearchableConfigurable {

    private var historyTraversalCheckbox: JCheckBox? = null
    private var isHistoryTraversalEnabled: Boolean = getCurrentFeatureSwitchSettings().historyTraversalSwitch

    private fun getFeatureSwitchSettings(): FeatureSwitchSettings = FeatureSwitchSettings(isHistoryTraversalEnabled)

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
    override fun isModified(): Boolean = getFeatureSwitchSettings().isModified()

    /**
     * Logic to run when the apply button is clicked in the settings page
     */
    override fun apply() {
        isHistoryTraversalEnabled = getFeatureSwitchSettings().historyTraversalSwitch
        historyTraversalCheckbox?.isSelected = getFeatureSwitchSettings().historyTraversalSwitch
        saveFeatureSwitchSettings(getFeatureSwitchSettings())
    }

    /**
     * Logic to run when the reset button is clicked in the settings page
     */
    override fun reset() {
        isHistoryTraversalEnabled = getCurrentFeatureSwitchSettings().historyTraversalSwitch
        historyTraversalCheckbox?.isSelected = getCurrentFeatureSwitchSettings().historyTraversalSwitch
    }

    /**
     * Creates the UI for this settings page
     */
    override fun createComponent(): JComponent? {
        return panel {
            titledRow("History Traversal Settings") { }
            row {
                historyTraversalCheckbox = CheckBox("History Traversal", isHistoryTraversalEnabled)
                row { historyTraversalCheckbox!!() }
                historyTraversalCheckbox!!.addActionListener{ isHistoryTraversalEnabled = historyTraversalCheckbox!!.isSelected }
            }
        }
    }
}


