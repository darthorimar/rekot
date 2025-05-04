package me.darthorimar.rekot.screen

import com.googlecode.lanterna.screen.Screen
import me.darthorimar.rekot.app.AppComponent
import me.darthorimar.rekot.app.SubscriptionContext
import me.darthorimar.rekot.config.AppConfig
import org.koin.core.component.inject
import java.util.*
import kotlin.concurrent.schedule

/*
 * Mac prints some garbage characters when the app is open, so we need to refresh the screen
 */
class HackyMacBugFix(private val screen: Screen) : AppComponent {
    private val appConfig: AppConfig by inject()

    private val enabled get() = appConfig.hackyMacFix
    private var timerInitiated = false
    private val timer by lazy { Timer(true) }

    context(SubscriptionContext)
    override fun performSubscriptions() {
        if (!enabled) return
        if (System.getProperty("os.name").lowercase().contains("mac")) {
            schedule(1000)
            schedule(2000)
            schedule(4000)
        }
    }

    fun scheduleAfterTyping() {
        if (!enabled) return
        if (timerInitiated) return
        timerInitiated = true
        schedule(500)
        schedule(1000)
    }

    private fun schedule(delayMS: Long) {
        if (!enabled) return
        timer.schedule(delayMS /*ms*/) { screen.refresh(Screen.RefreshType.COMPLETE) }
    }
}
