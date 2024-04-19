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

class PrimitiveBooleanArraySubjectTest {

    @Test
    fun isEqualTo() {
        assertThat(booleanArrayOf(true, false, true)).isEqualTo(booleanArrayOf(true, false, true))
    }

    @Test
    fun isEqualTo_Same() {
        val same = booleanArrayOf(true, false, true)
        assertThat(same).isEqualTo(same)
    }

    @Test
    fun asList() {
        assertThat(booleanArrayOf(true, true, false)).asList().containsAtLeast(true, false)
    }

    @Test
    fun isEqualTo_Fail_UnequalOrdering() {
        assertFailsWith<AssertionError> {
            assertThat(booleanArrayOf(true, false, true))
                .isEqualTo(booleanArrayOf(false, true, true))
        }
    }

    @Test
    fun isEqualTo_Fail_NotAnArray() {
        assertFailsWith<AssertionError> {
            assertThat(booleanArrayOf(true, false, true)).isEqualTo(Any())
        }
    }

    @Test
    fun isNotEqualTo_SameLengths() {
        assertThat(booleanArrayOf(true, false)).isNotEqualTo(booleanArrayOf(true, true))
    }

    @Test
    fun isNotEqualTo_DifferentLengths() {
        assertThat(booleanArrayOf(true, false)).isNotEqualTo(booleanArrayOf(true, false, true))
    }

    @Test
    fun isNotEqualTo_DifferentTypes() {
        assertThat(booleanArrayOf(true, false)).isNotEqualTo(Any())
    }

    @Test
    fun isNotEqualTo_FailEquals() {
        assertFailsWith<AssertionError> {
            assertThat(booleanArrayOf(true, false)).isNotEqualTo(booleanArrayOf(true, false))
        }
    }

    @Test
    fun isNotEqualTo_FailSame() {
        val same = booleanArrayOf(true, false)
        assertFailsWith<AssertionError> {
            assertThat(same).isNotEqualTo(same)
        }
    }
}
