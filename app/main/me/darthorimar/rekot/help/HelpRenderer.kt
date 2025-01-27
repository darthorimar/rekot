package me.darthorimar.rekot.help

import com.googlecode.lanterna.screen.Screen
import me.darthorimar.rekot.app.AppComponent
import me.darthorimar.rekot.style.StyleRenderer
import org.koin.core.component.inject

class HelpRenderer : AppComponent {
    private val window: HelpWindow by inject()
    private val renderer: StyleRenderer by inject()
    private val screen: Screen by inject()

    fun render() {
        if (!window.shown) return
        val text = window.text()
        renderer.render(screen.newTextGraphics(), text)
    }
}
