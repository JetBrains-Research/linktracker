package org.intellij.plugin.tracker.services

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import java.sql.Timestamp

/**
 * @author Tommaso Brandirali
 *
 * Service that serves the purpose of storing data about previous runs of the plugin
 * on disk, in XML format.
 */
@State(name = "HistoryServiceData", storages = [Storage("historyServiceData.xml")])
class HistoryService(val project: Project) : PersistentStateComponent<HistoryService.State> {

    private val defaultTimestamp = Timestamp(System.currentTimeMillis()).time
    private var stateObject = State()

    data class State(var TIMESTAMP_BY_FILE_AND_LINK: HashMap<String, HashMap<String, Long>> = hashMapOf())

    /**
     * Called by IDEA to get the current state of this service, so that it can
     * be saved to persistence.
     */
    override fun getState(): State {
        return stateObject
    }

    private fun cleanup(state: State) {
        val linkInfoMapByProvPath = LinkRetrieverService.getInstance(project)
            .getLinksInProjectScope()
            .groupBy { l -> l.proveniencePath }
            .mapValues { l -> l.value.map { el -> el.linkPath }}
        val it = state.TIMESTAMP_BY_FILE_AND_LINK.iterator()
        while (it.hasNext()) {
            val entry = it.next()
            if (!linkInfoMapByProvPath.containsKey(entry.key)) {
                it.remove()
            } else {
                val it2 = entry.value.iterator()
                while (it2.hasNext()) {
                    val entry2 = it2.next()
                    if (!linkInfoMapByProvPath.getValue(entry.key).contains(entry2.key)) {
                        it2.remove()
                    }
                }
            }
        }
    }

    /**
     * Called by IDEA when new component state is loaded.
     *
     * @param state the state retrieved from disk
     */
    override fun loadState(state: State) {
        cleanup(state)
        stateObject = state
    }

    fun getTimestamp(markdownFilePath: String, linkPath: String): Long {
        val timestampByLinkMap = state.TIMESTAMP_BY_FILE_AND_LINK[markdownFilePath]
        if (timestampByLinkMap == null) {
            val newMap: HashMap<String, Long> = hashMapOf()
            newMap[linkPath] = defaultTimestamp
            state.TIMESTAMP_BY_FILE_AND_LINK[markdownFilePath] = newMap
            return defaultTimestamp
        } else {
            val savedTimestamp = timestampByLinkMap[linkPath]
            if (savedTimestamp == null) {
                timestampByLinkMap[linkPath] = defaultTimestamp
                return defaultTimestamp
            }
            return savedTimestamp
        }
    }

    fun saveTimestamp(markdownFilePath: String, linkPath: String, timestamp: Long) {
        state.TIMESTAMP_BY_FILE_AND_LINK[markdownFilePath]!![linkPath] = timestamp
    }

    /**
     * Used by IDEA to get a reference to the single instance of this class.
     */
    companion object {
        fun getInstance(project: Project): HistoryService = ServiceManager.getService(project, HistoryService::class.java)
    }
}
