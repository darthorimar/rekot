package me.darthorimar.rekot.projectStructure

import com.intellij.openapi.project.Project
import org.koin.dsl.module

val projectStructureModule = module {
    single<ProjectStructure> { ProjectStructureInitiator.initiateProjectStructure(appConfig = get()) }
    factory<Project> { get<ProjectStructure>().project }
}
