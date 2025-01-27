package me.darthorimar.rekot.completion

import org.koin.dsl.module

val completionModule = module {
    single { CompletionPopupFactory() }
    single { Completion() }
    single { CompletionItemFactory() }
    single { CompletionPopupRenderer() }
    single { CompletionItemSorter() }
}
