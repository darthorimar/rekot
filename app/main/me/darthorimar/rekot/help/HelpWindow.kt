package me.darthorimar.rekot.help

import me.darthorimar.rekot.app.AppComponent
import me.darthorimar.rekot.config.APP_LOGO
import me.darthorimar.rekot.config.APP_NAME
import me.darthorimar.rekot.config.AppConfig
import me.darthorimar.rekot.screen.ScreenController
import me.darthorimar.rekot.style.StyledText
import me.darthorimar.rekot.style.Styles
import me.darthorimar.rekot.style.styledText
import org.koin.core.component.inject

class HelpWindow : AppComponent {
    private val styles: Styles by inject()
    private val screenController: ScreenController by inject()

    private val keybindings =
        listOf(
            "Ctrl+E" binding "Execute current cell",
            "Ctrl+B" binding "Stop current cell execution",
            "Ctrl+D" binding "Delete current cell",
            "Ctrl+L" binding "Clear current cell",
            "Ctrl+R" binding "Refresh screen",
            "Ctrl+C" binding "Exit $APP_NAME",
        )

    private var _shown: Boolean = false

    fun show() {
        _shown = true
    }

    fun close() {
        _shown = false
    }

    val shown: Boolean
        get() = _shown

    fun text(): StyledText {
        return styledText {
            if (screenController.screenSize.rows > APP_LOGO.lines().size + keybindings.size + 2) {
                for (line in APP_LOGO.lines()) {
                    styledLine {
                        from(styles.HELP)
                        string(line)
                    }
                }
            }

            styledLine { from(styles.HELP) }

            styledLine {
                from(styles.HELP)
                string("$APP_NAME keybindings")
                styled {
                    italic()
                    string(" ")
                    string("(Press Esc to exit)")
                }
            }

            styledLine { from(styles.HELP) }

            for (keybinding in keybindings) {
                styledLine {
                    from(styles.HELP)
                    string("• ")
                    styled {
                        bold()
                        string(keybinding.key)
                    }
                    string(" - ")
                    string(keybinding.description)
                    fill(" ")
                }
            }
            styledLine { from(styles.HELP) }
            fillUp(styles.HELP, screenController.screenSize.rows)
        }
    }
}

private data class Keybinding(
    val key: String,
    val description: String,
)

private infix fun String.binding(description: String): Keybinding {
    return Keybinding(this, description)
}
