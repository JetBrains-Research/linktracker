package org.intellij.plugin.tracker.settings


import org.intellij.plugin.tracker.data.UserInfo
import org.intellij.plugin.tracker.utils.CredentialsManager

import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import java.util.*

class LinkTrackingTokenManagerForm {
    private var mainPanel: JPanel? = null
    private var mainTable: JTable? = null
    private var linksToCodeThatTextPane: JTextPane? = null
    private var model = DefaultTableModel()


    val component: JComponent?
        get() = mainPanel

    val savedState: List<UserInfo>
        get() {
            val list = ArrayList<UserInfo>()

            for (i in 0 until mainTable!!.rowCount) {
                val username = mainTable!!.getValueAt(i, 0) as String
                val platformName = mainTable!!.getValueAt(i, 2) as String
                val token = CredentialsManager.getCredentials(platformName, username)
                list.add(UserInfo(username, token, platformName))
            }
            return list
        }

    val currentState: List<UserInfo>
        get() {
            val list = ArrayList<UserInfo>()

            for (i in 0 until mainTable!!.rowCount) {
                val username = mainTable!!.getValueAt(i, 0) as String
                val token = mainTable!!.getValueAt(i, 1) as String
                val platform = mainTable!!.getValueAt(i, 2) as String

                list.add(UserInfo(username, token, platform))
            }
            return list
        }

    fun updateTable() {
        val list = savedState
        for (i in 0 until mainTable!!.rowCount) {
            val username = mainTable!!.getValueAt(i, 0) as String
            val platform = mainTable!!.getValueAt(i, 2) as String

            for ((username1, token, platform1) in list) {
                if (platform1 == platform && username1 == username) {
                    mainTable!!.setValueAt(token, i, 1)
                    break
                }
            }
        }
    }

    fun initializeTable(list: List<Pair<String, String>>) {
        model.addColumn(USERNAME_COLUMN)
        model.addColumn(TOKEN_COLUMN)
        model.addColumn(PLATFORM_COLUMN)
        mainTable!!.model = model

        if (list.size == 0) return

        val centerRenderer = DefaultTableCellRenderer()
        centerRenderer.horizontalAlignment = JLabel.CENTER

        for (i in 0 until mainTable!!.columnCount) {
            mainTable!!.getColumn(mainTable!!.getColumnName(i)).cellRenderer = centerRenderer
        }

        for ((first, second) in list) {
            val rowVector = Vector<Any>()
            rowVector.add(first)
            rowVector.add(CredentialsManager.getCredentials(second, first))
            rowVector.add(second)
            model.addRow(rowVector)
        }
    }

    companion object {

        private val USERNAME_COLUMN = "Username"
        private val TOKEN_COLUMN = "Token"
        private val PLATFORM_COLUMN = "Platform"
    }
}

