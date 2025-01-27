package me.darthorimar.rekot.editor.view

import org.koin.dsl.module

val editorViewModule = module {
    single { EditorViewProvider() }
    single { CodeViewGenerator() }
    single { ResultViewGenerator() }
    single { SoutViewGenerator() }
    single { ErrorViewGenerator() }
}
