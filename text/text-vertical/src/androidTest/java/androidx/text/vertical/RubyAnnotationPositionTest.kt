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
import android.text.SpannableString
import android.text.TextPaint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/** Text size of the base text, chosen so that 1em == 100px. */
private const val ONE_EM = 100f

/** Text size of the ruby text, i.e. 0.5em == 50px at the default ruby scale. */
private const val RUBY_EM = ONE_EM * RubySpan.DEFAULT_TEXT_SCALE

private const val BASE_PX = ONE_EM.toInt() // 100
private const val RUBY_PX = RUBY_EM.toInt() // 50

/** The base text reserves 1em and the ruby 0.5em, whichever [AnnotationPosition] is used. */
private const val LAYOUT_W = BASE_PX + RUBY_PX // 150

private const val BITMAP_H = 200

/** Vertical padding, so glyphs are never clipped by the top edge of the bitmap. */
private const val DRAW_Y = 50f

/** Inset applied to "must be empty" bands so antialiased edges never straddle a boundary. */
private const val ANTIALIAS_MARGIN = 2

/** U+3000 IDEOGRAPHIC SPACE: advances a full em but draws no pixels. */
private const val INVISIBLE_CHAR = "\u3000"

/** U+3042 HIRAGANA LETTER A. */
private const val VISIBLE_CHAR = "\u3042"

// Expected pixel bands. The side offsets of an upright run are derived purely from paint.textSize
// (see UprightLayoutRun), never from glyph outlines, so these boundaries are exact.
//
// AnnotationPosition.Before puts the ruby to the right of the base text:
//
//     0            100          150
//     +-------------+------------+
//     |  base text  |    ruby    |
//     +-------------+------------+
//
// AnnotationPosition.After puts the ruby to the left of the base text:
//
//     0      50                 150
//     +-------+------------------+
//     | ruby  |    base text     |
//     +-------+------------------+

private val BEFORE_BASE_TEXT_BAND = 0 until BASE_PX
private val BEFORE_RUBY_BAND = BASE_PX until LAYOUT_W
private val AFTER_RUBY_BAND = 0 until RUBY_PX
private val AFTER_BASE_TEXT_BAND = RUBY_PX until LAYOUT_W

/**
 * Verifies that [RubySpan.position] is honored when drawing vertical text.
 *
 * Per the TTML2 `tts:rubyPosition` semantics documented on [AnnotationPosition], in vertical
 * writing mode [AnnotationPosition.Before] places the ruby text to the right of the base text, and
 * [AnnotationPosition.After] places it to the left.
 *
 * Each test renders a single column into a [Bitmap] and asserts which vertical bands of pixels are
 * painted.
 *
 * To keep those assertions strictly discriminating, one of the two texts is [INVISIBLE_CHAR], which
 * advances a full em but paints nothing. That leaves exactly one source of painted pixels, so a
 * test can assert both that the expected band *is* painted and that the opposite band is *not*.
 * This matters: asserting only "the expected band is painted" would pass even when the position is
 * ignored, because the base text by itself already paints into the band where the ruby belongs.
 *
 * Do not "simplify" these tests by making both texts visible — that silently removes their ability
 * to detect a regression.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class RubyAnnotationPositionTest {

    private val paint =
        TextPaint().apply {
            textSize = ONE_EM
            color = Color.BLACK
            isAntiAlias = true
        }

    @Test
    fun positionBefore_rubyIsDrawnRightOfBaseText() {
        drawToBitmap(INVISIBLE_CHAR, VISIBLE_CHAR, AnnotationPosition.Before).run {
            assertBandIsDrawn(BEFORE_RUBY_BAND)
            assertBandIsEmpty(BEFORE_BASE_TEXT_BAND)
        }
    }

    @Test
    fun positionAfter_rubyIsDrawnLeftOfBaseText() {
        drawToBitmap(INVISIBLE_CHAR, VISIBLE_CHAR, AnnotationPosition.After).run {
            assertBandIsDrawn(AFTER_RUBY_BAND)
            assertBandIsEmpty(AFTER_BASE_TEXT_BAND)
        }
    }

    @Test
    fun positionBefore_baseTextIsDrawnAtLeftOfLayout() {
        drawToBitmap(VISIBLE_CHAR, INVISIBLE_CHAR, AnnotationPosition.Before).run {
            assertBandIsDrawn(BEFORE_BASE_TEXT_BAND)
            assertBandIsEmpty(BEFORE_RUBY_BAND)
        }
    }

    @Test
    fun positionAfter_baseTextIsDrawnAtRightOfLayout() {
        drawToBitmap(VISIBLE_CHAR, INVISIBLE_CHAR, AnnotationPosition.After).run {
            assertBandIsDrawn(AFTER_BASE_TEXT_BAND)
            assertBandIsEmpty(AFTER_RUBY_BAND)
        }
    }

    @Test
    fun positionBefore_baseTextAndRubyAreBothDrawn() {
        drawToBitmap(VISIBLE_CHAR, VISIBLE_CHAR, AnnotationPosition.Before).run {
            assertBandIsDrawn(BEFORE_BASE_TEXT_BAND)
            assertBandIsDrawn(BEFORE_RUBY_BAND)
        }
    }

    @Test
    fun positionAfter_baseTextAndRubyAreBothDrawn() {
        drawToBitmap(VISIBLE_CHAR, VISIBLE_CHAR, AnnotationPosition.After).run {
            assertBandIsDrawn(AFTER_RUBY_BAND)
            assertBandIsDrawn(AFTER_BASE_TEXT_BAND)
        }
    }

    @Test
    fun positionBefore_widthReservesBaseTextAndRuby() {
        val layout = createLayout(VISIBLE_CHAR, VISIBLE_CHAR, AnnotationPosition.Before)
        assertThat(layout.width).isEqualTo(LAYOUT_W.toFloat())
    }

    @Test
    fun positionAfter_widthReservesBaseTextAndRuby() {
        val layout = createLayout(VISIBLE_CHAR, VISIBLE_CHAR, AnnotationPosition.After)
        assertThat(layout.width).isEqualTo(LAYOUT_W.toFloat())
    }

    /** An unrecognized position falls back to the [RubySpan.DEFAULT_POSITION] placement. */
    @Test
    fun positionUnknown_isTreatedAsBefore() {
        drawToBitmap(INVISIBLE_CHAR, VISIBLE_CHAR, AnnotationPosition.Unknown).run {
            assertBandIsDrawn(BEFORE_RUBY_BAND)
            assertBandIsEmpty(BEFORE_BASE_TEXT_BAND)
        }
    }

    /** Lays out a single column of [baseText] annotated with [rubyText] at [position]. */
    private fun createLayout(
        baseText: String,
        rubyText: String,
        position: AnnotationPosition,
    ): VerticalTextLayout {
        val text =
            SpannableString(baseText).apply {
                setSpan(
                    RubySpan(rubyText, position, TextOrientation.Upright),
                    0,
                    baseText.length,
                    SpannableString.SPAN_INCLUSIVE_EXCLUSIVE,
                )
            }
        // Upright is pinned so the result never depends on the ICU vertical-orientation lookup.
        return VerticalTextLayout(
            text,
            0,
            text.length,
            paint,
            BITMAP_H.toFloat(),
            TextOrientation.Upright,
        )
    }

    private fun drawToBitmap(
        baseText: String,
        rubyText: String,
        position: AnnotationPosition,
    ): Bitmap {
        val layout = createLayout(baseText, rubyText, position)
        // Guards the band math: both positions must reserve base text + ruby width.
        assertThat(layout.width).isEqualTo(LAYOUT_W.toFloat())

        return Bitmap.createBitmap(LAYOUT_W, BITMAP_H, Bitmap.Config.ARGB_8888).also {
            val canvas = Canvas(it)
            canvas.drawColor(Color.WHITE)
            // The drawing origin of a vertical layout is the top-RIGHT corner.
            layout.draw(canvas, LAYOUT_W.toFloat(), DRAW_Y)
        }
    }

    /** Counts the non-background pixels in the vertical band of columns [band]. */
    private fun Bitmap.foregroundPixelsIn(band: IntRange): Int {
        var count = 0
        for (x in band) {
            for (y in 0 until height) {
                if (getPixel(x, y) != Color.WHITE) count++
            }
        }
        return count
    }

    private fun Bitmap.assertBandIsDrawn(band: IntRange) {
        assertThat(foregroundPixelsIn(band)).isGreaterThan(0)
    }

    private fun Bitmap.assertBandIsEmpty(band: IntRange) {
        assertThat(
                foregroundPixelsIn((band.first + ANTIALIAS_MARGIN)..(band.last - ANTIALIAS_MARGIN))
            )
            .isEqualTo(0)
    }
}
