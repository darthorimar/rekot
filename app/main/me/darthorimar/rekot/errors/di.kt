package me.darthorimar.rekot.errors

import org.koin.dsl.module

val errorsModule = module { single { CellErrorProvider() } }
