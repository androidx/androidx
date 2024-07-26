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

package androidx.compose.material3.windowsizeclass

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WindowSizeClassTest {
    @Test
    fun calculateWidthSizeClass_forNegativeWidth_throws() {
        assertFailsWith(IllegalArgumentException::class) {
            WindowWidthSizeClass.fromWidth((-10).dp, WindowWidthSizeClass.DefaultSizeClasses)
        }
    }

    @Test
    fun calculateHeightSizeClass_forNegativeHeight_throws() {
        assertFailsWith(IllegalArgumentException::class) {
            WindowHeightSizeClass.fromHeight((-10).dp, WindowHeightSizeClass.DefaultSizeClasses)
        }
    }

    @Test
    fun calculateWidthSizeClass_noSupportedSizeClass_throws() {
        assertFailsWith(IllegalArgumentException::class) {
            WindowWidthSizeClass.fromWidth(10.dp, emptySet())
        }
    }

    @Test
    fun calculateHeightSizeClass_noSupportedSizeClass_throws() {
        assertFailsWith(IllegalArgumentException::class) {
            WindowHeightSizeClass.fromHeight(10.dp, emptySet())
        }
    }

    @Test
    fun calculateWidthSizeClass() {
        assertWidthClass(WindowWidthSizeClass.Compact, 0.dp)
        assertWidthClass(WindowWidthSizeClass.Compact, 200.dp)

        assertWidthClass(WindowWidthSizeClass.Medium, 600.dp)
        assertWidthClass(WindowWidthSizeClass.Medium, 700.dp)

        assertWidthClass(WindowWidthSizeClass.Expanded, 840.dp)
        assertWidthClass(WindowWidthSizeClass.Expanded, 1000.dp)
    }

    @Test
    fun calculateHeightSizeClass() {
        assertHeightClass(WindowHeightSizeClass.Compact, 0.dp)
        assertHeightClass(WindowHeightSizeClass.Compact, 200.dp)

        assertHeightClass(WindowHeightSizeClass.Medium, 480.dp)
        assertHeightClass(WindowHeightSizeClass.Medium, 700.dp)

        assertHeightClass(WindowHeightSizeClass.Expanded, 900.dp)
        assertHeightClass(WindowHeightSizeClass.Expanded, 1000.dp)
    }

    @Test
    fun calculateWidthSizeClass_useBestMatchedSupportedSizeClasses() {
        assertWidthClass(
            WindowWidthSizeClass.Compact,
            700.dp,
            supportedSizeClasses =
                setOf(WindowWidthSizeClass.Compact, WindowWidthSizeClass.Expanded)
        )

        assertWidthClass(
            WindowWidthSizeClass.Medium,
            1000.dp,
            supportedSizeClasses = setOf(WindowWidthSizeClass.Compact, WindowWidthSizeClass.Medium)
        )
    }

    @Test
    fun calculateHeightSizeClass_useBestMatchedSupportedSizeClasses() {
        assertHeightClass(
            WindowHeightSizeClass.Compact,
            700.dp,
            supportedSizeClasses =
                setOf(WindowHeightSizeClass.Compact, WindowHeightSizeClass.Expanded)
        )

        assertHeightClass(
            WindowHeightSizeClass.Medium,
            1000.dp,
            supportedSizeClasses =
                setOf(WindowHeightSizeClass.Compact, WindowHeightSizeClass.Medium)
        )
    }

    @Test
    fun calculateWidthSizeClass_fallbackToTheSmallestSizeClasses() {
        assertWidthClass(
            WindowWidthSizeClass.Medium,
            200.dp,
            supportedSizeClasses = setOf(WindowWidthSizeClass.Medium, WindowWidthSizeClass.Expanded)
        )
    }

    @Test
    fun calculateHeightSizeClass_fallbackToTheSmallestSizeClasses() {
        assertHeightClass(
            WindowHeightSizeClass.Medium,
            200.dp,
            supportedSizeClasses =
                setOf(WindowHeightSizeClass.Medium, WindowHeightSizeClass.Expanded)
        )
    }

    @Test
    fun widthSizeClassToString() {
        assertEquals(WindowWidthSizeClass.Compact.toString(), "WindowWidthSizeClass.Compact")
        assertEquals(WindowWidthSizeClass.Medium.toString(), "WindowWidthSizeClass.Medium")
        assertEquals(WindowWidthSizeClass.Expanded.toString(), "WindowWidthSizeClass.Expanded")
    }

    @Test
    fun heightSizeClassToString() {
        assertEquals(WindowHeightSizeClass.Compact.toString(), "WindowHeightSizeClass.Compact")
        assertEquals(WindowHeightSizeClass.Medium.toString(), "WindowHeightSizeClass.Medium")
        assertEquals(WindowHeightSizeClass.Expanded.toString(), "WindowHeightSizeClass.Expanded")
    }

    @Test
    fun widthSizeClassCompareTo() {
        // Less than
        assertTrue(WindowWidthSizeClass.Compact < WindowWidthSizeClass.Medium)
        assertTrue(WindowWidthSizeClass.Compact < WindowWidthSizeClass.Expanded)
        assertTrue(WindowWidthSizeClass.Medium < WindowWidthSizeClass.Expanded)

        assertFalse(WindowWidthSizeClass.Compact < WindowWidthSizeClass.Compact)
        assertFalse(WindowWidthSizeClass.Medium < WindowWidthSizeClass.Medium)
        assertFalse(WindowWidthSizeClass.Expanded < WindowWidthSizeClass.Expanded)

        assertFalse(WindowWidthSizeClass.Expanded < WindowWidthSizeClass.Medium)
        assertFalse(WindowWidthSizeClass.Expanded < WindowWidthSizeClass.Compact)
        assertFalse(WindowWidthSizeClass.Medium < WindowWidthSizeClass.Compact)

        // Less than or equal to
        assertTrue(WindowWidthSizeClass.Compact <= WindowWidthSizeClass.Compact)
        assertTrue(WindowWidthSizeClass.Compact <= WindowWidthSizeClass.Medium)
        assertTrue(WindowWidthSizeClass.Compact <= WindowWidthSizeClass.Expanded)
        assertTrue(WindowWidthSizeClass.Medium <= WindowWidthSizeClass.Medium)
        assertTrue(WindowWidthSizeClass.Medium <= WindowWidthSizeClass.Expanded)
        assertTrue(WindowWidthSizeClass.Expanded <= WindowWidthSizeClass.Expanded)

        assertFalse(WindowWidthSizeClass.Expanded <= WindowWidthSizeClass.Medium)
        assertFalse(WindowWidthSizeClass.Expanded <= WindowWidthSizeClass.Compact)
        assertFalse(WindowWidthSizeClass.Medium <= WindowWidthSizeClass.Compact)

        // Greater than
        assertTrue(WindowWidthSizeClass.Expanded > WindowWidthSizeClass.Medium)
        assertTrue(WindowWidthSizeClass.Expanded > WindowWidthSizeClass.Compact)
        assertTrue(WindowWidthSizeClass.Medium > WindowWidthSizeClass.Compact)

        assertFalse(WindowWidthSizeClass.Expanded > WindowWidthSizeClass.Expanded)
        assertFalse(WindowWidthSizeClass.Medium > WindowWidthSizeClass.Medium)
        assertFalse(WindowWidthSizeClass.Compact > WindowWidthSizeClass.Compact)

        assertFalse(WindowWidthSizeClass.Compact > WindowWidthSizeClass.Medium)
        assertFalse(WindowWidthSizeClass.Compact > WindowWidthSizeClass.Expanded)
        assertFalse(WindowWidthSizeClass.Medium > WindowWidthSizeClass.Expanded)

        // Greater than or equal to
        assertTrue(WindowWidthSizeClass.Expanded >= WindowWidthSizeClass.Expanded)
        assertTrue(WindowWidthSizeClass.Expanded >= WindowWidthSizeClass.Medium)
        assertTrue(WindowWidthSizeClass.Expanded >= WindowWidthSizeClass.Compact)
        assertTrue(WindowWidthSizeClass.Medium >= WindowWidthSizeClass.Medium)
        assertTrue(WindowWidthSizeClass.Medium >= WindowWidthSizeClass.Compact)
        assertTrue(WindowWidthSizeClass.Compact >= WindowWidthSizeClass.Compact)

        assertFalse(WindowWidthSizeClass.Compact >= WindowWidthSizeClass.Medium)
        assertFalse(WindowWidthSizeClass.Compact >= WindowWidthSizeClass.Expanded)
        assertFalse(WindowWidthSizeClass.Medium >= WindowWidthSizeClass.Expanded)
    }

    @Test
    fun heightSizeClassCompareTo() {
        // Less than
        assertTrue(WindowHeightSizeClass.Compact < WindowHeightSizeClass.Medium)
        assertTrue(WindowHeightSizeClass.Compact < WindowHeightSizeClass.Expanded)
        assertTrue(WindowHeightSizeClass.Medium < WindowHeightSizeClass.Expanded)

        assertFalse(WindowHeightSizeClass.Compact < WindowHeightSizeClass.Compact)
        assertFalse(WindowHeightSizeClass.Medium < WindowHeightSizeClass.Medium)
        assertFalse(WindowHeightSizeClass.Expanded < WindowHeightSizeClass.Expanded)

        assertFalse(WindowHeightSizeClass.Expanded < WindowHeightSizeClass.Medium)
        assertFalse(WindowHeightSizeClass.Expanded < WindowHeightSizeClass.Compact)
        assertFalse(WindowHeightSizeClass.Medium < WindowHeightSizeClass.Compact)

        // Less than or equal to
        assertTrue(WindowHeightSizeClass.Compact <= WindowHeightSizeClass.Compact)
        assertTrue(WindowHeightSizeClass.Compact <= WindowHeightSizeClass.Medium)
        assertTrue(WindowHeightSizeClass.Compact <= WindowHeightSizeClass.Expanded)
        assertTrue(WindowHeightSizeClass.Medium <= WindowHeightSizeClass.Medium)
        assertTrue(WindowHeightSizeClass.Medium <= WindowHeightSizeClass.Expanded)
        assertTrue(WindowHeightSizeClass.Expanded <= WindowHeightSizeClass.Expanded)

        assertFalse(WindowHeightSizeClass.Expanded <= WindowHeightSizeClass.Medium)
        assertFalse(WindowHeightSizeClass.Expanded <= WindowHeightSizeClass.Compact)
        assertFalse(WindowHeightSizeClass.Medium <= WindowHeightSizeClass.Compact)

        // Greater than
        assertTrue(WindowHeightSizeClass.Expanded > WindowHeightSizeClass.Medium)
        assertTrue(WindowHeightSizeClass.Expanded > WindowHeightSizeClass.Compact)
        assertTrue(WindowHeightSizeClass.Medium > WindowHeightSizeClass.Compact)

        assertFalse(WindowHeightSizeClass.Expanded > WindowHeightSizeClass.Expanded)
        assertFalse(WindowHeightSizeClass.Medium > WindowHeightSizeClass.Medium)
        assertFalse(WindowHeightSizeClass.Compact > WindowHeightSizeClass.Compact)

        assertFalse(WindowHeightSizeClass.Compact > WindowHeightSizeClass.Medium)
        assertFalse(WindowHeightSizeClass.Compact > WindowHeightSizeClass.Expanded)
        assertFalse(WindowHeightSizeClass.Medium > WindowHeightSizeClass.Expanded)

        // Greater than or equal to
        assertTrue(WindowHeightSizeClass.Expanded >= WindowHeightSizeClass.Expanded)
        assertTrue(WindowHeightSizeClass.Expanded >= WindowHeightSizeClass.Medium)
        assertTrue(WindowHeightSizeClass.Expanded >= WindowHeightSizeClass.Compact)
        assertTrue(WindowHeightSizeClass.Medium >= WindowHeightSizeClass.Medium)
        assertTrue(WindowHeightSizeClass.Medium >= WindowHeightSizeClass.Compact)
        assertTrue(WindowHeightSizeClass.Compact >= WindowHeightSizeClass.Compact)

        assertFalse(WindowHeightSizeClass.Compact >= WindowHeightSizeClass.Medium)
        assertFalse(WindowHeightSizeClass.Compact >= WindowHeightSizeClass.Expanded)
        assertFalse(WindowHeightSizeClass.Medium >= WindowHeightSizeClass.Expanded)
    }

    private fun assertWidthClass(
        expectedSizeClass: WindowWidthSizeClass,
        width: Dp,
        supportedSizeClasses: Set<WindowWidthSizeClass> = WindowWidthSizeClass.DefaultSizeClasses
    ) {
        assertEquals(WindowWidthSizeClass.fromWidth(width, supportedSizeClasses), expectedSizeClass)
    }

    private fun assertHeightClass(
        expectedSizeClass: WindowHeightSizeClass,
        height: Dp,
        supportedSizeClasses: Set<WindowHeightSizeClass> = WindowHeightSizeClass.DefaultSizeClasses
    ) {
        assertEquals(WindowHeightSizeClass.fromHeight(height, supportedSizeClasses), expectedSizeClass)
    }
}
