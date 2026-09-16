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

import android.text.SpannableString
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

private const val SPAN_FLAG = SpannableString.SPAN_INCLUSIVE_EXCLUSIVE

/**
 * Test cases for [forEachOrientation].
 *
 * The function resolves each character to a [ResolvedOrientation]. Then it merges the adjacent
 * characters that have the same orientation into one run.
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
class OrientationsTest {
    private sealed interface Run

    private data class Upright(val start: Int, val end: Int) : Run

    private data class Rotate(val start: Int, val end: Int) : Run

    private data class TateChuYoko(val start: Int, val end: Int) : Run

    private fun resolve(
        text: CharSequence,
        start: Int = 0,
        end: Int = text.length,
        textOrientation: TextOrientation = TextOrientation.Mixed,
    ): List<Run> {
        val out = mutableListOf<Run>()
        forEachOrientation(text, start, end, textOrientation) { oStart, oEnd, orientation ->
            when (orientation) {
                ResolvedOrientation.Upright -> out.add(Upright(oStart, oEnd))
                ResolvedOrientation.Rotate -> out.add(Rotate(oStart, oEnd))
                ResolvedOrientation.TateChuYoko -> out.add(TateChuYoko(oStart, oEnd))
            }
        }
        return out
    }

    @Test
    fun orientation_emptyText() {
        // Empty text
        assertThat(resolve("")).isEmpty()
        assertThat(resolve("", 0, 0)).isEmpty()

        // Empty range
        assertThat(resolve("abc", 0, 0)).isEmpty()
        assertThat(resolve("abc", 1, 1)).isEmpty()

        // Reversed range (invalid range)
        assertThat(resolve("abc", 2, 1)).isEmpty()
    }

    @Test
    fun orientation_noOverrideText_MixedOrientation() {
        // Whole text
        // Japanese letters: resolved to upright.
        assertThat(resolve("あいうえお")).containsExactly(Upright(0, 5))

        // English letters: resolved to Rotate.
        assertThat(resolve("abcde")).containsExactly(Rotate(0, 5))

        // Japanese and English mixed text: resolve as multiple runs
        assertThat(resolve("あいうえおabcde")).containsExactly(Upright(0, 5), Rotate(5, 10)).inOrder()

        // Substring
        assertThat(resolve("あいうえお", 1, 3)).containsExactly(Upright(1, 3))

        assertThat(resolve("abcde", 1, 3)).containsExactly(Rotate(1, 3))

        assertThat(resolve("あいうえおabcde", 4, 7))
            .containsExactly(Upright(4, 5), Rotate(5, 7))
            .inOrder()
    }

    @Test
    fun orientation_noOverrideText_UprightOrientation() {
        assertThat(resolve("あいうえお", textOrientation = TextOrientation.Upright))
            .containsExactly(Upright(0, 5))

        assertThat(resolve("abcde", textOrientation = TextOrientation.Upright))
            .containsExactly(Upright(0, 5))

        assertThat(resolve("あいうえおabcde", textOrientation = TextOrientation.Upright))
            .containsExactly(Upright(0, 10))

        // Substring
        assertThat(resolve("あいうえお", 1, 3, TextOrientation.Upright)).containsExactly(Upright(1, 3))

        assertThat(resolve("abcde", 1, 3, TextOrientation.Upright)).containsExactly(Upright(1, 3))

        assertThat(resolve("あいうえおabcde", 4, 7, textOrientation = TextOrientation.Upright))
            .containsExactly(Upright(4, 7))
    }

    @Test
    fun orientation_noOverrideText_SidewaysOrientation() {
        assertThat(resolve("あいうえお", textOrientation = TextOrientation.Sideways))
            .containsExactly(Rotate(0, 5))

        assertThat(resolve("abcde", textOrientation = TextOrientation.Sideways))
            .containsExactly(Rotate(0, 5))

        assertThat(resolve("あいうえおabcde", textOrientation = TextOrientation.Sideways))
            .containsExactly(Rotate(0, 10))

        // Substring
        assertThat(resolve("あいうえお", 1, 3, TextOrientation.Sideways)).containsExactly(Rotate(1, 3))

        assertThat(resolve("abcde", 1, 3, TextOrientation.Sideways)).containsExactly(Rotate(1, 3))

        assertThat(resolve("あいうえおabcde", 4, 7, TextOrientation.Sideways))
            .containsExactly(Rotate(4, 7))
    }

    @Test
    fun orientation_overrideText_UprightOverride() {
        var runs =
            resolve(
                SpannableString("あいうえお").apply {
                    setSpan(TextOrientationSpan.Upright(), 1, 2, SPAN_FLAG)
                }
            )
        assertThat(runs).containsExactly(Upright(0, 5))

        runs =
            resolve(
                SpannableString("abcde").apply {
                    setSpan(TextOrientationSpan.Upright(), 1, 2, SPAN_FLAG)
                }
            )
        assertThat(runs).containsExactly(Rotate(0, 1), Upright(1, 2), Rotate(2, 5)).inOrder()

        runs =
            resolve(
                SpannableString("あいうえおabcde").apply {
                    setSpan(TextOrientationSpan.Upright(), 4, 7, SPAN_FLAG)
                }
            )
        assertThat(runs).containsExactly(Upright(0, 7), Rotate(7, 10)).inOrder()
    }

    @Test
    fun orientation_overrideText_SidewaysOverride() {
        var runs =
            resolve(
                SpannableString("あいうえお").apply {
                    setSpan(TextOrientationSpan.Sideways(), 1, 2, SPAN_FLAG)
                }
            )
        assertThat(runs).containsExactly(Upright(0, 1), Rotate(1, 2), Upright(2, 5)).inOrder()

        runs =
            resolve(
                SpannableString("abcde").apply {
                    setSpan(TextOrientationSpan.Sideways(), 1, 2, SPAN_FLAG)
                }
            )
        assertThat(runs).containsExactly(Rotate(0, 5))

        runs =
            resolve(
                SpannableString("あいうえおabcde").apply {
                    setSpan(TextOrientationSpan.Sideways(), 4, 7, SPAN_FLAG)
                }
            )
        assertThat(runs).containsExactly(Upright(0, 4), Rotate(4, 10)).inOrder()
    }

    /** If more than one span covers a range, the last span that you attach controls the result. */
    @Test
    fun orientation_overlappingSpans_lastAttachedWins() {
        // The test attaches Sideways first, then Upright, on the same range. The range stays
        // upright.
        var runs =
            resolve(
                SpannableString("abcde").apply {
                    setSpan(TextOrientationSpan.Sideways(), 1, 2, SPAN_FLAG)
                    setSpan(TextOrientationSpan.Upright(), 1, 2, SPAN_FLAG)
                }
            )
        assertThat(runs).containsExactly(Rotate(0, 1), Upright(1, 2), Rotate(2, 5)).inOrder()

        // The test reverses the order of the two spans. Then all the text becomes one run.
        runs =
            resolve(
                SpannableString("abcde").apply {
                    setSpan(TextOrientationSpan.Upright(), 1, 2, SPAN_FLAG)
                    setSpan(TextOrientationSpan.Sideways(), 1, 2, SPAN_FLAG)
                }
            )
        assertThat(runs).containsExactly(Rotate(0, 5))
    }

    @Test
    fun orientation_TateChuYoko() {
        var runs =
            resolve(
                SpannableString("abcde").apply {
                    setSpan(TextOrientationSpan.CombineUpright(), 1, 2, SPAN_FLAG)
                }
            )
        assertThat(runs).containsExactly(Rotate(0, 1), TateChuYoko(1, 2), Rotate(2, 5)).inOrder()

        // Even if two TateChuYoko runs are adjacent, the function must not merge them.
        runs =
            resolve(
                SpannableString("abcde").apply {
                    setSpan(TextOrientationSpan.CombineUpright(), 1, 2, SPAN_FLAG)
                    setSpan(TextOrientationSpan.CombineUpright(), 2, 4, SPAN_FLAG)
                }
            )
        assertThat(runs)
            .containsExactly(Rotate(0, 1), TateChuYoko(1, 2), TateChuYoko(2, 4), Rotate(4, 5))
            .inOrder()
    }
}
