package me.darthorimar.rekot.completion

import me.darthorimar.rekot.analysis.SafeAnalysisRunner
import me.darthorimar.rekot.app.AppComponent
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.types.impl.KaTypeRendererForSource
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaFunctionSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableSignature
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.types.Variance
import org.koin.core.component.inject

class CompletionItemFactory : AppComponent {
    private val safeAnalysisRunner: SafeAnalysisRunner by inject()

    context(KaSession)
    fun createCompletionItem(symbol: KaDeclarationSymbol): CompletionItem.Declaration? {
        return safeAnalysisRunner.runSafely {
            createCompletionItem(textToShow = renderSymbol(symbol) ?: return null, symbol = symbol)
        }
    }

    context(KaSession)
    fun createCompletionItem(signature: KaCallableSignature<*>): CompletionItem.Declaration? {
        val symbol = signature.symbol
        return safeAnalysisRunner.runSafely {
            createCompletionItem(textToShow = renderSignature(signature) ?: return null, symbol = symbol)
        }
    }

    context(KaSession)
    private fun createCompletionItem(textToShow: String, symbol: KaDeclarationSymbol): CompletionItem.Declaration? {
        if (textToShow.contains(COMPLETION_FAKE_IDENTIFIER)) return null
        val (textToInsert, offset) = getInsertionText(symbol) ?: return null
        return declarationCompletionItem {
            this.show = textToShow
            this.insert = textToInsert
            tag = getCompletionItemTag(symbol)
            moveCaret = offset
            location = symbol.location
            fqName = symbol.importableFqName?.asString()
            name = symbol.name?.asString() ?: ""
        }
    }

    private fun getCompletionItemTag(symbol: KaDeclarationSymbol): CompletionItemTag {
        return when (symbol) {
            is KaFunctionSymbol -> CompletionItemTag.FUNCTION
            is KaPropertySymbol -> CompletionItemTag.PROPERTY
            is KaClassLikeSymbol -> CompletionItemTag.CLASS
            else -> CompletionItemTag.LOCAL_VARIABLE
        }
    }

    context(KaSession)
    private fun getInsertionText(declaration: KaDeclarationSymbol): Pair<String, Int>? {
        return when {
            declaration is KaFunctionSymbol -> {
                val name = declaration.name?.asString() ?: return null
                when {
                    declaration.hasSingleFunctionTypeParameter() -> "$name {   }" to -3

                    declaration.valueParameters.isNotEmpty() -> "$name()" to -1
                    else -> "$name()" to 0
                }
            }

            else -> declaration.name?.asString()?.let { it to 0 }
        }
    }

    context(KaSession)
    private fun KaFunctionSymbol.hasSingleFunctionTypeParameter(): Boolean {
        val singleParameter = valueParameters.singleOrNull() ?: return false
        val kind = singleParameter.returnType.functionTypeKind ?: return false
        return kind == FunctionTypeKind.Function || kind == FunctionTypeKind.SuspendFunction
    }

    context(KaSession)
    private fun renderSymbol(symbol: KaSymbol): String? =
        when (symbol) {
            is KaFunctionSymbol -> renderSignature(symbol.asSignature())
            is KaVariableSymbol -> renderSignature(symbol.asSignature())
            is KaClassLikeSymbol -> renderClassLikeSymbol(symbol)

            else -> null
        }

    context(KaSession)
    private fun renderClassLikeSymbol(symbol: KaClassLikeSymbol): String? {
        val name = symbol.name?.asString() ?: return null
        val typeParameters = symbol.typeParameters
        val typeParametersText =
            if (typeParameters.isNotEmpty()) {
                typeParameters.joinToString(prefix = "<", postfix = ">") { it.name.asString() }
            } else ""
        return "$name$typeParametersText"
    }

    context(KaSession)
    private fun renderSignature(symbol: KaCallableSignature<*>): String? =
        when (symbol) {
            is KaFunctionSignature<*> ->
                buildString {
                    append(symbol.symbol.name?.asString() ?: return null)
                    append("(")
                    symbol.valueParameters.forEachIndexed { i, p ->
                        append(p.name.asString())
                        append(": ")
                        append(p.returnType.render(KaTypeRendererForSource.WITH_SHORT_NAMES, Variance.IN_VARIANCE))
                        if (i != symbol.valueParameters.lastIndex) {
                            append(", ")
                        }
                    }
                    append("): ")
                    append(symbol.returnType.render(KaTypeRendererForSource.WITH_SHORT_NAMES, Variance.OUT_VARIANCE))
                }

            is KaVariableSignature<*> ->
                buildString {
                    append(symbol.name.asString())
                    append(": ")
                    append(symbol.returnType.render(KaTypeRendererForSource.WITH_SHORT_NAMES, Variance.OUT_VARIANCE))
                }
        }
}
