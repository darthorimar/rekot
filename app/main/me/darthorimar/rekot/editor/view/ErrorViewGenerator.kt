package me.darthorimar.rekot.editor.view

import me.darthorimar.rekot.app.AppComponent
import me.darthorimar.rekot.cells.Cell
import me.darthorimar.rekot.errors.CellErrorProvider
import me.darthorimar.rekot.style.Colors
import org.koin.core.component.inject

class ErrorViewGenerator : AppComponent {
    private val cellErrorProvider: CellErrorProvider by inject()
    private val colors: Colors by inject()

    fun CellViewBuilder.error(cell: Cell) {
        when (val error = cellErrorProvider.getCellError(cell.id)) {
            is String -> {
                header("Error")
                codeLikeBlock(colors.ERROR_BG) {
                    for (line in error.lines()) {
                        navigatableLine {
                            foregroundColor(colors.DEFAULT)
                            string(line)
                        }
                    }
                }
            }
            null -> {}
        }
    }
}
