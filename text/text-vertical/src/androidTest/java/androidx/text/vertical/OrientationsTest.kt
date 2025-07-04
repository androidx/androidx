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

import android.text.SpannableString
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.text.vertical.OrientationMode
import androidx.text.vertical.ResolvedOrientation
import androidx.text.vertical.TextOrientation
import androidx.text.vertical.TextOrientationSpan
import androidx.text.vertical.forEachOrientation
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

private const val SPAN_FLAG = SpannableString.SPAN_INCLUSIVE_EXCLUSIVE

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
        @OrientationMode textOrientation: Int = TextOrientation.MIXED,
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

    private fun resolve(
        text: CharSequence,
        @OrientationMode textOrientation: Int = TextOrientation.MIXED,
    ) = resolve(text, 0, text.length, textOrientation)

    @Test
    fun emptyText() {
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
    fun noOverrideText_MixedOrientation() {
        // Whole text
        // Japanese letters: resolved to upright.
        var runs = resolve("あいうえお")
        assertThat(runs.size).isEqualTo(1)
        assertThat(runs[0]).isEqualTo(Upright(0, 5))

        // English letters: resolved to Rotate.
        runs = resolve("abcde")
        assertThat(runs.size).isEqualTo(1)
        assertThat(runs[0]).isEqualTo(Rotate(0, 5))

        // Japanese and English mixed text: resolve as multiple runs
        runs = resolve("あいうえおabcde")
        assertThat(runs.size).isEqualTo(2)
        assertThat(runs[0]).isEqualTo(Upright(0, 5))
        assertThat(runs[1]).isEqualTo(Rotate(5, 10))

        // Substring
        runs = resolve("あいうえお", 1, 3)
        assertThat(runs.size).isEqualTo(1)
        assertThat(runs[0]).isEqualTo(Upright(1, 3))

        runs = resolve("abcde", 1, 3)
        assertThat(runs.size).isEqualTo(1)
        assertThat(runs[0]).isEqualTo(Rotate(1, 3))

        runs = resolve("あいうえおabcde", 4, 7)
        assertThat(runs.size).isEqualTo(2)
        assertThat(runs[0]).isEqualTo(Upright(4, 5))
        assertThat(runs[1]).isEqualTo(Rotate(5, 7))
    }

    @Test
    fun noOverrideText_UprightOrientation() {
        var runs = resolve("あいうえお", TextOrientation.UPRIGHT)
        assertThat(runs.size).isEqualTo(1)
        assertThat(runs[0]).isEqualTo(Upright(0, 5))

        runs = resolve("abcde", TextOrientation.UPRIGHT)
        assertThat(runs.size).isEqualTo(1)
        assertThat(runs[0]).isEqualTo(Upright(0, 5))

        runs = resolve("あいうえおabcde", TextOrientation.UPRIGHT)
        assertThat(runs.size).isEqualTo(1)
        assertThat(runs[0]).isEqualTo(Upright(0, 10))

        // Substring
        runs = resolve("あいうえお", 1, 3, TextOrientation.UPRIGHT)
        assertThat(runs.size).isEqualTo(1)
        assertThat(runs[0]).isEqualTo(Upright(1, 3))

        runs = resolve("abcde", 1, 3, TextOrientation.UPRIGHT)
        assertThat(runs.size).isEqualTo(1)
        assertThat(runs[0]).isEqualTo(Upright(1, 3))

        runs = resolve("あいうえおabcde", 4, 7, textOrientation = TextOrientation.UPRIGHT)
        assertThat(runs.size).isEqualTo(1)
        assertThat(runs[0]).isEqualTo(Upright(4, 7))
    }

    @Test
    fun noOverrideText_SidewaysOrientation() {
        var runs = resolve("あいうえお", TextOrientation.SIDEWAYS)
        assertThat(runs.size).isEqualTo(1)
        assertThat(runs[0]).isEqualTo(Rotate(0, 5))

        runs = resolve("abcde", TextOrientation.SIDEWAYS)
        assertThat(runs.size).isEqualTo(1)
        assertThat(runs[0]).isEqualTo(Rotate(0, 5))

        runs = resolve("あいうえおabcde", TextOrientation.SIDEWAYS)
        assertThat(runs.size).isEqualTo(1)
        assertThat(runs[0]).isEqualTo(Rotate(0, 10))

        // Substring
        runs = resolve("あいうえお", 1, 3, TextOrientation.SIDEWAYS)
        assertThat(runs.size).isEqualTo(1)
        assertThat(runs[0]).isEqualTo(Rotate(1, 3))

        runs = resolve("abcde", 1, 3, TextOrientation.SIDEWAYS)
        assertThat(runs.size).isEqualTo(1)
        assertThat(runs[0]).isEqualTo(Rotate(1, 3))

        runs = resolve("あいうえおabcde", 4, 7, TextOrientation.SIDEWAYS)
        assertThat(runs.size).isEqualTo(1)
        assertThat(runs[0]).isEqualTo(Rotate(4, 7))
    }

    @Test
    fun overrideText_UprightOverride() {
        var runs =
            resolve(
                SpannableString("あいうえお").apply {
                    setSpan(TextOrientationSpan.Upright(), 1, 2, SPAN_FLAG)
                }
            )
        assertThat(runs.size).isEqualTo(1)
        assertThat(runs[0]).isEqualTo(Upright(0, 5))

        runs =
            resolve(
                SpannableString("abcde").apply {
                    setSpan(TextOrientationSpan.Upright(), 1, 2, SPAN_FLAG)
                }
            )
        assertThat(runs.size).isEqualTo(3)
        assertThat(runs[0]).isEqualTo(Rotate(0, 1))
        assertThat(runs[1]).isEqualTo(Upright(1, 2))
        assertThat(runs[2]).isEqualTo(Rotate(2, 5))

        runs =
            resolve(
                SpannableString("あいうえおabcde").apply {
                    setSpan(TextOrientationSpan.Upright(), 4, 7, SPAN_FLAG)
                }
            )
        assertThat(runs.size).isEqualTo(2)
        assertThat(runs[0]).isEqualTo(Upright(0, 7))
        assertThat(runs[1]).isEqualTo(Rotate(7, 10))
    }

    @Test
    fun overrideText_SidewaysOverride() {
        var runs =
            resolve(
                SpannableString("あいうえお").apply {
                    setSpan(TextOrientationSpan.Sideways(), 1, 2, SPAN_FLAG)
                }
            )
        assertThat(runs.size).isEqualTo(3)
        assertThat(runs[0]).isEqualTo(Upright(0, 1))
        assertThat(runs[1]).isEqualTo(Rotate(1, 2))
        assertThat(runs[2]).isEqualTo(Upright(2, 5))

        runs =
            resolve(
                SpannableString("abcde").apply {
                    setSpan(TextOrientationSpan.Sideways(), 1, 2, SPAN_FLAG)
                }
            )
        assertThat(runs.size).isEqualTo(1)
        assertThat(runs[0]).isEqualTo(Rotate(0, 5))

        runs =
            resolve(
                SpannableString("あいうえおabcde").apply {
                    setSpan(TextOrientationSpan.Sideways(), 4, 7, SPAN_FLAG)
                }
            )
        assertThat(runs.size).isEqualTo(2)
        assertThat(runs[0]).isEqualTo(Upright(0, 4))
        assertThat(runs[1]).isEqualTo(Rotate(4, 10))
    }

    @Test
    fun tateChuToko() {
        var runs =
            resolve(
                SpannableString("abcde").apply {
                    setSpan(TextOrientationSpan.TextCombineUpright(), 1, 2, SPAN_FLAG)
                }
            )
        assertThat(runs.size).isEqualTo(3)
        assertThat(runs[0]).isEqualTo(Rotate(0, 1))
        assertThat(runs[1]).isEqualTo(TateChuYoko(1, 2))
        assertThat(runs[2]).isEqualTo(Rotate(2, 5))

        // TateChuYoko should not be extended even if they are connected.
        runs =
            resolve(
                SpannableString("abcde").apply {
                    setSpan(TextOrientationSpan.TextCombineUpright(), 1, 2, SPAN_FLAG)
                    setSpan(TextOrientationSpan.TextCombineUpright(), 2, 4, SPAN_FLAG)
                }
            )
        assertThat(runs.size).isEqualTo(4)
        assertThat(runs[0]).isEqualTo(Rotate(0, 1))
        assertThat(runs[1]).isEqualTo(TateChuYoko(1, 2))
        assertThat(runs[2]).isEqualTo(TateChuYoko(2, 4))
        assertThat(runs[3]).isEqualTo(Rotate(4, 5))
    }
}
