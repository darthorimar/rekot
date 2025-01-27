package me.darthorimar.rekot.app

import me.darthorimar.rekot.events.Event
import me.darthorimar.rekot.events.EventListener
import me.darthorimar.rekot.events.EventQueue
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.GlobalContext
import org.koin.core.definition.Kind
import kotlin.reflect.full.isSubclassOf

interface AppComponent : KoinComponent {
    context(SubscriptionContext)
    fun performSubscriptions() {}

    fun fireEvent(event: Event) {
        get<EventQueue>().fire(event)
    }

    companion object {
        fun performSubscriptions() {
            val context = object : SubscriptionContext {}
            for (subscriptable in allComponents()) {
                with(context) { subscriptable.performSubscriptions() }
            }
        }

        @OptIn(KoinInternalApi::class)
        private fun allComponents(): List<AppComponent> {
            val koin = GlobalContext.get()
            return koin.instanceRegistry.instances
                .map { it.value.beanDefinition }
                .filter { it.kind == Kind.Singleton }
                .filter { it.primaryType.isSubclassOf(AppComponent::class) }
                .map { koin.get(clazz = it.primaryType, qualifier = null, parameters = null) }
        }
    }
}

interface SubscriptionContext : KoinComponent

inline fun <reified E : Event> SubscriptionContext.subscribe(listener: EventListener<E>) {
    get<EventQueue>().subscribe<E>(listener)
}
