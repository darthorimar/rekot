package me.darthorimar.rekot.util

fun <T> List<T>.limit(offset: Int, count: Int): List<T> {
    return subList(offset.coerceAtLeast(0), offset.coerceAtLeast(0) + count.coerceAtMost(size))
}
