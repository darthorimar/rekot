package me.darthorimar.rekot.app

import me.darthorimar.rekot.events.Event

class AppState : AppComponent {
    private var _active: Boolean = true

    context(SubscriptionContext)
    override fun performSubscriptions() {
        subscribe<Event.CloseApp> { _active = false }
    }

    val active: Boolean
        get() = _active
}
