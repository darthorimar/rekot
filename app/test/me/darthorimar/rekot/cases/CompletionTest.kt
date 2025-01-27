package me.darthorimar.rekot.cases

import io.kotest.core.spec.style.FunSpec
import io.kotest.koin.KoinExtension
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import me.darthorimar.rekot.app.appModule
import me.darthorimar.rekot.completion.*
import me.darthorimar.rekot.infra.AppTest
import me.darthorimar.rekot.infra.CellExecuting
import me.darthorimar.rekot.mocks.mockModule
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolLocation
import org.jetbrains.kotlin.lexer.KtTokens
import org.koin.test.get

class CompletionTest : FunSpec(), AppTest, CellExecuting {
    private fun complete(): CompletionPopup? {
        val cell = editor.focusedCell
        return get<CompletionPopupFactory>().createPopup(cell.id, cell.text, cell.cursor, CompletionSession())
    }

    private fun completeAndCheckItems(vararg expected: CompletionItem, nothingElse: Boolean = false) {
        val popup = complete()
        val actual = popup?.elements.orEmpty()
        actual shouldContainAll expected.toList()

        if (nothingElse) {
            (actual - expected) shouldBe emptyList()
        }
    }

    private fun Collection<CompletionItem>.render() = joinToString(separator = "\n") { it.toString() }

    private fun Array<out CompletionItem>.render() = toList().render()

    init {
        initAppTest()
        extension(KoinExtension(listOf(appModule, mockModule)))

        context("identifier position") {
            context("toplevel") {
                test("Empty Cell") {
                    initCell("l<C>")
                    completeAndCheckItems(
                        declarationCompletionItem {
                            show = "listOf(element: T): List<T>"
                            insert = "listOf()"
                            moveCaret = -1
                            tag = CompletionItemTag.FUNCTION
                            location = KaSymbolLocation.TOP_LEVEL
                            fqName = "kotlin.collections.listOf"
                        },
                        declarationCompletionItem {
                            show = "listOfNotNull(element: T): List<T>"
                            insert = "listOfNotNull()"
                            moveCaret = -1
                            tag = CompletionItemTag.FUNCTION
                            location = KaSymbolLocation.TOP_LEVEL
                            fqName = "kotlin.collections.listOfNotNull"
                        },
                    )
                }

                test("print(ln)") {
                    initCell("p<C>")
                    completeAndCheckItems(
                        declarationCompletionItem {
                            show = "print(message: Any?): Unit"
                            insert = "print()"
                            moveCaret = -1
                            tag = CompletionItemTag.FUNCTION
                            location = KaSymbolLocation.TOP_LEVEL
                            fqName = "kotlin.io.print"
                        },
                        declarationCompletionItem {
                            show = "println(): Unit"
                            insert = "println()"
                            tag = CompletionItemTag.FUNCTION
                            location = KaSymbolLocation.TOP_LEVEL
                            fqName = "kotlin.io.println"
                        },
                    )
                }

                test("classifiers") {
                    initCell(
                        """
                            |class AA
                            |typealias AAA = Int
                            |object AAAA
                            |A<C>
                            """
                            .trimMargin())
                    completeAndCheckItems(
                        declarationCompletionItem {
                            show = "AA"
                            insert = "AA"
                            tag = CompletionItemTag.CLASS
                            location = KaSymbolLocation.TOP_LEVEL
                            fqName = "AA"
                        },
                        declarationCompletionItem {
                            show = "AAA"
                            insert = "AAA"
                            tag = CompletionItemTag.CLASS
                            location = KaSymbolLocation.TOP_LEVEL
                            fqName = "AAA"
                        },
                        declarationCompletionItem {
                            show = "AAAA"
                            insert = "AAAA"
                            tag = CompletionItemTag.CLASS
                            location = KaSymbolLocation.TOP_LEVEL
                            fqName = "AAAA"
                        },
                    )
                }
            }

            context("members") {
                test("from this class") {
                    initCell(
                        """
                    |class A {
                    |   val aa = 10
                    |   fun aaa() {}
                    |
                    |   fun foo() {
                    |       a<C>
                    |   }
                    |}
                     """
                            .trimMargin())
                    completeAndCheckItems(
                        declarationCompletionItem {
                            show = "aa: Int"
                            insert = "aa"
                            name = "aa"
                            tag = CompletionItemTag.PROPERTY
                            location = KaSymbolLocation.CLASS
                        },
                        declarationCompletionItem {
                            show = "aaa(): Unit"
                            insert = "aaa()"
                            name = "aaa"
                            tag = CompletionItemTag.FUNCTION
                            location = KaSymbolLocation.CLASS
                        },
                    )
                }

                test("from super class") {
                    initCell(
                        """
                    |open class Super {
                    |  val to = 10
                    |  fun too() {}
                    |}
                    |
                    |class A: Super() {
                    |   fun foo() {
                    |       t<C>
                    |   }
                    |}
                     """
                            .trimMargin())
                    completeAndCheckItems(
                        declarationCompletionItem {
                            show = "to: Int"
                            insert = "to"
                            name = "to"
                            tag = CompletionItemTag.PROPERTY
                            location = KaSymbolLocation.CLASS
                        },
                        declarationCompletionItem {
                            show = "too(): Unit"
                            insert = "too()"
                            name = "too"
                            tag = CompletionItemTag.FUNCTION
                            location = KaSymbolLocation.CLASS
                        },
                        declarationCompletionItem {
                            show = "toString(): String"
                            insert = "toString()"
                            name = "toString"
                            tag = CompletionItemTag.FUNCTION
                            location = KaSymbolLocation.CLASS
                        },
                    )
                }
            }

            context("receivers") {
                test("Extension receiver") {
                    initCell(
                        """
                    |fun String.foo() {
                    |    l<C>
                    |}
                     """
                            .trimMargin())
                    completeAndCheckItems(
                        declarationCompletionItem {
                            show = "length: Int"
                            insert = "length"
                            name = "length"
                            tag = CompletionItemTag.PROPERTY
                            location = KaSymbolLocation.CLASS
                        })
                }

                test("with") {
                    initCell(
                        """
                    |with("string") {
                    |    l<C>
                    |}
                     """
                            .trimMargin())
                    completeAndCheckItems(
                        declarationCompletionItem {
                            show = "length: Int"
                            insert = "length"
                            name = "length"
                            tag = CompletionItemTag.PROPERTY
                            location = KaSymbolLocation.CLASS
                        })
                }
            }
        }

        context("after dot") {
            test("string methods") {
                initCell(
                    """
                    |val s = "string"
                    |s.l<C>
                     """
                        .trimMargin())
                completeAndCheckItems(
                    declarationCompletionItem {
                        show = "length: Int"
                        insert = "length"
                        name = "length"
                        tag = CompletionItemTag.PROPERTY
                        location = KaSymbolLocation.CLASS
                    },
                    declarationCompletionItem {
                        show = "lines(): List<String>"
                        insert = "lines()"
                        tag = CompletionItemTag.FUNCTION
                        location = KaSymbolLocation.TOP_LEVEL
                        fqName = "kotlin.text.lines"
                    },
                    declarationCompletionItem {
                        show = "lowercase(): String"
                        insert = "lowercase()"
                        tag = CompletionItemTag.FUNCTION
                        location = KaSymbolLocation.TOP_LEVEL
                        fqName = "kotlin.text.lowercase"
                    },
                )
            }

            test("Collection methods") {
                initCell(
                    """
                    |val l = listOf(1, 2, 3)
                    |l.f<C>
                     """
                        .trimMargin())
                completeAndCheckItems(
                    declarationCompletionItem {
                        show = "filter(predicate: (Int) -> Boolean): List<Int>"
                        insert = "filter {   }"
                        moveCaret = -3
                        tag = CompletionItemTag.FUNCTION
                        location = KaSymbolLocation.TOP_LEVEL
                        fqName = "kotlin.collections.filter"
                    },
                    declarationCompletionItem {
                        show = "filterNot(predicate: (Int) -> Boolean): List<Int>"
                        insert = "filterNot {   }"
                        moveCaret = -3
                        tag = CompletionItemTag.FUNCTION
                        location = KaSymbolLocation.TOP_LEVEL
                        fqName = "kotlin.collections.filterNot"
                    },
                    declarationCompletionItem {
                        show = "first(): Int"
                        insert = "first()"
                        tag = CompletionItemTag.FUNCTION
                        location = KaSymbolLocation.TOP_LEVEL
                        fqName = "kotlin.collections.first"
                    },
                )
            }

            test("Paths.get") {
                initCell(
                    """
                    |import java.nio.file.Paths
                    |val p = Paths.<C>
                     """
                        .trimMargin())
                completeAndCheckItems(
                    declarationCompletionItem {
                        show = "get(p0: String, p1: String): Path?"
                        insert = "get()"
                        moveCaret = -1
                        fqName = "java.nio.file.Paths.get"
                        tag = CompletionItemTag.FUNCTION
                        location = KaSymbolLocation.CLASS
                    },
                    declarationCompletionItem {
                        show = "get(p0: URI): Path?"
                        insert = "get()"
                        moveCaret = -1
                        fqName = "java.nio.file.Paths.get"
                        tag = CompletionItemTag.FUNCTION
                        location = KaSymbolLocation.CLASS
                    },
                    nothingElse = true,
                )
            }
        }

        context("type") {
            test("property") {
                initCell("val a: L<C>")
                completeAndCheckItems(
                    declarationCompletionItem {
                        show = "List<E>"
                        insert = "List"
                        tag = CompletionItemTag.CLASS
                        location = KaSymbolLocation.TOP_LEVEL
                        fqName = "kotlin.collections.List"
                    },
                    declarationCompletionItem {
                        show = "Long"
                        insert = "Long"
                        tag = CompletionItemTag.CLASS
                        location = KaSymbolLocation.TOP_LEVEL
                        fqName = "kotlin.Long"
                    },
                )
            }
            test("function") {
                initCell("fun foo(): L<C>")
                completeAndCheckItems(
                    declarationCompletionItem {
                        show = "List<E>"
                        insert = "List"
                        tag = CompletionItemTag.CLASS
                        location = KaSymbolLocation.TOP_LEVEL
                        fqName = "kotlin.collections.List"
                    },
                    declarationCompletionItem {
                        show = "Long"
                        insert = "Long"
                        tag = CompletionItemTag.CLASS
                        location = KaSymbolLocation.TOP_LEVEL
                        fqName = "kotlin.Long"
                    },
                )
            }

            test("super type") {
                initCell("class A: L<C>")
                completeAndCheckItems(
                    declarationCompletionItem {
                        show = "List<E>"
                        insert = "List"
                        tag = CompletionItemTag.CLASS
                        location = KaSymbolLocation.TOP_LEVEL
                        fqName = "kotlin.collections.List"
                    },
                    declarationCompletionItem {
                        show = "Long"
                        insert = "Long"
                        tag = CompletionItemTag.CLASS
                        location = KaSymbolLocation.TOP_LEVEL
                        fqName = "kotlin.Long"
                    },
                )
            }

            test("parameter") {
                initCell("fun foo(a: L<C>)")
                completeAndCheckItems(
                    declarationCompletionItem {
                        show = "List<E>"
                        insert = "List"
                        tag = CompletionItemTag.CLASS
                        location = KaSymbolLocation.TOP_LEVEL
                        fqName = "kotlin.collections.List"
                    },
                    declarationCompletionItem {
                        show = "Long"
                        insert = "Long"
                        tag = CompletionItemTag.CLASS
                        location = KaSymbolLocation.TOP_LEVEL
                        fqName = "kotlin.Long"
                    },
                )
            }
        }

        context("type after dot") {
            test("property") {
                initCell(
                    """
                    |class A { class B }
                    |val a: A.<C>
                     """
                        .trimMargin())
                completeAndCheckItems(
                    declarationCompletionItem {
                        show = "B"
                        insert = "B"
                        tag = CompletionItemTag.CLASS
                        location = KaSymbolLocation.CLASS
                        fqName = "A.B"
                    })
            }

            test("function") {
                initCell(
                    """
                    |class A { class B }
                    |fun foo(): A.<C>
                     """
                        .trimMargin())
                completeAndCheckItems(
                    declarationCompletionItem {
                        show = "B"
                        insert = "B"
                        tag = CompletionItemTag.CLASS
                        location = KaSymbolLocation.CLASS
                        fqName = "A.B"
                    })
            }

            test("super type") {
                initCell(
                    """
                    |class A { class B }
                    |class C: A.<C>
                     """
                        .trimMargin())
                completeAndCheckItems(
                    declarationCompletionItem {
                        show = "B"
                        insert = "B"
                        tag = CompletionItemTag.CLASS
                        location = KaSymbolLocation.CLASS
                        fqName = "A.B"
                    })
            }

            test("parameter") {
                initCell(
                    """
                    |class A { class B }
                    |fun foo(a: A.<C>)
                     """
                        .trimMargin())
                completeAndCheckItems(
                    declarationCompletionItem {
                        show = "B"
                        insert = "B"
                        tag = CompletionItemTag.CLASS
                        location = KaSymbolLocation.CLASS
                        fqName = "A.B"
                    })
            }

            test("Int.") {
                initCell("Int.<C>")
                completeAndCheckItems(
                    declarationCompletionItem {
                        show = "MAX_VALUE: Int"
                        insert = "MAX_VALUE"
                        fqName = "kotlin.Int.Companion.MAX_VALUE"
                        tag = CompletionItemTag.PROPERTY
                        location = KaSymbolLocation.CLASS
                    },
                    declarationCompletionItem {
                        show = "MIN_VALUE: Int"
                        insert = "MIN_VALUE"
                        fqName = "kotlin.Int.Companion.MIN_VALUE"
                        tag = CompletionItemTag.PROPERTY
                        location = KaSymbolLocation.CLASS
                    },
                    declarationCompletionItem {
                        show = "SIZE_BITS: Int"
                        insert = "SIZE_BITS"
                        fqName = "kotlin.Int.Companion.SIZE_BITS"
                        tag = CompletionItemTag.PROPERTY
                        location = KaSymbolLocation.CLASS
                    },
                    declarationCompletionItem {
                        show = "SIZE_BYTES: Int"
                        insert = "SIZE_BYTES"
                        fqName = "kotlin.Int.Companion.SIZE_BYTES"
                        tag = CompletionItemTag.PROPERTY
                        location = KaSymbolLocation.CLASS
                    },
                    declarationCompletionItem {
                        show = "equals(other: Any?): Boolean"
                        insert = "equals()"
                        moveCaret = -1
                        name = "equals"
                        tag = CompletionItemTag.FUNCTION
                        location = KaSymbolLocation.CLASS
                    },
                    declarationCompletionItem {
                        show = "hashCode(): Int"
                        insert = "hashCode()"
                        tag = CompletionItemTag.FUNCTION
                        name = "hashCode"
                        location = KaSymbolLocation.CLASS
                    },
                    declarationCompletionItem {
                        show = "toString(): String"
                        insert = "toString()"
                        tag = CompletionItemTag.FUNCTION
                        name = "toString"
                        location = KaSymbolLocation.CLASS
                    },
                    nothingElse = true,
                )
            }

            test("Int.A") {
                initCell("Int.Arr<C>")
                completeAndCheckItems(nothingElse = true)
            }
        }

        context("res") {
            test("prev from the same cell") {
                initCell("1 + 1<C>")
                repeat(5) {
                    val result = executeFocussedCell()
                    result.resultValue shouldBe 2
                }
                updateCellText("r<C>")
                completeAndCheckItems(
                    declarationCompletionItem {
                        show = "res1: Int"
                        insert = "res1"
                        name = "res1"
                        tag = CompletionItemTag.PROPERTY
                        location = KaSymbolLocation.CLASS
                    },
                    declarationCompletionItem {
                        show = "res2: Int"
                        insert = "res2"
                        name = "res2"
                        tag = CompletionItemTag.PROPERTY
                        location = KaSymbolLocation.CLASS
                    },
                    declarationCompletionItem {
                        show = "res3: Int"
                        insert = "res3"
                        name = "res3"
                        tag = CompletionItemTag.PROPERTY
                        location = KaSymbolLocation.CLASS
                    },
                    declarationCompletionItem {
                        show = "res4: Int"
                        insert = "res4"
                        name = "res4"
                        tag = CompletionItemTag.PROPERTY
                        location = KaSymbolLocation.CLASS
                    },
                    declarationCompletionItem {
                        show = "res5: Int"
                        insert = "res5"
                        name = "res5"
                        tag = CompletionItemTag.PROPERTY
                        location = KaSymbolLocation.CLASS
                    },
                )
            }

            test("from other cells") {
                initCells("1 + 1<C>", "2 + 2<C>", "3 + 3<C>")
                executeAllCells().map { it.resultValue } shouldBe listOf(2, 4, 6)
                initCell("r<C>")
                completeAndCheckItems(
                    declarationCompletionItem {
                        show = "res1: Int"
                        insert = "res1"
                        name = "res1"
                        tag = CompletionItemTag.PROPERTY
                        location = KaSymbolLocation.CLASS
                    },
                    declarationCompletionItem {
                        show = "res2: Int"
                        insert = "res2"
                        name = "res2"
                        tag = CompletionItemTag.PROPERTY
                        location = KaSymbolLocation.CLASS
                    },
                    declarationCompletionItem {
                        show = "res3: Int"
                        insert = "res3"
                        name = "res3"
                        tag = CompletionItemTag.PROPERTY
                        location = KaSymbolLocation.CLASS
                    },
                )
            }
        }

        context("auto import") {
            test("math abs") {
                initCell("a<C>")

                completeAndCheckItems(
                    declarationCompletionItem {
                        show = "abs(n: Int): Int"
                        insert = "abs()"
                        moveCaret = -1
                        tag = CompletionItemTag.FUNCTION
                        location = KaSymbolLocation.TOP_LEVEL
                        fqName = "kotlin.math.abs"
                        withImport()
                    })
            }

            test("java Date") {
                initCell("Dat<C>")
                completeAndCheckItems(
                    declarationCompletionItem {
                        show = "Date"
                        insert = "Date"
                        tag = CompletionItemTag.CLASS
                        location = KaSymbolLocation.TOP_LEVEL
                        fqName = "java.util.Date"
                        withImport()
                    })
            }
        }

        context("keywords") {
            test("fun") {
                initCell("f<C>")
                completeAndCheckItems(keywordCompletionItem(KtTokens.FUN_KEYWORD) { withSpace() })
            }
            test("val/var") {
                initCell("va<C>")
                completeAndCheckItems(
                    keywordCompletionItem(KtTokens.VAL_KEYWORD) { withSpace() },
                    keywordCompletionItem(KtTokens.VAR_KEYWORD) { withSpace() },
                )
            }
            test("class") {
                initCell("cla<C>")
                completeAndCheckItems(keywordCompletionItem(KtTokens.CLASS_KEYWORD) { withSpace() })
            }
        }
    }
}
