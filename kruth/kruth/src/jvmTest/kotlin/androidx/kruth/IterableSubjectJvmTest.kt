/*
 * Copyright 2022 The Android Open Source Project
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

class IterableSubjectJvmTest {

    // The test fails on native, see https://youtrack.jetbrains.com/issue/KT-56089
    @Test
    fun isInStrictOrderWithNonComparableElementsFailure() {
        assertFailsWith<ClassCastException> {
            assertThat(listOf(1 as Any, "2", 3, "4")).isInStrictOrder()
        }
    }

    // The test fails on native, see https://youtrack.jetbrains.com/issue/KT-56089
    @Test
    fun isInOrderWithNonComparableElementsFailure() {
        assertFailsWith<ClassCastException> {
            assertThat(listOf(1 as Any, "2", 2, "3")).isInOrder()
        }
    }
}
