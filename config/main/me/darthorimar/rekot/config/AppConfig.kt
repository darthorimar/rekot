package me.darthorimar.rekot.config

import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories

class AppConfig(
    val appDir: Path,
    val logsDir: Path,
    val tmpDir: Path,
    val stdlibPath: Path,
    val javaHome: Path,
    val tabSize: Int,
    val colorSpace: ColorSpace,
) {
    val completionPopupHeight = 10
    val completionPopupMinWidth = 30

    fun init() {
        logsDir.createDirectories()
        System.setProperty(LOG_DIR_PROPERTY, logsDir.absolutePathString())
    }
}

enum class ColorSpace {
    RGB,
    Xterm256
}
