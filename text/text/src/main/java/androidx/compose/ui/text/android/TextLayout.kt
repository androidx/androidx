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
import android.os.Trace
import android.text.BoringLayout
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextDirectionHeuristic
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import androidx.annotation.Px
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.text.android.LayoutCompat.ALIGN_CENTER
import androidx.compose.ui.text.android.LayoutCompat.ALIGN_LEFT
import androidx.compose.ui.text.android.LayoutCompat.ALIGN_NORMAL
import androidx.compose.ui.text.android.LayoutCompat.ALIGN_OPPOSITE
import androidx.compose.ui.text.android.LayoutCompat.ALIGN_RIGHT
import androidx.compose.ui.text.android.LayoutCompat.BreakStrategy
import androidx.compose.ui.text.android.LayoutCompat.LineBreakStyle
import androidx.compose.ui.text.android.LayoutCompat.LineBreakWordStyle
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
import androidx.compose.ui.text.android.LayoutCompat.TEXT_DIRECTION_ANY_RTL_LTR
import androidx.compose.ui.text.android.LayoutCompat.TEXT_DIRECTION_FIRST_STRONG_LTR
import androidx.compose.ui.text.android.LayoutCompat.TEXT_DIRECTION_FIRST_STRONG_RTL
import androidx.compose.ui.text.android.LayoutCompat.TEXT_DIRECTION_LOCALE
import androidx.compose.ui.text.android.LayoutCompat.TEXT_DIRECTION_LTR
import androidx.compose.ui.text.android.LayoutCompat.TEXT_DIRECTION_RTL
import androidx.compose.ui.text.android.LayoutCompat.TextDirection
import androidx.compose.ui.text.android.LayoutCompat.TextLayoutAlignment
import androidx.compose.ui.text.android.style.BaselineShiftSpan
import androidx.compose.ui.text.android.style.LineHeightStyleSpan
import androidx.compose.ui.text.android.style.getEllipsizedLeftPadding
import androidx.compose.ui.text.android.style.getEllipsizedRightPadding
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * We swap canvas delegates, and can share the wrapper.
 */
private val SharedTextAndroidCanvas: TextAndroidCanvas = TextAndroidCanvas()

/**
 * Wrapper for Static Text Layout classes.
 *
 * @param charSequence text to be laid out.
 * @param width the maximum width for the text
 * @param textPaint base paint used for text layout
 * @param alignment text alignment for the text layout. One of [TextLayoutAlignment].
 * @param ellipsize whether the text needs to be ellipsized. If the maxLines is set and text
 * cannot fit in the provided number of lines.
 * @param textDirectionHeuristic the heuristics to be applied while deciding on the text direction.
 * @param lineSpacingMultiplier the multiplier to be applied to each line of the text.
 * @param lineSpacingExtra the extra height to be added to each line of the text.
 * @param includePadding defines whether the extra space to be applied beyond font ascent and
 * descent
 * @param fallbackLineSpacing Sets Android TextView#setFallbackLineSpacing. This value should
 * be set to true in most cases and it is the default on platform; otherwise tall scripts such
 * as Burmese or Tibetan result in clippings on top and bottom sometimes making the text
 * not-readable.
 * @param maxLines the maximum number of lines to be laid out.
 * @param breakStrategy the strategy to be used for line breaking
 * @param hyphenationFrequency set the frequency to control the amount of automatic hyphenation
 * applied.
 * @param justificationMode whether to justify the text.
 * @param leftIndents the indents to be applied to the left of the text as pixel values. Each
 * element in the array is applied to the corresponding line. For lines past the last element in
 * array, the last element repeats.
 * @param rightIndents the indents to be applied to the right of the text as pixel values. Each
 * element in the array is applied to the corresponding line. For lines past the last element in
 * array, the last element repeats.
 * @param layoutIntrinsics previously calculated [LayoutIntrinsics] for this text
 *
 * @see StaticLayoutFactory
 * @see BoringLayoutFactory
 *
 * @suppress
 */
@OptIn(InternalPlatformTextApi::class)
@InternalPlatformTextApi
internal class TextLayout constructor(
    charSequence: CharSequence,
    width: Float,
    textPaint: TextPaint,
    @TextLayoutAlignment alignment: Int = DEFAULT_ALIGNMENT,
    ellipsize: TextUtils.TruncateAt? = null,
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
    val layoutIntrinsics: LayoutIntrinsics = LayoutIntrinsics(
        charSequence,
        textPaint,
        textDirectionHeuristic
    )
) {
    val maxIntrinsicWidth: Float
        get() = layoutIntrinsics.maxIntrinsicWidth

    val minIntrinsicWidth: Float
        get() = layoutIntrinsics.minIntrinsicWidth

    val didExceedMaxLines: Boolean

    /**
     * Please do not access this object directly from runtime code.
     */
    @VisibleForTesting
    val layout: Layout

    /**
     * Resolved line count. If maxLines smaller than the real number of lines in the text, this
     * property will return the minimum between the two
     */
    val lineCount: Int

    /**
     * Top padding is added for backporting fallbackLineSpacing behavior. If a tall script is
     * being laid out, topPadding might be non zero (based on Android version and support in
     * StaticLayout and BoringLayout). When top padding is non-zero, the height of the TextLayout
     * will increase with top padding to prevent clipping of the top of the first line.
     */
    @VisibleForTesting
    internal val topPadding: Int

    /**
     * Bottom padding is added for backporting fallbackLineSpacing behavior. If a tall script is
     * being laid out, bottomPadding might be non zero (based on Android version and support in
     * StaticLayout and BoringLayout). When bottom padding is non-zero, the height of the TextLayout
     * will increase with bottom padding to prevent clipping of the bottom if the last line.
     */
    @VisibleForTesting
    internal val bottomPadding: Int

    /**
     * When letter spacing, align and ellipsize applied to text, the ellipsized line is indented
     * wrong. For example for an LTR text, the last line is indented in a way where the beginning
     * of the line is less than 0 and the text is cut at the beginning.
     *
     * This attribute is used to fix the line left pixel positions accordingly.
     */
    private val leftPadding: Float

    /**
     * When letter spacing, align and ellipsize applied to text, the ellipsized line is indented
     * wrong. For example for an RTL text, the last line is indented in a way where the beginning
     * of the line is more than layout width and the text is cut at the beginning.
     *
     * This attribute is used to fix the line right pixel positions accordingly.
     */
    private val rightPadding: Float

    /**
     * When true the wrapped layout that was created is a BoringLayout.
     */
    private val isBoringLayout: Boolean

    /**
     * When the last line of the text is empty, ParagraphStyle's are not applied. This becomes
     * visible during edit operations when the text field is empty or user inputs an new line
     * character. This layout contains the text layout that would be applied if the last line
     * was not empty.
     */
    private val lastLineFontMetrics: FontMetricsInt?

    /**
     * Holds the difference in line height for the lastLineFontMetrics and the wrapped text layout.
     */
    private val lastLineExtra: Int

    private val lineHeightSpans: Array<LineHeightStyleSpan>

    private val rect: Rect = Rect()

    init {
        val end = charSequence.length
        val frameworkTextDir = getTextDirectionHeuristic(textDirectionHeuristic)
        val frameworkAlignment = TextAlignmentAdapter.get(alignment)

        // BoringLayout won't adjust line height for baselineShift,
        // use StaticLayout for those spans.
        val hasBaselineShiftSpans = if (charSequence is Spanned) {
            // nextSpanTransition returns limit if there isn't any span.
            charSequence.nextSpanTransition(-1, end, BaselineShiftSpan::class.java) < end
        } else {
            false
        }

        Trace.beginSection("TextLayout:initLayout")
        try {
            val boringMetrics = layoutIntrinsics.boringMetrics

            val widthInt = ceil(width).toInt()
            layout = if (boringMetrics != null && layoutIntrinsics.maxIntrinsicWidth <= width &&
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
                    ellipsizedWidth = widthInt
                )
            } else {
                isBoringLayout = false
                StaticLayoutFactory.create(
                    text = charSequence,
                    start = 0,
                    end = charSequence.length,
                    paint = textPaint,
                    width = widthInt,
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
                    rightIndents = rightIndents
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
                  1. if ellipsis is applied, ellipsisCount of lastLine is greater than 0.
                  2. if ellipsis is not applies, lineEnd of the last line is unequals to
                  charSequence.length.
                  On certain cases, even though ellipsize is set, text overflow might still be
                  handled by truncating.
                  So we have to check both cases, no matter what ellipsis parameter is passed.
                 */
                layout.getEllipsisCount(lineCount - 1) > 0 ||
                    layout.getLineEnd(lineCount - 1) != charSequence.length
            }

        val verticalPaddings = getVerticalPaddings()

        lineHeightSpans = getLineHeightSpans()
        val lineHeightPaddings = getLineHeightPaddings(lineHeightSpans)
        topPadding = max(verticalPaddings.topPadding, lineHeightPaddings.topPadding)
        bottomPadding = max(verticalPaddings.bottomPadding, lineHeightPaddings.bottomPadding)

        val lastLineMetricsPair = getLastLineMetrics(textPaint, frameworkTextDir, lineHeightSpans)
        lastLineFontMetrics = lastLineMetricsPair.first
        lastLineExtra = lastLineMetricsPair.second

        leftPadding = layout.getEllipsizedLeftPadding(lineCount - 1)
        rightPadding = layout.getEllipsizedRightPadding(lineCount - 1)
    }

    private val layoutHelper by lazy(LazyThreadSafetyMode.NONE) { LayoutHelper(layout) }

    val text: CharSequence
        get() = layout.text

    val height: Int
        get() = if (didExceedMaxLines) {
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

    fun getLineLeft(lineIndex: Int): Float = layout.getLineLeft(lineIndex) +
        if (lineIndex == lineCount - 1) leftPadding else 0f

    /**
     * Return the horizontal leftmost position of the line in pixels.
     */
    fun getLineRight(lineIndex: Int): Float = layout.getLineRight(lineIndex) +
        if (lineIndex == lineCount - 1) rightPadding else 0f

    /**
     * Return the vertical position of the top of the line in pixels. If the line is equal to the
     * line count, returns the bottom of the last line.
     */
    fun getLineTop(line: Int): Float {
        val top = layout.getLineTop(line).toFloat()
        return top + if (line == 0) 0 else topPadding
    }

    /**
     * Return the vertical position of the bottom of the line in pixels.
     */
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

    /**
     * Return the vertical position of the baseline of the line in pixels.
     */
    fun getLineBaseline(line: Int): Float {
        return topPadding + if (line == lineCount - 1 && lastLineFontMetrics != null) {
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

    /**
     * Return the width of the line in pixels.
     */
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
        if (layout.getEllipsisStart(lineIndex) == 0) { // no ellipsis
            layout.getLineEnd(lineIndex)
        } else {
            // Layout#getLineEnd usually gets the end of text for the last line even if ellipsis
            // happens. However, if LF character is included in the ellipsized region, getLineEnd
            // returns LF character offset. So, use end of text for line end here.
            layout.text.length
        }

    /**
     * Return the text offset after the last visible character on the specified line. For example
     * whitespaces are not counted as visible characters.
     */
    fun getLineVisibleEnd(lineIndex: Int): Int =
        if (layout.getEllipsisStart(lineIndex) == 0) { // no ellipsis
            layout.getLineVisibleEnd(lineIndex)
        } else {
            layout.getLineStart(lineIndex) + layout.getEllipsisStart(lineIndex)
        }

    fun isLineEllipsized(lineIndex: Int) = layout.isLineEllipsized(lineIndex)

    fun getLineEllipsisOffset(lineIndex: Int): Int = layout.getEllipsisStart(lineIndex)

    fun getLineEllipsisCount(lineIndex: Int): Int = layout.getEllipsisCount(lineIndex)

    fun getLineForVertical(vertical: Int): Int = layout.getLineForVertical(vertical - topPadding)

    fun getOffsetForHorizontal(line: Int, horizontal: Float): Int {
        return layout.getOffsetForHorizontal(line, horizontal + -1 * getHorizontalPadding(line))
    }

    /**
     * Returns horizontal position for an offset from the drawing origin of a new character would
     * be inserted at that offset.
     *
     * *primary* means that the inserting character's direction will be resolved to the
     * *same* direction to the paragraph direction. For example, the insertion position for an LTR
     * character in an LTR paragraph or RTL character in an RTL paragraph.
     *
     * The location that is being queried can also be different based on line breaks. Consider the
     * following example:
     *
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
            upstream = upstream
        ) + getHorizontalPadding(getLineForOffset(offset))
    }

    /**
     * Returns horizontal position for an offset from the drawing origin of a new character would
     * be inserted at that offset.
     *
     * *secondary* means that the inserting character's direction will be resolved to the
     * *opposite* direction to the paragraph direction. For example, the insertion position for an
     * RTL character in an LTR paragraph or LTR character in an RTL paragraph.
     *
     * The location that is being queried can also be different based on line breaks. Consider the
     * following example:
     *
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
            upstream = upstream
        ) + getHorizontalPadding(getLineForOffset(offset))
    }
    fun getLineForOffset(offset: Int): Int = layout.getLineForOffset(offset)

    fun isRtlCharAt(offset: Int): Boolean = layout.isRtlCharAt(offset)

    fun getParagraphDirection(line: Int): Int = layout.getParagraphDirection(line)

    fun getSelectionPath(start: Int, end: Int, dest: Path) {
        layout.getSelectionPath(start, end, dest)
        if (topPadding != 0 && !dest.isEmpty) {
            dest.offset(0f /* dx */, topPadding.toFloat() /* dy */)
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
     * each index in that segment represents left, top, right, bottom of the character.
     * @param arrayStart the inclusive start index in the array where the function will start
     * filling in the values from
     */
    fun fillBoundingBoxes(
        startOffset: Int,
        endOffset: Int,
        array: FloatArray,
        arrayStart: Int
    ) {
        val textLength = text.length
        require(startOffset >= 0) { "startOffset must be > 0" }
        require(startOffset < textLength) { "startOffset must be less than text length" }
        require(endOffset > startOffset) { "endOffset must be greater than startOffset" }
        require(endOffset <= textLength) { "endOffset must be smaller or equal to text length" }

        val range = endOffset - startOffset
        val minArraySize = range * 4

        require((array.size - arrayStart) >= minArraySize) {
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

    /**
     * Returns the bounding box as Rect of the character for given character offset.
     */
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

        with(SharedTextAndroidCanvas) {
            setCanvas(canvas)
            layout.draw(this)
        }

        if (topPadding != 0) {
            canvas.translate(0f, -1 * topPadding.toFloat())
        }
    }

    internal fun isFallbackLinespacingApplied(): Boolean {
        return if (isBoringLayout) {
            BoringLayoutFactory.isFallbackLineSpacingEnabled(layout as BoringLayout)
        } else {
            StaticLayoutFactory.isFallbackLineSpacingEnabled(
                layout as StaticLayout,
                fallbackLineSpacing
            )
        }
    }
}

/**
 * This class is intended to be used *only* by [TextLayout.fillBoundingBoxes]. It is tightly coupled
 * to the code in callee. Do not use.
 *
 * Assumes that downstream calls always called with offset followed by offset+1 in upstream
 * case. Therefore it does not add the downstream calls to the result to the cache but check if
 * it already exists in the cache for early return.
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
     * Returns the primary/secondary horizontal position for upstream or downstream.
     * Very tightly coupled to how get is called from the [TextLayout.fillBoundingBoxes] function.
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
    private fun get(
        offset: Int,
        upstream: Boolean,
        cache: Boolean,
        primary: Boolean
    ): Float {
        // even if upstream is requested, if the character is not on a line start/end upstream
        // and downstream results will be the same
        val upstreamFinal = if (upstream) {
            val lineNo = layout.layout.getLineForOffset(offset, upstream)
            val lineStart = layout.getLineStart(lineNo)
            val lineEnd = layout.getLineEnd(lineNo)
            offset == lineStart || offset == lineEnd
        } else {
            false
        }

        // key for the current request
        val tmpKey = (offset) * 4 + if (primary) {
            if (upstreamFinal) 0 else 1
        } else {
            if (upstreamFinal) 2 else 3
        }

        if (cachedKey == tmpKey) return cachedValue

        val result = if (primary) {
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
internal fun getTextDirectionHeuristic(@TextDirection textDirectionHeuristic: Int):
    TextDirectionHeuristic {
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

internal fun VerticalPaddings(
    topPadding: Int,
    bottomPadding: Int
) = VerticalPaddings(packInts(topPadding, bottomPadding))

@kotlin.jvm.JvmInline
internal value class VerticalPaddings internal constructor(internal val packedValue: Long) {

  val topPadding: Int
      get() = unpackInt1(packedValue)

  val bottomPadding: Int
      get() = unpackInt2(packedValue)
}

@OptIn(InternalPlatformTextApi::class)
private fun TextLayout.getVerticalPaddings(): VerticalPaddings {
    if (includePadding || isFallbackLinespacingApplied()) return ZeroVerticalPadding

    val paint = layout.paint
    val text = layout.text

    val firstLineTextBounds = paint.getCharSequenceBounds(
        text,
        layout.getLineStart(0),
        layout.getLineEnd(0)
    )
    val ascent = layout.getLineAscent(0)

    // when textBounds.top is "higher" than ascent, we need to add the difference into account
    // since includeFontPadding is false, ascent is at the top of Layout
    val topPadding = if (firstLineTextBounds.top < ascent) {
        ascent - firstLineTextBounds.top
    } else {
        layout.topPadding
    }

    val lastLineTextBounds = if (lineCount == 1) {
        // reuse the existing rect since there is single line
        firstLineTextBounds
    } else {
        val line = lineCount - 1
        paint.getCharSequenceBounds(text, layout.getLineStart(line), layout.getLineEnd(line))
    }
    val descent = layout.getLineDescent(lineCount - 1)

    // when textBounds.bottom is "lower" than descent, we need to add the difference into account
    // since includeFontPadding is false, descent is at the bottom of Layout
    val bottomPadding = if (lastLineTextBounds.bottom > descent) {
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
private fun TextLayout.getLineHeightPaddings(
    lineHeightSpans: Array<LineHeightStyleSpan>
): VerticalPaddings {
    var firstAscentDiff = 0
    var lastDescentDiff = 0

    for (span in lineHeightSpans) {
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
    lineHeightSpans: Array<LineHeightStyleSpan>
): Pair<FontMetricsInt?, Int> {
    val lastLine = lineCount - 1
    // did not check for "\n" since the last line might include zero width characters
    if (layout.getLineStart(lastLine) == layout.getLineEnd(lastLine) &&
        lineHeightSpans.isNotEmpty()
    ) {
        val emptyText = SpannableString("\u200B")
        val lineHeightSpan = lineHeightSpans.first()
        val newLineHeightSpan = lineHeightSpan.copy(
            startIndex = 0,
            endIndex = emptyText.length,
            trimFirstLineTop = if (lastLine != 0 && lineHeightSpan.trimLastLineBottom) {
                false
            } else {
                lineHeightSpan.trimLastLineBottom
            }
        )

        emptyText.setSpan(
            newLineHeightSpan,
            0,
            emptyText.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        val tmpLayout = StaticLayoutFactory.create(
            text = emptyText,
            start = 0,
            end = emptyText.length,
            width = Int.MAX_VALUE,
            paint = textPaint,
            textDir = frameworkTextDir,
            includePadding = includePadding,
            useFallbackLineSpacing = fallbackLineSpacing
        )

        val lastLineFontMetrics = FontMetricsInt().apply {
            ascent = tmpLayout.getLineAscent(0)
            descent = tmpLayout.getLineDescent(0)
            top = tmpLayout.getLineTop(0)
            bottom = tmpLayout.getLineBottom(0)
        }

        val lastLineExtra = lastLineFontMetrics.bottom - getLineHeight(lastLine).toInt()
        return Pair(lastLineFontMetrics, lastLineExtra)
    }
    return Pair(null, 0)
}

@OptIn(InternalPlatformTextApi::class)
private fun TextLayout.getLineHeightSpans(): Array<LineHeightStyleSpan> {
    if (text !is Spanned) return emptyArray()
    val lineHeightStyleSpans = (text as Spanned).getSpans(
        0, text.length, LineHeightStyleSpan::class.java
    )
    if (lineHeightStyleSpans.isEmpty()) return emptyArray()
    return lineHeightStyleSpans
}

internal fun Layout.isLineEllipsized(lineIndex: Int) = this.getEllipsisCount(lineIndex) > 0
