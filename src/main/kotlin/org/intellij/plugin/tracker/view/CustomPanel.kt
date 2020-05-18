package org.intellij.plugin.tracker.view

import java.awt.Color
import javax.swing.*


class CustomPanel : JPanel() {

   private val jp2: JPanel = JPanel()
   private val b2: JButton

    init {
        jp2.background = Color.green
        b2 = JButton("DEBUGGING.")
        jp2.add(b2)
        add(jp2)
    }
}