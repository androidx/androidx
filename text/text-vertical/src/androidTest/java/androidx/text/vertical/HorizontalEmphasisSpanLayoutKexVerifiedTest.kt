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
import android.graphics.PointF
import android.graphics.RectF
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.SuperscriptSpan
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.ceil
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests branches in [HorizontalEmphasisSpanLayout] derived from Kex symbolic execution paths.
 *
 * Covers initialization, [HorizontalEmphasisSpanLayout.spanWidth],
 * [HorizontalEmphasisSpanLayout.fillFontMetrics], and [HorizontalEmphasisSpanLayout.draw].
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class HorizontalEmphasisSpanLayoutKexVerifiedTest {

    private var canvasBitmap: Bitmap? = null

    @Before
    fun clearThreadLocalPaintCache() {
        workingPaintCache.remove()
    }

    @After
    fun recycleCanvasBitmap() {
        canvasBitmap?.recycle()
        canvasBitmap = null
    }

    // Returns the measured spanWidth of the body text.
    @Test
    fun kex_emphasisLayout_getSpanWidth_returnsMeasuredBodyWidth() {
        val paint = TextPaint().apply { textSize = 40f }
        val text = SpannableString("Emphasis")
        val layout =
            HorizontalEmphasisSpanLayout(
                text = text,
                start = 0,
                end = text.length,
                emphasis = "•",
                position = AnnotationPosition.Before,
                paint = paint,
                relSize = 0.5f,
            )

        val expectedWidth = ceil(Layout.getDesiredWidth(text, 0, text.length, paint)).toInt()
        assertThat(layout.spanWidth).isEqualTo(expectedWidth)
    }

    // Overwrites ascent, descent, top, and bottom with boxAscent and boxDescent.
    @Test
    fun kex_emphasisLayout_fillFontMetrics_setsAscentDescentTopAndBottom() {
        val paint = TextPaint().apply { textSize = 40f }
        val text = SpannableString("Hello")
        val layout =
            HorizontalEmphasisSpanLayout(
                text = text,
                start = 0,
                end = text.length,
                emphasis = "•",
                position = AnnotationPosition.Before,
                paint = paint,
                relSize = 0.5f,
            )
        val fm =
            Paint.FontMetricsInt().apply {
                ascent = 777
                descent = 777
                top = 777
                bottom = 777
            }

        layout.fillFontMetrics(fm)

        assertThat(fm.ascent).isLessThan(0)
        assertThat(fm.descent).isGreaterThan(0)
        assertThat(fm.top).isEqualTo(fm.ascent)
        assertThat(fm.bottom).isEqualTo(fm.descent)
    }

    // Branch: empty body text range (start == end) with AnnotationPosition.Before and TextPaint.
    // Creates an empty positions array and expands boxAscent upward by markLineHeight.
    @Test
    fun kex_emphasisLayout_init_emptyRangePositionBeforeWithTextPaint_createsZeroWidthLayout() {
        val paint = TextPaint().apply { textSize = 40f }
        val text = SpannableString("Hello")
        val layout =
            HorizontalEmphasisSpanLayout(
                text = text,
                start = 0,
                end = 0,
                emphasis = "•",
                position = AnnotationPosition.Before,
                paint = paint,
                relSize = 0.5f,
            )

        val drawnMarks = mutableListOf<String>()
        layout.draw(recordingCanvas(mark = "•", marks = drawnMarks), 0f, 100f, paint)

        assertThat(layout.spanWidth).isEqualTo(0)
        assertThat(drawnMarks).isEmpty()
    }

    // Branch: empty body text range (start == end) with AnnotationPosition.After and TextPaint.
    // Creates an empty positions array and expands boxDescent downward by markLineHeight.
    @Test
    fun kex_emphasisLayout_init_emptyRangePositionAfterWithTextPaint_createsZeroWidthLayout() {
        val paint = TextPaint().apply { textSize = 40f }
        val text = SpannableString("Hello")
        val layout =
            HorizontalEmphasisSpanLayout(
                text = text,
                start = 0,
                end = 0,
                emphasis = "•",
                position = AnnotationPosition.After,
                paint = paint,
                relSize = 0.5f,
            )

        val drawnMarks = mutableListOf<String>()
        layout.draw(recordingCanvas(mark = "•", marks = drawnMarks), 0f, 100f, paint)

        assertThat(layout.spanWidth).isEqualTo(0)
        assertThat(drawnMarks).isEmpty()
    }

    // Branch: non-empty range containing non-target characters (spaces and punctuation where
    // isEmphasisTarget(codePoint) is false) with AnnotationPosition.Before and TextPaint.
    @Test
    fun kex_emphasisLayout_init_nonTargetCharsPositionBeforeWithTextPaint_leavesPositionsNaN() {
        val paint = TextPaint().apply { textSize = 40f }
        val text = SpannableString(" ,.")
        val layout =
            HorizontalEmphasisSpanLayout(
                text = text,
                start = 0,
                end = text.length,
                emphasis = "•",
                position = AnnotationPosition.Before,
                paint = paint,
                relSize = 0.5f,
            )

        val drawnMarks = mutableListOf<String>()
        layout.draw(recordingCanvas(mark = "•", marks = drawnMarks), 0f, 100f, paint)

        assertThat(layout.spanWidth).isGreaterThan(0)
        assertThat(drawnMarks).isEmpty()
    }

    // Branch: empty body text range (start == end) with AnnotationPosition.After and plain Paint.
    // Initializes workingPaintCache from plain Paint and creates an empty positions array.
    @Test
    fun kex_emphasisLayout_init_emptyRangePositionAfterWithPlainPaint_createsZeroWidthLayout() {
        val plainPaint = Paint().apply { textSize = 40f }
        val text = SpannableString("Hello")
        val layout =
            HorizontalEmphasisSpanLayout(
                text = text,
                start = 0,
                end = 0,
                emphasis = "•",
                position = AnnotationPosition.After,
                paint = plainPaint,
                relSize = 0.5f,
            )

        val drawnMarks = mutableListOf<String>()
        layout.draw(recordingCanvas(mark = "•", marks = drawnMarks), 0f, 100f, plainPaint)

        assertThat(layout.spanWidth).isEqualTo(0)
        assertThat(drawnMarks).isEmpty()
    }

    // Branch: non-empty range containing non-target characters with AnnotationPosition.After and
    // plain Paint. Leaves positions[gs] as Float.NaN.
    @Test
    fun kex_emphasisLayout_init_nonTargetCharsPositionAfterWithPlainPaint_leavesPositionsNaN() {
        val plainPaint = Paint().apply { textSize = 40f }
        val text = SpannableString(" ,.")
        val layout =
            HorizontalEmphasisSpanLayout(
                text = text,
                start = 0,
                end = text.length,
                emphasis = "•",
                position = AnnotationPosition.After,
                paint = plainPaint,
                relSize = 0.5f,
            )

        val drawnMarks = mutableListOf<String>()
        layout.draw(recordingCanvas(mark = "•", marks = drawnMarks), 0f, 100f, plainPaint)

        assertThat(layout.spanWidth).isGreaterThan(0)
        assertThat(drawnMarks).isEmpty()
    }

    // Branch: non-empty range with target letter graphemes (isEmphasisTarget(codePoint) is true),
    // AnnotationPosition.After, and TextPaint. Populates positions[gs] with centered coordinates.
    @Test
    fun kex_emphasisLayout_init_targetGraphemesPositionAfterWithTextPaint_populatesPositions() {
        val textPaint = TextPaint().apply { textSize = 40f }
        val text = SpannableString("AB")
        val layout =
            HorizontalEmphasisSpanLayout(
                text = text,
                start = 0,
                end = text.length,
                emphasis = "•",
                position = AnnotationPosition.After,
                paint = textPaint,
                relSize = 0.5f,
            )

        val drawnMarks = mutableListOf<String>()
        val markPositions = mutableListOf<PointF>()
        layout.draw(
            recordingCanvas(mark = "•", marks = drawnMarks, markPositions = markPositions),
            0f,
            100f,
            textPaint,
        )

        assertThat(drawnMarks).containsExactly("•", "•")
        assertThat(markPositions[1].x).isGreaterThan(markPositions[0].x)
    }

    // Branch: non-empty range with target letter graphemes, AnnotationPosition.Before, and plain
    // Paint. Populates positions[gs] using the working TextPaint cache.
    @Test
    fun kex_emphasisLayout_init_targetGraphemesPositionBeforeWithPlainPaint_populatesPositions() {
        val plainPaint = Paint().apply { textSize = 40f }
        val text = SpannableString("AB")
        val layout =
            HorizontalEmphasisSpanLayout(
                text = text,
                start = 0,
                end = text.length,
                emphasis = "•",
                position = AnnotationPosition.Before,
                paint = plainPaint,
                relSize = 0.5f,
            )

        val drawnMarks = mutableListOf<String>()
        val markPositions = mutableListOf<PointF>()
        layout.draw(
            recordingCanvas(mark = "•", marks = drawnMarks, markPositions = markPositions),
            0f,
            100f,
            plainPaint,
        )

        assertThat(drawnMarks).containsExactly("•", "•")
        assertThat(markPositions[1].x).isGreaterThan(markPositions[0].x)
    }

    // Branch: empty body range (start == end), position is AnnotationPosition.Unknown,
    // plain Paint. Proves fallback `position != AnnotationPosition.After` sets isMarkOver = true.
    @Test
    fun kex_emphasisLayout_init_emptyRangePositionUnknownWithPlainPaint_treatsAsMarkOver() {
        val plainPaint = Paint().apply { textSize = 40f }
        val text = SpannableString("Hello")

        val layoutUnknown =
            HorizontalEmphasisSpanLayout(
                text = text,
                start = 0,
                end = 0,
                emphasis = "•",
                position = AnnotationPosition.Unknown,
                paint = plainPaint,
                relSize = 0.5f,
            )
        val layoutBefore =
            HorizontalEmphasisSpanLayout(
                text = text,
                start = 0,
                end = 0,
                emphasis = "•",
                position = AnnotationPosition.Before,
                paint = plainPaint,
                relSize = 0.5f,
            )

        val fmUnknown = Paint.FontMetricsInt()
        val fmBefore = Paint.FontMetricsInt()
        layoutUnknown.fillFontMetrics(fmUnknown)
        layoutBefore.fillFontMetrics(fmBefore)

        assertThat(fmUnknown.ascent).isEqualTo(fmBefore.ascent)
        assertThat(fmUnknown.descent).isEqualTo(fmBefore.descent)
    }

    // Branch: non-empty body range, position is AnnotationPosition.Unknown, TextPaint.
    // Proves fallback `position != AnnotationPosition.After` sets isMarkOver = true.
    @Test
    fun kex_emphasisLayout_init_positionUnknownWithTextPaint_treatsAsMarkOver() {
        val paint = TextPaint().apply { textSize = 40f }
        val text = SpannableString("Hello")

        val layoutUnknown =
            HorizontalEmphasisSpanLayout(
                text = text,
                start = 0,
                end = text.length,
                emphasis = "•",
                position = AnnotationPosition.Unknown,
                paint = paint,
                relSize = 0.5f,
            )
        val layoutBefore =
            HorizontalEmphasisSpanLayout(
                text = text,
                start = 0,
                end = text.length,
                emphasis = "•",
                position = AnnotationPosition.Before,
                paint = paint,
                relSize = 0.5f,
            )

        val fmUnknown = Paint.FontMetricsInt()
        val fmBefore = Paint.FontMetricsInt()
        layoutUnknown.fillFontMetrics(fmUnknown)
        layoutBefore.fillFontMetrics(fmBefore)

        assertThat(fmUnknown.ascent).isEqualTo(fmBefore.ascent)
        assertThat(fmUnknown.descent).isEqualTo(fmBefore.descent)
    }

    // Branch: empty positions array, isMarkOver == false (After), plain Paint.
    // Draws neither background rectangle nor emphasis marks.
    @Test
    fun kex_emphasisLayout_draw_emptyPositionsMarkUnderWithPlainPaint_drawsNoRectOrMarks() {
        val paint = TextPaint().apply { textSize = 40f }
        val layout =
            HorizontalEmphasisSpanLayout(
                text = SpannableString("Hello"),
                start = 0,
                end = 0,
                emphasis = "•",
                position = AnnotationPosition.After,
                paint = paint,
                relSize = 0.5f,
            )

        val drawnRects = mutableListOf<RectF>()
        val drawnMarks = mutableListOf<String>()
        val canvas = recordingCanvas(rects = drawnRects, mark = "•", marks = drawnMarks)

        layout.draw(canvas, 10f, 100f, Paint().apply { textSize = 40f })

        assertThat(drawnRects).isEmpty()
        assertThat(drawnMarks).isEmpty()
    }

    // Branch: empty positions array, isMarkOver == true (Before), plain Paint.
    // Draws neither background rectangle nor emphasis marks.
    @Test
    fun kex_emphasisLayout_draw_emptyPositionsMarkOverWithPlainPaint_drawsNoRectOrMarks() {
        val paint = TextPaint().apply { textSize = 40f }
        val layout =
            HorizontalEmphasisSpanLayout(
                text = SpannableString("Hello"),
                start = 0,
                end = 0,
                emphasis = "•",
                position = AnnotationPosition.Before,
                paint = paint,
                relSize = 0.5f,
            )

        val drawnRects = mutableListOf<RectF>()
        val drawnMarks = mutableListOf<String>()
        val canvas = recordingCanvas(rects = drawnRects, mark = "•", marks = drawnMarks)

        layout.draw(canvas, 10f, 100f, Paint().apply { textSize = 40f })

        assertThat(drawnRects).isEmpty()
        assertThat(drawnMarks).isEmpty()
    }

    // Branch: empty positions array, isMarkOver == false (After), TextPaint(bgColor = 0).
    // Draws neither background rectangle nor emphasis marks.
    @Test
    fun kex_emphasisLayout_draw_emptyPositionsMarkUnderWithZeroBgColor_drawsNoRectOrMarks() {
        val paint =
            TextPaint().apply {
                textSize = 40f
                bgColor = Color.TRANSPARENT
            }
        val layout =
            HorizontalEmphasisSpanLayout(
                text = SpannableString("Hello"),
                start = 0,
                end = 0,
                emphasis = "•",
                position = AnnotationPosition.After,
                paint = paint,
                relSize = 0.5f,
            )

        val drawnRects = mutableListOf<RectF>()
        val drawnMarks = mutableListOf<String>()
        val canvas = recordingCanvas(rects = drawnRects, mark = "•", marks = drawnMarks)

        layout.draw(canvas, 10f, 100f, paint)

        assertThat(drawnRects).isEmpty()
        assertThat(drawnMarks).isEmpty()
    }

    // Branch: empty positions array, isMarkOver == true (Before), TextPaint(bgColor = 0).
    // Draws neither background rectangle nor emphasis marks.
    @Test
    fun kex_emphasisLayout_draw_emptyPositionsMarkOverWithZeroBgColor_drawsNoRectOrMarks() {
        val paint =
            TextPaint().apply {
                textSize = 40f
                bgColor = Color.TRANSPARENT
            }
        val layout =
            HorizontalEmphasisSpanLayout(
                text = SpannableString("Hello"),
                start = 0,
                end = 0,
                emphasis = "•",
                position = AnnotationPosition.Before,
                paint = paint,
                relSize = 0.5f,
            )

        val drawnRects = mutableListOf<RectF>()
        val drawnMarks = mutableListOf<String>()
        val canvas = recordingCanvas(rects = drawnRects, mark = "•", marks = drawnMarks)

        layout.draw(canvas, 10f, 100f, paint)

        assertThat(drawnRects).isEmpty()
        assertThat(drawnMarks).isEmpty()
    }

    // Branch: empty positions array (spanWidth == 0), isMarkOver == true (Before),
    // TextPaint(bgColor != 0). Skips the zero-width background box via left >= right and draws
    // zero emphasis marks.
    @Test
    fun kex_emphasisLayout_draw_emptyPositionsMarkOverWithBgColor_skipsZeroWidthBoxAndMarks() {
        val paint = TextPaint().apply { textSize = 40f }
        val layout =
            HorizontalEmphasisSpanLayout(
                text = SpannableString("Hello"),
                start = 0,
                end = 0,
                emphasis = "•",
                position = AnnotationPosition.Before,
                paint = paint,
                relSize = 0.5f,
            )

        val drawnRects = mutableListOf<RectF>()
        val drawnMarks = mutableListOf<String>()
        val canvas = recordingCanvas(rects = drawnRects, mark = "•", marks = drawnMarks)
        val bgPaint =
            TextPaint().apply {
                textSize = 40f
                bgColor = Color.GREEN
            }

        layout.draw(canvas, 15f, 100f, bgPaint)

        assertThat(layout.spanWidth).isEqualTo(0)
        assertThat(drawnRects).isEmpty()
        assertThat(drawnMarks).isEmpty()
    }

    // Branch: empty positions array (spanWidth == 0), isMarkOver == false (After),
    // TextPaint(bgColor != 0). Skips the zero-width background box via left >= right and draws
    // zero emphasis marks.
    @Test
    fun kex_emphasisLayout_draw_emptyPositionsMarkUnderWithBgColor_skipsZeroWidthBoxAndMarks() {
        val paint = TextPaint().apply { textSize = 40f }
        val layout =
            HorizontalEmphasisSpanLayout(
                text = SpannableString("Hello"),
                start = 0,
                end = 0,
                emphasis = "•",
                position = AnnotationPosition.After,
                paint = paint,
                relSize = 0.5f,
            )

        val drawnRects = mutableListOf<RectF>()
        val drawnMarks = mutableListOf<String>()
        val canvas = recordingCanvas(rects = drawnRects, mark = "•", marks = drawnMarks)
        val bgPaint =
            TextPaint().apply {
                textSize = 40f
                bgColor = Color.GREEN
            }

        layout.draw(canvas, 15f, 100f, bgPaint)

        assertThat(layout.spanWidth).isEqualTo(0)
        assertThat(drawnRects).isEmpty()
        assertThat(drawnMarks).isEmpty()
    }

    // Branch: positions contains only Float.NaN entries (whitespace), isMarkOver == false (After),
    // plain Paint. Skips every NaN entry via `if (pos.isNaN()) return@forEach`.
    @Test
    fun kex_emphasisLayout_draw_allPositionsNaNMarkUnderWithPlainPaint_skipsMarks() {
        val paint = TextPaint().apply { textSize = 40f }
        val text = SpannableString("   ")
        val layout =
            HorizontalEmphasisSpanLayout(
                text = text,
                start = 0,
                end = text.length,
                emphasis = "•",
                position = AnnotationPosition.After,
                paint = paint,
                relSize = 0.5f,
            )

        val drawnRects = mutableListOf<RectF>()
        val drawnMarks = mutableListOf<String>()
        val canvas = recordingCanvas(rects = drawnRects, mark = "•", marks = drawnMarks)

        layout.draw(canvas, 10f, 100f, Paint().apply { textSize = 40f })

        assertThat(drawnRects).isEmpty()
        assertThat(drawnMarks).isEmpty()
    }

    // Branch: positions contains only Float.NaN entries (whitespace), isMarkOver == true (Before),
    // TextPaint(bgColor != 0). Draws the background box and skips all mark draw calls.
    @Test
    fun kex_emphasisLayout_draw_allPositionsNaNMarkOverWithBgColor_drawsBackgroundAndSkipsMarks() {
        val paint = TextPaint().apply { textSize = 40f }
        val text = SpannableString("   ")
        val layout =
            HorizontalEmphasisSpanLayout(
                text = text,
                start = 0,
                end = text.length,
                emphasis = "•",
                position = AnnotationPosition.Before,
                paint = paint,
                relSize = 0.5f,
            )
        val fm = Paint.FontMetricsInt()
        layout.fillFontMetrics(fm)

        val drawnRects = mutableListOf<RectF>()
        val drawnMarks = mutableListOf<String>()
        val canvas = recordingCanvas(rects = drawnRects, mark = "•", marks = drawnMarks)
        val bgPaint =
            TextPaint().apply {
                textSize = 40f
                bgColor = Color.LTGRAY
            }

        layout.draw(canvas, 10f, 100f, bgPaint)

        assertThat(drawnRects)
            .containsExactly(
                RectF(10f, 100f + fm.ascent, 10f + layout.spanWidth, 100f + fm.descent)
            )
        assertThat(drawnMarks).isEmpty()
    }

    // Branch: single valid target grapheme (!pos.isNaN()), isMarkOver == true (Before), plain
    // Paint. Draws a single emphasis mark above the body baseline.
    @Test
    fun kex_emphasisLayout_draw_singleTargetMarkOverWithPlainPaint_drawsMarkAboveBody() {
        val paint = TextPaint().apply { textSize = 40f }
        val text = SpannableString("A")
        val layout =
            HorizontalEmphasisSpanLayout(
                text = text,
                start = 0,
                end = text.length,
                emphasis = "•",
                position = AnnotationPosition.Before,
                paint = paint,
                relSize = 0.5f,
            )

        val drawnRects = mutableListOf<RectF>()
        val drawnMarks = mutableListOf<String>()
        val markPositions = mutableListOf<PointF>()
        val canvas =
            recordingCanvas(
                rects = drawnRects,
                mark = "•",
                marks = drawnMarks,
                markPositions = markPositions,
            )

        layout.draw(canvas, 10f, 100f, Paint().apply { textSize = 40f })

        assertThat(drawnRects).isEmpty()
        assertThat(drawnMarks).containsExactly("•")
        assertThat(markPositions.single().y).isLessThan(100f)
    }

    // Branch: single valid target grapheme (!pos.isNaN()), isMarkOver == false (After),
    // TextPaint(bgColor = 0). Draws a single emphasis mark below the body baseline.
    @Test
    fun kex_emphasisLayout_draw_singleTargetMarkUnderWithZeroBgColor_drawsMarkBelowBody() {
        val paint = TextPaint().apply { textSize = 40f }
        val text = SpannableString("A")
        val layout =
            HorizontalEmphasisSpanLayout(
                text = text,
                start = 0,
                end = text.length,
                emphasis = "•",
                position = AnnotationPosition.After,
                paint = paint,
                relSize = 0.5f,
            )

        val drawnRects = mutableListOf<RectF>()
        val drawnMarks = mutableListOf<String>()
        val markPositions = mutableListOf<PointF>()
        val canvas =
            recordingCanvas(
                rects = drawnRects,
                mark = "•",
                marks = drawnMarks,
                markPositions = markPositions,
            )
        val drawPaint =
            TextPaint().apply {
                textSize = 40f
                bgColor = Color.TRANSPARENT
            }

        layout.draw(canvas, 10f, 100f, drawPaint)

        assertThat(drawnRects).isEmpty()
        assertThat(drawnMarks).containsExactly("•")
        assertThat(markPositions.single().y).isGreaterThan(100f)
    }

    // Branch: single valid target grapheme (!pos.isNaN()), isMarkOver == false (After),
    // TextPaint(bgColor != 0). Fills the background box and draws the mark below the body line.
    @Test
    fun kex_emphasisLayout_draw_singleTargetMarkUnderWithBgColor_drawsBackgroundAndMarkBelowBody() {
        val paint = TextPaint().apply { textSize = 40f }
        val text = SpannableString("A")
        val layout =
            HorizontalEmphasisSpanLayout(
                text = text,
                start = 0,
                end = text.length,
                emphasis = "•",
                position = AnnotationPosition.After,
                paint = paint,
                relSize = 0.5f,
            )
        val fm = Paint.FontMetricsInt()
        layout.fillFontMetrics(fm)

        val drawnRects = mutableListOf<RectF>()
        val drawnMarks = mutableListOf<String>()
        val markPositions = mutableListOf<PointF>()
        val canvas =
            recordingCanvas(
                rects = drawnRects,
                mark = "•",
                marks = drawnMarks,
                markPositions = markPositions,
            )
        val drawPaint =
            TextPaint().apply {
                textSize = 40f
                bgColor = Color.MAGENTA
            }

        layout.draw(canvas, 10f, 100f, drawPaint)

        assertThat(drawnRects)
            .containsExactly(
                RectF(10f, 100f + fm.ascent, 10f + layout.spanWidth, 100f + fm.descent)
            )
        assertThat(drawnMarks).containsExactly("•")
        assertThat(markPositions.single().y).isGreaterThan(100f)
    }

    // Branch: multiple target graphemes (length 2), isMarkOver == false (After), plain Paint.
    // Draws an emphasis mark for each target grapheme without a background rectangle.
    @Test
    fun kex_emphasisLayout_draw_multipleTargetsMarkUnderWithPlainPaint_drawsAllMarks() {
        val paint = TextPaint().apply { textSize = 40f }
        val text = SpannableString("Hi")
        val layout =
            HorizontalEmphasisSpanLayout(
                text = text,
                start = 0,
                end = text.length,
                emphasis = "•",
                position = AnnotationPosition.After,
                paint = paint,
                relSize = 0.5f,
            )

        val drawnRects = mutableListOf<RectF>()
        val drawnMarks = mutableListOf<String>()
        val markPositions = mutableListOf<PointF>()
        val canvas =
            recordingCanvas(
                rects = drawnRects,
                mark = "•",
                marks = drawnMarks,
                markPositions = markPositions,
            )

        layout.draw(canvas, 10f, 100f, Paint().apply { textSize = 40f })

        assertThat(drawnRects).isEmpty()
        assertThat(drawnMarks).containsExactly("•", "•")
        assertThat(markPositions[1].x).isGreaterThan(markPositions[0].x)
    }

    // Branch: multiple target graphemes (length 2), isMarkOver == true (Before),
    // TextPaint(bgColor = 0). Draws an emphasis mark above each target grapheme.
    @Test
    fun kex_emphasisLayout_draw_multipleTargetsMarkOverWithZeroBgColor_drawsAllMarks() {
        val paint = TextPaint().apply { textSize = 40f }
        val text = SpannableString("Hi")
        val layout =
            HorizontalEmphasisSpanLayout(
                text = text,
                start = 0,
                end = text.length,
                emphasis = "•",
                position = AnnotationPosition.Before,
                paint = paint,
                relSize = 0.5f,
            )

        val drawnRects = mutableListOf<RectF>()
        val drawnMarks = mutableListOf<String>()
        val markPositions = mutableListOf<PointF>()
        val canvas =
            recordingCanvas(
                rects = drawnRects,
                mark = "•",
                marks = drawnMarks,
                markPositions = markPositions,
            )
        val drawPaint =
            TextPaint().apply {
                textSize = 40f
                bgColor = Color.TRANSPARENT
            }

        layout.draw(canvas, 10f, 100f, drawPaint)

        assertThat(drawnRects).isEmpty()
        assertThat(drawnMarks).containsExactly("•", "•")
        assertThat(markPositions[0].y).isLessThan(100f)
        assertThat(markPositions[1].x).isGreaterThan(markPositions[0].x)
    }

    // Branch: multiple target graphemes (length 2), isMarkOver == false (After),
    // TextPaint(bgColor != 0). Fills the background rectangle and draws both emphasis marks.
    @Test
    fun kex_emphasisLayout_draw_multipleTargetsMarkUnderWithBgColor_drawsBackgroundAndAllMarks() {
        val paint = TextPaint().apply { textSize = 40f }
        val text = SpannableString("Hi")
        val layout =
            HorizontalEmphasisSpanLayout(
                text = text,
                start = 0,
                end = text.length,
                emphasis = "•",
                position = AnnotationPosition.After,
                paint = paint,
                relSize = 0.5f,
            )
        val fm = Paint.FontMetricsInt()
        layout.fillFontMetrics(fm)

        val drawnRects = mutableListOf<RectF>()
        val drawnMarks = mutableListOf<String>()
        val markPositions = mutableListOf<PointF>()
        val canvas =
            recordingCanvas(
                rects = drawnRects,
                mark = "•",
                marks = drawnMarks,
                markPositions = markPositions,
            )
        val drawPaint =
            TextPaint().apply {
                textSize = 40f
                bgColor = Color.MAGENTA
            }

        layout.draw(canvas, 10f, 100f, drawPaint)

        assertThat(drawnRects)
            .containsExactly(
                RectF(10f, 100f + fm.ascent, 10f + layout.spanWidth, 100f + fm.descent)
            )
        assertThat(drawnMarks).containsExactly("•", "•")
        assertThat(markPositions[0].y).isGreaterThan(100f)
        assertThat(markPositions[1].x).isGreaterThan(markPositions[0].x)
    }

    // Branch: mixed target and non-target graphemes in positions (some valid, some Float.NaN),
    // with SuperscriptSpan baselineShift and bgColor reset to 0 on bodyLayout.paint, and caller
    // paint textSize restored after withTextScale(relSize).
    @Test
    fun kex_emphasisLayout_draw_mixedTargetAndSkippedPositions_drawsOnlyTargetsAndResetsPaint() {
        val basePaint = TextPaint().apply { textSize = 40f }
        val mixedText =
            SpannableString("A B!C").apply {
                setSpan(SuperscriptSpan(), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        val layout =
            HorizontalEmphasisSpanLayout(
                text = mixedText,
                start = 0,
                end = mixedText.length,
                emphasis = "•",
                position = AnnotationPosition.Before,
                paint = basePaint,
                relSize = 0.5f,
            )
        val fm = Paint.FontMetricsInt()
        layout.fillFontMetrics(fm)

        val drawnRects = mutableListOf<RectF>()
        val drawnMarks = mutableListOf<String>()
        val canvas = recordingCanvas(rects = drawnRects, mark = "•", marks = drawnMarks)
        val richDrawPaint =
            TextPaint().apply {
                textSize = 40f
                bgColor = Color.YELLOW
                baselineShift = 20
                linkColor = Color.GREEN
            }

        layout.draw(canvas, 10f, 120f, richDrawPaint)

        assertThat(drawnMarks).containsExactly("•", "•", "•")
        assertThat(drawnRects)
            .containsExactly(
                RectF(10f, 120f + fm.ascent, 10f + layout.spanWidth, 120f + fm.descent)
            )
        assertThat(richDrawPaint.textSize).isEqualTo(40f)
        assertThat(workingPaintCache.get()?.bgColor).isEqualTo(Color.TRANSPARENT)
        assertThat(workingPaintCache.get()?.baselineShift).isEqualTo(0)
        assertThat(workingPaintCache.get()?.linkColor).isEqualTo(Color.GREEN)
    }

    private fun recordingCanvas(
        rects: MutableList<RectF>? = null,
        mark: String? = null,
        marks: MutableList<String>? = null,
        markPositions: MutableList<PointF>? = null,
    ): Canvas {
        val bitmap =
            canvasBitmap
                ?: Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888).also {
                    canvasBitmap = it
                }
        return object : Canvas(bitmap) {
            override fun drawRect(
                left: Float,
                top: Float,
                right: Float,
                bottom: Float,
                paint: Paint,
            ) {
                rects?.add(RectF(left, top, right, bottom))
            }

            override fun drawText(text: String, x: Float, y: Float, paint: Paint) {
                if (mark == null || text == mark) {
                    marks?.add(text)
                    markPositions?.add(PointF(x, y))
                }
            }
        }
    }
}
