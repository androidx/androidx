/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.serialization.schema

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Tests for [Reserved].
 */
class ReservedTest {
    @Test
    fun testContainsIds() {
        val reserved = Reserved(ids = setOf(1, 2))
        assertThat(1 in reserved).isTrue()
        assertThat(2 in reserved).isTrue()
        assertThat(3 in reserved).isFalse()
    }

    @Test
    fun testContainsNames() {
        val reserved = Reserved(names = setOf("foo", "bar"))
        assertThat("foo" in reserved).isTrue()
        assertThat("bar" in reserved).isTrue()
        assertThat("quux" in reserved).isFalse()
    }

    @Test
    fun testContainsIdRanges() {
        val reserved = Reserved(idRanges = setOf(1..10, 21..40))

        for (i in 1..10) {
            assertThat(i in reserved).isTrue()
        }

        for (i in 11..20) {
            assertThat(i in reserved).isFalse()
        }

        for (i in 21..40) {
            assertThat(i in reserved).isTrue()
        }
    }
}
