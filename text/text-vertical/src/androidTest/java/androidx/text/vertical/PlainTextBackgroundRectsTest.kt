/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.text.vertical

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.ReplacementSpan
import android.text.style.SubscriptSpan
import android.text.style.SuperscriptSpan
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.text.vertical.RubyPixel.ANTIALIAS_MARGIN
import androidx.text.vertical.RubyPixel.VISIBLE_CHAR
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class PlainTextBackgroundRectsTest {
    private val paint = TextPaint().apply { textSize = TEXT_SIZE }
    private val bitmaps = mutableListOf<Bitmap>()

    @After
    fun recycleBitmaps() {
        bitmaps.forEach { it.recycle() }
    }

    @Test
    fun emptyText_returnsNoBox() {
        val text = SpannableString("")

        val rects = plainTextBackgroundRects(layoutOf(text), text, paint)

        assertThat(rects).isEmpty()
    }

    @Test
    fun plainText_returnsOneBoxFromAscentToDescent() {
        val text = SpannableString("abc")
        val layout = layoutOf(text)

        val rects = plainTextBackgroundRects(layout, text, paint)

        assertThat(rects).hasSize(1)
        assertBox(rects[0], layout, line = 0, start = 0, end = 3, paint.fontMetricsInt)
    }

    @Test
    fun newline_isNotInBox_andBlankLineGetsNoBox() {
        // The lines are "ab\n", "\n", and "cd".
        val text = SpannableString("ab\n\ncd")
        val layout = layoutOf(text)

        val rects = plainTextBackgroundRects(layout, text, paint)

        assertThat(rects).hasSize(2)
        assertBox(rects[0], layout, line = 0, start = 0, end = 2, paint.fontMetricsInt)
        assertBox(rects[1], layout, line = 2, start = 4, end = 6, paint.fontMetricsInt)
    }

    @Test
    fun trailingNewline_producesNoBoxForTrailingEmptyLine() {
        val text = SpannableString("ab\n")
        val layout = layoutOf(text)

        val rects = plainTextBackgroundRects(layout, text, paint)

        assertThat(rects).hasSize(1)
        assertBox(rects[0], layout, line = 0, start = 0, end = 2, paint.fontMetricsInt)
    }

    @Test
    fun wrappedLine_excludesTrailingSpaces() {
        val text = SpannableString("ab   cd")
        // Make the layout wide enough for "ab" on line 0, wrapping "cd" to line 1.
        val width = paint.measureText("ab ").toInt()
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width).build()

        val rects = plainTextBackgroundRects(layout, text, paint)

        assertThat(rects).hasSize(2)
        assertBox(rects[0], layout, line = 0, start = 0, end = 2, paint.fontMetricsInt)
        assertBox(rects[1], layout, line = 1, start = 5, end = 7, paint.fontMetricsInt)
    }

    @Test
    fun wrappedCjkLine_withoutSpaces_returnsOneBoxPerLine() {
        val text = SpannableString("あいうえお")
        // Make the layout wide enough for two characters per line.
        val width = paint.measureText("あい").toInt()
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width).build()

        val rects = plainTextBackgroundRects(layout, text, paint)

        assertThat(rects).hasSize(layout.lineCount)
        for (line in 0 until layout.lineCount) {
            val start = layout.getLineStart(line)
            val end = layout.getLineEnd(line)
            assertBox(
                rects[line],
                layout,
                line = line,
                start = start,
                end = end,
                paint.fontMetricsInt,
            )
        }
    }

    @Test
    fun replacementSpanRun_getsNoBox() {
        val text = SpannableString("abcde")
        text.setSpan(
            FixedWidthReplacementSpan(REPLACEMENT_WIDTH),
            1,
            3,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        val layout = layoutOf(text)

        val rects = plainTextBackgroundRects(layout, text, paint)

        assertThat(rects).hasSize(2)
        assertBox(rects[0], layout, line = 0, start = 0, end = 1, paint.fontMetricsInt)
        assertBox(rects[1], layout, line = 0, start = 3, end = 5, paint.fontMetricsInt)
    }

    @Test
    fun replacementSpanAtLineBoundaries_getsNoBox() {
        val text = SpannableString("abcde")
        text.setSpan(
            FixedWidthReplacementSpan(REPLACEMENT_WIDTH),
            0,
            1,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        text.setSpan(
            FixedWidthReplacementSpan(REPLACEMENT_WIDTH),
            4,
            5,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        val layout = layoutOf(text)

        val rects = plainTextBackgroundRects(layout, text, paint)

        assertThat(rects).hasSize(1)
        assertBox(rects[0], layout, line = 0, start = 1, end = 4, paint.fontMetricsInt)
    }

    @Test
    fun relativeSizeSpan_changesBoxHeightOfItsRun() {
        val text = SpannableString("abcd")
        text.setSpan(RelativeSizeSpan(2f), 2, 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val layout = layoutOf(text)

        val rects = plainTextBackgroundRects(layout, text, paint)

        val largePaint = TextPaint(paint).apply { textSize = 2 * TEXT_SIZE }
        assertThat(rects).hasSize(2)
        assertBox(rects[0], layout, line = 0, start = 0, end = 2, paint.fontMetricsInt)
        assertBox(rects[1], layout, line = 0, start = 2, end = 4, largePaint.fontMetricsInt)
    }

    @Test
    fun rubySpanBefore_plainBoxBottomMeetsSpanBottom() =
        assertPlainBoxesMeetSpan(AnnotationPosition.Before) { RubySpan("るび", position = it) }

    @Test
    fun rubySpanAfter_plainBoxTopMeetsSpanTop() =
        assertPlainBoxesMeetSpan(AnnotationPosition.After) { RubySpan("るび", position = it) }

    @Test
    fun emphasisSpanBefore_plainBoxBottomMeetsSpanBottom() =
        assertPlainBoxesMeetSpan(AnnotationPosition.Before) { EmphasisSpan(position = it) }

    @Test
    fun emphasisSpanAfter_plainBoxTopMeetsSpanTop() =
        assertPlainBoxesMeetSpan(AnnotationPosition.After) { EmphasisSpan(position = it) }

    @Test
    fun rtlRun_returnsBoxWithLeftLessThanRight() {
        val text = SpannableString("שלום")
        val layout =
            StaticLayout.Builder.obtain(text, 0, text.length, paint, LAYOUT_WIDTH)
                .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL)
                .build()

        val rects = plainTextBackgroundRects(layout, text, paint)

        assertThat(rects).hasSize(1)
        assertThat(rects[0].left).isLessThan(rects[0].right)
        assertThat(rects[0].left).isWithin(0.5f).of(layout.getLineLeft(0))
        assertThat(rects[0].right).isWithin(0.5f).of(layout.getLineRight(0))
    }

    @Test
    fun mixedBidiRun_returnsOneBoxPerDirectionRun() {
        val text = SpannableString("abcשלוםdef")
        val layout =
            StaticLayout.Builder.obtain(text, 0, text.length, paint, LAYOUT_WIDTH)
                .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR)
                .build()

        val rects = plainTextBackgroundRects(layout, text, paint)

        assertThat(rects).hasSize(3)
        for (rect in rects) {
            assertThat(rect.left).isLessThan(rect.right)
        }
        val sorted = rects.sortedBy { it.left }
        assertThat(sorted[0].left).isWithin(0.5f).of(layout.getLineLeft(0))
        assertThat(sorted[0].right).isWithin(0.5f).of(sorted[1].left)
        assertThat(sorted[1].right).isWithin(0.5f).of(sorted[2].left)
        assertThat(sorted[2].right).isWithin(0.5f).of(layout.getLineRight(0))
    }

    @Test
    fun rtlParagraphWithLtrDigits_boxesMatchSelectionPathBounds() {
        val text = SpannableString("שלום 123 עולם")
        val layout =
            StaticLayout.Builder.obtain(text, 0, text.length, paint, LAYOUT_WIDTH)
                .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL)
                .build()

        val rects = plainTextBackgroundRects(layout, text, paint)

        assertThat(rects).hasSize(3)
        for (rect in rects) {
            assertThat(rect.left).isLessThan(rect.right)
        }
        val sorted = rects.sortedBy { it.left }
        assertThat(sorted[0].left).isWithin(0.5f).of(layout.getLineLeft(0))
        assertThat(sorted[0].right).isWithin(0.5f).of(sorted[1].left)
        assertThat(sorted[1].right).isWithin(0.5f).of(sorted[2].left)
        assertThat(sorted[2].right).isWithin(0.5f).of(layout.getLineRight(0))
        assertThat(sorted[1].width()).isWithin(1f).of(paint.measureText("123"))
    }

    @Test
    fun softWrappedLineWithLeadingReplacementSpan_doesNotIncludeMarginOrNextLineInBox() {
        // Line 0 has a ReplacementSpan at [0, 1) followed by RTL "אב" at [1, 3), soft-wrapping
        // CJK "あい" at [3, 5) to line 1 without spaces so getLineEnd(0) == getLineVisibleEnd(0).
        // Explicit LTR direction is used because MeasuredParagraph replaces ReplacementSpan ranges
        // with U+FFFC (BiDi neutral) before FIRSTSTRONG_LTR analysis.
        val text = SpannableString("Xאבあい")
        text.setSpan(
            FixedWidthReplacementSpan(REPLACEMENT_WIDTH),
            0,
            1,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        val width =
            (REPLACEMENT_WIDTH + paint.measureText("אב") + paint.measureText("あ") / 2f).toInt()
        val layout =
            StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                .setTextDirection(TextDirectionHeuristics.LTR)
                .build()

        val rects = plainTextBackgroundRects(layout, text, paint)

        assertThat(layout.lineCount).isEqualTo(2)
        assertThat(layout.getLineEnd(0)).isEqualTo(3)
        assertThat(rects).hasSize(2)
        // Line 0 plain box must cover only [1, 3) ("אב"), starting after REPLACEMENT_WIDTH and
        // ending at getLineRight(0), without including x=0 from line 1 or x=width from line 0.
        assertThat(rects[0].left).isWithin(0.5f).of(REPLACEMENT_WIDTH.toFloat())
        assertThat(rects[0].right).isWithin(0.5f).of(layout.getLineRight(0))
        assertThat(rects[0].right).isLessThan(width.toFloat())
    }

    @Test
    fun softWrappedLineEndingWithRtlRun_doesNotExtendBoxToLineRight() {
        // In an LTR paragraph, line 0 contains LTR "Hi " [0, 3), styled RTL "אב" [3, 5), and
        // plain RTL "גד" [5, 7), soft-wrapping CJK "あいう" [7, 10) to line 1. Visually on line 0:
        // "Hi " is leftmost, "גד" [5, 7) is in the middle, and "אב" [3, 5) is rightmost at
        // getLineRight(0). Even though [5, 7) ends at getLineEnd(0), its box must not extend to
        // getLineRight(0).
        val text = SpannableString("Hi אבגדあいう")
        text.setSpan(RelativeSizeSpan(1f), 3, 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val width = (paint.measureText("Hi אבגד") + paint.measureText("あ") / 2f).toInt()
        val layout =
            StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR)
                .build()

        val rects = plainTextBackgroundRects(layout, text, paint)

        assertThat(layout.lineCount).isAtLeast(2)
        assertThat(layout.getLineEnd(0)).isEqualTo(7)
        // Line 0 produces 3 boxes in logical order: [0, 3) "Hi ", [3, 5) "אב", [5, 7) "גד".
        assertThat(rects.size).isAtLeast(3)
        val hiBox = rects[0]
        val alefBetBox = rects[1]
        val gimelDaletBox = rects[2]
        assertThat(hiBox.left).isWithin(0.5f).of(layout.getLineLeft(0))
        assertThat(hiBox.right).isWithin(0.5f).of(gimelDaletBox.left)
        assertThat(gimelDaletBox.right).isWithin(0.5f).of(alefBetBox.left)
        assertThat(alefBetBox.right).isWithin(0.5f).of(layout.getLineRight(0))
        assertThat(gimelDaletBox.right).isLessThan(layout.getLineRight(0) - 1f)
    }

    @Test
    fun zeroLengthReplacementSpan_doesNotSuppressPlainTextBoxes() {
        val text = SpannableString("abc")
        text.setSpan(
            FixedWidthReplacementSpan(REPLACEMENT_WIDTH),
            1,
            1,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        val layout = layoutOf(text)

        val rects = plainTextBackgroundRects(layout, text, paint)

        assertThat(rects).hasSize(2)
        assertBox(rects[0], layout, line = 0, start = 0, end = 1, paint.fontMetricsInt)
        assertBox(rects[1], layout, line = 0, start = 1, end = 3, paint.fontMetricsInt)
    }

    @Test
    fun zeroLengthMetricAffectingSpan_doesNotChangePlainTextBoxHeight() {
        val text = SpannableString("abc")
        text.setSpan(RelativeSizeSpan(2f), 1, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val layout = layoutOf(text)

        val rects = plainTextBackgroundRects(layout, text, paint)

        assertThat(rects).hasSize(2)
        assertBox(rects[0], layout, line = 0, start = 0, end = 1, paint.fontMetricsInt)
        assertBox(rects[1], layout, line = 0, start = 1, end = 3, paint.fontMetricsInt)
    }

    @Test
    fun superscriptAndSubscriptSpans_expandBoxVerticalBounds() {
        val text = SpannableString("abcd")
        text.setSpan(SuperscriptSpan(), 0, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.setSpan(SubscriptSpan(), 2, 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val layout = layoutOf(text)

        val rects = plainTextBackgroundRects(layout, text, paint)

        val superMetrics =
            Paint.FontMetricsInt().also { fm ->
                val workPaint = TextPaint(paint).apply { baselineShift = 0 }
                SuperscriptSpan().updateMeasureState(workPaint)
                workPaint.getFontMetricsInt(fm)
                fm.ascent += workPaint.baselineShift
            }
        val subMetrics =
            Paint.FontMetricsInt().also { fm ->
                val workPaint = TextPaint(paint).apply { baselineShift = 0 }
                SubscriptSpan().updateMeasureState(workPaint)
                workPaint.getFontMetricsInt(fm)
                fm.descent += workPaint.baselineShift
            }
        assertThat(rects).hasSize(2)
        assertBox(rects[0], layout, line = 0, start = 0, end = 2, superMetrics)
        assertBox(rects[1], layout, line = 0, start = 2, end = 4, subMetrics)
    }

    @Test
    fun ltrParagraphWithRtlAndArabicIndicDigits_splitsSameParityBidiRuns() {
        // In an LTR paragraph, "Price " [0, 6) has BiDi level 0, "١٢٣" [6, 9) has level 2, and
        // " شيكل" [9, 14) has level 1. Visually " شيكل" sits between "Price " and "١٢٣", so the
        // two even-level runs must not be merged into one box that bridges across " شيكل".
        val text = SpannableString("Price ١٢٣ شيكل")
        val layout =
            StaticLayout.Builder.obtain(text, 0, text.length, paint, LAYOUT_WIDTH)
                .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR)
                .build()

        val rects = plainTextBackgroundRects(layout, text, paint)

        assertThat(rects).hasSize(3)
        val sorted = rects.sortedBy { it.left }
        assertThat(sorted[0].left).isWithin(0.5f).of(layout.getLineLeft(0))
        assertThat(sorted[0].right).isWithin(0.5f).of(sorted[1].left)
        assertThat(sorted[1].right).isWithin(0.5f).of(sorted[2].left)
        assertThat(sorted[2].right).isWithin(0.5f).of(layout.getLineRight(0))
    }

    @Test
    fun sameParityBidiRunsSeparatedByRubySpan_doNotBridgeAcrossRubySpan() {
        // In an LTR paragraph, "ع" [0, 1) and the RubySpan on "رب" [1, 3) have BiDi level 1 (RTL),
        // "١٢٣" [3, 6) has level 2 (LTR), and "hello" [6, 11) has level 0 (LTR). Visually the
        // level-1 RubySpan sits between "١٢٣" and "hello", so [3, 6) and [6, 11) must be split
        // into two boxes that do not cross the RubySpan.
        val text = SpannableString("عرب١٢٣hello")
        text.setSpan(
            RubySpan("ر", position = AnnotationPosition.Before),
            1,
            3,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        val layout =
            StaticLayout.Builder.obtain(text, 0, text.length, paint, LAYOUT_WIDTH)
                .setTextDirection(TextDirectionHeuristics.LTR)
                .build()

        val rects = plainTextBackgroundRects(layout, text, paint)

        assertThat(rects).hasSize(3)
        val sorted = rects.sortedBy { it.left }
        for (i in 0 until sorted.size - 1) {
            assertThat(sorted[i].right).isAtMost(sorted[i + 1].left + 0.5f)
        }
        val totalPlainWidth =
            paint.measureText("ع") + paint.measureText("١٢٣") + paint.measureText("hello")
        assertThat(rects.sumOf { it.width().toDouble() }.toFloat()).isWithin(2f).of(totalPlainWidth)
    }

    @Test
    fun prepareHorizontalText_attachesNoTextBackgroundSpanWithPriority() {
        val input =
            SpannableString("Hello").apply {
                setSpan(
                    BackgroundColorSpan(Color.RED),
                    1,
                    4,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }

        val prepared = prepareHorizontalText(input)
        val noBgSpans = prepared.getSpans(0, prepared.length, NoTextBackgroundSpan::class.java)
        val bgSpans = prepared.getSpans(0, prepared.length, BackgroundColorSpan::class.java)

        assertThat(noBgSpans).hasLength(1)
        val noBgSpan = noBgSpans[0]
        assertThat(prepared.getSpanStart(noBgSpan)).isEqualTo(0)
        assertThat(prepared.getSpanEnd(noBgSpan)).isEqualTo(input.length)
        assertThat(prepared.getSpanFlags(noBgSpan) and Spanned.SPAN_PRIORITY)
            .isEqualTo(1 shl Spanned.SPAN_PRIORITY_SHIFT)
        assertThat(bgSpans).hasLength(1)
        assertThat(prepared.getSpanStart(bgSpans[0])).isEqualTo(1)
        assertThat(prepared.getSpanEnd(bgSpans[0])).isEqualTo(4)
    }

    @Test
    fun bitmapRendering_fillsPlainTextInlineBackgroundAndRubyWithoutAnnotationBleed() {
        // Character 0 is plain text (style background), character 1 carries an inline
        // BackgroundColorSpan, and character 2 carries a RubySpan that expands lineTop above
        // the plain text font ascent.
        val source =
            SpannableString(VISIBLE_CHAR.repeat(3)).apply {
                setSpan(
                    ForegroundColorSpan(Color.TRANSPARENT),
                    0,
                    3,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
                setSpan(BackgroundColorSpan(Color.CYAN), 1, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(
                    RubySpan(
                        SpannableString(VISIBLE_CHAR).apply {
                            setSpan(
                                ForegroundColorSpan(Color.TRANSPARENT),
                                0,
                                length,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                            )
                        }
                    ),
                    2,
                    3,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
        val prepared = prepareHorizontalText(source)
        val renderPaint = RubyPixel.newPaint().apply { color = Color.TRANSPARENT }
        val layout =
            StaticLayout.Builder.obtain(prepared, 0, prepared.length, renderPaint, BITMAP_WIDTH)
                .build()
        renderPaint.bgColor = Color.YELLOW
        val rects = plainTextBackgroundRects(layout, prepared, renderPaint)
        val bitmap =
            Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Bitmap.Config.ARGB_8888).also {
                bitmaps += it
            }
        val fillPaint = Paint().apply { color = Color.YELLOW }
        Canvas(bitmap).apply {
            drawColor(Color.WHITE)
            rects.forEach { drawRect(it, fillPaint) }
            layout.draw(this)
        }

        val plainRect = rects[0]
        val plainCenterX =
            ((layout.getPrimaryHorizontal(0) + layout.getPrimaryHorizontal(1)) / 2f).toInt()
        val plainBodyY = ((plainRect.top + plainRect.bottom) / 2f).toInt()
        val plainAnnotationBandY = layout.getLineTop(0) + ANTIALIAS_MARGIN
        val inlineBgCenterX =
            ((layout.getPrimaryHorizontal(1) + layout.getPrimaryHorizontal(2)) / 2f).toInt()
        val rubyCenterX = ((layout.getPrimaryHorizontal(2) + layout.getLineRight(0)) / 2f).toInt()

        // Plain text body is filled by plainTextBackgroundRects, while the annotation band above
        // it stays white because NoTextBackgroundSpan cleared TextPaint.bgColor on plain runs.
        assertThat(bitmap.getPixel(plainCenterX, plainBodyY)).isEqualTo(Color.YELLOW)
        assertThat(bitmap.getPixel(plainCenterX, plainAnnotationBandY)).isEqualTo(Color.WHITE)
        // Inline BackgroundColorSpan overrides NoTextBackgroundSpan.
        assertThat(bitmap.getPixel(inlineBgCenterX, plainBodyY)).isEqualTo(Color.CYAN)
        // RubySpan still receives renderPaint.bgColor in both its body and annotation bands.
        assertThat(bitmap.getPixel(rubyCenterX, plainBodyY)).isEqualTo(Color.YELLOW)
        assertThat(bitmap.getPixel(rubyCenterX, plainAnnotationBandY)).isEqualTo(Color.YELLOW)
    }

    private fun layoutOf(text: Spanned): Layout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, LAYOUT_WIDTH).build()

    /**
     * Asserts that [rect] covers the text from [start] to [end] on [line].
     *
     * The top and the bottom of [rect] are at the ascent and the descent of [metrics] from the
     * baseline of [line].
     */
    private fun assertBox(
        rect: RectF,
        layout: Layout,
        line: Int,
        start: Int,
        end: Int,
        metrics: Paint.FontMetricsInt,
    ) {
        val baseline = layout.getLineBaseline(line).toFloat()
        val expectedRight =
            if (end < layout.getLineEnd(line)) {
                layout.getPrimaryHorizontal(end)
            } else {
                layout.getLineRight(line)
            }
        assertThat(rect.left).isEqualTo(layout.getPrimaryHorizontal(start))
        assertThat(rect.right).isEqualTo(expectedRight)
        assertThat(rect.top).isEqualTo(baseline + metrics.ascent)
        assertThat(rect.bottom).isEqualTo(baseline + metrics.descent)
    }

    /**
     * Asserts that the plain text boxes next to a span meet the span box on the side without the
     * annotation.
     *
     * @param position the position of the annotation.
     * @param createSpan creates the span for [position].
     */
    private fun assertPlainBoxesMeetSpan(
        position: AnnotationPosition,
        createSpan: (AnnotationPosition) -> ReplacementSpan,
    ) {
        // Most devices draw CJK text with a fallback font. The fallback font has other metrics.
        val text = SpannableString("あ上あ")
        val span = createSpan(position)
        text.setSpan(span, 1, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val layout = layoutOf(text)

        val rects = plainTextBackgroundRects(layout, text, paint)

        val spanBox = Paint.FontMetricsInt()
        span.getSize(paint, text, 1, 2, spanBox)
        val baseline = layout.getLineBaseline(0).toFloat()
        assertThat(rects).hasSize(2)
        for (rect in rects) {
            if (position == AnnotationPosition.After) {
                assertThat(rect.top).isEqualTo(baseline + spanBox.ascent)
            } else {
                assertThat(rect.bottom).isEqualTo(baseline + spanBox.descent)
            }
        }
    }

    private companion object {
        const val TEXT_SIZE = 40f
        const val LAYOUT_WIDTH = 10_000
        const val REPLACEMENT_WIDTH = 100
        const val BITMAP_WIDTH = 360
        const val BITMAP_HEIGHT = 220
    }
}

/** Replaces its text with an empty space that is [width] pixels wide. */
private class FixedWidthReplacementSpan(private val width: Int) : ReplacementSpan() {
    override fun getSize(
        paint: Paint,
        text: CharSequence?,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?,
    ): Int = width

    override fun draw(
        canvas: Canvas,
        text: CharSequence?,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint,
    ) {}
}
