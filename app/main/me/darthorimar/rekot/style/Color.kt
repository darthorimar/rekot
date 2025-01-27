package me.darthorimar.rekot.style

import org.jetbrains.kotlin.util.capitalizeDecapitalize.toUpperCaseAsciiOnly

sealed interface Color {
    data class Color256(val index: Int) : Color

    data class ColorRGB(val r: Int, val g: Int, val b: Int) : Color {
        override fun toString(): String {
            return String.format("#%02X%02X%02X", r, g, b)
        }

        companion object {
            fun hex(hex: String): ColorRGB {
                val cleanedHex = hex.removePrefix("0x").toUpperCaseAsciiOnly()
                val intValue = cleanedHex.toLong(16).toInt()
                val r = (intValue shr 16) and 0xFF
                val g = (intValue shr 8) and 0xFF
                val b = intValue and 0xFF
                return ColorRGB(r, g, b)
            }
        }
    }
}
