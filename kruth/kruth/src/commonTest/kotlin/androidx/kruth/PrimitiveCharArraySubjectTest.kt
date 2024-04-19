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

class PrimitiveCharArraySubjectTest {

    @Test
    fun isEqualTo() {
        assertThat(charArrayOf('a', 'q')).isEqualTo(charArrayOf('a', 'q'))
    }

    @Test
    fun isEqualTo_Same() {
        val same = charArrayOf('a', 'q')
        assertThat(same).isEqualTo(same)
    }

    @Test
    fun asList() {
        assertThat(charArrayOf('a', 'q', 'z')).asList().containsAtLeast('a', 'z')
    }

    @Test
    fun isEqualTo_Fail_UnequalOrdering() {
        assertFailsWith<AssertionError> {
            assertThat(charArrayOf('a', 'q')).isEqualTo(charArrayOf('q', 'a'))
        }
    }

    @Test
    fun isEqualTo_Fail_DifferentKindOfcharArrayOf() {
        assertFailsWith<AssertionError> {
            assertThat(charArrayOf('a', 'q')).isEqualTo(intArrayOf())
        }
    }

    @Test
    fun isNotEqualTo_SameLengths() {
        assertThat(charArrayOf('a', 'q')).isNotEqualTo(charArrayOf('q', 'a'))
    }

    @Test
    fun isNotEqualTo_DifferentLengths() {
        assertThat(charArrayOf('a', 'q')).isNotEqualTo(charArrayOf('q', 'a', 'b'))
    }

    @Test
    fun isNotEqualTo_DifferentTypes() {
        assertThat(charArrayOf('a', 'q')).isNotEqualTo(Any())
    }

    @Test
    fun isNotEqualTo_FailEquals() {
        assertFailsWith<AssertionError> {
            assertThat(charArrayOf('a', 'q')).isNotEqualTo(charArrayOf('a', 'q'))
        }
    }

    @Test
    fun isNotEqualTo_FailSame() {
        val same = charArrayOf('a', 'q')
        assertFailsWith<AssertionError> {
            assertThat(same).isNotEqualTo(same)
        }
    }
}
