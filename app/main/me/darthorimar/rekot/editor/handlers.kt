package me.darthorimar.rekot.editor

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.nextLeafs
import com.intellij.psi.util.parents
import me.darthorimar.rekot.analysis.CellAnalyzer
import me.darthorimar.rekot.app.AppComponent
import me.darthorimar.rekot.cells.CellModifier
import me.darthorimar.rekot.config.AppConfig
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespace
import org.koin.core.component.inject

class EnterHandler : AppComponent {
    private val appConfig: AppConfig by inject()
    private val cellAnalyzer: CellAnalyzer by inject()

    fun enter(modifier: CellModifier) =
        cellAnalyzer.inAnalysisContext(modifier.cell, cursor = modifier.cell.cursor) {
            with(modifier) {
                val currentLine = currentLine
                lines[cursor.row] = currentLine.substring(0, cursor.column)
                val elementAtCursor = getPsiElementAtCursor()

                val indentLevel =
                    elementAtCursor?.let { indentLevel(it) * appConfig.tabSize }
                        ?: currentLine.takeWhile { it == ' ' }.length
                if (elementAtCursor == null || !tryEnterBetweenPairBraces(elementAtCursor, currentLine, indentLevel)) {
                    insertNewLine(currentLine.substring(cursor.column), indent = indentLevel)
                }
                down()
                cursor.column = indentLevel
            }
        }

    private fun CellModifier.tryEnterBetweenPairBraces(
        elementAtCursor: PsiElement,
        currentLine: String,
        indentLevel: Int,
    ): Boolean {
        val closingBrace = bracesTokenPairs[elementAtCursor.node.elementType]
        if (closingBrace != null &&
            elementAtCursor.getNextSiblingIgnoringWhitespace()?.node?.elementType == closingBrace) {
            insertNewLine("", indent = indentLevel)
            val leftover = currentLine.substring(cursor.column)
            if (leftover.isNotBlank()) {
                insertNewLine(leftover, indent = (indentLevel - appConfig.tabSize).coerceAtLeast(0), offset = 1)
            }

            return true
        }
        return false
    }

    private fun indentLevel(element: PsiElement) =
        element
            .parents(withSelf = true)
            .takeWhile { it !is KtBlockExpression || it.parent !is KtScript }
            .count { it is KtBlockExpression }
}

class TypingHandler : AppComponent {
    private val analyzer: CellAnalyzer by inject()

    fun type(char: Char, CellModifier: CellModifier) =
        with(CellModifier) {
            insert(char)
            analyzer.inAnalysisContext(CellModifier.cell, cursor = cell.cursor) {
                val elementAtCursor = getPsiElementAtCursor()
                if (elementAtCursor != null) {
                    if (char in bracketPairs) {
                        val closing = bracketPairs.getValue(char)
                        if (expectsClosingBrace(elementAtCursor, closing)) {
                            insert(closing, cursorOffset = 1)
                        }
                    }
                }
                cursor.column++
            }
        }

    private fun expectsClosingBrace(openingPsi: PsiElement, closing: Char) =
        openingPsi.nextLeafs.any { leaf ->
            if (leaf !is PsiErrorElement) return@any false
            val errorDescription = leaf.errorDescription
            // not the best way to check for this, but it's simple and works in the most cases :)
            errorDescription == "Expecting '${closing}'" ||
                errorDescription == "Expecting an expression" ||
                closing == '}' && errorDescription == "Missing '}"
        }
}

private val bracesTokenPairs =
    mapOf(
        KtTokens.LPAR to KtTokens.RPAR,
        KtTokens.LBRACE to KtTokens.RBRACE,
        KtTokens.LBRACKET to KtTokens.RBRACKET,
        KtTokens.OPEN_QUOTE to KtTokens.CLOSING_QUOTE,
    )

private val bracketPairs = mapOf('(' to ')', '{' to '}', '[' to ']', '"' to '"')
