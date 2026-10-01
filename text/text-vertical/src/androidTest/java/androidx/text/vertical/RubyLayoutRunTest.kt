/*
 * Copyright 2025 The Android Open Source Project
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

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.text.NoCopySpan
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.CharacterStyle
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

private const val SPAN_FLAG = SpannableString.SPAN_INCLUSIVE_EXCLUSIVE

@RunWith(AndroidJUnit4::class)
@SmallTest
class RubyLayoutRunTest {
    private val PREFIX = "PREFIX_PREFIX_PREFIX"
    private val SUFFIX = "SUFFIX_SUFFIX_SUFFIX"
    private val LATIN_TEXT = "abcde"
    private val RUBY_TEXT = "ABCDE"

    private val TEXT = PREFIX + LATIN_TEXT + SUFFIX
    private val LATIN_START = PREFIX.length
    private val LATIN_END = LATIN_START + LATIN_TEXT.length

    private val ONE_EM = 10f // make 1em = 10px
    private val HALF_EM = ONE_EM / 2

    private val PAINT = TextPaint().apply { textSize = ONE_EM }

    private fun getVerticalAdvance(text: String, scaleFactor: Float = 1.0f): Float =
        PAINT.withTextScale(scaleFactor) { measureTextVertical(text) }

    private fun getHorizontalAdvance(text: String, scaleFactor: Float = 1.0f): Float =
        PAINT.withTextScale(scaleFactor) { measureText(text) }

    private class MockCanvas() : Canvas() {
        data class DrawRectCall(
            val left: Float,
            val top: Float,
            val right: Float,
            val bottom: Float,
            val color: Int,
        )

        data class DrawTextRunCall(
            val text: CharSequence,
            val start: Int,
            val end: Int,
            val paint: Paint,
            val skewX: Float = 0f,
            val skewY: Float = 0f,
        )

        val invocations = mutableListOf<DrawTextRunCall>()
        val drawnRects = mutableListOf<DrawRectCall>()
        private val skewStack = ArrayDeque<Pair<Float, Float>>()
        private var currentSkewX = 0f
        private var currentSkewY = 0f

        override fun save(): Int {
            skewStack.addLast(currentSkewX to currentSkewY)
            return super.save()
        }

        override fun restore() {
            val popped = skewStack.removeLastOrNull()
            if (popped != null) {
                currentSkewX = popped.first
                currentSkewY = popped.second
            }
            super.restore()
        }

        override fun restoreToCount(saveCount: Int) {
            // Canvas() starts at save count 1, so skewStack.size == saveCount after the last
            // save().
            while (skewStack.size >= saveCount && skewStack.isNotEmpty()) {
                val popped = skewStack.removeLast()
                currentSkewX = popped.first
                currentSkewY = popped.second
            }
            super.restoreToCount(saveCount)
        }

        override fun skew(sx: Float, sy: Float) {
            currentSkewX += sx
            currentSkewY += sy
            super.skew(sx, sy)
        }

        override fun drawRect(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            paint: Paint,
        ) {
            super.drawRect(left, top, right, bottom, paint)
            drawnRects.add(DrawRectCall(left, top, right, bottom, paint.color))
        }

        override fun drawText(
            text: CharSequence,
            start: Int,
            end: Int,
            x: Float,
            y: Float,
            paint: Paint,
        ) {
            super.drawText(text, start, end, x, y, paint)
            invocations.add(
                DrawTextRunCall(text, start, end, Paint(paint), currentSkewX, currentSkewY)
            )
        }
    }

    @Test
    fun rubyLayoutRun_CreateRubyShorterThanBaseText() {
        val rubySpan = RubySpan(RUBY_TEXT)
        RubyLayoutRun(TEXT, LATIN_START, LATIN_END, TextOrientation.Mixed, PAINT, rubySpan).run {
            assertThat(start).isEqualTo(LATIN_START)
            assertThat(end).isEqualTo(LATIN_END)
            assertThat(width).isEqualTo(ONE_EM * 1.5f) // 1em for base text, 0.5em for ruby.
            // Since the ruby is shorter than base text, the base text is height for the run.
            assertThat(height).isEqualTo(getHorizontalAdvance(LATIN_TEXT))
            assertThat(leftSideOffset).isEqualTo(-HALF_EM) // leftSide is half of 1em
            assertThat(rightSideOffset)
                .isEqualTo(ONE_EM) // right half of 1em + 0.5em for ruby width.

            val mock = MockCanvas()
            draw(mock, 0f, 0f, PAINT)
            assertThat(mock.invocations.size).isEqualTo(2)
            val bodyIndex = if (mock.invocations[0].text == TEXT) 0 else 1
            val rubyIndex = if (bodyIndex == 0) 1 else 0

            mock.invocations[bodyIndex].run {
                assertThat(start).isEqualTo(LATIN_START)
                assertThat(end).isEqualTo(LATIN_END)
                assertThat(paint.hasVerticalTextFlag()).isFalse()
                assertThat(paint.textSize).isEqualTo(PAINT.textSize)
            }
            mock.invocations[rubyIndex].run {
                assertThat(start).isEqualTo(0)
                assertThat(end).isEqualTo(RUBY_TEXT.length)
                assertThat(paint.hasVerticalTextFlag()).isFalse()
                assertThat(paint.textSize).isEqualTo(PAINT.textSize * 0.5f)
            }
        }
    }

    @Test
    fun rubyLayoutRun_CreateRubyLongerThanBaseText() {
        val LONG_RUBY_TEXT = RUBY_TEXT.repeat(10)
        val rubySpan = RubySpan(LONG_RUBY_TEXT)
        RubyLayoutRun(TEXT, LATIN_START, LATIN_END, TextOrientation.Mixed, PAINT, rubySpan).run {
            assertThat(start).isEqualTo(LATIN_START)
            assertThat(end).isEqualTo(LATIN_END)
            assertThat(width).isEqualTo(ONE_EM * 1.5f) // 1em for base text, 0.5em for ruby.
            // Since the ruby is longer than base text, the ruby text is height for the run.
            assertThat(height).isEqualTo(getHorizontalAdvance(LONG_RUBY_TEXT, 0.5f /* scale */))
            assertThat(leftSideOffset).isEqualTo(-HALF_EM) // leftSide is half of 1em
            assertThat(rightSideOffset)
                .isEqualTo(ONE_EM) // right half of 1em + 0.5em for ruby width.

            val mock = MockCanvas()
            draw(mock, 0f, 0f, PAINT)
            assertThat(mock.invocations.size).isEqualTo(2)
            val bodyIndex = if (mock.invocations[0].text == TEXT) 0 else 1
            val rubyIndex = if (bodyIndex == 0) 1 else 0

            mock.invocations[bodyIndex].run {
                assertThat(start).isEqualTo(LATIN_START)
                assertThat(end).isEqualTo(LATIN_END)
                assertThat(paint.hasVerticalTextFlag()).isFalse()
                assertThat(paint.textSize).isEqualTo(PAINT.textSize)
            }
            mock.invocations[rubyIndex].run {
                assertThat(start).isEqualTo(0)
                assertThat(end).isEqualTo(LONG_RUBY_TEXT.length)
                assertThat(paint.hasVerticalTextFlag()).isFalse()
                assertThat(paint.textSize).isEqualTo(PAINT.textSize * 0.5f)
            }
        }
    }

    @Test
    fun rubyLayoutRun_CreateRubyUprightOrientation() {
        val LONG_RUBY_TEXT = RUBY_TEXT.repeat(10)
        val rubySpan = RubySpan(LONG_RUBY_TEXT, orientation = TextOrientation.Upright)
        RubyLayoutRun(TEXT, LATIN_START, LATIN_END, TextOrientation.Mixed, PAINT, rubySpan).run {
            assertThat(start).isEqualTo(LATIN_START)
            assertThat(end).isEqualTo(LATIN_END)
            assertThat(width).isEqualTo(ONE_EM * 1.5f) // 1em for base text, 0.5em for ruby.
            // The ruby text is layout with Upright orientation. Therefore, the vertical advance
            // is used for the height.
            assertThat(height).isEqualTo(getVerticalAdvance(LONG_RUBY_TEXT, 0.5f /* scale */))
            assertThat(leftSideOffset).isEqualTo(-HALF_EM) // leftSide is half of 1em
            assertThat(rightSideOffset)
                .isEqualTo(ONE_EM) // right half of 1em + 0.5em for ruby width.

            val mock = MockCanvas()
            draw(mock, 0f, 0f, PAINT)
            val usesVerticalTextFlag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA

            assertThat(mock.invocations).isNotEmpty()
            mock.invocations[0].run {
                assertThat(text).isEqualTo(TEXT)
                assertThat(start).isEqualTo(LATIN_START)
                assertThat(end).isEqualTo(LATIN_END)
                assertThat(paint.hasVerticalTextFlag()).isFalse()
                assertThat(paint.textSize).isEqualTo(PAINT.textSize)
            }
            val rubyInvocations = mock.invocations.drop(1)
            for (invocation in rubyInvocations) {
                assertThat(invocation.text).isEqualTo(LONG_RUBY_TEXT)
                assertThat(invocation.paint.hasVerticalTextFlag()).isEqualTo(usesVerticalTextFlag)
                assertThat(invocation.paint.textSize).isEqualTo(PAINT.textSize * 0.5f)
            }
            val rubyRanges = rubyInvocations.map { it.start to it.end }
            if (usesVerticalTextFlag) {
                assertThat(rubyRanges).containsExactly(0 to LONG_RUBY_TEXT.length)
            } else {
                assertThat(rubyRanges)
                    .containsExactlyElementsIn((0 until LONG_RUBY_TEXT.length).map { it to it + 1 })
                    .inOrder()
            }
        }
    }

    @Test
    fun rubyLayoutRun_CreateRubyScale() {
        val LONG_RUBY_TEXT = RUBY_TEXT.repeat(10)
        val rubySpan = RubySpan(LONG_RUBY_TEXT, textScale = 0.3f)
        RubyLayoutRun(TEXT, LATIN_START, LATIN_END, TextOrientation.Mixed, PAINT, rubySpan).run {
            assertThat(start).isEqualTo(LATIN_START)
            assertThat(end).isEqualTo(LATIN_END)
            assertThat(width).isEqualTo(ONE_EM * 1.3f) // 1em for base text, 0.5em for ruby.
            // The ruby text is layout with Upright orientation. Therefore, the vertical advance
            // is used for the height.
            assertThat(height).isEqualTo(getHorizontalAdvance(LONG_RUBY_TEXT, 0.3f /* scale */))
            assertThat(leftSideOffset).isEqualTo(-HALF_EM) // leftSide is half of 1em
            // right half of 1em + 0.5em for ruby width.
            assertThat(rightSideOffset).isEqualTo(HALF_EM + 0.3f * ONE_EM)

            val mock = MockCanvas()
            draw(mock, 0f, 0f, PAINT)
            assertThat(mock.invocations.size).isEqualTo(2)
            val bodyIndex = if (mock.invocations[0].text == TEXT) 0 else 1
            val rubyIndex = if (bodyIndex == 0) 1 else 0

            mock.invocations[bodyIndex].run {
                assertThat(start).isEqualTo(LATIN_START)
                assertThat(end).isEqualTo(LATIN_END)
                assertThat(paint.hasVerticalTextFlag()).isFalse()
                assertThat(paint.textSize).isEqualTo(PAINT.textSize)
            }
            mock.invocations[rubyIndex].run {
                assertThat(start).isEqualTo(0)
                assertThat(end).isEqualTo(LONG_RUBY_TEXT.length)
                assertThat(paint.hasVerticalTextFlag()).isFalse()
                assertThat(paint.textSize).isEqualTo(PAINT.textSize * 0.3f)
            }
        }
    }

    @Test
    fun rubyLayoutRun_PositionBeforeReservesSpaceOnRight() {
        val rubySpan = RubySpan(RUBY_TEXT, AnnotationPosition.Before)
        RubyLayoutRun(TEXT, LATIN_START, LATIN_END, TextOrientation.Mixed, PAINT, rubySpan).run {
            assertThat(width).isEqualTo(ONE_EM * 1.5f)
            assertThat(leftSideOffset).isEqualTo(-HALF_EM)
            assertThat(rightSideOffset).isEqualTo(ONE_EM) // right half of 1em + 0.5em ruby
        }
    }

    @Test
    fun rubyLayoutRun_PositionAfterReservesSpaceOnLeft() {
        val rubySpan = RubySpan(RUBY_TEXT, AnnotationPosition.After)
        RubyLayoutRun(TEXT, LATIN_START, LATIN_END, TextOrientation.Mixed, PAINT, rubySpan).run {
            assertThat(width).isEqualTo(ONE_EM * 1.5f)
            assertThat(leftSideOffset).isEqualTo(-ONE_EM) // left half of 1em + 0.5em ruby
            assertThat(rightSideOffset).isEqualTo(HALF_EM)
        }
    }

    @Test
    fun rubyLayoutRun_propagatesCoveringStyleSpans_toRubyAnnotation() {
        val styledRuby =
            SpannableString("あいう").apply {
                setSpan(ForegroundColorSpan(Color.BLUE), 0, 1, SPAN_FLAG)
            }
        val rubySpan = RubySpan(styledRuby, orientation = TextOrientation.Upright)
        val styledBody =
            SpannableString("漢字").apply {
                setSpan(ForegroundColorSpan(Color.RED), 0, 2, SPAN_FLAG)
                setSpan(BackgroundColorSpan(Color.YELLOW), 0, 2, SPAN_FLAG)
                setSpan(FontShearSpan(0.25f), 0, 2, SPAN_FLAG)
                setSpan(AbsoluteSizeSpan(40), 0, 2, SPAN_FLAG)
                // Partial span on [0, 1) must not propagate to the ruby annotation.
                setSpan(ForegroundColorSpan(Color.GREEN), 0, 1, SPAN_FLAG)
                setSpan(rubySpan, 0, 2, SPAN_FLAG)
            }

        val run = RubyLayoutRun(styledBody, 0, 2, TextOrientation.Mixed, PAINT, rubySpan)
        val mock = MockCanvas()
        run.draw(mock, 60f, 10f, PAINT)

        val rubyCalls = mock.invocations.filter { it.text.toString() == "あいう" }
        val firstCharCalls = rubyCalls.filter { it.start == 0 }
        val remainingCharCalls = rubyCalls.filter { it.start >= 1 }
        assertThat(firstCharCalls).hasSize(1)
        assertThat(remainingCharCalls).isNotEmpty()

        // First ruby character [0, 1): inner ForegroundColorSpan(Color.BLUE) overrides covering
        // Color.RED, while covering FontShearSpan(0.25f), BackgroundColorSpan(Color.YELLOW), and
        // scaled AbsoluteSizeSpan(40) (40f * 0.5f = 20f) still apply.
        assertThat(firstCharCalls[0].end).isEqualTo(1)
        assertThat(firstCharCalls[0].paint.color).isEqualTo(Color.BLUE)
        assertThat(firstCharCalls[0].paint.textSize).isEqualTo(40f * rubySpan.textScale)
        assertThat(firstCharCalls[0].skewY).isEqualTo(-0.25f)

        // Remaining ruby characters in [1, 3): inherit covering ForegroundColorSpan(Color.RED),
        // scaled AbsoluteSizeSpan(40) (20f), and FontShearSpan(0.25f), and do not inherit partial
        // Color.GREEN.
        for (call in remainingCharCalls) {
            assertThat(call.paint.color).isEqualTo(Color.RED)
            assertThat(call.paint.textSize).isEqualTo(40f * rubySpan.textScale)
            assertThat(call.skewY).isEqualTo(-0.25f)
        }

        // With AbsoluteSizeSpan(40) on body (width = 40, rightSide = 20) and scaled ruby size = 20
        // (leftSide = -10, width = 20), ruby column x-bounds are [60 + 20, 60 + 40] = [80, 100].
        val rubyRects = mock.drawnRects.filter { it.left == 80f && it.right == 100f }
        assertThat(rubyRects)
            .containsExactly(
                MockCanvas.DrawRectCall(
                    left = 80f,
                    top = 10f,
                    right = 100f,
                    bottom = 20f,
                    color = Color.YELLOW,
                ),
                MockCanvas.DrawRectCall(
                    left = 80f,
                    top = 80f,
                    right = 100f,
                    bottom = 90f,
                    color = Color.YELLOW,
                ),
                MockCanvas.DrawRectCall(
                    left = 80f,
                    top = 20f,
                    right = 100f,
                    bottom = 40f,
                    color = Color.YELLOW,
                ),
                MockCanvas.DrawRectCall(
                    left = 80f,
                    top = 40f,
                    right = 100f,
                    bottom = 80f,
                    color = Color.YELLOW,
                ),
            )
            .inOrder()
    }

    @Test
    fun rubyLayoutRun_coveringRelativeSizeSpan_appliesRubyScaleOnce() {
        val rubySpan = RubySpan("あいう", orientation = TextOrientation.Upright)
        val styledBody =
            SpannableString("漢字").apply {
                setSpan(RelativeSizeSpan(2f), 0, 2, SPAN_FLAG)
                setSpan(rubySpan, 0, 2, SPAN_FLAG)
            }

        val run = RubyLayoutRun(styledBody, 0, 2, TextOrientation.Mixed, PAINT, rubySpan)
        val mock = MockCanvas()
        run.draw(mock, 0f, 0f, PAINT)

        // RelativeSizeSpan(2f) makes the ruby text two times larger, the same as the base text:
        // 10f * 2f * 0.5f = 10f. If the wrapper applies the ruby scale two times, the size is 5f.
        val rubyCalls = mock.invocations.filter { it.text.toString() == "あいう" }
        assertThat(rubyCalls).isNotEmpty()
        for (call in rubyCalls) {
            assertThat(call.paint.textSize).isEqualTo(ONE_EM * 2f * rubySpan.textScale)
        }
    }

    @Test
    fun rubyLayoutRun_shorterSide_fillsOnlyCenteringSliversWithBaseBackground() {
        val originX = 60f
        val originY = 10f

        // Case A: Body ("漢字", height = 2 * 10 = 20) is taller than Ruby ("あ", height = 1 * 5 = 5).
        // Ruby is shifted by heightDiffHalf = (20 - 5) / 2 = 7.5 -> rubyY = 17.5, rubyBottom =
        // 22.5.
        val rubyA =
            SpannableString("あ").apply {
                setSpan(BackgroundColorSpan(Color.MAGENTA), 0, 1, SPAN_FLAG)
            }
        val shortRubySpan = RubySpan(rubyA, orientation = TextOrientation.Upright)
        val bodyTallerText =
            SpannableString("漢字").apply {
                setSpan(BackgroundColorSpan(Color.YELLOW), 0, 2, SPAN_FLAG)
                setSpan(shortRubySpan, 0, 2, SPAN_FLAG)
            }
        val runA =
            RubyLayoutRun(bodyTallerText, 0, 2, TextOrientation.Upright, PAINT, shortRubySpan)
        val canvasA = MockCanvas()
        runA.draw(canvasA, originX, originY, PAINT)

        val rubyColumnRectsA = canvasA.drawnRects.filter { it.left == 65f && it.right == 70f }
        assertThat(rubyColumnRectsA)
            .containsExactly(
                // Top centering sliver [10.0, 17.5] filled with covering Color.YELLOW
                MockCanvas.DrawRectCall(
                    left = 65f,
                    top = 10f,
                    right = 70f,
                    bottom = 17.5f,
                    color = Color.YELLOW,
                ),
                // Bottom centering sliver [22.5, 30.0] filled with covering Color.YELLOW
                MockCanvas.DrawRectCall(
                    left = 65f,
                    top = 22.5f,
                    right = 70f,
                    bottom = 30f,
                    color = Color.YELLOW,
                ),
                // Ruby style run [17.5, 22.5] drawn by rubyLayoutRuns.draw with inner Color.MAGENTA
                MockCanvas.DrawRectCall(
                    left = 65f,
                    top = 17.5f,
                    right = 70f,
                    bottom = 22.5f,
                    color = Color.MAGENTA,
                ),
            )
            .inOrder()

        // Case B: Ruby ("あいうえおか", height = 6 * 5 = 30) is taller than Body ("漢字", height = 2 * 10 =
        // 20).
        // Body is shifted by -heightDiffHalf = (30 - 20) / 2 = 5 -> bodyY = 15, bodyBottom = 35.
        val longRubySpan = RubySpan("あいうえおか", orientation = TextOrientation.Upright)
        val basePaintWithBg = TextPaint(PAINT).apply { bgColor = Color.CYAN }
        val bodyWithInnerBg =
            SpannableString("漢字").apply {
                // Inner span on [0, 1) does not cover the full [0, 2) ruby range.
                setSpan(BackgroundColorSpan(Color.MAGENTA), 0, 1, SPAN_FLAG)
                setSpan(longRubySpan, 0, 2, SPAN_FLAG)
            }
        val runB =
            RubyLayoutRun(
                bodyWithInnerBg,
                0,
                2,
                TextOrientation.Upright,
                basePaintWithBg,
                longRubySpan,
            )
        val canvasB = MockCanvas()
        runB.draw(canvasB, originX, originY, basePaintWithBg)

        val bodyColumnRectsB = canvasB.drawnRects.filter { it.left == 55f && it.right == 65f }
        assertThat(bodyColumnRectsB)
            .containsExactly(
                // Top centering sliver [10.0, 15.0] filled with covering Color.CYAN
                MockCanvas.DrawRectCall(
                    left = 55f,
                    top = 10f,
                    right = 65f,
                    bottom = 15f,
                    color = Color.CYAN,
                ),
                // Bottom centering sliver [35.0, 40.0] filled with covering Color.CYAN
                MockCanvas.DrawRectCall(
                    left = 55f,
                    top = 35f,
                    right = 65f,
                    bottom = 40f,
                    color = Color.CYAN,
                ),
                // Body style run [0, 1) [15.0, 25.0] drawn with inner Color.MAGENTA
                MockCanvas.DrawRectCall(
                    left = 55f,
                    top = 15f,
                    right = 65f,
                    bottom = 25f,
                    color = Color.MAGENTA,
                ),
                // Body style run [1, 2) [25.0, 35.0] drawn with base Color.CYAN
                MockCanvas.DrawRectCall(
                    left = 55f,
                    top = 25f,
                    right = 65f,
                    bottom = 35f,
                    color = Color.CYAN,
                ),
            )
            .inOrder()

        // Case C: AnnotationPosition.After places the ruby column on the left side of the body
        // column: [originX + bodyLeft - rubyWidth, originX + bodyLeft] = [50, 55].
        val afterRubySpan =
            RubySpan(
                rubyA,
                position = AnnotationPosition.After,
                orientation = TextOrientation.Upright,
            )
        val afterBodyText =
            SpannableString("漢字").apply {
                setSpan(BackgroundColorSpan(Color.YELLOW), 0, 2, SPAN_FLAG)
                setSpan(afterRubySpan, 0, 2, SPAN_FLAG)
            }
        val runC = RubyLayoutRun(afterBodyText, 0, 2, TextOrientation.Upright, PAINT, afterRubySpan)
        val canvasC = MockCanvas()
        runC.draw(canvasC, originX, originY, PAINT)

        val rubyColumnRectsC = canvasC.drawnRects.filter { it.left == 50f && it.right == 55f }
        assertThat(rubyColumnRectsC)
            .containsExactly(
                MockCanvas.DrawRectCall(
                    left = 50f,
                    top = 10f,
                    right = 55f,
                    bottom = 17.5f,
                    color = Color.YELLOW,
                ),
                MockCanvas.DrawRectCall(
                    left = 50f,
                    top = 22.5f,
                    right = 55f,
                    bottom = 30f,
                    color = Color.YELLOW,
                ),
                MockCanvas.DrawRectCall(
                    left = 50f,
                    top = 17.5f,
                    right = 55f,
                    bottom = 22.5f,
                    color = Color.MAGENTA,
                ),
            )
            .inOrder()
    }

    @Test
    fun rubyLayoutRun_reusedSpanInstanceOnBodyAndRubySubrange_keepsCoveringSpanOnRemainingRuby() {
        val sharedColor = ForegroundColorSpan(Color.RED)
        val sharedShear = FontShearSpan(0.25f)
        val styledRuby =
            SpannableString("あいう").apply {
                // Reusing the same span instances on a sub-range [0, 1) of rubyText must not move
                // the covering spans from [0, 3) to [0, 1) on styledRubyText.
                setSpan(sharedColor, 0, 1, SPAN_FLAG)
                setSpan(sharedShear, 0, 1, SPAN_FLAG)
            }
        val rubySpan = RubySpan(styledRuby, orientation = TextOrientation.Upright)
        val styledBody =
            SpannableString("漢字").apply {
                setSpan(sharedColor, 0, 2, SPAN_FLAG)
                setSpan(sharedShear, 0, 2, SPAN_FLAG)
                setSpan(rubySpan, 0, 2, SPAN_FLAG)
            }

        val run = RubyLayoutRun(styledBody, 0, 2, TextOrientation.Upright, PAINT, rubySpan)
        val mock = MockCanvas()
        run.draw(mock, 60f, 10f, PAINT)

        val remainingCharCalls =
            mock.invocations.filter { it.text.toString() == "あいう" && it.start >= 1 }
        assertThat(remainingCharCalls).isNotEmpty()
        for (call in remainingCharCalls) {
            assertThat(call.paint.color).isEqualTo(Color.RED)
            assertThat(call.skewY).isEqualTo(-0.25f)
        }
    }

    @Test
    fun rubyLayoutRun_noCopySpanOnRubyText_isNotCopiedWhenCoveringSpanApplies() {
        val noCopyColorSpan =
            object : CharacterStyle(), NoCopySpan {
                override fun updateDrawState(tp: TextPaint) {
                    tp.color = Color.MAGENTA
                }
            }
        val styledRuby =
            SpannableString("あいう").apply { setSpan(noCopyColorSpan, 0, length, SPAN_FLAG) }
        val rubySpan = RubySpan(styledRuby, orientation = TextOrientation.Upright)
        val styledBody =
            SpannableString("漢字").apply {
                setSpan(ForegroundColorSpan(Color.RED), 0, 2, SPAN_FLAG)
                setSpan(rubySpan, 0, 2, SPAN_FLAG)
            }

        val run = RubyLayoutRun(styledBody, 0, 2, TextOrientation.Upright, PAINT, rubySpan)
        val mock = MockCanvas()
        run.draw(mock, 60f, 10f, PAINT)

        val rubyCalls = mock.invocations.filter { it.text.toString() == "あいう" }
        assertThat(rubyCalls).isNotEmpty()
        for (call in rubyCalls) {
            assertThat(call.paint.color).isEqualTo(Color.RED)
        }
    }

    @Test
    fun rubyLayoutRun_coveringNonSizeMetricSpan_andInnerAbsoluteSizeSpan_andNonHalfRubyScale() {
        val customScale = 0.3f

        // 1. Covering StyleSpan(BOLD) without a size span preserves the exact scaled textSize.
        val plainRubySpan =
            RubySpan("あいう", textScale = customScale, orientation = TextOrientation.Upright)
        val boldOnlyBody =
            SpannableString("漢字").apply {
                setSpan(StyleSpan(Typeface.BOLD), 0, 2, SPAN_FLAG)
                setSpan(plainRubySpan, 0, 2, SPAN_FLAG)
            }
        val boldOnlyRun =
            RubyLayoutRun(boldOnlyBody, 0, 2, TextOrientation.Upright, PAINT, plainRubySpan)
        val boldOnlyCanvas = MockCanvas()
        boldOnlyRun.draw(boldOnlyCanvas, 0f, 0f, PAINT)

        val boldOnlyRubyCalls = boldOnlyCanvas.invocations.filter { it.text.toString() == "あいう" }
        assertThat(boldOnlyRubyCalls).isNotEmpty()
        for (call in boldOnlyRubyCalls) {
            assertThat(call.paint.textSize).isEqualTo(ONE_EM * customScale)
        }

        // 2. Inner AbsoluteSizeSpan(8) on [0, 1) overrides covering size without being scaled by
        // customScale, while [1, 3) scales RelativeSizeSpan(2f) by customScale (0.3f).
        val styledRuby =
            SpannableString("あいう").apply { setSpan(AbsoluteSizeSpan(8), 0, 1, SPAN_FLAG) }
        val rubySpan =
            RubySpan(styledRuby, textScale = customScale, orientation = TextOrientation.Upright)
        val styledBody =
            SpannableString("漢字").apply {
                setSpan(StyleSpan(Typeface.BOLD), 0, 2, SPAN_FLAG)
                setSpan(RelativeSizeSpan(2f), 0, 2, SPAN_FLAG)
                setSpan(rubySpan, 0, 2, SPAN_FLAG)
            }
        val run = RubyLayoutRun(styledBody, 0, 2, TextOrientation.Upright, PAINT, rubySpan)
        val mock = MockCanvas()
        run.draw(mock, 0f, 0f, PAINT)

        val rubyCalls = mock.invocations.filter { it.text.toString() == "あいう" }
        val firstCharCalls = rubyCalls.filter { it.start == 0 }
        val remainingCharCalls = rubyCalls.filter { it.start >= 1 }
        assertThat(firstCharCalls).hasSize(1)
        assertThat(remainingCharCalls).isNotEmpty()
        assertThat(firstCharCalls[0].paint.textSize).isEqualTo(8f)
        for (call in remainingCharCalls) {
            assertThat(call.paint.textSize).isWithin(1e-3f).of(ONE_EM * 2f * customScale)
        }
    }

    @Test
    fun rubyLayoutRun_equalBodyAndRubyHeight_drawsNoCenteringSlivers() {
        // Body ("あ", height = 1 * 10 = 10) and Ruby ("あい", height = 2 * 5 = 10) have equal height.
        val rubySpan = RubySpan("あい", orientation = TextOrientation.Upright)
        val styledBody =
            SpannableString("あ").apply {
                setSpan(BackgroundColorSpan(Color.YELLOW), 0, 1, SPAN_FLAG)
                setSpan(rubySpan, 0, 1, SPAN_FLAG)
            }

        val run = RubyLayoutRun(styledBody, 0, 1, TextOrientation.Upright, PAINT, rubySpan)
        val mock = MockCanvas()
        run.draw(mock, 60f, 10f, PAINT)

        // Only the 1 body column rect [55, 65] and 1 ruby column rect [65, 70] are drawn.
        assertThat(mock.drawnRects)
            .containsExactly(
                MockCanvas.DrawRectCall(
                    left = 55f,
                    top = 10f,
                    right = 65f,
                    bottom = 20f,
                    color = Color.YELLOW,
                ),
                MockCanvas.DrawRectCall(
                    left = 65f,
                    top = 10f,
                    right = 70f,
                    bottom = 20f,
                    color = Color.YELLOW,
                ),
            )
    }

    @Test
    fun rubyLayoutRun_rubyTextSpanWithPriority_overridesCoveringSpan() {
        val lowPriorityFlags = SPAN_FLAG or (1 shl Spanned.SPAN_PRIORITY_SHIFT)
        // The covering spans also use the maximum priority. RubyLayoutRun attaches them
        // first, so a ruby text span with this priority must still win.
        val maxPriorityFlags = SPAN_FLAG or Spanned.SPAN_PRIORITY
        val styledRuby =
            SpannableString("あいう").apply {
                setSpan(ForegroundColorSpan(Color.BLUE), 0, 1, lowPriorityFlags)
                setSpan(ForegroundColorSpan(Color.GREEN), 1, 2, maxPriorityFlags)
            }
        val rubySpan = RubySpan(styledRuby, orientation = TextOrientation.Upright)
        val styledBody =
            SpannableString("漢字").apply {
                setSpan(ForegroundColorSpan(Color.RED), 0, 2, SPAN_FLAG)
                setSpan(rubySpan, 0, 2, SPAN_FLAG)
            }

        val run = RubyLayoutRun(styledBody, 0, 2, TextOrientation.Upright, PAINT, rubySpan)
        val mock = MockCanvas()
        run.draw(mock, 60f, 10f, PAINT)

        val rubyCalls = mock.invocations.filter { it.text.toString() == "あいう" }
        val firstCharCalls = rubyCalls.filter { it.start == 0 }
        val secondCharCalls = rubyCalls.filter { it.start == 1 }
        val thirdCharCalls = rubyCalls.filter { it.start == 2 }
        assertThat(firstCharCalls).hasSize(1)
        assertThat(secondCharCalls).hasSize(1)
        assertThat(thirdCharCalls).hasSize(1)
        assertThat(firstCharCalls[0].paint.color).isEqualTo(Color.BLUE)
        assertThat(secondCharCalls[0].paint.color).isEqualTo(Color.GREEN)
        assertThat(thirdCharCalls[0].paint.color).isEqualTo(Color.RED)
    }
}

private fun Paint.hasVerticalTextFlag() =
    (flags and Paint.VERTICAL_TEXT_FLAG) == Paint.VERTICAL_TEXT_FLAG
