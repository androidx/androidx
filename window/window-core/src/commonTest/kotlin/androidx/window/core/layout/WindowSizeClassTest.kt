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

import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_MEDIUM_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
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
    fun is_width_at_least_breakpoint_returns_false_when_breakpoint_is_greater() {
        val width = 200
        val height = 100
        val sizeClass = WindowSizeClass(width, height)

        assertFalse(sizeClass.isWidthAtLeastBreakpoint(width + 1))
    }

    @Test
    fun is_width_at_least_breakpoint_returns_true_when_breakpoint_is_equal() {
        val width = 200
        val height = 100
        val sizeClass = WindowSizeClass(width, height)

        assertTrue(sizeClass.isWidthAtLeastBreakpoint(width))
    }

    @Test
    fun is_width_at_least_breakpoint_returns_true_when_breakpoint_is_smaller() {
        val width = 200
        val height = 100
        val sizeClass = WindowSizeClass(width, height)

        assertTrue(sizeClass.isWidthAtLeastBreakpoint(width - 1))
    }

    /**
     * Tests that the width breakpoint logic works as expected. The following sample shows what the
     * dev use site should be
     *
     * WIDTH_DP_MEDIUM_LOWER_BOUND = 600 WIDTH_DP_EXPANDED_LOWER_BOUND = 840
     *
     * fun process(sizeClass: WindowSizeClass) { when {
     * sizeClass.isWidthAtLeast(WIDTH_DP_EXPANDED_LOWER_BOUND) -> doExpanded()
     * sizeClass.isWidthAtLeast(WIDTH_DP_MEDIUM_LOWER_BOUND) -> doMedium() else -> doCompact() } }
     *
     * val belowMediumBreakpoint = WindowSizeClass(minWidthDp = 300, minHeightDp = 0) val
     * equalMediumBreakpoint = WindowSizeClass(minWidthDp = 600, minHeightDp = 0) val
     * expandedBreakpoint = WindowSizeClass(minWidthDp = 840, minHeightDp = 0)
     *
     * process(belowBreakpoint) -> doSomethingCompact() process(equalMediumBreakpoint) ->
     * doSomethingMedium() process(expandedBreakpoint) -> doSomethingExpanded()
     *
     * So the following must be true
     *
     * expandedBreakpoint WindowSizeClass(840, 0).isWidthAtLeast(WIDTH_DP_EXPANDED_LOWER_BOUND) ==
     * true WindowSizeClass(840, 0).isWidthAtLeast(WIDTH_DP_MEDIUM_LOWER_BOUND) == true
     *
     * equalMediumBreakpoint WindowSizeClass(600, 0).isWidthAtLeast(WIDTH_DP_EXPANDED_LOWER_BOUND)
     * == false WindowSizeClass(600, 0).isWidthAtLeast(WIDTH_DP_MEDIUM_LOWER_BOUND) == true
     *
     * belowBreakpoint WindowSizeClass(0, 0).isWidthAtLeast(WIDTH_DP_EXPANDED_LOWER_BOUND) == false
     * WindowSizeClass(0, 0).isWidthAtLeast(WIDTH_DP_MEDIUM_LOWER_BOUND) == false
     */
    @Test
    fun is_width_at_least_bounds_checks() {
        // expandedBreakpoint
        assertTrue(
            WindowSizeClass(WIDTH_DP_EXPANDED_LOWER_BOUND, 0)
                .isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND)
        )
        assertTrue(
            WindowSizeClass(WIDTH_DP_EXPANDED_LOWER_BOUND, 0)
                .isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)
        )

        // equalMediumBreakpoint
        assertFalse(
            WindowSizeClass(WIDTH_DP_MEDIUM_LOWER_BOUND, 0)
                .isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND)
        )
        assertTrue(
            WindowSizeClass(WIDTH_DP_MEDIUM_LOWER_BOUND, 0)
                .isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)
        )

        // belowBreakpoint
        assertFalse(WindowSizeClass(0, 0).isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND))
        assertFalse(WindowSizeClass(0, 0).isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND))
    }

    /**
     * Tests that the width breakpoint logic works as expected. The following sample shows what the
     * dev use site should be
     *
     * HEIGHT_DP_MEDIUM_LOWER_BOUND = 480 HEIGHT_DP_EXPANDED_LOWER_BOUND = 900
     *
     * fun process(sizeClass: WindowSizeClass) { when {
     * sizeClass.isHeightAtLeast(HEIGHT_DP_EXPANDED_LOWER_BOUND) -> doExpanded()
     * sizeClass.isHeightAtLeast(HEIGHT_DP_MEDIUM_LOWER_BOUND) -> doMedium() else -> doCompact() } }
     *
     * val belowMediumBreakpoint = WindowSizeClass(minWidthDp = 0, minHeightDp = 0) val
     * equalMediumBreakpoint = WindowSizeClass(minWidthDp = 0, minHeightDp = 480) val
     * expandedBreakpoint = WindowSizeClass(minWidthDp = 0, minHeightDp = 900)
     *
     * process(belowBreakpoint) -> doSomethingCompact() process(equalMediumBreakpoint) ->
     * doSomethingMedium() process(expandedBreakpoint) -> doSomethingExpanded()
     *
     * So the following must be true
     *
     * expandedBreakpoint WindowSizeClass(0, 900).isWidthAtLeast(HEIGHT_DP_EXPANDED_LOWER_BOUND) ==
     * true WindowSizeClass(0, 900).isWidthAtLeast(HEIGHT_DP_MEDIUM_LOWER_BOUND) == true
     *
     * equalMediumBreakpoint WindowSizeClass(0, 480).isWidthAtLeast(HEIGHT_DP_EXPANDED_LOWER_BOUND)
     * == false WindowSizeClass(0, 480).isWidthAtLeast(HEIGHT_DP_MEDIUM_LOWER_BOUND) == true
     *
     * belowBreakpoint WindowSizeClass(0, 0).isWidthAtLeast(HEIGHT_DP_EXPANDED_LOWER_BOUND) == false
     * WindowSizeClass(0, 0).isWidthAtLeast(HEIGHT_DP_MEDIUM_LOWER_BOUND) == false
     */
    @Test
    fun is_height_at_least_bounds_checks() {
        // expandedBreakpoint
        assertTrue(
            WindowSizeClass(0, HEIGHT_DP_EXPANDED_LOWER_BOUND)
                .isHeightAtLeastBreakpoint(HEIGHT_DP_EXPANDED_LOWER_BOUND)
        )
        assertTrue(
            WindowSizeClass(0, HEIGHT_DP_EXPANDED_LOWER_BOUND)
                .isHeightAtLeastBreakpoint(HEIGHT_DP_MEDIUM_LOWER_BOUND)
        )

        // equalMediumBreakpoint
        assertFalse(
            WindowSizeClass(0, HEIGHT_DP_MEDIUM_LOWER_BOUND)
                .isHeightAtLeastBreakpoint(HEIGHT_DP_EXPANDED_LOWER_BOUND)
        )
        assertTrue(
            WindowSizeClass(0, HEIGHT_DP_MEDIUM_LOWER_BOUND)
                .isHeightAtLeastBreakpoint(HEIGHT_DP_MEDIUM_LOWER_BOUND)
        )

        // belowBreakpoint
        assertFalse(WindowSizeClass(0, 0).isHeightAtLeastBreakpoint(HEIGHT_DP_EXPANDED_LOWER_BOUND))
        assertFalse(WindowSizeClass(0, 0).isHeightAtLeastBreakpoint(HEIGHT_DP_MEDIUM_LOWER_BOUND))
    }

    /**
     * Tests that the width breakpoint logic works as expected. The following sample shows what the
     * dev use site should be
     *
     * DIAGONAL_BOUND_MEDIUM = 600, 600 DIAGONAL_BOUND_EXPANDED = 900, 900
     *
     * fun process(sizeClass: WindowSizeClass) { when { sizeClass.isAtLeast(DIAGONAL_BOUND_EXPANDED,
     * DIAGONAL_BOUND_EXPANDED) -> doExpanded() sizeClass.isAtLeast(DIAGONAL_BOUND_MEDIUM,
     * DIAGONAL_BOUND_MEDIUM) -> doMedium() else -> doCompact() } }
     *
     * val belowMediumBreakpoint = WindowSizeClass(minWidthDp = 0, minHeightDp = 0) val
     * equalMediumBreakpoint = WindowSizeClass(minWidthDp = 600, minHeightDp = 600) val
     * expandedBreakpoint = WindowSizeClass(minWidthDp = 900, minHeightDp = 900)
     *
     * process(belowBreakpoint) -> doSomethingCompact() process(equalMediumBreakpoint) ->
     * doSomethingMedium() process(expandedBreakpoint) -> doSomethingExpanded()
     *
     * So the following must be true
     *
     * expandedBreakpoint WindowSizeClass(900, 900).isWidthAtLeast(WIDTH_DP_EXPANDED_LOWER_BOUND) ==
     * true WindowSizeClass(900, 900).isWidthAtLeast(WIDTH_DP_MEDIUM_LOWER_BOUND) == true
     *
     * equalMediumBreakpoint WindowSizeClass(600, 600).isWidthAtLeast(WIDTH_DP_EXPANDED_LOWER_BOUND)
     * == false WindowSizeClass(600, 600).isWidthAtLeast(WIDTH_DP_MEDIUM_LOWER_BOUND) == true
     *
     * belowBreakpoint WindowSizeClass(0, 0).isWidthAtLeast(WIDTH_DP_EXPANDED_LOWER_BOUND) == false
     * WindowSizeClass(0, 0).isWidthAtLeast(WIDTH_DP_MEDIUM_LOWER_BOUND) == false
     */
    @Test
    fun is_area_at_least_bounds_checks() {
        val diagonalMedium = 600
        val diagonalExpanded = 900
        // expandedBreakpoint
        assertTrue(
            WindowSizeClass(diagonalExpanded, diagonalExpanded)
                .isAtLeastBreakpoint(diagonalExpanded, diagonalExpanded)
        )
        assertTrue(
            WindowSizeClass(diagonalExpanded, diagonalExpanded)
                .isAtLeastBreakpoint(diagonalMedium, diagonalMedium)
        )

        // equalMediumBreakpoint
        assertFalse(
            WindowSizeClass(diagonalMedium, diagonalMedium)
                .isAtLeastBreakpoint(diagonalExpanded, diagonalExpanded)
        )
        assertTrue(
            WindowSizeClass(diagonalMedium, diagonalMedium)
                .isAtLeastBreakpoint(diagonalMedium, diagonalMedium)
        )

        // belowBreakpoint
        assertFalse(WindowSizeClass(0, 0).isAtLeastBreakpoint(diagonalExpanded, diagonalExpanded))
        assertFalse(WindowSizeClass(0, 0).isAtLeastBreakpoint(diagonalMedium, diagonalMedium))
    }

    @Test
    fun is_height_at_least_breakpoint_returns_false_when_breakpoint_is_greater() {
        val width = 200
        val height = 100
        val sizeClass = WindowSizeClass(width, height)

        assertFalse(sizeClass.isHeightAtLeastBreakpoint(height + 1))
    }

    @Test
    fun is_height_at_least_breakpoint_returns_true_when_breakpoint_is_equal() {
        val width = 200
        val height = 100
        val sizeClass = WindowSizeClass(width, height)

        assertTrue(sizeClass.isHeightAtLeastBreakpoint(height))
    }

    @Test
    fun is_height_at_least_breakpoint_returns_true_when_breakpoint_is_smaller() {
        val width = 200
        val height = 100
        val sizeClass = WindowSizeClass(width, height)

        assertTrue(sizeClass.isHeightAtLeastBreakpoint(height - 1))
    }

    @Test
    fun is_at_least_breakpoint_returns_false_when_breakpoint_is_greater() {
        val width = 200
        val height = 100
        val sizeClass = WindowSizeClass(width, height)

        assertFalse(sizeClass.isAtLeastBreakpoint(width, height + 1))
        assertFalse(sizeClass.isAtLeastBreakpoint(width + 1, height))
    }

    @Test
    fun is_at_least_breakpoint_returns_true_when_breakpoint_is_equal() {
        val width = 200
        val height = 100
        val sizeClass = WindowSizeClass(width, height)

        assertTrue(sizeClass.isAtLeastBreakpoint(width, height))
    }

    @Test
    fun is_at_least_breakpoint_returns_true_when_breakpoint_is_smaller() {
        val width = 200
        val height = 100
        val sizeClass = WindowSizeClass(width, height)

        assertTrue(sizeClass.isAtLeastBreakpoint(width, height - 1))
        assertTrue(sizeClass.isAtLeastBreakpoint(width - 1, height))
    }
}
