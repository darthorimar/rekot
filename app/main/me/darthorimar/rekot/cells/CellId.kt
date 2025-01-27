package me.darthorimar.rekot.cells

class CellId(private val tag: Int) : Comparable<CellId> {
    val isInitialCell: Boolean
        get() = tag == 1

    override fun compareTo(other: CellId): Int {
        return tag.compareTo(other.tag)
    }

    override fun toString(): String {
        return tag.toString()
    }
}
