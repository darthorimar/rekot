package me.darthorimar.rekot.events

import org.koin.dsl.module

val eventModule = module {
    single { EventQueue() }
    single { KeyboardEventProcessor() }
}
