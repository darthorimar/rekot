@file:Suppress("CONTEXT_RECEIVERS_DEPRECATED")

package me.darthorimar.rekot.completion

import com.intellij.psi.util.parentOfType
import me.darthorimar.rekot.analysis.CellAnalysisContext
import me.darthorimar.rekot.analysis.CellAnalyzer
import me.darthorimar.rekot.app.AppComponent
import me.darthorimar.rekot.cells.CellId
import me.darthorimar.rekot.cursor.Cursor
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.scopes.KaTypeScope
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaDeclarationContainerSymbol
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.createJavaClassFinder
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.koin.core.component.inject

class CompletionPopupFactory : AppComponent {
    private val cellAnalyzer: CellAnalyzer by inject()

    fun createPopup(cellId: CellId, cellText: String, cursor: Cursor, session: CompletionSession): CompletionPopup? {
        val text = buildString {
            for ((i, line) in cellText.lines().withIndex()) {
                if (i == cursor.row) {
                    appendLine(line.replaceRange(cursor.column, cursor.column, COMPLETION_FAKE_IDENTIFIER))
                } else {
                    appendLine(line)
                }
            }
        }
        return cellAnalyzer.inAnalysisContext(
            cellId.toString(),
            text,
            cursor.modified { column += COMPLETION_FAKE_IDENTIFIER.length },
        ) {
            val element =
                getPsiElementAtCursor()?.parentOfType<KtElement>(withSelf = true) ?: return@inAnalysisContext null

            analyze {
                try {
                    createPopup(element, session, cellId)
                } catch (_: InterruptedCompletionException) {
                    null
                }
            }
        }
    }

    context(KaSession, CellAnalysisContext)
    private fun createPopup(element: KtElement, session: CompletionSession, cellId: CellId): CompletionPopup? {
        val position = getPosition(element) ?: return null
        val filter =
            when (val prefix = position.prefix) {
                null -> { _: Name -> true }
                else -> { name: Name -> matchesPrefix(prefix, name.asString()) }
            }

        val applicabilityChecker =
            when (position) {
                is CompletionPosition.AfterDot ->
                    createExtensionCandidateChecker(ktFile, position.nameExpression, position.receiver)

                is CompletionPosition.Identifier ->
                    createExtensionCandidateChecker(
                        ktFile,
                        element as KtSimpleNameExpression,
                        explicitReceiver = null,
                    )

                else -> null
            }
        val visibilityChecker =
            when (position) {
                is CompletionPosition.AfterDot ->
                    createUseSiteVisibilityChecker(ktFile.symbol, position.receiver, position.nameExpression)

                is CompletionPosition.Identifier -> createUseSiteVisibilityChecker(ktFile.symbol, null, element)

                is CompletionPosition.NestedType -> createUseSiteVisibilityChecker(ktFile.symbol, null, element)

                is CompletionPosition.Type -> createUseSiteVisibilityChecker(ktFile.symbol, null, element)
            }
        val collector = CompletionItemsCollector(applicabilityChecker, visibilityChecker, filter, session)
        with(collector) {
            when (position) {
                is CompletionPosition.AfterDot -> completeAfterDot(position, ktFile, element)
                is CompletionPosition.Identifier -> completeIdentifier(ktFile, element)
                is CompletionPosition.Type -> completeType(ktFile, element)
                is CompletionPosition.NestedType -> completeNestedType(position)
            }
        }

        val elements = collector.build().distinctBy { it.show }

        if (elements.isEmpty()) return null
        return CompletionPopup(cellId, elements, position.prefix ?: "")
    }

    context(KaSession)
    private fun CompletionItemsCollector.completeAfterDot(
        position: CompletionPosition.AfterDot,
        file: KtFile,
        element: KtElement,
    ) {
        when (val receiverTarget = position.receiver.mainReference?.resolveToSymbol()) {
            is KaClassSymbol -> {
                completeStaticMembers(receiverTarget)
                receiverTarget.staticDeclaredMemberScope.classifiers
                    .filterIsInstance<KaClassSymbol>()
                    .firstOrNull { it.classKind == KaClassKind.COMPANION_OBJECT }
                    ?.let { completeScope(it.combinedMemberScope) }
            }
            else -> {
                position.receiver.expressionType?.scope?.let { completeTypeScope(it) }

                if (position.prefix != null) {
                    val scopeContext = file.scopeContext(element)
                    for (scope in scopeContext.scopes) {
                        add(scope.scope.callables(nameFilter).filter { it.isExtension })
                    }
                }

                add(getKotlinDeclarationsFromIndex(nameFilter).filter { symbol ->
                    symbol is KaCallableSymbol && symbol.isExtension
                }) { withImport() }
            }
        }
    }

    context(KaSession)
    private fun CompletionItemsCollector.completeStaticMembers(kaClassSymbol: KaClassSymbol) {
        add(kaClassSymbol.staticDeclaredMemberScope.callables(nameFilter))
    }

    context(KaSession)
    private fun CompletionItemsCollector.completeIdentifier(file: KtFile, element: KtElement) {
        completeKeywords(element)
        completeScopeContext(file, element)
        for (declaration in getKotlinDeclarationsFromIndex(nameFilter)) {
            add(declaration) { withImport() }
        }
        completeJavaDeclarationsFromIndex(nameFilter)
    }

    context(KaSession)
    private fun CompletionItemsCollector.completeScopeContext(file: KtFile, element: KtElement) {
        val scopeContext = file.scopeContext(element)
        for (scope in scopeContext.scopes) {
            add(scope.scope.callables(nameFilter))
            add(scope.scope.classifiers(nameFilter))
        }
        for (implicitReceiver in scopeContext.implicitReceivers) {
            implicitReceiver.type.scope?.let { completeTypeScope(it) }
        }
    }

    context(KaSession)
    private fun CompletionItemsCollector.completeKeywords(element: KtElement) {
        add(listOf(KtTokens.TRUE_KEYWORD, KtTokens.FALSE_KEYWORD, KtTokens.NULL_KEYWORD))
        if (element.parent is KtScriptInitializer) {
            add(
                listOf(
                    KtTokens.VAL_KEYWORD,
                    KtTokens.VAR_KEYWORD,
                    KtTokens.FUN_KEYWORD,
                    KtTokens.CLASS_KEYWORD,
                    KtTokens.INTERFACE_KEYWORD,
                    KtTokens.OBJECT_KEYWORD,
                )) {
                    withSpace()
                }
        }
    }

    context(KaSession)
    private fun CompletionItemsCollector.completeJavaDeclarationsFromIndex(filter: (Name) -> Boolean) {
        val javaClassFinder = useSiteModule.project.createJavaClassFinder(analysisScope)
        for (packageName in popularJDKPackages) {
            for (name in javaClassFinder.knownClassNamesInPackage(packageName).orEmpty()) {
                if ('$' in name) continue
                val identifier = Name.identifierIfValid(name) ?: continue
                if (!filter(identifier)) continue
                val classId = ClassId(packageName, identifier)
                val javaClass = javaClassFinder.findClass(classId) ?: continue
                check(javaClass is JavaClassImpl)
                val psiClass = javaClass.psi
                val symbol = psiClass.namedClassSymbol ?: continue
                add(symbol) { withImport() }
            }
        }
    }

    context(KaSession)
    private fun getKotlinDeclarationsFromIndex(filter: (Name) -> Boolean): Sequence<KaDeclarationSymbol> {
        val declarationProvider =
            KotlinDeclarationProviderFactory.getInstance(useSiteModule.project)
                .createDeclarationProvider(analysisScope, useSiteModule)
        return sequence {
            for (packageName in declarationProvider.computePackageNamesWithTopLevelCallables()!!) {
                val packageFqName = FqName(packageName)
                for (name in declarationProvider.getTopLevelCallableNamesInPackage(packageFqName)) {
                    if (!filter(name)) continue
                    val callableId = CallableId(packageFqName, name)
                    declarationProvider.getTopLevelFunctions(callableId).forEach { yield(it.symbol) }
                    declarationProvider.getTopLevelProperties(callableId).forEach { yield(it.symbol) }
                }
            }

            for (packageName in declarationProvider.computePackageNamesWithTopLevelClassifiers()!!) {
                val packageFqName = FqName(packageName)
                for (name in declarationProvider.getTopLevelKotlinClassLikeDeclarationNamesInPackage(packageFqName)) {
                    if (!filter(name)) continue
                    val classId = ClassId(packageFqName, name)
                    declarationProvider.getClassLikeDeclarationByClassId(classId)?.let { yield(it.symbol) }
                }
            }
        }
    }

    context(KaSession)
    private fun CompletionItemsCollector.completeType(file: KtFile, element: KtElement) {
        for (scope in file.scopeContext(element).scopes) {
            add(scope.scope.classifiers(nameFilter))
        }
    }

    context(KaSession)
    private fun CompletionItemsCollector.completeNestedType(position: CompletionPosition.NestedType) {
        val classifiers =
            when (val symbol = position.receiver.referenceExpression?.mainReference?.resolveToSymbol()) {
                is KaTypeAliasSymbol -> symbol.expandedType.scope?.getClassifierSymbols(nameFilter)
                is KaDeclarationContainerSymbol -> symbol.combinedMemberScope.classifiers(nameFilter)
                else -> null
            } ?: return
        add(classifiers)
    }

    context(KaSession)
    private fun CompletionItemsCollector.completeTypeScope(scope: KaTypeScope) {
        add(scope.getCallableSignatures(nameFilter))
        add(scope.getClassifierSymbols(nameFilter))
    }

    context(KaSession)
    private fun CompletionItemsCollector.completeScope(scope: KaScope) {
        add(scope.callables(nameFilter))
        add(scope.classifiers(nameFilter))
    }

    context(KaSession)
    private fun getPosition(element: KtElement): CompletionPosition? =
        when (element) {
            is KtNameReferenceExpression -> {
                when (val parent = element.parent) {
                    is KtDotQualifiedExpression if parent.selectorExpression == element -> getPosition(parent)
                    is KtUserType if parent.referenceExpression == element -> getPositionByUserType(parent)
                    else -> CompletionPosition.Identifier(
                        element.getReferencedName().removeSuffix(COMPLETION_FAKE_IDENTIFIER)
                    )
                }
            }

            is KtQualifiedExpression -> {
                val selector = element.selectorExpression
                CompletionPosition.AfterDot(
                    (selector as? KtNameReferenceExpression)
                        ?.getReferencedName()
                        ?.removeSuffix(COMPLETION_FAKE_IDENTIFIER),
                    element.receiverExpression,
                )
            }

            is KtUserType -> getPositionByUserType(element)

            else -> null
        }

    private fun getPositionByUserType(element: KtUserType) =
        when (val qualifier = element.qualifier) {
            null -> CompletionPosition.Type(element.referencedName.orEmpty().removeSuffix(COMPLETION_FAKE_IDENTIFIER))
            else ->
                CompletionPosition.NestedType(
                    element.referencedName.orEmpty().removeSuffix(COMPLETION_FAKE_IDENTIFIER),
                    qualifier,
                )
        }

    private sealed interface CompletionPosition {
        val prefix: String?

        class Identifier(override val prefix: String) : CompletionPosition

        class AfterDot(override val prefix: String?, val receiver: KtExpression) : CompletionPosition {
            val nameExpression: KtSimpleNameExpression
                get() = (receiver.parent as KtQualifiedExpression).selectorExpression as KtSimpleNameExpression
        }

        class Type(override val prefix: String) : CompletionPosition

        class NestedType(override val prefix: String?, val receiver: KtUserType) : CompletionPosition
    }

    companion object {
        private val popularJDKPackages =
            listOf(
                    "java.lang",
                    "java.lang.annotation",
                    "java.lang.invoke",
                    "java.lang.module",
                    "java.lang.ref",
                    "java.lang.reflect",
                    "java.util",
                    "java.util.concurrent",
                    "java.util.concurrent.atomic",
                    "java.util.concurrent.locks",
                    "java.util.function",
                    "java.util.jar",
                    "java.util.logging",
                    "java.util.prefs",
                    "java.util.regex",
                    "java.util.random",
                    "java.util.spi",
                    "java.util.stream",
                    "java.util.zip",
                    "java.io",
                    "java.nio",
                    "java.nio.channels",
                    "java.nio.channels.spi",
                    "java.nio.charset",
                    "java.nio.charset.spi",
                    "java.nio.file",
                    "java.nio.file.attribute",
                    "java.nio.file.spi",
                    "java.net",
                    "java.net.spi",
                    "java.time",
                    "java.time.chrono",
                    "java.time.format",
                    "java.time.temporal",
                    "java.time.zone",
                    "java.text",
                    "java.text.spi",
                    "java.math",
                    "java.io",
                )
                .map { FqName(it) }
    }
}
