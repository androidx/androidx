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
import android.os.Build
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.text.vertical.ResolvedOrientation.Rotate
import androidx.text.vertical.ResolvedOrientation.TateChuYoko
import androidx.text.vertical.ResolvedOrientation.Upright
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests branches in `LayoutRun.kt` ([createLayoutRun], [LayoutRun], [TateChuYokoLayoutRun],
 * [RotateLayoutRun], [UprightLayoutRun], [forStyleRuns], [withVerticalFlag], [tempPaint], and
 * [isEmphasisTarget]) derived from Kex symbolic execution paths.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class LayoutRunKexVerifiedTest {

    private val oneEm = 10f
    private val halfEm = oneEm * 0.5f
    private val paint = TextPaint().apply { textSize = oneEm }

    // On API 36+, drawTextVertical draws at the center x; on API < 36, it subtracts advance / 2.
    private fun expectedVerticalMarkX(centerX: Float, markHalfAdvance: Float): Float =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            centerX
        } else {
            centerX - markHalfAdvance
        }

    private class RecordingCanvas : Canvas() {
        data class TextCall(
            val text: String,
            val start: Int,
            val end: Int,
            val x: Float,
            val y: Float,
            val textSize: Float,
            val textScaleX: Float,
            val skewX: Float,
            val skewY: Float,
        )

        private data class SavedSkew(val saveCount: Int, val skewX: Float, val skewY: Float)

        val textCalls = mutableListOf<TextCall>()
        private val skewStack = ArrayDeque<SavedSkew>()
        private var currentSkewX = 0f
        private var currentSkewY = 0f

        override fun save(): Int {
            val count = super.save()
            skewStack.addLast(SavedSkew(count, currentSkewX, currentSkewY))
            return count
        }

        override fun restore() {
            val popped = skewStack.removeLastOrNull()
            if (popped != null) {
                currentSkewX = popped.skewX
                currentSkewY = popped.skewY
            }
            super.restore()
        }

        override fun restoreToCount(saveCount: Int) {
            while (skewStack.isNotEmpty() && skewStack.last().saveCount >= saveCount) {
                val popped = skewStack.removeLast()
                currentSkewX = popped.skewX
                currentSkewY = popped.skewY
            }
            super.restoreToCount(saveCount)
        }

        override fun skew(sx: Float, sy: Float) {
            currentSkewX += sx
            currentSkewY += sy
            super.skew(sx, sy)
        }

        private fun record(
            text: String,
            start: Int,
            end: Int,
            x: Float,
            y: Float,
            paint: Paint,
        ) {
            textCalls.add(
                TextCall(
                    text = text,
                    start = start,
                    end = end,
                    x = x,
                    y = y,
                    textSize = paint.textSize,
                    textScaleX = paint.textScaleX,
                    skewX = currentSkewX,
                    skewY = currentSkewY,
                )
            )
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
            record(text.toString(), start, end, x, y, paint)
        }

        override fun drawText(
            text: String,
            start: Int,
            end: Int,
            x: Float,
            y: Float,
            paint: Paint,
        ) {
            super.drawText(text, start, end, x, y, paint)
            record(text, start, end, x, y, paint)
        }

        override fun drawText(text: String, x: Float, y: Float, paint: Paint) {
            super.drawText(text, x, y, paint)
            record(text, 0, text.length, x, y, paint)
        }
    }

    @Test
    fun createLayoutRun_dispatchesAllResolvedOrientations() {
        val text = "ab"
        assertThat(createLayoutRun(text, 0, 2, paint, Rotate))
            .isInstanceOf(RotateLayoutRun::class.java)
        assertThat(createLayoutRun(text, 0, 2, paint, Upright))
            .isInstanceOf(UprightLayoutRun::class.java)
        assertThat(createLayoutRun(text, 0, 2, paint, TateChuYoko))
            .isInstanceOf(TateChuYokoLayoutRun::class.java)
    }

    @Test
    fun tateChuYoko_widthExceedsByUpToTenPercent_expandsBoundsWithoutShrinkingScaleX() {
        val bigEm = 100f
        val bigPaint =
            TextPaint().apply {
                textSize = bigEm
                typeface = Typeface.MONOSPACE
            }
        val singleCharWidth = bigPaint.measureText("a")
        val secondCharScale = (bigEm * 1.05f - singleCharWidth) / singleCharWidth
        // The run width w stays bigEm only while the second character is not scaled above 1.0f.
        assertThat(secondCharScale).isLessThan(1f)
        val spanned =
            SpannableString("aa").apply {
                setSpan(RelativeSizeSpan(secondCharScale), 1, 2, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            }

        val run = TateChuYokoLayoutRun(spanned, 0, 2, bigPaint)

        // When w < textWidth <= w * 1.1f, the run expands left and right bounds by 1.1f and
        // keeps scaleX at 1f.
        assertThat(run.leftSideOffset).isWithin(1e-3f).of(-bigEm * 0.5f * 1.1f)
        assertThat(run.rightSideOffset).isWithin(1e-3f).of(bigEm * 0.5f * 1.1f)
        assertThat(run.width).isWithin(1e-3f).of(bigEm * 1.1f)

        val canvas = RecordingCanvas()
        run.draw(canvas, 0f, 0f, bigPaint)
        assertThat(canvas.textCalls).hasSize(2)
        for (call in canvas.textCalls) {
            assertThat(call.textScaleX).isWithin(1e-4f).of(1f)
        }
    }

    @Test
    fun tateChuYoko_emphasisBeforeAndAfter_reservesSpaceAndDrawsLastSpanWithShear() {
        val beforeSpan =
            EmphasisSpan(
                EmphasisStyle.Dot,
                position = AnnotationPosition.Before,
                scale = 0.5f,
            )
        val afterSpan =
            EmphasisSpan(
                EmphasisStyle.Circle,
                position = AnnotationPosition.After,
                scale = 0.5f,
            )
        val markPaint = TextPaint(paint).apply { textSize = oneEm * 0.5f }
        val beforeMarkHalfAdvance = markPaint.measureText(beforeSpan.letter) / 2f
        val afterMarkHalfAdvance = markPaint.measureText(afterSpan.letter) / 2f

        val textBefore =
            SpannableString("1").apply {
                setSpan(beforeSpan, 0, 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                setSpan(FontShearSpan(0.2f), 0, 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            }
        val runBefore = TateChuYokoLayoutRun(textBefore, 0, 1, paint)
        assertThat(runBefore.leftSideOffset).isWithin(1e-4f).of(-halfEm)
        assertThat(runBefore.rightSideOffset).isWithin(1e-4f).of(halfEm + oneEm * 0.5f)

        val canvasBefore = RecordingCanvas()
        runBefore.draw(canvasBefore, 50f, 20f, paint)
        val bodyCallBefore = canvasBefore.textCalls.first { it.text == "1" }
        val markCallBefore = canvasBefore.textCalls.first { it.text == beforeSpan.letter }
        assertThat(bodyCallBefore.skewX).isWithin(1e-4f).of(-0.2f)
        assertThat(markCallBefore.x)
            .isWithin(1e-3f)
            .of(expectedVerticalMarkX(50f + oneEm * 0.75f, beforeMarkHalfAdvance))

        // When two EmphasisSpans cover the range, the last span wins.
        val textAfter =
            SpannableString("1").apply {
                setSpan(beforeSpan, 0, 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                setSpan(afterSpan, 0, 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            }
        val runAfter = TateChuYokoLayoutRun(textAfter, 0, 1, paint)
        assertThat(runAfter.leftSideOffset).isWithin(1e-4f).of(-halfEm - oneEm * 0.5f)
        assertThat(runAfter.rightSideOffset).isWithin(1e-4f).of(halfEm)

        val canvasAfter = RecordingCanvas()
        runAfter.draw(canvasAfter, 50f, 20f, paint)
        val markCallAfter = canvasAfter.textCalls.first { it.text == afterSpan.letter }
        assertThat(markCallAfter.x)
            .isWithin(1e-3f)
            .of(expectedVerticalMarkX(50f - oneEm * 0.75f, afterMarkHalfAdvance))
    }

    @Test
    fun tateChuYoko_getCharAdvances_singleAndMultiCharRanges() {
        val singleRun = TateChuYokoLayoutRun("1", 0, 1, paint)
        val singleOut = FloatArray(1) { -1f }
        singleRun.getCharAdvances(singleOut, paint)
        assertThat(singleOut[0]).isEqualTo(singleRun.height)

        val multiRun = TateChuYokoLayoutRun("123", 0, 3, paint)
        val multiOut = FloatArray(3) { 99f }
        multiRun.getCharAdvances(multiOut, paint)
        assertThat(multiOut[0]).isEqualTo(multiRun.height)
        assertThat(multiOut[1]).isEqualTo(0f)
        assertThat(multiOut[2]).isEqualTo(0f)
    }

    @Test
    fun rotateLayoutRun_drawWithFontShear_andGetCharAdvances() {
        val text =
            SpannableString("ab").apply {
                setSpan(FontShearSpan(0.3f), 0, 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            }
        val run = RotateLayoutRun(text, 0, 2, paint)

        val canvas = RecordingCanvas()
        run.draw(canvas, 40f, 10f, paint)
        assertThat(canvas.textCalls).hasSize(2)
        assertThat(canvas.textCalls[0].start).isEqualTo(0)
        assertThat(canvas.textCalls[0].end).isEqualTo(1)
        assertThat(canvas.textCalls[0].skewX).isWithin(1e-4f).of(-0.3f)
        assertThat(canvas.textCalls[1].start).isEqualTo(1)
        assertThat(canvas.textCalls[1].end).isEqualTo(2)
        assertThat(canvas.textCalls[1].skewX).isEqualTo(0f)

        val advances = FloatArray(2)
        run.getCharAdvances(advances, paint)
        assertThat(advances.sum()).isWithin(1e-3f).of(run.height)
    }

    @Test
    fun uprightLayoutRun_emphasisBeforeAndAfter_surrogatePairAndPunctuationSkip_andFontShear() {
        // "𠮟。あ" has a surrogate pair at [0, 2), punctuation at [2, 3), and hiragana at [3, 4).
        val raw = "𠮟。あ"
        val markPaint = TextPaint(paint).apply { textSize = oneEm * 0.5f }

        val emphasisBefore =
            EmphasisSpan(
                EmphasisStyle.Dot,
                position = AnnotationPosition.Before,
                scale = 0.5f,
            )
        val beforeMarkHalfAdvance = markPaint.measureText(emphasisBefore.letter) / 2f
        val textBefore =
            SpannableString(raw).apply {
                setSpan(emphasisBefore, 0, raw.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            }
        val runBefore = UprightLayoutRun(textBefore, 0, raw.length, paint)
        assertThat(runBefore.leftSideOffset).isWithin(1e-4f).of(-halfEm)
        assertThat(runBefore.rightSideOffset).isWithin(1e-4f).of(halfEm + oneEm * 0.5f)

        val canvasBefore = RecordingCanvas()
        runBefore.draw(canvasBefore, 60f, 10f, paint)
        val markCallsBefore = canvasBefore.textCalls.filter { it.text == emphasisBefore.letter }
        assertThat(markCallsBefore).hasSize(2)
        for (call in markCallsBefore) {
            assertThat(call.x)
                .isWithin(1e-3f)
                .of(expectedVerticalMarkX(60f + oneEm * 0.75f, beforeMarkHalfAdvance))
        }

        val emphasisAfter =
            EmphasisSpan(
                EmphasisStyle.Sesame,
                position = AnnotationPosition.After,
                scale = 0.5f,
            )
        val afterMarkHalfAdvance = markPaint.measureText(emphasisAfter.letter) / 2f
        val text =
            SpannableString(raw).apply {
                setSpan(emphasisAfter, 0, raw.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                setSpan(FontShearSpan(0.25f), 0, raw.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            }

        val run = UprightLayoutRun(text, 0, raw.length, paint)
        assertThat(run.leftSideOffset).isWithin(1e-4f).of(-halfEm - oneEm * 0.5f)
        assertThat(run.rightSideOffset).isWithin(1e-4f).of(halfEm)

        val canvas = RecordingCanvas()
        run.draw(canvas, 60f, 10f, paint)

        // The emphasis mark is drawn for "𠮟" and "あ", and skipped for the second surrogate unit
        // and for "。".
        val markCalls = canvas.textCalls.filter { it.text == emphasisAfter.letter }
        assertThat(markCalls).hasSize(2)
        for (call in markCalls) {
            assertThat(call.x)
                .isWithin(1e-3f)
                .of(expectedVerticalMarkX(60f - oneEm * 0.75f, afterMarkHalfAdvance))
        }

        val bodyCalls = canvas.textCalls.filter { it.text == raw }
        assertThat(bodyCalls).isNotEmpty()
        for (call in bodyCalls) {
            assertThat(call.skewY).isWithin(1e-4f).of(-0.25f)
        }

        val advances = FloatArray(raw.length)
        run.getCharAdvances(advances, paint)
        assertThat(advances[1]).isEqualTo(0f)
        assertThat(advances.sum()).isWithin(1e-3f).of(run.height)
    }

    @Test
    fun forStyleRuns_plainStringEmptyRangeAndOverlappingSpans() {
        val plainCalls = mutableListOf<Triple<Int, Int, Int>>()
        val bgPaint = TextPaint(paint).apply { bgColor = Color.GREEN }
        "plain"
            .forStyleRuns(1, 4, bgPaint) { rStart, rEnd, _, bgColor, shear, emphasis ->
                plainCalls.add(Triple(rStart, rEnd, bgColor))
                assertThat(shear).isEqualTo(0f)
                assertThat(emphasis).isNull()
            }
        assertThat(plainCalls).containsExactly(Triple(1, 4, Color.GREEN))

        var emptyCallCount = 0
        SpannableString("abc").forStyleRuns(2, 2, paint) { _, _, _, _, _, _ -> emptyCallCount++ }
        assertThat(emptyCallCount).isEqualTo(0)

        val firstEmphasis = EmphasisSpan(EmphasisStyle.Dot)
        val secondEmphasis = EmphasisSpan(EmphasisStyle.Triangle)
        val spanned =
            SpannableString("ab").apply {
                setSpan(FontShearSpan(0.1f), 0, 2, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                setSpan(FontShearSpan(0.4f), 0, 2, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                setSpan(firstEmphasis, 0, 2, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                setSpan(secondEmphasis, 0, 2, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                setSpan(ForegroundColorSpan(Color.RED), 0, 2, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                setSpan(BackgroundColorSpan(Color.YELLOW), 0, 2, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            }

        var observedShear = 0f
        var observedEmphasis: EmphasisSpan? = null
        var observedBg = 0
        var observedColor = 0
        spanned.forStyleRuns(0, 2, paint) { _, _, rPaint, bgColor, shear, emphasis ->
            observedShear = shear
            observedEmphasis = emphasis
            observedBg = bgColor
            observedColor = rPaint.color
        }
        assertThat(observedShear).isEqualTo(0.4f)
        assertThat(observedEmphasis).isSameInstanceAs(secondEmphasis)
        assertThat(observedBg).isEqualTo(Color.YELLOW)
        assertThat(observedColor).isEqualTo(Color.RED)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
    fun withVerticalFlag_setsFlagDuringBlockAndRestoresOriginalFlags() {
        val workPaint = Paint()
        workPaint.flags = workPaint.flags and Paint.VERTICAL_TEXT_FLAG.inv()
        val originalFlags = workPaint.flags

        val flagInside = workPaint.withVerticalFlag {
            (flags and Paint.VERTICAL_TEXT_FLAG) == Paint.VERTICAL_TEXT_FLAG
        }
        assertThat(flagInside).isTrue()
        assertThat(workPaint.flags).isEqualTo(originalFlags)

        workPaint.flags = originalFlags or Paint.VERTICAL_TEXT_FLAG
        workPaint.withVerticalFlag {
            assertThat((flags and Paint.VERTICAL_TEXT_FLAG) == Paint.VERTICAL_TEXT_FLAG).isTrue()
        }
        assertThat(workPaint.flags).isEqualTo(originalFlags or Paint.VERTICAL_TEXT_FLAG)

        workPaint.flags = originalFlags
        assertThrows(IllegalStateException::class.java) {
            workPaint.withVerticalFlag { throw IllegalStateException("boom") }
        }
        assertThat(workPaint.flags).isEqualTo(originalFlags)
    }

    @Test
    fun tempPaint_reusesPooledPaintAndCapsPoolSize() {
        var firstPaint: TextPaint? = null
        tempPaint { firstPaint = it }
        tempPaint { secondPaint -> assertThat(secondPaint).isSameInstanceAs(firstPaint) }

        assertThrows(IllegalStateException::class.java) {
            tempPaint { paintInBlock ->
                assertThat(paintInBlock).isSameInstanceAs(firstPaint)
                throw IllegalStateException("boom")
            }
        }
        tempPaint { afterThrow -> assertThat(afterThrow).isSameInstanceAs(firstPaint) }

        val nestedPaints = mutableListOf<TextPaint>()
        tempPaint { p1 ->
            nestedPaints.add(p1)
            tempPaint { p2 ->
                nestedPaints.add(p2)
                tempPaint { p3 ->
                    nestedPaints.add(p3)
                    tempPaint { p4 -> nestedPaints.add(p4) }
                }
            }
        }
        assertThat(nestedPaints).hasSize(4)
        assertThat(nestedPaints.toSet()).hasSize(4)

        val reusedPaints = mutableListOf<TextPaint>()
        tempPaint { r1 ->
            reusedPaints.add(r1)
            tempPaint { r2 ->
                reusedPaints.add(r2)
                tempPaint { r3 ->
                    reusedPaints.add(r3)
                    tempPaint { r4 -> reusedPaints.add(r4) }
                }
            }
        }
        assertThat(reusedPaints.subList(0, 3))
            .containsExactly(nestedPaints[1], nestedPaints[2], nestedPaints[3])
            .inOrder()
        assertThat(reusedPaints[3]).isNotIn(nestedPaints)
    }

    @Test
    fun isEmphasisTarget_returnsFalseForAllExcludedCategoriesAndTrueForTargets() {
        // Excluded Unicode categories:
        assertThat(isEmphasisTarget(0x0000)).isFalse() // CONTROL
        assertThat(isEmphasisTarget(0x200B)).isFalse() // FORMAT
        assertThat(isEmphasisTarget(0xFDD0)).isFalse() // UNASSIGNED (noncharacter)
        assertThat(isEmphasisTarget(0x2028)).isFalse() // LINE_SEPARATOR
        assertThat(isEmphasisTarget(0x2029)).isFalse() // PARAGRAPH_SEPARATOR
        assertThat(isEmphasisTarget(' '.code)).isFalse() // SPACE_SEPARATOR
        assertThat(isEmphasisTarget('_'.code)).isFalse() // CONNECTOR_PUNCTUATION
        assertThat(isEmphasisTarget('-'.code)).isFalse() // DASH_PUNCTUATION
        assertThat(isEmphasisTarget(0x2019)).isFalse() // FINAL_PUNCTUATION
        assertThat(isEmphasisTarget(0x2018)).isFalse() // INITIAL_PUNCTUATION
        assertThat(isEmphasisTarget('。'.code)).isFalse() // OTHER_PUNCTUATION

        // Target characters:
        assertThat(isEmphasisTarget('あ'.code)).isTrue()
        assertThat(isEmphasisTarget('漢'.code)).isTrue()
        assertThat(isEmphasisTarget('A'.code)).isTrue()
        assertThat(isEmphasisTarget('1'.code)).isTrue()
    }
}
