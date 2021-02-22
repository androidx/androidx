/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.window;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import android.graphics.Rect;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for {@link WindowMetrics} class. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public final class WindowMetricsTest {

    @Test
    public void testGetBounds() {
        Rect bounds = new Rect(1, 2, 3, 4);
        WindowMetrics windowMetrics = new WindowMetrics(bounds);
        assertEquals(bounds, windowMetrics.getBounds());
    }

    @Test
    public void testEquals_sameBounds() {
        Rect bounds = new Rect(1, 2, 3, 4);
        WindowMetrics windowMetrics0 = new WindowMetrics(bounds);
        WindowMetrics windowMetrics1 = new WindowMetrics(bounds);

        assertEquals(windowMetrics0, windowMetrics1);
    }

    @Test
    public void testEquals_differentBounds() {
        Rect bounds0 = new Rect(1, 2, 3, 4);
        WindowMetrics windowMetrics0 = new WindowMetrics(bounds0);

        Rect bounds1 = new Rect(6, 7, 8, 9);
        WindowMetrics windowMetrics1 = new WindowMetrics(bounds1);

        assertNotEquals(windowMetrics0, windowMetrics1);
    }

    @Test
    public void testHashCode_matchesIfEqual() {
        Rect bounds = new Rect(1, 2, 3, 4);
        WindowMetrics windowMetrics0 = new WindowMetrics(bounds);
        WindowMetrics windowMetrics1 = new WindowMetrics(bounds);

        assertEquals(windowMetrics0.hashCode(), windowMetrics1.hashCode());
    }
}
