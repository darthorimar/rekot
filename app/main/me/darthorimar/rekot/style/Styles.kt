@file:Suppress("PropertyName")

package me.darthorimar.rekot.style

import me.darthorimar.rekot.app.AppComponent
import me.darthorimar.rekot.config.AppConfig
import me.darthorimar.rekot.config.ColorSpace
import org.koin.core.component.inject

class Styles : AppComponent {
    private val colors: Colors by inject()

    val HELP = style {
        foregroundColor(colors.DEFAULT)
        backgroundColor(colors.CELL_BG)
    }

    val HEADER = style {
        backgroundColor(colors.CELL_BG)
        foregroundColor(colors.COMMENT)
        italic()
    }

    val CODE = style {
        backgroundColor(colors.EDITOR_BG)
        foregroundColor(colors.DEFAULT)
    }

    val EMPTY = style {
        foregroundColor(colors.DEFAULT)
        backgroundColor(colors.SEPARATOR_BG)
    }

    val KEYWORD = CODE.with { foregroundColor(colors.KEYWORD) }

    val STRING = CODE.with { foregroundColor(colors.STRING) }

    val STRING_TEMPLATE_ENTRY = CODE.with { foregroundColor(colors.STRING_TEMPLATE_ENTRY) }

    val EXTENSION_FUNCTION_CALL = CODE.with { foregroundColor(colors.FUNCTION) }

    val TOP_LEVEL = styleModifier { italic() }

    val BACKING_FIELD_REFERENCE =
        CODE.with {
            bold()
            underline()
        }

    val PROPERTY = CODE.with { foregroundColor(colors.PROPERTY) }

    val NUMBER = CODE.with { foregroundColor(colors.NUMBER) }

    val COMMENT =
        CODE.with {
            foregroundColor(colors.COMMENT)
            italic()
        }

    val SOUT = style {
        foregroundColor(colors.DEFAULT)
        backgroundColor(colors.SOUT_BG)
        italic()
    }

    val COMPLETION = style {
        backgroundColor(colors.COMPLETION_POPUP_BG)
        foregroundColor(colors.DEFAULT)
    }

    val COMPLETION_SELECTED = styleModifier { backgroundColor(colors.COMPLETION_SELECTED_POPUP_BG) }

    val MUTABLE = styleModifier { italic() }

    val ERROR = styleModifier { foregroundColor(colors.ERROR) }
}

class Colors : AppComponent {
    private val config: AppConfig by inject()

    val CELL_BG = color("0x000000", 0)
    val EDITOR_BG = color("0x1E1E22", 234)
    val SEPARATOR_BG = color("0x000000", 0)
    val ERROR_BG = color("0x6d353c", 131)
    val COMPLETION_POPUP_BG = color("0x2b2d30", 236)
    val COMPLETION_SELECTED_POPUP_BG = color("0x43454a", 238)
    val SOUT_BG = EDITOR_BG

    val DEFAULT = color("0xBCBEC4", 250)
    val COMMENT = color("0x7A7E85", 243)
    val LINE_NUMBER = color("0x3D3D42", 238)
    val KEYWORD = color("0xCF8E6D", 179)
    val STRING = color("0x6AAB73", 108)
    val STRING_TEMPLATE_ENTRY = color("0xCF8E6D", 179)
    val NUMBER = color("0x2AACB8", 38)
    val ERROR = color("0x6d353c", 131)

    val FUNCTION = color("0x57AAF7", 75)
    val PROPERTY = color("0xC77DBB", 176)
    val CLASS = color("0x6CC24A", 113)
    val LOCAL_VARIABLE = color("0xF5A623", 214)

    private fun color(hex: String, index: Int): Color {
        return when (config.colorSpace) {
            ColorSpace.Xterm256 -> Color.Color256(index)
            ColorSpace.RGB -> Color.ColorRGB.hex(hex)
        }
    }
}
