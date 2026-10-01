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
import android.text.StaticLayout
import android.text.TextPaint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.ceil
import kotlin.math.max
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests branches in [HorizontalRubySpanLayout] derived from Kex symbolic execution paths.
 *
 * Covers initialization, [HorizontalRubySpanLayout.spanWidth],
 * [HorizontalRubySpanLayout.fillFontMetrics], and [HorizontalRubySpanLayout.draw].
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class HorizontalRubySpanLayoutKexVerifiedTest {

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

    // Returns the measured spanWidth max(bodyWidth, rubyWidth).
    @Test
    fun kex_rubyLayout_getSpanWidth_returnsMaxOfBodyAndRubyWidth() {
        val paint = TextPaint().apply { textSize = 40f }
        val text = SpannableString("BodyText")
        val ruby = "Ruby"
        val layout =
            HorizontalRubySpanLayout(
                text = text,
                start = 0,
                end = text.length,
                rubyText = ruby,
                position = AnnotationPosition.Before,
                paint = paint,
                rubyScale = 0.5f,
            )

        val expectedBodyWidth = ceil(Layout.getDesiredWidth(text, 0, text.length, paint)).toInt()
        val rubyPaint = TextPaint(paint).apply { textSize *= 0.5f }
        val expectedRubyWidth =
            ceil(Layout.getDesiredWidth(ruby, 0, ruby.length, rubyPaint)).toInt()

        assertThat(layout.spanWidth).isEqualTo(max(expectedBodyWidth, expectedRubyWidth))
    }

    // Overwrites ascent, descent, top, and bottom with boxAscent and boxDescent.
    @Test
    fun kex_rubyLayout_fillFontMetrics_setsAscentDescentTopAndBottom() {
        val paint = TextPaint().apply { textSize = 40f }
        val text = SpannableString("Hello")
        val ruby = "World"
        val layout =
            HorizontalRubySpanLayout(
                text = text,
                start = 0,
                end = text.length,
                rubyText = ruby,
                position = AnnotationPosition.Before,
                paint = paint,
                rubyScale = 0.5f,
            )
        val fm =
            Paint.FontMetricsInt().apply {
                ascent = 999
                descent = 999
                top = 999
                bottom = 999
            }

        layout.fillFontMetrics(fm)

        assertThat(fm.ascent).isLessThan(0)
        assertThat(fm.descent).isGreaterThan(0)
        assertThat(fm.top).isEqualTo(fm.ascent)
        assertThat(fm.bottom).isEqualTo(fm.descent)
    }

    // Branch: empty body range (start == end), position == AnnotationPosition.Before, TextPaint.
    // Places ruby over the empty body range and sets spanWidth to the ruby width.
    @Test
    fun kex_rubyLayout_init_emptyRangePositionBeforeWithTextPaint_expandsBoxAscentUpward() {
        val paint = TextPaint().apply { textSize = 40f }
        val text = SpannableString("Hello")
        val ruby = "Ruby"
        val layout =
            HorizontalRubySpanLayout(
                text = text,
                start = 0,
                end = 0,
                rubyText = ruby,
                position = AnnotationPosition.Before,
                paint = paint,
                rubyScale = 0.5f,
            )

        val bodyLayout = StaticLayout.Builder.obtain("", 0, 0, paint, 0).build()
        val rubyPaint = TextPaint(paint).apply { textSize *= 0.5f }
        val expectedRubyWidth =
            ceil(Layout.getDesiredWidth(ruby, 0, ruby.length, rubyPaint)).toInt()
        val rubyLayout =
            StaticLayout.Builder.obtain(ruby, 0, ruby.length, rubyPaint, expectedRubyWidth).build()
        val rubyLineHeight = rubyLayout.getLineDescent(0) - rubyLayout.getLineAscent(0)

        val fm = Paint.FontMetricsInt()
        layout.fillFontMetrics(fm)
        assertThat(layout.spanWidth).isEqualTo(expectedRubyWidth)
        assertThat(fm.ascent).isEqualTo(bodyLayout.getLineAscent(0) - rubyLineHeight)
        assertThat(fm.descent).isEqualTo(bodyLayout.getLineDescent(0))
    }

    // Branch: non-empty body range (start < end), position == AnnotationPosition.Before, TextPaint.
    // Places ruby over the body line and expands boxAscent upward by rubyLineHeight.
    @Test
    fun kex_rubyLayout_init_positionBeforeWithTextPaint_expandsBoxAscentUpward() {
        val paint = TextPaint().apply { textSize = 40f }
        val text = SpannableString("Hello")
        val ruby = "Ruby"
        val layout =
            HorizontalRubySpanLayout(
                text = text,
                start = 0,
                end = text.length,
                rubyText = ruby,
                position = AnnotationPosition.Before,
                paint = paint,
                rubyScale = 0.5f,
            )

        val bodyLayout =
            StaticLayout.Builder.obtain(text, 0, text.length, paint, Int.MAX_VALUE).build()
        val rubyPaint = TextPaint(paint).apply { textSize *= 0.5f }
        val rubyLayout =
            StaticLayout.Builder.obtain(ruby, 0, ruby.length, rubyPaint, Int.MAX_VALUE).build()
        val rubyLineHeight = rubyLayout.getLineDescent(0) - rubyLayout.getLineAscent(0)

        val fm = Paint.FontMetricsInt()
        layout.fillFontMetrics(fm)
        assertThat(fm.ascent).isEqualTo(bodyLayout.getLineAscent(0) - rubyLineHeight)
        assertThat(fm.descent).isEqualTo(bodyLayout.getLineDescent(0))
    }

    // Branch: non-empty body range (start < end), position == AnnotationPosition.After, TextPaint.
    // Sets isRubyOver = false and expands boxDescent downward by rubyLineHeight.
    @Test
    fun kex_rubyLayout_init_positionAfterWithTextPaint_expandsBoxDescentDownward() {
        val paint = TextPaint().apply { textSize = 40f }
        val text = SpannableString("Hello")
        val ruby = "Ruby"
        val layout =
            HorizontalRubySpanLayout(
                text = text,
                start = 0,
                end = text.length,
                rubyText = ruby,
                position = AnnotationPosition.After,
                paint = paint,
                rubyScale = 0.5f,
            )

        val bodyLayout =
            StaticLayout.Builder.obtain(text, 0, text.length, paint, Int.MAX_VALUE).build()
        val rubyPaint = TextPaint(paint).apply { textSize *= 0.5f }
        val rubyLayout =
            StaticLayout.Builder.obtain(ruby, 0, ruby.length, rubyPaint, Int.MAX_VALUE).build()
        val rubyLineHeight = rubyLayout.getLineDescent(0) - rubyLayout.getLineAscent(0)

        val fm = Paint.FontMetricsInt()
        layout.fillFontMetrics(fm)
        assertThat(fm.ascent).isEqualTo(bodyLayout.getLineAscent(0))
        assertThat(fm.descent).isEqualTo(bodyLayout.getLineDescent(0) + rubyLineHeight)
    }

    // Branch: empty body range (start == end), position == AnnotationPosition.After, TextPaint.
    // Sets isRubyOver = false on an empty body range and expands boxDescent downward.
    @Test
    fun kex_rubyLayout_init_emptyRangePositionAfterWithTextPaint_expandsBoxDescentDownward() {
        val paint = TextPaint().apply { textSize = 40f }
        val text = SpannableString("Hello")
        val ruby = "Ruby"
        val layout =
            HorizontalRubySpanLayout(
                text = text,
                start = 0,
                end = 0,
                rubyText = ruby,
                position = AnnotationPosition.After,
                paint = paint,
                rubyScale = 0.5f,
            )

        val bodyLayout = StaticLayout.Builder.obtain("", 0, 0, paint, 0).build()
        val rubyPaint = TextPaint(paint).apply { textSize *= 0.5f }
        val expectedRubyWidth =
            ceil(Layout.getDesiredWidth(ruby, 0, ruby.length, rubyPaint)).toInt()
        val rubyLayout =
            StaticLayout.Builder.obtain(ruby, 0, ruby.length, rubyPaint, expectedRubyWidth).build()
        val rubyLineHeight = rubyLayout.getLineDescent(0) - rubyLayout.getLineAscent(0)

        val fm = Paint.FontMetricsInt()
        layout.fillFontMetrics(fm)
        assertThat(layout.spanWidth).isEqualTo(expectedRubyWidth)
        assertThat(fm.ascent).isEqualTo(bodyLayout.getLineAscent(0))
        assertThat(fm.descent).isEqualTo(bodyLayout.getLineDescent(0) + rubyLineHeight)
    }

    // Branch: non-empty body range (start < end), position == AnnotationPosition.After, plain
    // Paint.
    // Initializes workingPaintCache from plain Paint and expands boxDescent downward.
    @Test
    fun kex_rubyLayout_init_positionAfterWithPlainPaint_expandsBoxDescentDownward() {
        val plainPaint = Paint().apply { textSize = 40f }
        val text = SpannableString("Hello")
        val ruby = "Ruby"
        val layout =
            HorizontalRubySpanLayout(
                text = text,
                start = 0,
                end = text.length,
                rubyText = ruby,
                position = AnnotationPosition.After,
                paint = plainPaint,
                rubyScale = 0.5f,
            )

        val refTextPaint = TextPaint().apply { set(plainPaint) }
        val bodyLayout =
            StaticLayout.Builder.obtain(text, 0, text.length, refTextPaint, Int.MAX_VALUE).build()
        val rubyPaint = TextPaint(refTextPaint).apply { textSize *= 0.5f }
        val rubyLayout =
            StaticLayout.Builder.obtain(ruby, 0, ruby.length, rubyPaint, Int.MAX_VALUE).build()
        val rubyLineHeight = rubyLayout.getLineDescent(0) - rubyLayout.getLineAscent(0)

        val fm = Paint.FontMetricsInt()
        layout.fillFontMetrics(fm)
        assertThat(layout.spanWidth).isGreaterThan(0)
        assertThat(fm.ascent).isEqualTo(bodyLayout.getLineAscent(0))
        assertThat(fm.descent).isEqualTo(bodyLayout.getLineDescent(0) + rubyLineHeight)
    }

    // Branch: empty body range (start == end), position == AnnotationPosition.Before, plain Paint.
    // Initializes workingPaintCache from plain Paint on an empty body range with ruby over.
    @Test
    fun kex_rubyLayout_init_emptyRangePositionBeforeWithPlainPaint_expandsBoxAscentUpward() {
        val plainPaint = Paint().apply { textSize = 40f }
        val text = SpannableString("Hello")
        val ruby = "Ruby"
        val layout =
            HorizontalRubySpanLayout(
                text = text,
                start = 0,
                end = 0,
                rubyText = ruby,
                position = AnnotationPosition.Before,
                paint = plainPaint,
                rubyScale = 0.5f,
            )

        val refTextPaint = TextPaint().apply { set(plainPaint) }
        val bodyLayout = StaticLayout.Builder.obtain("", 0, 0, refTextPaint, 0).build()
        val rubyPaint = TextPaint(refTextPaint).apply { textSize *= 0.5f }
        val expectedRubyWidth =
            ceil(Layout.getDesiredWidth(ruby, 0, ruby.length, rubyPaint)).toInt()
        val rubyLayout =
            StaticLayout.Builder.obtain(ruby, 0, ruby.length, rubyPaint, expectedRubyWidth).build()
        val rubyLineHeight = rubyLayout.getLineDescent(0) - rubyLayout.getLineAscent(0)

        val fm = Paint.FontMetricsInt()
        layout.fillFontMetrics(fm)
        assertThat(layout.spanWidth).isEqualTo(expectedRubyWidth)
        assertThat(fm.ascent).isEqualTo(bodyLayout.getLineAscent(0) - rubyLineHeight)
        assertThat(fm.descent).isEqualTo(bodyLayout.getLineDescent(0))
    }

    // Branch: empty body range (start == end), position == AnnotationPosition.After, plain Paint.
    // Initializes workingPaintCache from plain Paint on an empty body range with ruby under.
    @Test
    fun kex_rubyLayout_init_emptyRangePositionAfterWithPlainPaint_expandsBoxDescentDownward() {
        val plainPaint = Paint().apply { textSize = 40f }
        val text = SpannableString("Hello")
        val ruby = "Ruby"
        val layout =
            HorizontalRubySpanLayout(
                text = text,
                start = 0,
                end = 0,
                rubyText = ruby,
                position = AnnotationPosition.After,
                paint = plainPaint,
                rubyScale = 0.5f,
            )

        val refTextPaint = TextPaint().apply { set(plainPaint) }
        val bodyLayout = StaticLayout.Builder.obtain("", 0, 0, refTextPaint, 0).build()
        val rubyPaint = TextPaint(refTextPaint).apply { textSize *= 0.5f }
        val expectedRubyWidth =
            ceil(Layout.getDesiredWidth(ruby, 0, ruby.length, rubyPaint)).toInt()
        val rubyLayout =
            StaticLayout.Builder.obtain(ruby, 0, ruby.length, rubyPaint, expectedRubyWidth).build()
        val rubyLineHeight = rubyLayout.getLineDescent(0) - rubyLayout.getLineAscent(0)

        val fm = Paint.FontMetricsInt()
        layout.fillFontMetrics(fm)
        assertThat(layout.spanWidth).isEqualTo(expectedRubyWidth)
        assertThat(fm.ascent).isEqualTo(bodyLayout.getLineAscent(0))
        assertThat(fm.descent).isEqualTo(bodyLayout.getLineDescent(0) + rubyLineHeight)
    }

    // Branch: non-empty body range, position == AnnotationPosition.Unknown, TextPaint.
    // Proves fallback `position != AnnotationPosition.After` treats Unknown as line-over.
    @Test
    fun kex_rubyLayout_init_positionUnknownWithTextPaint_treatsAsRubyOver() {
        val paint = TextPaint().apply { textSize = 40f }
        val text = SpannableString("Hello")
        val ruby = "Ruby"

        val layoutUnknown =
            HorizontalRubySpanLayout(
                text = text,
                start = 0,
                end = text.length,
                rubyText = ruby,
                position = AnnotationPosition.Unknown,
                paint = paint,
                rubyScale = 0.5f,
            )
        val layoutBefore =
            HorizontalRubySpanLayout(
                text = text,
                start = 0,
                end = text.length,
                rubyText = ruby,
                position = AnnotationPosition.Before,
                paint = paint,
                rubyScale = 0.5f,
            )

        val fmUnknown = Paint.FontMetricsInt()
        val fmBefore = Paint.FontMetricsInt()
        layoutUnknown.fillFontMetrics(fmUnknown)
        layoutBefore.fillFontMetrics(fmBefore)

        assertThat(fmUnknown.ascent).isEqualTo(fmBefore.ascent)
        assertThat(fmUnknown.descent).isEqualTo(fmBefore.descent)
    }

    // Branch: non-empty body range, position == AnnotationPosition.Before, plain Paint when
    // workingPaintCache already holds a dirty TextPaint instance.
    @Test
    fun kex_rubyLayout_init_positionBeforeWithPlainPaintAndDirtyCache_resetsCacheAndMeasures() {
        workingPaintCache.set(
            TextPaint().apply {
                bgColor = Color.RED
                baselineShift = 25
                linkColor = Color.BLUE
            }
        )
        val plainPaint = Paint().apply { textSize = 40f }
        val text = SpannableString("Hello")
        val ruby = "Ruby"

        val layout =
            HorizontalRubySpanLayout(
                text = text,
                start = 0,
                end = text.length,
                rubyText = ruby,
                position = AnnotationPosition.Before,
                paint = plainPaint,
                rubyScale = 0.5f,
            )

        assertThat(layout.spanWidth).isGreaterThan(0)
        assertThat(workingPaintCache.get()?.bgColor).isEqualTo(Color.TRANSPARENT)
        assertThat(workingPaintCache.get()?.baselineShift).isEqualTo(0)
        assertThat(workingPaintCache.get()?.linkColor).isEqualTo(0)
    }

    // Branch: non-empty body range, position == AnnotationPosition.Before, plain Paint on
    // cold workingPaintCache.
    @Test
    fun kex_rubyLayout_init_positionBeforeWithPlainPaint_expandsBoxAscentUpward() {
        val plainPaint = Paint().apply { textSize = 40f }
        val text = SpannableString("Hello")
        val ruby = "Ruby"
        val layout =
            HorizontalRubySpanLayout(
                text = text,
                start = 0,
                end = text.length,
                rubyText = ruby,
                position = AnnotationPosition.Before,
                paint = plainPaint,
                rubyScale = 0.5f,
            )

        val refTextPaint = TextPaint().apply { set(plainPaint) }
        val bodyLayout =
            StaticLayout.Builder.obtain(text, 0, text.length, refTextPaint, Int.MAX_VALUE).build()
        val rubyPaint = TextPaint(refTextPaint).apply { textSize *= 0.5f }
        val rubyLayout =
            StaticLayout.Builder.obtain(ruby, 0, ruby.length, rubyPaint, Int.MAX_VALUE).build()
        val rubyLineHeight = rubyLayout.getLineDescent(0) - rubyLayout.getLineAscent(0)

        val fm = Paint.FontMetricsInt()
        layout.fillFontMetrics(fm)
        assertThat(fm.ascent).isEqualTo(bodyLayout.getLineAscent(0) - rubyLineHeight)
        assertThat(fm.descent).isEqualTo(bodyLayout.getLineDescent(0))
    }

    // Branch: non-empty body range, position == AnnotationPosition.Unknown, plain Paint.
    // Treats Unknown as ruby-over when initialized with a plain Paint.
    @Test
    fun kex_rubyLayout_init_positionUnknownWithPlainPaint_treatsAsRubyOver() {
        val plainPaint = Paint().apply { textSize = 40f }
        val text = SpannableString("Hello")
        val ruby = "Ruby"

        val layoutUnknown =
            HorizontalRubySpanLayout(
                text = text,
                start = 0,
                end = text.length,
                rubyText = ruby,
                position = AnnotationPosition.Unknown,
                paint = plainPaint,
                rubyScale = 0.5f,
            )
        val layoutBefore =
            HorizontalRubySpanLayout(
                text = text,
                start = 0,
                end = text.length,
                rubyText = ruby,
                position = AnnotationPosition.Before,
                paint = plainPaint,
                rubyScale = 0.5f,
            )

        val fmUnknown = Paint.FontMetricsInt()
        val fmBefore = Paint.FontMetricsInt()
        layoutUnknown.fillFontMetrics(fmUnknown)
        layoutBefore.fillFontMetrics(fmBefore)

        assertThat(fmUnknown.ascent).isEqualTo(fmBefore.ascent)
        assertThat(fmUnknown.descent).isEqualTo(fmBefore.descent)
    }

    // Branch: empty body range (start == end), position == AnnotationPosition.Unknown,
    // plain Paint. Treats Unknown as ruby-over on an empty body range.
    @Test
    fun kex_rubyLayout_init_emptyRangePositionUnknownWithPlainPaint_treatsAsRubyOver() {
        val plainPaint = Paint().apply { textSize = 40f }
        val text = SpannableString("Hello")
        val ruby = "Ruby"

        val layoutUnknown =
            HorizontalRubySpanLayout(
                text = text,
                start = 0,
                end = 0,
                rubyText = ruby,
                position = AnnotationPosition.Unknown,
                paint = plainPaint,
                rubyScale = 0.5f,
            )
        val layoutBefore =
            HorizontalRubySpanLayout(
                text = text,
                start = 0,
                end = 0,
                rubyText = ruby,
                position = AnnotationPosition.Before,
                paint = plainPaint,
                rubyScale = 0.5f,
            )

        val fmUnknown = Paint.FontMetricsInt()
        val fmBefore = Paint.FontMetricsInt()
        layoutUnknown.fillFontMetrics(fmUnknown)
        layoutBefore.fillFontMetrics(fmBefore)

        assertThat(layoutUnknown.spanWidth).isEqualTo(layoutBefore.spanWidth)
        assertThat(fmUnknown.ascent).isEqualTo(fmBefore.ascent)
        assertThat(fmUnknown.descent).isEqualTo(fmBefore.descent)
    }

    // Branch: isRubyOver == true, paint is TextPaint with bgColor != 0.
    // Fills the span background box once and resets bgColor and baselineShift on layout paints.
    @Test
    fun kex_rubyLayout_draw_rubyOverWithNonZeroBgColor_drawsBackgroundAndResetsLayoutPaint() {
        val paint = TextPaint().apply { textSize = 40f }
        val text = SpannableString("Hello")
        val ruby = "Ruby"
        val layout =
            HorizontalRubySpanLayout(
                text = text,
                start = 0,
                end = text.length,
                rubyText = ruby,
                position = AnnotationPosition.Before,
                paint = paint,
                rubyScale = 0.5f,
            )
        val fm = Paint.FontMetricsInt()
        layout.fillFontMetrics(fm)

        val drawnRects = mutableListOf<RectF>()
        val drawnColors = mutableListOf<Int>()
        val canvas = recordingCanvas(rects = drawnRects, rectColors = drawnColors)
        val drawPaint =
            TextPaint().apply {
                textSize = 40f
                bgColor = Color.YELLOW
                baselineShift = 15
            }

        layout.draw(canvas, 10f, 100f, drawPaint)

        assertThat(drawnRects)
            .containsExactly(
                RectF(10f, 100f + fm.ascent, 10f + layout.spanWidth, 100f + fm.descent)
            )
        assertThat(drawnColors).containsExactly(Color.YELLOW)
        assertThat(workingPaintCache.get()?.bgColor).isEqualTo(Color.TRANSPARENT)
        assertThat(workingPaintCache.get()?.baselineShift).isEqualTo(0)
    }

    // Branch: isRubyOver == true, paint is TextPaint with bgColor == 0.
    // Skips background rect fill and draws body and ruby text above the body line.
    @Test
    fun kex_rubyLayout_draw_rubyOverWithZeroBgColor_skipsBackgroundRect() {
        val paint = TextPaint().apply { textSize = 40f }
        val text = SpannableString("Hello")
        val layout =
            HorizontalRubySpanLayout(
                text = text,
                start = 0,
                end = text.length,
                rubyText = "Ruby",
                position = AnnotationPosition.Before,
                paint = paint,
                rubyScale = 0.5f,
            )

        val drawnRects = mutableListOf<RectF>()
        val canvas = recordingCanvas(rects = drawnRects)
        val drawPaint =
            TextPaint().apply {
                textSize = 40f
                bgColor = Color.TRANSPARENT
            }

        layout.draw(canvas, 10f, 100f, drawPaint)

        assertThat(drawnRects).isEmpty()
    }

    // Branch: isRubyOver == true, paint !is TextPaint (plain Paint).
    // Skips background rect fill and resets working TextPaint fields from defaultTextPaint.
    @Test
    fun kex_rubyLayout_draw_rubyOverWithPlainPaint_skipsBackgroundAndResetsTextPaintFields() {
        val paint =
            TextPaint().apply {
                textSize = 40f
                linkColor = Color.BLUE
            }
        val text = SpannableString("Hello")
        val layout =
            HorizontalRubySpanLayout(
                text = text,
                start = 0,
                end = text.length,
                rubyText = "Ruby",
                position = AnnotationPosition.Before,
                paint = paint,
                rubyScale = 0.5f,
            )

        val drawnRects = mutableListOf<RectF>()
        val canvas = recordingCanvas(rects = drawnRects)
        val plainDrawPaint = Paint().apply { textSize = 40f }

        layout.draw(canvas, 10f, 100f, plainDrawPaint)

        assertThat(drawnRects).isEmpty()
        assertThat(workingPaintCache.get()?.linkColor).isEqualTo(0)
    }

    // Branch: isRubyOver == false (AnnotationPosition.After), paint is TextPaint with bgColor != 0.
    // Fills the span background box including the lower ruby band.
    @Test
    fun kex_rubyLayout_draw_rubyUnderWithNonZeroBgColor_drawsLowerExpandedBackgroundBox() {
        val paint = TextPaint().apply { textSize = 40f }
        val text = SpannableString("Hello")
        val layout =
            HorizontalRubySpanLayout(
                text = text,
                start = 0,
                end = text.length,
                rubyText = "Ruby",
                position = AnnotationPosition.After,
                paint = paint,
                rubyScale = 0.5f,
            )
        val fm = Paint.FontMetricsInt()
        layout.fillFontMetrics(fm)

        val drawnRects = mutableListOf<RectF>()
        val drawnColors = mutableListOf<Int>()
        val canvas = recordingCanvas(rects = drawnRects, rectColors = drawnColors)
        val drawPaint =
            TextPaint().apply {
                textSize = 40f
                bgColor = Color.CYAN
            }

        layout.draw(canvas, 20f, 120f, drawPaint)

        assertThat(drawnRects)
            .containsExactly(
                RectF(20f, 120f + fm.ascent, 20f + layout.spanWidth, 120f + fm.descent)
            )
        assertThat(drawnColors).containsExactly(Color.CYAN)
    }

    // Branch: isRubyOver == false (AnnotationPosition.After), paint is TextPaint with bgColor == 0.
    // Skips background rect fill and draws ruby text below the body line.
    @Test
    fun kex_rubyLayout_draw_rubyUnderWithZeroBgColor_skipsBackgroundRect() {
        val paint = TextPaint().apply { textSize = 40f }
        val text = SpannableString("Hello")
        val layout =
            HorizontalRubySpanLayout(
                text = text,
                start = 0,
                end = text.length,
                rubyText = "Ruby",
                position = AnnotationPosition.After,
                paint = paint,
                rubyScale = 0.5f,
            )

        val drawnRects = mutableListOf<RectF>()
        val canvas = recordingCanvas(rects = drawnRects)
        val drawPaint =
            TextPaint().apply {
                textSize = 40f
                bgColor = Color.TRANSPARENT
            }

        layout.draw(canvas, 20f, 120f, drawPaint)

        assertThat(drawnRects).isEmpty()
    }

    // Branch: isRubyOver == false (AnnotationPosition.After), paint !is TextPaint (plain Paint).
    // Skips background rect fill and resets working TextPaint fields.
    @Test
    fun kex_rubyLayout_draw_rubyUnderWithPlainPaint_skipsBackgroundAndResetsTextPaintFields() {
        val paint =
            TextPaint().apply {
                textSize = 40f
                linkColor = Color.RED
            }
        val text = SpannableString("Hello")
        val layout =
            HorizontalRubySpanLayout(
                text = text,
                start = 0,
                end = text.length,
                rubyText = "Ruby",
                position = AnnotationPosition.After,
                paint = paint,
                rubyScale = 0.5f,
            )

        val drawnRects = mutableListOf<RectF>()
        val canvas = recordingCanvas(rects = drawnRects)
        val plainDrawPaint = Paint().apply { textSize = 40f }

        layout.draw(canvas, 20f, 120f, plainDrawPaint)

        assertThat(drawnRects).isEmpty()
        assertThat(workingPaintCache.get()?.linkColor).isEqualTo(0)
    }

    private fun recordingCanvas(
        rects: MutableList<RectF>? = null,
        rectColors: MutableList<Int>? = null,
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
                rectColors?.add(paint.color)
            }
        }
    }
}
