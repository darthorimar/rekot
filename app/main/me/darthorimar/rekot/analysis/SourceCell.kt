package me.darthorimar.rekot.analysis

import com.intellij.psi.PsiElement
import me.darthorimar.rekot.cursor.Cursor
import me.darthorimar.rekot.projectStructure.KaCellScriptModule
import me.darthorimar.rekot.psi.document
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.util.PrivateForInline
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@OptIn(PrivateForInline::class)
class CellAnalysisContext(val ktFile: KtFile, val cellKtModule: KaCellScriptModule, val analysisCursor: Cursor?) :
    KoinComponent {
    @PrivateForInline val safeAnalysisRunner: SafeAnalysisRunner by inject()
    @PrivateForInline val cellErrorComputer: CellErrorComputer by inject()

    val errors by lazy { cellErrorComputer.computeErrors() }

    fun getPsiElementAtCursor(): PsiElement? {
        val cursor = analysisCursor ?: error("Cursor is not available in this context")
        val offset = ktFile.document.getLineStartOffset(cursor.row) + cursor.column
        return ktFile.findElementAt(offset - 1)
    }

    fun PsiElement.getLineNumber(): Int {
        return ktFile.document.getLineNumber(textRange.startOffset)
    }

    /** Returns null in a case of exception during computation */
    inline fun <R> analyze(action: KaSession.() -> R): R? {
        return safeAnalysisRunner.runSafely { analyze(ktFile) { action() } }
    }
}
