/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.compose.ui.text.platform

import org.jetbrains.skia.Font as SkFont
import org.jetbrains.skia.FontStyle as SkFontStyle
import org.jetbrains.skia.paragraph.Alignment as SkAlignment
import org.jetbrains.skia.paragraph.DecorationLineStyle as SkDecorationLineStyle
import org.jetbrains.skia.paragraph.DecorationStyle as SkDecorationStyle
import org.jetbrains.skia.paragraph.Direction as SkDirection
import org.jetbrains.skia.paragraph.Paragraph as SkParagraph
import org.jetbrains.skia.paragraph.ParagraphBuilder as SkParagraphBuilder
import org.jetbrains.skia.paragraph.Shadow as SkShadow
import org.jetbrains.skia.paragraph.TextIndent as SkTextIndent
import org.jetbrains.skia.paragraph.TextStyle as SkTextStyle
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.text.*
import androidx.compose.ui.text.AnnotatedString.Range
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import org.jetbrains.skia.FontFeature
import org.jetbrains.skia.Paint
import org.jetbrains.skia.paragraph.*
import org.jetbrains.skia.paragraph.ParagraphStyle

private val DefaultFontSize = 16.sp

@Suppress("DEPRECATION")
@Deprecated(
    "Font.ResourceLoader is deprecated, instead pass FontFamily.Resolver",
    replaceWith = ReplaceWith("ActualParagraph(text, style, spanStyles, placeholders, " +
        "maxLines, ellipsis, width, density, fontFamilyResolver)"),
)
internal actual fun ActualParagraph(
    text: String,
    style: TextStyle,
    spanStyles: List<Range<SpanStyle>>,
    placeholders: List<Range<Placeholder>>,
    maxLines: Int,
    ellipsis: Boolean,
    width: Float,
    density: Density,
    @Suppress("DEPRECATION") resourceLoader: Font.ResourceLoader
): Paragraph = SkiaParagraph(
    SkiaParagraphIntrinsics(
        text,
        style,
        spanStyles,
        placeholders,
        density,
        createFontFamilyResolver(resourceLoader)
    ),
    maxLines,
    ellipsis,
    Constraints(maxWidth = width.ceilToInt())
)

internal actual fun ActualParagraph(
    text: String,
    style: TextStyle,
    spanStyles: List<Range<SpanStyle>>,
    placeholders: List<Range<Placeholder>>,
    maxLines: Int,
    ellipsis: Boolean,
    constraints: Constraints,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver
): Paragraph = SkiaParagraph(
    SkiaParagraphIntrinsics(
        text,
        style,
        spanStyles,
        placeholders,
        density,
        fontFamilyResolver
    ),
    maxLines,
    ellipsis,
    constraints
)

internal actual fun ActualParagraph(
    paragraphIntrinsics: ParagraphIntrinsics,
    maxLines: Int,
    ellipsis: Boolean,
    constraints: Constraints
): Paragraph = SkiaParagraph(
    paragraphIntrinsics as SkiaParagraphIntrinsics,
    maxLines,
    ellipsis,
    constraints
)

// Computed ComputedStyles always have font/letter size in pixels for particular `density`.
// It's important because density could be changed in runtime, and it should force
// SkTextStyle to be recalculated. Or we can have different densities in different windows.
@OptIn(ExperimentalTextApi::class)
internal data class ComputedStyle(
    var textForegroundStyle: TextForegroundStyle,
    var brushSize: Size,
    var fontSize: Float,
    var fontWeight: FontWeight?,
    var fontStyle: FontStyle?,
    var fontSynthesis: FontSynthesis?,
    var fontFamily: FontFamily?,
    var fontFeatureSettings: String?,
    var letterSpacing: Float?,
    var baselineShift: BaselineShift?,
    var textGeometricTransform: TextGeometricTransform?,
    var localeList: LocaleList?,
    var background: Color = Color.Unspecified,
    var textDecoration: TextDecoration?,
    var textDecorationLineStyle: TextDecorationLineStyle?,
    var shadow: Shadow?,
    var drawStyle: DrawStyle?,
    var blendMode: BlendMode,
    var lineHeight: Float?,
) {

    constructor(
        density: Density,
        spanStyle: SpanStyle,
        brushSize: Size = Size.Unspecified,
        blendMode: BlendMode = DrawScope.DefaultBlendMode,
        lineHeight: TextUnit,
    ) : this(
        textForegroundStyle = spanStyle.textForegroundStyle,
        brushSize = brushSize,
        fontSize = with(density) { spanStyle.fontSize.toPx() },
        fontWeight = spanStyle.fontWeight,
        fontStyle = spanStyle.fontStyle,
        fontSynthesis = spanStyle.fontSynthesis,
        fontFamily = spanStyle.fontFamily,
        fontFeatureSettings = spanStyle.fontFeatureSettings,
        letterSpacing = if (spanStyle.letterSpacing.isSpecified) {
            with(density) { spanStyle.letterSpacing.toPx() }
        } else null,
        baselineShift = spanStyle.baselineShift,
        textGeometricTransform = spanStyle.textGeometricTransform,
        localeList = spanStyle.localeList,
        background = spanStyle.background,
        textDecoration = spanStyle.textDecoration,
        textDecorationLineStyle = spanStyle.platformStyle?.textDecorationLineStyle,
        shadow = spanStyle.shadow,
        drawStyle = spanStyle.drawStyle,
        blendMode = blendMode,
        lineHeight = if (lineHeight.isSpecified) {
            lineHeight.toPx(density, spanStyle.fontSize)
        } else null,
    )

    private fun toTextPaint(): Paint? = Paint().let {
        with(it.asComposePaint()) {
            color = textForegroundStyle.color
            applyBrush(textForegroundStyle.brush, brushSize, textForegroundStyle.alpha)
            applyDrawStyle(drawStyle)
            blendMode = this@ComputedStyle.blendMode
            return@let it.takeIf { shader != null || style != PaintingStyle.Fill || !it.isSrcOver }
        }
    }

    fun toSkTextStyle(fontFamilyResolver: FontFamily.Resolver): SkTextStyle {
        val res = SkTextStyle()
        if (textForegroundStyle.color.isSpecified) {
            res.color = textForegroundStyle.color.toArgb()
        }
        val foreground = toTextPaint()
        if (foreground != null) {
            res.foreground = foreground
        }
        fontStyle?.let {
            res.fontStyle = it.toSkFontStyle()
        }
        textDecoration?.let {
            res.decorationStyle =
                it.toSkDecorationStyle(textForegroundStyle.color, textDecorationLineStyle)
        }
        if (background != Color.Unspecified) {
            res.background = Paint().also {
                it.color = background.toArgb()
            }
        }
        fontWeight?.let {
            res.fontStyle = res.fontStyle.withWeight(it.weight)
        }
        shadow?.let {
            res.addShadow(it.toSkShadow())
        }

        letterSpacing?.let {
            res.letterSpacing = it
        }

        res.addFontFeatures(FontFeature.parseW3(fontFeatureSettings.orEmpty()))

        res.fontSize = fontSize
        fontFamily?.let {
            val resolved = fontFamilyResolver.resolve(
                it,
                fontWeight ?: FontWeight.Normal,
                fontStyle ?: FontStyle.Normal,
                fontSynthesis ?: FontSynthesis.None
            ).value as FontLoadResult
            res.fontFamilies = resolved.aliases.toTypedArray()
        }

        baselineShift?.let {
            val fontMetrics = res.fontMetrics
            res.baselineShift = it.multiplier * fontMetrics.ascent
        }
        lineHeight?.let {
            res.height = it / fontSize
        }

        return res
    }

    fun merge(density: Density, other: SpanStyle) {
        val fontSize = other.fontSize.toPx(density, fontSize)
        textForegroundStyle = textForegroundStyle.merge(other.textForegroundStyle)
        other.fontFamily?.let { fontFamily = it }
        this.fontSize = fontSize
        other.fontWeight?.let { fontWeight = it }
        other.fontStyle?.let { fontStyle = it }
        other.fontSynthesis?.let { fontSynthesis = it }
        other.fontFeatureSettings?.let { fontFeatureSettings = it }
        if (!other.letterSpacing.isUnspecified) {
            letterSpacing = other.letterSpacing.toPx(density, fontSize)
        }
        other.baselineShift?.let { baselineShift = it }
        other.textGeometricTransform?.let { textGeometricTransform = it }
        other.localeList?.let { localeList = it }
        if (other.background.isSpecified) {
            background = other.background
        }
        other.textDecoration?.let { textDecoration = it }
        other.shadow?.let { shadow = it }
        other.drawStyle?.let { drawStyle = it }
        other.platformStyle?.let { platformStyle ->
            platformStyle.textDecorationLineStyle?.let {
                textDecorationLineStyle = it
            }
        }
    }
}

// Building of SkTextStyle is a relatively expensive operation. We enable simple caching by
// mapping SpanStyle to SkTextStyle. To increase the efficiency of this mapping we are making
// most of the computations before converting Compose paragraph styles to Skia paragraph
private val skTextStylesCache = WeakKeysCache<ComputedStyle, SkTextStyle>()

internal class ParagraphBuilder(
    val fontFamilyResolver: FontFamily.Resolver,
    val text: String,
    var textStyle: TextStyle,
    var brushSize: Size = Size.Unspecified,
    var ellipsis: String = "",
    var maxLines: Int = Int.MAX_VALUE,
    val spanStyles: List<Range<SpanStyle>>,
    val placeholders: List<Range<Placeholder>>,
    val density: Density,
    val textDirection: ResolvedTextDirection,
    var drawStyle: DrawStyle? = null,
    var blendMode: BlendMode = DrawScope.DefaultBlendMode
) {
    private lateinit var initialStyle: SpanStyle
    private lateinit var defaultStyle: ComputedStyle
    private lateinit var ops: List<Op>

    /**
     * SkParagraph styles model doesn't match Compose's one.
     * SkParagraph has only a stack-based push/pop styles interface that works great with Span
     * trees.
     * But in Compose we have a list of SpanStyles attached to arbitrary ranges, possibly
     * overlapped, where a position in the list denotes style's priority
     * We map Compose styles to SkParagraph styles by projecting every range start/end to single
     * positions line and maintaining a list of active styles while building a paragraph. This list
     * of active styles is being compiled into single SkParagraph's style for every chunk of text
     */
    fun build(): SkParagraph {
        initialStyle = textStyle.toSpanStyle().copyWithDefaultFontSize(
            drawStyle = drawStyle
        )
        defaultStyle = ComputedStyle(density, initialStyle, brushSize, blendMode, textStyle.lineHeight)
        ops = makeOps(
            spanStyles,
            placeholders
        )

        var pos = 0
        val ps = textStyleToParagraphStyle(textStyle, defaultStyle)
        if (maxLines != Int.MAX_VALUE) {
            ps.maxLinesCount = maxLines
            ps.ellipsis = ellipsis
        }

        // this downcast is always safe because of sealed types, and we control construction
        val platformFontLoader = (fontFamilyResolver as FontFamilyResolverImpl).platformFontLoader
        val fontCollection = when (platformFontLoader) {
            is SkiaFontLoader -> platformFontLoader.fontCollection
            else -> throw IllegalStateException("Unsupported font loader $platformFontLoader")
        }

        val pb = SkParagraphBuilder(ps, fontCollection)

        var addText = true

        for (op in ops) {
            if (addText && pos < op.position) {
                pb.addText(text.subSequence(pos, op.position).toString())
            }

            when (op) {
                is Op.StyleAdd -> {
                    // FontLoader may have changed, so ensure that Font resolution is still valid
                    fontFamilyResolver.resolve(
                        op.style.fontFamily,
                        op.style.fontWeight ?: FontWeight.Normal,
                        op.style.fontStyle ?: FontStyle.Normal,
                        op.style.fontSynthesis ?: FontSynthesis.All
                    )
                    pb.pushStyle(makeSkTextStyle(op.style))
                }
                is Op.PutPlaceholder -> {
                    val placeholderStyle =
                        PlaceholderStyle(
                            op.width,
                            op.height,
                            op.cut.placeholder.placeholderVerticalAlign
                                .toSkPlaceholderAlignment(),
                            // TODO: figure out how exactly we have to work with BaselineMode & offset
                            BaselineMode.ALPHABETIC,
                            0f
                        )
                    pb.addPlaceholder(placeholderStyle)
                    addText = false
                }
                is Op.EndPlaceholder -> {
                    addText = true
                }
            }

            pos = op.position
        }

        if (addText && pos < text.length) {
            pb.addText(text.subSequence(pos, text.length).toString())
        }

        return pb.build()
    }

    private sealed class Op {
        abstract val position: Int

        data class StyleAdd(
            override val position: Int,
            val style: ComputedStyle
        ) : Op()

        data class PutPlaceholder(
            val cut: Cut.PutPlaceholder,
            var width: Float,
            var height: Float
        ) : Op() {
            override val position: Int by cut::position
        }

        data class EndPlaceholder(
            val cut: Cut.EndPlaceholder
        ) : Op() {
            override val position: Int by cut::position
        }
    }

    private sealed class Cut {
        abstract val position: Int

        data class StyleAdd(
            override val position: Int,
            val style: SpanStyle
        ) : Cut()

        data class StyleRemove(
            override val position: Int,
            val style: SpanStyle
        ) : Cut()

        data class PutPlaceholder(
            override val position: Int,
            val placeholder: Placeholder,
        ) : Cut()

        data class EndPlaceholder(override val position: Int) : Cut()
    }

    private fun makeOps(
        spans: List<Range<SpanStyle>>,
        placeholders: List<Range<Placeholder>>
    ): List<Op> {
        val cuts = mutableListOf<Cut>()
        for (span in spans) {
            cuts.add(Cut.StyleAdd(span.start, span.item))
            cuts.add(Cut.StyleRemove(span.end, span.item))
        }

        for (placeholder in placeholders) {
            cuts.add(Cut.PutPlaceholder(placeholder.start, placeholder.item))
            cuts.add(Cut.EndPlaceholder(placeholder.end))
        }

        val ops = mutableListOf<Op>(Op.StyleAdd(0, defaultStyle))
        cuts.sortBy { it.position }
        val activeStyles = mutableListOf(initialStyle)
        for (cut in cuts) {
            when (cut) {
                is Cut.StyleAdd -> {
                    activeStyles.add(cut.style)
                    val prev = previousStyleAddAtTheSamePosition(cut.position, ops)
                    if (prev == null) {
                        ops.add(
                            Op.StyleAdd(
                                cut.position,
                                mergeStyles(activeStyles).also { it.merge(density, cut.style) }
                            )
                        )
                    } else {
                        prev.style.merge(density, cut.style)
                    }
                }
                is Cut.StyleRemove -> {
                    activeStyles.remove(cut.style)
                    ops.add(Op.StyleAdd(cut.position, mergeStyles(activeStyles)))
                }
                is Cut.PutPlaceholder -> {
                    val currentStyle = mergeStyles(activeStyles)
                    val op = Op.PutPlaceholder(
                        cut = cut,
                        width = cut.placeholder.width.toPx(
                            density,
                            currentStyle.fontSize
                        ),
                        height = cut.placeholder.height.toPx(
                            density,
                            currentStyle.fontSize
                        ),
                    )
                    ops.add(op)
                }
                is Cut.EndPlaceholder -> ops.add(Op.EndPlaceholder(cut))
            }
        }
        return ops
    }

    private fun mergeStyles(activeStyles: List<SpanStyle>): ComputedStyle {
        // there is always at least one active style
        val style = ComputedStyle(density, activeStyles[0], brushSize, blendMode, textStyle.lineHeight)
        for (i in 1 until activeStyles.size) {
            style.merge(density, activeStyles[i])
        }
        return style
    }

    private fun previousStyleAddAtTheSamePosition(position: Int, ops: List<Op>): Op.StyleAdd? {
        for (prevOp in ops.asReversed()) {
            if (prevOp.position < position) return null
            if (prevOp is Op.StyleAdd) return prevOp
        }
        return null
    }

    private fun textStyleToParagraphStyle(
        style: TextStyle,
        computedStyle: ComputedStyle
    ): ParagraphStyle {
        val pStyle = ParagraphStyle()
        pStyle.textStyle = makeSkTextStyle(computedStyle)
        style.textAlign.let {
            pStyle.alignment = it.toSkAlignment()
        }

        val lineHeight = computedStyle.lineHeight
        if (lineHeight != null && lineHeight > computedStyle.fontSize) {
            val lineHeightStyle = style.lineHeightStyle ?: LineHeightStyle.Default
            pStyle.heightMode = lineHeightStyle.trim.toHeightMode()
        } else {
            /*
             * "DISABLE_ALL" replaces calculated from lineHeight
             * ascent for the first line and descent for the last line
             * to default font's values.
             *
             * To match android behavior, set it without taking into account trim value
             * in case when lineHeight < fontSize. This keeps the single line height NOT less
             * than defined in font. Note that it just ensures of minimal external paddings,
             * internal (between lines in multiline text) calculated as-is.
             */
            pStyle.heightMode = HeightMode.DISABLE_ALL
        }

        // TODO: Support lineHeightStyle.alignment. Currently it's not exposed in skia

        pStyle.direction = textDirection.toSkDirection()
        textStyle.textIndent?.run {
            with(density) {
                pStyle.textIndent = SkTextIndent(firstLine.toPx(), restLine.toPx())
            }
        }
        return pStyle
    }

    private fun makeSkTextStyle(style: ComputedStyle): SkTextStyle {
        return skTextStylesCache.get(style) {
            it.toSkTextStyle(fontFamilyResolver)
        }
    }

    internal val defaultFont by lazy {
        val loadResult = textStyle.resolveFontFamily(fontFamilyResolver)
        SkFont(loadResult?.typeface, defaultStyle.fontSize)
    }

    // workaround for https://bugs.chromium.org/p/skia/issues/detail?id=11321 :(
    internal fun emptyLineMetrics(paragraph: SkParagraph): Array<LineMetrics> {
        val metrics = defaultFont.metrics
        val heightMultiplier = defaultStyle.lineHeight?.let {
            it / defaultStyle.fontSize.toDouble()
        } ?: 1.0
        val ascent = metrics.ascent * heightMultiplier // TODO: Support non-proportional alignment
        val descent = metrics.descent * heightMultiplier // TODO: Support non-proportional alignment
        val baseline = paragraph.alphabeticBaseline.toDouble()
        val height = descent - ascent
        return arrayOf(
            LineMetrics(
                startIndex = 0,
                endIndex = 0,
                endExcludingWhitespaces = 0,
                endIncludingNewline = 0,
                isHardBreak = true,
                ascent = -ascent,
                descent = descent,
                unscaledAscent = ascent,
                height = height,
                width = 0.0,
                left = 0.0,
                baseline = baseline,
                lineNumber = 0
            )
        )
    }
}

private fun TextUnit.orDefaultFontSize() = when {
    isUnspecified -> DefaultFontSize
    isEm -> DefaultFontSize * value
    else -> this
}

private fun TextUnit.toPx(density: Density, fontSize: TextUnit): Float =
    toPx(density, with(density) { fontSize.toPx() })

private fun TextUnit.toPx(density: Density, fontSize: Float): Float = when {
    isUnspecified -> fontSize
    isEm -> fontSize * value
    isSp -> with(density) { toPx() }
    else -> error("Unexpected size in TextUnit.toPx")
}

private fun LineHeightStyle.Trim.toHeightMode(): HeightMode = when(this) {
    LineHeightStyle.Trim.Both -> HeightMode.DISABLE_ALL
    LineHeightStyle.Trim.FirstLineTop -> HeightMode.DISABLE_FIRST_ASCENT
    LineHeightStyle.Trim.LastLineBottom -> HeightMode.DISABLE_LAST_DESCENT
    LineHeightStyle.Trim.None -> HeightMode.ALL
    else -> HeightMode.DISABLE_ALL
}

private fun TextStyle.resolveFontFamily(
    fontFamilyResolver: FontFamily.Resolver
) = fontFamily?.let {
    fontFamilyResolver.resolve(
        fontFamily = it,
        fontWeight = fontWeight ?: FontWeight.Normal,
        fontStyle = fontStyle ?: FontStyle.Normal,
        fontSynthesis = fontSynthesis ?: FontSynthesis.All
    ).value as FontLoadResult
}

private fun SpanStyle.copyWithDefaultFontSize(drawStyle: DrawStyle? = null): SpanStyle {
    val fontSize = this.fontSize.orDefaultFontSize()
    val letterSpacing = when {
        this.letterSpacing.isEm -> fontSize * this.letterSpacing.value
        else -> this.letterSpacing
    }
    return this.copy(
        fontSize = fontSize,
        letterSpacing = letterSpacing,
        drawStyle = drawStyle
    )
}

// TODO: Remove from public
fun FontStyle.toSkFontStyle(): SkFontStyle {
    return when (this) {
        FontStyle.Italic -> org.jetbrains.skia.FontStyle.ITALIC
        else -> org.jetbrains.skia.FontStyle.NORMAL
    }
}

@Suppress("unused")
@Deprecated(
    message = "This method was not intended to be public",
    level = DeprecationLevel.HIDDEN
)
fun TextDecoration.toSkDecorationStyle(color: Color): SkDecorationStyle {
    return toSkDecorationStyle(color, null)
}

private fun TextDecoration.toSkDecorationStyle(
    color: Color,
    textDecorationLineStyle: TextDecorationLineStyle?
): SkDecorationStyle {
    val underline = contains(TextDecoration.Underline)
    val overline = false
    val lineThrough = contains(TextDecoration.LineThrough)
    val gaps = false
    val lineStyle =
        textDecorationLineStyle?.toSkDecorationLineStyle() ?: SkDecorationLineStyle.SOLID
    val thicknessMultiplier = 1f
    return SkDecorationStyle(
        underline,
        overline,
        lineThrough,
        gaps,
        color.toArgb(),
        lineStyle,
        thicknessMultiplier
    )
}

private fun TextDecorationLineStyle.toSkDecorationLineStyle(): SkDecorationLineStyle {
    return when (this) {
        TextDecorationLineStyle.Solid -> SkDecorationLineStyle.SOLID
        TextDecorationLineStyle.Double -> SkDecorationLineStyle.DOUBLE
        TextDecorationLineStyle.Dotted -> SkDecorationLineStyle.DOTTED
        TextDecorationLineStyle.Dashed -> SkDecorationLineStyle.DASHED
        TextDecorationLineStyle.Wavy -> SkDecorationLineStyle.WAVY
        else -> SkDecorationLineStyle.SOLID
    }
}

// TODO: Remove from public
fun PlaceholderVerticalAlign.toSkPlaceholderAlignment(): PlaceholderAlignment {
    return when (this) {
        PlaceholderVerticalAlign.AboveBaseline -> PlaceholderAlignment.ABOVE_BASELINE
        PlaceholderVerticalAlign.TextTop -> PlaceholderAlignment.TOP
        PlaceholderVerticalAlign.TextBottom -> PlaceholderAlignment.BOTTOM
        PlaceholderVerticalAlign.TextCenter -> PlaceholderAlignment.MIDDLE

        // TODO: figure out how we have to handle it properly
        PlaceholderVerticalAlign.Top -> PlaceholderAlignment.TOP
        PlaceholderVerticalAlign.Bottom -> PlaceholderAlignment.BOTTOM
        PlaceholderVerticalAlign.Center -> PlaceholderAlignment.MIDDLE
        else -> error("Invalid PlaceholderVerticalAlign.")
    }
}

internal fun Shadow.toSkShadow(): SkShadow {
    return SkShadow(color.toArgb(), offset.x, offset.y, blurRadius.toDouble())
}

internal fun TextAlign.toSkAlignment(): SkAlignment {
    return when (this) {
        TextAlign.Left -> SkAlignment.LEFT
        TextAlign.Right -> SkAlignment.RIGHT
        TextAlign.Center -> SkAlignment.CENTER
        TextAlign.Justify -> SkAlignment.JUSTIFY
        TextAlign.Start -> SkAlignment.START
        TextAlign.End -> SkAlignment.END
        else -> SkAlignment.START
    }
}

internal fun ResolvedTextDirection.toSkDirection(): SkDirection {
    return when (this) {
        ResolvedTextDirection.Ltr -> SkDirection.LTR
        ResolvedTextDirection.Rtl -> SkDirection.RTL
    }
}

internal fun TextBox.cursorHorizontalPosition(opposite: Boolean = false): Float {
    return when (direction) {
        SkDirection.LTR -> if (opposite) rect.left else rect.right
        SkDirection.RTL -> if (opposite) rect.right else rect.left
    }
}
