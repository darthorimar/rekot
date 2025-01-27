package me.darthorimar.rekot.style

interface StyleModifier {
    val foregroundColor: Color?
    val backgroundColor: Color?
    val isBold: Boolean?
    val isItalic: Boolean?
    val isUnderlined: Boolean?
    val isBlinking: Boolean?
    val isReversed: Boolean?
    val isStrikethrough: Boolean?

    companion object {
        val EMPTY = styleModifier {}
    }
}

interface StyleModifierBuilder : StyleBuilder

fun styleModifier(init: StyleModifierBuilder.() -> Unit): StyleModifier {
    return StyleModifierBuilderImpl().apply(init).build()
}

private class StyleModifierImpl(
    override val foregroundColor: Color?,
    override val backgroundColor: Color?,
    override val isBold: Boolean?,
    override val isItalic: Boolean?,
    override val isUnderlined: Boolean?,
    override val isBlinking: Boolean?,
    override val isReversed: Boolean?,
    override val isStrikethrough: Boolean?,
) : StyleModifier

private class StyleModifierBuilderImpl : StyleModifierBuilder {
    private var foregroundColor: Color? = null
    private var backgroundColor: Color? = null
    private var isBold: Boolean? = null
    private var isItalic: Boolean? = null
    private var isUnderlined: Boolean? = null
    private var isBlinking: Boolean? = null
    private var isReversed: Boolean? = null
    private var isStrikethrough: Boolean? = null

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
        foregroundColor = modifier.foregroundColor ?: foregroundColor
        backgroundColor = modifier.backgroundColor ?: backgroundColor
        isBold = modifier.isBold ?: isBold
        isItalic = modifier.isItalic ?: isItalic
        isUnderlined = modifier.isUnderlined ?: isUnderlined
        isBlinking = modifier.isBlinking ?: isBlinking
        isReversed = modifier.isReversed ?: isReversed
        isStrikethrough = modifier.isStrikethrough ?: isStrikethrough
    }

    override val currentStyle: Style
        get() = style { with(build()) }

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

    fun build(): StyleModifier =
        StyleModifierImpl(
            foregroundColor = foregroundColor,
            backgroundColor = backgroundColor,
            isBold = isBold,
            isItalic = isItalic,
            isUnderlined = isUnderlined,
            isBlinking = isBlinking,
            isReversed = isReversed,
            isStrikethrough = isStrikethrough,
        )
}
