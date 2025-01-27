package me.darthorimar.rekot.logging

import me.darthorimar.rekot.config.APP_NAME_LOWERCASE
import me.darthorimar.rekot.config.LOG_DIR_PROPERTY
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.logging.FileHandler
import java.util.logging.Level
import java.util.logging.Logger
import java.util.logging.SimpleFormatter
import kotlin.io.path.absolutePathString
import kotlin.io.path.div

inline fun <reified C> logger(): Logger {
    val logger = Logger.getLogger(C::class.java.getName())
    val handler = FileHandler(getLogFilePath().absolutePathString(), true).apply { formatter = SimpleFormatter() }
    logger.addHandler(handler)
    return logger
}

fun Logger.error(message: String, error: Throwable) {
    log(Level.SEVERE, message, error)
}

@PublishedApi
internal fun getLogFilePath(): Path {
    val logsDir = Paths.get(System.getProperty(LOG_DIR_PROPERTY))
    val dateSuffix = LocalDate.now().format(logNameDateFormatter)
    return logsDir / "$APP_NAME_LOWERCASE-$dateSuffix.log"
}

private val logNameDateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
