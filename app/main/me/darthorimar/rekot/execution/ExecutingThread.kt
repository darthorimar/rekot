package me.darthorimar.rekot.execution

import me.darthorimar.rekot.app.AppComponent
import me.darthorimar.rekot.cells.CellId
import me.darthorimar.rekot.events.Event
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class ExecutingThread : AppComponent {
    private var thread: Thread? = null
    private val _executingCell = AtomicReference<CellId?>(null)

    fun execute(cellId: CellId, task: () -> Unit) {
        if (!_executingCell.compareAndSet(null, cellId)) {
            error("Cannot add task, executing thread is busy")
        }
        thread = thread {
            try {
                task()
            } finally {
                _executingCell.set(null)
            }
        }
    }

    fun stop(cellId: CellId): Boolean {
        if (_executingCell.get() == null) return false
        val thread = thread ?: return false
        thread.interrupt()
        repeat(5) {
            if (!thread.isAlive) {
                _executingCell.set(null)
                return true
            }
            Thread.sleep(100)
            // wait for the thread to stop for 500ms
        }
        try {
            @Suppress("DEPRECATION") thread.stop()
        } catch (_: UnsupportedOperationException) {
            // starting from java 20, Thread.stop() is throwing UnsupportedOperationException and does not stop the
            // thread so we can do nothing here :(
            fireEvent(
                Event.Error(
                    cellId,
                    "You are running Java ${System.getProperty("java.version")}.\n" +
                        "Fully stopping a thread that does not listen to interruptions is not supported in Java versions 20 and above.\n" +
                        "So, we cannot stop the cell execution :(",
                ))
            return false
        }
        _executingCell.set(null)
        return true
    }

    val currentlyExecuting: CellId?
        get() = _executingCell.get()
}
