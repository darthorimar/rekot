package me.darthorimar.rekot.completion

import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.graphics.TextGraphics
import com.googlecode.lanterna.screen.Screen
import me.darthorimar.rekot.app.AppComponent
import me.darthorimar.rekot.cells.Cells
import me.darthorimar.rekot.config.AppConfig
import me.darthorimar.rekot.screen.ScreenController
import me.darthorimar.rekot.style.Colors
import me.darthorimar.rekot.style.StyleRenderer
import me.darthorimar.rekot.style.Styles
import me.darthorimar.rekot.style.styledText
import me.darthorimar.rekot.util.Scroller
import me.darthorimar.rekot.util.limit
import org.koin.core.component.inject

class CompletionPopupRenderer : AppComponent {
    private val appConfig: AppConfig by inject()
    private val cells: Cells by inject()
    private val screen: Screen by inject()
    private val completion: Completion by inject()
    private val renderer: StyleRenderer by inject()
    private val screenController: ScreenController by inject()
    private val styles: Styles by inject()
    private val colors: Colors by inject()

    private val scroller = Scroller(appConfig.completionPopupHeight)

    private val popupHeight
        get() = appConfig.completionPopupHeight

    fun render() {
        if (!completion.popupShown) return
        val popup = completion.popup
        val elements = popup.elements
        if (elements.isEmpty()) return
        val rootGraphics = screen.newTextGraphics()
        val popupWidth = computeWidth(rootGraphics)
        val offset = scroller.scroll(popup.selectedIndex)
        val addDotsAtTheEnd = offset + popupHeight < elements.size
        val graphics =
            rootGraphics.newTextGraphics(
                popup.computePosition(rootGraphics, popupWidth),
                TerminalSize(
                    /* columns= */ popupWidth,
                    /* rows= */ appConfig.completionPopupHeight + if (addDotsAtTheEnd) 1 else 0,
                ),
            )

        val toShow = elements.limit(offset, popupHeight)
        val text = styledText {
            for ((i, completionElement) in toShow.withIndex()) {
                val selected = popup.selectedIndex == offset + i
                styledLine {
                    from(styles.COMPLETION)
                    if (selected) {
                        with(styles.COMPLETION_SELECTED)
                    }
                    styled {
                        foregroundColor(
                            when (completionElement.tag) {
                                CompletionItemTag.FUNCTION -> colors.FUNCTION
                                CompletionItemTag.PROPERTY -> colors.PROPERTY
                                CompletionItemTag.CLASS -> colors.CLASS
                                CompletionItemTag.LOCAL_VARIABLE -> colors.LOCAL_VARIABLE
                                CompletionItemTag.KEYWORD -> colors.KEYWORD
                            })
                        string(completionElement.tag.text)
                    }

                    string(" ")
                    string(completionElement.show)
                }
            }
            if (addDotsAtTheEnd) {
                styledLine {
                    from(styles.COMPLETION)
                    foregroundColor(colors.COMMENT)
                    string("...")
                }
            }
        }
        renderer.render(graphics, text)
    }

    private fun computeWidth(on: TextGraphics): Int {
        return (on.size.columns * 3 / 4).coerceAtLeast(appConfig.completionPopupMinWidth)
    }

    private fun CompletionPopup.computePosition(on: TextGraphics, width: Int): TerminalPosition {
        val cell = cells.getCell(cellId)
        val columnOffset = (cell.cursor.column - prefix.length).coerceAtLeast(0)
        val column = if (columnOffset + width <= on.size.columns) columnOffset else on.size.columns - width

        val cursorPosition = screenController.cursor
        val row =
            if (cursorPosition.row + 1 + popupHeight >= on.size.rows) cursorPosition.row - popupHeight
            else cursorPosition.row + 1
        return TerminalPosition(column, row)
    }
}
