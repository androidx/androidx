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
import android.graphics.Typeface
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.text.vertical.HorizontalTextLayout
import androidx.text.vertical.RubySpan
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class HorizontalTextLayoutCacheTest {
    private val paint = TextPaint().apply { textSize = 40f }
    private val bitmaps = mutableListOf<Bitmap>()

    @After
    fun recycleBitmaps() {
        bitmaps.forEach { it.recycle() }
    }

    @Test
    fun textAlignLeftRight_alignToPhysicalEdgesAcrossRtlReplacementSpansAndMultiParagraphs() {
        val rtlRuby =
            SpannableString("שלום").apply {
                setSpan(RubySpan("רובי"), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        val multiParagraph = "Hello\nשלום"

        val rtlRubyLeft =
            HorizontalTextLayout(
                text = rtlRuby,
                paint = paint,
                minWidth = 1000,
                maxWidth = 1000,
                alignment = resolveHorizontalAlignment(TextAlign.Left),
                textDir = TextDirectionHeuristics.FIRSTSTRONG_LTR,
            )
        val rtlRubyRight =
            HorizontalTextLayout(
                text = rtlRuby,
                paint = paint,
                minWidth = 1000,
                maxWidth = 1000,
                alignment = resolveHorizontalAlignment(TextAlign.Right),
                textDir = TextDirectionHeuristics.FIRSTSTRONG_LTR,
            )
        val multiParagraphLeft =
            HorizontalTextLayout(
                text = multiParagraph,
                paint = paint,
                minWidth = 1000,
                maxWidth = 1000,
                alignment = resolveHorizontalAlignment(TextAlign.Left),
                textDir = TextDirectionHeuristics.FIRSTSTRONG_LTR,
            )
        val multiParagraphRight =
            HorizontalTextLayout(
                text = multiParagraph,
                paint = paint,
                minWidth = 1000,
                maxWidth = 1000,
                alignment = resolveHorizontalAlignment(TextAlign.Right),
                textDir = TextDirectionHeuristics.FIRSTSTRONG_LTR,
            )

        assertThat(rtlRubyLeft.getLineLeft(0)).isEqualTo(0f)
        assertThat(rtlRubyRight.getLineRight(0)).isEqualTo(1000f)
        assertThat(multiParagraphLeft.getLineLeft(0)).isEqualTo(0f)
        assertThat(multiParagraphLeft.getLineLeft(1)).isEqualTo(0f)
        assertThat(multiParagraphRight.getLineRight(0)).isEqualTo(1000f)
        assertThat(multiParagraphRight.getLineRight(1)).isEqualTo(1000f)
        assertThat(resolveHorizontalAlignment(TextAlign.Center))
            .isEqualTo(Layout.Alignment.ALIGN_CENTER)
        assertThat(resolveHorizontalAlignment(TextAlign.Start))
            .isEqualTo(Layout.Alignment.ALIGN_NORMAL)
        assertThat(resolveHorizontalAlignment(TextAlign.End))
            .isEqualTo(Layout.Alignment.ALIGN_OPPOSITE)
    }

    @Test
    fun cache_reusesLayoutForSameInputsAndInvalidatesOnChange() {
        val cache = HorizontalTextLayoutCache()
        val text =
            SpannableString("Hello").apply {
                setSpan(RubySpan("ruby"), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        val style = TextStyle(fontSize = 24.sp, background = Color.Yellow)
        val typeface = Typeface.DEFAULT
        val density = Density(1f)

        val layout1 = cache.getLayout(text, 0, 200, LayoutDirection.Ltr, style, typeface, density)
        val layout2 = cache.getLayout(text, 0, 200, LayoutDirection.Ltr, style, typeface, density)
        assertThat(layout1).isSameInstanceAs(layout2)

        val differentText =
            cache.getLayout("World", 0, 200, LayoutDirection.Ltr, style, typeface, density)
        assertThat(layout1).isNotSameInstanceAs(differentText)

        val differentMinWidth =
            cache.getLayout(text, 50, 200, LayoutDirection.Ltr, style, typeface, density)
        assertThat(layout1).isNotSameInstanceAs(differentMinWidth)

        val differentWidth =
            cache.getLayout(text, 0, 300, LayoutDirection.Ltr, style, typeface, density)
        assertThat(layout1).isNotSameInstanceAs(differentWidth)

        val differentDir =
            cache.getLayout(text, 0, 200, LayoutDirection.Ltr, style, typeface, density).also {
                val rtl =
                    cache.getLayout(text, 0, 200, LayoutDirection.Rtl, style, typeface, density)
                assertThat(it).isNotSameInstanceAs(rtl)
            }
        assertThat(differentDir).isNotNull()

        val neutralRtl =
            cache.getLayout("123", 200, 200, LayoutDirection.Rtl, style, typeface, density)
        assertThat(neutralRtl.getLineRight(0)).isEqualTo(200f)

        val differentTypeface =
            cache.getLayout(text, 0, 200, LayoutDirection.Ltr, style, Typeface.SERIF, density)
        assertThat(layout1).isNotSameInstanceAs(differentTypeface)

        val differentDensity =
            cache.getLayout(text, 0, 200, LayoutDirection.Ltr, style, typeface, Density(2f))
        assertThat(layout1).isNotSameInstanceAs(differentDensity)

        val noBgStyle = TextStyle(fontSize = 24.sp, background = Color.Unspecified)
        val noBgLayout =
            cache.getLayout(text, 0, 200, LayoutDirection.Ltr, noBgStyle, typeface, density)
        assertThat(layout1).isNotSameInstanceAs(noBgLayout)

        // Drawing layout1 after a subsequent cache miss with Color.Unspecified must still fill the
        // yellow RubySpan background (verifying that each cache miss allocates its own TextPaint).
        val bitmap = Bitmap.createBitmap(200, 120, Bitmap.Config.ARGB_8888).also { bitmaps += it }
        Canvas(bitmap).apply {
            drawColor(android.graphics.Color.WHITE)
            layout1.draw(this, 0f, 0f)
        }
        assertThat(bitmap.getPixel(2, 2)).isEqualTo(Color.Yellow.toArgb())
    }

    @Test
    fun cache_convertsAnnotatedStringSpanStyles() {
        val cache = HorizontalTextLayoutCache()
        val annotatedText = buildAnnotatedString {
            append("Hello ")
            withStyle(SpanStyle(fontSize = 32.sp)) { append("world") }
            append(" again")
        }
        val style = TextStyle(fontSize = 16.sp, background = Color.Yellow)

        val plainResult =
            cache.getLayout(
                text = "Hello world again",
                minWidth = 0,
                maxWidth = 1000,
                layoutDirection = LayoutDirection.Ltr,
                style = style,
                typeface = Typeface.DEFAULT,
                density = Density(1f),
            )
        val annotatedResult =
            cache.getLayout(
                text = annotatedText,
                minWidth = 0,
                maxWidth = 1000,
                layoutDirection = LayoutDirection.Ltr,
                style = style,
                typeface = Typeface.DEFAULT,
                density = Density(1f),
            )

        assertThat(annotatedResult.width).isGreaterThan(plainResult.width)
        assertThat(annotatedResult.height).isGreaterThan(plainResult.height)
    }
}
