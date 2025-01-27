package me.darthorimar.rekot.help

import org.koin.dsl.module

val helpModule = module {
    single { HelpRenderer() }
    single { HelpWindow() }
}
