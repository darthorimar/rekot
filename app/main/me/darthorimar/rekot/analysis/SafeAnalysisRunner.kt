package me.darthorimar.rekot.analysis

import me.darthorimar.rekot.app.AppComponent
import org.jetbrains.kotlin.util.PrivateForInline
import org.koin.core.component.inject

class SafeAnalysisRunner : AppComponent {
    @PrivateForInline val interceptor: CompilerErrorInterceptor by inject()

    @OptIn(PrivateForInline::class)
    inline fun <R> runSafely(defaultValue: R? = null, action: () -> R): R? {
        // sometimes Analysis API may fail during analysis and this may break the whole App
        return runCatching { action() }.onFailure { interceptor.intercept(it) }.getOrElse { defaultValue }
    }
}
