/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.kruth

import com.google.common.collect.Range
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ComparableSubjectJvmTest {

    @Test
    fun strings() {
        val range = Range.closed("a", "c")
        assertThat("b").isIn(range)
        assertThat("d").isNotIn(range)
    }

    @Test
    fun isIn_int() {
        val oneToFive = Range.closed(1, 5)
        assertThat(4).isIn(oneToFive)
        assertFailsWith<AssertionError> { assertThat(6).isIn(oneToFive) }
    }

    @Test
    fun isNotIn_int() {
        val oneToFive = Range.closed(1, 5)
        assertThat(6).isNotIn(oneToFive)
        assertFailsWith<AssertionError> { assertThat(4).isNotIn(oneToFive) }
    }

    @Test
    fun isIn_long() {
        val oneToFive = Range.closed(1L, 5L)
        assertThat(4L).isIn(oneToFive)
        assertFailsWith<AssertionError> { assertThat(6L).isIn(oneToFive) }
    }

    @Test
    fun isNotIn_long() {
        val oneToFive = Range.closed(1L, 5L)
        assertThat(6L).isNotIn(oneToFive)
        assertFailsWith<AssertionError> { assertThat(4L).isNotIn(oneToFive) }
    }

    @Test
    fun isIn_string() {
        val oneToFive = Range.closed("a", "c")
        assertThat("b").isIn(oneToFive)
        assertFailsWith<AssertionError> { assertThat("d").isIn(oneToFive) }
    }

    @Test
    fun isNotIn_string() {
        val oneToFive = Range.closed("a", "c")
        assertThat("d").isNotIn(oneToFive)
        assertFailsWith<AssertionError> { assertThat("b").isNotIn(oneToFive) }
    }
}
