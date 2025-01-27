package me.darthorimar.rekot.cells

import me.darthorimar.rekot.app.AppComponent
import me.darthorimar.rekot.events.Event

class Cells : AppComponent {
    private val _cells = mutableListOf<Cell>()
    private val _cellsById = mutableMapOf<CellId, Cell>()

    private var freeCellId = 1

    val cells: List<Cell>
        get() = _cells

    fun newCell(): Cell {
        val newCellId = CellId(freeCellId)
        val cell = Cell(newCellId)
        freeCellId++
        _cells += cell
        _cellsById[newCellId] = cell
        fireEvent(Event.CellTextChanged(newCellId))
        return cell
    }

    fun getCell(cellId: CellId): Cell {
        return _cellsById.getValue(cellId)
    }

    fun previousCell(cell: Cell): Cell {
        require(cells.size > 1)
        val prevCellIndex =
            when (val index = _cells.indexOf(cell)) {
                0 -> 1
                else -> index - 1
            }
        return _cells[prevCellIndex]
    }

    fun deleteCell(cell: Cell) {
        _cells.remove(cell)
        _cellsById.remove(cell.id)
    }
}
