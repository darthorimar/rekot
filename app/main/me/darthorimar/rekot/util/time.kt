package me.darthorimar.rekot.util

import me.darthorimar.rekot.logging.error
import me.darthorimar.rekot.logging.logger
import kotlin.time.measureTime

fun <R> withTimeLogging(
    activity: String,
    block: () -> R,
): R {
    val result: Result<R>
    val time = measureTime { result = runCatching { block() } }
    result.onFailure { exception ->
        logger.error("$activity failed", exception)
    }.onSuccess {
        logger.info("$activity took $time")
    }
    return result.getOrThrow()
}

private val logger = logger("timing")
