package me.darthorimar.rekot.execution

sealed interface CompiledFile {
    val path: String
    val content: ByteArray

    class CompiledClass(val fqName: String, override val path: String, override val content: ByteArray) : CompiledFile

    class CompiledNonClassFile(override val path: String, override val content: ByteArray) : CompiledFile
}
