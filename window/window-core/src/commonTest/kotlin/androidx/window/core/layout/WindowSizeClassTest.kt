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

package androidx.window.core.layout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Tests for [WindowSizeClass] that verify construction. */
class WindowSizeClassTest {

    @Suppress("DEPRECATION")
    @Test
    fun testWindowWidthSizeClass_compatibility() {
        val expected =
            listOf(
                WindowWidthSizeClass.COMPACT,
                WindowWidthSizeClass.MEDIUM,
                WindowWidthSizeClass.EXPANDED
            )

        val actual =
            listOf(100f, 700f, 900f)
                .map { width -> WindowSizeClass.compute(width, 100f) }
                .map { sizeClass -> sizeClass.windowWidthSizeClass }

        assertEquals(expected, actual)
    }

    @Suppress("DEPRECATION")
    @Test
    fun testWindowSizeClass_computeRounds() {
        val expected = WindowSizeClass.compute(0f, 0f)

        val actual = WindowSizeClass.compute(300f, 300f)

        assertEquals(expected, actual)
    }

    @Suppress("DEPRECATION")
    @Test
    fun testWindowHeightSizeClass_compatibility() {
        val expected =
            listOf(
                WindowHeightSizeClass.COMPACT,
                WindowHeightSizeClass.MEDIUM,
                WindowHeightSizeClass.EXPANDED
            )

        val actual =
            listOf(100f, 500f, 900f)
                .map { height -> WindowSizeClass.compute(100f, height) }
                .map { sizeClass -> sizeClass.windowHeightSizeClass }

        assertEquals(expected, actual)
    }

    @Test
    fun testEqualsImpliesHashCode() {
        val first = WindowSizeClass(100, 500)
        val second = WindowSizeClass(100, 500)

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }

    @Suppress("DEPRECATION")
    @Test
    fun truncated_float_does_not_throw() {
        val sizeClass = WindowSizeClass(0.5f, 0.5f)

        val widthSizeClass = sizeClass.windowWidthSizeClass
        val heightSizeClass = sizeClass.windowHeightSizeClass

        assertEquals(WindowWidthSizeClass.COMPACT, widthSizeClass)
        assertEquals(WindowHeightSizeClass.COMPACT, heightSizeClass)
    }

    @Suppress("DEPRECATION")
    @Test
    fun zero_size_class_does_not_throw() {
        val sizeClass = WindowSizeClass(0, 0)

        val widthSizeClass = sizeClass.windowWidthSizeClass
        val heightSizeClass = sizeClass.windowHeightSizeClass

        assertEquals(WindowWidthSizeClass.COMPACT, widthSizeClass)
        assertEquals(WindowHeightSizeClass.COMPACT, heightSizeClass)
    }

    @Test
    fun negative_width_throws() {
        assertFailsWith(IllegalArgumentException::class) { WindowSizeClass(-1, 0) }
    }

    @Test
    fun negative_height_throws() {
        assertFailsWith(IllegalArgumentException::class) { WindowSizeClass(0, -1) }
    }

    @Test
    fun is_width_at_least_returns_true_when_input_is_greater() {
        val width = 200
        val height = 100
        val sizeClass = WindowSizeClass(width, height)

        assertTrue(sizeClass.isWidthAtLeast(width + 1))
    }

    @Test
    fun is_width_at_least_returns_true_when_input_is_equal() {
        val width = 200
        val height = 100
        val sizeClass = WindowSizeClass(width, height)

        assertTrue(sizeClass.isWidthAtLeast(width))
    }

    @Test
    fun is_width_at_least_returns_false_when_input_is_smaller() {
        val width = 200
        val height = 100
        val sizeClass = WindowSizeClass(width, height)

        assertFalse(sizeClass.isWidthAtLeast(width - 1))
    }

    @Test
    fun is_height_at_least_returns_true_when_input_is_greater() {
        val width = 200
        val height = 100
        val sizeClass = WindowSizeClass(width, height)

        assertTrue(sizeClass.isHeightAtLeast(height + 1))
    }

    @Test
    fun is_height_at_least_returns_true_when_input_is_equal() {
        val width = 200
        val height = 100
        val sizeClass = WindowSizeClass(width, height)

        assertTrue(sizeClass.isHeightAtLeast(height))
    }

    @Test
    fun is_height_at_least_returns_false_when_input_is_smaller() {
        val width = 200
        val height = 100
        val sizeClass = WindowSizeClass(width, height)

        assertFalse(sizeClass.isHeightAtLeast(height - 1))
    }

    @Test
    fun is_at_least_returns_true_when_input_is_greater() {
        val width = 200
        val height = 100
        val sizeClass = WindowSizeClass(width, height)

        assertTrue(sizeClass.isAtLeast(width, height + 1))
        assertTrue(sizeClass.isAtLeast(width + 1, height))
    }

    @Test
    fun is_at_least_returns_true_when_input_is_equal() {
        val width = 200
        val height = 100
        val sizeClass = WindowSizeClass(width, height)

        assertTrue(sizeClass.isAtLeast(width, height))
    }

    @Test
    fun is_at_least_returns_false_when_input_is_smaller() {
        val width = 200
        val height = 100
        val sizeClass = WindowSizeClass(width, height)

        assertFalse(sizeClass.isAtLeast(width, height - 1))
        assertFalse(sizeClass.isAtLeast(width - 1, height))
    }
}
