package org.intellij.plugin.tracker.view

import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import java.awt.BorderLayout
import java.util.Vector
import javax.swing.JPanel
import javax.swing.table.DefaultTableModel
import org.intellij.plugin.tracker.data.FileChange
import org.intellij.plugin.tracker.data.Link

/**
 * Class creating summary method plugin view
 */
class MDView : JPanel() {
    private val table: JBTable = JBTable()
    private val model: DefaultTableModel = DefaultTableModel(
        Vector<Any?>(mutableListOf("Name", "Value")), 0)

    /**
     * Updating table view.
     */
    fun updateModel(fileChanges: MutableCollection<Pair<Link, FileChange>>) {
        val noOfRows = model.rowCount
        for (i in 0 until noOfRows) {
            model.removeRow(0)
        }
        for (change in fileChanges) {
            addRowModel("Link path", change.first.linkPath)
            addRowModel("Link text", change.first.linkText)
            addRowModel("Provenience path", change.first.proveniencePath)
            addRowModel("Found at line", change.first.foundAtLineNumber.toString())
            addRowModel("Link type", change.first.linkType.name)
            addRowModel("File name", change.second.fileName)
            addRowModel("Before path", change.second.beforePath)
            addRowModel("After path", change.second.afterPath)
            addRowModel("Move relative path", change.second.moveRelativePath)
            addRowModel("Change type", change.second.changeType)
        }
    }

    /**
     * Adds new row to model table
     */
    private fun addRowModel(name: String, value: String?) {
        val row: Vector<String> = Vector<String>()
        row.add(name)
        row.add(value)
        model.addRow(row)
    }

    /**
     * Constructor of class
     */
    init {
        table.model = model
        table.cellSelectionEnabled = false
        table.columnSelectionAllowed = false
        table.rowSelectionAllowed = false
        table.tableHeader.reorderingAllowed = false
        val scrollPane = JBScrollPane(table)
        layout = BorderLayout()
        add(scrollPane, BorderLayout.CENTER)
    }
}
