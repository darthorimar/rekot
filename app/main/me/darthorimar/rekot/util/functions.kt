package me.darthorimar.rekot.util

@Suppress("UNCHECKED_CAST") fun <T> id(): (T) -> T = _id as (T) -> T

private val _id = { a: Any? -> a }
