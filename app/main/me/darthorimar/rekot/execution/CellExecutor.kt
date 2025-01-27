package me.darthorimar.rekot.execution

import me.darthorimar.rekot.analysis.CellAnalysisContext
import me.darthorimar.rekot.analysis.CellAnalyzer
import me.darthorimar.rekot.analysis.CellContextKind
import me.darthorimar.rekot.app.AppComponent
import me.darthorimar.rekot.app.SubscriptionContext
import me.darthorimar.rekot.app.subscribe
import me.darthorimar.rekot.cells.Cell
import me.darthorimar.rekot.cells.CellId
import me.darthorimar.rekot.config.AppConfig
import me.darthorimar.rekot.events.Event
import org.jetbrains.kotlin.analysis.api.components.KaCompilationResult
import org.jetbrains.kotlin.analysis.api.components.KaCompilerTarget
import org.jetbrains.kotlin.analysis.api.components.isClassFile
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.fileClasses.internalNameWithoutInnerClasses
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.koin.core.component.inject
import java.lang.reflect.InvocationTargetException
import java.net.URLClassLoader
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeBytes

class CellExecutor : AppComponent {
    private val appConfig: AppConfig by inject()
    private val interceptor: ConsoleInterceptor by inject()
    private val analyzer: CellAnalyzer by inject()
    private val executionStateProvider: CellExecutionStateProvider by inject()

    private val executingThread = ExecutingThread()

    private var resIndex = 1
    private var executionNumber = 0

    private val classLoader = CellsClassLoader(URLClassLoader(arrayOf(appConfig.stdlibPath.toUri().toURL())))

    context(SubscriptionContext)
    override fun performSubscriptions() {
        subscribe<Event.CellExecutionStateChanged> { event ->
            executionNumber++
            if (event.state is CellExecutionState.Executed) {
                resIndex++
            }
        }
    }

    fun stop(cell: Cell) {
        val executionState = executionStateProvider.getCellExecutionState(cell.id)
        if (executionState is CellExecutionState.Executing) {
            if (executingThread.stop(cell.id)) {
                fireEvent(Event.CellExecutionStateChanged(cell.id, CellExecutionState.Error("Execution interrupted")))
            }
        }
    }

    fun execute(cell: Cell) {
        when (executingThread.currentlyExecuting) {
            cell.id -> {
                fireEvent(
                    Event.CellExecutionStateChanged(
                        cell.id, CellExecutionState.Error("Cell is already being executed")))
                return
            }
            is CellId -> {
                fireEvent(
                    Event.CellExecutionStateChanged(
                        cell.id, CellExecutionState.Error("Another cell is being executed")))
                return
            }
            null -> {}
        }
        val cellErrors = analyzer.inAnalysisContext(cell) { errors.allErrors }
        if (cellErrors.isNotEmpty()) {
            fireEvent(Event.CellExecutionStateChanged(cell.id, CellExecutionState.Error(cellErrors.first().message)))
            return
        }
        fireEvent(Event.CellExecutionStateChanged(cell.id, CellExecutionState.Executing))

        analyzer.inAnalysisContext(cell, kind = CellContextKind.Execution(executionNumber, resIndex)) {
            when (val compilationResult = compileCell()) {
                is KaCompilationResult.Failure -> {
                    fireEvent(
                        Event.CellExecutionStateChanged(
                            cell.id,
                            CellExecutionState.Error(compilationResult.errors.firstOrNull()?.defaultMessage ?: "Error"),
                        ))
                }

                is KaCompilationResult.Success -> {
                    executeCell(cell, compilationResult)
                }

                null -> {
                    fireEvent(
                        Event.CellExecutionStateChanged(
                            cell.id,
                            CellExecutionState.Error("Internal compiler error during execution"),
                        ))
                }
            }
        }
    }

    context(CellAnalysisContext)
    private fun executeCell(cell: Cell, compilationResult: KaCompilationResult.Success) {
        val className = ktFile.getCellClassName()
        val compiledFiles = compilationResult.toCompiledFiles()

        classLoader.updateEntries(cell.id, compiledFiles)
        val classToExecute = classLoader.loadClass(className)

        val classRoot = putFilesOnDisk(compiledFiles)

        val resultPropertyName = cellKtModule.resultVariableName!!
        val methodName = "get${resultPropertyName.capitalizeAsciiOnly()}"

        val method = classToExecute.methods.find { it.name == methodName }

        executingThread.execute(cell.id) {
            try {
                val (mainClassInstance, sout) = interceptor.intercept { createMainClassInstance(classToExecute) }
                val result =
                    if (method == null) {
                        ExecutionResult.Void(sout, className, mainClassInstance, classRoot)
                    } else {
                        val result = method.invoke(mainClassInstance)
                        ExecutionResult.Result(
                            result,
                            resultPropertyName,
                            className,
                            sout,
                            mainClassInstance,
                            classRoot,
                        )
                    }
                fireEvent(Event.CellExecutionStateChanged(cell.id, CellExecutionState.Executed(result)))
            } catch (e: InvocationTargetException) {
                @Suppress("DEPRECATION")
                val error =
                    when (e.cause) {
                        is InterruptedException,
                        is ThreadDeath -> "Execution interrupted"
                        else -> e.cause?.message ?: e.message ?: "Execution error"
                    }
                fireEvent(Event.CellExecutionStateChanged(cell.id, CellExecutionState.Error(error)))
            } catch (e: Exception) {
                fireEvent(
                    Event.CellExecutionStateChanged(cell.id, CellExecutionState.Error(e.message ?: "Execution error")))
            }
        }
    }

    context(CellAnalysisContext)
    private fun createMainClassInstance(mainClass: Class<*>): Any {
        val classes = cellKtModule.dependentScriptInstances
        val constructor = mainClass.getConstructor(*classes.map { it::class.java }.toTypedArray())
        return constructor.newInstance(*classes.toTypedArray())
    }

    private fun KtFile.getCellClassName(): String = script!!.fqName.internalNameWithoutInnerClasses

    private fun putFilesOnDisk(compiledFiles: List<CompiledFile>): Path {
        val tmpDir = appConfig.tmpDir.resolve(System.currentTimeMillis().toString()).createDirectory()
        for (kaCompiledFile in compiledFiles) {
            val file = tmpDir.resolve(kaCompiledFile.path)
            file.createParentDirectories()
            file.writeBytes(kaCompiledFile.content)
        }
        return tmpDir
    }

    private fun KaCompilationResult.Success.toCompiledFiles(): List<CompiledFile> =
        output.map { file ->
            when {
                file.isClassFile ->
                    CompiledFile.CompiledClass(
                        file.path.removeSuffix(".class").replace("/", ".").replace("\\", "."),
                        file.path,
                        file.content,
                    )

                else -> CompiledFile.CompiledNonClassFile(file.path, file.content)
            }
        }

    context(CellAnalysisContext)
    private fun compileCell(): KaCompilationResult? = analyze {
        val config =
            CompilerConfiguration().apply {
                put(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, LanguageVersionSettingsImpl.DEFAULT)
            }
        compile(ktFile, config, KaCompilerTarget.Jvm(isTestMode = false), allowedErrorFilter = { false })
    }
}
