package me.darthorimar.rekot.completion

const val COMPLETION_FAKE_IDENTIFIER = "RWwgUHN5IEtvbmdyb28g"

fun matchesPrefix(prefix: String?, item: String): Boolean {
    if (prefix == null) return true
    return item.contains(prefix, ignoreCase = true)
}
