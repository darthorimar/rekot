package me.darthorimar.rekot.editor.renderer

import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.screen.Screen
import me.darthorimar.rekot.app.AppComponent
import me.darthorimar.rekot.editor.Editor
import me.darthorimar.rekot.editor.view.EditorViewProvider
import me.darthorimar.rekot.style.StyleRenderer
import me.darthorimar.rekot.style.Styles
import me.darthorimar.rekot.style.styledText
import org.koin.core.component.inject

class CellViewRenderer(private val screen: Screen) : AppComponent {
    private val renderer: StyleRenderer by inject()
    private val editor: Editor by inject()
    private val editorViewProvider: EditorViewProvider by inject()
    private val styles: Styles by inject()

    fun render() {
        val view = editorViewProvider.view
        val text = styledText {
            with(view.lines.drop(editor.viewPosition).take(screen.terminalSize.rows).map { it.line })
            fillUp(styles.EMPTY, screen.terminalSize.rows)
        }
        renderer.render(screen.newTextGraphics(), text)

        screen.cursorPosition =
            TerminalPosition(/* column= */ editor.cursor.column, /* row= */ editor.cursor.row - editor.viewPosition)
    }
}
