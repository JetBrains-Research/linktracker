package org.intellij.plugin.tracker.settings

import com.intellij.ide.util.PropertiesComponent
import org.intellij.plugin.tracker.core.change.ChangeTrackingPolicy

/**
 * Data class responsible for the logic associated with the feature switch settings values, checking whether
 * the saved values are different from the ones currently present in the settings page and also saving them.
 */
data class FeatureSwitchSettings(
    /**
     * Boolean indicating whether the plugin should perform a Git history traversal for each link
     */
    val historyTraversalSwitch: Boolean
) {

    companion object {
        /**
         * Properties component service, which saves and also gets the saved feature switch values
         */
        private val propertiesComponent: PropertiesComponent = PropertiesComponent.getInstance()

        private const val HISTORY_TRAVERSAL_SWITCH = "HISTORY_TRAVERSAL_SWITCH"
        private const val DEFAULT_VALUE = "false"

        /**
         * Get the currently stored feature switch values and construct a new object of this class
         * that contains these values
         */
        fun getCurrentFeatureSwitchSettings(): FeatureSwitchSettings {
            val historyTraversalSwitch: Boolean = propertiesComponent.getValue(
                HISTORY_TRAVERSAL_SWITCH,
                DEFAULT_VALUE
            ).toBoolean()
            return FeatureSwitchSettings(historyTraversalSwitch)
        }

        /**
         * This method is called when the apply button in the settings page is pressed
         *
         * It then saves each of the feature switch settings values.
         */
        fun saveFeatureSwitchSettings(featureSwitchSettings: FeatureSwitchSettings) {
            propertiesComponent.setValue(
                HISTORY_TRAVERSAL_SWITCH,
                "${featureSwitchSettings.historyTraversalSwitch}"
            )
        }

        /**
         * Get the currently saved history traversal switch
         */
        fun getSavedHistoryTraversalSwitch(): Boolean = getCurrentFeatureSwitchSettings().historyTraversalSwitch

        fun getCorrespondentChangeTrackingPolicy(): ChangeTrackingPolicy {
            if (getSavedHistoryTraversalSwitch()) {
                return ChangeTrackingPolicy.HISTORY
            }
            return ChangeTrackingPolicy.LOCAL
        }
    }

    /**
     * Checks whether the saved feature switch settings is different than
     * the ones that are currently present in the settings page
     */
    fun isModified(): Boolean = this != (getCurrentFeatureSwitchSettings())
}