package me.darthorimar.rekot.editor

import me.darthorimar.rekot.analysis.CellAnalyzer
import me.darthorimar.rekot.app.AppComponent
import me.darthorimar.rekot.cells.Cell
import org.koin.core.component.inject

class FancyEditorCommands : AppComponent {
    private val analyzer: CellAnalyzer by inject()

    @Suppress("UnstableApiUsage")
    fun insertImport(cell: Cell, import: String) {
        val insertAfterLineIndex =
            analyzer.inAnalysisContext(cell) {
                val existingImports =
                    ktFile.importDirectives
                        .mapNotNull { it.importPath }
                        .filterNot { it.isAllUnder }
                        .mapTo(mutableSetOf()) { it.fqName.asString() }
                if (import in existingImports) return@inAnalysisContext null
                ktFile.importList?.imports?.lastOrNull()?.getLineNumber()?.let { it + 1 } ?: 0
            } ?: return

        cell.modify {
            insertNewLineFromStart("import $import", insertAfterLineIndex)
            if (insertAfterLineIndex == 0) {
                insertNewLineFromStart("", 1)
            }
        }
    }
}
