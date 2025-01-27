package me.darthorimar.rekot.completion

import me.darthorimar.rekot.app.AppComponent
import org.apache.commons.text.similarity.FuzzyScore
import java.util.*

class CompletionItemSorter : AppComponent {
    fun sort(items: List<CompletionItem>, prefix: String): List<CompletionItem> {
        val fuzzy = FuzzyScore(Locale.US)
        return items.sortedByDescending { item ->
            val name = item.name
            when {
                name == prefix -> 100
                name.startsWith(prefix) -> 99
                name.startsWith(prefix, ignoreCase = true) -> 98
                name.contains(prefix) -> 97
                name.contains(prefix, ignoreCase = true) -> 96
                name.startsWith("res") -> 95
                else -> fuzzy.fuzzyScore(name, prefix)
            }
        }
    }

    private val declarationComparator =
        compareBy<CompletionItem.Declaration> {
            // stdlib declarations first
            it.fqName?.startsWith("kotlin.") == true
        }
}

private inline fun <T, reified S : T> comparatorFor(comparator: Comparator<S>): Comparator<T> = Comparator { a, b ->
    if (a is S && b is S) {
        comparator.compare(a, b)
    } else 0
}
