package me.darthorimar.rekot.cases

import io.kotest.core.spec.style.FunSpec
import io.kotest.koin.KoinExtension
import io.kotest.matchers.shouldBe
import me.darthorimar.rekot.app.appModule
import me.darthorimar.rekot.events.Event
import me.darthorimar.rekot.execution.CellExecutionState
import me.darthorimar.rekot.execution.CellExecutionStateProvider
import me.darthorimar.rekot.execution.ReadFromSystemInNotAllowedException
import me.darthorimar.rekot.infra.AppTest
import me.darthorimar.rekot.infra.CellExecuting
import me.darthorimar.rekot.mocks.mockModule
import org.koin.test.get

class ExecutorTest : FunSpec(), AppTest, CellExecuting {

    init {
        initAppTest()
        extension(KoinExtension(listOf(appModule, mockModule)))

        context("single cell") {
            test("simple expression") {
                initCell("1+1<C>")
                val result = executeFocussedCell().resultValue
                result shouldBe 2
            }

            test("top-level function invocation") {
                initCell(
                    """
                    |fun sum(a: Int, b: Int): Int {
                    |    return a + b
                    |}
                    |sum(1, 1)<C>
                """
                        .trimMargin())
                val result = executeFocussedCell().resultValue
                result shouldBe 2
            }

            test("class instance creation") {
                initCell(
                    """
                    |class A {
                    |   override fun toString() = "A"
                    |}
                    |A()<C>
                """
                        .trimMargin())
                val result = executeFocussedCell().resultValue
                result.toString() shouldBe "A"
            }

            test("import from jdk") {
                initCell(
                    """
                    |import java.time.LocalDateTime
                    |import java.time.format.DateTimeFormatter
                    |
                    |val fixedDateTime = LocalDateTime.of(2025, 1, 1, 12, 0, 0)
                    |val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    |val formattedDateTime = fixedDateTime.format(formatter)
                    |formattedDateTime<C>
                    """
                        .trimMargin())
                val result = executeFocussedCell().resultValue
                result shouldBe "2025-01-01 12:00:00"
            }

            test("import from kotlin stdlib") {
                initCell(
                    """
                    |import kotlin.math.sqrt
                    |import kotlin.collections.List
                    |
                    |val numbers: List<Int> = listOf(1, 4, 9, 16, 25)
                    |val squareRoots = numbers.map { sqrt(it.toDouble()) }
                    |numbers to squareRoots<C>
                    """
                        .trimMargin())
                val result = executeFocussedCell().resultValue
                result shouldBe (listOf(1, 4, 9, 16, 25) to listOf(1.0, 2.0, 3.0, 4.0, 5.0))
            }

            test("sout capture") {
                initCells("repeat(3) { println(it) }<C>")
                val result = executeFocussedCell()
                result.sout shouldBe "0\n1\n2\n"
            }

            test("sout&result capture") {
                initCells("var x = 1; repeat(3) { println(it); x *= 2 }; x<C>")
                val result = executeFocussedCell()
                result.resultValue shouldBe 8
                result.sout shouldBe "0\n1\n2\n"
            }

            test("using collections from Java") {
                initCell(
                    """
                    |import java.util.ArrayList
                    |val list = ArrayList<Int>()
                    |list.add(1)
                    |list.add(2)
                    |list.add(3)
                    |list.sum()<C>
                    """
                        .trimMargin())
                val result = executeFocussedCell().resultValue
                result shouldBe 6
            }

            test("readLine is not allowed") {
                initCell("readLine()!!<C>")
                val result = executeCell(cells.cells.first())
                result shouldBe CellExecutionState.Error(ReadFromSystemInNotAllowedException.MESSAGE)
            }
        }

        context("Reuse the same cell") {
            test("reuse res") {
                initCell("1+1<C>")
                val result1 = executeFocussedCell().resultValue
                result1 shouldBe 2
                updateCellText("res1 + 1<C>")
                val result2 = executeFocussedCell().resultValue
                result2 shouldBe 3
            }

            test("reuse cell after error") {
                initCell("error(\"AAAA\")<C>")
                val result1 = executeFocussedCell()
                result1.error shouldBe "AAAA"
                updateCellText("1<C>")
                val result2 = executeFocussedCell()
                result2.resultValue shouldBe 1
            }

            test("reuse cell after interruption") {
                initCell("Thread.sleep(1_000_000)<C>")

                cellExecutor.execute(focusedCell).P()
                val state1 = get<CellExecutionStateProvider>().getCellExecutionState(focusedCell.id)
                state1 shouldBe CellExecutionState.Executing

                Thread.sleep(1000)

                fireEvent(Event.Keyboard.StopExecution).P()
                Thread.sleep(1000).P()
                val state2 = get<CellExecutionStateProvider>().getCellExecutionState(focusedCell.id)
                state2 shouldBe CellExecutionState.Error("Execution interrupted")
            }
        }

        context("multiple cells") {
            test("res from previous") {
                initCells("1 + 1<C>", "res1 + 1<C>", "res1 + res2<C>")
                val results = executeAllCells().map { it.resultValue }
                results shouldBe listOf(2, 3, 5)
            }

            test("val from previous") {
                initCells("val a = 5<C>", "a + 1<C>")
                val results = executeAllCells().last().resultValue
                results shouldBe 6
            }

            test("fun from previous") {
                initCells("fun a() = 5<C>", "a() + 1<C>")
                val results = executeAllCells().last().resultValue
                results shouldBe 6
            }

            test("class from previous") {
                initCells("class A(val x: Int)<C>\n 1", "A(1).x<C>")
                val results = executeAllCells().last().resultValue
                results shouldBe 1
            }

            test("object state from previous") {
                initCells("object X { var x = 10 }; X.x++<C>", "X.x++; X.x<C>")
                val results = executeAllCells().last().resultValue
                results shouldBe 12
            }
        }
    }
}
