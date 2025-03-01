package me.darthorimar.rekot.infra

import io.kotest.core.TestConfiguration
import me.darthorimar.rekot.app.AppComponent
import me.darthorimar.rekot.cells.Cells
import me.darthorimar.rekot.cursor.Cursor
import me.darthorimar.rekot.editor.Editor
import me.darthorimar.rekot.editor.view.CodeViewGenerator
import me.darthorimar.rekot.events.Event
import me.darthorimar.rekot.events.EventQueue
import me.darthorimar.rekot.projectStructure.ProjectStructure
import org.koin.test.KoinTest
import org.koin.test.get

interface AppTest : KoinTest {
    val editor
        get() = get<Editor>()

    val cells
        get() = get<Cells>()

    val queue
        get() = get<EventQueue>()

    val focusedCell
        get() = editor.focusedCell

    val cellText: String
        get() {
            val focused = focusedCell
            val cursor = focused.cursor
            return focused.lines
                .mapIndexed { index, line ->
                    if (index == cursor.row) line.replaceRange(cursor.column, cursor.column, "<C>") else line
                }
                .joinToString(separator = "\n")
        }

    fun TestConfiguration.initAppTest() {
        beforeEach {
            AppComponent.performSubscriptions()
            get<CodeViewGenerator>().setUserTyped()
        }
        afterEach { get<ProjectStructure>().shutdown() }
    }

    fun fireEvent(event: Event) {
        get<EventQueue>().fire(event)
    }

    @Suppress("TestFunctionName")
    fun Any?.P() {
        queue.processAllNonBlocking()
    }

    fun initCells(vararg texts: String) {
        check(cells.cells.isEmpty())
        for (text in texts) {
            initCell(text)
        }
    }

    fun initCell(text: String = "<C>") {
        editor.navigateToCell(cells.newCell())
        updateCellText(text)
    }

    fun updateCellText(text: String) {
        if (text.indexOf("<C>") < 0) {
            error("Caret position not defined")
        }

        val lines = mutableListOf<String>()
        var cursor: Cursor? = null
        for ((row, line) in text.split("\n").withIndex()) {
            val caretPosition = line.indexOf("<C>")
            if (caretPosition >= 0) {
                lines += line.replace("<C>", "")
                cursor = Cursor(row = row, column = caretPosition)
            } else {
                lines += line
            }
        }
        focusedCell.modify {
            setLines(lines)
            this.cursor.row = cursor!!.row
            this.cursor.column = cursor.column
        }
        queue.processAllNonBlocking()
    }
}
