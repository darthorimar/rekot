package me.darthorimar.rekot.projectStructure

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import me.darthorimar.rekot.app.AppComponent
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreProjectEnvironment

class ProjectStructure(
    val kotlinCoreProjectEnvironment: KotlinCoreProjectEnvironment,
    val essentialLibraries: ProjectEssentialLibraries,
    val builtins: Builtins,
    val projectStructureProvider: ProjectStructureProviderImpl,
    private val projectDisposable: Disposable,
) : AppComponent {
    val project: Project
        get() = kotlinCoreProjectEnvironment.project

    fun shutdown() {
        Disposer.dispose(projectDisposable)
    }
}
