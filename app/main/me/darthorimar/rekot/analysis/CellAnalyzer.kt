package me.darthorimar.rekot.analysis

import me.darthorimar.rekot.app.AppComponent
import me.darthorimar.rekot.cells.Cell
import me.darthorimar.rekot.cursor.Cursor
import me.darthorimar.rekot.projectStructure.KaCellScriptModule
import me.darthorimar.rekot.projectStructure.ProjectStructure
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.util.suffixIfNot
import org.koin.core.component.inject
import java.util.concurrent.ConcurrentHashMap

class CellAnalyzer : AppComponent {
    private val projectStructure: ProjectStructure by inject()
    private val compiledCellStorage: CompiledCellStorage by inject()

    private val contexts = ConcurrentHashMap<ID, CellAnalysisContext>()

    fun getAllCells() = buildList { addAll(contexts.values) }

    fun <R> inAnalysisContext(
        cell: Cell,
        text: String = cell.text,
        cursor: Cursor? = null,
        kind: CellContextKind = CellContextKind.Analysis,
        action: CellAnalysisContext.() -> R,
    ): R {
        return inAnalysisContext(cell.id.toString(), text, cursor, kind, action)
    }

    fun <R> inAnalysisContext(
        tag: String,
        text: String,
        cursor: Cursor?,
        kind: CellContextKind = CellContextKind.Analysis,
        action: CellAnalysisContext.() -> R,
    ): R {
        val context = createContext(tag, text, cursor, kind)
        val id = ID()
        contexts[id] = context
        try {
            return action(context)
        } finally {
            contexts.remove(id)
        }
    }

    private fun createContext(tag: String, text: String, cursor: Cursor?, kind: CellContextKind): CellAnalysisContext {
        val fileName =
            when (kind) {
                is CellContextKind.Analysis -> "CellForAnalysis$tag"
                is CellContextKind.Execution -> "CellForExecution${kind.executionNumber}"
            }
        val ktFile = createKtFile(text, fileName)
        val kaModule =
            createKaCellScriptModule(
                ktFile,
                resultVariableIndex = (kind as? CellContextKind.Execution)?.resultVariableIndex,
            )
        return CellAnalysisContext(ktFile, kaModule, cursor)
    }

    private fun createKaCellScriptModule(ktFile: KtFile, resultVariableIndex: Int?): KaCellScriptModule {
        val allCompiledCells = compiledCellStorage.allCompiledCells()
        val binaryDependencies = buildList {
            addAll(projectStructure.essentialLibraries.kaModules)
            allCompiledCells.mapTo(this) { it.compiledLibraryModule }
            add(projectStructure.builtins.kaModule)
        }
        return KaCellScriptModule(
                ktFile,
                projectStructure.kotlinCoreProjectEnvironment.project,
                allCompiledCells.map { it.executionResult.scriptInstance },
                binaryDependencies,
                imports = allCompiledCells.flatMap { it.classifiersDeclared },
                resultVariableName = resultVariableIndex?.let { "res$it" },
            )
            .also { projectStructure.projectStructureProvider.setModule(ktFile, it) }
    }

    private fun createKtFile(text: String, fileName: String): KtFile {
        val factory = KtPsiFactory(projectStructure.project, eventSystemEnabled = true)
        return factory.createFile(fileName.suffixIfNot(".kts"), text)
    }
}

sealed interface CellContextKind {
    data object Analysis : CellContextKind

    data class Execution(val executionNumber: Int, val resultVariableIndex: Int) : CellContextKind
}

private class ID()
