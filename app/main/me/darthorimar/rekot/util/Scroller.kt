package me.darthorimar.rekot.util

class Scroller(private var height: Int) {
    private var _viewPosition = 0

    val viewPosition: Int
        get() = _viewPosition

    fun scroll(cursorPosition: Int): Int {
        if (cursorPosition < _viewPosition) {
            _viewPosition = cursorPosition
        } else if (cursorPosition >= _viewPosition + height) {
            _viewPosition = cursorPosition - height + 1
        }
        return viewPosition
    }

    fun resize(newHeight: Int) {
        height = newHeight
    }
}
