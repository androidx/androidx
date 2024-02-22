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
package androidx.window.core.layout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class WindowSizeClassSelectorsTest {

    private val widthDp = 100
    private val heightDp = 200

    @Test
    fun widestOrEqualWidthDp_return_null_if_no_width_match() {
        val sizeClass = WindowSizeClass(widthDp, heightDp)

        val actual = setOf(sizeClass).widestOrEqualWidthDp(0, heightDp)

        assertNull(actual)
    }

    @Test
    fun widestOrEqualWidthDp_return_widest_match() {
        val smallSizeClass = WindowSizeClass(widthDp / 2, heightDp)
        val mediumSizeClass = WindowSizeClass(widthDp, heightDp)
        val largeSizeClass = WindowSizeClass(widthDp * 2, heightDp)

        val actual = setOf(smallSizeClass, mediumSizeClass, largeSizeClass)
            .widestOrEqualWidthDp(widthDp + 1, heightDp)

        assertEquals(mediumSizeClass, actual)
    }

    @Test
    fun widestOrEqualWidthDp_return_exact_match() {
        val smallSizeClass = WindowSizeClass(widthDp / 2, heightDp)
        val mediumSizeClass = WindowSizeClass(widthDp, heightDp)
        val largeSizeClass = WindowSizeClass(widthDp * 2, heightDp)

        val actual = setOf(smallSizeClass, mediumSizeClass, largeSizeClass)
            .widestOrEqualWidthDp(widthDp, heightDp)

        assertEquals(mediumSizeClass, actual)
    }

    @Test
    fun widestOrEqualWidthDp_multiple_matches_return_width() {
        val smallSizeClass = WindowSizeClass(widthDp, heightDp / 2)
        val mediumSizeClass = WindowSizeClass(widthDp, heightDp)
        val largeSizeClass = WindowSizeClass(widthDp, heightDp * 2)

        val actual = setOf(smallSizeClass, mediumSizeClass, largeSizeClass)
            .widestOrEqualWidthDp(widthDp, heightDp * 3)

        assertEquals(largeSizeClass, actual)
    }

    @Test
    fun widestOrEqualWidth_throws_on_negative_height() {
        assertFailsWith(IllegalArgumentException::class) {
            val sizeClass = WindowSizeClass(widthDp, heightDp)

            setOf(sizeClass).widestOrEqualWidthDp(0, -1)
        }
    }

    @Test
    fun widestOrEqualWidth_throws_on_negative_width() {
        assertFailsWith(IllegalArgumentException::class) {
            val sizeClass = WindowSizeClass(widthDp, heightDp)

            setOf(sizeClass).widestOrEqualWidthDp(-1, 0)
        }
    }

    @Test
    fun widestOrEqualWidthDp_return_null_if_no_height_match() {
        val sizeClass = WindowSizeClass(widthDp, heightDp)

        val actual = setOf(sizeClass).widestOrEqualWidthDp(widthDp, heightDp - 1)

        assertNull(actual)
    }

    @Test
    fun widestOrEqualWidthDp_return_value_if_has_exact_height_match() {
        val sizeClass = WindowSizeClass(widthDp, heightDp)

        val actual = setOf(sizeClass).widestOrEqualWidthDp(widthDp, heightDp)

        assertEquals(sizeClass, actual)
    }
}
