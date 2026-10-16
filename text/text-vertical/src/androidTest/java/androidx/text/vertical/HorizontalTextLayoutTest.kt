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
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.style.ForegroundColorSpan
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.text.vertical.RubyPixel.ANTIALIAS_MARGIN
import androidx.text.vertical.RubyPixel.VISIBLE_CHAR
import com.google.common.truth.Truth.assertThat
import kotlin.math.ceil
import kotlin.math.max
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class HorizontalTextLayoutTest {
    private val paint = TextPaint().apply { textSize = 40f }
    private val bitmaps = mutableListOf<Bitmap>()

    @After
    fun recycleBitmaps() {
        bitmaps.forEach { it.recycle() }
    }

    @Test
    fun singleLine_widthEqualsDesiredWidth() {
        val text = "Hello"
        val expectedWidth = ceil(Layout.getDesiredWidth(text, 0, text.length, paint)).toInt()

        val layout =
            HorizontalTextLayout(
                text = text,
                paint = paint,
                minWidth = 0,
                maxWidth = 1000,
                alignment = Layout.Alignment.ALIGN_NORMAL,
                textDir = TextDirectionHeuristics.FIRSTSTRONG_LTR,
            )

        assertThat(layout.lineCount).isEqualTo(1)
        assertThat(layout.width).isEqualTo(expectedWidth)
        assertThat(layout.width).isLessThan(1000)
    }

    @Test
    fun wrappedLines_widthEqualsWidestLine_andPreservesBgColor() {
        val text = "Hello world again"
        val maxWidth = ceil(paint.measureText("Hello world") + 20f).toInt()
        val expectedBgColor = Color.YELLOW
        paint.bgColor = expectedBgColor

        val layout =
            HorizontalTextLayout(
                text = text,
                paint = paint,
                minWidth = 0,
                maxWidth = maxWidth,
                alignment = Layout.Alignment.ALIGN_NORMAL,
                textDir = TextDirectionHeuristics.FIRSTSTRONG_LTR,
            )

        var maxLineWidth = 0f
        for (i in 0 until layout.lineCount) {
            maxLineWidth = max(maxLineWidth, layout.getLineMax(i))
        }
        val expectedWrappedWidth = ceil(maxLineWidth).toInt()
        assertThat(layout.lineCount).isAtLeast(2)
        assertThat(layout.width).isEqualTo(expectedWrappedWidth)
        assertThat(layout.width).isLessThan(maxWidth)
        assertThat(paint.bgColor).isEqualTo(expectedBgColor)
    }

    @Test
    fun wrappedLineWithTrailingSpaces_shrinksToVisibleLineMax() {
        val text = "ab   cd"
        val maxWidth = ceil(paint.measureText("ab ") + 1f).toInt()
        val expectedVisibleWidth =
            ceil(max(paint.measureText("ab"), paint.measureText("cd"))).toInt()

        val layout =
            HorizontalTextLayout(
                text = text,
                paint = paint,
                minWidth = 0,
                maxWidth = maxWidth,
                alignment = Layout.Alignment.ALIGN_NORMAL,
                textDir = TextDirectionHeuristics.FIRSTSTRONG_LTR,
            )

        assertThat(layout.lineCount).isEqualTo(2)
        assertThat(layout.width).isEqualTo(expectedVisibleWidth)
        assertThat(layout.width).isLessThan(maxWidth)
    }

    @Test
    fun fixedWidth_skipsDesiredWidthAndCentersLines() {
        val text = "Hi"

        val layout =
            HorizontalTextLayout(
                text = text,
                paint = paint,
                minWidth = 1000,
                maxWidth = 1000,
                alignment = Layout.Alignment.ALIGN_CENTER,
                textDir = TextDirectionHeuristics.FIRSTSTRONG_LTR,
            )

        assertThat(layout.width).isEqualTo(1000)
        assertThat(layout.getLineLeft(0)).isGreaterThan(0f)
    }

    @Test
    fun unboundedWidth_usesDesiredWidth() {
        val text = "Hello unbounded"
        val expectedWidth = ceil(Layout.getDesiredWidth(text, 0, text.length, paint)).toInt()

        val layout =
            HorizontalTextLayout(
                text = text,
                paint = paint,
                minWidth = 0,
                maxWidth = Int.MAX_VALUE,
                alignment = Layout.Alignment.ALIGN_NORMAL,
                textDir = TextDirectionHeuristics.FIRSTSTRONG_LTR,
            )

        assertThat(layout.lineCount).isEqualTo(1)
        assertThat(layout.width).isEqualTo(expectedWidth)
    }

    @Test
    fun subRange_lineStartAndLineEndReturnOffsetsInOriginalText() {
        val text = "prefixHello\nWorldSuffix"
        val start = 6
        val end = 17 // "Hello\nWorld"

        val layout =
            HorizontalTextLayout(
                text = text,
                start = start,
                end = end,
                paint = paint,
                minWidth = 0,
                maxWidth = 1000,
            )

        assertThat(layout.lineCount).isEqualTo(2)
        assertThat(layout.getLineStart(0)).isEqualTo(6)
        assertThat(layout.getLineEnd(0)).isEqualTo(12)
        assertThat(layout.getLineStart(1)).isEqualTo(12)
        assertThat(layout.getLineEnd(1)).isEqualTo(17)
    }

    @Test
    fun draw_fillsPlainTextAndRubyBackgroundsAtOffset() {
        val text =
            SpannableString(VISIBLE_CHAR.repeat(2)).apply {
                setSpan(
                    ForegroundColorSpan(Color.TRANSPARENT),
                    0,
                    length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
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
                    1,
                    2,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
        val renderPaint =
            RubyPixel.newPaint().apply {
                color = Color.TRANSPARENT
                bgColor = Color.YELLOW
            }
        val layout = HorizontalTextLayout(text = text, paint = renderPaint, maxWidth = 300)
        val offsetX = 20f
        val offsetY = 16f
        val bitmap = Bitmap.createBitmap(360, 220, Bitmap.Config.ARGB_8888).also { bitmaps += it }
        Canvas(bitmap).apply {
            drawColor(Color.WHITE)
            layout.draw(this, offsetX, offsetY)
        }

        val plainRect = layout.plainBackgroundRects.single()
        val plainCenterX = (offsetX + (plainRect.left + plainRect.right) / 2f).toInt()
        val plainBodyY = (offsetY + (plainRect.top + plainRect.bottom) / 2f).toInt()
        val annotationBandY = (offsetY + layout.getLineTop(0) + ANTIALIAS_MARGIN).toInt()
        val rubyCenterX = (offsetX + plainRect.right + ANTIALIAS_MARGIN * 2).toInt()

        assertThat(bitmap.getPixel(plainCenterX, plainBodyY)).isEqualTo(Color.YELLOW)
        assertThat(bitmap.getPixel(plainCenterX, annotationBandY)).isEqualTo(Color.WHITE)
        assertThat(bitmap.getPixel(rubyCenterX, plainBodyY)).isEqualTo(Color.YELLOW)
        assertThat(bitmap.getPixel(rubyCenterX, annotationBandY)).isEqualTo(Color.YELLOW)
    }
}
