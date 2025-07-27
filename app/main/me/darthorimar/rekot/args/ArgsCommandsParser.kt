package me.darthorimar.rekot.args

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintHelpMessage
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parsers.CommandLineParser
import me.darthorimar.rekot.config.APP_NAME

private class RekotCommand() : CliktCommand(APP_NAME) {
    val version by option("--version", "-version", help = "Print $APP_NAME version").flag()
    val appDir by option("--app-dir", help = "Print directory with the $APP_NAME configuration").flag()
        .validate {
            if (it && version) fail("Cannot use --app-dir and --version together")
        }

    fun createCommand(): ArgsCommand {
        return when {
            version -> ArgsCommand.Secondary.Version
            appDir -> ArgsCommand.Secondary.AppDir
            else -> ArgsCommand.RunApp
        }
    }

    override fun run() {}
}

object ArgsCommandsParser {
    fun parse(args: Array<String>): ArgsCommand {
        val command = RekotCommand()

        var result: ArgsCommand? = null
        try {
            CommandLineParser.parseAndRun(command, args.toList()) { result = (it as RekotCommand).createCommand() }
        } catch (e: PrintHelpMessage) {
            return ArgsCommand.Secondary.Help(e.context?.command?.getFormattedHelp() ?: "Help not available")
        }
        return result ?: error("Cannot parse command from args: ${args.joinToString(" ")}")
    }
}
