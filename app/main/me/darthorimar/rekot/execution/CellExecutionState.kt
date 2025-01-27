package me.darthorimar.rekot.execution

sealed interface CellExecutionState {
    data class Executed(val result: ExecutionResult) : CellExecutionState

    data object Executing : CellExecutionState

    data class Error(val errorMessage: String) : CellExecutionState
}
