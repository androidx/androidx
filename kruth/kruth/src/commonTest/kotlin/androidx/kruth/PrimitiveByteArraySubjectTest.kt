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

class PrimitiveByteArraySubjectTest {

    @Test
    fun isEqualTo() {
        assertThat(byteArrayOf(0, 1)).isEqualTo(byteArrayOf(0, 1))
    }

    @Test
    fun isEqualTo_Same() {
        val same = byteArrayOf(0, 1)
        assertThat(same).isEqualTo(same)
    }

    @Test
    fun asList() {
        assertThat(byteArrayOf(0, 1, 2)).asList().containsAtLeast(0.toByte(), 2.toByte())
    }

    @Test
    fun isEqualTo_Fail_UnequalOrdering() {
        assertFailsWith<AssertionError> {
            assertThat(byteArrayOf(0, 123)).isEqualTo(byteArrayOf(123, 0))
        }
    }

    @Test
    fun isEqualTo_Fail_NotAnArray() {
        assertFailsWith<AssertionError> {
            assertThat(byteArrayOf(0, 1)).isEqualTo(intArrayOf())
        }
    }

    @Test
    fun isNotEqualTo_SameLengths() {
        assertThat(byteArrayOf(0, 1)).isNotEqualTo(byteArrayOf(1, 0))
    }

    @Test
    fun isNotEqualTo_DifferentLengths() {
        assertThat(byteArrayOf(0, 1)).isNotEqualTo(byteArrayOf(1, 0, 2))
    }

    @Test
    fun isNotEqualTo_DifferentTypes() {
        assertThat(byteArrayOf(0, 1)).isNotEqualTo(Any())
    }

    @Test
    fun isNotEqualTo_FailEquals() {
        assertFailsWith<AssertionError> {
            assertThat(byteArrayOf(0, 1)).isNotEqualTo(byteArrayOf(0, 1))
        }
    }

    @Test
    fun isNotEqualTo_FailSame() {
        val same = byteArrayOf(0, 1)
        assertFailsWith<AssertionError> {
            assertThat(same).isNotEqualTo(same)
        }
    }
}
