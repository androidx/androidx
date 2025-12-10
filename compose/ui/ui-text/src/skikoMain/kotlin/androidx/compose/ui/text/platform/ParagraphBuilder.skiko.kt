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

@file:OptIn(ExperimentalTextApi::class, InternalTextApi::class)
@file:JvmName("SkiaParagraph_skikoKt")
@file:JvmMultifileClass

package androidx.compose.ui.text.platform

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.FontRasterizationSettings
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextDecorationLineStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.WeakKeysCache
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontFamilyResolverImpl
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.SkiaFontLoader
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextForegroundStyle
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.toSkFontRastrSettings
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachReversed
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import org.jetbrains.skia.Font as SkFont
import org.jetbrains.skia.FontFeature
import org.jetbrains.skia.FontStyle as SkFontStyle
import org.jetbrains.skia.Paint as SkPaint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.paragraph.Alignment as SkAlignment
import org.jetbrains.skia.paragraph.BaselineMode
import org.jetbrains.skia.paragraph.DecorationLineStyle as SkDecorationLineStyle
import org.jetbrains.skia.paragraph.DecorationStyle as SkDecorationStyle
import org.jetbrains.skia.paragraph.Direction as SkDirection
import org.jetbrains.skia.paragraph.FontRastrSettings as SkFontRastrSettings
import org.jetbrains.skia.paragraph.HeightMode
import org.jetbrains.skia.paragraph.LineMetrics
import org.jetbrains.skia.paragraph.Paragraph as SkParagraph
import org.jetbrains.skia.paragraph.ParagraphBuilder as SkParagraphBuilder
import org.jetbrains.skia.paragraph.ParagraphStyle
import org.jetbrains.skia.paragraph.PlaceholderAlignment
import org.jetbrains.skia.paragraph.PlaceholderStyle
import org.jetbrains.skia.paragraph.Shadow as SkShadow
import org.jetbrains.skia.paragraph.TextBox
import org.jetbrains.skia.paragraph.TextIndent as SkTextIndent
import org.jetbrains.skia.paragraph.TextStyle as SkTextStyle

private val DefaultFontSize = 16.sp

// Computed ComputedStyles always have font/letter size in pixels for particular `density`.
// It's important because density could be changed in runtime, and it should force
// SkTextStyle to be recalculated. Or we can have different densities in different windows.
private sealed interface ComputedStyle {
    val textForegroundStyle: TextForegroundStyle
    val brushSize: Size
    val fontSize: Float
    val fontWeight: FontWeight?
    val fontStyle: FontStyle?
    val fontSynthesis: FontSynthesis?
    val fontFamily: FontFamily?
    val fontFeatureSettings: String?
    val letterSpacing: Float?
    val baselineShift: BaselineShift?
    val textGeometricTransform: TextGeometricTransform?
    val localeList: LocaleList?
    val background: Color
    val textDecoration: TextDecoration?
    val textDecorationLineStyle: TextDecorationLineStyle?
    val shadow: Shadow?
    val drawStyle: DrawStyle?
    val blendMode: BlendMode
    val lineHeight: Float?
    val topRatio: Float

    // Compile-time guarantee to be used as a key in the cache
    data class Immutable(
        override val textForegroundStyle: TextForegroundStyle = TextForegroundStyle.Unspecified,
        override val brushSize: Size = Size.Unspecified,
        override val fontSize: Float = Float.NaN,
        override val fontWeight: FontWeight? = null,
        override val fontStyle: FontStyle? = null,
        override val fontSynthesis: FontSynthesis? = null,
        override val fontFamily: FontFamily? = null,
        override val fontFeatureSettings: String? = null,
        override val letterSpacing: Float? = null,
        override val baselineShift: BaselineShift? = null,
        override val textGeometricTransform: TextGeometricTransform? = null,
        override val localeList: LocaleList? = null,
        override val background: Color = Color.Unspecified,
        override val textDecoration: TextDecoration? = null,
        override val textDecorationLineStyle: TextDecorationLineStyle? = null,
        override val shadow: Shadow? = null,
        override val drawStyle: DrawStyle? = null,
        override val blendMode: BlendMode = DrawScope.DefaultBlendMode,
        override val lineHeight: Float? = null,
        override val topRatio: Float = -1f,
    ) : ComputedStyle {
        private val _foregroundPaint = SkiaTextPaint()
        fun getForegroundPaint(): SkPaint {
            // `asFrameworkPaint` doesn't create a copy,
            // so all the changes will be applied to skia paint.
            val paint = _foregroundPaint.asFrameworkPaint()
            paint.reset()
            _foregroundPaint.color = textForegroundStyle.color
            _foregroundPaint.setBrush(textForegroundStyle.brush, brushSize, textForegroundStyle.alpha)
            _foregroundPaint.setDrawStyle(drawStyle)
            _foregroundPaint.blendMode = blendMode
            return paint
        }

        fun toSkTextStyle(fontFamilyResolver: FontFamily.Resolver): SkTextStyle {
            val res = SkTextStyle()
            if (textForegroundStyle.color.isSpecified) {
                res.color = textForegroundStyle.color.toArgb()
            }
            val foreground = getForegroundPaint()
            if (foreground.shader != null ||
                foreground.mode != PaintMode.FILL ||
                !foreground.isSrcOver) {
                res.foreground = foreground
            }
            fontStyle?.let {
                res.fontStyle = it.toSkFontStyle()
            }
            textDecoration.takeUnless { it == TextDecoration.None }?.let {
                res.decorationStyle =
                    it.toSkDecorationStyle(textForegroundStyle.color, textDecorationLineStyle)
            }
            if (background != Color.Unspecified) {
                res.background = SkPaint().also {
                    it.color = background.toArgb()
                }
            }
            fontWeight?.let {
                res.fontStyle = res.fontStyle.withWeight(it.weight)
            }
            shadow.takeUnless { it == Shadow.None }?.let {
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
                res.typeface = resolved.typeface
            }

            baselineShift?.let {
                val fontMetrics = res.fontMetrics
                res.baselineShift = it.multiplier * fontMetrics.ascent
            }
            lineHeight?.let {
                res.height = it / fontSize
            }
            res.topRatio = topRatio

            return res
        }

        fun toMutable() = Mutable(
            textForegroundStyle = textForegroundStyle,
            brushSize = brushSize,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontSynthesis = fontSynthesis,
            fontFamily = fontFamily,
            fontFeatureSettings = fontFeatureSettings,
            letterSpacing = letterSpacing,
            baselineShift = baselineShift,
            textGeometricTransform = textGeometricTransform,
            localeList = localeList,
            background = background,
            textDecoration = textDecoration,
            textDecorationLineStyle = textDecorationLineStyle,
            shadow = shadow,
            drawStyle = drawStyle,
            blendMode = blendMode,
            lineHeight = lineHeight,
            topRatio = topRatio,
        )
    }

    // Keep mutable variant to merge in place, without additional allocations
    class Mutable(
        override var textForegroundStyle: TextForegroundStyle,
        override var brushSize: Size,
        override var fontSize: Float,
        override var fontWeight: FontWeight?,
        override var fontStyle: FontStyle?,
        override var fontSynthesis: FontSynthesis?,
        override var fontFamily: FontFamily?,
        override var fontFeatureSettings: String?,
        override var letterSpacing: Float?,
        override var baselineShift: BaselineShift?,
        override var textGeometricTransform: TextGeometricTransform?,
        override var localeList: LocaleList?,
        override var background: Color,
        override var textDecoration: TextDecoration?,
        override var textDecorationLineStyle: TextDecorationLineStyle?,
        override var shadow: Shadow?,
        override var drawStyle: DrawStyle?,
        override var blendMode: BlendMode,
        override var lineHeight: Float?,
        override var topRatio: Float,
    ) : ComputedStyle {
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

        fun toImmutable() = Immutable(
            textForegroundStyle = textForegroundStyle,
            brushSize = brushSize,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontSynthesis = fontSynthesis,
            fontFamily = fontFamily,
            fontFeatureSettings = fontFeatureSettings,
            letterSpacing = letterSpacing,
            baselineShift = baselineShift,
            textGeometricTransform = textGeometricTransform,
            localeList = localeList,
            background = background,
            textDecoration = textDecoration,
            textDecorationLineStyle = textDecorationLineStyle,
            shadow = shadow,
            drawStyle = drawStyle,
            blendMode = blendMode,
            lineHeight = lineHeight,
            topRatio = topRatio,
        )
    }
}

private fun ComputedStyle(
    density: Density,
    spanStyle: SpanStyle,
    brushSize: Size = Size.Unspecified,
    blendMode: BlendMode = DrawScope.DefaultBlendMode,
    lineHeight: TextUnit,
    lineHeightStyle: LineHeightStyle?,
) = ComputedStyle.Mutable(
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
    topRatio = (lineHeightStyle?.alignment ?: LineHeightStyle.Alignment.Proportional).topRatio,
)

// Building of SkTextStyle is a relatively expensive operation. We enable simple caching by
// mapping SpanStyle to SkTextStyle. To increase the efficiency of this mapping we are making
// most of the computations before converting Compose paragraph styles to Skia paragraph
private val skTextStylesCache = WeakKeysCache<ComputedStyle.Immutable, SkTextStyle>()

internal class ParagraphBuilder(
    val fontFamilyResolver: FontFamily.Resolver,
    val text: String,
    var textStyle: TextStyle,
    var brushSize: Size = Size.Unspecified,
    var ellipsis: String = "",
    var maxLines: Int = Int.MAX_VALUE,
    var annotations: List<AnnotatedString.Range<out AnnotatedString.Annotation>>,
    val placeholders: List<AnnotatedString.Range<Placeholder>>,
    val density: Density,
    val textDirection: ResolvedTextDirection,
    var drawStyle: DrawStyle? = null,
    var blendMode: BlendMode = DrawScope.DefaultBlendMode
) {
    private var defaultStyle = ComputedStyle.Immutable()
    private lateinit var initialStyle: SpanStyle
    private lateinit var ops: List<Op>

    private fun prepareDefaultStyle() {
        initialStyle = textStyle.toSpanStyle().copyWithDefaultFontSize(
            drawStyle = drawStyle
        )
        defaultStyle = ComputedStyle(
            density = density,
            spanStyle = initialStyle,
            brushSize = brushSize,
            blendMode = blendMode,
            lineHeight = textStyle.lineHeight,
            lineHeightStyle = textStyle.lineHeightStyle,
        ).toImmutable()
    }

    fun updateForegroundPaint(paragraph: SkParagraph?) {
        if (paragraph == null) return
        prepareDefaultStyle()
        val foregroundPaint = defaultStyle.getForegroundPaint()
        paragraph.updateForegroundPaint(0, text.length, foregroundPaint)
    }

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
        prepareDefaultStyle()
        ops = makeOps(
            annotations,
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

        ops.fastForEach { op ->
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

                    // It's always mutable at this point, so we can safely cast
                    val style = (op.style as ComputedStyle.Mutable).toImmutable()
                    // Store immutable reference because it's used as a weak reference key
                    op.style = style

                    pb.pushStyle(makeSkTextStyle(style))
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
            var style: ComputedStyle
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
        annotations: List<AnnotatedString.Range<out AnnotatedString.Annotation>>,
        placeholders: List<AnnotatedString.Range<Placeholder>>
    ): List<Op> {
        val cuts = mutableListOf<Cut>()
        annotations.fastForEach { annotation ->
            // TODO https://youtrack.jetbrains.com/issue/CMP-7151
            if (annotation.item !is SpanStyle) return@fastForEach

            cuts.add(Cut.StyleAdd(annotation.start, annotation.item))
            cuts.add(Cut.StyleRemove(annotation.end, annotation.item))
        }

        placeholders.fastForEach { placeholder ->
            cuts.add(Cut.PutPlaceholder(placeholder.start, placeholder.item))
            cuts.add(Cut.EndPlaceholder(placeholder.end))
        }

        val ops = mutableListOf<Op>(Op.StyleAdd(0, defaultStyle.toMutable()))
        cuts.sortBy { it.position }
        val activeStyles = mutableListOf(initialStyle)
        cuts.fastForEach { cut ->
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
                        // It's always mutable at this point, so we can safely cast
                        val style = prev.style as ComputedStyle.Mutable
                        style.merge(density, cut.style)
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

    private fun mergeStyles(activeStyles: List<SpanStyle>): ComputedStyle.Mutable {
        check(activeStyles.isNotEmpty()) { "There should be at least one active style" }
        val style = ComputedStyle(
            density = density,
            spanStyle = activeStyles[0],
            brushSize = brushSize,
            blendMode = blendMode,
            lineHeight = textStyle.lineHeight,
            lineHeightStyle = textStyle.lineHeightStyle
        )
        for (i in 1 until activeStyles.size) {
            style.merge(density, activeStyles[i])
        }
        return style
    }

    private fun previousStyleAddAtTheSamePosition(position: Int, ops: List<Op>): Op.StyleAdd? {
        ops.fastForEachReversed { prevOp ->
            if (prevOp.position < position) return null
            if (prevOp is Op.StyleAdd) return prevOp
        }
        return null
    }

    private fun makeSkFontRasterizationSettings(style: TextStyle): SkFontRastrSettings {
        val rasterizationSettings = style.paragraphStyle.platformStyle?.fontRasterizationSettings
            ?: FontRasterizationSettings.PlatformDefault
        return rasterizationSettings.toSkFontRastrSettings()
    }

    private fun textStyleToParagraphStyle(
        style: TextStyle,
        computedStyle: ComputedStyle.Immutable
    ): ParagraphStyle {
        val pStyle = ParagraphStyle()
        pStyle.replaceTabCharacters = true // https://youtrack.jetbrains.com/issue/CMP-6589
        pStyle.fontRastrSettings = makeSkFontRasterizationSettings(style)
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

        pStyle.direction = textDirection.toSkDirection()
        textStyle.textIndent?.run {
            with(density) {
                pStyle.textIndent = SkTextIndent(firstLine.toPx(), restLine.toPx())
            }
        }
        return pStyle
    }

    private fun makeSkTextStyle(style: ComputedStyle.Immutable): SkTextStyle {
        return skTextStylesCache.getOrPut(style) {
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
        var ascent = metrics.ascent.toDouble()
        var descent = metrics.descent.toDouble()
        val baseline = paragraph.alphabeticBaseline.toDouble()
        val lineHeight = defaultStyle.lineHeight
        if (lineHeight != null) {
            val topRatio = defaultStyle.topRatio
            if (topRatio in 0.0f..1.0f) {
                val extraLeading = lineHeight - defaultStyle.fontSize
                ascent -= extraLeading * topRatio
                descent += extraLeading * (1.0f - topRatio)
            } else {
                val multiplier = lineHeight / defaultStyle.fontSize
                ascent *= multiplier
                descent *= multiplier
            }
        }
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
@InternalTextApi
fun FontStyle.toSkFontStyle(): SkFontStyle {
    return when (this) {
        FontStyle.Italic -> SkFontStyle.ITALIC
        else -> SkFontStyle.NORMAL
    }
}

// TODO: Remove from public
@Suppress("unused")
@Deprecated(
    message = "This method was not intended to be public",
    level = DeprecationLevel.HIDDEN
)
@InternalTextApi
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
@InternalTextApi
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
