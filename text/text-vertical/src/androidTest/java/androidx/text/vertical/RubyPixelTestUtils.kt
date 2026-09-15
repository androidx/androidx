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
import android.graphics.Color
import android.text.StaticLayout
import android.text.TextPaint
import com.google.common.truth.Truth.assertWithMessage

/**
 * Shared fixtures for the horizontal ruby [AnnotationPosition] pixel tests.
 *
 * Grouped into an object rather than declared at the top level because the sibling vertical suites
 * already use some of these names as file-private constants.
 */
internal object RubyPixel {
    const val TEXT_SIZE = 100f
    const val RUBY_SCALE = 0.5f

    /** Square bitmap, large enough that no annotation band is ever clipped by an edge. */
    const val BITMAP_SIZE = 400

    /** Inset applied to "must be empty" bands, so antialiased edges never straddle a boundary. */
    const val ANTIALIAS_MARGIN = 2

    /** Red channel below this counts as painted. Ignores imperceptible antialiasing fringes. */
    const val FOREGROUND_THRESHOLD = 200

    /** U+3000 IDEOGRAPHIC SPACE: advances a full em but paints nothing. */
    const val INVISIBLE_CHAR = "\u3000"

    /** U+3042 HIRAGANA LETTER A. */
    const val VISIBLE_CHAR = "\u3042"

    fun newPaint(): TextPaint =
        TextPaint().apply {
            textSize = TEXT_SIZE
            color = Color.BLACK
            isAntiAlias = true
        }

    /**
     * Returns the ascent and descent of a single line of [text], measured the way
     * [HorizontalRubySpanLayout] measures it.
     *
     * Metrics must be taken from the exact string under test: [INVISIBLE_CHAR] may resolve to a
     * different font than the visible glyph, and its metrics would then differ.
     */
    fun lineMetrics(paint: TextPaint, text: CharSequence, scale: Float): Pair<Int, Int> {
        val scaledPaint = TextPaint(paint).apply { textSize *= scale }
        val layout =
            StaticLayout.Builder.obtain(text, 0, text.length, scaledPaint, Int.MAX_VALUE).build()
        return layout.getLineAscent(0) to layout.getLineDescent(0)
    }
}

/**
 * A rendered ruby run, together with the pixel bands its annotation may legally occupy.
 *
 * Bands are derived at run time rather than hardcoded: horizontal band boundaries come from real
 * font metrics, which differ across fonts and API levels.
 */
internal class RubyRender(
    val bitmap: Bitmap,
    val baseline: Int,
    bodyAscent: Int,
    bodyDescent: Int,
    rubyLineHeight: Int,
) {
    /** Rows spanned by the base text's own line box. */
    val bodyBand: IntRange = (baseline + bodyAscent) until (baseline + bodyDescent)

    /** Rows directly above [bodyBand], where an [AnnotationPosition.Before] ruby belongs. */
    val rubyBandAbove: IntRange =
        (baseline + bodyAscent - rubyLineHeight) until (baseline + bodyAscent)

    /** Rows directly below [bodyBand], where an [AnnotationPosition.After] ruby belongs. */
    val rubyBandBelow: IntRange =
        (baseline + bodyDescent) until (baseline + bodyDescent + rubyLineHeight)

    fun assertBandIsDrawn(rows: IntRange) {
        assertBandIsInsideBitmap(rows)
        assertWithMessage("expected rows %s to contain painted pixels", rows)
            .that(foregroundPixelsIn(rows))
            .isGreaterThan(0)
    }

    fun assertBandIsEmpty(rows: IntRange) {
        assertBandIsInsideBitmap(rows)
        // Shrink the band so that antialiasing at an adjacent glyph's edge cannot bleed in.
        val inset =
            (rows.first + RubyPixel.ANTIALIAS_MARGIN)..(rows.last - RubyPixel.ANTIALIAS_MARGIN)
        assertWithMessage("expected rows %s to be free of painted pixels", inset)
            .that(foregroundPixelsIn(inset))
            .isEqualTo(0)
    }

    /**
     * Fails if [rows] is not wholly inside the bitmap.
     *
     * Without this an out-of-bounds band would scan zero pixels, which silently turns
     * [assertBandIsEmpty] into a no-op that passes for the wrong reason.
     */
    private fun assertBandIsInsideBitmap(rows: IntRange) {
        assertWithMessage(
                "band %s falls outside the %spx bitmap, so the assertion would be vacuous",
                rows,
                bitmap.height,
            )
            .that(rows.first >= 0 && rows.last < bitmap.height)
            .isTrue()
    }

    /** Counts painted pixels across the full width of [rows]. */
    private fun foregroundPixelsIn(rows: IntRange): Int {
        var count = 0
        for (y in rows) {
            for (x in 0 until bitmap.width) {
                if (Color.red(bitmap.getPixel(x, y)) < RubyPixel.FOREGROUND_THRESHOLD) count++
            }
        }
        return count
    }
}
