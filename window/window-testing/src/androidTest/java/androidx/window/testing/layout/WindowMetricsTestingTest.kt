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

package androidx.window.testing.layout

import android.graphics.Rect
import androidx.window.layout.WindowMetrics
import org.junit.Assert.assertEquals
import org.junit.Test

class WindowMetricsTestingTest {
    private val left = 100
    private val top = 200
    private val right = 300
    private val bottom = 400

    private val bounds = Rect(left, top, right, bottom)
    private val density = 1f

    @Test
    fun testWindowMetrics_bounds_default() {
        val expected = TestWindowMetrics(bounds)
        val actual = WindowMetrics(bounds, density = density)

        assertEquals(expected, actual)
    }

    @Test
    fun testWindowMetrics_bounds_custom() {
        val expected = TestWindowMetrics(bounds, density = density)
        val actual = WindowMetrics(bounds, density = density)

        assertEquals(expected, actual)
    }

    @Test
    fun testWindowMetrics_coordinates_default() {
        val expected = TestWindowMetrics(left, top, right, bottom)
        val actual = WindowMetrics(bounds, density = density)

        assertEquals(expected, actual)
    }

    @Test
    fun testWindowMetrics_coordinates_custom() {
        val expected = TestWindowMetrics(left, top, right, bottom, density = density)
        val actual = WindowMetrics(bounds, density = density)

        assertEquals(expected, actual)
    }
}
