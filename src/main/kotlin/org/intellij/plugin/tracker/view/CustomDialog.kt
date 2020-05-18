package org.intellij.plugin.tracker.view

import com.intellij.openapi.ui.DialogWrapper
import javax.swing.JComponent

class CustomDialog internal constructor(canBeParent: Boolean) : DialogWrapper(canBeParent) {

    public override fun createCenterPanel(): JComponent? {
        return CustomPanel()
    }

    init {
        init()
    }
}