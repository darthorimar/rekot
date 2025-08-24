package me.darthorimar.rekot.projectStructure

import com.intellij.openapi.project.Project
import me.darthorimar.rekot.projectStructure.index.IndexSerializer
import me.darthorimar.rekot.projectStructure.index.IndexFactory
import me.darthorimar.rekot.projectStructure.index.IndexStorage
import me.darthorimar.rekot.projectStructure.index.Indexer
import me.darthorimar.rekot.projectStructure.index.StubTableSerializer
import org.koin.dsl.module

val projectStructureModule = module {
    single<ProjectStructure> { ProjectStructureInitiator.initiateProjectStructure(appConfig = get()) }
    factory<Project> { get<ProjectStructure>().project }
    single { IndexFactory() }
    single { IndexSerializer() }
    single { StubTableSerializer() }
    single { Indexer() }
    single { IndexStorage() }
}
