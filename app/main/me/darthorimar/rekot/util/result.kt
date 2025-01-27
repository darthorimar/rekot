package me.darthorimar.rekot.util

fun <T> Result<T>.retry(f: (Throwable) -> T): Result<T> =
    when {
        isSuccess -> this
        else -> {
            val error = exceptionOrNull()!!
            runCatching { f(error) }
        }
    }
