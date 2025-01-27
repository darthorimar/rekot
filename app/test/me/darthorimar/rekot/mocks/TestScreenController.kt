package me.darthorimar.rekot.mocks

import me.darthorimar.rekot.cursor.Cursor
import me.darthorimar.rekot.screen.ScreenController
import me.darthorimar.rekot.screen.ScreenSize

class TestScreenController : ScreenController {
    override fun refresh() {}

    override fun fullRefresh() {}

    override val screenSize: ScreenSize = ScreenSize(40, 80)

    override val cursor: Cursor
        get() = Cursor.zero()
}
