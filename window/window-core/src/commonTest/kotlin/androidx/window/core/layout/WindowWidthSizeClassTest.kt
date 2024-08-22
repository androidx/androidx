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

import androidx.window.core.layout.WindowWidthSizeClass.Companion.COMPACT
import androidx.window.core.layout.WindowWidthSizeClass.Companion.EXPANDED
import androidx.window.core.layout.WindowWidthSizeClass.Companion.MEDIUM
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WindowWidthSizeClassTest {

    @Test
    fun testComputeForDifferentBuckets() {
        val expected = listOf(COMPACT, MEDIUM, EXPANDED)
        val actual = listOf(599f, 839f, 840f).map { value -> WindowWidthSizeClass.compute(value) }
        assertEquals(expected, actual)
    }

    @Test
    fun testToStringContainsName() {
        val expected =
            listOf("COMPACT", "MEDIUM", "EXPANDED").map { value -> "WindowWidthSizeClass: $value" }
        val actual =
            listOf(599f, 839f, 840f).map { value -> WindowWidthSizeClass.compute(value).toString() }
        assertEquals(expected, actual)
    }

    @Test
    fun testInvalidSizeBucket() {
        assertFailsWith(IllegalArgumentException::class) { WindowWidthSizeClass.compute(-1f) }
    }
}
