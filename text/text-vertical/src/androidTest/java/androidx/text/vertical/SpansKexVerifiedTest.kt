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
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests branches in `AnnotationPosition.kt`, `EmphasisStyle.kt`, `EmphasisSpan.kt`,
 * `FontShearSpan.kt`, and `RubySpan.kt` derived from Kex symbolic execution paths.
 *
 * Covers [AnnotationPosition.fromInt], [EmphasisStyle.fromInt], [EmphasisSpan], [FontShearSpan],
 * and [RubySpan].
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class SpansKexVerifiedTest {

    private val paint = TextPaint().apply { textSize = 20f }
    private var canvasBitmap: Bitmap? = null
    private var canvasDrawCount = 0

    @After
    fun recycleCanvasBitmap() {
        canvasBitmap?.recycle()
        canvasBitmap = null
    }

    private fun createTestCanvas(): Canvas {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        canvasBitmap = bitmap
        canvasDrawCount = 0
        return object : Canvas(bitmap) {
            override fun translate(dx: Float, dy: Float) {
                canvasDrawCount++
                super.translate(dx, dy)
            }

            override fun drawText(text: String, x: Float, y: Float, paint: Paint) {
                canvasDrawCount++
                super.drawText(text, x, y, paint)
            }

            override fun drawText(
                text: CharSequence,
                start: Int,
                end: Int,
                x: Float,
                y: Float,
                paint: Paint,
            ) {
                canvasDrawCount++
                super.drawText(text, start, end, x, y, paint)
            }
        }
    }

    // =========================================================================================
    // Target 1: AnnotationPosition
    // =========================================================================================

    // Branch: value == Before.value (0). Returns AnnotationPosition.Before.
    @Test
    fun kex_annotationPosition_fromInt_whenBeforeValue_returnsBefore() {
        val result = AnnotationPosition.fromInt(AnnotationPosition.Before.value)

        assertThat(result).isSameInstanceAs(AnnotationPosition.Before)
        assertThat(result.value).isEqualTo(0)
    }

    // Branch: value == After.value (1). Returns AnnotationPosition.After.
    @Test
    fun kex_annotationPosition_fromInt_whenAfterValue_returnsAfter() {
        val result = AnnotationPosition.fromInt(AnnotationPosition.After.value)

        assertThat(result).isSameInstanceAs(AnnotationPosition.After)
        assertThat(result.value).isEqualTo(1)
    }

    // Branch: else (value is neither Before.value nor After.value). Returns
    // AnnotationPosition.Unknown.
    @Test
    fun kex_annotationPosition_fromInt_whenUnknownOrOutOfBounds_returnsUnknown() {
        assertThat(AnnotationPosition.fromInt(AnnotationPosition.Unknown.value))
            .isSameInstanceAs(AnnotationPosition.Unknown)
        assertThat(AnnotationPosition.Unknown.value).isEqualTo(-1)
        assertThat(AnnotationPosition.fromInt(2)).isSameInstanceAs(AnnotationPosition.Unknown)
        assertThat(AnnotationPosition.fromInt(Int.MIN_VALUE))
            .isSameInstanceAs(AnnotationPosition.Unknown)
        assertThat(AnnotationPosition.fromInt(Int.MAX_VALUE))
            .isSameInstanceAs(AnnotationPosition.Unknown)
    }

    // =========================================================================================
    // Target 2: EmphasisStyle
    // =========================================================================================

    // Branch: value == Dot.value (1). Returns EmphasisStyle.Dot.
    @Test
    fun kex_emphasisStyle_fromInt_whenDotValue_returnsDot() {
        val result = EmphasisStyle.fromInt(EmphasisStyle.Dot.value)

        assertThat(result).isSameInstanceAs(EmphasisStyle.Dot)
        assertThat(result.value).isEqualTo(1)
    }

    // Branch: value == Circle.value (2). Returns EmphasisStyle.Circle.
    @Test
    fun kex_emphasisStyle_fromInt_whenCircleValue_returnsCircle() {
        val result = EmphasisStyle.fromInt(EmphasisStyle.Circle.value)

        assertThat(result).isSameInstanceAs(EmphasisStyle.Circle)
        assertThat(result.value).isEqualTo(2)
    }

    // Branch: value == DoubleCircle.value (3). Returns EmphasisStyle.DoubleCircle.
    @Test
    fun kex_emphasisStyle_fromInt_whenDoubleCircleValue_returnsDoubleCircle() {
        val result = EmphasisStyle.fromInt(EmphasisStyle.DoubleCircle.value)

        assertThat(result).isSameInstanceAs(EmphasisStyle.DoubleCircle)
        assertThat(result.value).isEqualTo(3)
    }

    // Branch: value == Triangle.value (4). Returns EmphasisStyle.Triangle.
    @Test
    fun kex_emphasisStyle_fromInt_whenTriangleValue_returnsTriangle() {
        val result = EmphasisStyle.fromInt(EmphasisStyle.Triangle.value)

        assertThat(result).isSameInstanceAs(EmphasisStyle.Triangle)
        assertThat(result.value).isEqualTo(4)
    }

    // Branch: value == Sesame.value (5). Returns EmphasisStyle.Sesame.
    @Test
    fun kex_emphasisStyle_fromInt_whenSesameValue_returnsSesame() {
        val result = EmphasisStyle.fromInt(EmphasisStyle.Sesame.value)

        assertThat(result).isSameInstanceAs(EmphasisStyle.Sesame)
        assertThat(result.value).isEqualTo(5)
    }

    // Branch: else (unrecognized integer value). Defaults to EmphasisStyle.Dot.
    @Test
    fun kex_emphasisStyle_fromInt_whenOutOfBounds_defaultsToDot() {
        assertThat(EmphasisStyle.fromInt(0)).isSameInstanceAs(EmphasisStyle.Dot)
        assertThat(EmphasisStyle.fromInt(-1)).isSameInstanceAs(EmphasisStyle.Dot)
        assertThat(EmphasisStyle.fromInt(6)).isSameInstanceAs(EmphasisStyle.Dot)
        assertThat(EmphasisStyle.fromInt(Int.MIN_VALUE)).isSameInstanceAs(EmphasisStyle.Dot)
        assertThat(EmphasisStyle.fromInt(Int.MAX_VALUE)).isSameInstanceAs(EmphasisStyle.Dot)
    }

    // =========================================================================================
    // Target 3: EmphasisSpan
    // =========================================================================================

    // Branch: Default constructor parameters apply DEFAULT_EMPHASIS_STYLE, DEFAULT_EMPHASIS_FILL,
    // DEFAULT_POSITION, and DEFAULT_SCALE.
    @Test
    fun kex_emphasisSpan_defaultConstructor_usesDefaultProperties() {
        val span = EmphasisSpan()

        assertThat(span.style).isSameInstanceAs(EmphasisSpan.DEFAULT_EMPHASIS_STYLE)
        assertThat(span.isFilled).isEqualTo(EmphasisSpan.DEFAULT_EMPHASIS_FILL)
        assertThat(span.position).isSameInstanceAs(EmphasisSpan.DEFAULT_POSITION)
        assertThat(span.scale).isEqualTo(EmphasisSpan.DEFAULT_SCALE)
        assertThat(span.letter).isEqualTo("\u2022")
    }

    // Branch: style == EmphasisStyle.Dot with isFilled == true and isFilled == false.
    @Test
    fun kex_emphasisSpan_letter_whenStyleIsDot_selectsFilledOrOpenBullet() {
        val filled = EmphasisSpan(style = EmphasisStyle.Dot, isFilled = true)
        val open = EmphasisSpan(style = EmphasisStyle.Dot, isFilled = false)

        assertThat(filled.letter).isEqualTo("\u2022")
        assertThat(open.letter).isEqualTo("\u25E6")
    }

    // Branch: style == EmphasisStyle.Circle with isFilled == true and isFilled == false.
    @Test
    fun kex_emphasisSpan_letter_whenStyleIsCircle_selectsFilledOrOpenCircle() {
        val filled = EmphasisSpan(style = EmphasisStyle.Circle, isFilled = true)
        val open = EmphasisSpan(style = EmphasisStyle.Circle, isFilled = false)

        assertThat(filled.letter).isEqualTo("\u25CF")
        assertThat(open.letter).isEqualTo("\u25CB")
    }

    // Branch: style == EmphasisStyle.DoubleCircle with isFilled == true and isFilled == false.
    @Test
    fun kex_emphasisSpan_letter_whenStyleIsDoubleCircle_selectsFilledOrOpenDoubleCircle() {
        val filled = EmphasisSpan(style = EmphasisStyle.DoubleCircle, isFilled = true)
        val open = EmphasisSpan(style = EmphasisStyle.DoubleCircle, isFilled = false)

        assertThat(filled.letter).isEqualTo("\u25C9")
        assertThat(open.letter).isEqualTo("\u25CE")
    }

    // Branch: style == EmphasisStyle.Triangle with isFilled == true and isFilled == false.
    @Test
    fun kex_emphasisSpan_letter_whenStyleIsTriangle_selectsFilledOrOpenTriangle() {
        val filled = EmphasisSpan(style = EmphasisStyle.Triangle, isFilled = true)
        val open = EmphasisSpan(style = EmphasisStyle.Triangle, isFilled = false)

        assertThat(filled.letter).isEqualTo("\u25B2")
        assertThat(open.letter).isEqualTo("\u25B3")
    }

    // Branch: style == EmphasisStyle.Sesame with isFilled == true and isFilled == false.
    @Test
    fun kex_emphasisSpan_letter_whenStyleIsSesame_selectsFilledOrOpenSesame() {
        val filled = EmphasisSpan(style = EmphasisStyle.Sesame, isFilled = true)
        val open = EmphasisSpan(style = EmphasisStyle.Sesame, isFilled = false)

        assertThat(filled.letter).isEqualTo("\uFE45")
        assertThat(open.letter).isEqualTo("\uFE46")
    }

    // Branch: getSize when text == null returns 0 without modifying FontMetricsInt.
    @Test
    fun kex_emphasisSpan_getSize_whenTextIsNull_returnsZero() {
        val span = EmphasisSpan()
        val fontMetrics = Paint.FontMetricsInt().apply { ascent = -5 }

        val size = span.getSize(paint, null, 0, 0, fontMetrics)

        assertThat(size).isEqualTo(0)
        assertThat(fontMetrics.ascent).isEqualTo(-5)
    }

    // Branch: getSize and draw when text is not Spanned throw IllegalArgumentException.
    @Test
    fun kex_emphasisSpan_getSizeAndDraw_whenTextIsNotSpanned_throwIllegalArgumentException() {
        val span = EmphasisSpan()
        val canvas = createTestCanvas()

        assertThrows(IllegalArgumentException::class.java) { span.getSize(paint, "あ", 0, 1, null) }
        assertThrows(IllegalArgumentException::class.java) {
            span.draw(canvas, "あ", 0, 1, 0f, 0, 20, 30, paint)
        }
    }

    // Branch: getSize when text is Spanned delegates to HorizontalEmphasisSpanLayout and populates
    // FontMetricsInt.
    @Test
    fun kex_emphasisSpan_getSize_whenTextIsSpanned_returnsLayoutWidthAndUpdatesMetrics() {
        val span =
            EmphasisSpan(
                style = EmphasisStyle.Circle,
                isFilled = true,
                position = AnnotationPosition.After,
                scale = 0.75f,
            )
        val spanned =
            SpannableString("あ").apply { setSpan(span, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
        val fontMetrics = Paint.FontMetricsInt()

        val size = span.getSize(paint, spanned, 0, 1, fontMetrics)

        assertThat(size).isGreaterThan(0)
        assertThat(fontMetrics.ascent).isLessThan(0)
        assertThat(fontMetrics.descent).isGreaterThan(0)
        assertThat(fontMetrics.top).isEqualTo(fontMetrics.ascent)
        assertThat(fontMetrics.bottom).isEqualTo(fontMetrics.descent)
    }

    // Branch: draw when text == null returns early without drawing onto the canvas.
    @Test
    fun kex_emphasisSpan_draw_whenTextIsNull_doesNotDraw() {
        val span = EmphasisSpan()
        val canvas = createTestCanvas()

        span.draw(canvas, null, 0, 0, 0f, 0, 20, 30, paint)

        assertThat(canvasDrawCount).isEqualTo(0)
    }

    // Branch: draw when text is Spanned delegates to HorizontalEmphasisSpanLayout and draws onto
    // the canvas.
    @Test
    fun kex_emphasisSpan_draw_whenTextIsSpanned_drawsLayout() {
        val span = EmphasisSpan()
        val spanned =
            SpannableString("あ").apply { setSpan(span, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
        val canvas = createTestCanvas()

        span.draw(canvas, spanned, 0, 1, 10f, 0, 50, 80, paint)

        assertThat(canvasDrawCount).isGreaterThan(0)
        assertThat(span.getSize(paint, spanned, 0, 1, null)).isGreaterThan(0)
    }

    // =========================================================================================
    // Target 4: FontShearSpan
    // =========================================================================================

    // Branch: Default constructor uses DEFAULT_FONT_SHEAR.
    @Test
    fun kex_fontShearSpan_defaultConstructor_usesDefaultFontShear() {
        val span = FontShearSpan()

        assertThat(span.fontShear).isEqualTo(FontShearSpan.DEFAULT_FONT_SHEAR)
    }

    // Branch: Explicit constructor stores the caller-supplied fontShear value.
    @Test
    fun kex_fontShearSpan_customConstructor_storesCustomFontShear() {
        val customShear = -0.5f
        val span = FontShearSpan(customShear)

        assertThat(span.fontShear).isEqualTo(customShear)
    }

    // Branch: updateMeasureState and updateDrawState (with non-null and null TextPaint) are no-ops.
    @Test
    fun kex_fontShearSpan_updateMeasureStateAndUpdateDrawState_leaveTextPaintUnchanged() {
        val span = FontShearSpan(0.4f)
        val textPaint =
            TextPaint().apply {
                textSize = 24f
                textSkewX = -0.25f
                color = Color.BLUE
            }

        span.updateMeasureState(textPaint)
        span.updateDrawState(textPaint)
        span.updateDrawState(null)

        assertThat(textPaint.textSize).isEqualTo(24f)
        assertThat(textPaint.textSkewX).isEqualTo(-0.25f)
        assertThat(textPaint.color).isEqualTo(Color.BLUE)
    }

    // =========================================================================================
    // Target 5: RubySpan
    // =========================================================================================

    // Branch: Default constructor parameters apply DEFAULT_POSITION, DEFAULT_ORIENTATION, and
    // DEFAULT_TEXT_SCALE.
    @Test
    fun kex_rubySpan_defaultConstructor_usesDefaultProperties() {
        val span = RubySpan("かな")

        assertThat(span.text.toString()).isEqualTo("かな")
        assertThat(span.position).isSameInstanceAs(RubySpan.DEFAULT_POSITION)
        assertThat(span.orientation).isEqualTo(RubySpan.DEFAULT_ORIENTATION)
        assertThat(span.textScale).isEqualTo(RubySpan.DEFAULT_TEXT_SCALE)
    }

    // Branch: Explicit constructor stores custom position, orientation, and textScale.
    @Test
    fun kex_rubySpan_customConstructor_storesCustomProperties() {
        val span =
            RubySpan(
                text = "かな",
                position = AnnotationPosition.After,
                orientation = TextOrientation.Upright,
                textScale = 0.75f,
            )

        assertThat(span.text.toString()).isEqualTo("かな")
        assertThat(span.position).isSameInstanceAs(AnnotationPosition.After)
        assertThat(span.orientation).isEqualTo(TextOrientation.Upright)
        assertThat(span.textScale).isEqualTo(0.75f)
    }

    // Branch: getSize when text == null returns 0 without modifying FontMetricsInt.
    @Test
    fun kex_rubySpan_getSize_whenTextIsNull_returnsZero() {
        val span = RubySpan("かな")
        val fontMetrics = Paint.FontMetricsInt().apply { ascent = -7 }

        val size = span.getSize(paint, null, 0, 0, fontMetrics)

        assertThat(size).isEqualTo(0)
        assertThat(fontMetrics.ascent).isEqualTo(-7)
    }

    // Branch: getSize and draw when text is not Spanned throw IllegalArgumentException.
    @Test
    fun kex_rubySpan_getSizeAndDraw_whenTextIsNotSpanned_throwIllegalArgumentException() {
        val span = RubySpan("かな")
        val canvas = createTestCanvas()

        assertThrows(IllegalArgumentException::class.java) { span.getSize(paint, "漢字", 0, 2, null) }
        assertThrows(IllegalArgumentException::class.java) {
            span.draw(canvas, "漢字", 0, 2, 0f, 0, 20, 30, paint)
        }
    }

    // Branch: getSize when text is Spanned delegates to HorizontalRubySpanLayout and populates
    // FontMetricsInt.
    @Test
    fun kex_rubySpan_getSize_whenTextIsSpanned_returnsLayoutWidthAndUpdatesMetrics() {
        val span = RubySpan("かな", AnnotationPosition.After, TextOrientation.Mixed, 0.75f)
        val spanned =
            SpannableString("漢字").apply { setSpan(span, 0, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
        val fontMetrics = Paint.FontMetricsInt()

        val size = span.getSize(paint, spanned, 0, 2, fontMetrics)

        assertThat(size).isGreaterThan(0)
        assertThat(fontMetrics.ascent).isLessThan(0)
        assertThat(fontMetrics.descent).isGreaterThan(0)
        assertThat(fontMetrics.top).isEqualTo(fontMetrics.ascent)
        assertThat(fontMetrics.bottom).isEqualTo(fontMetrics.descent)
    }

    // Branch: draw when text == null returns early without drawing onto the canvas.
    @Test
    fun kex_rubySpan_draw_whenTextIsNull_doesNotDraw() {
        val span = RubySpan("かな")
        val canvas = createTestCanvas()

        span.draw(canvas, null, 0, 0, 0f, 0, 20, 30, paint)

        assertThat(canvasDrawCount).isEqualTo(0)
    }

    // Branch: draw when text is Spanned delegates to HorizontalRubySpanLayout and draws onto the
    // canvas.
    @Test
    fun kex_rubySpan_draw_whenTextIsSpanned_drawsLayout() {
        val span = RubySpan("かな")
        val spanned =
            SpannableString("漢字").apply { setSpan(span, 0, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
        val canvas = createTestCanvas()

        span.draw(canvas, spanned, 0, 2, 10f, 0, 50, 80, paint)

        assertThat(canvasDrawCount).isGreaterThan(0)
        assertThat(span.getSize(paint, spanned, 0, 2, null)).isGreaterThan(0)
    }
}
