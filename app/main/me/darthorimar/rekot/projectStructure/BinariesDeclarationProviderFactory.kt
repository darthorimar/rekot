package me.darthorimar.rekot.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.standalone.base.declarations.KotlinStandaloneDeclarationProviderFactory

class BinariesDeclarationProviderFactory(project: Project, libraryBinaryFiles: List<VirtualFile>) :
    KotlinDeclarationProviderFactory {
    private val delegate =
        KotlinStandaloneDeclarationProviderFactory(
            project,
            sourceKtFiles = emptyList(),
            binaryRoots = libraryBinaryFiles,
            shouldBuildStubsForBinaryLibraries = true,
            skipBuiltins = true,
        )

    override fun createDeclarationProvider(
        scope: GlobalSearchScope,
        contextualModule: KaModule?,
    ): KotlinDeclarationProvider {
        return delegate.createDeclarationProvider(scope, contextualModule)
    }
}
