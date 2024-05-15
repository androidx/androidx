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

class PrimitiveLongArraySubjectTest {

    @Test
    fun isEqualTo() {
        assertThat(longArrayOf(2, 5)).isEqualTo(longArrayOf(2, 5))
    }

    @Test
    fun isEqualTo_Same() {
        val same = longArrayOf(2, 5)
        assertThat(same).isEqualTo(same)
    }

    @Test
    fun asList() {
        assertThat(longArrayOf(5, 2, 9)).asList().containsAtLeast(2L, 9L)
    }

    @Test
    fun isEqualTo_Fail_UnequalOrdering() {
        assertFailsWith<AssertionError> {
            assertThat(longArrayOf(2, 3)).isEqualTo(longArrayOf(3, 2))
        }
    }

    @Test
    fun isEqualTo_Fail_NotAnlongArrayOf() {
        assertFailsWith<AssertionError> {
            assertThat(longArrayOf(2, 3, 4)).isEqualTo(intArrayOf())
        }
    }

    @Test
    fun isNotEqualTo_SameLengths() {
        assertThat(longArrayOf(2, 3)).isNotEqualTo(longArrayOf(3, 2))
    }

    @Test
    fun isNotEqualTo_DifferentLengths() {
        assertThat(longArrayOf(2, 3)).isNotEqualTo(longArrayOf(2, 3, 1))
    }

    @Test
    fun isNotEqualTo_DifferentTypes() {
        assertThat(longArrayOf(2, 3)).isNotEqualTo(Any())
    }

    @Test
    fun isNotEqualTo_FailEquals() {
        assertFailsWith<AssertionError> {
            assertThat(longArrayOf(2, 3)).isNotEqualTo(longArrayOf(2, 3))
        }
    }

    @Test
    fun isNotEqualTo_FailSame() {
        val same = longArrayOf(2, 3)
        assertFailsWith<AssertionError> {
            assertThat(same).isNotEqualTo(same)
        }
    }
}
