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

import androidx.window.core.layout.WindowSizeClass.Companion.BREAKPOINTS_V1
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_MEDIUM_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import kotlin.test.Test
import kotlin.test.assertEquals

class WindowSizeClassSelectorsTest {

    val coreSet = BREAKPOINTS_V1

    @Test
    fun compute_window_size_class_with_floats_truncates() {
        // coreSet does not contain 10, 10
        val intResult =
            coreSet.computeWindowSizeClass(
                WIDTH_DP_MEDIUM_LOWER_BOUND,
                HEIGHT_DP_MEDIUM_LOWER_BOUND
            )
        val floatResult =
            coreSet.computeWindowSizeClass(
                WIDTH_DP_MEDIUM_LOWER_BOUND + .9f,
                HEIGHT_DP_MEDIUM_LOWER_BOUND + .9f
            )

        assertEquals(intResult, floatResult)
    }

    @Test
    fun compute_window_size_class_returns_zero_for_default() {
        // coreSet does not contain 10, 10
        val actual = coreSet.computeWindowSizeClass(10, 10)

        assertEquals(WindowSizeClass(0, 0), actual)
    }

    @Test
    fun compute_window_size_class_returns_exact_match() {
        val expected = WindowSizeClass(WIDTH_DP_MEDIUM_LOWER_BOUND, HEIGHT_DP_MEDIUM_LOWER_BOUND)

        // coreSet contains WindowSizeClass(MEDIUM, MEDIUM)
        val actual =
            coreSet.computeWindowSizeClass(
                WIDTH_DP_MEDIUM_LOWER_BOUND,
                HEIGHT_DP_MEDIUM_LOWER_BOUND
            )

        assertEquals(expected, actual)
    }

    @Test
    fun compute_window_size_class_returns_bounded_match() {
        val expected = WindowSizeClass(WIDTH_DP_MEDIUM_LOWER_BOUND, HEIGHT_DP_MEDIUM_LOWER_BOUND)

        // coreSet contains WindowSizeClass(MEDIUM, MEDIUM)
        val actual =
            coreSet.computeWindowSizeClass(
                WIDTH_DP_MEDIUM_LOWER_BOUND + 1,
                HEIGHT_DP_MEDIUM_LOWER_BOUND + 1
            )

        assertEquals(expected, actual)
    }

    @Test
    fun compute_window_size_class_prefers_width() {
        val expected = WindowSizeClass(minWidthDp = 100, minHeightDp = 50)

        val actual =
            setOf(
                    WindowSizeClass(minWidthDp = 100, minHeightDp = 50),
                    WindowSizeClass(minWidthDp = 50, minHeightDp = 100)
                )
                .computeWindowSizeClass(100, 100)

        assertEquals(expected, actual)
    }

    @Test
    fun compute_window_size_class_breaks_tie_with_height() {
        val expected = WindowSizeClass(minWidthDp = 100, minHeightDp = 100)

        val actual =
            setOf(
                    WindowSizeClass(minWidthDp = 100, minHeightDp = 50),
                    WindowSizeClass(minWidthDp = 100, minHeightDp = 100)
                )
                .computeWindowSizeClass(200, 200)

        assertEquals(expected, actual)
    }

    @Test
    fun compute_window_size_class_preferring_height_returns_zero_for_default() {
        // coreSet does not contain 10, 10
        val actual = coreSet.computeWindowSizeClassPreferHeight(10, 10)

        assertEquals(WindowSizeClass(0, 0), actual)
    }

    @Test
    fun compute_window_size_class_preferring_height_returns_exact_match() {
        val expected = WindowSizeClass(WIDTH_DP_MEDIUM_LOWER_BOUND, HEIGHT_DP_MEDIUM_LOWER_BOUND)

        // coreSet contains WindowSizeClass(MEDIUM, MEDIUM)
        val actual =
            coreSet.computeWindowSizeClassPreferHeight(
                WIDTH_DP_MEDIUM_LOWER_BOUND,
                HEIGHT_DP_MEDIUM_LOWER_BOUND
            )

        assertEquals(expected, actual)
    }

    @Test
    fun compute_window_size_class_preferring_height_returns_bounded_match() {
        val expected = WindowSizeClass(WIDTH_DP_MEDIUM_LOWER_BOUND, HEIGHT_DP_MEDIUM_LOWER_BOUND)

        // coreSet contains WindowSizeClass(MEDIUM, MEDIUM)
        val actual =
            coreSet.computeWindowSizeClassPreferHeight(
                WIDTH_DP_MEDIUM_LOWER_BOUND + 1,
                HEIGHT_DP_MEDIUM_LOWER_BOUND + 1
            )

        assertEquals(expected, actual)
    }

    @Test
    fun compute_window_size_class_preferring_height_prefers_height() {
        val expected = WindowSizeClass(minWidthDp = 50, minHeightDp = 100)

        val actual =
            setOf(
                    WindowSizeClass(minWidthDp = 100, minHeightDp = 50),
                    WindowSizeClass(minWidthDp = 50, minHeightDp = 100)
                )
                .computeWindowSizeClassPreferHeight(100, 100)

        assertEquals(expected, actual)
    }

    @Test
    fun compute_window_size_class_preferring_height_breaks_tie_with_width() {
        val expected = WindowSizeClass(minWidthDp = 100, minHeightDp = 100)

        val actual =
            setOf(
                    WindowSizeClass(minWidthDp = 50, minHeightDp = 100),
                    WindowSizeClass(minWidthDp = 100, minHeightDp = 100)
                )
                .computeWindowSizeClass(200, 200)

        assertEquals(expected, actual)
    }

    @Test
    fun edge_case_matching_bucket_has_min_height_0() {
        val expected = WindowSizeClass(WIDTH_DP_EXPANDED_LOWER_BOUND, 0)
        val actual = BREAKPOINTS_V1.computeWindowSizeClass(1290, 400)

        assertEquals(expected, actual)
    }

    @Test
    fun edge_case_matching_bucket_has_min_width_0() {
        val expected = WindowSizeClass(0, HEIGHT_DP_EXPANDED_LOWER_BOUND)
        val actual = BREAKPOINTS_V1.computeWindowSizeClassPreferHeight(400, 1290)

        assertEquals(expected, actual)
    }
}
