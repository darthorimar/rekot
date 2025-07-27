package me.darthorimar.rekot.args

import me.darthorimar.rekot.config.APP_VERSION
import me.darthorimar.rekot.config.ConfigFactory
import kotlin.io.path.absolutePathString

object ArgsSecondaryCommandExecutor {
    fun execute(command: ArgsCommand.Secondary) {
        when (command) {
            is ArgsCommand.Secondary.Help -> {
                println(command.help)
            }
            ArgsCommand.Secondary.AppDir -> {
                println(ConfigFactory.getDefaultAppDirectory().absolutePathString())
            }
            ArgsCommand.Secondary.Version -> {
                println(APP_VERSION)
            }
        }
    }
}
