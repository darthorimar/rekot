package me.darthorimar.rekot.projectStructure

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.impl.base.projectStructure.KaBuiltinsModuleImpl
import org.jetbrains.kotlin.analysis.api.standalone.base.declarations.KotlinStandaloneDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.decompiler.psi.BuiltinsVirtualFileProvider
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms

class Builtins(project: Project) {
    val symbolProvider =
        KotlinStandaloneDeclarationProviderFactory(
            project,
            sourceKtFiles = emptyList(),
            binaryRoots = emptyList(),
            shouldBuildStubsForBinaryLibraries = true,
            skipBuiltins = false,
        )

    val kaModule = KaBuiltinsModuleImpl(JvmPlatforms.defaultJvmPlatform, project)

    init {
        BuiltinsVirtualFileProvider.getInstance().getBuiltinVirtualFiles().forEach { it.kaModule = kaModule }
    }
}
