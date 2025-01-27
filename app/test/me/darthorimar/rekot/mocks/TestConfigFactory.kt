package me.darthorimar.rekot.mocks

import me.darthorimar.rekot.config.APP_NAME
import me.darthorimar.rekot.config.AppConfig
import me.darthorimar.rekot.config.ColorSpace
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.io.path.div

object TestConfigFactory {
    fun createTestConfig(): AppConfig {
        val appDir = Files.createTempDirectory("${APP_NAME}_test")
        return AppConfig(
            logsDir = (appDir / "logs").createDirectories(),
                tmpDir = (appDir / "tmp").createDirectories(),
                stdlibPath = getStdlibPath(),
                javaHome = Paths.get(System.getProperty("java.home")),
                colorSpace = ColorSpace.RGB,
                tabSize = 2,
            )
            .also { it.init() }
    }

    private fun getStdlibPath(): Path {
        val kotlinStdlib = Sequence::class.java.protectionDomain.codeSource.location

        return Paths.get(kotlinStdlib.file).toAbsolutePath().also { println("Found stdlib for tests at $it") }
    }
}
