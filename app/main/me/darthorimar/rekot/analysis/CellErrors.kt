package me.darthorimar.rekot.analysis

class CellErrors(private val errorPerLine: Map<Int, List<CellError>>) {
    fun byLine(line: Int): List<CellError> = errorPerLine[line] ?: emptyList()

    val allErrors: List<CellError>
        get() = errorPerLine.values.flatten()

    companion object {
        val EMPTY = CellErrors(emptyMap())
    }
}
