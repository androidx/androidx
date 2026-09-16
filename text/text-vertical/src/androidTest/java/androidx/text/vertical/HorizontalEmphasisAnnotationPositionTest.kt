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

import android.graphics.Canvas
import android.graphics.Paint
import android.text.SpannableString
import android.text.Spanned
import android.text.SpannedString
import android.text.StaticLayout
import android.text.TextPaint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/** Text size of the base text, chosen so that 1em == 100px. */
private const val ONE_EM = 100f

/** The default filled [EmphasisStyle.Dot] maps to U+2022 BULLET. */
private const val MARK_LETTER = "\u2022"

private const val BASE_TEXT = "Hello"

/** The draw origin used by the draw tests. Mark offsets are read relative to it. */
private const val ORIGIN = 0f

/**
 * Verifies that [EmphasisSpan.position] is honored when drawing horizontal text.
 *
 * Per the TTML2 `tts:rubyPosition` semantics documented on [AnnotationPosition], in horizontal
 * writing mode [AnnotationPosition.Before] places the emphasis mark above the base text, and
 * [AnnotationPosition.After] places it below.
 *
 * Each test asserts one of two things.
 * - The font metrics the span reports. The expansion moves from the ascent to the descent, and the
 *   amount is the mark line height, so the expected values are exact on every API level.
 * - The baseline the mark is drawn on, relative to the base text baseline. A mark above the text
 *   draws at a negative offset and a mark below it draws at a positive one.
 *
 * Metrics and drawing are asserted separately on purpose. A change that moves the mark but leaves
 * the metric expansion on the old side makes the mark collide with the adjacent line, and a suite
 * that only checks the drawing cannot see it.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class HorizontalEmphasisAnnotationPositionTest {

    private val paint = TextPaint().apply { textSize = ONE_EM }
    private val text = SpannedString(BASE_TEXT)

    // ---------------------------------------------------------------------------------------
    // Font metrics
    // ---------------------------------------------------------------------------------------

    @Test
    fun positionBefore_expandsTheAscent() {
        val fm = metricsOf(AnnotationPosition.Before)
        val body = bodyMetrics()

        assertThat(fm.ascent).isEqualTo(body.ascent - markLineHeight())
        assertThat(fm.descent).isEqualTo(body.descent)
    }

    @Test
    fun positionAfter_expandsTheDescent() {
        val fm = metricsOf(AnnotationPosition.After)
        val body = bodyMetrics()

        assertThat(fm.ascent).isEqualTo(body.ascent)
        assertThat(fm.descent).isEqualTo(body.descent + markLineHeight())
    }

    /** The mark takes the same room on either side, so the line height must not change. */
    @Test
    fun lineHeight_isIdenticalForBeforeAndAfter() {
        val before = metricsOf(AnnotationPosition.Before)
        val after = metricsOf(AnnotationPosition.After)

        assertThat(after.descent - after.ascent).isEqualTo(before.descent - before.ascent)
    }

    @Test
    fun topAndBottom_trackAscentAndDescent() {
        val after = metricsOf(AnnotationPosition.After)

        assertThat(after.top).isEqualTo(after.ascent)
        assertThat(after.bottom).isEqualTo(after.descent)
    }

    /** An unrecognized position falls back to the [EmphasisSpan.DEFAULT_POSITION] placement. */
    @Test
    fun positionUnknown_isTreatedAsBefore() {
        val unknown = metricsOf(AnnotationPosition.Unknown)
        val before = metricsOf(AnnotationPosition.Before)

        assertThat(unknown.ascent).isEqualTo(before.ascent)
        assertThat(unknown.descent).isEqualTo(before.descent)
    }

    // ---------------------------------------------------------------------------------------
    // Drawing
    // ---------------------------------------------------------------------------------------

    @Test
    fun positionBefore_markIsDrawnAboveTheBaseline() {
        val marks = drawAndCollectMarks(AnnotationPosition.Before)

        assertThat(marks).isNotEmpty()
        marks.forEach { assertThat(it).isLessThan(ORIGIN) }
    }

    @Test
    fun positionAfter_markIsDrawnBelowTheBaseline() {
        val marks = drawAndCollectMarks(AnnotationPosition.After)

        assertThat(marks).isNotEmpty()
        marks.forEach { assertThat(it).isGreaterThan(ORIGIN) }
    }

    @Test
    fun position_doesNotChangeTheNumberOfMarks() {
        val before = drawAndCollectMarks(AnnotationPosition.Before)
        val after = drawAndCollectMarks(AnnotationPosition.After)

        assertThat(after).hasSize(before.size)
    }

    // ---------------------------------------------------------------------------------------
    // EmphasisSpan plumbing
    // ---------------------------------------------------------------------------------------

    /** [EmphasisSpan] has to forward its position, or the renderer never sees it. */
    @Test
    fun emphasisSpan_forwardsThePositionToTheHorizontalLayout() {
        val beforeFm = Paint.FontMetricsInt()
        spanOver(AnnotationPosition.Before).let {
            it.getSize(paint, spannedFor(it), 0, BASE_TEXT.length, beforeFm)
        }
        val afterFm = Paint.FontMetricsInt()
        spanOver(AnnotationPosition.After).let {
            it.getSize(paint, spannedFor(it), 0, BASE_TEXT.length, afterFm)
        }

        assertThat(beforeFm.ascent).isLessThan(afterFm.ascent)
        assertThat(afterFm.descent).isGreaterThan(beforeFm.descent)
    }

    // ---------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------

    private fun layoutOf(position: AnnotationPosition) =
        HorizontalEmphasisSpanLayout(
            text,
            0,
            BASE_TEXT.length,
            MARK_LETTER,
            position,
            paint,
            EmphasisSpan.DEFAULT_SCALE,
        )

    private fun metricsOf(position: AnnotationPosition): Paint.FontMetricsInt =
        Paint.FontMetricsInt().also { layoutOf(position).fillFontMetrics(it) }

    /** Body metrics, measured independently of the layout under test. */
    private fun bodyMetrics(): Paint.FontMetricsInt {
        val bodyLayout =
            StaticLayout.Builder.obtain(text, 0, BASE_TEXT.length, paint, Integer.MAX_VALUE).build()
        return Paint.FontMetricsInt().apply {
            ascent = bodyLayout.getLineAscent(0)
            descent = bodyLayout.getLineDescent(0)
        }
    }

    /** Height of the mark line box, measured independently at the scaled text size. */
    private fun markLineHeight(): Int {
        val scaledPaint =
            TextPaint(paint).apply { textSize = paint.textSize * EmphasisSpan.DEFAULT_SCALE }
        val markLayout =
            StaticLayout.Builder.obtain(
                    MARK_LETTER,
                    0,
                    MARK_LETTER.length,
                    scaledPaint,
                    Integer.MAX_VALUE,
                )
                .build()
        return markLayout.getLineDescent(0) - markLayout.getLineAscent(0)
    }

    private fun spanOver(position: AnnotationPosition) = EmphasisSpan(position = position)

    private fun spannedFor(span: EmphasisSpan): Spanned =
        SpannableString(BASE_TEXT).apply {
            setSpan(span, 0, length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        }

    /** Draws the layout and returns the baseline of every emphasis mark it paints. */
    private fun drawAndCollectMarks(position: AnnotationPosition): List<Float> {
        val markYs = mutableListOf<Float>()
        val canvas = RecordingCanvas { drawn, y ->
            if (drawn == MARK_LETTER) markYs.add(y)
        }
        layoutOf(position).draw(canvas, ORIGIN, ORIGIN, paint)
        return markYs
    }

    /** A [Canvas] that reports the text and y coordinate of every `drawText` call. */
    private class RecordingCanvas(val onDrawText: (String, Float) -> Unit) : Canvas() {
        override fun drawText(
            text: CharSequence,
            start: Int,
            end: Int,
            x: Float,
            y: Float,
            paint: Paint,
        ) {
            super.drawText(text, start, end, x, y, paint)
            onDrawText(text.substring(start, end), y)
        }

        override fun drawText(text: String, x: Float, y: Float, paint: Paint) {
            super.drawText(text, x, y, paint)
            onDrawText(text, y)
        }
    }
}
