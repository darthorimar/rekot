package me.darthorimar.rekot.infra

import io.kotest.matchers.types.shouldBeTypeOf
import me.darthorimar.rekot.cells.Cell
import me.darthorimar.rekot.events.Event
import me.darthorimar.rekot.execution.CellExecutionState
import me.darthorimar.rekot.execution.CellExecutor
import me.darthorimar.rekot.execution.ExecutionResult
import org.koin.test.get

interface CellExecuting : AppTest {
    val cellExecutor
        get() = get<CellExecutor>()

    fun executeCell(cell: Cell): CellExecutionState {
        cellExecutor.execute(cell)
        return awaitCellExecution()
    }

    fun awaitCellExecution(): CellExecutionState {
        var event: Event?
        while (true) {
            Thread.sleep(10)
            event = queue.pollAndProcessSingle() ?: continue
            if (event is Event.CellExecutionStateChanged) {
                when (val state = event.state) {
                    is CellExecutionState.Error -> {
                        queue.pollAndProcessAll()
                        return state
                    }

                    is CellExecutionState.Executed -> {
                        queue.pollAndProcessAll()
                        return state
                    }

                    CellExecutionState.Executing -> continue
                }
            }
        }
    }

    fun executeAllCells(): List<CellExecutionState> {
        val results: MutableList<CellExecutionState> = mutableListOf()
        for (cell in cells.cells) {
            results += executeCell(cell)
        }
        return results
    }

    fun executeFocussedCell(): CellExecutionState {
        return executeCell(focusedCell)
    }

    val CellExecutionState.result: ExecutionResult
        get() {
            this.shouldBeTypeOf<CellExecutionState.Executed>()
            return this.result
        }

    val CellExecutionState.sout: String?
        get() = result.sout

    val CellExecutionState.error: String
        get() {
            this.shouldBeTypeOf<CellExecutionState.Error>()
            return this.errorMessage
        }

    val CellExecutionState.resultValue: Any?
        get() {
            val result = result
            result.shouldBeTypeOf<ExecutionResult.Result>()
            return result.value
        }
}
