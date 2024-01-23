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

import androidx.window.core.ExperimentalWindowCoreApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalWindowCoreApi::class)
class WindowSizeClassUtilTest {

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

    @Test
    fun scoreWindowSizeClassWithinWidthDp_exact_match_has_max_value() {
        val sizeClass = WindowSizeClass(widthDp, heightDp)

        val actual = sizeClass.scoreWithinWidthDp(widthDp)

        assertEquals(Integer.MAX_VALUE, actual)
    }

    @Test
    fun scoreWindowSizeClassWithinWidthDp_too_wide_has_negative_value() {
        val sizeClass = WindowSizeClass(widthDp, heightDp)

        val actual = sizeClass.scoreWithinWidthDp(widthDp / 2)

        assertEquals(-1, actual)
    }

    @Test
    fun scoreWindowSizeClassWithinWidthDp_closer_match_has_larger_value() {
        val narrowSizeClass = WindowSizeClass(widthDp - 10, heightDp)
        val wideSizeClass = WindowSizeClass(widthDp - 5, heightDp)

        val narrowScore = narrowSizeClass.scoreWithinWidthDp(widthDp)
        val widerScore = wideSizeClass.scoreWithinWidthDp(widthDp)

        assertTrue(narrowScore < widerScore)
    }

    @Test
    fun scoreWindowSizeClassWithinHeightDp_exact_match_has_max_value() {
        val sizeClass = WindowSizeClass(widthDp, heightDp)

        val actual = sizeClass.scoreWithinHeightDp(heightDp)

        assertEquals(Integer.MAX_VALUE, actual)
    }

    @Test
    fun scoreWindowSizeClassWithinHeightDp_too_tall_has_negative_value() {
        val sizeClass = WindowSizeClass(widthDp, heightDp)

        val actual = sizeClass.scoreWithinHeightDp(heightDp / 2)

        assertEquals(-1, actual)
    }

    @Test
    fun scoreWindowSizeClassWithinHeightDp_closer_match_has_larger_value() {
        val shortSizeClass = WindowSizeClass(widthDp, heightDp - 10)
        val tallSizeClass = WindowSizeClass(widthDp, heightDp - 5)

        val narrowScore = shortSizeClass.scoreWithinHeightDp(heightDp)
        val widerScore = tallSizeClass.scoreWithinHeightDp(heightDp)

        assertTrue(narrowScore < widerScore)
    }

    @Test
    fun scoreWindowSizeClassAreaWithinBounds_exact_match_has_max_value() {
        val sizeClass = WindowSizeClass(widthDp, heightDp)

        val actual = sizeClass.scoreWithinAreaBounds(widthDp, heightDp)

        assertEquals(Integer.MAX_VALUE, actual)
    }

    @Test
    fun scoreWindowSizeClassAreaWithinBounds_too_large_has_negative_value() {
        val sizeClass = WindowSizeClass(widthDp, heightDp)

        val actual = sizeClass.scoreWithinAreaBounds(widthDp - 1, heightDp)

        assertEquals(-1, actual)
    }

    @Test
    fun scoreWindowSizeClassAreaWithinBounds_closer_match_has_larger_value() {
        val smallSizeClass = WindowSizeClass(widthDp - 10, heightDp - 10)
        val largeSizeClass = WindowSizeClass(widthDp - 5, heightDp - 5)

        val smallScore = smallSizeClass.scoreWithinAreaBounds(widthDp, heightDp)
        val largeScore = largeSizeClass.scoreWithinAreaBounds(widthDp, heightDp)

        assertTrue(smallScore < largeScore, "Expected smallScore < large score," +
            " but was false. smallScore: $smallScore, largeScore: $largeScore")
    }
}
