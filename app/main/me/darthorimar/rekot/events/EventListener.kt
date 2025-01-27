package me.darthorimar.rekot.events

fun interface EventListener<E : Event> {
    fun onEvent(event: E)
}
