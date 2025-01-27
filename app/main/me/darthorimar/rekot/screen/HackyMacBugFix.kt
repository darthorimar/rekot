package me.darthorimar.rekot.screen

import com.googlecode.lanterna.screen.Screen
import me.darthorimar.rekot.app.AppComponent
import me.darthorimar.rekot.app.SubscriptionContext
import java.util.*
import kotlin.concurrent.schedule

/*
 * Mac prints some garbage characters when the app is open, so we need to refresh the screen
 */
class HackyMacBugFix(private val screen: Screen) : AppComponent {
    private var timerInitiated = false
    private val timer = Timer(true)

    context(SubscriptionContext)
    override fun performSubscriptions() {
        if (System.getProperty("os.name").lowercase().contains("mac")) {
            schedule(1000)
            schedule(2000)
            schedule(4000)
        }
    }

    fun scheduleAfterTyping() {
        if (timerInitiated) return
        timerInitiated = true
        schedule(500)
        schedule(1000)
    }

    private fun schedule(delayMS: Long) {
        timer.schedule(delayMS /*ms*/) { screen.refresh(Screen.RefreshType.COMPLETE) }
    }
}
