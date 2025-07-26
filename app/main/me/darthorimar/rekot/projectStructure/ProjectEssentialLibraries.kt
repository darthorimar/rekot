package me.darthorimar.rekot.projectStructure

class ProjectEssentialLibraries(val stdlib: Library, val jdk: Library) {
    val kaModules
        get() = listOf(stdlib.kaModule, jdk.kaModule)

    val allLibraries
        get() = listOf(stdlib, jdk)
}
