package me.darthorimar.rekot.analysis

import me.darthorimar.rekot.app.AppComponent
import me.darthorimar.rekot.logging.error
import me.darthorimar.rekot.logging.logger

private val loggerLoggerErrorInterceptor = logger<CompilerErrorInterceptor>()

interface CompilerErrorInterceptor : AppComponent {
    fun intercept(error: Throwable)
}

class LoggingErrorInterceptor : CompilerErrorInterceptor {
    override fun intercept(error: Throwable) {
        loggerLoggerErrorInterceptor.error("Compiler error", error)
    }
}
