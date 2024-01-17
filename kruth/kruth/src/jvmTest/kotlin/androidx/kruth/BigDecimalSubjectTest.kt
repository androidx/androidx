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

import java.math.BigDecimal
import java.math.BigDecimal.TEN
import kotlin.test.Test
import kotlin.test.assertFailsWith

class BigDecimalSubjectTest {

    @Test
    fun isEqualTo() {
        // make sure this still works
        assertThat(TEN).isEqualTo(TEN)
    }

    @Test
    fun isEquivalentAccordingToCompareTo() {
        // make sure this still works
        assertThat(TEN).isEquivalentAccordingToCompareTo(TEN)
    }

    @Test
    fun isEqualToIgnoringScale_bigDecimal() {
        assertThat(TEN).isEqualToIgnoringScale(TEN)
        assertThat(TEN).isEqualToIgnoringScale(BigDecimal(10))

        assertFailsWith<AssertionError> {
            assertThat(TEN).isEqualToIgnoringScale(BigDecimal(3))
        }
    }

    @Test
    fun isEqualToIgnoringScale_int() {
        assertThat(TEN).isEqualToIgnoringScale(10)

        assertFailsWith<AssertionError> {
            assertThat(TEN).isEqualToIgnoringScale(3)
        }
    }

    @Test
    fun isEqualToIgnoringScale_long() {
        assertThat(TEN).isEqualToIgnoringScale(10L)

        assertFailsWith<AssertionError> {
            assertThat(TEN).isEqualToIgnoringScale(3L)
        }
    }

    @Test
    fun isEqualToIgnoringScale_string() {
        assertThat(TEN).isEqualToIgnoringScale("10")
        assertThat(TEN).isEqualToIgnoringScale("10.")
        assertThat(TEN).isEqualToIgnoringScale("10.0")
        assertThat(TEN).isEqualToIgnoringScale("10.00")

        assertFailsWith<AssertionError> {
            assertThat(TEN).isEqualToIgnoringScale("3")
        }
    }
}
