package me.darthorimar.rekot.style

interface StyledLineBuilder : StyleBuilder {
    fun gap(gap: Int)

    fun string(text: String)

    fun fill(text: String)

    fun styled(init: StyledLineBuilder.() -> Unit)

    fun styled(style: Style, init: StyledLineBuilder.() -> Unit)

    fun glaze(modifier: StyleModifier, start: Int, end: Int)
}

class StyleGlaze(val modifier: StyleModifier, val start: Int, val end: Int)

sealed interface StyledString {
    val text: String
    val style: Style

    class Regular(override val text: String, override val style: Style) : StyledString {
        override fun toString(): String {
            return "R<\"$text\", $style>"
        }
    }

    class Filled(override val text: String, override val style: Style) : StyledString {
        override fun toString(): String {
            return "F<\"$text\", $style>"
        }
    }
}

fun styledLine(init: StyledLineBuilder.() -> Unit): StyledLine {
    return StyledLineBuilderImpl(style = null, strings = emptyList()).apply(init).buildLine()
}

class StyledLine(val strings: List<StyledString>, val styleGlazes: List<StyleGlaze>, val finalStyle: Style) {
    override fun toString(): String {
        return SimpleStyledLineRenderer.renderLine(this, 80)
    }
}

private class StyledLineBuilderImpl(style: Style?, strings: List<StyledString>) : StyledLineBuilder {
    private val strings = strings.toMutableList()
    private val styleBuilder = StyleBuilderImpl(style)
    private val styleGlazes = mutableListOf<StyleGlaze>()

    override fun foregroundColor(color: Color) = styleBuilder.foregroundColor(color)

    override fun backgroundColor(color: Color) = styleBuilder.backgroundColor(color)

    override fun from(style: Style) = styleBuilder.from(style)

    override fun with(modifier: StyleModifier) = styleBuilder.with(modifier)

    override fun bold() = styleBuilder.bold()

    override fun italic() = styleBuilder.italic()

    override fun underline() = styleBuilder.underline()

    override fun blink() = styleBuilder.blink()

    override fun reverse() = styleBuilder.reverse()

    override fun strikethrough() = styleBuilder.strikethrough()

    override fun noBold() = styleBuilder.noBold()

    override fun noItalic() = styleBuilder.noItalic()

    override fun noUnderline() = styleBuilder.noUnderline()

    override fun noBlink() = styleBuilder.noBlink()

    override fun noReverse() = styleBuilder.noReverse()

    override fun noStrikethrough() = styleBuilder.noStrikethrough()

    override val currentStyle: Style
        get() = styleBuilder.build()

    override fun gap(gap: Int) {
        string(" ".repeat(gap))
    }

    override fun string(text: String) {
        strings.add(StyledString.Regular(text, currentStyle))
    }

    override fun fill(text: String) {
        strings.add(StyledString.Filled(text, currentStyle))
    }

    override fun styled(init: StyledLineBuilder.() -> Unit) {
        styled(styleBuilder.build(), init)
    }

    override fun styled(style: Style, init: StyledLineBuilder.() -> Unit) {
        val newLines = StyledLineBuilderImpl(style, emptyList()).apply(init).strings
        this.strings += newLines
    }

    override fun glaze(modifier: StyleModifier, start: Int, end: Int) {
        styleGlazes += StyleGlaze(modifier, start, end)
    }

    fun buildLine(): StyledLine {
        return StyledLine(strings, styleGlazes, currentStyle)
    }
}
