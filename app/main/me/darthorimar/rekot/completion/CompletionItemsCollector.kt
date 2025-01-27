@file:Suppress("CONTEXT_RECEIVERS_DEPRECATED")

package me.darthorimar.rekot.completion

import me.darthorimar.rekot.analysis.SafeAnalysisRunner
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaCompletionExtensionCandidateChecker
import org.jetbrains.kotlin.analysis.api.components.KaExtensionApplicabilityResult
import org.jetbrains.kotlin.analysis.api.components.KaUseSiteVisibilityChecker
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.name
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDeclaration
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.contracts.contract

class CompletionItemsCollector(
    private val applicabilityChecker: KaCompletionExtensionCandidateChecker?,
    private val visibilityChecker: KaUseSiteVisibilityChecker,
    val nameFilter: (Name) -> Boolean,
    private val session: CompletionSession,
) : KoinComponent {
    private val factory: CompletionItemFactory by inject()
    private val safeAnalysisRunner: SafeAnalysisRunner by inject()

    private val items = mutableListOf<CompletionItem>()
    private val symbols = mutableSetOf<KaDeclarationSymbol>()

    context(KaSession)
    fun add(declaration: KtDeclaration?, modify: (DeclarationCompletionItemBuilder.() -> Unit)? = null) {
        add(declaration?.symbol, modify)
    }

    context(KaSession)
    @JvmName("addDeclarations")
    fun add(declarations: Iterable<KtDeclaration>, modify: (DeclarationCompletionItemBuilder.() -> Unit)? = null) {
        declarations.forEach { add(it, modify) }
    }

    context(KaSession)
    fun add(symbol: KaDeclarationSymbol?, modify: (DeclarationCompletionItemBuilder.() -> Unit)? = null) {
        when (symbol) {
            null -> {}
            in symbols -> {}

            is KaCallableSymbol -> {
                val substituted = symbol.asApplicableSignature()
                if (substituted != null) {
                    add(substituted, modify)
                } else if (!symbol.isExtension) {
                    _add(symbol, modify)
                }
            }

            else -> {
                _add(symbol, modify)
            }
        }
    }

    context(KaSession)
    @JvmName("addSymbols")
    fun add(symbols: Sequence<KaDeclarationSymbol>, modify: (DeclarationCompletionItemBuilder.() -> Unit)? = null) {
        symbols.forEach { add(it, modify) }
    }

    context(KaSession)
    fun add(signature: KaCallableSignature<*>?, modify: (DeclarationCompletionItemBuilder.() -> Unit)? = null) {
        _add(signature, modify)
    }

    context(KaSession)
    @JvmName("addSignatures")
    fun add(symbols: Sequence<KaCallableSignature<*>>, modify: (DeclarationCompletionItemBuilder.() -> Unit)? = null) {
        symbols.forEach { add(it, modify) }
    }

    context(KaSession)
    fun add(item: KtSingleValueToken, modify: (KeywordItemBuilder.() -> Unit)? = null) {
        session.interruptIfCancelled()
        val name = Name.identifier(item.value)
        if (!nameFilter(name)) return
        val element = keywordCompletionItem(item)

        items +=
            if (modify == null) element
            else
                keywordCompletionItem() {
                    with(element)
                    modify()
                }
    }

    context(KaSession)
    fun add(items: Iterable<KtSingleValueToken>, modify: (KeywordItemBuilder.() -> Unit)? = null) {
        items.forEach { add(it, modify) }
    }

    context(KaSession)
    @Suppress("FunctionName")
    private fun _add(symbol: KaDeclarationSymbol?, modify: (DeclarationCompletionItemBuilder.() -> Unit)?) {
        session.interruptIfCancelled()
        if (!acceptsSymbol(symbol)) return
        val item = factory.createCompletionItem(symbol) ?: return
        symbols += symbol
        items +=
            if (modify == null) item
            else {
                declarationCompletionItem {
                    with(item)
                    modify()
                }
            }
    }

    context(KaSession)
    @Suppress("FunctionName")
    private fun _add(signature: KaCallableSignature<*>?, modify: (DeclarationCompletionItemBuilder.() -> Unit)?) {
        session.interruptIfCancelled()
        if (!acceptsSymbol(signature?.symbol)) return

        val item = factory.createCompletionItem(signature) ?: return
        symbols += signature.symbol
        items +=
            if (modify == null) item
            else
                declarationCompletionItem {
                    with(item)
                    modify()
                }
    }

    context(KaSession)
    private fun acceptsSymbol(symbol: KaDeclarationSymbol?): Boolean {
        contract { returns(true) implies (symbol != null) }
        if (symbol == null) return false
        if (symbol in symbols) return false
        if (!visibilityChecker.isVisible(symbol)) {
            return false
        }
        if (symbol.name?.asString()?.contains(COMPLETION_FAKE_IDENTIFIER) == true) return false
        return true
    }

    context(KaSession)
    private fun KaCallableSymbol.asApplicableSignature(): KaCallableSignature<KaCallableSymbol>? {
        val checker = applicabilityChecker ?: return asSignature()
        return safeAnalysisRunner.runSafely {
            when (val applicability = checker.computeApplicability(this@asApplicableSignature)) {
                is KaExtensionApplicabilityResult.Applicable -> substitute(applicability.substitutor)
                else -> null
            }
        }
    }

    fun build(): Collection<CompletionItem> {
        return items
    }
}
