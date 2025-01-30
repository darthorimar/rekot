package me.darthorimar.rekot.args

import me.darthorimar.rekot.config.APP_VERSION
import me.darthorimar.rekot.config.ConfigFactory
import kotlin.io.path.absolutePathString

object ArgsSecondaryCommandExecutor {
    fun execute(command: ArgsCommand.Secondary) {
        when (command) {
            ArgsCommand.Secondary.HELP -> {
                println(ArgsCommandsParser.help())
            }
            ArgsCommand.Secondary.PRINT_CONFIG -> {
                println(ConfigFactory.getDefaultAppDirectory().absolutePathString())
            }
            ArgsCommand.Secondary.VERSION -> {
                println(APP_VERSION)
            }
        }
    }
}
