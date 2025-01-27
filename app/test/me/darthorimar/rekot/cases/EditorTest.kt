package me.darthorimar.rekot.cases

import io.kotest.core.spec.style.FunSpec
import io.kotest.koin.KoinExtension
import io.kotest.matchers.shouldBe
import me.darthorimar.rekot.app.appModule
import me.darthorimar.rekot.infra.AppTest
import me.darthorimar.rekot.mocks.mockModule

class EditorTest : FunSpec(), AppTest {
    init {
        initAppTest()
        extension(KoinExtension(listOf(appModule, mockModule)))

        context("Typing") {
            test("Char typing") {
                initCell()
                editor.type('a').P()
                cellText shouldBe "a<C>"
            }
            test("String typing") {
                initCell()
                editor.type("abcde").P()
                cellText shouldBe "abcde<C>"
            }
            test("Multiple typings with caret move") {
                initCell()
                editor.type("abcde").P()
                editor.type('_').P()
                repeat(5) { editor.left() }.P()
                editor.type("f").P()
                repeat(2) { editor.right() }.P()
                editor.type("rr").P()
                cellText shouldBe "afbcrr<C>de_"
            }
        }

        context("Enter handler") {
            context("Function declaration") {
                test("{") {
                    initCell("fun foo() {<C>")
                    editor.enter().P()
                    cellText shouldBe
                        """
                        |fun foo() {
                        |  <C>"""
                            .trimMargin()
                }

                test("{}") {
                    initCell("fun foo() {<C>}")
                    editor.enter().P()
                    cellText shouldBe
                        """
                    |fun foo() {
                    |  <C>
                    |}
                """
                            .trimMargin()
                }
            }

            context("If statement") {
                test("{") {
                    initCell(
                        """
                        |fun foo() {
                        |  if (true) {<C>
                        |}"""
                            .trimMargin())
                    editor.enter().P()
                    cellText shouldBe
                        """
                        |fun foo() {
                        |  if (true) {
                        |    <C>
                        |}"""
                            .trimMargin()
                }

                test("{}") {
                    initCell(
                        """
                        |fun foo() {
                        |  if (true) {<C>}
                        |}"""
                            .trimMargin())
                    editor.enter().P()
                    cellText shouldBe
                        """
                        |fun foo() {
                        |  if (true) {
                        |    <C>
                        |  }
                        |}"""
                            .trimMargin()
                }
            }
        }

        context("Braces insertion") {
            test("fun foo() {}") {
                initCell("fun foo() <C>")
                editor.type('{').P()
                cellText shouldBe "fun foo() {<C>}"
            }

            test("class X {}") {
                initCell("class X<C>")
                editor.type('{').P()
                cellText shouldBe "class X{<C>}"
            }

            test("fun x()") {
                initCell("fun x<C>")
                editor.type('(').P()
                cellText shouldBe "fun x(<C>)"
            }

            test("if ()") {
                initCell("if<C>")
                editor.type('(').P()
                cellText shouldBe "if(<C>)"
            }

            test("Expr ()") {
                initCell("val q = <C>")
                editor.type('(').P()
                cellText shouldBe "val q = (<C>)"
            }

            test("while ()") {
                initCell("while<C>")
                editor.type('(').P()
                cellText shouldBe "while(<C>)"
            }

            test("for ()") {
                initCell("for<C>")
                editor.type('(').P()
                cellText shouldBe "for(<C>)"
            }

            test("try {}") {
                initCell("try<C>")
                editor.type('{').P()
                cellText shouldBe "try{<C>}"
            }

            test("when {}") {
                initCell("when<C>")
                editor.type('{').P()
                cellText shouldBe "when{<C>}"
            }

            test("foo()") {
                initCell("foo<C>")
                editor.type('(').P()
                cellText shouldBe "foo(<C>)"
            }

            test("@Annotation()") {
                initCell("@Annotation<C>")
                editor.type('(').P()
                cellText shouldBe "@Annotation(<C>)"
            }

            test("do {}") {
                initCell("do <C>")
                editor.type('{').P()
                cellText shouldBe "do {<C>}"
            }

            test("companion object") {
                initCell("companion object <C>")
                editor.type('{').P()
                cellText shouldBe "companion object {<C>}"
            }

            test("\"\"") {
                initCell("println(<C>)")
                editor.type('"').P()
                cellText shouldBe "println(\"<C>\")"
            }
        }
    }
}
