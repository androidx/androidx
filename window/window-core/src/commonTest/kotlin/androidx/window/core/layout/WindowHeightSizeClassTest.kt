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

@file:Suppress("DEPRECATION")

package androidx.window.core.layout

import androidx.window.core.layout.WindowHeightSizeClass.Companion.COMPACT
import androidx.window.core.layout.WindowHeightSizeClass.Companion.EXPANDED
import androidx.window.core.layout.WindowHeightSizeClass.Companion.MEDIUM
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WindowHeightSizeClassTest {

    @Test
    fun testComputeForDifferentBuckets() {
        val expected = listOf(COMPACT, MEDIUM, EXPANDED)
        val actual = listOf(479f, 899f, 900f).map { value -> WindowHeightSizeClass.compute(value) }
        assertEquals(expected, actual)
    }

    @Test
    fun testToStringContainsName() {
        val expected =
            listOf("COMPACT", "MEDIUM", "EXPANDED").map { value -> "WindowHeightSizeClass: $value" }
        val actual =
            listOf(479f, 899f, 900f).map { value ->
                WindowHeightSizeClass.compute(value).toString()
            }
        assertEquals(expected, actual)
    }

    @Test
    fun testInvalidSizeBucket() {
        assertFailsWith(IllegalArgumentException::class) { WindowHeightSizeClass.compute(-1f) }
    }
}
