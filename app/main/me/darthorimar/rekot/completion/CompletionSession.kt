package me.darthorimar.rekot.completion

class CompletionSession {
    @Volatile private var running = true

    fun stop() {
        running = false
    }

    fun interruptIfCancelled() {
        if (!running) {
            throw InterruptedCompletionException()
        }
    }
}

class InterruptedCompletionException : Exception()
