package me.darthorimar.rekot.execution

import me.darthorimar.rekot.cells.CellId
import java.io.IOException
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap


class CellsClassLoader(
    jarPaths: List<Path>,
) : ClassLoader(getSystemClassLoader()) {
    private val urlClassLoader: URLClassLoader
    private val loadedClassesCache = ConcurrentHashMap<String, Class<*>>()

    init {
        val urls: Array<URL> = try {
            jarPaths.map { it.toUri().toURL() }.toTypedArray()
        } catch (e: IOException) {
            throw IllegalArgumentException("Failed to convert JAR paths to URLs", e)
        }
        urlClassLoader = URLClassLoader(urls, parent)
    }

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
        // Check if already loaded and cached
        loadedClassesCache[name]?.let { return it }

        // Try loading from the map
        map[name]?.let { file ->
            val clazz = defineClass(name, file.content, 0, file.content.size)
            loadedClassesCache[name] = clazz
            return clazz
        }

        return try {
            val clazz = urlClassLoader.loadClass(name)
            loadedClassesCache[name] = clazz
            clazz
        } catch (e: ClassNotFoundException) {
            throw ClassNotFoundException("Class $name not found in map or JARs", e)
        }
    }

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        if (name.startsWith("java.") || name.startsWith("kotlin.")) {
            return super.loadClass(name, resolve)
        }

        synchronized(getClassLoadingLock(name)) {
            val clazz = findLoadedClass(name) ?: findClass(name)
            if (resolve) {
                resolveClass(clazz)
            }
            return clazz
        }
    }
}