package me.darthorimar.rekot.editor.view

import me.darthorimar.rekot.cells.Cell
import me.darthorimar.rekot.cells.CellId
import me.darthorimar.rekot.style.StyledLine

class EditorView(private val cells: Map<Cell, CellView>) {
    val lines: List<EditorViewLine> = cells.values.flatMap { it.lines }

    private val offsets: Map<Cell, Int> = buildMap {
        var offset = 0
        for ((cell, lines) in cells) {
            put(cell, offset)
            offset += lines.lines.size
        }
    }

    fun cellOffset(cell: Cell): Int {
        return offsets.getValue(cell)
    }

    fun view(cell: Cell): CellView {
        return cells.getValue(cell)
    }

    fun codeLineNumberToOffset(cell: Cell, codeLineNumber: Int): Int {
        return cellOffset(cell) + view(cell).codeLineNumberToOffset(codeLineNumber)
    }

    fun offsetToCodeLineNumber(cell: Cell, offset: Int): Int {
        return view(cell).offsetToCodeLineNumber(offset - cellOffset(cell))
    }
}

class CellView(private val cellId: CellId, val lines: List<EditorViewLine>) {
    private val lineNumberToOffset = buildMap {
        var lineNumber = 0
        for ((i, line) in lines.withIndex()) {
            if (line is EditorViewLine.CodeLine) {
                put(lineNumber, i)
                lineNumber++
            }
        }
    }

    private val offsetToLineNumber = lineNumberToOffset.entries.associate { (k, v) -> v to k }

    fun codeLineNumberToOffset(lineNumber: Int): Int {
        return lineNumberToOffset[lineNumber]
            ?: error(
                "Can't find view line with for Cell#${cellId}, lineNumber: $lineNumber, available: ${lineNumberToOffset.entries}")
    }

    fun offsetToCodeLineNumber(offset: Int): Int {
        return offsetToLineNumber[offset]
            ?: error(
                "Can't find line number for Cell#$cellId, offset: $offset, available: ${offsetToLineNumber.entries}")
    }
}

sealed interface EditorViewLine {
    val line: StyledLine
    val cell: Cell

    data class CodeLine(override val line: StyledLine, override val cell: Cell) : EditorViewLine {
        override fun toString(): String {
            return " C $line"
        }
    }

    data class NavigatableLine(override val line: StyledLine, override val cell: Cell) : EditorViewLine {
        override fun toString(): String {
            return " M $line"
        }
    }

    data class NonNavigatableLine(override val line: StyledLine, override val cell: Cell) : EditorViewLine {
        override fun toString(): String {
            return "NN $line"
        }
    }
}
