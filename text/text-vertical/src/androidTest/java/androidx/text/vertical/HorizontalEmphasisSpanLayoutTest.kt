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
import android.text.style.RelativeSizeSpan
import android.text.style.SuperscriptSpan
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.ceil
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class HorizontalEmphasisSpanLayoutTest {
    private lateinit var paint: TextPaint
    private val text = SpannedString("Hello")
    private val emphasisMark = "•"

    @Before
    fun setup() {
        paint = TextPaint().apply { textSize = 20f }
    }

    @Test
    fun getSpanWidth_basicCalculation() {
        val layout =
            HorizontalEmphasisSpanLayout(
                text,
                0,
                text.length,
                emphasisMark,
                AnnotationPosition.Before,
                paint,
                1.0f,
            )
        val expectedWidth = ceil(paint.measureText(text, 0, text.length)).toInt()
        assertThat(layout.spanWidth).isEqualTo(expectedWidth)
    }

    @Test
    fun getSpanWidth_withStyledText() {
        val spannable = SpannableString(text)
        spannable.setSpan(RelativeSizeSpan(2.0f), 0, text.length, 0)

        val layout =
            HorizontalEmphasisSpanLayout(
                spannable,
                0,
                text.length,
                emphasisMark,
                AnnotationPosition.Before,
                paint,
                1.0f,
            )
        val expectedWidth =
            ceil(Layout.getDesiredWidth(spannable, 0, spannable.length, paint)).toInt()

        assertThat(layout.spanWidth).isEqualTo(expectedWidth)
    }

    @Test
    fun fillFontMetrics_metricsExpansion() {
        val layout =
            HorizontalEmphasisSpanLayout(
                text,
                0,
                text.length,
                emphasisMark,
                AnnotationPosition.Before,
                paint,
                1.0f,
            )
        val bodyLayout =
            StaticLayout.Builder.obtain(text, 0, text.length, paint, Integer.MAX_VALUE).build()
        val emLayout =
            StaticLayout.Builder.obtain(
                    emphasisMark,
                    0,
                    emphasisMark.length,
                    paint,
                    Integer.MAX_VALUE,
                )
                .build()

        val bodyAscent = bodyLayout.getLineAscent(0)
        val bodyDescent = bodyLayout.getLineDescent(0)
        val emLineHeight = emLayout.getLineDescent(0) - emLayout.getLineAscent(0)

        val fm = Paint.FontMetricsInt()
        layout.fillFontMetrics(fm)
        assertThat(fm.ascent).isEqualTo(bodyAscent - emLineHeight)
        assertThat(fm.descent).isEqualTo(bodyDescent)
        assertThat(fm.top).isEqualTo(bodyAscent - emLineHeight)
        assertThat(fm.bottom).isEqualTo(bodyDescent)
    }

    // A relative size of 1.0f cannot show a scaling defect, because scaled and unscaled
    // measurements are equal. These two tests use 0.5f.

    @Test
    fun constructor_doesNotChangeTheCallerTextSize() {
        val callerPaint = TextPaint().apply { textSize = 100f }

        HorizontalEmphasisSpanLayout(
            text,
            0,
            text.length,
            emphasisMark,
            AnnotationPosition.Before,
            callerPaint,
            0.5f,
        )

        assertThat(callerPaint.textSize).isEqualTo(100f)
    }

    @Test
    fun constructor_restoresWorkingPaintTextSize() {
        val callerPaint = TextPaint().apply { textSize = 100f }

        HorizontalEmphasisSpanLayout(
            text,
            0,
            text.length,
            emphasisMark,
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

        HorizontalEmphasisSpanLayout(
            text,
            0,
            text.length,
            emphasisMark,
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
        HorizontalEmphasisSpanLayout(
            text,
            0,
            text.length,
            emphasisMark,
            AnnotationPosition.Before,
            TextPaint().apply {
                textSize = 100f
                linkColor = 0xFF445566.toInt()
                density = 2.5f
                drawableState = intArrayOf(android.R.attr.state_pressed)
            },
            0.5f,
        )

        HorizontalEmphasisSpanLayout(
            text,
            0,
            text.length,
            emphasisMark,
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
        val layout =
            HorizontalEmphasisSpanLayout(
                text,
                0,
                text.length,
                emphasisMark,
                AnnotationPosition.Before,
                TextPaint().apply { textSize = 100f },
                0.5f,
            )

        val richDrawPaint =
            TextPaint().apply {
                textSize = 100f
                baselineShift = 18
                bgColor = 0xFFAA0000.toInt()
                linkColor = 0xFF00BB00.toInt()
                density = 3.0f
            }
        layout.draw(Canvas(), 0f, 0f, richDrawPaint)

        // TODO(b/561269843): bodyLayout.paint currently aliases workingPaintCache. Update this
        // test when b/561269843 gives the layout its own paint.
        // draw() sets baselineShift to 0. draw_appliesSuperscriptBaselineShiftToBodyOnce shows why.
        val workPaintAfterRichDraw = workingPaintCache.get()!!
        assertThat(workPaintAfterRichDraw.baselineShift).isEqualTo(0)
        assertThat(workPaintAfterRichDraw.bgColor).isEqualTo(0xFFAA0000.toInt())
        assertThat(workPaintAfterRichDraw.linkColor).isEqualTo(0xFF00BB00.toInt())
        assertThat(workPaintAfterRichDraw.density).isEqualTo(3.0f)

        val plainPaint = Paint().apply { textSize = 100f }
        layout.draw(Canvas(), 0f, 0f, plainPaint)

        val workPaintAfterPlainDraw = workingPaintCache.get()!!
        assertThat(workPaintAfterPlainDraw.baselineShift).isEqualTo(0)
        assertThat(workPaintAfterPlainDraw.bgColor).isEqualTo(0)
        assertThat(workPaintAfterPlainDraw.linkColor).isEqualTo(0)
        assertThat(workPaintAfterPlainDraw.density).isEqualTo(1.0f)
    }

    @Test
    fun draw_appliesSuperscriptBaselineShiftToBodyOnce() {
        val superscriptText =
            SpannableString(text).apply {
                setSpan(SuperscriptSpan(), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        val layout =
            HorizontalEmphasisSpanLayout(
                superscriptText,
                0,
                superscriptText.length,
                emphasisMark,
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

    @Test
    fun fillFontMetrics_reservesSpaceForTheScaledMark() {
        val relSize = 0.5f
        val basePaint = TextPaint().apply { textSize = 100f }
        val layout =
            HorizontalEmphasisSpanLayout(
                text,
                0,
                text.length,
                emphasisMark,
                AnnotationPosition.Before,
                TextPaint(basePaint),
                relSize,
            )
        val bodyLayout =
            StaticLayout.Builder.obtain(text, 0, text.length, basePaint, Integer.MAX_VALUE).build()
        val scaledPaint = TextPaint(basePaint).apply { textSize = basePaint.textSize * relSize }
        val markLayout =
            StaticLayout.Builder.obtain(
                    emphasisMark,
                    0,
                    emphasisMark.length,
                    scaledPaint,
                    Integer.MAX_VALUE,
                )
                .build()
        val markLineHeight = markLayout.getLineDescent(0) - markLayout.getLineAscent(0)

        val fm = Paint.FontMetricsInt()
        layout.fillFontMetrics(fm)

        assertThat(fm.ascent).isEqualTo(bodyLayout.getLineAscent(0) - markLineHeight)
        assertThat(fm.descent).isEqualTo(bodyLayout.getLineDescent(0))
        assertThat(fm.top).isEqualTo(bodyLayout.getLineAscent(0) - markLineHeight)
        assertThat(fm.bottom).isEqualTo(bodyLayout.getLineDescent(0))
    }

    @Test
    fun draw_positionBefore_placesMarksAtExactBaselineAndHorizontalCenter() {
        assertDrawMarkCoordinates(AnnotationPosition.Before)
    }

    @Test
    fun draw_positionAfter_placesMarksAtExactBaselineAndHorizontalCenter() {
        assertDrawMarkCoordinates(AnnotationPosition.After)
    }

    private fun assertDrawMarkCoordinates(position: AnnotationPosition) {
        val relSize = 0.5f
        val drawX = 15f
        val drawY = 200f
        val basePaint = TextPaint().apply { textSize = 100f }
        val layout =
            HorizontalEmphasisSpanLayout(
                text,
                0,
                text.length,
                emphasisMark,
                position,
                TextPaint(basePaint),
                relSize,
            )
        val bodyWidth = ceil(Layout.getDesiredWidth(text, 0, text.length, basePaint)).toInt()
        val bodyLayout =
            StaticLayout.Builder.obtain(text, 0, text.length, basePaint, bodyWidth).build()
        val scaledPaint = TextPaint(basePaint).apply { textSize = basePaint.textSize * relSize }
        val emphasisWidth =
            ceil(Layout.getDesiredWidth(emphasisMark, 0, emphasisMark.length, scaledPaint)).toInt()
        val emLayout =
            StaticLayout.Builder.obtain(
                    emphasisMark,
                    0,
                    emphasisMark.length,
                    scaledPaint,
                    emphasisWidth,
                )
                .build()
        val markedIndices = (0 until text.length).filter { isEmphasisTarget(text[it].code) }
        val expectedXs = markedIndices.map { i ->
            val charWidth = basePaint.measureText(text, i, i + 1)
            drawX + bodyLayout.getPrimaryHorizontal(i) + (charWidth - emphasisWidth) / 2f
        }

        val drawnXs = mutableListOf<Float>()
        val drawnYs = mutableListOf<Float>()
        val canvas =
            object : Canvas() {
                override fun drawText(text: String, x: Float, y: Float, paint: Paint) {
                    if (text == emphasisMark) {
                        drawnXs.add(x)
                        drawnYs.add(y)
                    }
                }
            }

        layout.draw(canvas, drawX, drawY, TextPaint(basePaint))

        assertThat(drawnYs).hasSize(markedIndices.size)
        assertThat(drawnYs.distinct()).hasSize(1)
        val markTop = drawnYs.first() + emLayout.getLineAscent(0)
        val markBottom = drawnYs.first() + emLayout.getLineDescent(0)
        if (position == AnnotationPosition.Before) {
            assertThat(markBottom).isEqualTo(drawY + bodyLayout.getLineAscent(0))
        } else {
            assertThat(markTop).isEqualTo(drawY + bodyLayout.getLineDescent(0))
        }
        assertThat(drawnXs).containsExactlyElementsIn(expectedXs).inOrder()
    }

    /**
     * Draws [layout] and returns the y of each text run. Only the body layout draws text runs,
     * because the emphasis marks are drawn with [Canvas.drawText].
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
}
