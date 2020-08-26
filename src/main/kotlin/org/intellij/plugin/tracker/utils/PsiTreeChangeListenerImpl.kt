package org.intellij.plugin.tracker.utils

import com.intellij.psi.PsiTreeChangeAdapter
import com.intellij.psi.PsiTreeChangeEvent
import org.intellij.plugin.tracker.data.results.ScanResult

/**
 * A listener for the PsiTree that invalidates specific scan result links
 * if their containing file's Psi Tree is changed.
 */
object PsiTreeChangeListenerImpl : PsiTreeChangeAdapter() {

    /**
     * The latest scan results.
     */
    private lateinit var myScanResult: ScanResult

    fun updateResult(scanResult: ScanResult) {
        myScanResult = scanResult
    }

    /**
     * Called on any Psi change event,
     * invalidates any link in the scan results that belongs to the file affected by the event.
     */
    private fun react(event: PsiTreeChangeEvent) {
        val filePath = event.file!!.virtualFile.path
        if (myScanResult.myIsValid) {
            myScanResult.invalidateByFile(filePath)
        }
    }

    override fun beforeChildrenChange(event: PsiTreeChangeEvent) {
        react(event)
    }
}
