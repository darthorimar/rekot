package me.darthorimar.rekot.config.persistent

import java.nio.file.Path
import java.util.*
import kotlin.io.path.div
import kotlin.io.path.isRegularFile
import kotlin.io.path.reader
import kotlin.io.path.writer

internal object PersistentConfigFactory {
    fun readOrCreateDefault(appDir: Path): PersistentConfig {
        val config = getConfigFilePath(appDir)
        if (config.isRegularFile()) {
            val properties = Properties()
            config.reader().use { properties.load(it) }
            return deserialize(properties)
        } else {
            val defaultConfig = DefaultConfigFactory.createDefaultConfig(appDir, config)
            val properties = serialize(defaultConfig)
            config.writer().use { properties.store(it, null) }
            return defaultConfig
        }
    }

    fun getConfigFilePath(appDir: Path) = appDir / "config.properties"

    private fun serialize(config: PersistentConfig): Properties =
        Properties().apply {
            setProperty(PersistentConfig::stdlibPath.name, config.stdlibPath)
            config.javaHome?.let { javaHome -> setProperty(PersistentConfig::javaHome.name, javaHome) }
            setProperty(PersistentConfig::tabSize.name, config.tabSize.toString())
            setProperty(PersistentConfig::rgbColors.name, config.rgbColors.toString())
        }

    private fun deserialize(properties: Properties): PersistentConfig =
        PersistentConfig(
            properties.getProperty(PersistentConfig::stdlibPath.name),
            properties.getProperty(PersistentConfig::javaHome.name),
            properties.getProperty(PersistentConfig::tabSize.name).toInt(),
            properties.getProperty(PersistentConfig::rgbColors.name).toBoolean(),
        )
}
