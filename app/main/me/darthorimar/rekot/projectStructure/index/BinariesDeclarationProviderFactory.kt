package me.darthorimar.rekot.projectStructure.index

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinCompositeDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaNotUnderContentRootModule
import org.jetbrains.kotlin.analysis.api.standalone.base.declarations.KotlinStandaloneDeclarationProvider
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreProjectEnvironment
import org.jetbrains.kotlin.platform.TargetPlatform

@Suppress(
    "EXPOSED_PARAMETER_TYPE",
    "INVISIBLE_REFERENCE",
)
class BinariesDeclarationProviderFactory(
    private val projectEnvironment: KotlinCoreProjectEnvironment,
    private val indexes: List<DeclarationIndex>,
) : KotlinDeclarationProviderFactory {
    override fun createDeclarationProvider(
        scope: GlobalSearchScope,
        contextualModule: KaModule?,
    ): KotlinDeclarationProvider {
        // todo combine DeclarationIndexes instead
        return KotlinCompositeDeclarationProvider.create(
            indexes.map { index ->
                KotlinStandaloneDeclarationProvider(
                    index,
                    scope,
                    FakeContextModule,
                    projectEnvironment.environment,
                    shouldComputeBinaryLibraryPackageSets = true,
                )
            }
        )
    }
}

private object FakeContextModule : KaNotUnderContentRootModule {
    override val name: String
        get() = TODO("Not yet implemented")

    @KaPlatformInterface
    override val baseContentScope: GlobalSearchScope
        get() = TODO("Not yet implemented")

    override val contentScope: GlobalSearchScope
        get() = TODO("Not yet implemented")

    override val directDependsOnDependencies: List<KaModule>
        get() = TODO("Not yet implemented")

    override val directFriendDependencies: List<KaModule>
        get() = TODO("Not yet implemented")

    override val directRegularDependencies: List<KaModule>
        get() = TODO("Not yet implemented")

    @KaExperimentalApi
    override val moduleDescription: String
        get() = TODO("Not yet implemented")

    override val project: Project
        get() = TODO("Not yet implemented")

    override val targetPlatform: TargetPlatform
        get() = TODO("Not yet implemented")

    override val transitiveDependsOnDependencies: List<KaModule>
        get() = TODO("Not yet implemented")
}
