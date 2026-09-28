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
import android.graphics.Rect
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.BackgroundColorSpan
import android.text.style.CharacterStyle
import android.text.style.ForegroundColorSpan
import android.text.style.MetricAffectingSpan
import android.text.style.ReplacementSpan
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.text.vertical.RubyPixel.ANTIALIAS_MARGIN
import androidx.text.vertical.RubyPixel.FOREGROUND_THRESHOLD
import androidx.text.vertical.RubyPixel.RUBY_SCALE
import androidx.text.vertical.RubyPixel.VISIBLE_CHAR
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.ceil
import kotlin.math.max
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

private const val BITMAP_WIDTH = 320
private const val BITMAP_HEIGHT = 220
private const val BACKGROUND_COLOR = Color.YELLOW
private const val TRANSLUCENT_BACKGROUND_COLOR = 0x800000FF.toInt()
private const val PARTIAL_BACKGROUND_COLOR = Color.CYAN

/** A text pixel counts as red or blue if that channel is larger than the other by this value. */
private const val COLOR_DIFFERENCE = 128

/**
 * Verifies how [RubySpan] and [EmphasisSpan] fill [TextPaint.bgColor] in horizontal layout.
 *
 * The fill is the same as in [VerticalTextLayout]:
 * - [RubySpan] fills the row of the body text and the row of the ruby text across the full span
 *   width. Where one text is narrower than the other, the area next to the narrower text is also
 *   filled.
 * - [EmphasisSpan] fills one box that includes the band of the emphasis marks.
 *
 * The fill uses the same color as plain text. A span that covers only part of the body text fills
 * only its characters.
 *
 * Each text has one plain character before the span. The platform fills the plain character, and
 * the tests use it to get the color of one fill.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class HorizontalSpanBackgroundTest {

    private val bitmaps = mutableListOf<Bitmap>()

    @After
    fun recycleBitmaps() {
        bitmaps.forEach { it.recycle() }
    }

    @Test
    fun rubySpan_rubyWiderThanBody_fillsBodyRowAcrossSpanWidth() {
        val rubyText = VISIBLE_CHAR.repeat(3)
        val rendered = render(RubySpan(rubyText, textScale = RUBY_SCALE), 1)
        val boxes = rendered.rubyBoxes(rubyText, AnnotationPosition.Before)

        rendered.assertCornersAre(boxes.body, BACKGROUND_COLOR)
        rendered.assertCornersAre(boxes.ruby, BACKGROUND_COLOR)
        // The ruby text is wider, and the areas next to the body text are also filled.
        rendered.assertCornersAre(boxes.leftOfBody, BACKGROUND_COLOR)
        rendered.assertCornersAre(boxes.rightOfBody, BACKGROUND_COLOR)
        rendered.assertSpanTextIsDrawn()
    }

    @Test
    fun rubySpan_bodyWiderThanRuby_fillsRubyRowAcrossSpanWidth() {
        val rendered = render(RubySpan(VISIBLE_CHAR, textScale = RUBY_SCALE), 2)
        val boxes = rendered.rubyBoxes(VISIBLE_CHAR, AnnotationPosition.Before)

        rendered.assertCornersAre(boxes.body, BACKGROUND_COLOR)
        rendered.assertCornersAre(boxes.ruby, BACKGROUND_COLOR)
        // The body text is wider, and the areas next to the ruby text are also filled.
        rendered.assertCornersAre(boxes.leftOfRuby, BACKGROUND_COLOR)
        rendered.assertCornersAre(boxes.rightOfRuby, BACKGROUND_COLOR)
        rendered.assertSpanTextIsDrawn()
    }

    @Test
    fun rubySpan_positionAfter_fillsRubyRowBelowBody() {
        val rubyText = VISIBLE_CHAR.repeat(3)
        val rendered =
            render(RubySpan(rubyText, AnnotationPosition.After, textScale = RUBY_SCALE), 1)
        val boxes = rendered.rubyBoxes(rubyText, AnnotationPosition.After)

        rendered.assertCornersAre(boxes.body, BACKGROUND_COLOR)
        rendered.assertCornersAre(boxes.ruby, BACKGROUND_COLOR)
        rendered.assertCornersAre(boxes.leftOfBody, BACKGROUND_COLOR)
        rendered.assertCornersAre(boxes.rightOfBody, BACKGROUND_COLOR)
        rendered.assertSpanTextIsDrawn()
    }

    @Test
    fun emphasisSpan_fillsBoxIncludingMarkBand() {
        val rendered = render(EmphasisSpan(), 2)

        rendered.assertCornersAre(rendered.spanBox, BACKGROUND_COLOR)
        rendered.assertSpanTextIsDrawn()
    }

    @Test
    fun emphasisSpan_positionAfter_fillsBoxIncludingMarkBandBelowBody() {
        val rendered = render(EmphasisSpan(position = AnnotationPosition.After), 2)

        rendered.assertCornersAre(rendered.spanBox, BACKGROUND_COLOR)
        rendered.assertSpanTextIsDrawn()
    }

    @Test
    fun rubySpan_noBackgroundColor_doesNotFill() {
        val rubyText = VISIBLE_CHAR.repeat(3)
        val rendered = render(RubySpan(rubyText, textScale = RUBY_SCALE), 1, bgColor = 0)
        val boxes = rendered.rubyBoxes(rubyText, AnnotationPosition.Before)

        rendered.assertCornersAre(rendered.spanBox, Color.WHITE)
        rendered.assertCornersAre(boxes.leftOfBody, Color.WHITE)
        rendered.assertCornersAre(boxes.rightOfBody, Color.WHITE)
    }

    @Test
    fun rubySpan_translucentBackgroundColor_fillsOnlyOnce() {
        val rendered =
            render(
                RubySpan(VISIBLE_CHAR, textScale = RUBY_SCALE),
                2,
                bgColor = TRANSLUCENT_BACKGROUND_COLOR,
            )
        val boxes = rendered.rubyBoxes(VISIBLE_CHAR, AnnotationPosition.Before)

        // If an area is filled two times, its color is darker than the plain character.
        val expected = rendered.plainTextColor()
        assertWithMessage("plain text fill").that(expected).isNotEqualTo(Color.WHITE)
        rendered.assertCornersAre(boxes.body, expected)
        rendered.assertCornersAre(boxes.ruby, expected)
        rendered.assertCornersAre(boxes.leftOfRuby, expected)
        rendered.assertCornersAre(boxes.rightOfRuby, expected)
    }

    @Test
    fun emphasisSpan_translucentBackgroundColor_fillsOnlyOnce() {
        val rendered = render(EmphasisSpan(), 2, bgColor = TRANSLUCENT_BACKGROUND_COLOR)

        val expected = rendered.plainTextColor()
        assertWithMessage("plain text fill").that(expected).isNotEqualTo(Color.WHITE)
        rendered.assertCornersAre(rendered.spanBox, expected)
    }

    @Test
    fun rubySpan_coveringMetricAffectingSpanWithTranslucentBgColor_fillsOnlyOnce() {
        val rendered =
            render(
                RubySpan(VISIBLE_CHAR, textScale = RUBY_SCALE),
                2,
                bgColor = 0,
                coveringBgColor = TRANSLUCENT_BACKGROUND_COLOR,
            )
        val boxes = rendered.rubyBoxes(VISIBLE_CHAR, AnnotationPosition.Before)

        val expected = rendered.plainTextColor()
        assertWithMessage("plain text fill").that(expected).isNotEqualTo(Color.WHITE)
        rendered.assertCornersAre(boxes.body, expected)
        rendered.assertCornersAre(boxes.ruby, expected)
        rendered.assertCornersAre(boxes.leftOfRuby, expected)
        rendered.assertCornersAre(boxes.rightOfRuby, expected)
    }

    @Test
    fun emphasisSpan_coveringMetricAffectingSpanWithTranslucentBgColor_fillsOnlyOnce() {
        val rendered =
            render(
                EmphasisSpan(),
                2,
                bgColor = 0,
                coveringBgColor = TRANSLUCENT_BACKGROUND_COLOR,
            )

        val expected = rendered.plainTextColor()
        assertWithMessage("plain text fill").that(expected).isNotEqualTo(Color.WHITE)
        rendered.assertCornersAre(rendered.spanBox, expected)
    }

    @Test
    fun emphasisSpan_coveringStrokeSpan_fillsBox() {
        // HorizontalEmphasisSpanLayout applies StrokeSpan to a pooled paint when it finds the
        // positions of the marks. The fill must not get Paint.Style.STROKE from that paint.
        val rendered = render(EmphasisSpan(), 2, coveringStyle = StrokeSpan())

        rendered.assertCornersAre(rendered.spanBox, BACKGROUND_COLOR)
    }

    @Test
    fun rubySpan_afterVerticalTextWithStrokeSpan_fillsBodyAndRubyRows() {
        // Vertical text and horizontal spans get paints from the same pool on a thread. This
        // vertical text applies StrokeSpan to a pooled paint.
        val strokeText =
            SpannableString(VISIBLE_CHAR).apply {
                setSpan(StrokeSpan(), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        val strokePaint = RubyPixel.newPaint()
        UprightLayoutRun(strokeText, 0, strokeText.length, strokePaint)
            .draw(Canvas(), 0f, 0f, strokePaint)

        val rubyText = VISIBLE_CHAR.repeat(3)
        val rendered = render(RubySpan(rubyText, textScale = RUBY_SCALE), 1)
        val boxes = rendered.rubyBoxes(rubyText, AnnotationPosition.Before)

        rendered.assertCornersAre(boxes.body, BACKGROUND_COLOR)
        rendered.assertCornersAre(boxes.ruby, BACKGROUND_COLOR)
    }

    @Test
    fun rubySpan_coveringBackgroundColorSpan_fillsBodyAndRubyRows() {
        // TextLine does not apply a BackgroundColorSpan to the paint of a ReplacementSpan.
        val rubyText = VISIBLE_CHAR.repeat(3)
        val rendered =
            render(
                RubySpan(rubyText, textScale = RUBY_SCALE),
                1,
                bgColor = 0,
                coveringStyle = BackgroundColorSpan(BACKGROUND_COLOR),
            )
        val boxes = rendered.rubyBoxes(rubyText, AnnotationPosition.Before)

        rendered.assertCornersAre(boxes.body, BACKGROUND_COLOR)
        rendered.assertCornersAre(boxes.ruby, BACKGROUND_COLOR)
        rendered.assertCornersAre(boxes.leftOfBody, BACKGROUND_COLOR)
        rendered.assertCornersAre(boxes.rightOfBody, BACKGROUND_COLOR)
        rendered.assertSpanTextIsDrawn()
    }

    @Test
    fun emphasisSpan_coveringBackgroundColorSpan_fillsBox() {
        val rendered =
            render(
                EmphasisSpan(),
                2,
                bgColor = 0,
                coveringStyle = BackgroundColorSpan(BACKGROUND_COLOR),
            )

        rendered.assertCornersAre(rendered.spanBox, BACKGROUND_COLOR)
        rendered.assertSpanTextIsDrawn()
    }

    @Test
    fun rubySpan_partialBackgroundColorSpan_fillsOnlyItsCharacter() {
        val rendered =
            render(
                RubySpan(VISIBLE_CHAR, textScale = RUBY_SCALE),
                2,
                bgColor = 0,
                partialStyle = BackgroundColorSpan(PARTIAL_BACKGROUND_COLOR),
            )
        val boxes = rendered.rubyBoxes(VISIBLE_CHAR, AnnotationPosition.Before)

        rendered.assertCornersAre(boxes.lastBodyChar, PARTIAL_BACKGROUND_COLOR)
        rendered.assertCornersAre(boxes.otherBodyChars, Color.WHITE)
        rendered.assertCornersAre(boxes.ruby, Color.WHITE)
    }

    @Test
    fun rubySpan_partialBackgroundColorSpanWithPriority_fillsItsCharacter() {
        // Spanned.getSpans returns a span that has a priority before the spans that have none.
        val rendered =
            render(
                RubySpan(VISIBLE_CHAR, textScale = RUBY_SCALE),
                2,
                bgColor = 0,
                partialStyle = BackgroundColorSpan(PARTIAL_BACKGROUND_COLOR),
                partialStyleFlags =
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE or (1 shl Spanned.SPAN_PRIORITY_SHIFT),
            )
        val boxes = rendered.rubyBoxes(VISIBLE_CHAR, AnnotationPosition.Before)

        rendered.assertCornersAre(boxes.lastBodyChar, PARTIAL_BACKGROUND_COLOR)
    }

    @Test
    fun rubySpan_partialBackgroundColorSpanSetBeforeCoveringSpan_usesCoveringColor() {
        // In plain text, the span that is set last gives the color.
        val rendered =
            render(
                RubySpan(VISIBLE_CHAR, textScale = RUBY_SCALE),
                2,
                bgColor = 0,
                coveringBgColor = BACKGROUND_COLOR,
                partialStyle = BackgroundColorSpan(PARTIAL_BACKGROUND_COLOR),
            )
        val boxes = rendered.rubyBoxes(VISIBLE_CHAR, AnnotationPosition.Before)

        rendered.assertCornersAre(boxes.lastBodyChar, BACKGROUND_COLOR)
        rendered.assertCornersAre(boxes.otherBodyChars, BACKGROUND_COLOR)
        rendered.assertCornersAre(boxes.ruby, BACKGROUND_COLOR)
    }

    @Test
    fun rubySpan_partialForegroundColorSpanSetBeforeCoveringSpan_usesCoveringColor() {
        val rendered =
            render(
                RubySpan(VISIBLE_CHAR, textScale = RUBY_SCALE),
                2,
                bgColor = 0,
                coveringStyle = ForegroundColorSpan(Color.RED),
                partialStyle = ForegroundColorSpan(Color.BLUE),
            )
        val box = rendered.rubyBoxes(VISIBLE_CHAR, AnnotationPosition.Before).lastBodyChar

        assertWithMessage("blue pixels in box %s", box)
            .that(rendered.countPixels(box) { Color.blue(it) - Color.red(it) > COLOR_DIFFERENCE })
            .isEqualTo(0)
        assertWithMessage("red pixels in box %s", box)
            .that(rendered.countPixels(box) { Color.red(it) - Color.blue(it) > COLOR_DIFFERENCE })
            .isGreaterThan(0)
    }

    @Test
    fun drawSpanBackground_invertedBox_drawsNothing() {
        val bitmap = newBitmap()
        Canvas(bitmap).apply {
            drawColor(Color.WHITE)
            // Canvas.drawRect sorts the edges of a box, so it also fills a box with inverted edges.
            drawSpanBackground(20f, 10f, 10f, 30f, BACKGROUND_COLOR)
            drawSpanBackground(40f, 30f, 60f, 10f, BACKGROUND_COLOR)
        }

        assertThat(bitmap.getPixel(15, 20)).isEqualTo(Color.WHITE)
        assertThat(bitmap.getPixel(50, 20)).isEqualTo(Color.WHITE)
    }

    /** Creates a bitmap that [recycleBitmaps] recycles after the test. */
    private fun newBitmap(): Bitmap =
        Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Bitmap.Config.ARGB_8888).also {
            bitmaps += it
        }

    /**
     * Draws one plain character, then [bodyLength] characters that carry [span], into a white
     * bitmap.
     *
     * The text gets the spans in this order: [partialStyle] on the last character, a
     * [MetricBackgroundColorSpan] of [coveringBgColor] on all of the characters, [coveringStyle] on
     * all of the characters, and then [span].
     */
    private fun render(
        span: ReplacementSpan,
        bodyLength: Int,
        bgColor: Int = BACKGROUND_COLOR,
        coveringBgColor: Int = 0,
        coveringStyle: CharacterStyle? = null,
        partialStyle: CharacterStyle? = null,
        partialStyleFlags: Int = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
    ): Rendered {
        val text = VISIBLE_CHAR.repeat(1 + bodyLength)
        val spannable = SpannableString(text)
        if (partialStyle != null) {
            spannable.setSpan(partialStyle, text.length - 1, text.length, partialStyleFlags)
        }
        if (coveringBgColor != 0) {
            spannable.setSpan(
                MetricBackgroundColorSpan(coveringBgColor),
                0,
                text.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
        if (coveringStyle != null) {
            spannable.setSpan(coveringStyle, 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        spannable.setSpan(span, 1, text.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)

        val paint = RubyPixel.newPaint()
        val layout =
            StaticLayout.Builder.obtain(spannable, 0, spannable.length, paint, BITMAP_WIDTH).build()
        assertWithMessage("the layout must have one line").that(layout.lineCount).isEqualTo(1)
        assertWithMessage("the layout must fit in the bitmap")
            .that(layout.height)
            .isLessThan(BITMAP_HEIGHT)

        // The span box is the box that the span reserves in getSize.
        val fm = Paint.FontMetricsInt()
        val spanWidth = span.getSize(paint, spannable, 1, text.length, fm)
        val baseline = layout.getLineBaseline(0)
        val spanLeft = layout.getPrimaryHorizontal(1).toInt()

        // The Layout constructor sets bgColor to 0, so set it after the build.
        paint.bgColor = bgColor
        val bitmap = newBitmap()
        Canvas(bitmap).apply {
            drawColor(Color.WHITE)
            layout.draw(this)
        }

        return Rendered(
            bitmap = bitmap,
            paint = paint,
            bodyText = text.substring(1),
            baseline = baseline,
            plainBox =
                Rect(
                    layout.getPrimaryHorizontal(0).toInt(),
                    layout.getLineTop(0),
                    spanLeft,
                    layout.getLineBottom(0),
                ),
            spanBox =
                Rect(spanLeft, baseline + fm.ascent, spanLeft + spanWidth, baseline + fm.descent),
        )
    }

    /** A [MetricAffectingSpan] that sets [TextPaint.bgColor], like Compose `withStyle` does. */
    private class MetricBackgroundColorSpan(private val color: Int) : MetricAffectingSpan() {
        override fun updateMeasureState(textPaint: TextPaint) {
            textPaint.bgColor = color
        }

        override fun updateDrawState(tp: TextPaint) {
            tp.bgColor = color
        }
    }

    /** Draws the text as an outline. */
    private class StrokeSpan : CharacterStyle() {
        override fun updateDrawState(tp: TextPaint) {
            tp.style = Paint.Style.STROKE
        }
    }

    /** The boxes of a [RubySpan], in bitmap coordinates. */
    private class RubyBoxes(
        val body: Rect,
        val ruby: Rect,
        val leftOfBody: Rect,
        val rightOfBody: Rect,
        val leftOfRuby: Rect,
        val rightOfRuby: Rect,
        /** The box of the last character of the body text. */
        val lastBodyChar: Rect,
        /** The box of the body text characters before the last character. */
        val otherBodyChars: Rect,
    )

    private class Rendered(
        val bitmap: Bitmap,
        private val paint: TextPaint,
        private val bodyText: String,
        private val baseline: Int,
        private val plainBox: Rect,
        val spanBox: Rect,
    ) {
        /** Returns the boxes of a [RubySpan], measured the way [HorizontalRubySpanLayout] does. */
        fun rubyBoxes(rubyText: String, position: AnnotationPosition): RubyBoxes {
            val (bodyAscent, bodyDescent) = RubyPixel.lineMetrics(paint, bodyText, 1f)
            val (rubyAscent, rubyDescent) = RubyPixel.lineMetrics(paint, rubyText, RUBY_SCALE)
            val rubyLineHeight = rubyDescent - rubyAscent
            val rubyPaint = TextPaint(paint).apply { textSize *= RUBY_SCALE }
            val bodyWidth = ceil(Layout.getDesiredWidth(bodyText, paint)).toInt()
            val rubyWidth = ceil(Layout.getDesiredWidth(rubyText, rubyPaint)).toInt()
            val spanWidth = max(bodyWidth, rubyWidth)
            val bodyLeft = spanBox.left + (spanWidth - bodyWidth) / 2
            val rubyLeft = spanBox.left + (spanWidth - rubyWidth) / 2
            val lastCharLeft =
                bodyLeft + Layout.getDesiredWidth(bodyText, 0, bodyText.length - 1, paint)

            val bodyTop = baseline + bodyAscent
            val bodyBottom = baseline + bodyDescent
            val rubyTop =
                if (position == AnnotationPosition.After) bodyBottom else bodyTop - rubyLineHeight
            val rubyBottom = rubyTop + rubyLineHeight
            return RubyBoxes(
                body = Rect(bodyLeft, bodyTop, bodyLeft + bodyWidth, bodyBottom),
                ruby = Rect(rubyLeft, rubyTop, rubyLeft + rubyWidth, rubyBottom),
                leftOfBody = Rect(spanBox.left, bodyTop, bodyLeft, bodyBottom),
                rightOfBody = Rect(bodyLeft + bodyWidth, bodyTop, spanBox.right, bodyBottom),
                leftOfRuby = Rect(spanBox.left, rubyTop, rubyLeft, rubyBottom),
                rightOfRuby = Rect(rubyLeft + rubyWidth, rubyTop, spanBox.right, rubyBottom),
                lastBodyChar =
                    Rect(ceil(lastCharLeft).toInt(), bodyTop, bodyLeft + bodyWidth, bodyBottom),
                otherBodyChars = Rect(bodyLeft, bodyTop, lastCharLeft.toInt(), bodyBottom),
            )
        }

        /** Returns the fill color of the plain character, which the platform draws. */
        fun plainTextColor(): Int {
            val (x, y) = corners(plainBox).first()
            return bitmap.getPixel(x, y)
        }

        /** Checks the four corners of [box], moved in to stay clear of antialiased edges. */
        fun assertCornersAre(box: Rect, color: Int) {
            assertWithMessage("box %s is too small to check", box)
                .that(box.width() > 2 * ANTIALIAS_MARGIN && box.height() > 2 * ANTIALIAS_MARGIN)
                .isTrue()
            for ((x, y) in corners(box)) {
                assertWithMessage("pixel at (%s, %s) in box %s", x, y, box)
                    .that(bitmap.getPixel(x, y))
                    .isEqualTo(color)
            }
        }

        /** Checks that the fill does not cover the text of the span. */
        fun assertSpanTextIsDrawn() {
            assertWithMessage("dark pixels in the span box")
                .that(countPixels(spanBox) { Color.red(it) < FOREGROUND_THRESHOLD })
                .isGreaterThan(0)
        }

        /** Returns the number of pixels in [box] that [predicate] accepts. */
        fun countPixels(box: Rect, predicate: (color: Int) -> Boolean): Int {
            var count = 0
            for (y in box.top until box.bottom) {
                for (x in box.left until box.right) {
                    if (predicate(bitmap.getPixel(x, y))) count++
                }
            }
            return count
        }

        private fun corners(box: Rect): List<Pair<Int, Int>> {
            val l = box.left + ANTIALIAS_MARGIN
            val r = box.right - 1 - ANTIALIAS_MARGIN
            val t = box.top + ANTIALIAS_MARGIN
            val b = box.bottom - 1 - ANTIALIAS_MARGIN
            return listOf(l to t, r to t, l to b, r to b)
        }
    }
}
