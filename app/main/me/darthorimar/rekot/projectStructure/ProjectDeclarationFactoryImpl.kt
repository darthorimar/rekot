package me.darthorimar.rekot.projectStructure

import com.intellij.psi.search.GlobalSearchScope
import me.darthorimar.rekot.analysis.CellAnalyzer
import me.darthorimar.rekot.analysis.CompiledCellStorage
import me.darthorimar.rekot.app.AppComponent
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinCompositeDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinFileBasedDeclarationProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.koin.core.component.inject

class ProjectDeclarationFactoryImpl() : KotlinDeclarationProviderFactory, AppComponent {
    private val cellAnalyzer: CellAnalyzer by inject()
    private val compiledCellStorage: CompiledCellStorage by inject()
    private val projectStructure: ProjectStructure by inject()

    private val essentialLibrariesProviderFactory by lazy {
        BinariesDeclarationProviderFactory(
            projectStructure.project,
            projectStructure.essentialLibraries.allVirtualFiles,
        )
    }

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
            add(essentialLibrariesProviderFactory.createDeclarationProvider(scope, contextualModule))
            add(projectStructure.builtins.symbolProvider.createDeclarationProvider(scope, contextualModule))
        }
        return KotlinCompositeDeclarationProvider.create(providers)
    }
}
