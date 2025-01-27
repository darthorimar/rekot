package me.darthorimar.rekot.editor

import me.darthorimar.rekot.editor.view.editorViewModule
import org.koin.dsl.module

val editorModule = module {
    includes(editorViewModule, editorViewModule)
    single { Editor() }
    single { FancyEditorCommands() }
}
