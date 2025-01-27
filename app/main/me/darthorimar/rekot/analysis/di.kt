package me.darthorimar.rekot.analysis

import org.koin.dsl.module

val analysisModule = module {
    single { CellErrorComputer() }
    single { CellAnalyzer() }
    single { CompiledCellStorage() }
    single { SafeAnalysisRunner() }
}
