/*
 * Copyright 2024 The Android Open Source Project
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

import com.google.common.collect.ImmutableMultiset
import kotlin.test.Test
import kotlin.test.assertFailsWith

class MultisetSubjectTest {

    @Test
    fun hasCount() {
        val multiset = ImmutableMultiset.of("kurt", "kurt", "kluever")
        assertThat(multiset).hasCount("kurt", 2)
        assertThat(multiset).hasCount("kluever", 1)
        assertThat(multiset).hasCount("alfred", 0)
        assertWithMessage("name").that(multiset).hasCount("kurt", 2)
    }

    @Test
    fun hasCountFail() {
        val multiset = ImmutableMultiset.of("kurt", "kurt", "kluever")
        assertFailsWith<AssertionError> {
            assertThat(multiset).hasCount("kurt", 3)
        }
    }
}
