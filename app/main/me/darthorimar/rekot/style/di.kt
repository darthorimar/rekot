package me.darthorimar.rekot.style

import org.koin.dsl.module

val styleModule = module {
    single { StyleRenderer() }
    single { Styles() }
    single { Colors() }
}
