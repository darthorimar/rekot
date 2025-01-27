package me.darthorimar.rekot.style

import com.googlecode.lanterna.SGR
import com.googlecode.lanterna.TextCharacter
import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.graphics.StyleSet
import com.googlecode.lanterna.graphics.TextGraphics
import me.darthorimar.rekot.app.AppComponent

class StyleRenderer : AppComponent {
    fun render(graphics: TextGraphics, text: StyledText) {
        for ((row, line) in text.lines.withIndex()) {
            renderString(line, graphics, row)
        }
    }

    private fun renderString(line: StyledLine, graphics: TextGraphics, row: Int) {
        var column = 0
        for ((i, styledString) in line.strings.withIndex()) {
            graphics.setStyleFrom(styledString.style.toLanternaStyle())
            when (styledString) {
                is StyledString.Regular -> {
                    graphics.putString(column, row, styledString.text)
                    column += styledString.text.length
                }

                is StyledString.Filled -> {
                    val remainedLength = line.strings.drop(i + 1).sumOf { it.text.length }
                    val fillerLength = (graphics.size.columns - column - remainedLength).coerceAtLeast(0)
                    graphics.putString(column, row, styledString.text.repeat(fillerLength))
                    column += fillerLength
                }
            }
        }
        graphics.setStyleFrom(line.finalStyle.toLanternaStyle())
        graphics.putString(column, row, " ".repeat((graphics.size.columns - column).coerceAtLeast(0)))

        for (glaze in line.styleGlazes) {
            for (column in glaze.start..glaze.end.coerceAtMost(graphics.size.columns - 1)) {
                val char = graphics.getCharacter(column, row)
                graphics.setCharacter(column, row, char.withModifier(glaze.modifier))
            }
        }
    }

    private fun TextCharacter.withModifier(modifier: StyleModifier): TextCharacter {
        var r = this
        modifier.backgroundColor?.let { r = r.withBackgroundColor(it.toLanternaColor()) }
        modifier.foregroundColor?.let { r = r.withForegroundColor(it.toLanternaColor()) }

        modifier.isBold?.let { r = if (it) r.withModifier(SGR.BOLD) else r.withoutModifier(SGR.BOLD) }
        modifier.isItalic?.let { r = if (it) r.withModifier(SGR.ITALIC) else r.withoutModifier(SGR.ITALIC) }
        modifier.isUnderlined?.let { r = if (it) r.withModifier(SGR.UNDERLINE) else r.withoutModifier(SGR.UNDERLINE) }
        modifier.isBlinking?.let { r = if (it) r.withModifier(SGR.BLINK) else r.withoutModifier(SGR.BLINK) }
        modifier.isReversed?.let { r = if (it) r.withModifier(SGR.REVERSE) else r.withoutModifier(SGR.REVERSE) }
        modifier.isStrikethrough?.let {
            r = if (it) r.withModifier(SGR.CROSSED_OUT) else r.withoutModifier(SGR.CROSSED_OUT)
        }
        return r
    }

    private fun Style.toLanternaStyle(): StyleSet<*> =
        StyleSet.Set().apply {
            foregroundColor = this@toLanternaStyle.foregroundColor.toLanternaColor()
            backgroundColor = this@toLanternaStyle.backgroundColor.toLanternaColor()

            if (isBold) enableModifiers(SGR.BOLD)
            if (isItalic) enableModifiers(SGR.ITALIC)
            if (isUnderlined) enableModifiers(SGR.UNDERLINE)
            if (isBlinking) enableModifiers(SGR.BLINK)
            if (isReversed) enableModifiers(SGR.REVERSE)
            if (isStrikethrough) enableModifiers(SGR.CROSSED_OUT)
        }

    private fun Color.toLanternaColor(): TextColor {
        return when (this) {
            is Color.Color256 -> TextColor.Indexed(index)
            is Color.ColorRGB -> TextColor.RGB(r, g, b)
        }
    }
}
