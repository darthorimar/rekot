package me.darthorimar.rekot.cells

import me.darthorimar.rekot.app.AppComponent
import me.darthorimar.rekot.app.SubscriptionContext
import me.darthorimar.rekot.app.subscribe
import me.darthorimar.rekot.config.AppConfig
import me.darthorimar.rekot.cursor.Cursor
import me.darthorimar.rekot.cursor.CursorModifierImpl
import me.darthorimar.rekot.editor.Editor
import me.darthorimar.rekot.editor.view.EditorViewProvider
import me.darthorimar.rekot.events.Event
import me.darthorimar.rekot.screen.ScreenController
import org.jetbrains.annotations.TestOnly
import org.koin.core.component.inject

class CellModifier(val cell: Cell) : AppComponent {
    private val screenController: ScreenController by inject()
    private val appConfig: AppConfig by inject()

    val cursor = CursorModifierImpl(0, 0, screenController.screenSize.columns)

    context(SubscriptionContext)
    override fun performSubscriptions() {
        subscribe<Event.TerminalResized> { e -> cursor.updateMaxColumn(e.screenSize.columns) }
    }

    val lines: MutableList<String> = mutableListOf("")

    fun up(): Boolean {
        if (cursor.row > 0) {
            cursor.row--
            adjustColumn()
            return true
        }
        return false
    }

    fun down(): Boolean {
        if (cursor.row < lines.lastIndex) {
            cursor.row++
            adjustColumn()
            return true
        }
        return false
    }

    fun left() {
        offsetColumn(-1)
    }

    fun right() {
        offsetColumn(+1)
    }

    fun offsetColumn(offset: Int) {
        var newColumn = cursor.column + offset
        while (newColumn < 0 || newColumn > currentLine.length) {
            if (newColumn < 0) {
                if (cursor.row > 0) {
                    cursor.row--
                    newColumn += currentLine.length + 1
                } else {
                    newColumn = 0
                }
            } else if (newColumn > currentLine.length) {
                if (cursor.row < lines.lastIndex) {
                    newColumn -= currentLine.length + 1
                    cursor.row++
                } else {
                    newColumn = currentLine.length
                }
            }
        }
        cursor.column = newColumn
    }

    val currentLine: String
        get() = lines[cursor.row]

    fun backspace() {
        removeChar(cursor.row, cursor.column - 1)

        if (cursor.column == 0 && cursor.row > 0) {
            cursor.row--
            cursor.column = currentLine.length
        } else if (cursor.column > 0) {
            cursor.column--
        }
    }

    fun delete() {
        removeChar(cursor.row, cursor.column)
    }

    fun removeChar(row: Int, column: Int) {
        if (column >= 0 && column < lines[row].length) {
            lines[row] = lines[row].removeRange(column, column + 1)
            if (lines[row].isEmpty() && lines.size > 1 && row != lines.lastIndex) {
                lines.removeAt(row)
            }
        }
        if (column == -1 && row > 0) {
            lines[row - 1] = lines[row - 1] + lines[row]
            lines.removeAt(row)
        }
    }

    fun insert(char: Char, cursorOffset: Int = 0) {
        lines[cursor.row] =
            lines[cursor.row].replaceRange(cursor.column + cursorOffset, cursor.column + cursorOffset, char.toString())
    }

    fun insert(text: String) {
        val textLines = text.split('\n')
        if (textLines.size == 1) {
            lines[cursor.row] = lines[cursor.row].replaceRange(cursor.column, cursor.column, text)
        } else {
            val currentLine = lines[cursor.row]
            lines[cursor.row] = currentLine.substring(0, cursor.column) + textLines[0]
            lines.addAll(cursor.row + 1, textLines.drop(1) + currentLine.substring(cursor.column))
        }
        offsetColumn(text.length)
    }

    fun insertNewLine(text: String, indent: Int = 0, offset: Int = 0) {
        lines.add(cursor.row + 1 + offset, " ".repeat(indent) + text)
    }

    fun insertNewLineFromStart(text: String, lineNumber: Int, indent: Int = 0) {
        lines.add(lineNumber, " ".repeat(indent) + text)
        if (lineNumber <= cursor.row) {
            cursor.row++
            adjustColumn()
        }
    }

    @TestOnly
    fun setLines(newLines: List<String>) {
        lines.clear()
        lines += newLines
        if (lines.isEmpty()) lines += ""
        cursor.resetToZero()
    }

    fun clear() {
        lines.clear()
        lines += ""
        cursor.resetToZero()
        fireEvent(Event.CellCleared(cell.id))
    }

    fun tab() {
        insert(" ".repeat(appConfig.tabSize))
    }

    fun removeTab() {
        val spacesInBeginning = currentLine.takeWhile { it == ' ' }.length
        val removeSpaces = minOf(appConfig.tabSize, spacesInBeginning)
        lines[cursor.row] = currentLine.removePrefix(" ".repeat(removeSpaces))
        adjustColumn()
    }

    fun adjustColumn() {
        cursor.column = cursor.column.coerceIn(0, currentLine.length)
    }
}

class Cell(val id: CellId) : AppComponent {
    private val editorViewProvider: EditorViewProvider by inject()
    private val editor: Editor by inject()
    private val modifier = CellModifier(this)

    val text: String
        get() = lines.joinToString("\n")

    val lines: List<String>
        get() = modifier.lines

    val cursor: Cursor
        get() {
            return modifier.cursor.asCursor()
        }

    fun <R> modify(modification: CellModifier.() -> R): R {
        editorCursorToCellCursor()
        val result = modification(modifier)
        cellCursorToEditorCursor()
        fireEvent(Event.CellTextChanged(id))
        return result
    }

    private fun cellCursorToEditorCursor() {
        editor.cursor.row = editorViewProvider.view.codeLineNumberToOffset(this@Cell, modifier.cursor.row)
        editor.cursor.column = modifier.cursor.column
    }

    private fun editorCursorToCellCursor() {
        modifier.cursor.row = editorViewProvider.view.offsetToCodeLineNumber(this@Cell, editor.cursor.row)
    }

    override fun toString(): String {
        return "Cell#${id}"
    }
}
