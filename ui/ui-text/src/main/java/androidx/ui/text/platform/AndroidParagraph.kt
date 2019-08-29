/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.ui.text.platform

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.TextUtils
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.LeadingMarginSpan
import android.text.style.LocaleSpan
import android.text.style.RelativeSizeSpan
import android.text.style.ScaleXSpan
import android.text.style.StrikethroughSpan
import android.text.style.UnderlineSpan
import androidx.annotation.VisibleForTesting
import androidx.text.LayoutCompat.ALIGN_CENTER
import androidx.text.LayoutCompat.ALIGN_LEFT
import androidx.text.LayoutCompat.ALIGN_NORMAL
import androidx.text.LayoutCompat.ALIGN_OPPOSITE
import androidx.text.LayoutCompat.ALIGN_RIGHT
import androidx.text.LayoutCompat.DEFAULT_ALIGNMENT
import androidx.text.LayoutCompat.DEFAULT_JUSTIFICATION_MODE
import androidx.text.LayoutCompat.DEFAULT_LINESPACING_MULTIPLIER
import androidx.text.LayoutCompat.DEFAULT_MAX_LINES
import androidx.text.LayoutCompat.JUSTIFICATION_MODE_INTER_WORD
import androidx.text.LayoutCompat.TEXT_DIRECTION_FIRST_STRONG_LTR
import androidx.text.LayoutCompat.TEXT_DIRECTION_FIRST_STRONG_RTL
import androidx.text.LayoutCompat.TEXT_DIRECTION_LTR
import androidx.text.LayoutCompat.TEXT_DIRECTION_RTL
import androidx.text.TextLayout
import androidx.text.selection.WordBoundary
import androidx.text.style.BaselineShiftSpan
import androidx.text.style.FontFeatureSpan
import androidx.text.style.LetterSpacingSpan
import androidx.text.style.ShadowSpan
import androidx.text.style.SkewXSpan
import androidx.text.style.TypefaceSpan
import androidx.ui.core.Density
import androidx.ui.core.LayoutDirection
import androidx.ui.core.PxPosition
import androidx.ui.core.px
import androidx.ui.core.withDensity
import androidx.ui.engine.geometry.Rect
import androidx.ui.graphics.toArgb
import androidx.ui.painting.Canvas
import androidx.ui.painting.Path
import androidx.ui.text.AnnotatedString
import androidx.ui.text.Paragraph
import androidx.ui.text.ParagraphConstraints
import androidx.ui.text.ParagraphStyle
import androidx.ui.text.TextRange
import androidx.ui.text.TextStyle
import androidx.ui.text.font.FontStyle
import androidx.ui.text.font.FontSynthesis
import androidx.ui.text.font.FontWeight
import androidx.ui.text.style.TextAlign
import androidx.ui.text.style.TextDecoration
import androidx.ui.text.style.TextDirection
import androidx.ui.text.style.TextDirectionAlgorithm
import androidx.ui.text.style.TextIndent
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Android specific implementation for [Paragraph]
 */
internal class AndroidParagraph constructor(
    val text: String,
    val style: TextStyle,
    val paragraphStyle: ParagraphStyle,
    val textStyles: List<AnnotatedString.Item<TextStyle>>,
    val maxLines: Int?,
    val ellipsis: Boolean?,
    val typefaceAdapter: TypefaceAdapter,
    val density: Density,
    val layoutDirection: LayoutDirection
) : Paragraph {

    @VisibleForTesting
    internal val textPaint = TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG)

    private var layout: TextLayout? = null

    override var width: Float = 0.0f
        get() = layout?.let { field } ?: 0.0f

    override val height: Float
        get() = layout?.let {
            // TODO(Migration/haoyuchang): Figure out a way to add bottomPadding properly
            val lineCount = it.lineCount
            if (maxLines != null &&
                maxLines >= 0 &&
                maxLines < lineCount
            ) {
                it.layout.getLineBottom(maxLines - 1).toFloat()
            } else {
                it.layout.height.toFloat()
            }
        } ?: 0.0f

    override val maxIntrinsicWidth: Float
        get() = layout?.maxIntrinsicWidth ?: 0.0f

    override val minIntrinsicWidth: Float by lazy {
        androidx.text.minIntrinsicWidth(charSequence, textPaint)
    }

    override val firstBaseline: Float
        get() = layout?.getLineBaseline(0) ?: 0.0f

    override val lastBaseline: Float
        get() = if (maxLines != null && maxLines >= 0 && maxLines < lineCount) {
            layout?.getLineBaseline(maxLines - 1) ?: 0.0f
        } else {
            layout?.getLineBaseline(lineCount - 1) ?: 0.0f
        }


    override val didExceedMaxLines: Boolean
        get() = layout?.didExceedMaxLines ?: false

    @VisibleForTesting
    internal val textLocale: Locale
        get() = textPaint.textLocale

    override val lineCount: Int
        get() = ensureLayout.lineCount

    private val ensureLayout: TextLayout
        get() {
            return this.layout ?: throw java.lang.IllegalStateException(
                "layout() should be called first"
            )
        }

    @VisibleForTesting
    internal val underlyingText: CharSequence
        get() = ensureLayout.text

    private val charSequence: CharSequence by lazy {
        val newStyle = textPaint.applyTextStyle(style, typefaceAdapter, density)

        createStyledText(
            text = text,
            textIndent = paragraphStyle.textIndent,
            textStyles = listOf(
                AnnotatedString.Item(
                    newStyle,
                    0,
                    text.length
                )
            ) + textStyles,
            density = density,
            typefaceAdapter = typefaceAdapter
        )
    }

    override fun layout(constraints: ParagraphConstraints) {
        val width = constraints.width

        val alignment = toLayoutAlign(paragraphStyle.textAlign)

        val textDirectionHeuristic = resolveTextDirectionHeuristics(
            layoutDirection,
            paragraphStyle.textDirectionAlgorithm
        )

        val maxLines = maxLines ?: DEFAULT_MAX_LINES
        val justificationMode = when (paragraphStyle.textAlign) {
            TextAlign.Justify -> JUSTIFICATION_MODE_INTER_WORD
            else -> DEFAULT_JUSTIFICATION_MODE
        }

        val lineSpacingMultiplier = paragraphStyle.lineHeight ?: DEFAULT_LINESPACING_MULTIPLIER

        val ellipsize = if (ellipsis == true) {
            TextUtils.TruncateAt.END
        } else {
            null
        }

        layout = TextLayout(
            charSequence = charSequence,
            width = width,
            textPaint = textPaint,
            ellipsize = ellipsize,
            alignment = alignment,
            textDirectionHeuristic = textDirectionHeuristic,
            lineSpacingMultiplier = lineSpacingMultiplier,
            maxLines = maxLines,
            justificationMode = justificationMode
        )
        this.width = width
    }

    override fun getOffsetForPosition(position: PxPosition): Int {
        val line = ensureLayout.getLineForVertical(position.y.value.toInt())
        return ensureLayout.getOffsetForHorizontal(line, position.x.value)
    }

    /**
     * Returns the bounding box as Rect of the character for given character offset. Rect includes
     * the top, bottom, left and right of a character.
     */
    // TODO:(qqd) Implement RTL case.
    override fun getBoundingBox(offset: Int): Rect {
        val left = ensureLayout.getPrimaryHorizontal(offset)
        val right = ensureLayout.getPrimaryHorizontal(offset + 1)

        val line = ensureLayout.getLineForOffset(offset)
        val top = ensureLayout.getLineTop(line)
        val bottom = ensureLayout.getLineBottom(line)

        return Rect(top = top, bottom = bottom, left = left, right = right)
    }

    override fun getPathForRange(start: Int, end: Int): Path {
        if (start !in 0..end || end > text.length) {
            throw AssertionError(
                "Start($start) or End($end) is out of Range(0..${text.length}), or start > end!"
            )
        }
        val path = android.graphics.Path()
        ensureLayout.getSelectionPath(start, end, path)
        return Path(path)
    }

    override fun getCursorRect(offset: Int): Rect {
        if (offset !in 0..text.length) {
            throw AssertionError("offset($offset) is out of bounds (0,${text.length}")
        }
        // TODO(nona): Support cursor drawable.
        val cursorWidth = 4.0f
        val layout = ensureLayout
        val horizontal = layout.getPrimaryHorizontal(offset)
        val line = layout.getLineForOffset(offset)

        return Rect(
            horizontal - 0.5f * cursorWidth,
            layout.getLineTop(line),
            horizontal + 0.5f * cursorWidth,
            layout.getLineBottom(line)
        )
    }

    private val wordBoundary: WordBoundary by lazy {
        WordBoundary(textLocale, ensureLayout.text)
    }

    override fun getWordBoundary(offset: Int): TextRange {
        return TextRange(wordBoundary.getWordStart(offset), wordBoundary.getWordEnd(offset))
    }

    override fun getLineLeft(lineIndex: Int): Float = ensureLayout.getLineLeft(lineIndex)

    override fun getLineRight(lineIndex: Int): Float = ensureLayout.getLineRight(lineIndex)

    override fun getLineBottom(lineIndex: Int): Float = ensureLayout.getLineBottom(lineIndex)

    override fun getLineHeight(lineIndex: Int): Float = ensureLayout.getLineHeight(lineIndex)

    override fun getLineWidth(lineIndex: Int): Float = ensureLayout.getLineWidth(lineIndex)

    override fun getLineForOffset(offset: Int): Int = ensureLayout.getLineForOffset(offset)

    override fun getPrimaryHorizontal(offset: Int): Float =
        ensureLayout.getPrimaryHorizontal(offset)

    override fun getSecondaryHorizontal(offset: Int): Float =
        ensureLayout.getSecondaryHorizontal(offset)

    override fun getParagraphDirection(offset: Int): TextDirection {
        val lineIndex = ensureLayout.getLineForOffset(offset)
        val direction = ensureLayout.getParagraphDirection(lineIndex)
        return if (direction == 1) TextDirection.Ltr else TextDirection.Rtl
    }

    override fun getBidiRunDirection(offset: Int): TextDirection {
        return if (ensureLayout.isRtlCharAt(offset)) TextDirection.Rtl else TextDirection.Ltr
    }
    /**
     * @return true if the given line is ellipsized, else false.
     */
    internal fun isEllipsisApplied(lineIndex: Int): Boolean =
        ensureLayout.isEllipsisApplied(lineIndex)

    override fun paint(canvas: Canvas) {
        val nativeCanvas = canvas.nativeCanvas
        if (didExceedMaxLines) {
            nativeCanvas.save()
            nativeCanvas.clipRect(0f, 0f, width, height)
        }
        ensureLayout.paint(nativeCanvas)
        if (didExceedMaxLines) {
            nativeCanvas.restore()
        }
    }
}

private fun createTypeface(style: TextStyle, typefaceAdapter: TypefaceAdapter): Typeface {
    return typefaceAdapter.create(
        fontFamily = style.fontFamily,
        fontWeight = style.fontWeight ?: FontWeight.normal,
        fontStyle = style.fontStyle ?: FontStyle.Normal,
        fontSynthesis = style.fontSynthesis ?: FontSynthesis.All
    )
}

private fun createStyledText(
    text: String,
    textIndent: TextIndent?,
    textStyles: List<AnnotatedString.Item<TextStyle>>,
    density: Density,
    typefaceAdapter: TypefaceAdapter
): CharSequence {
    if (textStyles.isEmpty() && textIndent == null) return text
    val spannableString = SpannableString(text)

    textIndent?.let { indent ->
        if (indent.firstLine == 0.px && indent.restLine == 0.px) return@let
        spannableString.setSpan(
            LeadingMarginSpan.Standard(
                indent.firstLine.value.toInt(),
                indent.restLine.value.toInt()
            ),
            0,
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    for (textStyle in textStyles) {
        val start = textStyle.start
        val end = textStyle.end
        val style = textStyle.style

        if (start < 0 || start >= text.length || end <= start || end > text.length) continue

        // Be aware that SuperscriptSpan needs to be applied before all other spans which
        // affect FontMetrics
        style.baselineShift?.let {
            spannableString.setSpan(
                BaselineShiftSpan(it.multiplier),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        style.color?.let {
            spannableString.setSpan(
                ForegroundColorSpan(it.toArgb()),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        style.decoration?.let {
            if (it.contains(TextDecoration.Underline)) {
                spannableString.setSpan(
                    UnderlineSpan(),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            if (it.contains(TextDecoration.LineThrough)) {
                spannableString.setSpan(
                    StrikethroughSpan(),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        style.fontSize?.let {
            withDensity(density) {
                spannableString.setSpan(
                    AbsoluteSizeSpan(it.toPx().value.roundToInt()),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        // Be aware that fontSizeScale must be applied after fontSize.
        style.fontSizeScale?.let {
            spannableString.setSpan(
                RelativeSizeSpan(it),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        style.fontFeatureSettings?.let {
            spannableString.setSpan(
                FontFeatureSpan(it),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        if (style.hasFontAttributes()) {
            spannableString.setSpan(
                TypefaceSpan(createTypeface(style, typefaceAdapter)),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        // TODO(Migration/haoyuchang): implement textBaseLine
        style.textGeometricTransform?.scaleX?.let {
            spannableString.setSpan(
                ScaleXSpan(it),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        style.textGeometricTransform?.skewX?.let {
            spannableString.setSpan(
                SkewXSpan(it),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        // TODO(Migration/haoyuchang): support letter spacing with pixel.
        style.letterSpacing?.let {
            spannableString.setSpan(
                LetterSpacingSpan(it),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        // TODO(Migration/haoyuchang): implement height
        style.locale?.let {
            spannableString.setSpan(
                // TODO(Migration/haoyuchang): support locale fallback in the framework
                LocaleSpan(Locale(it.language, it.country ?: "")),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        // TODO(Migration/haoyuchang): framework only support background color now
        style.background?.let {
            spannableString.setSpan(
                BackgroundColorSpan(it.toArgb()),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        // TODO(Migration/haoyuchang): implement foreground or decide if we really need it
        style.shadow?.let {
            spannableString.setSpan(
                ShadowSpan(it.color.toArgb(), it.offset.dx, it.offset.dy, it.blurRadius.value),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }
    return spannableString
}

private fun TextPaint.applyTextStyle(
    style: TextStyle,
    typefaceAdapter: TypefaceAdapter,
    density: Density
): TextStyle {
    // TODO(haoyuchang) remove this engine.ParagraphStyle
    style.fontSize?.let {
        withDensity(density) {
            textSize = it.toPx().value
        }
    }

    // fontSizeScale must be applied after fontSize is applied.
    style.fontSizeScale?.let {
        textSize *= it
    }

    // TODO(siyamed): This default values are problem here. If the user just gives a single font
    // in the family, and does not provide any fontWeight, TypefaceAdapter will still get the
    // call as FontWeight.normal (which is the default value)
    if (style.hasFontAttributes()) {
        typeface = createTypeface(style, typefaceAdapter)
    }

    style.locale?.let {
        textLocale = Locale(
            it.language,
            it.country ?: ""
        )
    }

    style.color?.let {
        color = it.toArgb()
    }

    style.letterSpacing?.let {
        letterSpacing = it
    }

    style.fontFeatureSettings?.let {
        fontFeatureSettings = it
    }

    style.textGeometricTransform?.scaleX?.let {
        textScaleX *= it
    }

    style.textGeometricTransform?.skewX?.let {
        textSkewX += it
    }

    style.shadow?.let {
        setShadowLayer(
            it.blurRadius.value,
            it.offset.dx,
            it.offset.dy,
            it.color.toArgb()
        )
    }

    style.decoration?.let {
        if (it.contains(TextDecoration.Underline)) {
            isUnderlineText = true
        }
        if (it.contains(TextDecoration.LineThrough)) {
            isStrikeThruText = true
        }
    }

    // baselineShift and bgColor is reset in the Android Layout constructor.
    // therefore we cannot apply them on paint, have to use spans.
    return TextStyle(
        background = style.background,
        baselineShift = style.baselineShift
    )
}

/**
 * For a given [TextDirectionAlgorithm] return [TextLayout] constants for text direction
 * heuristics.
 */
internal fun resolveTextDirectionHeuristics(
    layoutDirection: LayoutDirection,
    textDirectionAlgorithm: TextDirectionAlgorithm?
): Int {
    if (textDirectionAlgorithm == null) {
        return if (layoutDirection == LayoutDirection.Ltr) {
            TEXT_DIRECTION_FIRST_STRONG_LTR
        } else {
            TEXT_DIRECTION_FIRST_STRONG_RTL
        }
    }

    return when (textDirectionAlgorithm) {
        TextDirectionAlgorithm.ContentOrLtr -> TEXT_DIRECTION_FIRST_STRONG_LTR
        TextDirectionAlgorithm.ContentOrRtl -> TEXT_DIRECTION_FIRST_STRONG_RTL
        TextDirectionAlgorithm.ForceLtr -> TEXT_DIRECTION_LTR
        TextDirectionAlgorithm.ForceRtl -> TEXT_DIRECTION_RTL
    }
}
/**
 * Returns true if this [TextStyle] contains any font style attributes set.
 */
private fun TextStyle.hasFontAttributes(): Boolean {
    return fontFamily != null || fontStyle != null || fontWeight != null
}

/**
 * Converts [TextAlign] into [TextLayout] alignment constants.
 */
private fun toLayoutAlign(align: TextAlign?): Int = when (align) {
    TextAlign.Left -> ALIGN_LEFT
    TextAlign.Right -> ALIGN_RIGHT
    TextAlign.Center -> ALIGN_CENTER
    TextAlign.Start -> ALIGN_NORMAL
    TextAlign.End -> ALIGN_OPPOSITE
    else -> DEFAULT_ALIGNMENT
}