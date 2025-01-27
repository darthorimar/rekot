package me.darthorimar.rekot.style

class Style(
    val foregroundColor: Color,
    val backgroundColor: Color,
    val isBold: Boolean,
    val isItalic: Boolean,
    val isUnderlined: Boolean,
    val isBlinking: Boolean,
    val isReversed: Boolean,
    val isStrikethrough: Boolean,
) {
    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String {
        return buildString {
            append("{F=${foregroundColor}, BG=${backgroundColor}")
            if (isBold) append(", B")
            if (isItalic) append(", I")
            if (isUnderlined) append(", U")
            if (isBlinking) append(", BL")
            if (isReversed) append(", R")
            if (isStrikethrough) append(", S")
            append("}")
        }
    }
}

interface StyleBuilder {
    val currentStyle: Style

    fun foregroundColor(color: Color)

    fun backgroundColor(color: Color)

    fun from(style: Style)

    fun with(modifier: StyleModifier)

    fun bold()

    fun italic()

    fun underline()

    fun blink()

    fun reverse()

    fun strikethrough()

    fun noBold()

    fun noItalic()

    fun noUnderline()

    fun noBlink()

    fun noReverse()

    fun noStrikethrough()
}

class StyleBuilderImpl(initialStyle: Style?) : StyleBuilder {
    private lateinit var foregroundColor: Color
    private lateinit var backgroundColor: Color
    private var isBold: Boolean = false
    private var isItalic: Boolean = false
    private var isUnderlined: Boolean = false
    private var isBlinking: Boolean = false
    private var isReversed: Boolean = false
    private var isStrikethrough: Boolean = false

    init {
        if (initialStyle != null) {
            from(initialStyle)
        }
    }

    override fun from(style: Style) {
        foregroundColor = style.foregroundColor
        backgroundColor = style.backgroundColor
        isBold = style.isBold
        isItalic = style.isItalic
        isUnderlined = style.isUnderlined
        isBlinking = style.isBlinking
        isReversed = style.isReversed
        isStrikethrough = style.isStrikethrough
    }

    override fun with(modifier: StyleModifier) {
        modifier.foregroundColor?.let { foregroundColor(it) }
        modifier.backgroundColor?.let { backgroundColor(it) }
        modifier.isBold?.let { if (it) bold() else noBold() }
        modifier.isItalic?.let { if (it) italic() else noItalic() }
        modifier.isUnderlined?.let { if (it) underline() else noUnderline() }
        modifier.isBlinking?.let { if (it) blink() else noBlink() }
        modifier.isReversed?.let { if (it) reverse() else noReverse() }
        modifier.isStrikethrough?.let { if (it) strikethrough() else noStrikethrough() }
    }

    override val currentStyle: Style
        get() = build()

    override fun foregroundColor(color: Color) {
        this.foregroundColor = color
    }

    override fun backgroundColor(color: Color) {
        this.backgroundColor = color
    }

    override fun bold() {
        this.isBold = true
    }

    override fun italic() {
        this.isItalic = true
    }

    override fun underline() {
        this.isUnderlined = true
    }

    override fun blink() {
        this.isBlinking = true
    }

    override fun reverse() {
        this.isReversed = true
    }

    override fun strikethrough() {
        this.isStrikethrough = true
    }

    override fun noBold() {
        this.isBold = false
    }

    override fun noItalic() {
        this.isItalic = false
    }

    override fun noUnderline() {
        this.isUnderlined = false
    }

    override fun noBlink() {
        this.isBlinking = false
    }

    override fun noReverse() {
        this.isReversed = false
    }

    override fun noStrikethrough() {
        this.isStrikethrough = false
    }

    fun build(): Style {
        return Style(
            foregroundColor,
            backgroundColor,
            isBold,
            isItalic,
            isUnderlined,
            isBlinking,
            isReversed,
            isStrikethrough,
        )
    }
}

fun style(initial: Style? = null, init: StyleBuilder.() -> Unit): Style {
    return StyleBuilderImpl(initialStyle = initial).apply(init).build()
}

fun Style.with(build: StyleBuilder.() -> Unit): Style {
    return StyleBuilderImpl(initialStyle = this).apply(build).build()
}

fun Style.with(modifier: StyleModifier): Style {
    return with { with(modifier) }
}
