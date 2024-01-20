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

import androidx.window.core.ExperimentalWindowCoreApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Tests for [WindowSizeClass] that verify construction.
 */
class WindowSizeClassTest {

    @Test
    fun testWidthSizeClass_construction() {
        val expected = listOf(
            WindowWidthSizeClass.COMPACT,
            WindowWidthSizeClass.MEDIUM,
            WindowWidthSizeClass.EXPANDED
        )

        val actual = listOf(100, 700, 900).map { width ->
            WindowSizeClass(widthDp = width, heightDp = 100)
        }.map { sizeClass ->
            sizeClass.windowWidthSizeClass
        }

        assertEquals(expected, actual)
    }

    @OptIn(ExperimentalWindowCoreApi::class)
    @Test
    fun testConstruction_usingPx() {
        val expected = WindowSizeClass(600, 600)

        val actual = WindowSizeClass.compute(600, 600, 1f)

        assertEquals(expected, actual)
    }

    @Test
    fun testHeightSizeClass_construction() {
        val expected = listOf(
            WindowHeightSizeClass.COMPACT,
            WindowHeightSizeClass.MEDIUM,
            WindowHeightSizeClass.EXPANDED
        )

        val actual = listOf(100, 500, 900).map { height ->
            WindowSizeClass(widthDp = 100, heightDp = height)
        }.map { sizeClass ->
            sizeClass.windowHeightSizeClass
        }

        assertEquals(expected, actual)
    }

    @Test
    fun testEqualsImpliesHashCode() {
        val first = WindowSizeClass(widthDp = 100, heightDp = 500)
        val second = WindowSizeClass(widthDp = 100, heightDp = 500)

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    @Suppress("DEPRECATION")
    fun truncated_float_does_not_throw() {
        val sizeClass = WindowSizeClass.compute(0.5f, 0.5f)

        val widthSizeClass = sizeClass.windowWidthSizeClass
        val heightSizeClass = sizeClass.windowHeightSizeClass

        assertEquals(WindowWidthSizeClass.COMPACT, widthSizeClass)
        assertEquals(WindowHeightSizeClass.COMPACT, heightSizeClass)
    }

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
        assertFailsWith(IllegalArgumentException::class) {
            WindowSizeClass(-1, 0)
        }
    }

    @Test
    fun negative_height_throws() {
        assertFailsWith(IllegalArgumentException::class) {
            WindowSizeClass(0, -1)
        }
    }
}
