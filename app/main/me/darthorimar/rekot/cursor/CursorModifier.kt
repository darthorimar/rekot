package me.darthorimar.rekot.cursor

interface CursorModifier {
    var row: Int
    var column: Int

    fun resetToZero() {
        row = 0
        column = 0
    }

    fun asCursor(): Cursor
}

class Cursor(val row: Int, val column: Int) {
    fun asModifier(maxColumn: Int? = null): CursorModifier = CursorModifierImpl(row, column, maxColumn)

    fun modified(modifier: CursorModifier.() -> Unit): Cursor = asModifier().apply(modifier).asCursor()

    override fun toString(): String {
        return "($row, $column)"
    }

    companion object {
        fun zero() = Cursor(0, 0)
    }
}
