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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

// U+1D400 Mathematical Bold capital A (𝐀). Two characters encode this one code point.
private const val SURROGATE_PAIR = "\uD835\uDC00"

/** Test cases for the helper functions that the vertical layout code uses to read characters. */
@SmallTest
@RunWith(AndroidJUnit4::class)
class TextUtilsTest {

    /** Collects the pairs of index and code point that [forEachCodePoint] reports. */
    private fun codePoints(
        text: CharSequence,
        start: Int = 0,
        end: Int = text.length,
    ): List<Pair<Int, Int>> {
        val out = mutableListOf<Pair<Int, Int>>()
        forEachCodePoint(text, start, end) { index, cp -> out.add(index to cp) }
        return out
    }

    @Test
    fun forEachCodePoint_bmpCharactersAdvanceOneIndex() {
        assertThat(codePoints("abc"))
            .containsExactly(0 to 'a'.code, 1 to 'b'.code, 2 to 'c'.code)
            .inOrder()
    }

    @Test
    fun forEachCodePoint_surrogatePairAdvancesTwoIndices() {
        // The surrogate pair is one code point. As a result, the next character has index 2, not
        // index 1. If the function reports the low surrogate alone, the decoding is not correct.
        assertThat(codePoints(SURROGATE_PAIR + "あ"))
            .containsExactly(0 to 0x1D400, 2 to 0x3042)
            .inOrder()
    }

    @Test
    fun forEachCodePoint_honorsRange() {
        assertThat(codePoints("abcde", 1, 3))
            .containsExactly(1 to 'b'.code, 2 to 'c'.code)
            .inOrder()

        // An empty range and a reversed range report no code points.
        assertThat(codePoints("abc", 0, 0)).isEmpty()
        assertThat(codePoints("abc", 2, 1)).isEmpty()
    }
}
