package me.darthorimar.rekot.events

import me.darthorimar.rekot.app.AppComponent
import me.darthorimar.rekot.app.SubscriptionContext
import me.darthorimar.rekot.app.subscribe
import me.darthorimar.rekot.cells.Cells
import me.darthorimar.rekot.completion.Completion
import me.darthorimar.rekot.editor.Editor
import me.darthorimar.rekot.events.Event.Keyboard.Typing.CharTyping
import me.darthorimar.rekot.execution.CellExecutor
import me.darthorimar.rekot.help.HelpWindow
import me.darthorimar.rekot.screen.ScreenController
import org.koin.core.component.inject

class KeyboardEventProcessor : AppComponent {
    private val editor: Editor by inject()
    private val cells: Cells by inject()
    private val screenController: ScreenController by inject()
    private val completion: Completion by inject()
    private val cellExecutor: CellExecutor by inject()
    private val helpWindow: HelpWindow by inject()

    context(SubscriptionContext)
    override fun performSubscriptions() {
        subscribe<Event.Keyboard> { event -> process(event) }
    }

    private fun process(event: Event.Keyboard) {
        when (event) {
            is CharTyping -> {
                if (completion.popupShown) {
                    when (event.char) {
                        '.' -> {
                            if (completion.choseItemOnDot()) {
                                fireEvent(CharTyping('.'))
                                return
                            }
                        }
                    }
                }
                editor.type(event.char)
            }

            is Event.Keyboard.Typing.TextTyping -> {
                editor.type(event.text)
            }

            Event.Keyboard.Backspace -> {
                editor.backspace()
                completion.closePopup()
            }

            Event.Keyboard.ClearCell -> {
                editor.clearCell()
                completion.closePopup()
            }

            Event.Keyboard.Delete -> {
                editor.delete()
                completion.closePopup()
            }

            Event.Keyboard.Enter -> {
                completionOrEditor({ completion.choseItem() }, { editor.enter() })
            }

            Event.Keyboard.Escape -> {
                helpWindow.close()
                completion.closePopup()
            }

            Event.Keyboard.ExecuteCell -> {
                cellExecutor.execute(editor.focusedCell)
                completion.closePopup()
            }

            Event.Keyboard.StopExecution -> {
                cellExecutor.stop(editor.focusedCell)
                completion.closePopup()
            }

            Event.Keyboard.NewCell -> {
                editor.navigateToCell(cells.newCell())
                completion.closePopup()
            }

            Event.Keyboard.DeleteCell -> {
                completion.closePopup()
                editor.deleteCell()
            }

            Event.Keyboard.RefreshScreen -> {
                screenController.fullRefresh()
            }

            Event.Keyboard.ShiftTab -> {
                editor.shiftTab()
            }

            Event.Keyboard.Tab -> {
                completionOrEditor({ completion.choseItem() }, { editor.tab() })
            }

            is Event.Keyboard.ArrowButton -> {
                when (event.direction) {
                    Event.Keyboard.ArrowButton.Direction.UP -> {
                        completionOrEditor({ completion.up() }, { editor.up() })
                    }
                    Event.Keyboard.ArrowButton.Direction.DOWN -> {
                        completionOrEditor({ completion.down() }, { editor.down() })
                    }

                    Event.Keyboard.ArrowButton.Direction.LEFT -> {
                        completion.closePopup()
                        editor.left()
                    }
                    Event.Keyboard.ArrowButton.Direction.RIGHT -> {
                        completion.closePopup()
                        editor.right()
                    }
                }
            }

            Event.Keyboard.ShowHelp -> {
                helpWindow.show()
            }
        }
    }

    private inline fun completionOrEditor(completion: () -> Unit = {}, editor: () -> Unit = {}) {
        if (this.completion.popupShown) {
            completion()
        } else {
            editor()
        }
    }
}
