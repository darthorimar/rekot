package me.darthorimar.rekot.args

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintHelpMessage
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.transformAll
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.clikt.parsers.CommandLineParser
import java.nio.file.Path
import me.darthorimar.rekot.config.APP_NAME
import java.nio.file.Paths
import kotlin.io.path.isRegularFile

private class RekotCommand() : CliktCommand(APP_NAME) {
    val version by option("--version", "-version", help = "Print $APP_NAME version").flag()
    val appDir by
        option("--app-dir", help = "Print directory with the $APP_NAME configuration").flag().validate {
            if (it && version) fail("Cannot use --app-dir and --version together")
        }

    val libPaths: List<Path> by
        option(
                "--lib-paths",
                help = "A semicolon-separated list of paths to libraries to load. Kotlin StdLib and JDK are added by default",
            )
            .convert { paths -> paths.split(";").map { Paths.get(it.trim()) } }
            .default(emptyList())
            .validate { paths ->
                for (path in paths) {
                    if (!path.isRegularFile()) {
                        fail("Invalid library path: $path is not a file")
                    }
                }
            }


    val libArtifacts: List<String> by
        option(
                "--lib-artifacts",
                help = "A semicolon-separated list of library coordinates to load in the form of 'group:artifact:version'")
            .convert { paths -> paths.split(";").map { it.trim() } }
            .default(emptyList())

    fun createCommand(): ArgsCommand {
        return when {
            version -> ArgsCommand.Secondary.Version
            appDir -> ArgsCommand.Secondary.AppDir
            else ->
                ArgsCommand.RunApp(
                    libraryPaths = libPaths,
                    libraryArtifacts = libArtifacts,
                )
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
