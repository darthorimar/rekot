package me.darthorimar.rekot.execution

import me.darthorimar.rekot.app.AppComponent
import me.darthorimar.rekot.app.SubscriptionContext
import me.darthorimar.rekot.app.subscribe
import me.darthorimar.rekot.cells.CellId
import me.darthorimar.rekot.events.Event

class CellExecutionStateProvider : AppComponent {
    private val states = mutableMapOf<CellId, CellExecutionState>()

    context(SubscriptionContext)
    override fun performSubscriptions() {
        subscribe<Event.CellExecutionStateChanged> { e ->
            states[e.cellId] = e.state
            if (e.state is CellExecutionState.Error) {
                fireEvent(Event.Error(e.cellId, e.state.errorMessage))
            }
        }
        subscribe<Event.CellCleared> { e -> states.remove(e.cellId) }
    }

    fun getCellExecutionState(cellId: CellId): CellExecutionState? {
        return states[cellId]
    }
}
