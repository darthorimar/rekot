package me.darthorimar.rekot.projectStructure.index

import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.stubs.StubElement
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import java.nio.file.Path
import kotlin.io.path.getLastModifiedTime
import org.jetbrains.kotlin.psi.stubs.KotlinStubVersions

@Suppress("EXPOSED_PARAMETER_TYPE")
class Index(
    val indexStamp: IndexStamp,
    val stubTable: StubTable,
    val declarationIndex: DeclarationIndex,
) {
    companion object {
        const val VERSION: Int = 1
    }
}

data class IndexStamp(
    val stubVersion: Int,
    val indexerVersion: Int,
    val timestamp: Long,
) {
    fun isUpToDate(root: Path): Boolean {
        return this == createFor(root)
    }

    companion object {
        fun createFor(root: Path): IndexStamp {
            return IndexStamp(
                stubVersion = KotlinStubVersions.CLASSFILE_STUB_VERSION + KotlinStubVersions.BUILTIN_STUB_VERSION,
                indexerVersion = Index.VERSION,
                timestamp = root.getLastModifiedTime().toInstant().epochSecond)
        }
    }
}

class StubTable(
    val files: List<PsiFileStub<*>>,
    val ids: StubIds,
)

class StubIds
private constructor(
    val stubToId: Object2IntMap<StubElement<*>>,
    val idToStub: Int2ObjectMap<StubElement<*>>,
) {
    val size
        get() = stubToId.size

    companion object {
        fun create(stubs: List<StubElement<*>>): StubIds {
            val stubToId = Object2IntOpenHashMap<StubElement<*>>(stubs.size)
            val idToStub = Int2ObjectOpenHashMap<StubElement<*>>(stubs.size)
            for ((index, stub) in stubs.withIndex()) {
                check(stubToId.put(stub, index) == 0)
                check(idToStub.put(index, stub) == null)
            }
            return StubIds(stubToId, idToStub)
        }

        fun create(idToStub: Int2ObjectMap<StubElement<*>>): StubIds {
            return StubIds(
                Object2IntOpenHashMap<StubElement<*>>(idToStub.size).apply {
                    for (entry in idToStub.int2ObjectEntrySet()) {
                        put(entry.value, entry.intKey)
                    }
                },
                idToStub,
            )
        }
    }
}

@Suppress("INVISIBLE_REFERENCE", "EXPOSED_TYPEALIAS_EXPANDED_TYPE")
typealias DeclarationIndex =
    org.jetbrains.kotlin.analysis.api.standalone.base.declarations.KotlinStandaloneDeclarationIndex
