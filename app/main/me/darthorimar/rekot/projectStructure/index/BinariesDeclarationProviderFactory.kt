package me.darthorimar.rekot.projectStructure.index

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinCompositeDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.standalone.base.declarations.KotlinStandaloneDeclarationProvider

@Suppress(
    "EXPOSED_PARAMETER_TYPE",
    "INVISIBLE_REFERENCE",
)
class BinariesDeclarationProviderFactory(
    private val indexes: List<DeclarationIndex>
) : KotlinDeclarationProviderFactory {
    override fun createDeclarationProvider(
        scope: GlobalSearchScope,
        contextualModule: KaModule?
    ): KotlinDeclarationProvider {
        // todo combine DeclarationIndexes instead
        return KotlinCompositeDeclarationProvider.create(
            indexes.map { KotlinStandaloneDeclarationProvider(it, scope) }
        )
    }
}
