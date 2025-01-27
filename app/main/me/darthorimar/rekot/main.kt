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
import me.darthorimar.rekot.config.AppConfig
import me.darthorimar.rekot.config.ConfigFactory
import me.darthorimar.rekot.screen.screenModuleFactory
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import java.io.OutputStream
import java.io.PrintStream
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val config = ConfigFactory.createConfig(args)
    config.init()

    val appConfigModule = module { single<AppConfig> { config } }

    startKoin { modules(appConfigModule, appModule) }

    withoutErr {
        withScreenAndTerminal { screen, terminal ->
            loadKoinModules(screenModuleFactory(screen, terminal))
            loadKoinModules(module { single<CompilerErrorInterceptor> { LoggingErrorInterceptor() } })
            AppComponent.performSubscriptions()
            App().runApp()
        }
    }
    exitProcess(0)
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
