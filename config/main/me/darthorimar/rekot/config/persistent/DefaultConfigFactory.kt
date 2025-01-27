package me.darthorimar.rekot.config.persistent

import java.nio.file.Path
import kotlin.io.path.absolutePathString
import me.darthorimar.rekot.config.APP_LOGO
import me.darthorimar.rekot.config.APP_NAME
import me.darthorimar.rekot.config.CAT_EMOJI
import me.darthorimar.rekot.config.ColorSpace
import me.darthorimar.rekot.config.stdlib.KotlinStdLibDownloader

internal object DefaultConfigFactory {
    fun createDefaultConfig(appDir: Path, configFile: Path): PersistentConfig {
        clear()
        printlnColor(green, APP_LOGO.trimIndent())
        println()
        println("$CAT_EMOJI Welcome to $APP_NAME")
        println()
        val rgbColors = chooseColorSpace(configFile) == ColorSpace.RGB
        println()
        val stdlibPath = KotlinStdLibDownloader.download(appDir)
        println()
        val tabSize = 2

        return PersistentConfig(
            stdlibPath = stdlibPath.absolutePathString(),
            javaHome = null,
            tabSize = tabSize,
            rgbColors = rgbColors,
        )
    }

    private fun chooseColorSpace(configFile: Path): ColorSpace {
        printlnColor(
            blue,
            "Please choose terminal color space.\n" +
                "Some terminals (like iTerm2) support the full RGB color space, " +
                "while others (like Terminal) support only 256 colors.\n" +
                "Choosing RGB if your terminal does not support it will result in broken colors.")
        println("${cyan}If colors are off, you can change your choice in ${configFile.toAbsolutePath()}")
        val result: ColorSpace
        while (true) {
            printColor(green, "> Do you want to use RGB color space (y/N)")
            val input = readLine()?.trim()?.lowercase()
            when (input) {
                "n",
                "no",
                "not" -> {
                    result = ColorSpace.Xterm256
                    break
                }
                "yes",
                "y" -> {
                    result = ColorSpace.RGB
                    break
                }
                else -> {
                    result = ColorSpace.Xterm256
                    break
                }
            }
        }

        return result
    }
}

private fun clear() {
    print("\u001B[H\u001B[2J")
    System.out.flush()
}

fun printColor(color: String, text: String) {
    print(color + text + reset)
}

fun printlnColor(color: String, text: String) {
    println(color + text + reset)
}

private const val reset = "\u001B[0m"
private const val black = "\u001B[30m"
private const val red = "\u001B[31m"
private const val green = "\u001B[32m"
private const val yellow = "\u001B[33m"
private const val blue = "\u001B[34m"
private const val purple = "\u001B[35m"
private const val cyan = "\u001B[36m"
private const val white = "\u001B[37m"
