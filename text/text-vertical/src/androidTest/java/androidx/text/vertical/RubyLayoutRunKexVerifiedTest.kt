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

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.MetricAffectingSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests branches in `RubyLayoutRun.kt` ([RubyLayoutRun], [forEachRubySpanTransition],
 * `buildStyledRubyText`, and `ScaledCoveringCharacterStyle`) derived from Kex symbolic execution
 * paths.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class RubyLayoutRunKexVerifiedTest {

    private val oneEm = 10f
    private val paint = TextPaint().apply { textSize = oneEm }

    private class RecordingCanvas : Canvas() {
        var drawnRectCount: Int = 0
            private set

        val drawnCharSequences = mutableListOf<CharSequence>()
        val drawnSubstrings = mutableListOf<String>()

        override fun drawRect(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            paint: Paint,
        ) {
            super.drawRect(left, top, right, bottom, paint)
            drawnRectCount++
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
            drawnCharSequences.add(text)
            drawnSubstrings.add(text.subSequence(start, end).toString())
        }
    }

    @Test
    fun forEachRubySpanTransition_plainAndSingleRubySpan_yieldsExpectedTransitions() {
        val plainTransitions = mutableListOf<Triple<Int, Int, RubySpan?>>()
        forEachRubySpanTransition("漢字", 0, 2) { rStart, rEnd, span ->
            plainTransitions.add(Triple(rStart, rEnd, span))
        }
        assertThat(plainTransitions).containsExactly(Triple(0, 2, null))

        val rubySpan = RubySpan("かな")
        val spanned =
            SpannableString("前漢字後").apply {
                setSpan(rubySpan, 1, 3, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            }
        val spannedTransitions = mutableListOf<Triple<Int, Int, RubySpan?>>()
        forEachRubySpanTransition(spanned, 0, 4) { rStart, rEnd, span ->
            spannedTransitions.add(Triple(rStart, rEnd, span))
        }
        assertThat(spannedTransitions)
            .containsExactly(
                Triple(0, 1, null),
                Triple(1, 3, rubySpan),
                Triple(3, 4, null),
            )
            .inOrder()
    }

    @Test
    fun forEachRubySpanTransition_overlappingRubySpans_throwsIllegalArgumentException() {
        val spanned =
            SpannableString("漢字").apply {
                setSpan(RubySpan("かん"), 0, 2, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                setSpan(RubySpan("じ"), 1, 2, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            }

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                forEachRubySpanTransition(spanned, 0, 2) { _, _, _ -> }
            }
        assertThat(exception).hasMessageThat().contains("RubySpan cannot be overlapped")
    }

    @Test
    fun rubyLayoutRun_initWithNestedRubySpan_throwsIllegalArgumentException() {
        val nestedRubyText =
            SpannableString("かな").apply {
                setSpan(RubySpan("inner"), 0, 2, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            }
        val outerRuby = RubySpan(nestedRubyText)

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                RubyLayoutRun("漢字", 0, 2, TextOrientation.Mixed, paint, outerRuby)
            }
        assertThat(exception).hasMessageThat().contains("Ruby cannot be nested")
    }

    @Test
    fun rubyLayoutRun_emptyRubyTextOrEmptyCoveringSpans_preservesExpectedText() {
        val emptyRuby = RubySpan("")
        val body =
            SpannableString("漢字").apply {
                setSpan(ForegroundColorSpan(Color.RED), 0, 2, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                setSpan(emptyRuby, 0, 2, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            }

        val run = RubyLayoutRun(body, 0, 2, TextOrientation.Upright, paint, emptyRuby)
        assertThat(run.height).isEqualTo(paint.measureTextVertical("漢字"))
        assertThat(run.leftSideOffset).isWithin(1e-4f).of(-oneEm * 0.5f)
        assertThat(run.rightSideOffset).isWithin(1e-4f).of(oneEm * 0.5f)
        assertThat(run.width).isWithin(1e-4f).of(oneEm)

        val canvas = RecordingCanvas()
        run.draw(canvas, 40f, 10f, paint)
        assertThat(canvas.drawnRectCount).isEqualTo(0)
        assertThat(canvas.drawnSubstrings).isNotEmpty()
        assertThat(canvas.drawnSubstrings.joinToString("")).isEqualTo("漢字")
        assertThat(canvas.drawnCharSequences.toSet()).containsExactly(body)

        // Without covering spans, buildStyledRubyText returns the original rubyText instance.
        val plainRubyText = "かな"
        val plainRubyRun =
            RubyLayoutRun("漢字", 0, 2, TextOrientation.Upright, paint, RubySpan(plainRubyText))
        val plainCanvas = RecordingCanvas()
        plainRubyRun.draw(plainCanvas, 40f, 10f, paint)
        assertThat(plainCanvas.drawnCharSequences.any { it === plainRubyText }).isTrue()
    }

    @Test
    fun rubyLayoutRun_getCharAdvances_singleAndMultiCharRanges() {
        val rubySpan = RubySpan("かな")
        val singleRun = RubyLayoutRun("漢", 0, 1, TextOrientation.Upright, paint, rubySpan)
        val singleOut = FloatArray(1) { -1f }
        singleRun.getCharAdvances(singleOut, paint)
        assertThat(singleOut[0]).isEqualTo(singleRun.height)

        val multiRun = RubyLayoutRun("漢字語", 0, 3, TextOrientation.Upright, paint, rubySpan)
        val multiOut = FloatArray(3) { 99f }
        multiRun.getCharAdvances(multiOut, paint)
        assertThat(multiOut[0]).isEqualTo(multiRun.height)
        assertThat(multiOut[1]).isEqualTo(0f)
        assertThat(multiOut[2]).isEqualTo(0f)
    }

    @Test
    fun scaledCoveringCharacterStyle_updateMeasureStateDrawStateAndUnderlying_handlesZeroAndPositiveScale() {
        val sizeSpan = AbsoluteSizeSpan(40)
        val relSizeSpan = RelativeSizeSpan(2f)
        val styleSpan = StyleSpan(Typeface.BOLD)
        val rubyHalf = RubySpan("かな", textScale = 0.5f)
        val bodyHalf =
            SpannableString("漢字").apply {
                setSpan(sizeSpan, 0, 2, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                setSpan(styleSpan, 0, 2, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                setSpan(rubyHalf, 0, 2, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            }
        val runHalf = RubyLayoutRun(bodyHalf, 0, 2, TextOrientation.Upright, paint, rubyHalf)
        val canvasHalf = RecordingCanvas()
        runHalf.draw(canvasHalf, 40f, 10f, paint)
        val styledHalf = canvasHalf.drawnCharSequences.first { it !== bodyHalf } as Spanned
        val metricSpansHalf =
            styledHalf.getSpans(0, styledHalf.length, MetricAffectingSpan::class.java)
        assertThat(metricSpansHalf).hasLength(2)

        val underlyingSet = metricSpansHalf.map { it.underlying }.toSet()
        assertThat(underlyingSet).containsExactly(sizeSpan, styleSpan)

        // AbsoluteSizeSpan(40) scales by 0.5f to 20f.
        // StyleSpan does not change textSize, so scaledSize stays 5f.
        val measurePaint = TextPaint(paint).apply { textSize = 5f }
        val wrappedSizeHalf = metricSpansHalf.first { it.underlying === sizeSpan }
        val wrappedStyleHalf = metricSpansHalf.first { it.underlying === styleSpan }

        wrappedStyleHalf.updateMeasureState(measurePaint)
        assertThat(measurePaint.textSize).isEqualTo(5f)
        wrappedStyleHalf.updateDrawState(measurePaint)
        assertThat(measurePaint.textSize).isEqualTo(5f)

        wrappedSizeHalf.updateMeasureState(measurePaint)
        assertThat(measurePaint.textSize).isEqualTo(20f)
        measurePaint.textSize = 5f
        wrappedSizeHalf.updateDrawState(measurePaint)
        assertThat(measurePaint.textSize).isEqualTo(20f)

        // When rubyScale is 0f, withUnscaledTextSize takes the else branch.
        // Without that guard, RelativeSizeSpan(2f) divides by zero and fails to set textSize to 0f.
        val rubyZero = RubySpan("かな", textScale = 0f)
        val bodyZero =
            SpannableString("漢字").apply {
                setSpan(sizeSpan, 0, 2, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                setSpan(relSizeSpan, 0, 2, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                setSpan(styleSpan, 0, 2, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                setSpan(rubyZero, 0, 2, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            }
        val runZero = RubyLayoutRun(bodyZero, 0, 2, TextOrientation.Upright, paint, rubyZero)
        val canvasZero = RecordingCanvas()
        runZero.draw(canvasZero, 40f, 10f, paint)
        val styledZero = canvasZero.drawnCharSequences.first { it !== bodyZero } as Spanned
        val metricSpansZero =
            styledZero.getSpans(0, styledZero.length, MetricAffectingSpan::class.java)
        val wrappedSizeZero = metricSpansZero.first { it.underlying === sizeSpan }
        val wrappedRelSizeZero = metricSpansZero.first { it.underlying === relSizeSpan }
        val wrappedStyleZero = metricSpansZero.first { it.underlying === styleSpan }

        val zeroPaint = TextPaint(paint).apply { textSize = 12f }
        wrappedStyleZero.updateMeasureState(zeroPaint)
        assertThat(zeroPaint.textSize).isEqualTo(12f)
        wrappedStyleZero.updateDrawState(zeroPaint)
        assertThat(zeroPaint.textSize).isEqualTo(12f)

        wrappedSizeZero.updateMeasureState(zeroPaint)
        assertThat(zeroPaint.textSize).isEqualTo(0f)
        zeroPaint.textSize = 12f
        wrappedSizeZero.updateDrawState(zeroPaint)
        assertThat(zeroPaint.textSize).isEqualTo(0f)

        zeroPaint.textSize = 12f
        wrappedRelSizeZero.updateMeasureState(zeroPaint)
        assertThat(zeroPaint.textSize).isEqualTo(0f)
        zeroPaint.textSize = 12f
        wrappedRelSizeZero.updateDrawState(zeroPaint)
        assertThat(zeroPaint.textSize).isEqualTo(0f)
    }
}
