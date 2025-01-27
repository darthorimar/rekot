package me.darthorimar.rekot.events

import me.darthorimar.rekot.app.AppComponent
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.reflect.KClass

class EventQueue : AppComponent {
    private val queue = ConcurrentLinkedQueue<Event>()

    val byEventListeners = mutableListOf<Pair<KClass<out Event>, EventListener<*>>>()

    fun fire(event: Event) {
        queue.offer(event)
    }

    fun pollAndProcessAll(): Boolean {
        var event: Event?
        var empty = true
        do {
            event = queue.poll() ?: break
            empty = false
            notifyListener(event)
        } while (true)
        return !empty
    }

    fun pollAndProcessSingle(): Event? {
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
}
