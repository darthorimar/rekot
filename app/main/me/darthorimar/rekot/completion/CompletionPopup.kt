package me.darthorimar.rekot.completion

import me.darthorimar.rekot.app.AppComponent
import me.darthorimar.rekot.cells.CellId
import me.darthorimar.rekot.cells.Cells
import me.darthorimar.rekot.editor.Editor
import me.darthorimar.rekot.editor.FancyEditorCommands
import org.koin.core.component.inject

class CompletionPopup(val cellId: CellId, elements: List<CompletionItem>, prefix: String) : AppComponent {
    private val editor: Editor by inject()
    private val cells: Cells by inject()
    private val fancyEditorCommands: FancyEditorCommands by inject()
    private val sorter: CompletionItemSorter by inject()

    private var _selectedIndex = 0
    private var _prefix = prefix
    private var _elements = elements.let { sorter.sort(it, _prefix) }

    val prefix: String
        get() = _prefix

    val elements: List<CompletionItem>
        get() = _elements

    val selectedIndex: Int
        get() = _selectedIndex

    fun up() {
        if (_selectedIndex > 0) {
            _selectedIndex--
        }
    }

    fun down() {
        if (_selectedIndex < elements.lastIndex) {
            _selectedIndex++
        }
    }

    fun choseItemOnDot(): Boolean {
        val element = selectedItem()
        if (prefix == element.name) {
            choseItem()
            return true
        }
        return false
    }

    fun choseItem() {
        val element = selectedItem()
        repeat(prefix.length) { editor.backspace() }
        editor.type(element.insert)
        (element as? CompletionItem.Declaration)?.let { declaration ->
            if (declaration.import) {
                fancyEditorCommands.insertImport(cells.getCell(cellId), declaration.fqName!!)
            }
        }
        editor.offsetColumn(element.moveCaret)
    }

    private fun selectedItem(): CompletionItem = elements[_selectedIndex]

    fun addPrefix(char: Char): Boolean {
        _prefix += char
        if (_elements.isEmpty()) return false
        val selectedElement = _elements[_selectedIndex]
        _elements = _elements.filter { matchesPrefix(_prefix, it.name) }.let { sorter.sort(it, _prefix) }

        val newElementIndex = if (_selectedIndex == 0) 0 else _elements.indexOf(selectedElement)
        _selectedIndex = if (newElementIndex >= 0) newElementIndex else 0
        return elements.isNotEmpty()
    }
}
