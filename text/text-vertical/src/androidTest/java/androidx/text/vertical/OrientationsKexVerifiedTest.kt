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

import android.os.Build
import android.text.SpannableString
import android.text.Spanned
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests branches in `Orientations.kt` derived from Kex symbolic execution paths.
 *
 * Covers [TextOrientation.fromInt], [TextOrientationSpan], `RunMerger`, [forEachOrientation],
 * `forOrientationNoSpans`, and `resolveOrientation`.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class OrientationsKexVerifiedTest {

    private data class OrientationRun(
        val start: Int,
        val end: Int,
        val orientation: ResolvedOrientation,
    )

    private fun collectRuns(
        text: CharSequence,
        start: Int = 0,
        end: Int = text.length,
        textOrientation: TextOrientation = TextOrientation.Mixed,
    ): List<OrientationRun> {
        val runs = mutableListOf<OrientationRun>()
        forEachOrientation(text, start, end, textOrientation) { runStart, runEnd, orientation ->
            runs.add(OrientationRun(runStart, runEnd, orientation))
        }
        return runs
    }

    // =========================================================================================
    // Target 1: TextOrientation.Companion.fromInt
    // =========================================================================================

    // Branch: value == 0. Returns TextOrientation.Mixed.
    @Test
    fun kex_textOrientation_fromInt_whenZero_returnsMixed() {
        assertThat(TextOrientation.fromInt(0)).isEqualTo(TextOrientation.Mixed)
    }

    // Branch: value == 1. Returns TextOrientation.Upright.
    @Test
    fun kex_textOrientation_fromInt_whenOne_returnsUpright() {
        assertThat(TextOrientation.fromInt(1)).isEqualTo(TextOrientation.Upright)
    }

    // Branch: value == 2. Returns TextOrientation.Sideways.
    @Test
    fun kex_textOrientation_fromInt_whenTwo_returnsSideways() {
        assertThat(TextOrientation.fromInt(2)).isEqualTo(TextOrientation.Sideways)
    }

    // Branch: value is out of bounds for TextOrientation.entries. Falls back to
    // TextOrientation.Mixed.
    @Test
    fun kex_textOrientation_fromInt_whenOutOfBounds_returnsMixed() {
        assertThat(TextOrientation.fromInt(-1)).isEqualTo(TextOrientation.Mixed)
        assertThat(TextOrientation.fromInt(3)).isEqualTo(TextOrientation.Mixed)
        assertThat(TextOrientation.fromInt(Int.MIN_VALUE)).isEqualTo(TextOrientation.Mixed)
        assertThat(TextOrientation.fromInt(Int.MAX_VALUE)).isEqualTo(TextOrientation.Mixed)
    }

    // =========================================================================================
    // Target 2: forEachOrientation & RunMerger
    // =========================================================================================

    // Branch: start >= end on both plain String and Spanned text. Returns immediately without
    // invoking consumer.
    @Test
    fun kex_forEachOrientation_whenStartGreaterOrEqualEnd_doesNotInvokeConsumer() {
        val spanned =
            SpannableString("abc").apply {
                setSpan(TextOrientationSpan.Upright(), 0, 3, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            }

        assertThat(collectRuns("abc", start = 2, end = 2)).isEmpty()
        assertThat(collectRuns("abc", start = 3, end = 1)).isEmpty()
        assertThat(collectRuns(spanned, start = 1, end = 1)).isEmpty()
        assertThat(collectRuns(spanned, start = 2, end = 0)).isEmpty()
    }

    // Branch: text is Spanned with no TextOrientationSpan attached (spans.isEmpty() in
    // forEachSpan). Delegates to forOrientationNoSpans and merges via RunMerger.
    @Test
    fun kex_forEachOrientation_whenSpannedWithoutOrientationSpans_resolvesAndMergesRuns() {
        val spanned = SpannableString("あいうabc")

        val runs = collectRuns(spanned, start = 0, end = spanned.length)

        assertThat(runs)
            .containsExactly(
                OrientationRun(0, 3, ResolvedOrientation.Upright),
                OrientationRun(3, 6, ResolvedOrientation.Rotate),
            )
            .inOrder()
    }

    // Branch: RunMerger merges an unspanned upright run with an adjacent
    // TextOrientationSpan.Upright run when prevOrientation == orientation.
    @Test
    fun kex_forEachOrientation_whenUnspannedUprightMeetsUprightSpan_mergesIntoSingleRun() {
        val spanned =
            SpannableString("あいうabc").apply {
                setSpan(TextOrientationSpan.Upright(), 3, 6, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            }

        val runs = collectRuns(spanned)

        assertThat(runs).containsExactly(OrientationRun(0, 6, ResolvedOrientation.Upright))
    }

    // Branch: RunMerger keeps adjacent TextOrientationSpan.CombineUpright runs separate even when
    // prevOrientation == ResolvedOrientation.TateChuYoko.
    @Test
    fun kex_forEachOrientation_whenAdjacentCombineUprightSpans_doesNotMergeRuns() {
        val spanned =
            SpannableString("1234").apply {
                setSpan(
                    TextOrientationSpan.CombineUpright(),
                    0,
                    2,
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE,
                )
                setSpan(
                    TextOrientationSpan.CombineUpright(),
                    2,
                    4,
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE,
                )
            }

        val runs = collectRuns(spanned)

        assertThat(runs)
            .containsExactly(
                OrientationRun(0, 2, ResolvedOrientation.TateChuYoko),
                OrientationRun(2, 4, ResolvedOrientation.TateChuYoko),
            )
            .inOrder()
    }

    // Branch: Multiple overlapping TextOrientationSpans on the same range use spans.last() for
    // CombineUpright, Upright, and Sideways.
    @Test
    fun kex_forEachOrientation_whenMultipleOverlappingSpans_usesLastAttachedSpan() {
        val combineWins =
            SpannableString("12").apply {
                setSpan(TextOrientationSpan.Sideways(), 0, 2, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                setSpan(
                    TextOrientationSpan.CombineUpright(),
                    0,
                    2,
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE,
                )
            }
        val uprightWins =
            SpannableString("12").apply {
                setSpan(
                    TextOrientationSpan.CombineUpright(),
                    0,
                    2,
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE,
                )
                setSpan(TextOrientationSpan.Upright(), 0, 2, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            }
        val sidewaysWins =
            SpannableString("あ").apply {
                setSpan(TextOrientationSpan.Upright(), 0, 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                setSpan(TextOrientationSpan.Sideways(), 0, 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            }

        assertThat(collectRuns(combineWins))
            .containsExactly(OrientationRun(0, 2, ResolvedOrientation.TateChuYoko))
        assertThat(collectRuns(uprightWins))
            .containsExactly(OrientationRun(0, 2, ResolvedOrientation.Upright))
        assertThat(collectRuns(sidewaysWins))
            .containsExactly(OrientationRun(0, 1, ResolvedOrientation.Rotate))
    }

    // =========================================================================================
    // Target 3: forOrientationNoSpans & resolveOrientation
    // =========================================================================================

    // Branch: Supplementary surrogate-pair code points (charCount == 2) step by 2 UTF-16 code units
    // in forOrientationNoSpans across Mixed, Upright, and Sideways orientations.
    @Test
    fun kex_forEachOrientation_whenSupplementarySurrogatePairs_resolvesCodePointBoundaries() {
        // U+1D400 (\uD835\uDC00, Mathematical Bold Capital A) is Rotate on all API levels;
        // U+3042 ('あ') is Upright on all API levels.
        val mathToHiragana = "\uD835\uDC00\u3042"
        assertThat(collectRuns(mathToHiragana, textOrientation = TextOrientation.Mixed))
            .containsExactly(
                OrientationRun(0, 2, ResolvedOrientation.Rotate),
                OrientationRun(2, 3, ResolvedOrientation.Upright),
            )
            .inOrder()

        // U+2000B (\uD840\uDC0B) is in CJK Unified Ideographs Extension B; 'A' is Basic Latin.
        // On API 29+, U+2000B resolves Upright via ICU VERTICAL_ORIENTATION while its low surrogate
        // (\uDC0B) alone resolves Rotate, so stepping by 1 instead of charCount (2) would split at
        // index 1. On API < 29, Extension B is not in the 8 UnicodeBlock fallback entries.
        val text = "\uD840\uDC0BA"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            assertThat(collectRuns(text, textOrientation = TextOrientation.Mixed))
                .containsExactly(
                    OrientationRun(0, 2, ResolvedOrientation.Upright),
                    OrientationRun(2, 3, ResolvedOrientation.Rotate),
                )
                .inOrder()
        } else {
            assertThat(collectRuns(text, textOrientation = TextOrientation.Mixed))
                .containsExactly(OrientationRun(0, 3, ResolvedOrientation.Rotate))
        }

        val uprightRuns = collectRuns(text, textOrientation = TextOrientation.Upright)
        val sidewaysRuns = collectRuns(text, textOrientation = TextOrientation.Sideways)

        assertThat(uprightRuns).containsExactly(OrientationRun(0, 3, ResolvedOrientation.Upright))
        assertThat(sidewaysRuns).containsExactly(OrientationRun(0, 3, ResolvedOrientation.Rotate))
    }

    // Branch: resolveOrientation with TextOrientation.Mixed across all 8 upright UnicodeBlock
    // categories (CJK Unified Ideographs, Extension A, Compatibility Ideographs, Hiragana,
    // Katakana, CJK Symbols and Punctuation, Halfwidth and Fullwidth Forms, General Punctuation)
    // and non-CJK rotated blocks. Exercises the 8 UnicodeBlock branches on API < 29 and verifies
    // the ICU VERTICAL_ORIENTATION path on API 29+.
    @Test
    fun kex_resolveOrientation_whenMixedAcrossAllEightUprightUnicodeBlocks_resolvesUpright() {
        // U+5B57 CJK_UNIFIED_IDEOGRAPHS
        // U+3400 CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
        // U+F900 CJK_COMPATIBILITY_IDEOGRAPHS
        // U+3042 HIRAGANA
        // U+30A2 KATAKANA
        // U+3002 CJK_SYMBOLS_AND_PUNCTUATION
        // U+FF21 HALFWIDTH_AND_FULLWIDTH_FORMS
        // U+203B GENERAL_PUNCTUATION (REFERENCE MARK)
        val allEightUprightBlocks = "\u5B57\u3400\uF900\u3042\u30A2\u3002\uFF21\u203B"
        val withTrailingLatin = "${allEightUprightBlocks}A"

        val runs = collectRuns(withTrailingLatin, textOrientation = TextOrientation.Mixed)

        assertThat(runs)
            .containsExactly(
                OrientationRun(0, 8, ResolvedOrientation.Upright),
                OrientationRun(8, 9, ResolvedOrientation.Rotate),
            )
            .inOrder()
    }
}
