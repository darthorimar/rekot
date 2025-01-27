package me.darthorimar.rekot.analysis

import me.darthorimar.rekot.execution.ExecutionResult
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule

class CompiledCell(
    val compiledLibraryModule: KaLibraryModule,
    val compiledCellProviderFactory: KotlinDeclarationProviderFactory,
    val executionResult: ExecutionResult,
    val classifiersDeclared: List<String>,
)
