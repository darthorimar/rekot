package me.darthorimar.rekot.execution

import me.darthorimar.rekot.util.retry
import java.nio.file.Path

object ExecutorValueRenderer {
    fun render(value: Any?): String =
        runCatching {
                when (value) {
                    is Array<*> -> value.joinToString(prefix = "[", postfix = "]") { render(it) }
                    is IntArray -> value.joinToString(prefix = "[", postfix = "]") { it.toString() }
                    is ByteArray -> value.joinToString(prefix = "[", postfix = "]") { it.toString() }
                    is ShortArray -> value.joinToString(prefix = "[", postfix = "]") { it.toString() }
                    is LongArray -> value.joinToString(prefix = "[", postfix = "]") { it.toString() }
                    is FloatArray -> value.joinToString(prefix = "[", postfix = "]") { it.toString() }
                    is DoubleArray -> value.joinToString(prefix = "[", postfix = "]") { it.toString() }
                    is CharArray -> value.joinToString(prefix = "[", postfix = "]") { it.toString() }
                    is BooleanArray -> value.joinToString(prefix = "[", postfix = "]") { it.toString() }
                    is UIntArray -> value.joinToString(prefix = "[", postfix = "]") { it.toString() }
                    is UByteArray -> value.joinToString(prefix = "[", postfix = "]") { it.toString() }
                    is UShortArray -> value.joinToString(prefix = "[", postfix = "]") { it.toString() }
                    is ULongArray -> value.joinToString(prefix = "[", postfix = "]") { it.toString() }
                    is Set<*> -> value.joinToString(prefix = "{", postfix = "}") { render(it) }
                    is List<*> -> value.joinToString(prefix = "[", postfix = "]") { render(it) }
                    is Path -> value.toString() // Path is Iterable, so its check comes before Iterable
                    is Collection<*> -> value.render()
                    is Sequence<*> -> value.toString()
                    is Map<*, *> -> value.entries.joinToString(prefix = "{", postfix = "}") { render(it) }
                    is Map.Entry<*, *> -> "(${render(value.key)}, ${render(value.value)})"
                    is CharSequence -> "\"$value\""
                    is Char -> "'$value'"
                    else -> value.toString()
                }
            }
            .retry { value.toString() }
            .getOrElse { value?.let { it::class.java }.toString() }

    private fun Collection<*>.render(prefix: String = this.shortClassName) = buildString {
        append(prefix)
        append("[")
        for ((i, e) in this@render.withIndex()) {
            if (i >= LIMIT) {
                append("...")
                break
            }
            append(render(e))
            append(", ")
        }
        append("]")
    }

    private val Any.shortClassName: String
        get() = this::class.simpleName ?: this::class.java.simpleName

    const val LIMIT = 10
}
