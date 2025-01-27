package me.darthorimar.rekot.editor

import me.darthorimar.rekot.app.AppComponent
import me.darthorimar.rekot.app.SubscriptionContext
import me.darthorimar.rekot.app.subscribe
import me.darthorimar.rekot.cells.Cell
import me.darthorimar.rekot.cells.CellModifier
import me.darthorimar.rekot.cells.Cells
import me.darthorimar.rekot.cursor.CursorModifierImpl
import me.darthorimar.rekot.editor.view.EditorView
import me.darthorimar.rekot.editor.view.EditorViewLine
import me.darthorimar.rekot.editor.view.EditorViewProvider
import me.darthorimar.rekot.events.Event
import me.darthorimar.rekot.execution.CellExecutionState
import me.darthorimar.rekot.execution.CellExecutionStateProvider
import me.darthorimar.rekot.screen.ScreenController
import me.darthorimar.rekot.util.Scroller
import org.koin.core.component.inject

class Editor : AppComponent {
    private val cells: Cells by inject()
    private val editorViewProvider: EditorViewProvider by inject()
    private val cellExecutionStateProvider: CellExecutionStateProvider by inject()
    private val screenController: ScreenController by inject()

    private val enterHandler = EnterHandler()
    private val typingHandler = TypingHandler()

    private val editorView: EditorView
        get() = editorViewProvider.view

    val focusedCell: Cell
        get() = currentLine.cell

    private val scroller = Scroller(screenController.screenSize.rows)
    val viewPosition: Int
        get() = scroller.viewPosition

    context(SubscriptionContext)
    override fun performSubscriptions() {
        subscribe<Event.TerminalResized> { e ->
            scroller.resize(e.screenSize.rows)
            cursor.updateMaxColumn(e.screenSize.columns)
        }
    }

    val cursor =
        CursorModifierImpl(row = 1, column = 0, maxColumn = screenController.screenSize.columns) { scroller.scroll(it) }

    private fun modify(action: CellModifier.() -> Unit) {
        if (currentLine is EditorViewLine.CodeLine) {
            focusedCell.modify(action)
        }
    }

    private val currentLine: EditorViewLine
        get() = editorView.lines[cursor.row]

    fun down() {
        while (true) {
            if (cursor.row == editorView.lines.size - 1) return
            cursor.row++
            val line = currentLine
            if (line !is EditorViewLine.NonNavigatableLine) {
                break
            }
        }
        adjustColumn()
    }

    fun up() {
        while (true) {
            if (cursor.row == 0) return
            cursor.row--
            val line = currentLine
            if (line !is EditorViewLine.NonNavigatableLine) {
                break
            }
        }
        adjustColumn()
    }

    private fun adjustColumn() {
        when (currentLine) {
            is EditorViewLine.CodeLine -> {
                modify { adjustColumn() }
            }

            is EditorViewLine.NavigatableLine -> {
                cursor.column = 0
            }

            is EditorViewLine.NonNavigatableLine -> {
                error("NonNavigatableLine should not be navigated 🙃")
            }
        }
    }

    fun left() {
        modify { left() }
    }

    fun right() {
        modify { right() }
    }

    fun delete() {
        modify { delete() }
    }

    fun backspace() {
        modify { backspace() }
    }

    fun enter() {
        modify { enterHandler.enter(this) }
    }

    fun type(char: Char) {
        modify { typingHandler.type(char, this) }
        fireEvent(Event.AfterCharTyping(focusedCell.id, char))
    }

    fun type(string: String) {
        modify { insert(string) }
    }

    fun offsetColumn(offset: Int) {
        modify { offsetColumn(offset) }
    }

    fun clearCell() {
        modify { clear() }
    }

    fun navigateToCell(newCell: Cell) {
        cursor.row = editorView.codeLineNumberToOffset(newCell, 0)
        cursor.column = 0
    }

    fun deleteCell() {
        if (cellExecutionStateProvider.getCellExecutionState(focusedCell.id) is CellExecutionState.Executing) {
            fireEvent(Event.Error(focusedCell.id, "Can't delete cell while it's executing"))
            return
        }
        if (cells.cells.size == 1) {
            fireEvent(Event.Error(focusedCell.id, "Can't delete the only cell"))
            return
        }
        val previousCell = cells.previousCell(focusedCell)
        cells.deleteCell(focusedCell)
        navigateToCell(previousCell)
    }

    fun tab() {
        modify { tab() }
    }

    fun shiftTab() {
        modify { removeTab() }
    }
}
