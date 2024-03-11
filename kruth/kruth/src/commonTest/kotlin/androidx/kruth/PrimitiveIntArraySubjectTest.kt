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

import kotlin.test.Test
import kotlin.test.assertFailsWith

class PrimitiveIntArraySubjectTest {

    @Test
    fun isEqualTo() {
        assertThat(intArrayOf(2, 5)).isEqualTo(intArrayOf(2, 5))
    }

    @Test
    fun isEqualTo_Same() {
        val same = intArrayOf(2, 5)
        assertThat(same).isEqualTo(same)
    }

    @Test
    fun asList() {
        assertThat(intArrayOf(5, 2, 9)).asList().containsAtLeast(2, 9)
    }

    @Test
    fun hasLength() {
        assertThat(intArrayOf()).hasLength(0)
        assertThat(intArrayOf(2, 5)).hasLength(2)
    }

    @Test
    fun hasLengthFail() {
        assertFailsWith<AssertionError> {
            assertThat(intArrayOf(2, 5)).hasLength(1)
        }
    }

    @Test
    fun hasLengthNegative() {
        assertFailsWith<IllegalArgumentException> {
            assertThat(intArrayOf(2, 5)).hasLength(-1)
        }
    }

    @Test
    fun isEmpty() {
        assertThat(intArrayOf()).isEmpty()
    }

    @Test
    fun isEmptyFail() {
        assertFailsWith<AssertionError> {
            assertThat(intArrayOf(2, 5)).isEmpty()
        }
    }

    @Test
    fun isNotEmpty() {
        assertThat(intArrayOf(2, 5)).isNotEmpty()
    }

    @Test
    fun isNotEmptyFail() {
        assertFailsWith<AssertionError> {
            assertThat(intArrayOf()).isNotEmpty()
        }
    }

    @Test
    fun isEqualTo_Fail_UnequalOrdering() {
        assertFailsWith<AssertionError> {
            assertThat(intArrayOf(2, 3)).isEqualTo(intArrayOf(3, 2))
        }
    }

    @Test
    fun isEqualTo_Fail_NotAnintArrayOf() {
        assertFailsWith<AssertionError> {
            assertThat(intArrayOf(2, 3, 4)).isEqualTo(Any())
        }
    }

    @Test
    fun isNotEqualTo_SameLengths() {
        assertThat(intArrayOf(2, 3)).isNotEqualTo(intArrayOf(3, 2))
    }

    @Test
    fun isNotEqualTo_DifferentLengths() {
        assertThat(intArrayOf(2, 3)).isNotEqualTo(intArrayOf(2, 3, 1))
    }

    @Test
    fun isNotEqualTo_DifferentTypes() {
        assertThat(intArrayOf(2, 3)).isNotEqualTo(Any())
    }

    @Test
    fun isNotEqualTo_FailEquals() {
        assertFailsWith<AssertionError> {
            assertThat(intArrayOf(2, 3)).isNotEqualTo(intArrayOf(2, 3))
        }
    }

    @Test
    fun isNotEqualTo_FailSame() {
        val same = intArrayOf(2, 3)
        assertFailsWith<AssertionError> {
            assertThat(same).isNotEqualTo(same)
        }
    }
}
