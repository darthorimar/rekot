package me.darthorimar.rekot.events

import me.darthorimar.rekot.app.AppComponent
import me.darthorimar.rekot.app.AppState
import org.koin.core.component.inject
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.getValue
import kotlin.reflect.KClass

class EventQueue : AppComponent {
    private val queue = LinkedBlockingQueue<Event>()

    private val appState: AppState by inject()

    val byEventListeners = mutableListOf<Pair<KClass<out Event>, EventListener<*>>>()

    fun fire(event: Event) {
        queue.offer(event)
    }

    fun processAllBlocking() {
        processFirstBlocking()
        processAllNonBlocking()
    }

    fun processAllNonBlocking() {
        do {
            val event = queue.poll() ?: break
            notifyListener(event)
        } while (true)
    }

    fun processFirstBlocking(): Event? {
        val appState = appState
        while (appState.active) {
            // timeout to listen for the app shutdown
            val event = queue.poll(POLL_TIMEOUT, TimeUnit.MILLISECONDS) ?: continue
            notifyListener(event)
            return event
        }
        return null
    }

    fun processFirstNonBlocking(): Event? {
        val event = queue.poll() ?: return null
        notifyListener(event)
        return event
    }

    private fun notifyListener(event: Event) {
        /*snapshot of the list to avoid CCE on subscribing inside the listener*/
        val listenersSnapshot = byEventListeners.toList()
        for ((eventKClass, listener) in listenersSnapshot) {
            if (eventKClass.isInstance(event)) {
                (listener as EventListener<Event>).onEvent(event)
            }
        }
    }

    inline fun <reified E : Event> subscribe(listener: EventListener<E>) {
        byEventListeners.add(E::class to listener)
    }

    companion object {
        private const val POLL_TIMEOUT = 100L
    }
}
