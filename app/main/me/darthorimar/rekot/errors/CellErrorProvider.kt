package me.darthorimar.rekot.errors

import me.darthorimar.rekot.app.AppComponent
import me.darthorimar.rekot.app.SubscriptionContext
import me.darthorimar.rekot.app.subscribe
import me.darthorimar.rekot.cells.CellId
import me.darthorimar.rekot.events.Event

class CellErrorProvider : AppComponent {
    private val errors = mutableMapOf<CellId, String>()

    context(SubscriptionContext)
    override fun performSubscriptions() {
        subscribe<Event.Error> { e -> errors[e.cellId] = e.message }
        subscribe<Event.CellTextChanged> { e -> clearError(e.cellId) }
        subscribe<Event.CellExecutionStateChanged> { e -> clearError(e.cellId) }
    }

    fun getCellError(cellId: CellId): String? {
        return errors[cellId]
    }

    private fun clearError(cellId: CellId) {
        errors.remove(cellId)
    }
}
