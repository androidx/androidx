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
package androidx.compose.ui.text.android

import android.graphics.Canvas
import android.graphics.Paint.FontMetricsInt
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.os.Trace
import android.text.BoringLayout
import android.text.GraphemeClusterSegmentFinder
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextDirectionHeuristic
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import androidx.annotation.Px
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.collection.MutableFloatList
import androidx.compose.ui.text.addBox
import androidx.compose.ui.text.android.LayoutCompat.ALIGN_CENTER
import androidx.compose.ui.text.android.LayoutCompat.ALIGN_LEFT
import androidx.compose.ui.text.android.LayoutCompat.ALIGN_NORMAL
import androidx.compose.ui.text.android.LayoutCompat.ALIGN_OPPOSITE
import androidx.compose.ui.text.android.LayoutCompat.ALIGN_RIGHT
import androidx.compose.ui.text.android.LayoutCompat.BreakStrategy
import androidx.compose.ui.text.android.LayoutCompat.DEFAULT_ALIGNMENT
import androidx.compose.ui.text.android.LayoutCompat.DEFAULT_BREAK_STRATEGY
import androidx.compose.ui.text.android.LayoutCompat.DEFAULT_HYPHENATION_FREQUENCY
import androidx.compose.ui.text.android.LayoutCompat.DEFAULT_INCLUDE_PADDING
import androidx.compose.ui.text.android.LayoutCompat.DEFAULT_JUSTIFICATION_MODE
import androidx.compose.ui.text.android.LayoutCompat.DEFAULT_LINESPACING_EXTRA
import androidx.compose.ui.text.android.LayoutCompat.DEFAULT_LINESPACING_MULTIPLIER
import androidx.compose.ui.text.android.LayoutCompat.DEFAULT_LINE_BREAK_STYLE
import androidx.compose.ui.text.android.LayoutCompat.DEFAULT_LINE_BREAK_WORD_STYLE
import androidx.compose.ui.text.android.LayoutCompat.DEFAULT_TEXT_DIRECTION
import androidx.compose.ui.text.android.LayoutCompat.HyphenationFrequency
import androidx.compose.ui.text.android.LayoutCompat.JustificationMode
import androidx.compose.ui.text.android.LayoutCompat.LineBreakStyle
import androidx.compose.ui.text.android.LayoutCompat.LineBreakWordStyle
import androidx.compose.ui.text.android.LayoutCompat.TEXT_DIRECTION_ANY_RTL_LTR
import androidx.compose.ui.text.android.LayoutCompat.TEXT_DIRECTION_FIRST_STRONG_LTR
import androidx.compose.ui.text.android.LayoutCompat.TEXT_DIRECTION_FIRST_STRONG_RTL
import androidx.compose.ui.text.android.LayoutCompat.TEXT_DIRECTION_LOCALE
import androidx.compose.ui.text.android.LayoutCompat.TEXT_DIRECTION_LTR
import androidx.compose.ui.text.android.LayoutCompat.TEXT_DIRECTION_RTL
import androidx.compose.ui.text.android.LayoutCompat.TEXT_GRANULARITY_WORD
import androidx.compose.ui.text.android.LayoutCompat.TextDirection
import androidx.compose.ui.text.android.LayoutCompat.TextGranularity
import androidx.compose.ui.text.android.LayoutCompat.TextLayoutAlignment
import androidx.compose.ui.text.android.selection.Api34SegmentFinder.toAndroidSegmentFinder
import androidx.compose.ui.text.android.selection.WordIterator
import androidx.compose.ui.text.android.selection.WordSegmentFinder
import androidx.compose.ui.text.android.style.BaselineShiftSpan
import androidx.compose.ui.text.android.style.LineHeightStyleSpan
import androidx.compose.ui.text.android.style.getEllipsizedLeftPadding
import androidx.compose.ui.text.android.style.getEllipsizedRightPadding
import androidx.compose.ui.text.boxBottom
import androidx.compose.ui.text.boxCount
import androidx.compose.ui.text.boxLeft
import androidx.compose.ui.text.boxRight
import androidx.compose.ui.text.boxTop
import androidx.compose.ui.text.internal.requirePrecondition
import androidx.compose.ui.text.setBox
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.util.fastCoerceAtMost
import kotlin.concurrent.getOrSet
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/** We swap canvas delegates, and can share the wrapper on the current thread. */
@VisibleForTesting
internal val SharedTextAndroidCanvas: ThreadLocal<TextAndroidCanvas> = ThreadLocal()

/**
 * Wrapper for Static Text Layout classes.
 *
 * @param charSequence text to be laid out.
 * @param width the maximum width for the text
 * @param textPaint base paint used for text layout
 * @param alignment text alignment for the text layout. One of [TextLayoutAlignment].
 * @param ellipsize whether the text needs to be ellipsized. If the maxLines is set and text cannot
 *   fit in the provided number of lines.
 * @param textDirectionHeuristic the heuristics to be applied while deciding on the text direction.
 * @param lineSpacingMultiplier the multiplier to be applied to each line of the text.
 * @param lineSpacingExtra the extra height to be added to each line of the text.
 * @param includePadding defines whether the extra space to be applied beyond font ascent and
 *   descent
 * @param fallbackLineSpacing Sets Android TextView#setFallbackLineSpacing. This value should be set
 *   to true in most cases and it is the default on platform; otherwise tall scripts such as Burmese
 *   or Tibetan result in clippings on top and bottom sometimes making the text not-readable.
 * @param maxLines the maximum number of lines to be laid out.
 * @param breakStrategy the strategy to be used for line breaking
 * @param hyphenationFrequency set the frequency to control the amount of automatic hyphenation
 *   applied.
 * @param justificationMode whether to justify the text.
 * @param leftIndents the indents to be applied to the left of the text as pixel values. Each
 *   element in the array is applied to the corresponding line. For lines past the last element in
 *   array, the last element repeats.
 * @param rightIndents the indents to be applied to the right of the text as pixel values. Each
 *   element in the array is applied to the corresponding line. For lines past the last element in
 *   array, the last element repeats.
 * @param layoutIntrinsics previously calculated [LayoutIntrinsics] for this text
 * @see StaticLayoutFactory
 * @see BoringLayoutFactory
 */
@OptIn(InternalPlatformTextApi::class)
internal class TextLayout
constructor(
    charSequence: CharSequence,
    width: Float,
    val textPaint: TextPaint,
    @TextLayoutAlignment alignment: Int = DEFAULT_ALIGNMENT,
    private val ellipsize: TextUtils.TruncateAt? = null,
    @TextDirection textDirectionHeuristic: Int = DEFAULT_TEXT_DIRECTION,
    lineSpacingMultiplier: Float = DEFAULT_LINESPACING_MULTIPLIER,
    @Px lineSpacingExtra: Float = DEFAULT_LINESPACING_EXTRA,
    val includePadding: Boolean = DEFAULT_INCLUDE_PADDING,
    val fallbackLineSpacing: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    @BreakStrategy breakStrategy: Int = DEFAULT_BREAK_STRATEGY,
    @LineBreakStyle lineBreakStyle: Int = DEFAULT_LINE_BREAK_STYLE,
    @LineBreakWordStyle lineBreakWordStyle: Int = DEFAULT_LINE_BREAK_WORD_STYLE,
    @HyphenationFrequency hyphenationFrequency: Int = DEFAULT_HYPHENATION_FREQUENCY,
    @JustificationMode justificationMode: Int = DEFAULT_JUSTIFICATION_MODE,
    leftIndents: IntArray? = null,
    rightIndents: IntArray? = null,
    val layoutIntrinsics: LayoutIntrinsics =
        LayoutIntrinsics(charSequence, textPaint, textDirectionHeuristic),
) {
    val maxIntrinsicWidth: Float
        get() = layoutIntrinsics.maxIntrinsicWidth

    val minIntrinsicWidth: Float
        get() = layoutIntrinsics.minIntrinsicWidth

    val didExceedMaxLines: Boolean

    private var backingWordIterator: WordIterator? = null
    val wordIterator: WordIterator
        get() {
            val finalWordIterator = backingWordIterator
            if (finalWordIterator != null) return finalWordIterator
            return WordIterator(layout.text, 0, layout.text.length, textPaint.textLocale).also {
                backingWordIterator = it
            }
        }

    /** Please do not access this object directly from runtime code. */
    @VisibleForTesting val layout: Layout

    /**
     * Resolved line count. If maxLines smaller than the real number of lines in the text, this
     * property will return the minimum between the two
     */
    val lineCount: Int

    /**
     * Top padding is added for backporting fallbackLineSpacing behavior. If a tall script is being
     * laid out, topPadding might be non zero (based on Android version and support in StaticLayout
     * and BoringLayout). When top padding is non-zero, the height of the TextLayout will increase
     * with top padding to prevent clipping of the top of the first line.
     */
    @VisibleForTesting internal val topPadding: Int

    /**
     * Bottom padding is added for backporting fallbackLineSpacing behavior. If a tall script is
     * being laid out, bottomPadding might be non zero (based on Android version and support in
     * StaticLayout and BoringLayout). When bottom padding is non-zero, the height of the TextLayout
     * will increase with bottom padding to prevent clipping of the bottom if the last line.
     */
    @VisibleForTesting internal val bottomPadding: Int

    /**
     * When letter spacing, align and ellipsize applied to text, the ellipsized line is indented
     * wrong. For example for an LTR text, the last line is indented in a way where the beginning of
     * the line is less than 0 and the text is cut at the beginning.
     *
     * This attribute is used to fix the line left pixel positions accordingly.
     */
    private val leftPadding: Float

    /**
     * When letter spacing, align and ellipsize applied to text, the ellipsized line is indented
     * wrong. For example for an RTL text, the last line is indented in a way where the beginning of
     * the line is more than layout width and the text is cut at the beginning.
     *
     * This attribute is used to fix the line right pixel positions accordingly.
     */
    private val rightPadding: Float

    /** When true the wrapped layout that was created is a BoringLayout. */
    private val isBoringLayout: Boolean

    /**
     * When the last line of the text is empty, ParagraphStyle's are not applied. This becomes
     * visible during edit operations when the text field is empty or user inputs an new line
     * character. This layout contains the text layout that would be applied if the last line was
     * not empty.
     */
    private val lastLineFontMetrics: FontMetricsInt?

    /**
     * Holds the difference in line height for the lastLineFontMetrics and the wrapped text layout.
     */
    private val lastLineExtra: Int

    private val lineHeightSpans: Array<LineHeightStyleSpan>?

    private val rect: Rect = Rect()

    init {
        val end = charSequence.length
        val frameworkTextDir = getTextDirectionHeuristic(textDirectionHeuristic)
        val frameworkAlignment = TextAlignmentAdapter.get(alignment)

        // BoringLayout won't adjust line height for baselineShift,
        // use StaticLayout for those spans.
        val hasBaselineShiftSpans =
            if (charSequence is Spanned) {
                // nextSpanTransition returns limit if there isn't any span.
                charSequence.nextSpanTransition(-1, end, BaselineShiftSpan::class.java) < end
            } else {
                false
            }

        Trace.beginSection("TextLayout:initLayout")
        try {
            val boringMetrics = layoutIntrinsics.boringMetrics

            val widthInt = ceil(width).toInt()
            layout =
                if (
                    boringMetrics != null &&
                        layoutIntrinsics.maxIntrinsicWidth <= width &&
                        !hasBaselineShiftSpans
                ) {
                    isBoringLayout = true
                    BoringLayoutFactory.create(
                        text = charSequence,
                        paint = textPaint,
                        width = widthInt,
                        metrics = boringMetrics,
                        alignment = frameworkAlignment,
                        includePadding = includePadding,
                        useFallbackLineSpacing = fallbackLineSpacing,
                        ellipsize = ellipsize,
                        ellipsizedWidth = widthInt,
                    )
                } else {
                    isBoringLayout = false
                    StaticLayoutFactory.create(
                        // See StringCharSequence: avoids copying the whole String once per drawn
                        // line. BoringLayout has a single line, so it is not wrapped.
                        text =
                            if (charSequence is String) {
                                StringCharSequence(charSequence)
                            } else {
                                charSequence
                            },
                        paint = textPaint,
                        width = widthInt,
                        start = 0,
                        end = charSequence.length,
                        textDir = frameworkTextDir,
                        alignment = frameworkAlignment,
                        maxLines = maxLines,
                        ellipsize = ellipsize,
                        ellipsizedWidth = ceil(width).toInt(),
                        lineSpacingMultiplier = lineSpacingMultiplier,
                        lineSpacingExtra = lineSpacingExtra,
                        justificationMode = justificationMode,
                        includePadding = includePadding,
                        useFallbackLineSpacing = fallbackLineSpacing,
                        breakStrategy = breakStrategy,
                        lineBreakStyle = lineBreakStyle,
                        lineBreakWordStyle = lineBreakWordStyle,
                        hyphenationFrequency = hyphenationFrequency,
                        leftIndents = leftIndents,
                        rightIndents = rightIndents,
                    )
                }
        } finally {
            Trace.endSection()
        }

        /* When ellipsis is false:
         1. Before API 25(include 25), if the number of the actual text lines in the layout is
         greater than the maxLines, layout.lineCount will be set to the maxLines.
         2. After API 25(exclude 25), the layout.lineCount will be the actual number of the text
         lines in the layout even if layout.lineCount > maxLines.
         When ellipsis is true:
         If the number of the actual text lines in the layout is greater than maxLines,
         layout.lineCount will be set to the maxLines.
         To unify the behavior of lineCount, no matter ellipsis is on or off, when the number of
         the actual text lines in the layout is greater than the maxLines, the maxLines is
         always returned.
        */
        lineCount = min(layout.lineCount, maxLines)
        val lastLine = lineCount - 1

        didExceedMaxLines =
            /* When lineCount is less than maxLines, actual line count is guaranteed not to exceed
            the maxLines.
            But when lineCount == maxLines, the actual line count may exceeds the maxLines in the
            following two scenarios:
            1. Ellipsis is on and the actual line count exceeds maxLines.
            2. It's under API 25(include 25), ellipsis is off and the actual line count exceeds
            the maxLines.
            */
            if (lineCount < maxLines) {
                false
            } else {
                /* When maxLines exceeds
                 1. if ellipsis is applied, ellipsisCount of lastLine is greater than 0. It works
                 for all ellipsis position because start/middle ellipsis only supported for a single
                 line text.
                 2. if ellipsis is not applies, lineEnd of the last line is unequals to
                 charSequence.length.
                 On certain cases, even though ellipsize is set, text overflow might still be
                 handled by truncating.
                 So we have to check both cases, no matter what ellipsis parameter is passed.
                */
                layout.getEllipsisCount(lastLine) > 0 ||
                    layout.getLineEnd(lastLine) != charSequence.length
            }

        lineHeightSpans = getLineHeightSpans()
        // Even though it is an array of spans, we know that LineHeightStyle is part of
        // ParagraphStyle and there can only be one ParagraphStyle per text layout. So there should
        // also be only one LineHeightStyleSpan in this array. Please check getLastLineMetrics that
        // uses a similar approach.
        val shouldForceTrimTop =
            lineHeightSpans?.firstOrNull()?.let {
                it.trimFirstLineTop && it.mode == LineHeightStyle.Mode.Tight
            } ?: false

        val shouldForceTrimBottom =
            lineHeightSpans?.firstOrNull()?.let {
                it.trimLastLineBottom && it.mode == LineHeightStyle.Mode.Tight
            } ?: false

        val verticalPaddings =
            if (shouldForceTrimTop && shouldForceTrimBottom) {
                ZeroVerticalPadding
            } else {
                val paddings = getVerticalPaddings()
                VerticalPaddings(
                    topPadding = if (shouldForceTrimTop) 0 else paddings.topPadding,
                    bottomPadding = if (shouldForceTrimBottom) 0 else paddings.bottomPadding,
                )
            }

        val lineHeightPaddings = lineHeightSpans?.getLineHeightPaddings() ?: ZeroVerticalPadding
        topPadding = max(verticalPaddings.topPadding, lineHeightPaddings.topPadding)
        bottomPadding = max(verticalPaddings.bottomPadding, lineHeightPaddings.bottomPadding)

        val fontMetrics = getLastLineMetrics(textPaint, frameworkTextDir, lineHeightSpans)
        lastLineExtra =
            if (fontMetrics != null) {
                fontMetrics.bottom - getLineHeight(lastLine).toInt()
            } else {
                0
            }
        // Set lastLineFontMetrics after calling getLineHeight() above, as the metrics
        // are different when lastLineFontMetrics is null
        lastLineFontMetrics = fontMetrics

        leftPadding = layout.getEllipsizedLeftPadding(lastLine)
        rightPadding = layout.getEllipsizedRightPadding(lastLine)
    }

    private var backingLayoutHelper: LayoutHelper? = null
    private val layoutHelper: LayoutHelper
        get() {
            if (backingLayoutHelper == null) {
                return LayoutHelper(layout).also { backingLayoutHelper = it }
            }
            return backingLayoutHelper!!
        }

    val text: CharSequence
        get() = layout.text

    val height: Int
        get() =
            if (didExceedMaxLines) {
                layout.getLineBottom(lineCount - 1)
            } else {
                layout.height
            } + topPadding + bottomPadding + lastLineExtra

    private fun getHorizontalPadding(line: Int): Float {
        return if (line == lineCount - 1) {
            leftPadding + rightPadding
        } else {
            0f
        }
    }

    fun getLineLeft(lineIndex: Int): Float =
        layout.getLineLeft(lineIndex) + if (lineIndex == lineCount - 1) leftPadding else 0f

    /** Return the horizontal leftmost position of the line in pixels. */
    fun getLineRight(lineIndex: Int): Float =
        layout.getLineRight(lineIndex) + if (lineIndex == lineCount - 1) rightPadding else 0f

    /**
     * Return the vertical position of the top of the line in pixels. If the line is equal to the
     * line count, returns the bottom of the last line.
     */
    fun getLineTop(line: Int): Float {
        val top = layout.getLineTop(line).toFloat()
        return top + if (line == 0) 0 else topPadding
    }

    /** Return the vertical position of the bottom of the line in pixels. */
    fun getLineBottom(line: Int): Float {
        if (line == lineCount - 1 && lastLineFontMetrics != null) {
            return layout.getLineBottom(line - 1).toFloat() + lastLineFontMetrics.bottom
        }

        return topPadding +
            layout.getLineBottom(line).toFloat() +
            if (line == lineCount - 1) bottomPadding else 0
    }

    /**
     * Returns the ascent of the line in the line coordinates. Baseline is considered to be 0,
     * therefore ascent is generally a negative value. The unit for values are pixels.
     *
     * @param line the line index starting from 0
     */
    fun getLineAscent(line: Int): Float {
        return if (line == lineCount - 1 && lastLineFontMetrics != null) {
            lastLineFontMetrics.ascent.toFloat()
        } else {
            layout.getLineAscent(line).toFloat()
        }
    }

    /** Return the vertical position of the baseline of the line in pixels. */
    fun getLineBaseline(line: Int): Float {
        return topPadding +
            if (line == lineCount - 1 && lastLineFontMetrics != null) {
                getLineTop(line) - lastLineFontMetrics.ascent
            } else {
                layout.getLineBaseline(line).toFloat()
            }
    }

    /**
     * Returns the descent of the line in the line coordinates. Baseline is considered to be 0,
     * therefore descent is generally a positive value. The unit for values are pixels.
     *
     * @param line the line index starting from 0
     */
    fun getLineDescent(line: Int): Float {
        return if (line == lineCount - 1 && lastLineFontMetrics != null) {
            lastLineFontMetrics.descent.toFloat()
        } else {
            layout.getLineDescent(line).toFloat()
        }
    }

    fun getLineHeight(lineIndex: Int): Float = getLineBottom(lineIndex) - getLineTop(lineIndex)

    /** Return the width of the line in pixels. */
    fun getLineWidth(lineIndex: Int): Float = layout.getLineWidth(lineIndex)

    /**
     * Return the text offset at the beginning of the line. If the line is equal to the line count,
     * returns the length of the text.
     */
    fun getLineStart(lineIndex: Int): Int = layout.getLineStart(lineIndex)

    /**
     * Return the text offset at the end of the line. If the line is equal to the line count,
     * returns the length of the text.
     */
    fun getLineEnd(lineIndex: Int): Int =
        if (layout.isLineEllipsized(lineIndex) && ellipsize == TextUtils.TruncateAt.END) {
            // Layout#getLineEnd usually gets the end of text for the last line even if ellipsis
            // happens. However, if LF character is included in the ellipsized region, getLineEnd
            // returns LF character offset. So, use end of text for line end here.
            layout.text.length
        } else {
            layout.getLineEnd(lineIndex)
        }

    /**
     * Return the text offset after the last visible character on the specified line. For example
     * whitespaces are not counted as visible characters.
     */
    fun getLineVisibleEnd(lineIndex: Int): Int =
        if (layout.isLineEllipsized(lineIndex) && ellipsize == TextUtils.TruncateAt.END) {
            layout.getLineStart(lineIndex) + layout.getEllipsisStart(lineIndex)
        } else {
            layoutHelper.getLineVisibleEnd(lineIndex)
        }

    fun isLineEllipsized(lineIndex: Int) = layout.isLineEllipsized(lineIndex)

    fun getLineEllipsisOffset(lineIndex: Int): Int = layout.getEllipsisStart(lineIndex)

    fun getLineEllipsisCount(lineIndex: Int): Int = layout.getEllipsisCount(lineIndex)

    fun getLineForVertical(vertical: Int): Int {
        if (lineCount <= 0) return 0
        return layout.getLineForVertical(vertical - topPadding).fastCoerceAtMost(lineCount - 1)
    }

    /**
     * Returns the line number for the vertical position unbounded. The result is not coerced to
     * [lineCount]. Be careful using this result in line query functions of
     * [androidx.compose.ui.text.MultiParagraph] because it may throw an [IllegalArgumentException].
     */
    fun getLineForVerticalUnbounded(vertical: Int): Int {
        return layout.getLineForVertical(vertical - topPadding)
    }

    fun getOffsetForHorizontal(line: Int, horizontal: Float): Int {
        return layout.getOffsetForHorizontal(line, horizontal + -1 * getHorizontalPadding(line))
    }

    /**
     * Returns horizontal position for an offset from the drawing origin of a new character would be
     * inserted at that offset.
     *
     * *primary* means that the inserting character's direction will be resolved to the *same*
     * direction to the paragraph direction. For example, the insertion position for an LTR
     * character in an LTR paragraph or RTL character in an RTL paragraph.
     *
     * The location that is being queried can also be different based on line breaks. Consider the
     * following example:
     * <pre>
     *    aa
     *    bb
     * <pre/>
     *
     *
     * In the example above, if offset is the end of the first line then it is required to know if
     * the position to be returned is the end of the first line ("aa") or the beginning of the next
     * line ("bb").
     *
     * When the end of line is needed [upstream] should be set to true; when the beginning of next
     * line is needed [upstream] should be set to false (therefore it is downstream).
     *
     * @param offset offset the character index
     * @param upstream to return the end of the line for offsets that are at the end
     * of a line. false returns the beginning of the next line
     *
     * @return the horizontal position of an offset from the drawing origin
     */
    fun getPrimaryHorizontal(offset: Int, upstream: Boolean = false): Float {
        return layoutHelper.getHorizontalPosition(
            offset,
            usePrimaryDirection = true,
            upstream = upstream,
        ) + getHorizontalPadding(getLineForOffset(offset))
    }

    /**
     * Returns horizontal position for an offset from the drawing origin of a new character would be
     * inserted at that offset.
     *
     * *secondary* means that the inserting character's direction will be resolved to the *opposite*
     * direction to the paragraph direction. For example, the insertion position for an RTL
     * character in an LTR paragraph or LTR character in an RTL paragraph.
     *
     * The location that is being queried can also be different based on line breaks. Consider the
     * following example:
     * <pre>
     *    aa
     *    bb
     * <pre/>
     *
     *
     * In the example above, if offset is the end of the first line then it is required to know if
     * the position to be returned is the end of the first line ("aa") or the beginning of the next
     * line ("bb").
     *
     * When the end of line is needed [upstream] should be set to true; when the beginning of next
     * line is needed [upstream] should be set to false (therefore it is downstream).
     *
     * @param offset offset the character index
     * @param upstream true to return the end of the line for offsets that are at the end
     * of a line. false returns the beginning of the next line.
     *
     * @return the horizontal position of an offset from the drawing origin
     */
    fun getSecondaryHorizontal(offset: Int, upstream: Boolean = false): Float {
        return layoutHelper.getHorizontalPosition(
            offset,
            usePrimaryDirection = false,
            upstream = upstream,
        ) + getHorizontalPadding(getLineForOffset(offset))
    }

    fun getLineForOffset(offset: Int): Int {
        if (lineCount <= 0) return 0
        if (offset >= layout.text.length) {
            return lineCount - 1
        }
        if (offset <= 0) {
            return 0
        }
        return layout.getLineForOffset(offset).fastCoerceAtMost(lineCount - 1)
    }

    fun isRtlCharAt(offset: Int): Boolean = layout.isRtlCharAt(offset)

    fun getParagraphDirection(line: Int): Int = layout.getParagraphDirection(line)

    fun getSelectionPath(start: Int, end: Int, dest: Path) {
        layout.getSelectionPath(start, end, dest)
        if (topPadding != 0 && !dest.isEmpty) {
            dest.offset(0f /* dx */, topPadding.toFloat() /* dy */)
        }
    }

    fun getRangeForRect(
        rect: RectF,
        @TextGranularity granularity: Int,
        inclusionStrategy: (RectF, RectF) -> Boolean,
    ): IntArray? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return AndroidLayoutApi34.getRangeForRect(this, rect, granularity, inclusionStrategy)
        }
        return getRangeForRect(layout, layoutHelper, rect, granularity, inclusionStrategy)
    }

    /**
     * A lightweight version of fillBoundingBoxes that only fills the horizontal bounds of
     * characters in the line referred by the given [lineIndex]. The result will be filled into the
     * given [array] where the left or right bounds i-th character in the line is stored as the (i *
     * 2)-th or (i * 2 + 1)-th element respectively.
     */
    internal fun fillLineHorizontalBounds(lineIndex: Int, array: FloatArray) {
        val lineStartOffset = getLineStart(lineIndex)
        val lineEndOffset = getLineEnd(lineIndex)

        val range = lineEndOffset - lineStartOffset
        val minArraySize = range * 2

        requirePrecondition(array.size >= minArraySize) {
            "array.size - arrayStart must be greater or equal than (endOffset - startOffset) * 2"
        }

        val cache = HorizontalPositionCache(this)

        val isLtrLine = getParagraphDirection(lineIndex) == Layout.DIR_LEFT_TO_RIGHT

        var arrayOffset = 0
        for (offset in lineStartOffset until lineEndOffset) {
            val isRtlChar = isRtlCharAt(offset)

            val left: Float
            val right: Float

            when {
                isLtrLine && !isRtlChar -> {
                    left = cache.getPrimaryDownstream(offset)
                    right = cache.getPrimaryUpstream(offset + 1)
                }
                isLtrLine && isRtlChar -> {
                    right = cache.getSecondaryDownstream(offset)
                    left = cache.getSecondaryUpstream(offset + 1)
                }
                isRtlChar -> {
                    right = cache.getPrimaryDownstream(offset)
                    left = cache.getPrimaryUpstream(offset + 1)
                }
                else -> {
                    left = cache.getSecondaryDownstream(offset)
                    right = cache.getSecondaryUpstream(offset + 1)
                }
            }
            array[arrayOffset] = left
            array[arrayOffset + 1] = right
            arrayOffset += 2
        }
    }

    /**
     * Fills the bounding boxes for characters within the [startOffset] (inclusive) and [endOffset]
     * (exclusive). The array is filled starting from [arrayStart] (inclusive). The coordinates are
     * in local text layout coordinates.
     *
     * The returned information consists of left/right of a character; line top and bottom for the
     * same character.
     *
     * For the grapheme consists of multiple code points, e.g. ligatures, combining marks, the first
     * character has the total width and the remaining are returned as zero-width.
     *
     * The array divided into segments of four where each index in that segment represents left,
     * top, right, bottom of the character.
     *
     * The size of the provided [array] should be greater or equal than fours times the range
     * provided with [startOffset] and [endOffset].
     *
     * The final order of characters in the [array] is from [startOffset] to [endOffset].
     *
     * @param startOffset inclusive startOffset, must be smaller than [endOffset]
     * @param endOffset exclusive end offset, must be greater than [startOffset]
     * @param array the array to fill in the values. The array divided into segments of four where
     *   each index in that segment represents left, top, right, bottom of the character.
     * @param arrayStart the inclusive start index in the array where the function will start
     *   filling in the values from
     */
    fun fillBoundingBoxes(startOffset: Int, endOffset: Int, array: FloatArray, arrayStart: Int) {
        val textLength = text.length
        requirePrecondition(startOffset >= 0) { "startOffset must be > 0" }
        requirePrecondition(startOffset < textLength) {
            "startOffset must be less than text length"
        }
        requirePrecondition(endOffset > startOffset) {
            "endOffset must be greater than startOffset"
        }
        requirePrecondition(endOffset <= textLength) {
            "endOffset must be smaller or equal to text length"
        }

        val range = endOffset - startOffset
        val minArraySize = range * 4

        requirePrecondition((array.size - arrayStart) >= minArraySize) {
            "array.size - arrayStart must be greater or equal than (endOffset - startOffset) * 4"
        }

        val firstLine = getLineForOffset(startOffset)
        val lastLine = getLineForOffset(endOffset - 1)

        val cache = HorizontalPositionCache(this)

        var arrayOffset = arrayStart
        for (line in firstLine..lastLine) {
            val lineStartOffset = getLineStart(line)
            val lineEndOffset = getLineEnd(line)
            val actualStartOffset = max(startOffset, lineStartOffset)
            val actualEndOffset = min(endOffset, lineEndOffset)

            val lineTop = getLineTop(line)
            val lineBottom = getLineBottom(line)

            val isLtrLine = getParagraphDirection(line) == Layout.DIR_LEFT_TO_RIGHT
            val isRtlLine = !isLtrLine

            for (offset in actualStartOffset until actualEndOffset) {
                val isRtlChar = isRtlCharAt(offset)

                val left: Float
                val right: Float
                when {
                    isLtrLine && !isRtlChar -> {
                        left = cache.getPrimaryDownstream(offset)
                        right = cache.getPrimaryUpstream(offset + 1)
                    }
                    isLtrLine && isRtlChar -> {
                        right = cache.getSecondaryDownstream(offset)
                        left = cache.getSecondaryUpstream(offset + 1)
                    }
                    isRtlLine && isRtlChar -> {
                        right = cache.getPrimaryDownstream(offset)
                        left = cache.getPrimaryUpstream(offset + 1)
                    }
                    else -> {
                        left = cache.getSecondaryDownstream(offset)
                        right = cache.getSecondaryUpstream(offset + 1)
                    }
                }
                array[arrayOffset] = left
                array[arrayOffset + 1] = lineTop
                array[arrayOffset + 2] = right
                array[arrayOffset + 3] = lineBottom
                arrayOffset += 4
            }
        }
    }

    /** Returns the bounding box as Rect of the character for given character offset. */
    fun getBoundingBox(offset: Int): RectF {
        // Although this function shares its core logic with [fillBoundingBoxes], there is no
        // need to use a [HorizontalPositionCache]. Hence, [getBoundingBox] runs the same algorithm
        // without using the cache. Any core logic change here or in [fillBoundingBoxes] should
        // be reflected on the other.
        val line = getLineForOffset(offset)
        val lineTop = getLineTop(line)
        val lineBottom = getLineBottom(line)

        val isLtrLine = getParagraphDirection(line) == Layout.DIR_LEFT_TO_RIGHT
        val isRtlChar = layout.isRtlCharAt(offset)

        val left: Float
        val right: Float
        when {
            isLtrLine && !isRtlChar -> {
                left = getPrimaryHorizontal(offset, upstream = false)
                right = getPrimaryHorizontal(offset + 1, upstream = true)
            }
            isLtrLine && isRtlChar -> {
                right = getSecondaryHorizontal(offset, upstream = false)
                left = getSecondaryHorizontal(offset + 1, upstream = true)
            }
            isRtlChar -> {
                right = getPrimaryHorizontal(offset, upstream = false)
                left = getPrimaryHorizontal(offset + 1, upstream = true)
            }
            else -> {
                left = getSecondaryHorizontal(offset, upstream = false)
                right = getSecondaryHorizontal(offset + 1, upstream = true)
            }
        }
        return RectF(left, lineTop, right, lineBottom)
    }

    /**
     * Populates [target] with coalesced per-line bounding rectangles for the character range from
     * [startOffset] (inclusive) to [endOffset] (exclusive), shifted vertically by [paragraphTop].
     *
     * Algorithm stages:
     * 1. Clamp `[startOffset, endOffset)` to `[0, getLineEnd(lineCount - 1))` to exclude characters
     *    truncated beyond `maxLines`, and resolve the intersecting line range
     *    `firstLine..lastLine`.
     * 2. For each `line` in `firstLine..lastLine`, intersect `[startOffset, clampedEnd)` with
     *    `[getLineStart(line), getLineVisibleEnd(line))` to exclude trailing whitespace at soft
     *    line wraps and ellipsized characters.
     * 3. **Full-line fast path**: When the segment spans the entire visible line (`[lineStart,
     *    lineVisibleEnd)`) and the line is not ellipsized (`canUseLineBounds`), emit
     *    `[getLineLeft(line), getLineRight(line)]` directly without querying horizontal character
     *    positions.
     * 4. **Run-boundary measurement**: Otherwise, walk contiguous same-direction runs
     *    ([isRtlCharAt]) across `[segLineStart, segLineEnd)`. When the paragraph is
     *    single-directional (`layoutHelper.analyzeBidi(paraNo) == null`), the entire segment is a
     *    single run and boundaries that coincide with `lineStart` or `lineVisibleEnd` reuse
     *    [getLineLeft] / [getLineRight] when `canUseLineBounds` is `true`; interior boundaries and
     *    bidirectional runs query [LayoutHelper.getHorizontalPosition] at `runStart` (`upstream =
     *    false`) and `runEnd` (`upstream = true`).
     * 5. **BiDi sorting and coalescing**: When a bidirectional line produces more than one box,
     *    sort that line's boxes left-to-right and merge touching or overlapping boxes via
     *    [sortAndCoalesceLineBoxes].
     *
     * @param startOffset inclusive start character offset within this [TextLayout]
     * @param endOffset exclusive end character offset within this [TextLayout]
     * @param paragraphTop vertical offset of this paragraph within its parent `MultiParagraph`
     * @param applyLineHeightOptimization whether the single-line `softWrap = false` line-height
     *   override is active on the enclosing `AndroidParagraph`
     * @param resolvedLineHeight resolved line height in pixels used when
     *   [applyLineHeightOptimization] is `true`
     * @param target [MutableFloatList] to which packed `[left, top, right, bottom]` coordinates are
     *   appended
     */
    internal fun fillRangeBoundingBoxes(
        startOffset: Int,
        endOffset: Int,
        paragraphTop: Float,
        applyLineHeightOptimization: Boolean,
        resolvedLineHeight: Float,
        target: MutableFloatList,
    ) {
        if (startOffset >= endOffset || lineCount <= 0) return
        val clampedEnd = min(endOffset, getLineEnd(lineCount - 1))
        if (startOffset >= clampedEnd) return

        val firstLine = getLineForOffset(startOffset)
        val lastLine = getLineForOffset(clampedEnd - 1)
        if (firstLine > lastLine) return

        target.ensureCapacity(target.size + (lastLine - firstLine + 1) * 4)

        for (line in firstLine..lastLine) {
            val lineStart = getLineStart(line)
            val lineVisibleEnd = getLineVisibleEnd(line)
            // The line segment to be processed, which is the intersection of
            // [startOffset, endOffset) and [lineStart, lineVisibleEnd).
            val segLineStart = max(startOffset, lineStart)
            val segLineEnd = min(clampedEnd, lineVisibleEnd)
            // Skip empty line segment.
            if (segLineStart >= segLineEnd) continue

            val lineTop = (if (applyLineHeightOptimization) 0f else getLineTop(line)) + paragraphTop
            val lineBottom =
                (if (applyLineHeightOptimization) resolvedLineHeight else getLineBottom(line)) +
                    paragraphTop

            val canUseLineBounds = !isLineEllipsized(line)
            // If the line segment is the entire line and the line bounds can be used, then we can
            // use the line bounds directly and skip the rest.
            if (segLineStart == lineStart && segLineEnd == lineVisibleEnd && canUseLineBounds) {
                val left = getLineLeft(line)
                val right = getLineRight(line)
                if (right - left > 0.01f) {
                    target.addBox(left, lineTop, right, lineBottom)
                }
                continue
            }

            val paraNo =
                if (layoutHelper.paragraphCount == 1) 0
                else layoutHelper.getParagraphForOffset(lineStart)
            val paragraphHasBidi = layoutHelper.analyzeBidi(paraNo) != null
            val isParaRtl = getParagraphDirection(line) == Layout.DIR_RIGHT_TO_LEFT
            val horizPadding = getHorizontalPadding(line)
            val lineBoxStartIndex = target.boxCount

            var runStart = segLineStart
            while (runStart < segLineEnd) {
                val isRtlRun = isRtlCharAt(runStart)
                var runEnd = if (paragraphHasBidi) runStart + 1 else segLineEnd
                while (runEnd < segLineEnd && isRtlCharAt(runEnd) == isRtlRun) {
                    runEnd++
                }
                val usePrimary = isParaRtl == isRtlRun
                val startX =
                    if (runStart == lineStart && canUseLineBounds && !paragraphHasBidi) {
                        if (!isRtlRun) getLineLeft(line) else getLineRight(line)
                    } else {
                        layoutHelper.getHorizontalPosition(runStart, usePrimary, upstream = false) +
                            horizPadding
                    }
                val endX =
                    if (runEnd == lineVisibleEnd && canUseLineBounds && !paragraphHasBidi) {
                        if (!isRtlRun) getLineRight(line) else getLineLeft(line)
                    } else {
                        layoutHelper.getHorizontalPosition(runEnd, usePrimary, upstream = true) +
                            horizPadding
                    }
                val left = if (!isRtlRun) startX else endX
                val right = if (!isRtlRun) endX else startX
                if (right - left > 0.01f) {
                    target.addBox(left, lineTop, right, lineBottom)
                }
                runStart = runEnd
            }

            if (target.boxCount - lineBoxStartIndex > 1) {
                sortAndCoalesceLineBoxes(target, lineBoxStartIndex, lineTop, lineBottom)
            }
        }
    }

    /**
     * Sorts the boxes emitted for a single line in `target` starting at box index
     * [lineBoxStartIndex] by `left` coordinate ascending using in-place insertion sort, and merges
     * adjacent boxes that overlap or touch within `0.5px`.
     *
     * Note: The choice of sorting algorithm here is not performance-critical because this function
     * is only reached when a single line contains multiple bidirectional (LTR/RTL) runs, which in
     * practice produces only a few boxes per line. Insertion sort operates in-place without
     * allocating an iterator or comparator.
     */
    private fun sortAndCoalesceLineBoxes(
        target: MutableFloatList,
        lineBoxStartIndex: Int,
        lineTop: Float,
        lineBottom: Float,
    ) {
        val boxCount = target.boxCount
        for (i in lineBoxStartIndex + 1 until boxCount) {
            val keyLeft = target.boxLeft(i)
            val keyTop = target.boxTop(i)
            val keyRight = target.boxRight(i)
            val keyBottom = target.boxBottom(i)
            var j = i - 1
            while (j >= lineBoxStartIndex && target.boxLeft(j) > keyLeft) {
                target.setBox(
                    j + 1,
                    target.boxLeft(j),
                    target.boxTop(j),
                    target.boxRight(j),
                    target.boxBottom(j),
                )
                j--
            }
            target.setBox(j + 1, keyLeft, keyTop, keyRight, keyBottom)
        }
        var writeIndex = lineBoxStartIndex
        for (i in lineBoxStartIndex + 1 until boxCount) {
            val prevLeft = target.boxLeft(writeIndex)
            val prevRight = target.boxRight(writeIndex)
            val nextLeft = target.boxLeft(i)
            val nextRight = target.boxRight(i)
            if (nextLeft <= prevRight + 0.5f) {
                target.setBox(
                    writeIndex,
                    prevLeft,
                    lineTop,
                    max(prevRight, nextRight),
                    lineBottom,
                )
            } else {
                writeIndex++
                if (writeIndex != i) {
                    target.setBox(
                        writeIndex,
                        nextLeft,
                        target.boxTop(i),
                        nextRight,
                        target.boxBottom(i),
                    )
                }
            }
        }
        val newFloatSize = (writeIndex + 1) * 4
        if (newFloatSize < target.size) {
            target.removeRange(newFloatSize, target.size)
        }
    }

    fun paint(canvas: Canvas) {
        // Fix "mDirect" optimization in BoringLayout that directly draws text when it's simple
        // in the case of an empty canvas, we don't need to do anything (which would typically be
        // done in Layout.draw), so this skips all work when canvas clips to empty - matching the
        // behavior in Layout.kt
        if (!canvas.getClipBounds(rect)) {
            // this is a pure "no-work" optimization for avoiding work when text is simple enough
            // to hit BoringLayout mDirect optimization and canvas clips to empty

            // this avoids calling Canvas.drawText on an empty canvas
            return
        }

        if (topPadding != 0) {
            canvas.translate(0f, topPadding.toFloat())
        }

        val threadSharedTextAndroidCanvas = SharedTextAndroidCanvas.getOrSet { TextAndroidCanvas() }
        threadSharedTextAndroidCanvas.withCanvas(canvas) { clipFixedCanvas ->
            layout.draw(clipFixedCanvas)
        }

        if (topPadding != 0) {
            canvas.translate(0f, -1 * topPadding.toFloat())
        }
    }

    /**
     * Checks certain configuration values to understand whether the created text layout comes with
     * fallback line spacing behavior included. Otherwise it needs to be handled here.
     */
    internal fun isFallbackLinespacingApplied(): Boolean {
        return if (isBoringLayout) {
            BoringLayoutFactory.isFallbackLineSpacingEnabled(layout as BoringLayout)
        } else {
            StaticLayoutFactory.isFallbackLineSpacingEnabled(
                layout as StaticLayout,
                fallbackLineSpacing,
            )
        }
    }
}

/**
 * This class is intended to be used *only* by [TextLayout.fillBoundingBoxes]. It is tightly coupled
 * to the code in callee. Do not use.
 *
 * Assumes that downstream calls always called with offset followed by offset+1 in upstream case.
 * Therefore it does not add the downstream calls to the result to the cache but check if it already
 * exists in the cache for early return.
 *
 * On the other hand upstream calls will be cached, since the same offset+1 might be needed on the
 * next character.
 */
@OptIn(InternalPlatformTextApi::class)
private class HorizontalPositionCache(val layout: TextLayout) {
    private var cachedKey: Int = -1
    private var cachedValue: Float = 0f

    fun getPrimaryDownstream(offset: Int): Float {
        // downstream results are not cached
        return get(offset, primary = true, upstream = false, cache = false)
    }

    fun getPrimaryUpstream(offset: Int): Float {
        // upstream results are cached
        return get(offset, primary = true, upstream = true, cache = true)
    }

    fun getSecondaryDownstream(offset: Int): Float {
        // downstream results are not cached
        return get(offset, primary = false, upstream = false, cache = false)
    }

    fun getSecondaryUpstream(offset: Int): Float {
        // upstream results are cached
        return get(offset, primary = false, upstream = true, cache = true)
    }

    /**
     * Returns the primary/secondary horizontal position for upstream or downstream. Very tightly
     * coupled to how get is called from the [TextLayout.fillBoundingBoxes] function.
     *
     * Everytime that function calls either with offset or offset+1. While calling offset, it will
     * set the cache param to false, while calling with offset+1 it will set the cache param to
     * true.
     *
     * For the noncached version, the cache is checked to see if the value exists and returned if
     * so.
     *
     * For the cached version, the cache is populated if the value has not been calculated.
     */
    private fun get(offset: Int, upstream: Boolean, cache: Boolean, primary: Boolean): Float {
        // even if upstream is requested, if the character is not on a line start/end upstream
        // and downstream results will be the same
        val upstreamFinal =
            if (upstream) {
                val lineNo = layout.layout.getLineForOffset(offset, upstream)
                val lineStart = layout.getLineStart(lineNo)
                val lineEnd = layout.getLineEnd(lineNo)
                offset == lineStart || offset == lineEnd
            } else {
                false
            }

        // key for the current request
        val tmpKey =
            (offset) * 4 +
                if (primary) {
                    if (upstreamFinal) 0 else 1
                } else {
                    if (upstreamFinal) 2 else 3
                }

        if (cachedKey == tmpKey) return cachedValue

        val result =
            if (primary) {
                layout.getPrimaryHorizontal(offset, upstream = upstream)
            } else {
                layout.getSecondaryHorizontal(offset, upstream = upstream)
            }

        if (cache) {
            cachedKey = tmpKey
            cachedValue = result
        }

        return result
    }
}

@OptIn(InternalPlatformTextApi::class)
internal fun getTextDirectionHeuristic(
    @TextDirection textDirectionHeuristic: Int
): TextDirectionHeuristic {
    return when (textDirectionHeuristic) {
        TEXT_DIRECTION_LTR -> TextDirectionHeuristics.LTR
        TEXT_DIRECTION_LOCALE -> TextDirectionHeuristics.LOCALE
        TEXT_DIRECTION_RTL -> TextDirectionHeuristics.RTL
        TEXT_DIRECTION_FIRST_STRONG_RTL -> TextDirectionHeuristics.FIRSTSTRONG_RTL
        TEXT_DIRECTION_ANY_RTL_LTR -> TextDirectionHeuristics.ANYRTL_LTR
        TEXT_DIRECTION_FIRST_STRONG_LTR -> TextDirectionHeuristics.FIRSTSTRONG_LTR
        else -> TextDirectionHeuristics.FIRSTSTRONG_LTR
    }
}

@OptIn(InternalPlatformTextApi::class)
internal object TextAlignmentAdapter {
    private val ALIGN_LEFT_FRAMEWORK: Layout.Alignment
    private val ALIGN_RIGHT_FRAMEWORK: Layout.Alignment

    init {
        val values = Layout.Alignment.values()
        var alignLeft = Layout.Alignment.ALIGN_NORMAL
        var alignRight = Layout.Alignment.ALIGN_NORMAL
        for (value in values) {
            if (value.name == "ALIGN_LEFT") {
                alignLeft = value
                continue
            }

            if (value.name == "ALIGN_RIGHT") {
                alignRight = value
                continue
            }
        }

        ALIGN_LEFT_FRAMEWORK = alignLeft
        ALIGN_RIGHT_FRAMEWORK = alignRight
    }

    fun get(@TextLayoutAlignment value: Int): Layout.Alignment {
        return when (value) {
            ALIGN_LEFT -> ALIGN_LEFT_FRAMEWORK
            ALIGN_RIGHT -> ALIGN_RIGHT_FRAMEWORK
            ALIGN_CENTER -> Layout.Alignment.ALIGN_CENTER
            ALIGN_OPPOSITE -> Layout.Alignment.ALIGN_OPPOSITE
            ALIGN_NORMAL -> Layout.Alignment.ALIGN_NORMAL
            else -> Layout.Alignment.ALIGN_NORMAL
        }
    }
}

internal fun VerticalPaddings(topPadding: Int, bottomPadding: Int) =
    VerticalPaddings(packInts(topPadding, bottomPadding))

@kotlin.jvm.JvmInline
internal value class VerticalPaddings internal constructor(internal val packedValue: Long) {

    val topPadding: Int
        get() = unpackInt1(packedValue)

    val bottomPadding: Int
        get() = unpackInt2(packedValue)
}

/**
 * When includeFontPadding is turned off and fallback line spacing is not applied, there remains a
 * chance that tall glyphs may be cut on top and bottom of the text layout. Vertical paddings is a
 * backport of `useFallbackLineSpacing` from the platform.
 */
private fun TextLayout.getVerticalPaddings(): VerticalPaddings {
    if (includePadding || isFallbackLinespacingApplied()) return ZeroVerticalPadding

    val paint = layout.paint
    val text = layout.text

    val firstLineTextBounds =
        paint.getCharSequenceBounds(text, layout.getLineStart(0), layout.getLineEnd(0))
    val ascent = layout.getLineAscent(0)

    // when textBounds.top is "higher" than ascent, we need to add the difference into account
    // since includeFontPadding is false, ascent is at the top of Layout
    val topPadding =
        if (firstLineTextBounds.top < ascent) {
            ascent - firstLineTextBounds.top
        } else {
            layout.topPadding
        }

    val lastLineTextBounds =
        if (lineCount == 1) {
            // reuse the existing rect since there is single line
            firstLineTextBounds
        } else {
            val line = lineCount - 1
            paint.getCharSequenceBounds(text, layout.getLineStart(line), layout.getLineEnd(line))
        }
    val descent = layout.getLineDescent(lineCount - 1)

    // when textBounds.bottom is "lower" than descent, we need to add the difference into account
    // since includeFontPadding is false, descent is at the bottom of Layout
    val bottomPadding =
        if (lastLineTextBounds.bottom > descent) {
            lastLineTextBounds.bottom - descent
        } else {
            layout.bottomPadding
        }

    return if (topPadding == 0 && bottomPadding == 0) {
        ZeroVerticalPadding
    } else {
        VerticalPaddings(topPadding, bottomPadding)
    }
}

private val ZeroVerticalPadding = VerticalPaddings(0, 0)

@OptIn(InternalPlatformTextApi::class)
private fun Array<LineHeightStyleSpan>.getLineHeightPaddings(): VerticalPaddings {
    var firstAscentDiff = 0
    var lastDescentDiff = 0

    for (span in this) {
        if (span.firstAscentDiff < 0) {
            firstAscentDiff = max(firstAscentDiff, abs(span.firstAscentDiff))
        }
        if (span.lastDescentDiff < 0) {
            lastDescentDiff = max(firstAscentDiff, abs(span.lastDescentDiff))
        }
    }

    return if (firstAscentDiff == 0 && lastDescentDiff == 0) {
        ZeroVerticalPadding
    } else {
        VerticalPaddings(firstAscentDiff, lastDescentDiff)
    }
}

@OptIn(InternalPlatformTextApi::class)
private fun TextLayout.getLastLineMetrics(
    textPaint: TextPaint,
    frameworkTextDir: TextDirectionHeuristic,
    lineHeightSpans: Array<LineHeightStyleSpan>?,
): FontMetricsInt? {
    val lastLine = lineCount - 1
    // did not check for "\n" since the last line might include zero width characters
    if (
        layout.getLineStart(lastLine) == layout.getLineEnd(lastLine) &&
            !lineHeightSpans.isNullOrEmpty()
    ) {
        val emptyText = SpannableString("\u200B")
        val lineHeightSpan = lineHeightSpans.first()
        val newLineHeightSpan =
            lineHeightSpan.copy(
                startIndex = 0,
                endIndex = emptyText.length,
                trimFirstLineTop =
                    if (lastLine != 0 && lineHeightSpan.trimLastLineBottom) {
                        false
                    } else {
                        lineHeightSpan.trimLastLineBottom
                    },
            )

        emptyText.setSpan(newLineHeightSpan, 0, emptyText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        val tmpLayout =
            StaticLayoutFactory.create(
                text = emptyText,
                paint = textPaint,
                width = Int.MAX_VALUE,
                start = 0,
                end = emptyText.length,
                textDir = frameworkTextDir,
                includePadding = includePadding,
                useFallbackLineSpacing = fallbackLineSpacing,
            )

        val lastLineFontMetrics =
            FontMetricsInt().apply {
                ascent = tmpLayout.getLineAscent(0)
                descent = tmpLayout.getLineDescent(0)
                top = tmpLayout.getLineTop(0)
                bottom = tmpLayout.getLineBottom(0)
            }

        return lastLineFontMetrics
    }
    return null
}

@OptIn(InternalPlatformTextApi::class)
private fun TextLayout.getLineHeightSpans(): Array<LineHeightStyleSpan>? {
    if (text !is Spanned) return null
    // text can be empty but still include a LineHeightStyleSpan. In that case hasSpan returns false
    // because nextSpanTransition ends up being 0 == text.length
    if (!(text as Spanned).hasSpan(LineHeightStyleSpan::class.java) && text.isNotEmpty()) {
        return null
    }
    val lineHeightStyleSpans =
        (text as Spanned).getSpans(0, text.length, LineHeightStyleSpan::class.java)
    return lineHeightStyleSpans
}

internal fun Layout.isLineEllipsized(lineIndex: Int) = this.getEllipsisCount(lineIndex) > 0

@RequiresApi(34)
internal object AndroidLayoutApi34 {
    internal fun getRangeForRect(
        layout: TextLayout,
        rectF: RectF,
        @TextGranularity granularity: Int,
        inclusionStrategy: (RectF, RectF) -> Boolean,
    ): IntArray? {

        val segmentFinder =
            when (granularity) {
                TEXT_GRANULARITY_WORD ->
                    WordSegmentFinder(layout.text, layout.wordIterator).toAndroidSegmentFinder()
                else -> GraphemeClusterSegmentFinder(layout.text, layout.textPaint)
            }

        return layout.layout.getRangeForRect(rectF, segmentFinder, inclusionStrategy)
    }
}
