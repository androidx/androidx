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

        val actual = listOf(100f, 700f, 900f).map { width ->
            WindowSizeClass.compute(width, 100f)
        }.map { sizeClass ->
            sizeClass.windowWidthSizeClass
        }

        assertEquals(expected, actual)
    }

    @Test
    fun testWindowSizeClass_computeRounds() {
        val expected = WindowSizeClass.compute(0f, 0f)

        val actual = WindowSizeClass.compute(300f, 300f)

        assertEquals(expected, actual)
    }

    @OptIn(ExperimentalWindowCoreApi::class)
    @Test
    fun testConstruction_usingPx() {
        val expected = WindowSizeClass.compute(600f, 600f)

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

        val actual = listOf(100f, 500f, 900f).map { height ->
            WindowSizeClass.compute(100f, height)
        }.map { sizeClass ->
            sizeClass.windowHeightSizeClass
        }

        assertEquals(expected, actual)
    }

    @Test
    fun testEqualsImpliesHashCode() {
        val first = WindowSizeClass.compute(100f, 500f)
        val second = WindowSizeClass.compute(100f, 500f)

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun truncated_float_does_not_throw() {
        val sizeClass = WindowSizeClass.compute(0.5f, 0.5f)

        val widthSizeClass = sizeClass.windowWidthSizeClass
        val heightSizeClass = sizeClass.windowHeightSizeClass

        assertEquals(WindowWidthSizeClass.COMPACT, widthSizeClass)
        assertEquals(WindowHeightSizeClass.COMPACT, heightSizeClass)
    }

    @Test
    fun zero_size_class_does_not_throw() {
        val sizeClass = WindowSizeClass.compute(0f, 0f)

        val widthSizeClass = sizeClass.windowWidthSizeClass
        val heightSizeClass = sizeClass.windowHeightSizeClass

        assertEquals(WindowWidthSizeClass.COMPACT, widthSizeClass)
        assertEquals(WindowHeightSizeClass.COMPACT, heightSizeClass)
    }

    @Test
    fun negative_width_throws() {
        assertFailsWith(IllegalArgumentException::class) {
            WindowSizeClass.compute(-1f, 0f)
        }
    }

    @Test
    fun negative_height_throws() {
        assertFailsWith(IllegalArgumentException::class) {
            WindowSizeClass.compute(0f, -1f)
        }
    }
}
