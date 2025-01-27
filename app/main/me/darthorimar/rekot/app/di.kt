package me.darthorimar.rekot.app

import me.darthorimar.rekot.analysis.analysisModule
import me.darthorimar.rekot.cells.cellsModule
import me.darthorimar.rekot.completion.completionModule
import me.darthorimar.rekot.editor.editorModule
import me.darthorimar.rekot.errors.errorsModule
import me.darthorimar.rekot.events.eventModule
import me.darthorimar.rekot.execution.executionModule
import me.darthorimar.rekot.help.helpModule
import me.darthorimar.rekot.projectStructure.projectStructureModule
import me.darthorimar.rekot.psi.psiModule
import me.darthorimar.rekot.style.styleModule
import org.koin.dsl.module

val appModule = module {
    includes(
        eventModule,
        completionModule,
        psiModule,
        cellsModule,
        editorModule,
        analysisModule,
        styleModule,
        projectStructureModule,
        executionModule,
        errorsModule,
        helpModule,
    )
    single { AppState() }
}
