package me.darthorimar.rekot.projectStructure.index

import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import me.darthorimar.rekot.app.AppComponent
import me.darthorimar.rekot.projectStructure.ProjectStructure
import me.darthorimar.rekot.projectStructure.modules.KaJarLibraryModuleImpl
import org.koin.core.component.inject

class IndexStorage : AppComponent {
    private val projectStructure: ProjectStructure by inject()
    private val indexFactory: IndexFactory by inject()

    private val _future =
        CompletableFuture.supplyAsync {
            val kaModules = projectStructure.libraries
            val binaryRootsToIndex =
                kaModules.filterIsInstance<KaJarLibraryModuleImpl>().flatMapTo(mutableSetOf()) { it.binaryRoots }
            binaryRootsToIndex.map { root -> indexFactory.index(root, projectStructure.kotlinCoreProjectEnvironment) }
        }


    val indexes: Collection<Index>
        get() = _future.get()
}
