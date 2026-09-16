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

import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/** Test cases for the [Paint] backports that the vertical layout code uses. */
@SmallTest
@RunWith(AndroidJUnit4::class)
class PaintCompatTest {

    private val paint = Paint().apply { textSize = 100f }

    private fun advance(text: String, offset: Int, out: FloatArray, outOffset: Int = 0): Float =
        paint.getRunCharacterAdvanceCompat(
            text,
            0,
            text.length,
            0,
            text.length,
            false,
            offset,
            out,
            outOffset,
        )

    @Test
    fun getRunCharacterAdvanceCompat_returnsAdvanceConsumedUpToOffset() {
        val text = "abcde"
        val advances = FloatArray(text.length)
        advance(text, text.length, advances)

        // Each prefix must report the sum of the advances that it covers. This rule is also true
        // for the empty prefix.
        for (offset in 0..text.length) {
            val out = FloatArray(text.length)
            val expected = advances.take(offset).sum()
            assertThat(advance(text, offset, out)).isWithin(0.01f).of(expected)
        }
    }

    @Test
    fun getRunCharacterAdvanceCompat_offsetIsMeasuredFromOutOffset() {
        val text = "abcde"
        val advances = FloatArray(text.length)
        advance(text, text.length, advances)

        val out = FloatArray(text.length + 3)
        val actual = advance(text, 2, out, outOffset = 3)

        assertThat(actual).isWithin(0.01f).of(advances[0] + advances[1])
        // The function must not change the part of the array before the output offset.
        assertThat(out.take(3)).containsExactly(0f, 0f, 0f).inOrder()
    }

    @Test
    fun getRunCharacterAdvanceCompat_fillsPerCharacterAdvances() {
        val text = "abcde"
        val out = FloatArray(text.length)
        val total = advance(text, text.length, out)

        assertThat(out.none { it <= 0f }).isTrue()
        assertThat(out.sum()).isWithin(0.01f).of(total)
    }
}
