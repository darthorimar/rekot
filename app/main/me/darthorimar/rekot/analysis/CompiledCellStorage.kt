package me.darthorimar.rekot.analysis

import me.darthorimar.rekot.app.AppComponent
import me.darthorimar.rekot.app.SubscriptionContext
import me.darthorimar.rekot.app.subscribe
import me.darthorimar.rekot.cells.Cell
import me.darthorimar.rekot.cells.CellId
import me.darthorimar.rekot.cells.Cells
import me.darthorimar.rekot.events.Event
import me.darthorimar.rekot.execution.CellExecutionState
import me.darthorimar.rekot.execution.ExecutionResult
import me.darthorimar.rekot.projectStructure.ProjectStructure
import me.darthorimar.rekot.projectStructure.createLibraryModule
import me.darthorimar.rekot.psi.CellPsiUtils
import org.jetbrains.kotlin.analysis.api.standalone.base.declarations.KotlinStandaloneDeclarationProviderFactory
import org.jetbrains.kotlin.psi.KtClassLikeDeclaration
import org.koin.core.component.inject

class CompiledCellStorage : AppComponent {
    private val projectStructure: ProjectStructure by inject()
    private val compiledCells = mutableListOf<CompiledCell>()
    private val psiFactory by inject<CellPsiUtils>()
    private val cells: Cells by inject()

    context(SubscriptionContext)
    override fun performSubscriptions() {
        subscribe<Event.CellExecutionStateChanged> { event ->
            val state = event.state as? CellExecutionState.Executed ?: return@subscribe
            updaterCompiledAnalyzableCell(event.cellId, state.result)
        }
    }

    private fun updaterCompiledAnalyzableCell(cellId: CellId, executedCell: ExecutionResult) {
        val kaLibraryModule =
            createLibraryModule(
                listOf(executedCell.classRoot),
                projectStructure.kotlinCoreProjectEnvironment,
                "CompiledCell#${cellId}, $executedCell",
                isSdk = false,
            )
        for (virtualFile in kaLibraryModule.virtualFiles) {
            projectStructure.projectStructureProvider.setModule(virtualFile, kaLibraryModule)
        }

        val compiledCellProviderFactory =
            KotlinStandaloneDeclarationProviderFactory(
                projectStructure.kotlinCoreProjectEnvironment.project,
                sourceKtFiles = emptyList(),
                binaryRoots = kaLibraryModule.virtualFiles,
                shouldBuildStubsForBinaryLibraries = true,
                skipBuiltins = true,
            )

        compiledCells +=
            CompiledCell(
                kaLibraryModule,
                compiledCellProviderFactory,
                executedCell,
                classifiersDeclared = getAllDefinedClassifiersImportableNames(cells.getCell(cellId), executedCell),
            )
    }

    private fun getAllDefinedClassifiersImportableNames(cell: Cell, executedCell: ExecutionResult): List<String> {
        val result = mutableListOf<String>()
        val file = psiFactory.createKtFile(cell, "Cell for Fq Names${cell.id}")
        for (declaration in file.script!!.declarations) {
            if (declaration is KtClassLikeDeclaration) {
                result += executedCell.scriptClassFqName + "." + declaration.name
            }
        }
        return result
    }

    fun allCompiledCells(): List<CompiledCell> {
        return compiledCells.reversed()
    }
}
