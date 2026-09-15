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
import android.text.Layout
import android.text.SpannedString
import android.text.TextPaint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.text.vertical.RubyPixel.BITMAP_SIZE
import androidx.text.vertical.RubyPixel.INVISIBLE_CHAR
import androidx.text.vertical.RubyPixel.RUBY_SCALE
import androidx.text.vertical.RubyPixel.VISIBLE_CHAR
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith

/** Baseline the layout is drawn at, leaving room for an annotation on either side of it. */
private const val BASELINE_Y = 200

/**
 * Verifies that [HorizontalRubySpanLayout] honors the [AnnotationPosition] it is constructed with,
 * both in the space it reserves via `fillFontMetrics` and in where it actually paints the
 * annotation.
 *
 * Terminology follows [CSS Writing Modes](https://drafts.csswg.org/css-writing-modes-4/#line-over):
 * [AnnotationPosition.Before] puts the ruby on the *line-over* side and [AnnotationPosition.After]
 * on the *line-under* side, which in horizontal writing mode read as above and below respectively.
 *
 * This suite constructs the layout directly, so the baseline is chosen by the test rather than
 * derived from a line box. It therefore cannot see whether [RubySpan] actually forwards its
 * position; [HorizontalRubyAnnotationPositionTest] covers that end-to-end through the public span
 * API and is the suite that fails if the forwarding is dropped.
 *
 * ## How a drawing case proves placement
 *
 * Exactly one of the base text and the ruby text is set to [RubyPixel.INVISIBLE_CHAR], so there is
 * only a single source of pixels. That makes it possible to assert both that the expected band is
 * painted *and* that the opposite band is empty. Asserting only the former would pass even against
 * the unfixed code, because the base glyph alone paints into the body band regardless of position.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class HorizontalRubySpanLayoutPositionTest {

    private val paint = RubyPixel.newPaint()

    // ---------------------------------------------------------------------------------------
    // Drawing
    // ---------------------------------------------------------------------------------------

    @Test
    fun positionBefore_rubyIsDrawnAboveBaseText() {
        val drawn = draw(INVISIBLE_CHAR, VISIBLE_CHAR, AnnotationPosition.Before)

        drawn.assertBandIsDrawn(drawn.rubyBandAbove)
        drawn.assertBandIsEmpty(drawn.bodyBand)
    }

    @Test
    fun positionAfter_rubyIsDrawnBelowBaseText() {
        val drawn = draw(INVISIBLE_CHAR, VISIBLE_CHAR, AnnotationPosition.After)

        drawn.assertBandIsDrawn(drawn.rubyBandBelow)
        drawn.assertBandIsEmpty(drawn.bodyBand)
    }

    @Test
    fun positionBefore_baseTextIsDrawnBelowRuby() {
        val drawn = draw(VISIBLE_CHAR, INVISIBLE_CHAR, AnnotationPosition.Before)

        drawn.assertBandIsDrawn(drawn.bodyBand)
        drawn.assertBandIsEmpty(drawn.rubyBandAbove)
    }

    @Test
    fun positionAfter_baseTextIsDrawnAboveRuby() {
        val drawn = draw(VISIBLE_CHAR, INVISIBLE_CHAR, AnnotationPosition.After)

        drawn.assertBandIsDrawn(drawn.bodyBand)
        drawn.assertBandIsEmpty(drawn.rubyBandBelow)
    }

    // ---------------------------------------------------------------------------------------
    // Reserved space. Reserving on the wrong side would let the annotation collide with the
    // adjacent line, so the position has to move the reservation and not just the drawing.
    // ---------------------------------------------------------------------------------------

    @Test
    fun fillFontMetrics_positionBefore_expandsAscentOnly() {
        assertMetrics(AnnotationPosition.Before, expectRubyAbove = true)
    }

    @Test
    fun fillFontMetrics_positionAfter_expandsDescentOnly() {
        assertMetrics(AnnotationPosition.After, expectRubyAbove = false)
    }

    @Test
    fun fillFontMetrics_positionUnknown_isTreatedAsBefore() {
        assertMetrics(AnnotationPosition.Unknown, expectRubyAbove = true)
    }

    @Test
    fun spanWidth_isIndependentOfPosition() {
        // The span is as wide as the wider of the two texts, so both sides of that choice have to
        // be covered: a position that leaked into the width would only show up on one of them.
        assertSpanWidthIsIndependentOfPosition(
            baseText = VISIBLE_CHAR.repeat(3),
            rubyText = VISIBLE_CHAR,
        )
        assertSpanWidthIsIndependentOfPosition(
            baseText = VISIBLE_CHAR,
            rubyText = VISIBLE_CHAR.repeat(3),
        )
    }

    // ---------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------

    private fun layoutOf(
        baseText: String,
        rubyText: String,
        position: AnnotationPosition,
    ): HorizontalRubySpanLayout {
        val text = SpannedString(baseText)
        return HorizontalRubySpanLayout(text, 0, text.length, rubyText, position, paint, RUBY_SCALE)
    }

    /** Width [HorizontalRubySpanLayout] measures [text] at, for a body ([scale] 1f) or a ruby. */
    private fun desiredWidth(text: String, scale: Float): Float =
        Layout.getDesiredWidth(text, TextPaint(paint).apply { textSize *= scale })

    /**
     * Asserts that the position does not change [HorizontalRubySpanLayout.spanWidth].
     *
     * Also pins down which of the two texts the width is expected to come from. Without that the
     * case could silently stop covering the branch it was written for if font metrics change.
     */
    private fun assertSpanWidthIsIndependentOfPosition(baseText: String, rubyText: String) {
        val bodyWidth = desiredWidth(baseText, 1f)
        val rubyWidth = desiredWidth(rubyText, RUBY_SCALE)
        assertWithMessage(
                "this case is meant to resolve the width from %s, but body=%s and ruby=%s",
                if (rubyText.length > baseText.length) "the ruby" else "the body",
                bodyWidth,
                rubyWidth,
            )
            .that(rubyWidth > bodyWidth)
            .isEqualTo(rubyText.length > baseText.length)

        val before = layoutOf(baseText, rubyText, AnnotationPosition.Before)
        val after = layoutOf(baseText, rubyText, AnnotationPosition.After)

        assertWithMessage("spanWidth for base %s with ruby %s", baseText, rubyText)
            .that(after.spanWidth)
            .isEqualTo(before.spanWidth)
    }

    private fun assertMetrics(position: AnnotationPosition, expectRubyAbove: Boolean) {
        val layout = layoutOf(VISIBLE_CHAR, VISIBLE_CHAR, position)
        val (bodyAscent, bodyDescent) = RubyPixel.lineMetrics(paint, VISIBLE_CHAR, 1f)
        val (rubyAscent, rubyDescent) = RubyPixel.lineMetrics(paint, VISIBLE_CHAR, RUBY_SCALE)
        val rubyLineHeight = rubyDescent - rubyAscent

        val fm = Paint.FontMetricsInt()
        layout.fillFontMetrics(fm)

        val expectedAscent = if (expectRubyAbove) bodyAscent - rubyLineHeight else bodyAscent
        val expectedDescent = if (expectRubyAbove) bodyDescent else bodyDescent + rubyLineHeight
        assertThat(fm.ascent).isEqualTo(expectedAscent)
        assertThat(fm.descent).isEqualTo(expectedDescent)
        assertThat(fm.top).isEqualTo(expectedAscent)
        assertThat(fm.bottom).isEqualTo(expectedDescent)
    }

    /** Draws the layout at a fixed baseline into a white bitmap. */
    private fun draw(
        baseText: String,
        rubyText: String,
        position: AnnotationPosition,
    ): RubyRender {
        val layout = layoutOf(baseText, rubyText, position)

        val bitmap = Bitmap.createBitmap(BITMAP_SIZE, BITMAP_SIZE, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(Color.WHITE)
            layout.draw(this, 0f, BASELINE_Y.toFloat(), paint)
        }

        val (bodyAscent, bodyDescent) = RubyPixel.lineMetrics(paint, baseText, 1f)
        val (rubyAscent, rubyDescent) = RubyPixel.lineMetrics(paint, rubyText, RUBY_SCALE)

        return RubyRender(
            bitmap = bitmap,
            baseline = BASELINE_Y,
            bodyAscent = bodyAscent,
            bodyDescent = bodyDescent,
            rubyLineHeight = rubyDescent - rubyAscent,
        )
    }
}
