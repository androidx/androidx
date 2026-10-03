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

package androidx.text.vertical.compose

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Typeface
import android.text.Spanned
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.MetricAffectingSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.text.vertical.TextOrientation
import androidx.text.vertical.VerticalTextLayout
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.ceil
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class AnnotatedStringConversionTest {

    private val density = Density(density = 2f, fontScale = 1f)

    @Test
    fun toSpanned_plainText_hasNoSpans() {
        val annotated = AnnotatedString("Hello World")
        val spanned = annotated.toSpanned(density)
        assertThat(spanned.toString()).isEqualTo("Hello World")
        assertThat(spanned.getSpans(0, spanned.length, Any::class.java)).isEmpty()
    }

    @Test
    fun toSpanned_emptyText() {
        val annotated = AnnotatedString("")
        val spanned = annotated.toSpanned(density)
        assertThat(spanned.length).isEqualTo(0)
        assertThat(spanned.getSpans(0, 0, Any::class.java)).isEmpty()
    }

    @Test
    fun toSpanned_foregroundColor() {
        val annotated = buildAnnotatedString {
            withStyle(SpanStyle(color = Color.Red)) { append("red") }
        }
        val spanned = annotated.toSpanned(density)
        val spans = spanned.getSpans(0, 3, ForegroundColorSpan::class.java)
        assertThat(spans).hasLength(1)
        assertThat(spans[0].foregroundColor).isEqualTo(AndroidColor.RED)
        assertThat(spanned.getSpanStart(spans[0])).isEqualTo(0)
        assertThat(spanned.getSpanEnd(spans[0])).isEqualTo(3)
    }

    @Test
    fun toSpanned_fontSize_sp_roundsToNearestInt() {
        val unitDensity = Density(density = 1f, fontScale = 1f)
        val annotated = buildAnnotatedString {
            withStyle(SpanStyle(fontSize = 10.4.sp)) { append("down") }
            withStyle(SpanStyle(fontSize = 10.6.sp)) { append("up") }
        }
        val spanned = annotated.toSpanned(unitDensity)
        val downSpans = spanned.getSpans(0, 4, AbsoluteSizeSpan::class.java)
        assertThat(downSpans).hasLength(1)
        assertThat(downSpans[0].size).isEqualTo(10)
        assertThat(downSpans[0].dip).isFalse()

        val upSpans = spanned.getSpans(4, 6, AbsoluteSizeSpan::class.java)
        assertThat(upSpans).hasLength(1)
        assertThat(upSpans[0].size).isEqualTo(11)
        assertThat(upSpans[0].dip).isFalse()
    }

    @Test
    fun toSpanned_fontSize_em() {
        val annotated = buildAnnotatedString {
            withStyle(SpanStyle(fontSize = 1.5.em)) { append("big") }
        }
        val spanned = annotated.toSpanned(density)
        val relativeSpans = spanned.getSpans(0, 3, RelativeSizeSpan::class.java)
        assertThat(relativeSpans).hasLength(1)
        assertThat(relativeSpans[0].sizeChange).isEqualTo(1.5f)
        assertThat(spanned.getSpanStart(relativeSpans[0])).isEqualTo(0)
        assertThat(spanned.getSpanEnd(relativeSpans[0])).isEqualTo(3)
        assertThat(spanned.getSpans(0, 3, AbsoluteSizeSpan::class.java)).isEmpty()
    }

    @Test
    fun toSpanned_fontSize_em_nestedMultipliesRelativeSize() {
        val annotated = buildAnnotatedString {
            withStyle(SpanStyle(fontSize = 2.em)) {
                withStyle(SpanStyle(fontSize = 1.5.em)) { append("nested") }
            }
        }
        val spanned = annotated.toSpanned(density)
        val paint = TextPaint().apply { textSize = 20f }
        spanned.applyMetricAffectingSpans(0, 6, paint)
        assertThat(paint.textSize).isEqualTo(60f) // 20f * 2f * 1.5f
    }

    @Test
    fun toSpanned_fontSize_unspecified_attachesNoSizeSpan() {
        val annotated = buildAnnotatedString {
            withStyle(SpanStyle(color = Color.Blue)) { append("blue") }
        }
        val spanned = annotated.toSpanned(density)
        assertThat(spanned.getSpans(0, 4, AbsoluteSizeSpan::class.java)).isEmpty()
        assertThat(spanned.getSpans(0, 4, RelativeSizeSpan::class.java)).isEmpty()
    }

    @Test
    fun toSpanned_fontWeightAndFontStyle() {
        val annotated = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("bold") }
            withStyle(SpanStyle(fontWeight = FontWeight.Normal)) { append("norm") }
            withStyle(SpanStyle(fontWeight = FontWeight.W600)) { append("semi") }
            withStyle(SpanStyle(fontWeight = FontWeight.W500)) { append("medm") }
            withStyle(SpanStyle(fontStyle = FontStyle.Normal)) { append("romn") }
            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append("ital") }
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                append("both")
            }
        }
        val spanned = annotated.toSpanned(density)
        assertThat(spanned.getSpans(0, 4, StyleSpan::class.java).single().style)
            .isEqualTo(Typeface.BOLD)
        assertThat(spanned.getSpans(4, 8, StyleSpan::class.java).single().style)
            .isEqualTo(Typeface.NORMAL)
        assertThat(spanned.getSpans(8, 12, StyleSpan::class.java).single().style)
            .isEqualTo(Typeface.BOLD)
        assertThat(spanned.getSpans(12, 16, StyleSpan::class.java).single().style)
            .isEqualTo(Typeface.NORMAL)
        assertThat(spanned.getSpans(16, 20, StyleSpan::class.java).single().style)
            .isEqualTo(Typeface.NORMAL)
        assertThat(spanned.getSpans(20, 24, StyleSpan::class.java).single().style)
            .isEqualTo(Typeface.ITALIC)
        assertThat(spanned.getSpans(24, 28, StyleSpan::class.java).single().style)
            .isEqualTo(Typeface.BOLD_ITALIC)
    }

    @Test
    fun toSpanned_background() {
        val annotated = buildAnnotatedString {
            withStyle(SpanStyle(background = Color.Yellow)) { append("highlight") }
        }
        val spanned = annotated.toSpanned(density)
        val spans = spanned.getSpans(0, 9, BackgroundColorSpan::class.java)
        assertThat(spans).hasLength(1)
        assertThat(spans[0].backgroundColor).isEqualTo(AndroidColor.YELLOW)
    }

    @Test
    fun toSpanned_letterSpacing_em() {
        val annotated = buildAnnotatedString {
            withStyle(SpanStyle(letterSpacing = 0.3.em)) { append("spaced") }
        }
        val spanned = annotated.toSpanned(density)
        val spans = spanned.getSpans(0, 6, LetterSpacingSpanEm::class.java)
        assertThat(spans).hasLength(1)
        val measurePaint = TextPaint()
        spans[0].updateMeasureState(measurePaint)
        assertThat(measurePaint.letterSpacing).isEqualTo(0.3f)

        val drawPaint = TextPaint()
        spans[0].updateDrawState(drawPaint)
        assertThat(drawPaint.letterSpacing).isEqualTo(0.3f)
    }

    @Test
    fun toSpanned_letterSpacing_sp_withSameSpanFontSize() {
        // fontSize = 16.sp = 32px (density 2), letterSpacing = 1.6.sp = 3.2px -> 3.2 / 32 = 0.1 em
        val annotated = buildAnnotatedString {
            withStyle(SpanStyle(fontSize = 16.sp, letterSpacing = 1.6.sp)) { append("spaced") }
        }
        val spanned = annotated.toSpanned(density)
        val paint = TextPaint()
        spanned.applyMetricAffectingSpans(0, 6, paint)
        assertThat(paint.textSize).isEqualTo(32f)
        assertThat(paint.letterSpacing).isWithin(1e-6f).of(0.1f)
    }

    @Test
    fun toSpanned_letterSpacing_sp_withOuterFontSizeSpan() {
        // Outer fontSize = 20.sp = 40px (density 2); inner letterSpacing = 2.sp = 4px -> 0.1 em
        val annotated = buildAnnotatedString {
            withStyle(SpanStyle(fontSize = 20.sp)) {
                append("ab")
                withStyle(SpanStyle(letterSpacing = 2.sp)) { append("cde") }
                append("f")
            }
        }
        val spanned = annotated.toSpanned(density)
        val paint = TextPaint()
        spanned.applyMetricAffectingSpans(2, 5, paint)
        assertThat(paint.textSize).isEqualTo(40f)
        assertThat(paint.letterSpacing).isWithin(1e-6f).of(0.1f)
    }

    @Test
    fun toSpanned_letterSpacing_sp_withOuterEmFontSizeSpan() {
        // Base textSize = 32px, outer RelativeSizeSpan(2f) -> 64px; inner 3.2.sp = 6.4px -> 0.1 em
        val annotated = buildAnnotatedString {
            withStyle(SpanStyle(fontSize = 2.em)) {
                append("ab")
                withStyle(SpanStyle(letterSpacing = 3.2.sp)) { append("cde") }
                append("f")
            }
        }
        val spanned = annotated.toSpanned(density)
        val paint = TextPaint().apply { textSize = 32f }
        spanned.applyMetricAffectingSpans(2, 5, paint)
        assertThat(paint.textSize).isEqualTo(64f)
        assertThat(paint.letterSpacing).isWithin(1e-6f).of(0.1f)
    }

    @Test
    fun toSpanned_letterSpacing_sp_noFontSize_usesPaintTextSize() {
        // Base textSize = 32px, letterSpacing = 2.sp = 4px (density 2) -> 4 / 32 = 0.125 em
        val annotated = buildAnnotatedString {
            withStyle(SpanStyle(letterSpacing = 2.sp)) { append("spaced") }
        }
        val spanned = annotated.toSpanned(density)
        val paint = TextPaint().apply { textSize = 32f }
        spanned.applyMetricAffectingSpans(0, 6, paint)
        assertThat(paint.letterSpacing).isWithin(1e-6f).of(0.125f)
    }

    @Test
    fun toSpanned_letterSpacing_sp_respectsTextScaleXAndZeroTextSize() {
        val annotated = buildAnnotatedString {
            withStyle(SpanStyle(letterSpacing = 2.sp)) { append("spaced") }
        }
        val spanned = annotated.toSpanned(density)
        val span = spanned.getSpans(0, 6, LetterSpacingSpanPx::class.java).single()

        // 2.sp * density 2 = 4px; emWidth = textSize(20f) * textScaleX(2f) = 40px -> 0.1 em
        val drawPaint =
            TextPaint().apply {
                textSize = 20f
                textScaleX = 2f
            }
        span.updateDrawState(drawPaint)
        assertThat(drawPaint.letterSpacing).isWithin(1e-6f).of(0.1f)

        val measurePaint =
            TextPaint().apply {
                textSize = 20f
                textScaleX = 2f
            }
        span.updateMeasureState(measurePaint)
        assertThat(measurePaint.letterSpacing).isWithin(1e-6f).of(0.1f)

        // When textSize is 0f (emWidth == 0f), letterSpacing must remain unchanged (no NaN/Inf).
        val zeroPaint =
            TextPaint().apply {
                textSize = 0f
                letterSpacing = 0.25f
            }
        span.updateDrawState(zeroPaint)
        assertThat(zeroPaint.letterSpacing).isEqualTo(0.25f)
        span.updateMeasureState(zeroPaint)
        assertThat(zeroPaint.letterSpacing).isEqualTo(0.25f)
    }

    @Test
    fun toSpanned_fontScale_scalesSpFontSizeAndLetterSpacing() {
        val scaledDensity = Density(density = 2f, fontScale = 1.5f) // 1.sp = 3px
        val annotated = buildAnnotatedString {
            withStyle(SpanStyle(fontSize = 10.sp, letterSpacing = 1.sp)) { append("scaled") }
        }
        val spanned = annotated.toSpanned(scaledDensity)
        val sizeSpan = spanned.getSpans(0, 6, AbsoluteSizeSpan::class.java).single()
        assertThat(sizeSpan.size).isEqualTo(30)

        val paint = TextPaint()
        spanned.applyMetricAffectingSpans(0, 6, paint)
        assertThat(paint.textSize).isEqualTo(30f)
        assertThat(paint.letterSpacing).isWithin(1e-6f).of(0.1f)
    }

    @Test
    fun toSpanned_letterSpacing_appliedAfterSizeSpans() {
        // Declare letterSpacing in an outer (earlier) SpanStyle and fontSize in an inner (later)
        // SpanStyle; Pass 2 must still attach LetterSpacingSpanPx after AbsoluteSizeSpan.
        val annotated = buildAnnotatedString {
            withStyle(SpanStyle(letterSpacing = 2.sp)) {
                withStyle(SpanStyle(fontSize = 20.sp)) { append("spaced") }
            }
        }
        val spanned = annotated.toSpanned(density)
        val spans = spanned.getSpans(0, 6, MetricAffectingSpan::class.java)
        assertThat(spans).hasLength(2)
        assertThat(spans[0]).isInstanceOf(AbsoluteSizeSpan::class.java)
        assertThat(spans[1]).isInstanceOf(LetterSpacingSpanPx::class.java)

        val paint = TextPaint().apply { textSize = 10f }
        spanned.applyMetricAffectingSpans(0, 6, paint)
        assertThat(paint.textSize).isEqualTo(40f)
        assertThat(paint.letterSpacing).isWithin(1e-6f).of(0.1f)
    }

    @Test
    fun toSpanned_emptyOrOutOfBoundsRange_isSkipped() {
        val annotated =
            AnnotatedString(
                text = "abc",
                spanStyles =
                    listOf(
                        AnnotatedString.Range(SpanStyle(color = Color.Red), start = 1, end = 1),
                        AnnotatedString.Range(SpanStyle(color = Color.Blue), start = -1, end = 2),
                        AnnotatedString.Range(SpanStyle(color = Color.Green), start = 0, end = 5),
                    ),
            )
        val spanned = annotated.toSpanned(density)
        assertThat(spanned.getSpans(0, 3, ForegroundColorSpan::class.java)).isEmpty()
    }

    @Test
    fun toSpanned_multipleSpans() {
        val annotated = buildAnnotatedString {
            withStyle(SpanStyle(color = Color.Red)) { append("red ") }
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("bold") }
        }
        val spanned = annotated.toSpanned(density)
        assertThat(spanned.toString()).isEqualTo("red bold")
        assertThat(spanned.getSpans(0, 4, ForegroundColorSpan::class.java)).hasLength(1)
        assertThat(spanned.getSpans(4, 8, StyleSpan::class.java)).hasLength(1)
    }

    @Test
    fun toSpanned_rendersInVerticalTextLayout() {
        val annotated = buildAnnotatedString {
            withStyle(SpanStyle(background = Color.Yellow, fontSize = 1.5.em)) { append("Hello") }
        }
        val spanned = annotated.toSpanned(density)
        val paint = TextPaint().apply { textSize = 24f }
        val layoutHeight = 200
        val layout =
            VerticalTextLayout(
                text = spanned,
                start = 0,
                end = spanned.length,
                paint = paint,
                height = layoutHeight.toFloat(),
                orientation = TextOrientation.Mixed,
            )
        assertWithMessage("VerticalTextLayout must draw the yellow background from toSpanned")
            .that(layout.countColorPixels(Color.Yellow.toArgb(), layoutHeight))
            .isGreaterThan(0)
    }

    private fun Spanned.applyMetricAffectingSpans(start: Int, end: Int, paint: TextPaint) {
        getSpans(start, end, MetricAffectingSpan::class.java).forEach { it.updateDrawState(paint) }
    }
}

private fun VerticalTextLayout.countColorPixels(targetColor: Int, height: Int): Int {
    val bitmapWidth = ceil(width).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(bitmapWidth, height, Bitmap.Config.ARGB_8888)
    try {
        draw(Canvas(bitmap), bitmapWidth.toFloat(), 0f)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return pixels.count { it == targetColor }
    } finally {
        bitmap.recycle()
    }
}
