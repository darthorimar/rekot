package me.darthorimar.rekot.args

import me.darthorimar.rekot.config.APP_NAME

object ArgsCommandsParser {
    fun parse(args: Array<String>): ArgsCommand {
        return when (args.size) {
            0 -> ArgsCommand.RUN_APP
            1 -> when (val arg = args.single()) {
                "--version", "-version", "version" -> ArgsCommand.Secondary.VERSION
                "--help", "-help", "help" -> ArgsCommand.Secondary.HELP
                "--app-dir" -> ArgsCommand.Secondary.PRINT_CONFIG
                else -> error("Unknown arguments `$arg`, use `--help` for supported commands")
            }
            else -> error("Too many arguments, use `--help` for supported commands")
        }
    }

    fun help(): String {
        return """
            | --version - print $APP_NAME version
            | --app-dir - print directory with the $APP_NAME configuration
            | --help    - print this message
        """.trimMargin()
    }
}

