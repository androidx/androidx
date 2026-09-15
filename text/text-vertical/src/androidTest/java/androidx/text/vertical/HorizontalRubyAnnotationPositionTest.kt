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
import android.text.Spanned
import android.text.StaticLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.text.vertical.RubyPixel.BITMAP_SIZE
import androidx.text.vertical.RubyPixel.INVISIBLE_CHAR
import androidx.text.vertical.RubyPixel.RUBY_SCALE
import androidx.text.vertical.RubyPixel.VISIBLE_CHAR
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/** Vertical padding, so that neither annotation band is ever clipped by the top edge. */
private const val DRAW_ORIGIN_Y = 100

/**
 * Width of a paragraph that holds two ruby spans. This is more than one span but less than two, so
 * the layout puts each span on its own line.
 */
private const val ONE_SPAN_WIDTH = 150

/**
 * Verifies that [RubySpan.position] is honored when the span is laid out in **horizontal** writing
 * mode, i.e. by a plain [StaticLayout]. [AnnotationPosition.Before] must place the ruby annotation
 * above the base text and [AnnotationPosition.After] must place it below.
 *
 * These cases render through the public [RubySpan] API rather than constructing
 * [HorizontalRubySpanLayout] directly, so they cover both halves of the defect: the position not
 * being forwarded by [RubySpan], and the layout having no notion of position at all. The sibling
 * [HorizontalRubySpanLayoutPositionTest] covers the layout in isolation and cannot see the
 * forwarding.
 *
 * ## How a case proves placement
 *
 * Exactly one of the base text and the ruby text is set to [RubyPixel.INVISIBLE_CHAR], so there is
 * only a single source of pixels in the bitmap. That makes it possible to assert both that the
 * expected band is painted *and* that the opposite band is empty.
 *
 * ```
 *                    AP.Before                    AP.After
 *                +---------------+  <- top    +---------------+  <- top
 *   ruby band -> |     ruby      |            |     base      | <- body band
 *                +---------------+            +---------------+  <- baseline
 *   body band -> |     base      |            |     ruby      | <- ruby band
 *                +---------------+            +---------------+
 * ```
 *
 * > A "the expected band is painted" assertion on its own is **not** sufficient: with the base text
 * > visible, the base glyph alone paints into the body band whichever position is in effect. The
 * > paired "opposite band is empty" assertion is what actually detects a regression. Do not make
 * > both texts visible in the same case.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class HorizontalRubyAnnotationPositionTest {

    private val paint = RubyPixel.newPaint()

    // ---------------------------------------------------------------------------------------
    // AnnotationPosition.Before: the ruby annotation belongs above the base text.
    // ---------------------------------------------------------------------------------------

    @Test
    fun positionBefore_rubyIsDrawnAboveBaseText() {
        val rendered = render(INVISIBLE_CHAR, VISIBLE_CHAR, AnnotationPosition.Before)

        rendered.assertBandIsDrawn(rendered.rubyBandAbove)
        rendered.assertBandIsEmpty(rendered.bodyBand)
    }

    @Test
    fun positionBefore_baseTextIsDrawnBelowRuby() {
        val rendered = render(VISIBLE_CHAR, INVISIBLE_CHAR, AnnotationPosition.Before)

        rendered.assertBandIsDrawn(rendered.bodyBand)
        rendered.assertBandIsEmpty(rendered.rubyBandAbove)
    }

    @Test
    fun positionBefore_baseTextAndRubyAreBothDrawn() {
        val rendered = render(VISIBLE_CHAR, VISIBLE_CHAR, AnnotationPosition.Before)

        rendered.assertBandIsDrawn(rendered.bodyBand)
        rendered.assertBandIsDrawn(rendered.rubyBandAbove)
    }

    // ---------------------------------------------------------------------------------------
    // AnnotationPosition.After: the ruby annotation belongs below the base text.
    // ---------------------------------------------------------------------------------------

    @Test
    fun positionAfter_rubyIsDrawnBelowBaseText() {
        val rendered = render(INVISIBLE_CHAR, VISIBLE_CHAR, AnnotationPosition.After)

        rendered.assertBandIsDrawn(rendered.rubyBandBelow)
        rendered.assertBandIsEmpty(rendered.bodyBand)
    }

    @Test
    fun positionAfter_baseTextIsDrawnAboveRuby() {
        val rendered = render(VISIBLE_CHAR, INVISIBLE_CHAR, AnnotationPosition.After)

        rendered.assertBandIsDrawn(rendered.bodyBand)
        rendered.assertBandIsEmpty(rendered.rubyBandBelow)
    }

    @Test
    fun positionAfter_baseTextAndRubyAreBothDrawn() {
        val rendered = render(VISIBLE_CHAR, VISIBLE_CHAR, AnnotationPosition.After)

        rendered.assertBandIsDrawn(rendered.bodyBand)
        rendered.assertBandIsDrawn(rendered.rubyBandBelow)
    }

    // ---------------------------------------------------------------------------------------
    // Line box metrics. Reserving space on the wrong side would let the annotation collide with
    // the adjacent line, so placement has to move the reservation, not just the drawing.
    // ---------------------------------------------------------------------------------------

    @Test
    fun lineHeight_isIdenticalForBeforeAndAfter() {
        val before = layoutOf(VISIBLE_CHAR, VISIBLE_CHAR, AnnotationPosition.Before)
        val after = layoutOf(VISIBLE_CHAR, VISIBLE_CHAR, AnnotationPosition.After)

        // The same amount of space is reserved either way, only on a different side of the
        // baseline, so the total line height must not depend on the position.
        assertThat(after.height).isEqualTo(before.height)
    }

    @Test
    fun positionAfter_baselineSitsAboveTheBeforeBaseline() {
        val before = layoutOf(VISIBLE_CHAR, VISIBLE_CHAR, AnnotationPosition.Before)
        val after = layoutOf(VISIBLE_CHAR, VISIBLE_CHAR, AnnotationPosition.After)

        // Before reserves the ruby box in the ascent, which pushes the baseline down. After
        // reserves it in the descent instead, so the baseline stays near the top of the line.
        assertThat(after.getLineBaseline(0)).isLessThan(before.getLineBaseline(0))
    }

    // ---------------------------------------------------------------------------------------
    // Two ruby spans in one paragraph. The platform gives one FontMetricsInt to every run and
    // does not clear it. A span that merges the box of an earlier run into its own box makes the
    // later line too tall. See b/561323466.
    // ---------------------------------------------------------------------------------------

    @Test
    fun lineHeight_isIdenticalForTwoRubySpansInOneParagraph() {
        val layout = layoutOfTwoRubyLines()
        assertThat(layout.lineCount).isEqualTo(2)

        val firstLine = layout.getLineBottom(0) - layout.getLineTop(0)
        val secondLine = layout.getLineBottom(1) - layout.getLineTop(1)
        assertThat(secondLine).isEqualTo(firstLine)
    }

    // ---------------------------------------------------------------------------------------
    // Unrecognized positions fall back to the default, matching the vertical layout.
    // ---------------------------------------------------------------------------------------

    @Test
    fun positionUnknown_isTreatedAsBefore() {
        val rendered = render(INVISIBLE_CHAR, VISIBLE_CHAR, AnnotationPosition.Unknown)

        rendered.assertBandIsDrawn(rendered.rubyBandAbove)
        rendered.assertBandIsEmpty(rendered.bodyBand)
    }

    // ---------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------

    /** Lays out [baseText] carrying a [RubySpan], the way any horizontal text widget would. */
    private fun layoutOf(
        baseText: String,
        rubyText: String,
        position: AnnotationPosition,
    ): StaticLayout {
        val spannable = SpannableString(baseText)
        spannable.setSpan(
            RubySpan(rubyText, position, textScale = RUBY_SCALE),
            0,
            baseText.length,
            Spanned.SPAN_INCLUSIVE_EXCLUSIVE,
        )
        return StaticLayout.Builder.obtain(spannable, 0, spannable.length, paint, BITMAP_SIZE)
            .build()
    }

    /**
     * Lays out two ruby spans in one paragraph, one span to a line. The first span puts the
     * annotation before the base text and the second span puts it after. The two spans write
     * different boxes, but they reserve the same height.
     */
    private fun layoutOfTwoRubyLines(): StaticLayout {
        val spannable = SpannableString(VISIBLE_CHAR + VISIBLE_CHAR)
        spannable.setSpan(
            RubySpan(VISIBLE_CHAR, AnnotationPosition.Before, textScale = RUBY_SCALE),
            0,
            1,
            Spanned.SPAN_INCLUSIVE_EXCLUSIVE,
        )
        spannable.setSpan(
            RubySpan(VISIBLE_CHAR, AnnotationPosition.After, textScale = RUBY_SCALE),
            1,
            2,
            Spanned.SPAN_INCLUSIVE_EXCLUSIVE,
        )
        return StaticLayout.Builder.obtain(spannable, 0, spannable.length, paint, ONE_SPAN_WIDTH)
            .build()
    }

    /** Draws [layoutOf] into a white bitmap and derives the bands the annotation may occupy. */
    private fun render(
        baseText: String,
        rubyText: String,
        position: AnnotationPosition,
    ): RubyRender {
        val layout = layoutOf(baseText, rubyText, position)

        val bitmap = Bitmap.createBitmap(BITMAP_SIZE, BITMAP_SIZE, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(Color.WHITE)
            translate(0f, DRAW_ORIGIN_Y.toFloat())
            layout.draw(this)
        }

        val (bodyAscent, bodyDescent) = RubyPixel.lineMetrics(paint, baseText, 1f)
        val (rubyAscent, rubyDescent) = RubyPixel.lineMetrics(paint, rubyText, RUBY_SCALE)

        return RubyRender(
            bitmap = bitmap,
            baseline = DRAW_ORIGIN_Y + layout.getLineBaseline(0),
            bodyAscent = bodyAscent,
            bodyDescent = bodyDescent,
            rubyLineHeight = rubyDescent - rubyAscent,
        )
    }
}
