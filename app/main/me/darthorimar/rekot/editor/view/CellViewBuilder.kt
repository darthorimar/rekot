package me.darthorimar.rekot.editor.view

import me.darthorimar.rekot.cells.Cell
import me.darthorimar.rekot.editor.view.EditorViewProvider.Companion.GAP
import me.darthorimar.rekot.style.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class CellViewBuilder(private val cell: Cell) : KoinComponent {
    protected val styles: Styles by inject()
    protected val colors: Colors by inject()

    private val lines = mutableListOf<EditorViewLine>()

    abstract fun styledLine(init: StyledLineBuilder.() -> Unit): StyledLine

    fun codeLine(builder: StyledLineBuilder.() -> Unit) {
        lines += EditorViewLine.CodeLine(styledLine(builder), cell)
    }

    fun navigatableLine(builder: StyledLineBuilder.() -> Unit) {
        lines += EditorViewLine.NavigatableLine(styledLine(builder), cell)
    }

    fun nonNavigatableLine(builder: StyledLineBuilder.() -> Unit) {
        lines += EditorViewLine.NonNavigatableLine(styledLine(builder), cell)
    }

    fun emptyLine() {
        nonNavigatableLine {
            from(styles.HEADER)
            fill(" ")
        }
    }

    fun header(text: String, modifier: StyleModifier = StyleModifier.EMPTY, extra: StyledLineBuilder.() -> Unit = {}) {
        nonNavigatableLine {
            from(styles.HEADER)
            with(modifier)
            gap(GAP)
            string(text)
            extra()
        }
    }

    fun codeLikeBlock(bgColor: Color, build: CellViewBuilder.() -> Unit) {
        lines += CellViewBuilderForCodeBlock(bgColor, cell).apply(build).build().lines
    }

    fun build(): CellView {
        return CellView(cell.id, lines)
    }
}

class CellViewBuilderImpl(cell: Cell) : CellViewBuilder(cell) {
    override fun styledLine(init: StyledLineBuilder.() -> Unit): StyledLine {
        return me.darthorimar.rekot.style.styledLine(init)
    }
}

private class CellViewBuilderForCodeBlock(private val bgColor: Color, cell: Cell) : CellViewBuilder(cell) {
    override fun styledLine(init: StyledLineBuilder.() -> Unit): StyledLine {
        return me.darthorimar.rekot.style.styledLine {
            foregroundColor(colors.CELL_BG)
            backgroundColor(bgColor)
            styled { init() }
            fill(" ")
        }
    }
}

inline fun cellView(cell: Cell, init: CellViewBuilderImpl.() -> Unit): CellView {
    return CellViewBuilderImpl(cell).apply(init).build()
}
