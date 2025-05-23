package me.darthorimar.rekot.config

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import me.darthorimar.rekot.config.persistent.PersistentConfigFactory

object ConfigFactory {
    fun createConfig(): AppConfig {
        val appDir = getDefaultAppDirectory().createDirectories()
        val persistentConfig = PersistentConfigFactory.readOrCreateDefault(appDir)
        val javaHome =
            (persistentConfig.javaHome?.let(Paths::get) ?: getJavaHome()).also { home ->
                if (home == null) {
                    error(
                        "Java home is not provided. " +
                            "Please set `java.home` environment variable or pass set it in the ${PersistentConfigFactory.getConfigFilePath(appDir)}.")
                }
                if (!home.isDirectory()) {
                    error("$home is not a valid Java home")
                }
            }!!

        val stdlibPath =
            persistentConfig.stdlibPath.let(Paths::get).also { path ->
                if (!path.isRegularFile()) {
                    error("Invalid stdlib path, $path is not a file")
                }
            }

        val logsDir = (appDir / "logs").createDirectories()
        val tmpDir = Files.createTempDirectory(APP_NAME_LOWERCASE)
        val tabSize = persistentConfig.tabSize
        return AppConfig(
            appDir = appDir,
            logsDir = logsDir,
            tmpDir = tmpDir,
            stdlibPath = stdlibPath,
            javaHome = javaHome,
            tabSize = tabSize,
            colorSpace = if (persistentConfig.rgbColors) ColorSpace.RGB else ColorSpace.Xterm256,
            hackyMacFix = persistentConfig.hackyMacFix,
        )
    }

    private fun getJavaHome(): Path? {
        val path = System.getProperty("java.home") ?: return null
        return Paths.get(path)
    }

    fun getDefaultAppDirectory(): Path {
        val osName = System.getProperty("os.name").lowercase()

        return when {
            osName.contains("win") -> {
                val appData = System.getenv("APPDATA") ?: System.getProperty("user.home")
                Paths.get(appData, APP_NAME_LOWERCASE)
            }

            osName.contains("nix") || osName.contains("nux") || osName.contains("mac") -> {
                val xdgConfigHome =
                    System.getenv("XDG_CONFIG_HOME") ?: Paths.get(System.getProperty("user.home"), ".config").toString()
                Paths.get(xdgConfigHome, APP_NAME_LOWERCASE)
            }

            else -> {
                Paths.get(System.getProperty("user.home"), ".$APP_NAME_LOWERCASE")
            }
        }
    }
}
