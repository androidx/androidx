/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.room.compiler.processing.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ParameterizedHelperTest {
    @Test
    fun testEnumerations() {
        assertThat(generateAllEnumerations()).isEmpty()

        // Comparing List of Arrays doesn't work(https://github.com/google/truth/issues/928), so
        // we're mapping it to List of Lists
        assertThat(
            generateAllEnumerations(
                listOf(false, true)
            ).map { it.toList() }).isEqualTo(
            listOf(
                listOf<Any>(false), listOf<Any>(true)
            )
        )
        assertThat(generateAllEnumerations(listOf(false, true), listOf())).isEmpty()
        assertThat(
            generateAllEnumerations(
                listOf(false, true),
                listOf(false, true)
            ).map { it.toList() }
        ).isEqualTo(
            listOf(
                listOf(false, false), listOf(false, true),
                listOf(true, false), listOf(true, true)
            )
        )
        assertThat(
            generateAllEnumerations(
                listOf(false, true),
                (0..2).toList(),
                listOf("low", "hi")
            ).map { it.toList() }
        ).isEqualTo(
            listOf(
                listOf(false, 0, "low"),
                listOf(false, 0, "hi"),
                listOf(false, 1, "low"),
                listOf(false, 1, "hi"),
                listOf(false, 2, "low"),
                listOf(false, 2, "hi"),
                listOf(true, 0, "low"),
                listOf(true, 0, "hi"),
                listOf(true, 1, "low"),
                listOf(true, 1, "hi"),
                listOf(true, 2, "low"),
                listOf(true, 2, "hi")
            )
        )
    }
}
