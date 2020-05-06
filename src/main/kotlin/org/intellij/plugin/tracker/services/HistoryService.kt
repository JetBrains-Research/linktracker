package org.intellij.plugin.tracker.services

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import org.intellij.plugin.tracker.data.RunResult

/**
 * @author Tommaso Brandirali
 *
 * Service that serves the purpose of storing data about previous runs of the plugin
 * on disk, in XML format.
 */
@State(name = "HistoryServiceData", storages = [Storage("historyServiceData.xml")])
class HistoryService : PersistentStateComponent<HistoryService.State> {

    /**
     * Used by IDEA to get a reference to the single instance of this class.
     */
    companion object {
        fun getInstance(project: Project): HistoryService =
                ServiceManager.getService(project, HistoryService::class.java)

    }

    private var state = State()

    data class State(
            // The listed results are ordered from most recent to last recent.
            // New results are pushed to the front of the list.
            var resultsList: ArrayList<RunResult> = ArrayList()
    )

    /**
     * Called by IDEA to get the current state of this service, so that it can
     * be saved to persistence.
     */
    override fun getState(): State {
        return state
    }

    /**
     * Called by IDEA when new component state is loaded.
     *
     * @param state the state retrieved from disk
     */
    override fun loadState(state: State) {
        this.state = state
    }

    fun setResultsList(resultsList: ArrayList<RunResult>) {
        state.resultsList = resultsList
    }

    fun getResultsList(): ArrayList<RunResult> {
        return state.resultsList
    }

    /**
     * Returns true if there is data from any previous runs saved on disk, false otherwise.
     */
    fun hasHistory(): Boolean {
        return state.resultsList.size != 0
    }

    /**
     * Saves given statistics per project by updating state.
     *
     * @param results the data to save on disk
     */
    fun saveResults(results: RunResult) {
        state.resultsList.add(0, results)
    }
}
