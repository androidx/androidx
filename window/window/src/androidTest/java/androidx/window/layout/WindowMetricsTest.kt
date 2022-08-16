/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.window.layout

import android.graphics.Rect
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.window.core.Bounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [WindowMetrics] class.  */
@SmallTest
@RunWith(AndroidJUnit4::class)
public class WindowMetricsTest {
    @Test
    public fun testGetBounds() {
        val bounds = Rect(1, 2, 3, 4)
        val windowMetrics = WindowMetrics(bounds)
        assertEquals(bounds, windowMetrics.bounds)
    }

    @Test
    public fun testEquals_sameBounds() {
        val bounds = Rect(1, 2, 3, 4)
        val windowMetrics0 = WindowMetrics(bounds)
        val windowMetrics1 = WindowMetrics(bounds)
        assertEquals(windowMetrics0, windowMetrics1)
    }

    @Test
    public fun testEquals_differentBounds() {
        val bounds0 = Rect(1, 2, 3, 4)
        val windowMetrics0 = WindowMetrics(bounds0)
        val bounds1 = Rect(6, 7, 8, 9)
        val windowMetrics1 = WindowMetrics(bounds1)
        assertNotEquals(windowMetrics0, windowMetrics1)
    }

    @Test
    public fun testHashCode_matchesIfEqual() {
        val bounds = Rect(1, 2, 3, 4)
        val windowMetrics0 = WindowMetrics(bounds)
        val windowMetrics1 = WindowMetrics(bounds)
        assertEquals(windowMetrics0.hashCode().toLong(), windowMetrics1.hashCode().toLong())
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @Test
    public fun testSameWindowInsets_emptyInsets() {
        assumePlatformROrAbove()
        val bounds = Bounds(1, 2, 3, 4)
        val windowInsetsCompat = WindowInsetsCompat.Builder().build()
        val windowMetricsA = WindowMetrics(bounds, windowInsetsCompat)
        val windowMetricsB = WindowMetrics(bounds, windowInsetsCompat)
        assertEquals(windowMetricsA, windowMetricsB)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @Test
    public fun testSameWindowInsets_nonEmptyInsets() {
        assumePlatformROrAbove()
        val bounds = Bounds(1, 2, 3, 4)
        val insets = Insets.of(6, 7, 8, 9)
        val builder = WindowInsetsCompat.Builder()
        for (type in WindowMetricsCalculatorCompat.insetsTypeMasks) {
            builder.setInsets(type, insets)
        }
        val windowInsetsCompat = builder.build()
        val windowMetricsA = WindowMetrics(bounds, windowInsetsCompat)
        val windowMetricsB = WindowMetrics(bounds, windowInsetsCompat)
        assertEquals(windowMetricsA, windowMetricsB)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @Test
    public fun testDiffWindowInsets() {
        assumePlatformROrAbove()
        val bounds = Bounds(1, 2, 3, 4)
        val insetsA = Insets.of(1, 2, 3, 4)
        val insetsB = Insets.of(6, 7, 8, 9)
        val builderA = WindowInsetsCompat.Builder()
        val builderB = WindowInsetsCompat.Builder()
        for (type in WindowMetricsCalculatorCompat.insetsTypeMasks) {
            builderA.setInsets(type, insetsA)
            builderB.setInsets(type, insetsB)
        }
        val windowInsetsCompatA = builderA.build()
        val windowInsetsCompatB = builderB.build()
        val windowMetricsA = WindowMetrics(bounds, windowInsetsCompatA)
        val windowMetricsB = WindowMetrics(bounds, windowInsetsCompatB)
        assertNotEquals(windowMetricsA, windowMetricsB)
    }

    private companion object {
        private fun assumePlatformROrAbove() {
            assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        }
    }
}
