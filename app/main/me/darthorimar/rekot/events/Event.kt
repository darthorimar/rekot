package me.darthorimar.rekot.events

import me.darthorimar.rekot.cells.CellId
import me.darthorimar.rekot.completion.CompletionPopup
import me.darthorimar.rekot.execution.CellExecutionState
import me.darthorimar.rekot.screen.ScreenSize

sealed interface Event {
    sealed interface Keyboard : Event {
        sealed interface Typing : Keyboard {
            data class TextTyping(val text: String) : Typing

            data class CharTyping(val char: Char) : Typing
        }

        data object Tab : Keyboard

        data object ShiftTab : Keyboard

        data object Enter : Keyboard

        data object Backspace : Keyboard

        data object Delete : Keyboard

        data object NewCell : Keyboard

        data object DeleteCell : Keyboard

        data object ClearCell : Keyboard

        data object RefreshScreen : Keyboard

        data object StopExecution : Keyboard

        data object ShowHelp : Keyboard

        data object Escape : Keyboard

        data object ExecuteCell : Keyboard

        data class ArrowButton(val direction: Direction) : Keyboard {
            enum class Direction {
                UP,
                DOWN,
                LEFT,
                RIGHT,
            }
        }
    }

    data class AfterCharTyping(val cellId: CellId, val char: Char) : Event

    data object CloseApp : Event

    data class ShowPopup(val popup: CompletionPopup) : Event

    data class CellTextChanged(val cellId: CellId) : Event

    data class CellCleared(val cellId: CellId) : Event

    data class CellExecutionStateChanged(val cellId: CellId, val state: CellExecutionState) : Event

    data class TerminalResized(val screenSize: ScreenSize) : Event

    data class Error(val cellId: CellId, val message: String) : Event
}
