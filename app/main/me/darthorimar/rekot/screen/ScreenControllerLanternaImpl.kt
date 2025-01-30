package me.darthorimar.rekot.screen

import com.googlecode.lanterna.screen.Screen
import com.googlecode.lanterna.terminal.Terminal
import me.darthorimar.rekot.app.SubscriptionContext
import me.darthorimar.rekot.cursor.Cursor
import me.darthorimar.rekot.events.Event

class ScreenControllerLanternaImpl(private val terminal: Terminal, private val screen: Screen) : ScreenController {

    context(SubscriptionContext)
    override fun performSubscriptions() {
        terminal.addResizeListener { _, _ ->
            screen.doResizeIfNecessary()
            fireEvent(Event.TerminalResized(screenSize))
        }
    }

    override fun fullRefresh() {
        screen.refresh(Screen.RefreshType.COMPLETE)
    }

    override fun refresh() {
        screen.refresh()
    }

    override val cursor: Cursor
        get() = Cursor(screen.cursorPosition.row, screen.cursorPosition.column)

    override val screenSize: ScreenSize
        get() = ScreenSize(screen.terminalSize.rows, screen.terminalSize.columns)
}
