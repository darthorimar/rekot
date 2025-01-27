package me.darthorimar.rekot.completion

import me.darthorimar.rekot.completion.CompletionItem.Declaration
import me.darthorimar.rekot.completion.CompletionItem.Keyword
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolLocation
import org.jetbrains.kotlin.lexer.KtSingleValueToken

sealed interface CompletionItem {
    val show: String
    val insert: String
    val tag: CompletionItemTag
    val moveCaret: Int
    val name: String

    data class Declaration(
        override val show: String,
        override val insert: String,
        override val name: String,
        override val tag: CompletionItemTag,
        override val moveCaret: Int = 0,
        val import: Boolean = false,
        val location: KaSymbolLocation,
        val fqName: String?,
    ) : CompletionItem {
        override fun toString(): String {
            return """|{
                      |  show: $show,
                      |  insert: $insert,
                      |  tag: $tag,
                      |  moveCaret: $moveCaret,
                      |  import: $import,
                      |  location: $location,
                      |  fqName: $fqName
                      |}"""
                .trimMargin()
        }
    }

    data class Keyword(
        override val show: String,
        override val insert: String = show,
        override val name: String,
        override val moveCaret: Int = 0,
    ) : CompletionItem {
        override val tag: CompletionItemTag
            get() = CompletionItemTag.KEYWORD

        override fun toString(): String {
            return """|{
                      |  show: $show,
                      |  insert: $insert,
                      |  moveCaret: $moveCaret,
                      |  tag: $tag
                      |}"""
                .trimIndent()
        }
    }
}

class KeywordItemBuilder {
    lateinit var textToShow: String
    lateinit var textToInsert: String
    var moveCaret: Int = 0
    lateinit var name: String

    fun with(completionItem: Keyword) {
        textToShow = completionItem.show
        textToInsert = completionItem.insert
        moveCaret = completionItem.moveCaret
        name = completionItem.name
    }

    fun withSpace() {
        textToInsert += " "
    }

    fun build() = Keyword(textToShow, textToInsert, name, moveCaret)
}

class DeclarationCompletionItemBuilder {
    lateinit var show: String
    var insert: String? = null
    lateinit var tag: CompletionItemTag
    var moveCaret: Int = 0
    var import: Boolean = false
    lateinit var location: KaSymbolLocation
    var fqName: String? = null
    var name: String? = null

    fun withImport() {
        import = true
    }

    fun with(completionItem: Declaration) {
        show = completionItem.show
        insert = completionItem.insert
        tag = completionItem.tag
        moveCaret = completionItem.moveCaret
        import = completionItem.import
        location = completionItem.location
        fqName = completionItem.fqName
        name = completionItem.name
    }

    fun build() =
        Declaration(
            show = show,
            insert = insert ?: show,
            name = name ?: fqName?.substringAfterLast(".") ?: error("No name provided"),
            tag = tag,
            moveCaret = moveCaret,
            import = import,
            location = location,
            fqName = fqName,
        )
}

fun declarationCompletionItem(block: DeclarationCompletionItemBuilder.() -> Unit): Declaration {
    val builder = DeclarationCompletionItemBuilder().apply(block)
    return builder.build()
}

fun keywordCompletionItem(keyword: KtSingleValueToken, block: KeywordItemBuilder.() -> Unit = {}): Keyword {
    return keywordCompletionItem {
        textToShow = keyword.value
        textToInsert = keyword.value
        name = keyword.value
        apply(block)
    }
}

fun keywordCompletionItem(block: KeywordItemBuilder.() -> Unit = {}): Keyword {
    val builder = KeywordItemBuilder().apply { apply(block) }
    return builder.build()
}

enum class CompletionItemTag(val text: String) {
    FUNCTION("ⓕ"),
    PROPERTY("ⓟ"),
    CLASS("ⓒ"),
    LOCAL_VARIABLE("ⓥ"),
    KEYWORD("ⓚ"),
}
