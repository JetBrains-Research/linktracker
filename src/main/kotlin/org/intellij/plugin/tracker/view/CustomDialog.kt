package org.intellij.plugin.tracker.view

import com.intellij.openapi.ui.DialogWrapper
import org.jetbrains.annotations.NotNull
import javax.swing.Action
import javax.swing.JComponent

class CustomDialog internal constructor(canBeParent: Boolean) : DialogWrapper(canBeParent) {

    public override fun createCenterPanel(): JComponent? {
        return CustomPanel()
    }

    @NotNull
    override fun createActions(): Array<out Action> {
        super.createDefaultActions()
        return arrayOf()
    }

    init {
        init()
    }
}