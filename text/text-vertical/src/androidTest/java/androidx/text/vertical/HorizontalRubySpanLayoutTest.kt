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
import android.graphics.Paint
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.SpannedString
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.RelativeSizeSpan
import android.text.style.SuperscriptSpan
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.ceil
import kotlin.math.max
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class HorizontalRubySpanLayoutTest {
    private lateinit var paint: TextPaint
    private val text = SpannedString("Hello")
    private val rubyText = "World"

    @Before
    fun setup() {
        paint = TextPaint().apply { textSize = 20f }
    }

    @Test
    fun getSpanWidth_basic_calculation() {
        val layout =
            HorizontalRubySpanLayout(
                text,
                0,
                text.length,
                rubyText,
                AnnotationPosition.Before,
                paint,
                1.0f,
            )
        val bodyWidth = ceil(paint.measureText(text, 0, text.length)).toInt()
        val rubyWidth = ceil(paint.measureText(rubyText, 0, rubyText.length)).toInt()
        assertThat(layout.spanWidth).isEqualTo(max(bodyWidth, rubyWidth))
    }

    @Test
    fun getSpanWidth_with_styled_text() {
        val spannable = SpannableString(text)
        spannable.setSpan(RelativeSizeSpan(2.0f), 0, text.length, 0)

        val layout =
            HorizontalRubySpanLayout(
                spannable,
                0,
                text.length,
                rubyText,
                AnnotationPosition.Before,
                paint,
                1.0f,
            )
        val expectedWidth =
            ceil(Layout.getDesiredWidth(spannable, 0, spannable.length, paint)).toInt()

        assertThat(layout.spanWidth).isEqualTo(expectedWidth)
    }

    @Test
    fun fillFontMetrics_metrics_expansion() {
        val layout =
            HorizontalRubySpanLayout(
                text,
                0,
                text.length,
                rubyText,
                AnnotationPosition.Before,
                paint,
                1.0f,
            )
        val bodyLayout =
            StaticLayout.Builder.obtain(text, 0, text.length, paint, Integer.MAX_VALUE).build()
        val rubyLayout =
            StaticLayout.Builder.obtain(rubyText, 0, rubyText.length, paint, Integer.MAX_VALUE)
                .build()

        val bodyAscent = bodyLayout.getLineAscent(0)
        val bodyDescent = bodyLayout.getLineDescent(0)
        val rubyLineHeight = rubyLayout.getLineDescent(0) - rubyLayout.getLineAscent(0)

        val fm = Paint.FontMetricsInt()
        layout.fillFontMetrics(fm)
        assertThat(fm.ascent).isEqualTo(bodyAscent - rubyLineHeight)
        assertThat(fm.descent).isEqualTo(bodyDescent)
        assertThat(fm.top).isEqualTo(bodyAscent - rubyLineHeight)
        assertThat(fm.bottom).isEqualTo(bodyDescent)
    }

    @Test
    fun constructor_restoresWorkingPaintTextSize() {
        val callerPaint = TextPaint().apply { textSize = 100f }

        HorizontalRubySpanLayout(
            text,
            0,
            text.length,
            rubyText,
            AnnotationPosition.Before,
            callerPaint,
            0.5f,
        )

        assertThat(workingPaintCache.get()?.textSize).isEqualTo(100f)
    }

    @Test
    fun constructor_copiesTextPaintFieldsToWorkingPaint() {
        val callerPaint =
            TextPaint().apply {
                textSize = 100f
                linkColor = 0xFF445566.toInt()
                density = 2.5f
                drawableState = intArrayOf(android.R.attr.state_pressed)
            }

        HorizontalRubySpanLayout(
            text,
            0,
            text.length,
            rubyText,
            AnnotationPosition.Before,
            callerPaint,
            0.5f,
        )

        // The Layout constructor sets baselineShift and bgColor on workPaint to 0 for all text.
        // Thus, this test checks linkColor, density, and drawableState.
        // draw_copiesTextPaintFieldsAndResetsForPlainPaint checks baselineShift and bgColor.
        val workPaint = workingPaintCache.get()!!
        assertThat(workPaint.linkColor).isEqualTo(0xFF445566.toInt())
        assertThat(workPaint.density).isEqualTo(2.5f)
        assertThat(workPaint.drawableState).isEqualTo(intArrayOf(android.R.attr.state_pressed))
    }

    @Test
    fun constructor_resetsTextPaintFieldsForPlainPaint() {
        HorizontalRubySpanLayout(
            text,
            0,
            text.length,
            rubyText,
            AnnotationPosition.Before,
            TextPaint().apply {
                textSize = 100f
                linkColor = 0xFF445566.toInt()
                density = 2.5f
                drawableState = intArrayOf(android.R.attr.state_pressed)
            },
            0.5f,
        )

        HorizontalRubySpanLayout(
            text,
            0,
            text.length,
            rubyText,
            AnnotationPosition.Before,
            Paint().apply { textSize = 100f },
            0.5f,
        )

        // The Layout constructor sets baselineShift and bgColor to 0. Thus, this test checks
        // linkColor, density, and drawableState.
        val workPaint = workingPaintCache.get()!!
        assertThat(workPaint.linkColor).isEqualTo(0)
        assertThat(workPaint.density).isEqualTo(1.0f)
        assertThat(workPaint.drawableState).isNull()
    }

    @Test
    fun draw_copiesTextPaintFieldsAndResetsForPlainPaint() {
        // TextLine copies the layout paint and then calls CharacterStyle.updateDrawState. Thus,
        // each recorder gets a copy of its layout paint, also if the layouts use different paints.
        val bodyPaints = mutableListOf<TextPaint>()
        val rubyPaints = mutableListOf<TextPaint>()
        val bodyText =
            SpannableString(text).apply {
                setSpan(paintRecorder(bodyPaints), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        val recordedRubyText =
            SpannableString(rubyText).apply {
                setSpan(paintRecorder(rubyPaints), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        val layout =
            HorizontalRubySpanLayout(
                bodyText,
                0,
                bodyText.length,
                recordedRubyText,
                AnnotationPosition.Before,
                TextPaint().apply { textSize = 100f },
                0.5f,
            )
        val bitmap = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val richDrawPaint =
            TextPaint().apply {
                textSize = 100f
                baselineShift = 18
                bgColor = 0xFFAA0000.toInt()
                linkColor = 0xFF00BB00.toInt()
                density = 3.0f
                drawableState = intArrayOf(android.R.attr.state_pressed)
            }
        layout.draw(canvas, 0f, 500f, richDrawPaint)

        // draw() sets baselineShift to 0. draw_appliesSuperscriptBaselineShiftToBodyOnce shows why.
        for (recorded in listOf(bodyPaints.last(), rubyPaints.last())) {
            assertThat(recorded.baselineShift).isEqualTo(0)
            assertThat(recorded.bgColor).isEqualTo(0xFFAA0000.toInt())
            assertThat(recorded.linkColor).isEqualTo(0xFF00BB00.toInt())
            assertThat(recorded.density).isEqualTo(3.0f)
            assertThat(recorded.drawableState).isEqualTo(intArrayOf(android.R.attr.state_pressed))
        }

        bodyPaints.clear()
        rubyPaints.clear()
        layout.draw(canvas, 0f, 500f, Paint().apply { textSize = 100f })
        bitmap.recycle()

        for (recorded in listOf(bodyPaints.last(), rubyPaints.last())) {
            assertThat(recorded.baselineShift).isEqualTo(0)
            assertThat(recorded.bgColor).isEqualTo(0)
            assertThat(recorded.linkColor).isEqualTo(0)
            assertThat(recorded.density).isEqualTo(1.0f)
            assertThat(recorded.drawableState).isNull()
        }
    }

    @Test
    fun draw_appliesSuperscriptBaselineShiftToBodyOnce() {
        val superscriptText =
            SpannableString(text).apply {
                setSpan(SuperscriptSpan(), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        val layout =
            HorizontalRubySpanLayout(
                superscriptText,
                0,
                superscriptText.length,
                rubyText,
                AnnotationPosition.Before,
                TextPaint().apply { textSize = 100f },
                0.5f,
            )

        // In horizontal text, TextLine applies the SuperscriptSpan to the paint that it gives to
        // the replacement span. The body layout keeps the SuperscriptSpan and applies it again.
        val superscriptPaint =
            TextPaint().apply {
                textSize = 100f
                SuperscriptSpan().updateDrawState(this)
            }
        val plainPaint = TextPaint().apply { textSize = 100f }

        val superscriptYs = recordTextRunYs(layout, superscriptPaint)
        assertThat(superscriptYs).isNotEmpty()
        assertThat(superscriptYs).isEqualTo(recordTextRunYs(layout, plainPaint))
    }

    /**
     * Draws [layout] and returns the y of each text run. Only the body layout draws text runs,
     * because the ruby text has no spans and [Layout] draws it with [Canvas.drawText].
     */
    private fun recordTextRunYs(layout: HorizontalSpanLayout, paint: Paint): List<Float> {
        val ys = mutableListOf<Float>()
        // Layout.draw skips lines outside the clip, so the canvas needs a bitmap.
        val bitmap = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888)
        val canvas =
            object : Canvas(bitmap) {
                override fun drawTextRun(
                    text: CharArray,
                    index: Int,
                    count: Int,
                    contextIndex: Int,
                    contextCount: Int,
                    x: Float,
                    y: Float,
                    isRtl: Boolean,
                    paint: Paint,
                ) {
                    ys.add(y)
                }

                override fun drawTextRun(
                    text: CharSequence,
                    start: Int,
                    end: Int,
                    contextStart: Int,
                    contextEnd: Int,
                    x: Float,
                    y: Float,
                    isRtl: Boolean,
                    paint: Paint,
                ) {
                    ys.add(y)
                }
            }
        layout.draw(canvas, 0f, 500f, paint)
        bitmap.recycle()
        return ys
    }

    /** Returns a span that adds a copy of each paint it gets to [paints]. */
    private fun paintRecorder(paints: MutableList<TextPaint>) =
        object : CharacterStyle() {
            override fun updateDrawState(tp: TextPaint) {
                paints.add(TextPaint().apply { set(tp) })
            }
        }
}
