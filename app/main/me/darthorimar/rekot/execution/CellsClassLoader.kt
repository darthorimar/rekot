package me.darthorimar.rekot.execution

import me.darthorimar.rekot.cells.CellId

class CellsClassLoader(stdlibClassLoader: ClassLoader) : ClassLoader(stdlibClassLoader) {
    private val indexToCompiledFile = mutableMapOf<CellId, List<CompiledFile>>()
    private var map: Map<String, CompiledFile.CompiledClass> = emptyMap()

    fun updateEntries(cellId: CellId, files: List<CompiledFile>) {
        indexToCompiledFile[cellId] = files
        map =
            indexToCompiledFile.entries
                .sortedBy { it.key }
                .asSequence()
                .flatMap { it.value }
                .filterIsInstance<CompiledFile.CompiledClass>()
                .associateBy { it.fqName }
    }

    override fun findClass(name: String): Class<*> {
        val file = map[name] ?: return super.findClass(name)
        return defineClass(name, file.content, 0, file.content.size)
    }
}
