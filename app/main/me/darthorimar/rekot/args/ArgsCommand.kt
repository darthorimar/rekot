package me.darthorimar.rekot.args

sealed interface ArgsCommand {
    sealed interface Secondary : ArgsCommand {
        data object Version : Secondary

        data object AppDir : Secondary

        data class Help(val help: String) : Secondary
    }

    data object RunApp : ArgsCommand
}
