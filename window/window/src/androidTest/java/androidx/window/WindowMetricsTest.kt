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
package androidx.window

import android.graphics.Rect
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
}
