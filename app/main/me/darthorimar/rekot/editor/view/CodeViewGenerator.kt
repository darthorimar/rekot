@file:Suppress("CONTEXT_RECEIVERS_DEPRECATED")

package me.darthorimar.rekot.editor.view

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import me.darthorimar.rekot.analysis.CellAnalysisContext
import me.darthorimar.rekot.analysis.CellAnalyzer
import me.darthorimar.rekot.app.AppComponent
import me.darthorimar.rekot.app.SubscriptionContext
import me.darthorimar.rekot.app.subscribe
import me.darthorimar.rekot.cells.Cell
import me.darthorimar.rekot.editor.view.EditorViewProvider.Companion.GAP
import me.darthorimar.rekot.events.Event
import me.darthorimar.rekot.psi.document
import me.darthorimar.rekot.style.*
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.api.symbols.KaBackingFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolLocation
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.koin.core.component.inject

class CodeViewGenerator : AppComponent {
    private val cellAnalyzer: CellAnalyzer by inject()
    private val styles: Styles by inject()
    private val colors: Colors by inject()

    private var userTyped = false

    context(SubscriptionContext)
    override fun performSubscriptions() {
        subscribe<Event.Keyboard> { userTyped = true }
    }

    @TestOnly
    fun setUserTyped() {
        userTyped = true
    }

    fun CellViewBuilder.code(cell: Cell) {
        cellAnalyzer.inAnalysisContext(cell) {
            codeLikeBlock(colors.EDITOR_BG) {
                if (!userTyped && cell.id.isInitialCell) {
                    codeLine {
                        from(styles.COMMENT)
                        string("// Write some Kotlin code here")
                    }
                    codeLine {
                        from(styles.COMMENT)
                        string("// Ctrl+E to execute, Ctrl+N to create a new cell. F1 to see all shortcuts")
                    }
                    return@codeLikeBlock
                }
                for (lineNumber in cell.lines.indices) {
                    val errorsForLine = errors.byLine(lineNumber)
                    val firstError = errorsForLine.firstOrNull()

                    codeLine {
                        for (element in getElementsAtLine(ktFile, lineNumber)) {
                            renderPsiElement(element)
                        }

                        firstError?.let { error -> glaze(styles.ERROR, error.colStart + GAP, error.colEnd + GAP) }
                    }

                    if (firstError != null) {
                        for ((i, errorLine) in firstError.message.lines().withIndex()) {
                            nonNavigatableLine {
                                styled {
                                    foregroundColor(colors.ERROR)
                                    gap(firstError.colStart)
                                    if (i == 0) {
                                        string("^ ")
                                    } else {
                                        string("  ")
                                    }
                                    string(errorLine)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    context(CellAnalysisContext)
    private fun StyledLineBuilder.renderPsiElement(element: Element) = styled {
        val psi = element.psiElement
        val parent = psi.parent
        val text = element.text

        val elementType = psi.node.elementType
        from(styles.CODE)
        when {
            KtTokens.KEYWORDS.contains(elementType) || KtTokens.SOFT_KEYWORDS.contains(elementType) -> {
                from(styles.KEYWORD)
            }

            elementType in KtTokens.STRINGS ||
                elementType == KtTokens.OPEN_QUOTE ||
                elementType == KtTokens.CLOSING_QUOTE -> {
                from(styles.STRING)
            }

            psi.parent is KtConstantExpression -> {
                from(styles.NUMBER)
            }

            KtTokens.COMMENTS.contains(elementType) -> {
                from(styles.COMMENT)
            }

            elementType == KtTokens.SHORT_TEMPLATE_ENTRY_START ||
                elementType == KtTokens.LONG_TEMPLATE_ENTRY_START ||
                elementType == KtTokens.LONG_TEMPLATE_ENTRY_END -> {
                from(styles.STRING_TEMPLATE_ENTRY)
            }

            parent is KtReferenceExpression -> {
                from(parent.computeStyle(currentStyle))
            }

            else -> {}
        }

        string(text)
    }

    context(CellAnalysisContext)
    private fun KtReferenceExpression.computeStyle(parentStyle: Style): Style =
        analyze {
            style(parentStyle) {
                when (val symbol = mainReference.resolveToSymbol()) {
                    is KaNamedFunctionSymbol -> {
                        if (symbol.isExtension) from(styles.EXTENSION_FUNCTION_CALL)
                        if (symbol.location == KaSymbolLocation.TOP_LEVEL) with(styles.TOP_LEVEL)
                    }

                    is KaBackingFieldSymbol -> from(styles.BACKING_FIELD_REFERENCE)

                    is KaPropertySymbol -> {
                        from(styles.PROPERTY)
                        if (!symbol.isVal) with(styles.MUTABLE)
                    }
                }
            }
        } ?: parentStyle

    private fun getElementsAtLine(psiFile: PsiFile, lineNumber: Int): List<Element> {
        val elements = mutableListOf<Element>()

        val document = psiFile.document

        if (lineNumber < 0 || lineNumber >= document.lineCount) {
            return emptyList()
        }

        val lineStartOffset = document.getLineStartOffset(lineNumber)
        val lineEndOffset = document.getLineEndOffset(lineNumber)
        val lineRange = TextRange(lineStartOffset, lineEndOffset)

        var currentElement: PsiElement? = psiFile.findElementAt(lineStartOffset)
        while (currentElement != null && currentElement.textRange.startOffset < lineEndOffset) {
            val elementRange = currentElement.textRange
            val intersection = elementRange.intersection(lineRange)
            elements.add(Element(currentElement, document.getText(intersection)))
            currentElement = PsiTreeUtil.nextLeaf(currentElement)
        }

        return elements
    }

    private class Element(val psiElement: PsiElement, val text: String)
}
