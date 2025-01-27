package me.darthorimar.rekot.editor.view

import me.darthorimar.rekot.analysis.CellAnalyzer
import me.darthorimar.rekot.app.AppComponent
import me.darthorimar.rekot.cells.Cell
import me.darthorimar.rekot.cursor.Cursor
import me.darthorimar.rekot.execution.CellExecutionState
import me.darthorimar.rekot.execution.CellExecutionStateProvider
import me.darthorimar.rekot.execution.ExecutionResult
import me.darthorimar.rekot.execution.ExecutorValueRenderer
import me.darthorimar.rekot.style.Colors
import me.darthorimar.rekot.style.Styles
import org.jetbrains.kotlin.analysis.api.renderer.types.impl.KaTypeRendererForSource
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.types.Variance
import org.koin.core.component.inject

class ResultViewGenerator : AppComponent {
    private val executionResultProvider: CellExecutionStateProvider by inject()
    private val cellAnalyzer: CellAnalyzer by inject()
    private val styles: Styles by inject()
    private val colors: Colors by inject()

    fun CellViewBuilder.result(cell: Cell) {
        when (val executionResult = executionResultProvider.getCellExecutionState(cell.id)) {
            is CellExecutionState.Error -> {}
            CellExecutionState.Executing -> {
                header("Executing...") {
                    italic()
                    string(" (Ctrl+B to cancel)")
                }
            }

            is CellExecutionState.Executed ->
                when (val result = executionResult.result) {
                    is ExecutionResult.Result -> {
                        header("Result")
                        codeLikeBlock(colors.EDITOR_BG) {
                            val valueRenderedLines = ExecutorValueRenderer.render(result.value).lines()
                            navigatableLine {
                                styled {
                                    from(styles.PROPERTY)
                                    string(result.resultVariableName)
                                }
                                styled {
                                    from(styles.CODE)
                                    val typeText = getResultVariableTypeRendered(result)
                                    if (typeText != null) {
                                        string(": $typeText")
                                    }
                                }
                                styled {
                                    from(styles.CODE)
                                    string(" = ")
                                    if (valueRenderedLines.size == 1) {
                                        string(valueRenderedLines.single())
                                    }
                                }
                            }
                            if (valueRenderedLines.size > 1) {
                                for (line in valueRenderedLines) {
                                    navigatableLine {
                                        styled {
                                            from(styles.CODE)
                                            gap((result.resultVariableName + " = ").length)
                                            string(line)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    is ExecutionResult.Void -> {
                        if (result.sout == null) {
                            header("Result")
                            codeLikeBlock(colors.EDITOR_BG) {
                                navigatableLine {
                                    from(styles.CODE)
                                    string("res = Unit")
                                }
                            }
                        }
                    }
                }

            null -> {}
        }
    }

    private fun getResultVariableTypeRendered(result: ExecutionResult.Result): String? {
        return cellAnalyzer.inAnalysisContext("res", result.resultVariableName, Cursor.zero()) {
            analyze {
                val reference = ktFile.collectDescendantsOfType<KtNameReferenceExpression>().single()
                val type = reference.expressionType ?: return@inAnalysisContext null
                type.render(KaTypeRendererForSource.WITH_SHORT_NAMES, Variance.INVARIANT)
            }
        }
    }
}
