package me.darthorimar.rekot.projectStructure

import me.darthorimar.rekot.projectStructure.modules.KaJarLibraryModuleImpl
import me.darthorimar.rekot.projectStructure.modules.KaRekotLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreProjectEnvironment
import java.nio.file.Path

class Library(val kaModule: KaRekotLibraryModule) {
    companion object {
        fun create(
            roots: List<Path>,
            kotlinCoreProjectEnvironment: KotlinCoreProjectEnvironment,
            name: String,
        ): Library {
            val kaLibraryModule = KaJarLibraryModuleImpl(roots, name, kotlinCoreProjectEnvironment.project)
            return Library(kaLibraryModule)
        }
    }
}
