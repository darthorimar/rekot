package me.darthorimar.rekot.args

@Suppress("ClassName")
sealed interface ArgsCommand {
    sealed interface Secondary : ArgsCommand {
        data object VERSION : Secondary

        data object PRINT_CONFIG : Secondary

        data object HELP : Secondary
    }

    data object RUN_APP : ArgsCommand
}
