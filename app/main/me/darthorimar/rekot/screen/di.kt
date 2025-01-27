package me.darthorimar.rekot.screen

import com.googlecode.lanterna.screen.Screen
import com.googlecode.lanterna.terminal.Terminal
import me.darthorimar.rekot.editor.renderer.CellViewRenderer
import org.koin.dsl.module

fun screenModuleFactory(screen: Screen, terminal: Terminal) = module {
    single<ScreenController> { ScreenControllerLanternaImpl(terminal, screen) }
    single { KeyboardInputPoller(screen) }
    single { CellViewRenderer(screen) }
    single { screen }
    single { HackyMacBugFix(screen) }
}
