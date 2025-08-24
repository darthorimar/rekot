package me.darthorimar.rekot.projectStructure.index

import com.intellij.psi.stubs.StubElement
import me.darthorimar.rekot.app.AppComponent
import me.darthorimar.rekot.projectStructure.getVirtualFilesByRoots
import org.jetbrains.kotlin.analysis.api.standalone.base.declarations.KotlinStandaloneDeclarationProviderFactory
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreProjectEnvironment
import org.jetbrains.kotlin.psi.KtFile
import java.io.ByteArrayOutputStream
import java.nio.file.Path

class Indexer : AppComponent {
     fun index(root: Path, kotlinCoreProjectEnvironment: KotlinCoreProjectEnvironment): Index {
        val files = getVirtualFilesByRoots(listOf(root), kotlinCoreProjectEnvironment)
        val factory =
            KotlinStandaloneDeclarationProviderFactory(
                kotlinCoreProjectEnvironment.project,
                kotlinCoreProjectEnvironment.environment,
                sourceKtFiles = emptyList(),
                binaryRoots = files,
                shouldBuildStubsForBinaryLibraries = true,
                skipBuiltins = true,
            )
        val declarationIndex =
            KotlinStandaloneDeclarationProviderFactory::class
                .java
                .declaredFields
                .single { it.name == "index" }
                .apply { isAccessible = true }
                .get(factory) as DeclarationIndex
        val stubTable = createStubTable(declarationIndex)
        return Index(
            indexStamp = IndexStamp.createFor(root),
            stubTable = stubTable,
            declarationIndex = declarationIndex,
        )
    }

    @Suppress("INVISIBLE_REFERENCE")
    private fun createStubTable(declarationIndex: DeclarationIndex): StubTable {
        val files: Set<KtFile> = buildSet {
            declarationIndex.facadeFileMap.values.forEach { files -> addAll(files) }
            declarationIndex.multiFileClassPartMap.values.forEach { files -> addAll(files) }
            declarationIndex.scriptMap.values.forEach { files -> files.mapTo(this) { it.containingKtFile } }
            declarationIndex.classMap.values.forEach { files -> files.mapTo(this) { it.containingKtFile } }
            declarationIndex.typeAliasMap.values.forEach { files -> files.mapTo(this) { it.containingKtFile } }
            declarationIndex.topLevelFunctionMap.values.forEach { files -> files.mapTo(this) { it.containingKtFile } }
            declarationIndex.topLevelPropertyMap.values.forEach { files -> files.mapTo(this) { it.containingKtFile } }
            declarationIndex.classesBySupertypeName.values.forEach { files ->
                files.mapTo(this) { it.containingKtFile }
            }
            declarationIndex.inheritableTypeAliasesByAliasedName.values.forEach { files ->
                files.mapTo(this) { it.containingKtFile }
            }
        }
        val stubs = buildList {
            fun rec(stub: StubElement<*>) {
                add(stub)
                for (child in stub.childrenStubs) {
                    rec(child)
                }
            }
            for (file in files) {
                val stub = file.stub ?: error("Stub for file ${file.name} is null")
                rec(stub)
            }
        }
        return StubTable(
            files.map { it.stub ?: error("Stub for file ${it.name} is null") },
            ids = StubIds.create(stubs),
        )
    }
}