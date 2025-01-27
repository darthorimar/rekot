package me.darthorimar.rekot.execution

import java.nio.file.Path

sealed interface ExecutionResult {
    val sout: String?
    val scriptClassFqName: String
    val scriptInstance: Any
    val classRoot: Path

    class Result(
        val value: Any?,
        val resultVariableName: String,
        override val scriptClassFqName: String,
        override val sout: String?,
        override val scriptInstance: Any,
        override val classRoot: Path,
    ) : ExecutionResult

    class Void(
        override val sout: String?,
        override val scriptClassFqName: String,
        override val scriptInstance: Any,
        override val classRoot: Path,
    ) : ExecutionResult
}
