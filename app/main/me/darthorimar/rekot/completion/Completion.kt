package me.darthorimar.rekot.completion

import me.darthorimar.rekot.app.AppComponent
import me.darthorimar.rekot.app.SubscriptionContext
import me.darthorimar.rekot.app.subscribe
import me.darthorimar.rekot.cells.CellId
import me.darthorimar.rekot.cells.Cells
import me.darthorimar.rekot.events.Event
import org.koin.core.component.inject
import java.util.concurrent.Executors

class Completion : AppComponent {
    private val popupFactory: CompletionPopupFactory by inject()
    private val cells: Cells by inject()

    private val executor = Executors.newSingleThreadExecutor()
    private var _popup: CompletionPopup? = null
    private var currentSession: CompletionSession? = null

    context(SubscriptionContext)
    override fun performSubscriptions() {
        subscribe<Event.Keyboard.Typing.CharTyping> { e -> }

        subscribe<Event.ShowPopup> { showPopup(it.popup) }
        subscribe<Event.AfterCharTyping> { e ->
            currentSession?.stop()

            if (shouldShowCompletionPopupAfterTyping(e.char)) {
                val popup = _popup
                if (popup != null) {
                    if (!popup.addPrefix(e.char)) {
                        closePopup()
                    }
                } else {
                    scheduleShowingCompletionPopup(e.cellId)
                }
            } else {
                closePopup()
            }
        }
    }

    private fun scheduleShowingCompletionPopup(cellId: CellId) {
        val cell = cells.getCell(cellId)
        val cellText = cell.text
        val cellIndex = cell.id
        val cellCursor = cell.cursor

        val newSession = CompletionSession()
        currentSession = newSession

        executor.execute {
            val popup =
                try {
                    popupFactory.createPopup(cellIndex, cellText, cellCursor, newSession) ?: return@execute
                } catch (e: InterruptedCompletionException) {
                    return@execute
                }
            fireEvent(Event.ShowPopup(popup))
        }
    }

    private fun shouldShowCompletionPopupAfterTyping(char: Char): Boolean {
        return char.isLetterOrDigit() || char == '.'
    }

    val popup: CompletionPopup
        get() = _popup ?: error("Popup is not shown")

    val popupShown: Boolean
        get() = _popup != null

    fun choseItem() {
        ensurePopupShown()
        popup.choseItem()
        closePopup()
    }

    fun choseItemOnDot(): Boolean {
        ensurePopupShown()
        if (popup.choseItemOnDot()) {
            closePopup()
            return true
        }
        return false
    }

    fun closePopup() {
        _popup = null
    }

    fun showPopup(popup: CompletionPopup) {
        _popup = popup
    }

    fun up() {
        ensurePopupShown()
        popup.up()
    }

    fun down() {
        ensurePopupShown()
        popup.down()
    }

    private fun ensurePopupShown() {
        check(_popup != null) { "Popup is not shown" }
    }
}
