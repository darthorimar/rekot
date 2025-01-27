package me.darthorimar.rekot.editor.view

import me.darthorimar.rekot.app.AppComponent
import me.darthorimar.rekot.app.SubscriptionContext
import me.darthorimar.rekot.app.subscribe
import me.darthorimar.rekot.cells.Cell
import me.darthorimar.rekot.cells.Cells
import me.darthorimar.rekot.events.Event
import me.darthorimar.rekot.screen.ScreenController
import me.darthorimar.rekot.style.styleModifier
import me.darthorimar.rekot.util.Scroller
import org.koin.core.component.inject

class EditorViewProvider : AppComponent {
    private val cells: Cells by inject()
    private val screenController: ScreenController by inject()

    private val codeViewGenerator: CodeViewGenerator by inject()
    private val soutViewGenerator: SoutViewGenerator by inject()
    private val resultViewGenerator: ResultViewGenerator by inject()
    private val errorViewGenerator: ErrorViewGenerator by inject()

    private val scroller = Scroller(screenController.screenSize.rows)

    context(SubscriptionContext)
    override fun performSubscriptions() {
        subscribe<Event.TerminalResized> { e -> scroller.resize(e.screenSize.rows) }
    }

    val view: EditorView
        get() = compute()

    private fun compute(): EditorView {
        val cellViews = cells.cells.associateWith { cellView(it) { cell(it) } }
        return EditorView(cellViews)
    }

    private fun CellViewBuilder.cell(cell: Cell) {
        header("Cell#${cell.id}", modifier = styleModifier { bold() })
        with(codeViewGenerator) { code(cell) }
        with(errorViewGenerator) { error(cell) }
        with(resultViewGenerator) { result(cell) }
        with(soutViewGenerator) { sout(cell) }
        emptyLine()
    }

    companion object {
        const val GAP = 0
    }
}
