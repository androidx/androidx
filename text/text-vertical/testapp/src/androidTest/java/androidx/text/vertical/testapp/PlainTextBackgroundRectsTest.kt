/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.text.vertical.testapp

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.RelativeSizeSpan
import android.text.style.ReplacementSpan
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.text.vertical.AnnotationPosition
import androidx.text.vertical.EmphasisSpan
import androidx.text.vertical.RubySpan
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class PlainTextBackgroundRectsTest {
    private val paint = TextPaint().apply { textSize = TEXT_SIZE }

    @Test
    fun emptyText_returnsNoBox() {
        val text = SpannableString("")

        val rects = plainTextBackgroundRects(layoutOf(text), text, paint)

        assertThat(rects).isEmpty()
    }

    @Test
    fun plainText_returnsOneBoxFromFontTopToFontBottom() {
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

    private fun layoutOf(text: Spanned): Layout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, LAYOUT_WIDTH).build()

    /**
     * Asserts that [rect] covers the text from [start] to [end] on [line].
     *
     * The top and the bottom of [rect] are at the top and the bottom of [metrics] from the baseline
     * of [line].
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
        assertThat(rect.top).isEqualTo(baseline + metrics.top)
        assertThat(rect.bottom).isEqualTo(baseline + metrics.bottom)
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
