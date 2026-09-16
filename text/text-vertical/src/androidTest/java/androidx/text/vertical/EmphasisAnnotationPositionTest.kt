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
import android.graphics.Paint
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/** Text size of the base text, chosen so that 1em == 100px. */
private const val ONE_EM = 100f

/** Scale of the emphasis mark, i.e. 0.5em == 50px at the default emphasis scale. */
private const val MARK_SCALE = EmphasisSpan.DEFAULT_SCALE

/** Width the emphasis mark reserves, i.e. 50px. */
private const val MARK_PX = ONE_EM * MARK_SCALE

/** Half of the base text, i.e. the distance from the baseline to either side of the glyph. */
private const val HALF_EM = ONE_EM * 0.5f

/** The default filled [EmphasisStyle.Dot] maps to U+2022 BULLET. */
private const val MARK_LETTER = "\u2022"

/** U+3042 HIRAGANA LETTER A. */
private const val VISIBLE_CHAR = "\u3042"

/** The run origin used by the draw tests. Mark offsets are read relative to it. */
private const val ORIGIN = 0f

/**
 * Verifies that [EmphasisSpan.position] is honored when drawing vertical text.
 *
 * Per the TTML2 `tts:rubyPosition` semantics documented on [AnnotationPosition], in vertical
 * writing mode [AnnotationPosition.Before] places the emphasis mark to the right of the base text,
 * and [AnnotationPosition.After] places it to the left.
 *
 * Each test asserts one of two things.
 * - The reserved side offsets on the [LayoutRun]. These come purely from `paint.textSize` and the
 *   mark scale, never from glyph outlines, so the expected values are exact on every API level.
 * - The x coordinate the mark is drawn at, relative to the run origin. Only the sign is asserted,
 *   because the pre-BAKLAVA backport in [CanvasCompat] centers each cluster and therefore shifts
 *   the coordinate by half of the mark advance. That shift can never flip the sign: the mark offset
 *   is `textSize * (1 + scale) / 2` while the shift is about `textSize * scale / 2`, so `textSize /
 *   2` of margin always remains.
 *
 * Reservation and drawing are asserted separately on purpose. A change that moves the mark but
 * leaves the reservation on the old side makes the mark collide with the adjacent column, and a
 * suite that only checks the drawing cannot see it.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class EmphasisAnnotationPositionTest {

    private val paint = TextPaint().apply { textSize = ONE_EM }

    // ---------------------------------------------------------------------------------------
    // UprightLayoutRun — reservation
    // ---------------------------------------------------------------------------------------

    @Test
    fun upright_positionBefore_reservesTheMarkOnTheRight() {
        val run = uprightRun(AnnotationPosition.Before)

        assertThat(run.leftSideOffset).isEqualTo(-HALF_EM)
        assertThat(run.rightSideOffset).isEqualTo(HALF_EM + MARK_PX)
    }

    @Test
    fun upright_positionAfter_reservesTheMarkOnTheLeft() {
        val run = uprightRun(AnnotationPosition.After)

        assertThat(run.leftSideOffset).isEqualTo(-HALF_EM - MARK_PX)
        assertThat(run.rightSideOffset).isEqualTo(HALF_EM)
    }

    /** The mark takes the same room on either side, so the column width must not change. */
    @Test
    fun upright_widthIsIdenticalForBeforeAndAfter() {
        val before = uprightRun(AnnotationPosition.Before)
        val after = uprightRun(AnnotationPosition.After)

        assertThat(after.rightSideOffset - after.leftSideOffset)
            .isEqualTo(before.rightSideOffset - before.leftSideOffset)
    }

    /** An unrecognized position falls back to the [EmphasisSpan.DEFAULT_POSITION] placement. */
    @Test
    fun upright_positionUnknown_isTreatedAsBefore() {
        val run = uprightRun(AnnotationPosition.Unknown)

        assertThat(run.leftSideOffset).isEqualTo(-HALF_EM)
        assertThat(run.rightSideOffset).isEqualTo(HALF_EM + MARK_PX)
    }

    // ---------------------------------------------------------------------------------------
    // UprightLayoutRun — drawing
    // ---------------------------------------------------------------------------------------

    @Test
    fun upright_positionBefore_markIsDrawnRightOfTheBaseline() {
        val marks = drawAndCollectMarks(uprightRun(AnnotationPosition.Before))

        assertThat(marks).hasSize(1)
        assertThat(marks.single()).isGreaterThan(ORIGIN)
    }

    @Test
    fun upright_positionAfter_markIsDrawnLeftOfTheBaseline() {
        val marks = drawAndCollectMarks(uprightRun(AnnotationPosition.After))

        assertThat(marks).hasSize(1)
        assertThat(marks.single()).isLessThan(ORIGIN)
    }

    @Test
    fun upright_positionUnknown_isDrawnRightOfTheBaseline() {
        val marks = drawAndCollectMarks(uprightRun(AnnotationPosition.Unknown))

        assertThat(marks).hasSize(1)
        assertThat(marks.single()).isGreaterThan(ORIGIN)
    }

    // ---------------------------------------------------------------------------------------
    // TateChuYokoLayoutRun — reservation
    // ---------------------------------------------------------------------------------------

    @Test
    fun tateChuYoko_positionBefore_reservesTheMarkOnTheRight() {
        val run = tateChuYokoRun(AnnotationPosition.Before)

        assertThat(run.leftSideOffset).isEqualTo(-HALF_EM)
        assertThat(run.rightSideOffset).isEqualTo(HALF_EM + MARK_PX)
    }

    @Test
    fun tateChuYoko_positionAfter_reservesTheMarkOnTheLeft() {
        val run = tateChuYokoRun(AnnotationPosition.After)

        assertThat(run.leftSideOffset).isEqualTo(-HALF_EM - MARK_PX)
        assertThat(run.rightSideOffset).isEqualTo(HALF_EM)
    }

    @Test
    fun tateChuYoko_widthIsIdenticalForBeforeAndAfter() {
        val before = tateChuYokoRun(AnnotationPosition.Before)
        val after = tateChuYokoRun(AnnotationPosition.After)

        assertThat(after.rightSideOffset - after.leftSideOffset)
            .isEqualTo(before.rightSideOffset - before.leftSideOffset)
    }

    /** An unrecognized position falls back to the [EmphasisSpan.DEFAULT_POSITION] placement. */
    @Test
    fun tateChuYoko_positionUnknown_isTreatedAsBefore() {
        val run = tateChuYokoRun(AnnotationPosition.Unknown)

        assertThat(run.leftSideOffset).isEqualTo(-HALF_EM)
        assertThat(run.rightSideOffset).isEqualTo(HALF_EM + MARK_PX)
    }

    // ---------------------------------------------------------------------------------------
    // TateChuYokoLayoutRun — drawing
    // ---------------------------------------------------------------------------------------

    @Test
    fun tateChuYoko_positionBefore_markIsDrawnRightOfTheBaseline() {
        val marks = drawAndCollectMarks(tateChuYokoRun(AnnotationPosition.Before))

        assertThat(marks).hasSize(1)
        assertThat(marks.single()).isGreaterThan(ORIGIN)
    }

    @Test
    fun tateChuYoko_positionAfter_markIsDrawnLeftOfTheBaseline() {
        val marks = drawAndCollectMarks(tateChuYokoRun(AnnotationPosition.After))

        assertThat(marks).hasSize(1)
        assertThat(marks.single()).isLessThan(ORIGIN)
    }

    // ---------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------

    private fun emphasizedText(position: AnnotationPosition): Spanned =
        SpannableString(VISIBLE_CHAR).apply {
            setSpan(
                EmphasisSpan(position = position),
                0,
                length,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE,
            )
        }

    private fun uprightRun(position: AnnotationPosition): UprightLayoutRun {
        val text = emphasizedText(position)
        return UprightLayoutRun(text, 0, text.length, paint)
    }

    private fun tateChuYokoRun(position: AnnotationPosition): TateChuYokoLayoutRun {
        val text = emphasizedText(position)
        return TateChuYokoLayoutRun(text, 0, text.length, paint)
    }

    /** Draws [run] and returns the x coordinate of every emphasis mark it paints. */
    private fun drawAndCollectMarks(run: LayoutRun): List<Float> {
        val markXs = mutableListOf<Float>()
        val canvas = RecordingCanvas { drawn, x ->
            if (drawn == MARK_LETTER) markXs.add(x)
        }
        run.draw(canvas, ORIGIN, ORIGIN, paint)
        return markXs
    }

    /** A [Canvas] that reports the text and x coordinate of every `drawText` call. */
    private class RecordingCanvas(val onDrawText: (String, Float) -> Unit) : Canvas() {
        override fun drawText(
            text: CharSequence,
            start: Int,
            end: Int,
            x: Float,
            y: Float,
            paint: Paint,
        ) {
            super.drawText(text, start, end, x, y, paint)
            onDrawText(text.substring(start, end), x)
        }

        override fun drawText(text: String, x: Float, y: Float, paint: Paint) {
            super.drawText(text, x, y, paint)
            onDrawText(text, x)
        }
    }
}
