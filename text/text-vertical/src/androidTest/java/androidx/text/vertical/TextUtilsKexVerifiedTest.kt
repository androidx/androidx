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
import android.text.style.UnderlineSpan
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.text.CharacterIterator
import java.util.Locale
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests branches in `TextUtils.kt` derived from Kex symbolic execution paths.
 *
 * Covers [forEachSpan], [forEachParagraph], [withSave], [withTextScale], [withTextScaleX],
 * [CharSequenceCharacterIterator], and [forEachGrapheme].
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class TextUtilsKexVerifiedTest {

    // =========================================================================================
    // Target 1: forEachSpan
    // =========================================================================================

    // Branch: text !is Spanned invokes consumer once with an empty span array and returns.
    @Test
    fun kex_forEachSpan_whenTextIsNotSpanned_invokesConsumerOnceWithEmptyArray() {
        val segments = mutableListOf<Triple<Int, Int, Int>>()

        forEachSpan<UnderlineSpan>("plain", start = 1, end = 4) { segStart, segEnd, spans ->
            segments.add(Triple(segStart, segEnd, spans.size))
        }

        assertThat(segments).containsExactly(Triple(1, 4, 0))
    }

    // Branch: text is Spanned and start >= end skips the while (spanI < end) loop without invoking
    // consumer.
    @Test
    fun kex_forEachSpan_whenTextIsSpannedAndStartGreaterOrEqualEnd_doesNotInvokeConsumer() {
        val spanned =
            SpannableString("styled").apply {
                setSpan(UnderlineSpan(), 0, 6, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        var callCount = 0

        forEachSpan<UnderlineSpan>(spanned, start = 3, end = 3) { _, _, _ -> callCount++ }
        forEachSpan<UnderlineSpan>(spanned, start = 4, end = 2) { _, _, _ -> callCount++ }

        assertThat(callCount).isEqualTo(0)
    }

    // Branch: text is Spanned and start < end iterates across span transitions and collects active
    // spans per segment.
    @Test
    fun kex_forEachSpan_whenTextIsSpannedWithTransitions_emitsSegmentsInOrder() {
        val span = UnderlineSpan()
        val spanned =
            SpannableString("abcdef").apply {
                setSpan(span, 2, 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        val segments = mutableListOf<Triple<Int, Int, List<UnderlineSpan>>>()

        forEachSpan<UnderlineSpan>(spanned, start = 0, end = 6) { segStart, segEnd, spans ->
            segments.add(Triple(segStart, segEnd, spans.toList()))
        }

        assertThat(segments)
            .containsExactly(
                Triple(0, 2, emptyList<UnderlineSpan>()),
                Triple(2, 4, listOf(span)),
                Triple(4, 6, emptyList<UnderlineSpan>()),
            )
            .inOrder()
    }

    // =========================================================================================
    // Target 2: forEachParagraph
    // =========================================================================================

    private fun collectParagraphs(text: CharSequence, start: Int, end: Int): List<Pair<Int, Int>> {
        val paragraphs = mutableListOf<Pair<Int, Int>>()
        text.forEachParagraph(start, end) { pStart, pEnd -> paragraphs.add(pStart to pEnd) }
        return paragraphs
    }

    // Branch: start >= end skips the while (paraStart < end) loop and emits no paragraphs.
    @Test
    fun kex_forEachParagraph_whenStartGreaterOrEqualEnd_emitsNoParagraphs() {
        assertThat(collectParagraphs("a\nb", start = 1, end = 1)).isEmpty()
        assertThat(collectParagraphs("a\nb", start = 2, end = 0)).isEmpty()
    }

    // Branch: indexOf('\n', paraStart) == -1 clamps paraEnd to end.
    @Test
    fun kex_forEachParagraph_whenNoNewlineFound_emitsSingleRangeToEnd() {
        assertThat(collectParagraphs("abcdef", start = 1, end = 5)).containsExactly(1 to 5)
    }

    // Branch: indexOf('\n', paraStart) >= end clamps paraEnd to end when newline is at or after
    // end.
    @Test
    fun kex_forEachParagraph_whenNewlineAtOrAfterEnd_clampsParagraphEndToRangeEnd() {
        // Newline is at index 3; when end == 3, paraEnd >= end is true.
        assertThat(collectParagraphs("abc\ndef", start = 0, end = 3)).containsExactly(0 to 3)
        // Newline is at index 4; when end == 3, paraEnd >= end is true.
        assertThat(collectParagraphs("abcd\nef", start = 1, end = 3)).containsExactly(1 to 3)
    }

    // Branch: indexOf('\n', paraStart) < end increments paraEnd to include '\n' and continues
    // iterating across consecutive and trailing paragraphs.
    @Test
    fun kex_forEachParagraph_whenMultipleNewlinesInsideRange_includesSeparatorInEachParagraph() {
        val text = "ab\n\ncd\nef"

        assertThat(collectParagraphs(text, start = 0, end = text.length))
            .containsExactly(0 to 3, 3 to 4, 4 to 7, 7 to 9)
            .inOrder()
    }

    // =========================================================================================
    // Target 3: withSave, withTextScale, and withTextScaleX
    // =========================================================================================

    // Branch: Canvas.withSave saves canvas state and restores to checkpoint on both normal return
    // and exception throw.
    @Test
    fun kex_withSave_restoresCanvasSaveCountOnNormalReturnAndException() {
        val canvas = Canvas()
        canvas.save()
        val initialSaveCount = canvas.saveCount

        canvas.withSave {
            translate(10f, 20f)
            assertThat(saveCount).isEqualTo(initialSaveCount + 1)
        }
        assertThat(canvas.saveCount).isEqualTo(initialSaveCount)

        assertThrows(IllegalStateException::class.java) {
            canvas.withSave {
                save()
                throw IllegalStateException("expected failure")
            }
        }
        assertThat(canvas.saveCount).isEqualTo(initialSaveCount)
    }

    // Branch: Paint.withTextScale scales textSize for block execution and restores originalSize in
    // finally on both normal return and exception throw.
    @Test
    fun kex_withTextScale_scalesTextSizeAndRestoresOriginalSizeInFinally() {
        val paint = Paint().apply { textSize = 20f }

        val scaledDuringBlock = paint.withTextScale(0.5f) { textSize }

        assertThat(scaledDuringBlock).isEqualTo(10f)
        assertThat(paint.textSize).isEqualTo(20f)

        assertThrows(IllegalStateException::class.java) {
            paint.withTextScale(2.0f) { throw IllegalStateException("expected failure") }
        }
        assertThat(paint.textSize).isEqualTo(20f)
    }

    // Branch: Paint.withTextScaleX sets textScaleX for block execution and restores originalScaleX
    // in finally on both normal return and exception throw.
    @Test
    fun kex_withTextScaleX_setsTextScaleXAndRestoresOriginalScaleXInFinally() {
        val paint = Paint().apply { textScaleX = 1.25f }

        val scaleXDuringBlock = paint.withTextScaleX(0.75f) { textScaleX }

        assertThat(scaleXDuringBlock).isEqualTo(0.75f)
        assertThat(paint.textScaleX).isEqualTo(1.25f)

        assertThrows(IllegalStateException::class.java) {
            paint.withTextScaleX(0.5f) { throw IllegalStateException("expected failure") }
        }
        assertThat(paint.textScaleX).isEqualTo(1.25f)
    }

    // =========================================================================================
    // Target 4: CharSequenceCharacterIterator
    // =========================================================================================

    // Branch: Empty range (start == end) returns CharacterIterator.DONE for current(), first(),
    // last(), next(), previous(), and setIndex(start).
    @Test
    fun kex_charSequenceCharacterIterator_whenEmptyRange_returnsDoneAcrossAllNavigationMethods() {
        val iterator = CharSequenceCharacterIterator("abcd", start = 2, end = 2)

        assertThat(iterator.beginIndex).isEqualTo(2)
        assertThat(iterator.endIndex).isEqualTo(2)
        assertThat(iterator.index).isEqualTo(2)
        assertThat(iterator.current()).isEqualTo(CharacterIterator.DONE)
        assertThat(iterator.first()).isEqualTo(CharacterIterator.DONE)
        assertThat(iterator.last()).isEqualTo(CharacterIterator.DONE)
        assertThat(iterator.index).isEqualTo(2)
        assertThat(iterator.next()).isEqualTo(CharacterIterator.DONE)
        assertThat(iterator.index).isEqualTo(2)
        assertThat(iterator.previous()).isEqualTo(CharacterIterator.DONE)
        assertThat(iterator.index).isEqualTo(2)
        assertThat(iterator.setIndex(2)).isEqualTo(CharacterIterator.DONE)
    }

    // Branch: Non-empty range (start < end) navigates forward and backward via first(), next(),
    // last(), previous(), and clone().
    @Test
    fun kex_charSequenceCharacterIterator_whenNonEmptyRange_navigatesAndClonesState() {
        val iterator = CharSequenceCharacterIterator("xyzab", start = 1, end = 4)

        assertThat(iterator.beginIndex).isEqualTo(1)
        assertThat(iterator.endIndex).isEqualTo(4)
        assertThat(iterator.first()).isEqualTo('y')
        assertThat(iterator.index).isEqualTo(1)

        // Advance within range: index < end branch in next().
        assertThat(iterator.next()).isEqualTo('z')
        assertThat(iterator.index).isEqualTo(2)

        // Clone preserves current index independently.
        val cloned = iterator.clone() as CharSequenceCharacterIterator
        assertThat(cloned.index).isEqualTo(2)
        assertThat(cloned.current()).isEqualTo('z')

        assertThat(iterator.next()).isEqualTo('a')
        assertThat(iterator.index).isEqualTo(3)

        // Advance to end: index >= end branch in next() clamps index to end and returns DONE.
        assertThat(iterator.next()).isEqualTo(CharacterIterator.DONE)
        assertThat(iterator.index).isEqualTo(4)
        assertThat(iterator.next()).isEqualTo(CharacterIterator.DONE)
        assertThat(iterator.index).isEqualTo(4)

        // Step backward from end: index > start branch in previous().
        assertThat(iterator.previous()).isEqualTo('a')
        assertThat(iterator.index).isEqualTo(3)

        // Jump to last(): start != end branch sets index = end - 1.
        assertThat(iterator.last()).isEqualTo('a')
        assertThat(iterator.index).isEqualTo(3)

        // Step back to start and beyond: index <= start branch in previous() returns DONE.
        assertThat(iterator.previous()).isEqualTo('z')
        assertThat(iterator.previous()).isEqualTo('y')
        assertThat(iterator.index).isEqualTo(1)
        assertThat(iterator.previous()).isEqualTo(CharacterIterator.DONE)
        assertThat(iterator.index).isEqualTo(1)

        // Cloned iterator remained at index 2.
        assertThat(cloned.index).isEqualTo(2)
        assertThat(cloned.current()).isEqualTo('z')
    }

    // Branch: setIndex(pos) returns current() when pos in start..end, and throws
    // IllegalArgumentException when pos < start or pos > end.
    @Test
    fun kex_charSequenceCharacterIterator_setIndex_validatesRangeBounds() {
        val iterator = CharSequenceCharacterIterator("abcdef", start = 1, end = 4)

        assertThat(iterator.setIndex(1)).isEqualTo('b')
        assertThat(iterator.index).isEqualTo(1)
        assertThat(iterator.setIndex(3)).isEqualTo('d')
        assertThat(iterator.index).isEqualTo(3)
        assertThat(iterator.setIndex(4)).isEqualTo(CharacterIterator.DONE)
        assertThat(iterator.index).isEqualTo(4)

        assertThrows(IllegalArgumentException::class.java) { iterator.setIndex(0) }
        assertThrows(IllegalArgumentException::class.java) { iterator.setIndex(5) }
    }

    // =========================================================================================
    // Target 5: forEachGrapheme, acquireGraphemeIterator, and releaseGraphemeIterator
    // =========================================================================================

    // Branch: Empty range (start == end) in forEachGrapheme skips the while (grNext != DONE) loop
    // and pools the BreakIterator for subsequent reuse.
    @Test
    fun kex_forEachGrapheme_whenEmptyRangeAndRepeatedCalls_reusesPooledBreakIterator() {
        val emptyGraphemes = mutableListOf<Pair<Int, Int>>()
        "あいう"
            .forEachGrapheme(start = 1, end = 1, Locale.JAPANESE) { gStart, gEnd ->
                emptyGraphemes.add(gStart to gEnd)
            }
        assertThat(emptyGraphemes).isEmpty()

        // Second call with the same Locale hits the pooled != null branch in
        // acquireGraphemeIterator.
        val graphemes = mutableListOf<Pair<Int, Int>>()
        "a\u0301b"
            .forEachGrapheme(start = 0, end = 3, Locale.JAPANESE) { gStart, gEnd ->
                graphemes.add(gStart to gEnd)
            }
        assertThat(graphemes).containsExactly(0 to 2, 2 to 3).inOrder()
    }

    // Branch: Reentrant forEachGrapheme with the same Locale exercises map[locale] == null on inner
    // acquire/release and map[locale] != null on outer release.
    @Test
    fun kex_forEachGrapheme_whenNestedWithSameLocale_handlesOccupiedPoolSlot() {
        val outerRanges = mutableListOf<Pair<Int, Int>>()
        val innerRanges = mutableListOf<Pair<Int, Int>>()

        "ab"
            .forEachGrapheme(start = 0, end = 2, Locale.US) { gStart, gEnd ->
                outerRanges.add(gStart to gEnd)
                if (gStart == 0) {
                    "xy"
                        .forEachGrapheme(start = 0, end = 2, Locale.US) { inStart, inEnd ->
                            innerRanges.add(inStart to inEnd)
                        }
                }
            }

        assertThat(outerRanges).containsExactly(0 to 1, 1 to 2).inOrder()
        assertThat(innerRanges).containsExactly(0 to 1, 1 to 2).inOrder()
    }
}
