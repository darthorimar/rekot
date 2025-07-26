package me.darthorimar.rekot.projectStructure.index

import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.psi.stubs.*
import com.intellij.psi.tree.IStubFileElementType
import com.intellij.util.io.AbstractStringEnumerator
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import me.darthorimar.rekot.app.AppComponent
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.SmartPointerIncompatiblePsiFile
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreProjectEnvironment
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.stubs.KotlinFileStub
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class StubTableSerializer : AppComponent {
    private val stubElementTypes: Map<String, StubSerializer<*>> by lazy {
        buildMap {
            for (type in IStubElementType.enumerate { true }) {
                val id = when (type) {
                    is IStubElementType<*, *> -> type.externalId
                    is IStubFileElementType<*> -> type.externalId
                    else -> null
                } ?: continue
                put(id, type as StubSerializer<*>)
            }
        }
    }

    private val _buffer = ByteArrayOutputStream()

    fun serialize(table: StubTable, output: Output) {
        useBuffer {
            val enumerator = CollectingStringEnumerator()
            val tableBytes = writeBytes { output -> serialize(table, output, enumerator) }
            val enumeratorBytes = writeBytes { output -> serializeEnumerator(output, enumerator) }
            output.write(enumeratorBytes)
            output.writeBytes(tableBytes)
        }
    }

    fun deserialize(input: Input, kotlinCoreEnvironment: KotlinCoreProjectEnvironment): StubTable {
        val enumerator = deserializeEnumerator(input)
        return deserialize(input, enumerator, kotlinCoreEnvironment)
    }

    private fun deserialize(
        input: Input,
        enumerator: AbstractStringEnumerator,
        kotlinCoreEnvironment: KotlinCoreProjectEnvironment
    ): StubTable {
        val stubs = mutableListOf<PsiFileStub<*>>()
        val stubById = Int2ObjectOpenHashMap<StubElement<*>>()
        val psiManager = PsiManager.getInstance(kotlinCoreEnvironment.project)
        val jarVfs = kotlinCoreEnvironment.environment.jarFileSystem
        repeat(input.readInt()) {
            val path = input.readString()
            val virtualFile = jarVfs.findFileByPath(path) ?: error("Virtual file not found for path: $path")
            val fileStub = deserializeStub(input, enumerator, stubById, parent = null) as PsiFileStubImpl<KtFile>
            val fileViewProvider = KtClassFileViewProvider(psiManager, virtualFile)
            val fakeFile = KtJarFile(fileViewProvider, fileStub as KotlinFileStub)
            fileStub.psi = fakeFile
            stubs += fileStub
        }
        return StubTable(stubs, StubIds.create(stubById))
    }

    private fun serialize(table: StubTable, output: Output, enumerator: AbstractStringEnumerator) {
        output.writeInt(table.files.size)
        for (fileStub in table.files) {
            check(fileStub is PsiFileStubImpl<*>)
            output.writeString(fileStub.psi.virtualFile.path)
            serializeStub(
                fileStub,
                fileStub.type,
                output,
                enumerator,
                table.ids.stubToId,
            )
        }
    }

    private fun serializeEnumerator(output: Output, enumerator: CollectingStringEnumerator) {
        val mappings = enumerator.mappings
        output.writeInt(mappings.size)
        for ((i, s) in mappings) {
            output.writeInt(i)
            output.writeString(s)
        }
    }

    private fun deserializeEnumerator(input: Input): AbstractStringEnumerator {
        val size = input.readInt()
        val map = Int2ObjectOpenHashMap<String>(size)
        repeat(size) {
            val key = input.readInt()
            val value = input.readString()
            map[key] = value
        }
        return FixedStringEnumerator(map)
    }

    private fun serializeStub(
        stub: StubElement<*>,
        serializer: StubSerializer<*>,
        output: Output,
        enumerator: AbstractStringEnumerator,
        stubIds: Map<StubElement<*>, Int>
    ) {
        stub as (StubElement<PsiElement>)
        serializer as StubSerializer<StubElement<PsiElement>>

        val stubBytes = useBuffer  { buffer ->
            serializer.serialize(stub, StubOutputStream(buffer, enumerator))
            buffer.toByteArray()
        }

        output.writeInt(enumerator.enumerate(serializer.externalId))
        output.writeInt(stubIds.getValue(stub))
        output.writeInt(stubBytes.size)
        output.writeBytes(stubBytes)
        val children = stub.childrenStubs

        output.writeInt(children.size)
        for (child in children) {
            serializeStub(child, child.stubType, output, enumerator, stubIds)
        }
    }

    private fun <R> useBuffer(action: (ByteArrayOutputStream) -> R): R {
        _buffer.reset()
        return action(_buffer).also { _buffer.reset() }
    }

    private fun deserializeStub(
        input: Input,
        enumerator: AbstractStringEnumerator,
        stubById: Int2ObjectOpenHashMap<StubElement<*>>,
        parent: StubElement<*>?,
    ): StubElement<*> {
        val stubTypeId = input.readInt()
        val stubId = input.readInt()
        val stubBytesSize = input.readInt()
        val stubBytes = input.readBytes(stubBytesSize)

        val stubType = stubElementTypes.getValue(enumerator.valueOf(stubTypeId)!!)
        val stub = ByteArrayInputStream(stubBytes).use { input ->
            val stubIn = StubInputStream(input, enumerator)
            stubType.deserialize(stubIn, parent)
        }

        repeat(input.readInt()) { deserializeStub(input, enumerator, stubById, parent = stub) }
        stubById[stubId] = stub
        return stub
    }
}

private class KtJarFile(
    private val fileViewProvider: KtClassFileViewProvider,
    private val fileStub: KotlinFileStub,
) : KtFile(fileViewProvider, isCompiled = true), SmartPointerIncompatiblePsiFile {
    override fun getStub(): KotlinFileStub? = fileStub

    override val greenStub: KotlinFileStub?
        get() = fileStub

    override fun isPhysical() = false
}

private class KtClassFileViewProvider(
    psiManager: PsiManager,
    virtualFile: VirtualFile,
) : SingleRootFileViewProvider(psiManager, virtualFile, true, KotlinLanguage.INSTANCE)

private class FixedStringEnumerator(
    override val intToString: Int2ObjectMap<String>,
) : AbstractStringEnumeratorBase() {
    val stringToInt = Object2IntOpenHashMap<String>(intToString.size).apply {
        for ((i, s) in intToString) {
             put(s, i)
        }
    }

    override fun enumerate(value: String?): Int {
        if (value == null) return 0
        return stringToInt.getInt(value)
    }
}

private class CollectingStringEnumerator : AbstractStringEnumeratorBase() {
    override val intToString = Int2ObjectOpenHashMap<String>()
    private val stringToInt = Object2IntOpenHashMap<String>()

    val mappings: Map<Int, String>
        get() = intToString

    private var freeId = 1

    override fun enumerate(value: String?): Int {
        if (value == null) return 0

        if (value !in stringToInt) {
            val id = freeId
            stringToInt[value] = id
            intToString[id] = value
            freeId++
            return id
        } else {
            return stringToInt.getValue(value)
        }
    }
}

private abstract class AbstractStringEnumeratorBase : AbstractStringEnumerator {
    protected abstract val intToString: Int2ObjectMap<String>

    final override fun valueOf(idx: Int): String? {
        if (idx == 0) return null // 0 is reserved for null value
        return intToString.get(idx) ?: error("No value for $idx")
    }

    final override fun markCorrupted(): Unit = shouldNotBeCalled()

    final override fun close(): Unit = shouldNotBeCalled()

    final override fun isDirty(): Boolean = shouldNotBeCalled()

    final override fun force(): Unit = shouldNotBeCalled()
}
