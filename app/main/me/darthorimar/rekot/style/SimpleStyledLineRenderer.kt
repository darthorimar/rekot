package me.darthorimar.rekot.style

object SimpleStyledLineRenderer {
    fun renderLine(line: StyledLine, width: Int): String {
        var column = 0
        val result = StringBuilder()
        for ((i, styledString) in line.strings.withIndex()) {
            when (styledString) {
                is StyledString.Regular -> {
                    result.append(styledString.text)
                    column += styledString.text.length
                }

                is StyledString.Filled -> {
                    val remainedLength = line.strings.drop(i + 1).sumOf { it.text.length }
                    val fillerLength = (width - column - remainedLength).coerceAtLeast(0)
                    result.append(styledString.text.repeat(fillerLength))
                    column += fillerLength
                }
            }
        }

        return result.toString()
    }
}
