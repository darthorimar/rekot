package me.darthorimar.rekot.args

import java.nio.file.Path

sealed interface ArgsCommand {
    sealed interface Secondary : ArgsCommand {
        data object Version : Secondary

        data object AppDir : Secondary

        data class Help(val help: String) : Secondary
    }

    data class RunApp(
        val libraryPaths: List<Path>,
        val libraryArtifacts: List<String>,
    ) : ArgsCommand
}
