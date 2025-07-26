package me.darthorimar.rekot.projectStructure.index

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.intellij.psi.PsiElement
import com.intellij.psi.StubBasedPsiElement
import com.intellij.psi.impl.source.PsiFileImpl
import com.intellij.psi.stubs.PsiFileStub
import java.nio.file.Path
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream
import me.darthorimar.rekot.app.AppComponent
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreProjectEnvironment
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.koin.core.component.inject
import kotlin.jvm.java

@Suppress("INVISIBLE_REFERENCE", "EXPOSED_PARAMETER_TYPE", "EXPOSED_FUNCTION_RETURN_TYPE")
class IndexSerializer : AppComponent {
    private val stubTableSerializer: StubTableSerializer by inject()

    fun deserialize(from: Path, kotlinCoreEnvironment: KotlinCoreProjectEnvironment): Index {
        return from.inputStream().use { inputStream ->
            Input(inputStream).use { input -> deserialize(input, kotlinCoreEnvironment) }
        }
    }

    fun deserializeStamp(from: Path): IndexStamp {
        return from.inputStream().use { inputStream ->
            Input(inputStream).use { input -> deserializeStamp(input) }
        }
    }

    fun serialize(index: Index, to: Path) {
        to.outputStream().use { outputStream ->
            Output(outputStream).use { output ->
                serializeStamp(index.indexStamp, output)
                stubTableSerializer.serialize(index.stubTable, output)
                deserializeDeclarationIndex(index.declarationIndex, index.stubTable, output)
            }
        }
    }

    private fun deserializeStamp(input: Input): IndexStamp {
        val stubVersion = input.readInt()
        val indexerVersion = input.readInt()
        val timestamp = input.readLong()
        return IndexStamp(stubVersion, indexerVersion, timestamp)
    }

    private fun serializeStamp(stamp: IndexStamp, output: Output) {
        output.writeInt(stamp.stubVersion)
        output.writeInt(stamp.indexerVersion)
        output.writeLong(stamp.timestamp)
    }

    private fun deserialize(input: Input, kotlinCoreEnvironment: KotlinCoreProjectEnvironment): Index {
        val stamp = deserializeStamp(input)
        val table = stubTableSerializer.deserialize(input, kotlinCoreEnvironment)
        return Index(
            stamp,
            stubTable = table,
            declarationIndex = serializeDeclarationIndex(input, table),
        )
    }

    private fun serializeDeclarationIndex(input: Input, table: StubTable): DeclarationIndex {
        val kryo = configureKryo(table)
        return kryo.readObject(input, DeclarationIndex::class.java)
    }

    private fun deserializeDeclarationIndex(declarationIndex: DeclarationIndex, table: StubTable, output: Output) {
        val kryo = configureKryo(table)
        kryo.writeObject(output, declarationIndex)
    }

    private fun configureKryo(table: StubTable): Kryo {
        val kryo = Kryo()
        val psiSerializer = StubToIdSerializer(table.ids)

        kryo.apply {
            isRegistrationRequired = false

            addDefaultSerializer(PsiElement::class.java, psiSerializer)
            register(Name::class.java, NameSerializer)
            register(FqName::class.java, FqNameSerializer)
            register(ClassId::class.java, ClassIdSerializer)
            register(CallableId::class.java, CallableIdSerializer)
        }
        return kryo
    }
}

private class StubToIdSerializer(private val stubIds: StubIds) : Serializer<PsiElement>() {
    override fun write(kryo: Kryo, output: Output, value: PsiElement) {
        val stub =
            when (value) {
                is PsiFileImpl -> value.stub ?: error("PsiFileImpl without stub: $value")
                is StubBasedPsiElement<*> -> value.stub ?: error("StubBasedPsiElement without stub: $value")
                else -> error("Expected PsiFileImpl or StubBasedPsiElement, got: ${value::class.java}")
            }
        val id = stubIds.stubToId.getInt(stub)
        output.writeInt(id)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out PsiElement>): PsiElement {
        val id = input.readInt()
        val stub = stubIds.idToStub[id] ?: error("Unknown stub ID: $id")
        return when (stub) {
            is PsiFileStub<*> -> stub.psi ?: error("No stub")
            else -> stub.psi as PsiElement
        }
    }
}

private object NameSerializer : Serializer<Name>() {
    override fun write(kryo: Kryo, output: Output, `object`: Name) {
        output.writeString(`object`.asString())
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out Name>): Name {
        return Name.guessByFirstCharacter(input.readString())
    }
}

private object FqNameSerializer : Serializer<FqName>() {
    override fun write(kryo: Kryo, output: Output, `object`: FqName) {
        output.writeString(`object`.asString())
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out FqName>): FqName {
        return FqName(input.readString())
    }
}

private object ClassIdSerializer : Serializer<ClassId>() {
    override fun write(kryo: Kryo, output: Output, `object`: ClassId) {
        output.writeString(`object`.asString())
        output.writeBoolean(`object`.isLocal)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out ClassId>): ClassId {
        return ClassId.fromString(input.readString(), input.readBoolean())
    }
}

private object CallableIdSerializer : Serializer<CallableId>() {
    override fun write(kryo: Kryo, output: Output, `object`: CallableId) {
        kryo.writeObject(output, `object`.classId)
        kryo.writeObject(output, `object`.callableName)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out CallableId>): CallableId {
        return CallableId(
            kryo.readObject(input, ClassId::class.java),
            kryo.readObject(input, Name::class.java),
        )
    }
}
