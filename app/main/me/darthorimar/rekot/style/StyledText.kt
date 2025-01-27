package me.darthorimar.rekot.style

class StyledText(val lines: List<StyledLine>)

interface StyledTextBuilder {
    fun styledLine(init: StyledLineBuilder.() -> Unit)

    fun with(text: StyledText)

    fun with(lines: List<StyledLine>)

    fun fillUp(style: Style, height: Int)
}

abstract class StyledTextBuilderBaseImpl : StyledTextBuilder {
    protected val lines = mutableListOf<StyledLine>()

    override fun styledLine(init: StyledLineBuilder.() -> Unit) {
        lines.add(me.darthorimar.rekot.style.styledLine(init))
    }

    override fun fillUp(style: Style, height: Int) {
        if (lines.size >= height) {
            return
        }
        repeat(height - lines.size) { styledLine { from(style) } }
    }

    override fun with(text: StyledText) {
        lines += text.lines
    }

    override fun with(lines: List<StyledLine>) {
        this.lines += lines
    }
}

class StyledTextBuilderImpl : StyledTextBuilderBaseImpl() {
    fun build(): StyledText {
        return StyledText(lines)
    }
}

fun styledText(init: StyledTextBuilder.() -> Unit): StyledText {
    return StyledTextBuilderImpl().apply(init).build()
}
