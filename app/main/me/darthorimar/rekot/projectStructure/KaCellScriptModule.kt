package me.darthorimar.rekot.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaScriptModule
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtFile

class KaCellScriptModule(
    override val file: KtFile,
    override val project: Project,
    val dependentScriptInstances: List<Any>,
    binaryDependencies: List<KaModule>,
    val imports: List<String>,
    val resultVariableName: String?,
) : KaScriptModule {
    override val directRegularDependencies: List<KaModule> = binaryDependencies

    override val contentScope: GlobalSearchScope
        get() = GlobalSearchScope.fileScope(file)

    override val languageVersionSettings: LanguageVersionSettings
        get() = LanguageVersionSettingsImpl.DEFAULT

    override val targetPlatform: TargetPlatform
        get() = JvmPlatforms.defaultJvmPlatform

    override val transitiveDependsOnDependencies: List<KaModule>
        get() = emptyList()

    override val directDependsOnDependencies: List<KaModule>
        get() = emptyList()

    override val directFriendDependencies: List<KaModule>
        get() = emptyList()
}
