package me.darthorimar.rekot.projectStructure

import com.intellij.psi.search.GlobalSearchScope
import me.darthorimar.rekot.analysis.CellAnalyzer
import me.darthorimar.rekot.analysis.CompiledCellStorage
import me.darthorimar.rekot.app.AppComponent
import me.darthorimar.rekot.projectStructure.index.BinariesDeclarationProviderFactory
import me.darthorimar.rekot.projectStructure.index.IndexFactory
import me.darthorimar.rekot.projectStructure.modules.KaJarLibraryModuleImpl
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinCompositeDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinFileBasedDeclarationProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.koin.core.component.inject
import java.util.concurrent.CompletableFuture

class ProjectDeclarationFactoryImpl() : KotlinDeclarationProviderFactory, AppComponent {
    private val cellAnalyzer: CellAnalyzer by inject()
    private val compiledCellStorage: CompiledCellStorage by inject()
    private val projectStructure: ProjectStructure by inject()
    private val indexFactory: IndexFactory by inject()

    private val _factoryFuture =
        CompletableFuture.supplyAsync {
            val kaModules = projectStructure.libraries
            val binaryRootsToIndex =
                kaModules.filterIsInstance<KaJarLibraryModuleImpl>().flatMapTo(mutableSetOf()) { it.binaryRoots }
            val indexFutures =
                binaryRootsToIndex.map { root ->
                    CompletableFuture.supplyAsync {
                        indexFactory.index(root, projectStructure.kotlinCoreProjectEnvironment)
                    }
                }
            val indexes = indexFutures.map { it.join() }
            BinariesDeclarationProviderFactory(indexes)
        }

    private val librariesFactory
        get() = _factoryFuture.get()

    override fun createDeclarationProvider(
        scope: GlobalSearchScope,
        contextualModule: KaModule?,
    ): KotlinDeclarationProvider {
        val providers = buildList {
            cellAnalyzer.getAllCells().mapNotNullTo(this) { analyzableCell ->
                if (analyzableCell.ktFile.virtualFile !in scope) return@mapNotNullTo null
                KotlinFileBasedDeclarationProvider(analyzableCell.ktFile)
            }
            compiledCellStorage.allCompiledCells().mapTo(this) {
                it.compiledCellProviderFactory.createDeclarationProvider(scope, contextualModule)
            }
            add(librariesFactory.createDeclarationProvider(scope, contextualModule))
            add(projectStructure.builtins.symbolProvider.createDeclarationProvider(scope, contextualModule))
        }
        return KotlinCompositeDeclarationProvider.create(providers)
    }
}
