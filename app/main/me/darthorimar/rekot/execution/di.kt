package me.darthorimar.rekot.execution

import org.koin.dsl.module

val executionModule = module {
    single { CellExecutionStateProvider() }
    single { CellExecutor() }
    single { ConsoleInterceptor() }
}
