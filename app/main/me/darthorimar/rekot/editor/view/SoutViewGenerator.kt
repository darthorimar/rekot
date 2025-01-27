package me.darthorimar.rekot.editor.view

import me.darthorimar.rekot.app.AppComponent
import me.darthorimar.rekot.cells.Cell
import me.darthorimar.rekot.execution.CellExecutionState
import me.darthorimar.rekot.execution.CellExecutionStateProvider
import me.darthorimar.rekot.style.Colors
import me.darthorimar.rekot.style.Styles
import org.koin.core.component.inject

class SoutViewGenerator : AppComponent {
    private val executionResultProvider: CellExecutionStateProvider by inject()
    private val styles: Styles by inject()
    private val colors: Colors by inject()

    fun CellViewBuilder.sout(cell: Cell) {
        val executionResult = executionResultProvider.getCellExecutionState(cell.id)
        if (executionResult !is CellExecutionState.Executed) return
        val sout = executionResult.result.sout ?: return

        header("Output")
        codeLikeBlock(colors.SOUT_BG) {
            for (soutLine in sout.split('\n').dropLastWhile { it.isEmpty() }) {
                navigatableLine {
                    from(styles.SOUT)
                    string(soutLine)
                }
            }
        }
    }
}
