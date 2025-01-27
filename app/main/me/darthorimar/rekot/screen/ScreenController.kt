package me.darthorimar.rekot.screen

import me.darthorimar.rekot.app.AppComponent
import me.darthorimar.rekot.cursor.Cursor

interface ScreenController : AppComponent {
    fun refresh()

    fun fullRefresh()

    val screenSize: ScreenSize

    val cursor: Cursor
}

data class ScreenSize(val rows: Int, val columns: Int)
