package org.intellij.plugin.tracker.view

import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import java.awt.BorderLayout
import java.util.Vector
import javax.swing.JPanel
import javax.swing.table.DefaultTableModel

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
    fun updateModel(statistics: MutableList<Any>) {
        val noOfRows = model.rowCount
        for (i in 0 until noOfRows) {
            model.removeRow(0)
        }
        addRowModel("Number of Files", statistics[0])
        addRowModel("Number of Links", statistics[1])
        addRowModel("Number of Files with Links", statistics[2])
    }

    /**
     * Adds new row to model table
     */
    private fun addRowModel(name: String, value: Any?) {
        val row: Vector<String> = Vector<String>()
        row.add(name)
        row.add(value.toString())
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
