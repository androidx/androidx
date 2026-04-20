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

package androidx.compose.foundation.text.input.internal

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class IntervalTest {

    @Test
    fun flagsCombinations() {
        val flags = listOf(false to false, true to false, false to true, true to true)

        for ((flag1, flag2) in flags) {
            val interval = Interval(start = 10, end = 20, flag1 = flag1, flag2 = flag2)
            assertThat(interval.start).isEqualTo(10)
            assertThat(interval.end).isEqualTo(20)
            assertThat(interval.flag1).isEqualTo(flag1)
            assertThat(interval.flag2).isEqualTo(flag2)
        }
    }

    @Test
    fun extremePositiveValues() {
        val interval =
            Interval(start = Int.MAX_VALUE, end = Int.MAX_VALUE, flag1 = true, flag2 = false)
        assertThat(interval.start).isEqualTo(Int.MAX_VALUE)
        assertThat(interval.end).isEqualTo(Int.MAX_VALUE)
        assertThat(interval.flag1).isTrue()
        assertThat(interval.flag2).isFalse()
    }

    @Test
    fun invalidNegativeStartValue_doesNotCorruptOtherFields() {
        val interval = Interval(start = -10, end = 20, flag1 = true, flag2 = true)

        // Negative value might not be preserved correctly because the sign bit is used for the flag
        // However, other fields should still be retrieved correctly
        assertThat(interval.end).isEqualTo(20)
        assertThat(interval.flag1).isTrue()
        assertThat(interval.flag2).isTrue()

        val intervalFalse = Interval(start = -10, end = 20, flag1 = false, flag2 = false)
        assertThat(intervalFalse.end).isEqualTo(20)
        assertThat(intervalFalse.flag1).isFalse()
        assertThat(intervalFalse.flag2).isFalse()
    }

    @Test
    fun invalidNegativeEndValue_doesNotCorruptOtherFields() {
        val interval = Interval(start = 10, end = -20, flag1 = true, flag2 = true)

        assertThat(interval.start).isEqualTo(10)
        assertThat(interval.flag1).isTrue()
        assertThat(interval.flag2).isTrue()

        val intervalFalse = Interval(start = 10, end = -20, flag1 = false, flag2 = false)
        assertThat(intervalFalse.start).isEqualTo(10)
        assertThat(intervalFalse.flag1).isFalse()
        assertThat(intervalFalse.flag2).isFalse()
    }

    @Test
    fun invalidNegativeBothValues_doesNotCorruptFlags() {
        val interval = Interval(start = -10, end = -20, flag1 = true, flag2 = false)

        assertThat(interval.flag1).isTrue()
        assertThat(interval.flag2).isFalse()

        val intervalFalse = Interval(start = -10, end = -20, flag1 = false, flag2 = true)
        assertThat(intervalFalse.flag1).isFalse()
        assertThat(intervalFalse.flag2).isTrue()
    }

    @Test
    fun invalidInterval_hasCorrectValues() {
        val interval = Interval.Invalid
        assertThat(interval.start).isEqualTo(Int.MAX_VALUE)
        assertThat(interval.end).isEqualTo(0)
    }

    @Test
    fun zeroValues() {
        val interval = Interval(start = 0, end = 0, flag1 = false, flag2 = false)
        assertThat(interval.start).isEqualTo(0)
        assertThat(interval.end).isEqualTo(0)
        assertThat(interval.flag1).isFalse()
        assertThat(interval.flag2).isFalse()
    }
}
