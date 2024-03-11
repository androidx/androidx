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

class PrimitiveShortArraySubjectTest {

    @Test
    fun isEqualTo() {
        assertThat(shortArrayOf(1, 0, 1)).isEqualTo(shortArrayOf(1, 0, 1))
    }

    @Test
    fun isEqualTo_Same() {
        val same = shortArrayOf(1, 0, 1)
        assertThat(same).isEqualTo(same)
    }

    @Test
    fun asList() {
        assertThat(shortArrayOf(1, 1, 0)).asList().containsAtLeast(1.toShort(), 0.toShort())
    }

    @Test
    fun asListWithoutCastingFails() {
        assertFailsWith<AssertionError> {
            assertThat(shortArrayOf(1, 1, 0)).asList().containsAtLeast(1, 0)
        }
    }

    @Test
    fun isEqualTo_Fail_UnequalOrdering() {
        assertFailsWith<AssertionError> {
            assertThat(shortArrayOf(1, 0, 1)).isEqualTo(shortArrayOf(0, 1, 1))
        }
    }

    @Test
    fun isEqualTo_Fail_NotAnshortArrayOf() {
        assertFailsWith<AssertionError> {
            assertThat(shortArrayOf(1, 0, 1)).isEqualTo(Any())
        }
    }

    @Test
    fun isNotEqualTo_SameLengths() {
        assertThat(shortArrayOf(1, 0)).isNotEqualTo(shortArrayOf(1, 1))
    }

    @Test
    fun isNotEqualTo_DifferentLengths() {
        assertThat(shortArrayOf(1, 0)).isNotEqualTo(shortArrayOf(1, 0, 1))
    }

    @Test
    fun isNotEqualTo_DifferentTypes() {
        assertThat(shortArrayOf(1, 0)).isNotEqualTo(Any())
    }

    @Test
    fun isNotEqualTo_FailEquals() {
        assertFailsWith<AssertionError> {
            assertThat(shortArrayOf(1, 0)).isNotEqualTo(shortArrayOf(1, 0))
        }
    }

    @Test
    fun isNotEqualTo_FailSame() {
        val same = shortArrayOf(1, 0)
        assertFailsWith<AssertionError> {
            assertThat(same).isNotEqualTo(same)
        }
    }
}
