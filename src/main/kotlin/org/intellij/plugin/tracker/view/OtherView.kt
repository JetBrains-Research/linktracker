package org.intellij.plugin.tracker.view

import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.WindowConstants
import javax.swing.tree.TreePath

/**
 * Class creating other view
 */
class OtherView : JPanel(BorderLayout()) {

    init {
        val contentPane = JPanel()
        val cbt = JCheckBoxTree()
        cbt.addCheckChangeEventListener(object : JCheckBoxTree.CheckChangeEventListener {
            override fun checkStateChanged(event: JCheckBoxTree.CheckChangeEvent?) {
                println("event")
                val paths: Array<TreePath> = cbt.getCheckedPaths()
                for (tp in paths) {
                    for (pathPart in tp.getPath()) {
                        print("$pathPart,")
                    }
                    println()
                }
            }
        })
        contentPane.layout = BorderLayout()
        contentPane.add(cbt)
        add(contentPane, BorderLayout.CENTER)
    }


}

