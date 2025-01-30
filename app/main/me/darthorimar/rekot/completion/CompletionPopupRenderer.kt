package me.darthorimar.rekot.completion

import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TerminalSize
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

    fun render() {
        if (!completion.popupShown) return
        val popup = completion.popup
        val elements = popup.elements
        if (elements.isEmpty()) return
        val width = computeWidth()
        val offset = scroller.scroll(popup.selectedIndex)
        val height = computeHeight()
        scroller.resize(height)
        val addDotsAtTheEnd = offset + height < elements.size

        val position =
            popup.computePosition(
                width = width,
                height = height,
                addDotsAtTheEnd = addDotsAtTheEnd,
            )
        val graphics =
            screen
                .newTextGraphics()
                .newTextGraphics(
                    position,
                    TerminalSize(
                        /* columns= */ width,
                        /* rows= */ height + if (addDotsAtTheEnd) 1 else 0,
                    ),
                )

        val toShow = elements.limit(offset, height)
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

    private fun computeHeight(): Int {
        val raw = appConfig.completionPopupHeight
        return raw.coerceAtMost(screenController.screenSize.rows / 2)
    }

    private fun computeWidth(): Int {
        return (screen.terminalSize.columns * 3 / 4).coerceAtLeast(appConfig.completionPopupMinWidth)
    }

    private fun CompletionPopup.computePosition(width: Int, height: Int, addDotsAtTheEnd: Boolean): TerminalPosition {
        val cell = cells.getCell(cellId)
        val columnOffset = (cell.cursor.column - prefix.length).coerceAtLeast(0)
        val column =
            if (columnOffset + width <= screen.terminalSize.columns) columnOffset
            else screen.terminalSize.columns - width

        val cursorPosition = screenController.cursor
        val row =
            if (cursorPosition.row + 1 + height >= screen.terminalSize.rows) {
                cursorPosition.row - height - (if (addDotsAtTheEnd) 1 else 0)
            } else {
                cursorPosition.row + 1
            }
        return TerminalPosition(column, row)
    }
}
