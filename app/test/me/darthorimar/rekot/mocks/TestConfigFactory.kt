package me.darthorimar.rekot.mocks

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.io.path.createDirectory
import kotlin.io.path.div
import me.darthorimar.rekot.config.APP_NAME
import me.darthorimar.rekot.config.APP_NAME_LOWERCASE
import me.darthorimar.rekot.config.AppConfig
import me.darthorimar.rekot.config.ColorSpace

object TestConfigFactory {
    private val indexDir by lazy {
        Files.createTempDirectory(APP_NAME_LOWERCASE + "_index").apply { resolve("data").createDirectory() }
    }

    fun createTestConfig(): AppConfig {
        val appDir = Files.createTempDirectory("${APP_NAME}_test")
        val stdlibPath = getStdlibPath()
        return AppConfig(
                appDir = appDir,
                logsDir = (appDir / "logs").createDirectories(),
                indexDir = indexDir,
                tmpDir = (appDir / "tmp").createDirectories(),
                javaHome = Paths.get(System.getProperty("java.home")),
                tabSize = 2,
                colorSpace = ColorSpace.RGB,
                hackyMacFix = false,
                libraries = listOf(stdlibPath),
            )
            .also { it.init() }
    }

    private fun getStdlibPath(): Path {
        val kotlinStdlib = Sequence::class.java.protectionDomain.codeSource.location

        return Paths.get(kotlinStdlib.file).toAbsolutePath().also { println("Found stdlib for tests at $it") }
    }
}
