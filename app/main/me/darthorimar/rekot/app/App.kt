package me.darthorimar.rekot.app

import me.darthorimar.rekot.cells.Cells
import me.darthorimar.rekot.completion.CompletionPopupRenderer
import me.darthorimar.rekot.editor.Editor
import me.darthorimar.rekot.editor.renderer.CellViewRenderer
import me.darthorimar.rekot.events.EventQueue
import me.darthorimar.rekot.help.HelpRenderer
import me.darthorimar.rekot.logging.error
import me.darthorimar.rekot.logging.logger
import me.darthorimar.rekot.screen.KeyboardInputPoller
import me.darthorimar.rekot.screen.ScreenController
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val logger = logger<App>()

class App : KoinComponent {
    private val cells: Cells by inject()
    private val editor: Editor by inject()
    private val screenController: ScreenController by inject()
    private val queue: EventQueue by inject()
    private val keyboardInputPoller: KeyboardInputPoller by inject()

    private val cellViewRenderer: CellViewRenderer by inject()
    private val completionPopupRenderer: CompletionPopupRenderer by inject()
    private val helpRenderer: HelpRenderer by inject()

    private val appState: AppState by inject()

    fun runApp() {
        try {
            doRun()
        } catch (e: Exception) {
            logger.error("Error during app execution", e)
            throw e
        }
    }

    private fun doRun() {
        editor.navigateToCell(cells.newCell())

        while (appState.active) {
            keyboardInputPoller.pollAndFire()
            val needRender = queue.pollAndProcessAll()
            if (needRender) {
                render()
            }
        }
    }

    private fun render() {
        cellViewRenderer.render()
        completionPopupRenderer.render()
        helpRenderer.render()
        screenController.refresh()
    }
}
