package me.darthorimar.rekot.screen

import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType
import com.googlecode.lanterna.screen.Screen
import me.darthorimar.rekot.app.AppComponent
import me.darthorimar.rekot.events.Event
import org.koin.core.component.inject

class KeyboardInputPoller(private val screen: Screen) : AppComponent {
    private val hackyMacBugFix: HackyMacBugFix by inject()

    fun pollAndFire() {
        pollKeyboard()?.let { fireEvent(it) }
    }

    private fun pollKeyboard(): Event? {
        val keyStroke = screen.pollInput() ?: return null
        hackyMacBugFix.scheduleAfterTyping()
        val textToPaste = handlePasteAction(keyStroke)
        val character = keyStroke.realCharacter

        return when {
            textToPaste != null -> Event.Keyboard.Typing.TextTyping(textToPaste)
            keyStroke.isCtrlDown && character == 'e' -> Event.Keyboard.ExecuteCell
            keyStroke.isCtrlDown && character == 'n' -> Event.Keyboard.NewCell
            keyStroke.isCtrlDown && character == 'd' -> Event.Keyboard.DeleteCell
            keyStroke.isCtrlDown && character == 'l' -> Event.Keyboard.ClearCell
            keyStroke.isCtrlDown && character == 'r' -> Event.Keyboard.RefreshScreen
            keyStroke.isCtrlDown && character == 'b' -> Event.Keyboard.StopExecution
            keyStroke.keyType == KeyType.F1 -> Event.Keyboard.ShowHelp
            keyStroke.keyType == KeyType.ArrowDown ->
                Event.Keyboard.ArrowButton(Event.Keyboard.ArrowButton.Direction.DOWN)
            keyStroke.keyType == KeyType.ArrowUp -> Event.Keyboard.ArrowButton(Event.Keyboard.ArrowButton.Direction.UP)
            keyStroke.keyType == KeyType.ArrowLeft ->
                Event.Keyboard.ArrowButton(Event.Keyboard.ArrowButton.Direction.LEFT)
            keyStroke.keyType == KeyType.ArrowRight ->
                Event.Keyboard.ArrowButton(Event.Keyboard.ArrowButton.Direction.RIGHT)
            keyStroke.keyType == KeyType.Delete -> Event.Keyboard.Delete
            keyStroke.keyType == KeyType.Backspace -> Event.Keyboard.Backspace
            keyStroke.keyType == KeyType.Enter -> Event.Keyboard.Enter
            keyStroke.keyType == KeyType.ReverseTab -> Event.Keyboard.ShiftTab
            keyStroke.keyType == KeyType.Tab -> Event.Keyboard.Tab
            keyStroke.keyType == KeyType.Escape -> Event.Keyboard.Escape
            keyStroke.keyType == KeyType.EOF -> Event.CloseApp
            character != null -> Event.Keyboard.Typing.CharTyping(character)
            else -> null
        }
    }

    private fun handlePasteAction(keyStroke: KeyStroke): String? {
        val firstChar = keyStroke.realCharacter ?: return null
        var moreInput: KeyStroke? = screen.pollInput() ?: return null
        val textToPaste = StringBuilder()
        while (moreInput != null) {
            if (textToPaste.isEmpty()) {
                textToPaste.append(firstChar)
            }
            moreInput.realCharacter?.let { textToPaste.append(it) }
            moreInput = screen.pollInput()
        }
        if (textToPaste.isEmpty()) return null
        return textToPaste.toString()
    }

    // sometimes some special characters may appear in `KeyStroke`
    private val KeyStroke.realCharacter: Char?
        get() {
            val char = character ?: return null
            if (char == '\n') return char
            if (char == '\t') return ' '
            if (Character.isISOControl(char)) return null
            return char
        }
}
