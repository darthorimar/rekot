package me.darthorimar.rekot.analysis

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiErrorElement
import me.darthorimar.rekot.app.AppComponent
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaDiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticWithPsi
import org.jetbrains.kotlin.analysis.api.diagnostics.KaSeverity
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

class CellErrorComputer : AppComponent {

    context(CellAnalysisContext)
    fun computeErrors(): CellErrors {
        val errors = buildList {
            addAll(computeSyntaxErrors(ktFile))
            addAll(computeSemanticErrors(ktFile) ?: return CellErrors.EMPTY)
        }
        return CellErrors(errors.groupBy { it.lineNumber })
    }

    private fun computeSyntaxErrors(ktFile: KtFile): List<CellError> {
        val syntaxErrors = ktFile.collectDescendantsOfType<PsiErrorElement>()
        return syntaxErrors.map { error -> error.toCellError(ktFile) }
    }

    /*
     * Returns null in a case of error during computation
     */
    context(CellAnalysisContext)
    private fun computeSemanticErrors(ktFile: KtFile): List<CellError>? {
        return analyze {
            ktFile.collectDiagnostics(KaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS).mapNotNull { error ->
                toCellError(error, ktFile)
            }
        }
    }

    private fun PsiErrorElement.toCellError(ktFile: KtFile): CellError {
        val range = textRange.toEditorRange(ktFile)
        return CellError(
            lineNumber = range.lineNumber,
            colStart = range.colStart,
            colEnd = range.colEnd,
            message = errorDescription,
        )
    }

    context(KaSession)
    private fun toCellError(error: KaDiagnosticWithPsi<*>, ktFile: KtFile): CellError? {
        if (error.severity != KaSeverity.ERROR) return null
        val range = error.textRanges.firstOrNull()?.toEditorRange(ktFile) ?: error.psi.textRange.toEditorRange(ktFile)
        return CellError(
            lineNumber = range.lineNumber,
            colStart = range.colStart,
            colEnd = range.colEnd,
            message = error.defaultMessage,
        )
    }

    private fun TextRange.toEditorRange(psiFile: KtFile): Range {
        val document = psiFile.viewProvider.document
        val startLineNumber = document.getLineNumber(startOffset)
        val endLineNumber = document.getLineNumber(endOffset)

        return Range(
            lineNumber = startLineNumber,
            colStart = startOffset - document.getLineStartOffset(startLineNumber),
            colEnd =
                if (startLineNumber == endLineNumber) endOffset - document.getLineStartOffset(startLineNumber)
                else document.getLineEndOffset(startLineNumber),
        )
    }

    private class Range(val lineNumber: Int, val colStart: Int, val colEnd: Int)
}
