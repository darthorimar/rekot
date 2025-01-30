package me.darthorimar.rekot

import com.googlecode.lanterna.screen.Screen
import com.googlecode.lanterna.screen.TerminalScreen
import com.googlecode.lanterna.terminal.DefaultTerminalFactory
import com.googlecode.lanterna.terminal.Terminal
import me.darthorimar.rekot.analysis.CompilerErrorInterceptor
import me.darthorimar.rekot.analysis.LoggingErrorInterceptor
import me.darthorimar.rekot.app.App
import me.darthorimar.rekot.app.AppComponent
import me.darthorimar.rekot.app.appModule
import me.darthorimar.rekot.args.ArgsCommand
import me.darthorimar.rekot.args.ArgsCommandsParser
import me.darthorimar.rekot.args.ArgsSecondaryCommandExecutor
import me.darthorimar.rekot.config.AppConfig
import me.darthorimar.rekot.config.ConfigFactory
import me.darthorimar.rekot.screen.screenModuleFactory
import me.darthorimar.rekot.updates.UpdatesChecker
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import java.io.OutputStream
import java.io.PrintStream
import kotlin.system.exitProcess
import me.darthorimar.rekot.logging.*


fun main(args: Array<String>) {
    when (val command = ArgsCommandsParser.parse(args)) {
        ArgsCommand.RUN_APP -> {
            runApp()
        }
        is ArgsCommand.Secondary -> {
            ArgsSecondaryCommandExecutor.execute(command)
        }
    }
}

private fun runApp() {
    val config = ConfigFactory.createConfig()
    config.init()

    val appConfigModule = module { single<AppConfig> { config } }

    startKoin { modules(appConfigModule, appModule) }

    try {
        withoutErr {
            withScreenAndTerminal { screen, terminal ->
                loadKoinModules(screenModuleFactory(screen, terminal))
                loadKoinModules(productionAppModule)
                AppComponent.performSubscriptions()
                App().runApp()
            }
        }
    } catch (e: Throwable) {
        logger<App>().error("Application error", e)
        throw e
    } finally {
        exitProcess(0)
    }
}

private val productionAppModule = module {
    single<CompilerErrorInterceptor> { LoggingErrorInterceptor() }
    single { UpdatesChecker() }
}

private fun withoutErr(block: () -> Unit) {
    val original = System.err
    System.setErr(voidStream)
    try {
        block()
    } finally {
        System.setErr(original)
    }
}

private fun withScreenAndTerminal(block: (Screen, Terminal) -> Unit) {
    var screen: Screen? = null
    var terminal: Terminal? = null
    try {
        terminal =
            DefaultTerminalFactory()
                //            .setForceTextTerminal(true)
                .createTerminal()
        screen = TerminalScreen(terminal)
        screen.startScreen()
        block(screen, terminal)
    } finally {
        screen?.stopScreen()
        terminal?.close()
    }
}

private val voidStream =
    PrintStream(
        object : OutputStream() {
            override fun write(b: Int) {}
        })
