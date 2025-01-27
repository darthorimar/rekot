package me.darthorimar.rekot.mocks

import me.darthorimar.rekot.analysis.CompilerErrorInterceptor
import me.darthorimar.rekot.analysis.LoggingErrorInterceptor
import me.darthorimar.rekot.config.AppConfig
import me.darthorimar.rekot.screen.ScreenController
import org.koin.dsl.module

val mockModule = module {
    single<ScreenController> { TestScreenController() }
    single<AppConfig> { TestConfigFactory.createTestConfig() }
    single<CompilerErrorInterceptor> { LoggingErrorInterceptor() }
}
