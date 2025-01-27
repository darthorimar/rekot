package me.darthorimar.rekot.projectStructure

import com.intellij.mock.MockProject
import me.darthorimar.rekot.app.AppComponent
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition.FromConfigurations
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionProvider
import org.jetbrains.kotlin.scripting.definitions.ScriptEvaluationConfigurationFromHostConfiguration
import org.jetbrains.kotlin.scripting.resolve.KtFileScriptSource
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration

class CellScriptDefinitionProvider(private val project: MockProject) : ScriptDefinitionProvider, AppComponent {
    override fun findDefinition(script: SourceCode): ScriptDefinition {
        val ktFile = (script as KtFileScriptSource).ktFile
        val kaCellScriptModule =
            KotlinProjectStructureProvider.getModule(project, ktFile, useSiteModule = null) as KaCellScriptModule
        return CellScriptDefinition(kaCellScriptModule, kaCellScriptModule.resultVariableName)
    }

    override fun getDefaultDefinition(): ScriptDefinition = error("Should not be called")

    override fun getKnownFilenameExtensions(): Sequence<String> = sequenceOf("kts")

    override fun isScript(script: SourceCode): Boolean = true
}

private class CellScriptDefinition(
    private val scriptModule: KaCellScriptModule,
    private val resultPropertyName: String?,
) :
    FromConfigurations(
        defaultJvmScriptingHostConfiguration,
        ScriptCompilationConfiguration {
            implicitReceivers(*scriptModule.dependentScriptInstances.map { it::class }.toTypedArray())
            resultPropertyName?.let { resultField(it) }
            defaultImports(scriptModule.imports)
        },
        ScriptEvaluationConfigurationFromHostConfiguration(defaultJvmScriptingHostConfiguration),
    ) {
    override val isDefault = true
}
