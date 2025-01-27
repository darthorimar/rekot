package me.darthorimar.rekot.cursor

class CursorModifierImpl(
    row: Int,
    column: Int,
    private var maxColumn: Int? = null,
    private val onRowModification: (Int) -> Unit = {},
) : CursorModifier {
    override var column = column
        set(value) {
            field =
                when {
                    maxColumn != null -> value.coerceAtMost(maxColumn!!)
                    else -> value
                }.coerceAtLeast(0)
        }

    override var row = row
        set(value) {
            field = value.coerceAtLeast(0)
            column = column
            onRowModification(value)
        }

    override fun resetToZero() {
        row = 0
        column = 0
    }

    fun updateMaxColumn(newMaxColumn: Int) {
        maxColumn = newMaxColumn
        column = column
    }

    override fun asCursor(): Cursor = Cursor(row, column)
}
