package me.darthorimar.rekot.execution

import me.darthorimar.rekot.app.AppComponent
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.PrintStream

class ConsoleInterceptor : AppComponent {
    fun <R> intercept(block: () -> R): Pair<R, String?> {
        val buffer = ByteArrayOutputStream()
        val stream = PrintStream(buffer)

        val originalOut = System.out
        val originalIn = System.`in`
        System.setOut(stream)
        System.setIn(exceptionThrowingIn)
        val result =
            try {
                block()
            } finally {
                System.setIn(originalIn)
                System.setOut(originalOut)
            }
        val sout = buffer.toByteArray().decodeToString().takeUnless { it.isEmpty() }
        return result to sout
    }
}

private val exceptionThrowingIn =
    object : InputStream() {
        override fun read(): Int {
            throw ReadFromSystemInNotAllowedException()
        }
    }

class ReadFromSystemInNotAllowedException : Exception(MESSAGE) {
    companion object {
        const val MESSAGE = "Reading from `System.in` is not allowed"
    }
}
