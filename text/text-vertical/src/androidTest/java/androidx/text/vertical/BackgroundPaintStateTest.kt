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
import android.text.style.BackgroundColorSpan
import android.text.style.CharacterStyle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test cases for the background fill when a pooled paint has state from an earlier style run.
 *
 * The text is blank, so only the background paints pixels. Each test reads the pixel at the center
 * of a background rectangle. An outline has no pixels at the center.
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
class BackgroundPaintStateTest {

    @Test
    fun uprightLayoutRun_plainString_fillsBackgroundAfterStrokeRun() {
        // The last style run of this text sets Paint.Style.STROKE on a pooled paint.
        val strokeText = SpannableString("あい").apply { setSpan(StrokeSpan(), 1, 2, SPAN_FLAG) }
        val strokePaint = TextPaint().apply { textSize = TEXT_SIZE }
        UprightLayoutRun(strokeText, 0, strokeText.length, strokePaint)
            .draw(Canvas(), ORIGIN_X, ORIGIN_Y, strokePaint)

        val bgPaint =
            TextPaint().apply {
                textSize = TEXT_SIZE
                bgColor = Color.YELLOW
            }
        val run = UprightLayoutRun(BLANK_TEXT, 0, BLANK_TEXT.length, bgPaint)
        val bitmap = createBitmap()
        run.draw(Canvas(bitmap), ORIGIN_X, ORIGIN_Y, bgPaint)

        val centerX = ORIGIN_X + (run.leftSideOffset + run.rightSideOffset) / 2
        val centerY = ORIGIN_Y + run.height / 2
        val centerPixel = bitmap.getPixel(centerX.toInt(), centerY.toInt())
        bitmap.recycle()
        assertThat(centerPixel).isEqualTo(Color.YELLOW)
    }

    @Test
    fun rubyLayoutRun_coveringStrokeSpan_fillsSliverBackground() {
        val rubySpan = RubySpan(BLANK_RUBY_TEXT, orientation = TextOrientation.Upright)
        val text =
            SpannableString(BLANK_TEXT).apply {
                setSpan(BackgroundColorSpan(Color.YELLOW), 0, length, SPAN_FLAG)
                setSpan(StrokeSpan(), 0, length, SPAN_FLAG)
                setSpan(rubySpan, 0, length, SPAN_FLAG)
            }
        val paint = TextPaint().apply { textSize = TEXT_SIZE }
        val run = RubyLayoutRun(text, 0, text.length, TextOrientation.Upright, paint, rubySpan)
        val bitmap = createBitmap()
        run.draw(Canvas(bitmap), ORIGIN_X, ORIGIN_Y, paint)

        // The ruby text is shorter than the base text. draw() fills a sliver above and a sliver
        // below the ruby text.
        val rubyPaint = TextPaint().apply { textSize = TEXT_SIZE * rubySpan.textScale }
        val rubyRun = UprightLayoutRun(BLANK_RUBY_TEXT, 0, BLANK_RUBY_TEXT.length, rubyPaint)
        val sliverHeight = (run.height - rubyRun.height) / 2
        assertWithMessage("Sliver height").that(sliverHeight).isAtLeast(MIN_SLIVER_HEIGHT)
        // The ruby column ends at rightSideOffset. Its center is half of the ruby width to the
        // left of that edge.
        val sliverCenterX = ORIGIN_X + run.rightSideOffset - rubyRun.width / 2
        val topSliverCenterY = ORIGIN_Y + sliverHeight / 2
        val bottomSliverCenterY = ORIGIN_Y + run.height - sliverHeight / 2
        val topPixel = bitmap.getPixel(sliverCenterX.toInt(), topSliverCenterY.toInt())
        val bottomPixel = bitmap.getPixel(sliverCenterX.toInt(), bottomSliverCenterY.toInt())
        bitmap.recycle()
        assertWithMessage("Pixels at the centers of the top and bottom slivers")
            .that(listOf(topPixel, bottomPixel))
            .isEqualTo(listOf(Color.YELLOW, Color.YELLOW))
    }

    /** Draws the text as an outline. */
    private class StrokeSpan : CharacterStyle() {
        override fun updateDrawState(tp: TextPaint) {
            tp.style = Paint.Style.STROKE
        }
    }
}

private const val TEXT_SIZE = 40f
private const val ORIGIN_X = 100f
private const val ORIGIN_Y = 20f
private const val BITMAP_SIZE = 200
private const val SPAN_FLAG = Spanned.SPAN_INCLUSIVE_EXCLUSIVE

/** The center of a thinner sliver can touch the outline of the sliver. */
private const val MIN_SLIVER_HEIGHT = 4f

/** Ideographic spaces have an advance but draw no glyphs. */
private const val BLANK_TEXT = "\u3000\u3000"
private const val BLANK_RUBY_TEXT = "\u3000"

private fun createBitmap(): Bitmap =
    Bitmap.createBitmap(BITMAP_SIZE, BITMAP_SIZE, Bitmap.Config.ARGB_8888)
