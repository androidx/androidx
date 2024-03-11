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

class ObjectArraySubjectTest {

    @Test
    fun isEqualTo() {
        assertThat(arrayOf("A", 5L)).isEqualTo(arrayOf("A", 5L))
    }

    @Test
    fun isEqualTo_same() {
        val same = arrayOf("A", 5L)
        assertThat(same).isEqualTo(same)
    }

    @Test
    fun asList() {
        assertThat(arrayOf("A", 5L)).asList().contains("A")
    }

    @Test
    fun hasLength() {
        assertThat(emptyArray<Any>()).hasLength(0)
        assertThat(arrayOf("A", 5L)).hasLength(2)
        assertThat(arrayOf<Array<Any>>()).hasLength(0)
        assertThat(arrayOf<Array<Any>>(emptyArray())).hasLength(1)
    }

    @Test
    fun hasLengthFail() {
        assertFailsWith<AssertionError> {
            assertThat(arrayOf("A", 5L)).hasLength(1)
        }
    }

    @Test
    fun hasLengthMultiFail() {
        assertFailsWith<AssertionError> {
            assertThat(arrayOf<Array<Any>>(arrayOf("A"), arrayOf(5L))).hasLength(1)
        }
    }

    @Test
    fun hasLengthNegative() {
        assertFailsWith<IllegalArgumentException> {
            assertThat(arrayOf(2, 5)).hasLength(-1)
        }
    }

    @Test
    fun isEmpty() {
        assertThat(emptyArray<Any>()).isEmpty()
        assertThat(arrayOf<Array<Any>>()).isEmpty()
    }

    @Test
    fun isEmptyFail() {
        assertFailsWith<AssertionError> {
            assertThat(arrayOf("A", 5L)).isEmpty()
        }
    }

    @Test
    fun isNotEmpty() {
        assertThat(arrayOf("A", 5L)).isNotEmpty()
        assertThat(arrayOf(arrayOf("A"), arrayOf(5L))).isNotEmpty()
    }

    @Test
    fun isNotEmptyFail() {
        assertFailsWith<AssertionError> {
            assertThat(emptyArray<Any>()).isNotEmpty()
        }
    }

    @Test
    fun isEqualTo_fail_unequalOrdering() {
        assertFailsWith<AssertionError> {
            assertThat(arrayOf("A", 5L)).isEqualTo(arrayOf(5L, "A"))
        }
    }

    @Test
    fun isEqualTo_fail_unequalOrderingMultiDimensional_00() {
        assertFailsWith<AssertionError> {
            assertThat(arrayOf(arrayOf("A"), arrayOf(5L)))
                .isEqualTo(arrayOf(arrayOf(5L), arrayOf("A")))
        }
    }

    @Test
    fun isEqualTo_fail_unequalOrderingMultiDimensional_01() {
        assertFailsWith<AssertionError> {
            assertThat(arrayOf(arrayOf("A", "B"), arrayOf(5L)))
                .isEqualTo(arrayOf(arrayOf("A"), arrayOf(5L)))
        }
    }

    @Test
    fun isEqualTo_fail_unequalOrderingMultiDimensional_11() {
        assertFailsWith<AssertionError> {
            assertThat(arrayOf(arrayOf("A"), arrayOf(5L)))
                .isEqualTo(arrayOf(arrayOf("A"), arrayOf(5L, 6L)))
        }
    }

    @Test
    fun isEqualTo_fail_notAnArray() {
        assertFailsWith<AssertionError> {
            assertThat(arrayOf("A", 5L)).isEqualTo(Any())
        }
    }

    @Test
    fun isNotEqualTo_sameLengths() {
        assertThat(arrayOf("A", 5L)).isNotEqualTo(arrayOf("C", 5L))
        assertThat(arrayOf(arrayOf("A"), arrayOf(5L)))
            .isNotEqualTo(arrayOf(arrayOf("C"), arrayOf(5L)))
    }

    @Test
    fun isNotEqualTo_differentLengths() {
        assertThat(arrayOf("A", 5L)).isNotEqualTo(arrayOf("A", 5L, "c"))
        assertThat(arrayOf(arrayOf("A"), arrayOf(5L)))
            .isNotEqualTo(arrayOf(arrayOf("A", "c"), arrayOf(5L)))
        assertThat(arrayOf(arrayOf("A"), arrayOf(5L)))
            .isNotEqualTo(arrayOf(arrayOf("A"), arrayOf(5L), arrayOf("C")))
    }

    @Test
    fun isNotEqualTo_differentTypes() {
        assertThat(arrayOf("A", 5L)).isNotEqualTo(Any())
    }

    @Test
    fun isNotEqualTo_failEquals() {
        assertFailsWith<AssertionError> {
            assertThat(arrayOf("A", 5L)).isNotEqualTo(arrayOf("A", 5L))
        }
    }

    @Test
    fun isNotEqualTo_failEqualsMultiDimensional() {
        assertFailsWith<AssertionError> {
            assertThat(arrayOf(arrayOf("A"), arrayOf(5L)))
                .isNotEqualTo(arrayOf(arrayOf("A"), arrayOf(5L)))
        }
    }

    @Test
    fun isNotEqualTo_failSame() {
        val same = arrayOf("A", 5L)
        assertFailsWith<AssertionError> {
            assertThat(same).isNotEqualTo(same)
        }
    }

    @Test
    fun isNotEqualTo_failSameMultiDimensional() {
        val same = arrayOf(arrayOf("A"), arrayOf(5L))
        assertFailsWith<AssertionError> {
            assertThat(same).isNotEqualTo(same)
        }
    }

    @Test
    fun stringArrayIsEqualTo() {
        assertThat(arrayOf("A", "B")).isEqualTo(arrayOf("A", "B"))
        assertThat(arrayOf(arrayOf("A"), arrayOf("B")))
            .isEqualTo(arrayOf(arrayOf("A"), arrayOf("B")))
    }

    @Test
    fun stringArrayAsList() {
        assertThat(arrayOf("A", "B")).asList().contains("A")
    }

    @Test
    fun multiDimensionalStringArrayAsList() {
        val ab = arrayOf("A", "B")
        assertThat(arrayOf(ab, arrayOf("C"))).asList().contains(ab)
    }

    @Test
    fun stringArrayIsEqualTo_fail_unequalLength() {
        assertFailsWith<AssertionError> {
            assertThat(arrayOf("A", "B")).isEqualTo(arrayOf("B"))
        }
    }

    @Test
    fun stringArrayIsEqualTo_fail_unequalLengthMultiDimensional() {
        assertFailsWith<AssertionError> {
            assertThat(arrayOf(arrayOf("A"), arrayOf("B")))
                .isEqualTo(arrayOf(arrayOf("A")))
        }
    }

    @Test
    fun stringArrayIsEqualTo_fail_unequalOrdering() {
        assertFailsWith<AssertionError> {
            assertThat(arrayOf("A", "B")).isEqualTo(arrayOf("B", "A"))
        }
    }

    @Test
    fun stringArrayIsEqualTo_fail_unequalOrderingMultiDimensional() {
        assertFailsWith<AssertionError> {
            assertThat(arrayOf(arrayOf("A"), arrayOf("B")))
                .isEqualTo(arrayOf(arrayOf("B"), arrayOf("A")))
        }
    }

    @Test
    fun setArrayIsEqualTo_fail_unequalOrdering() {
        assertFailsWith<AssertionError> {
            assertThat(arrayOf(setOf("A"), setOf("B")))
                .isEqualTo(arrayOf(setOf("B"), setOf("A")))
        }
    }

    @Test
    fun primitiveMultiDimensionalArrayIsEqualTo() {
        assertThat(arrayOf(intArrayOf(1, 2), intArrayOf(3), intArrayOf(4, 5, 6)))
            .isEqualTo(arrayOf(intArrayOf(1, 2), intArrayOf(3), intArrayOf(4, 5, 6)))
    }

    @Test
    fun primitiveMultiDimensionalArrayIsEqualTo_fail_unequalOrdering() {
        assertFailsWith<AssertionError> {
            assertThat(arrayOf(intArrayOf(1, 2), intArrayOf(3), intArrayOf(4, 5, 6)))
                .isEqualTo(arrayOf(intArrayOf(1, 2), intArrayOf(3), intArrayOf(4, 5, 6, 7)))
        }
    }

    @Test
    fun primitiveMultiDimensionalArrayIsNotEqualTo() {
        assertThat(arrayOf(intArrayOf(1, 2), intArrayOf(3), intArrayOf(4, 5, 6)))
            .isNotEqualTo(arrayOf(intArrayOf(1, 2), intArrayOf(3), intArrayOf(4, 5, 6, 7)))
    }

    @Test
    fun primitiveMultiDimensionalArrayIsNotEqualTo_fail_equal() {
        assertFailsWith<AssertionError> {
            assertThat(arrayOf(intArrayOf(1, 2), intArrayOf(3), intArrayOf(4, 5, 6)))
                .isNotEqualTo(arrayOf(intArrayOf(1, 2), intArrayOf(3), intArrayOf(4, 5, 6)))
        }
    }

    @Test
    fun boxedAndUnboxed() {
        assertFailsWith<AssertionError> {
            assertThat(arrayOf(intArrayOf(0))).isEqualTo(arrayOf(arrayOf(0)))
        }
    }
}
