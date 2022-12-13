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

package androidx.core.view;

import static android.view.MotionEvent.AXIS_BRAKE;
import static android.view.MotionEvent.AXIS_X;
import static android.view.MotionEvent.AXIS_Y;

import static androidx.core.view.MotionEventCompat.AXIS_SCROLL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import android.os.Build;
import android.view.MotionEvent;
import android.view.VelocityTracker;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class VelocityTrackerCompatTest {
    @Mock private VelocityTracker mTracker;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testIsAxisSupported_planarAxes() {
        assertTrue(VelocityTrackerCompat.isAxisSupported(mTracker, AXIS_X));
        assertTrue(VelocityTrackerCompat.isAxisSupported(mTracker, AXIS_Y));
    }

    @Test
    public void testIsAxisSupported_nonPlanarAxes() {
        if (Build.VERSION.SDK_INT >= 34) {
            when(mTracker.isAxisSupported(MotionEvent.AXIS_SCROLL)).thenReturn(true);

            assertTrue(VelocityTrackerCompat.isAxisSupported(mTracker, AXIS_SCROLL));
        } else {
            assertFalse(
                    VelocityTrackerCompat.isAxisSupported(VelocityTracker.obtain(), AXIS_SCROLL));
        }

        // Check against an axis that has not yet been supported at any Android version.
        assertFalse(VelocityTrackerCompat.isAxisSupported(VelocityTracker.obtain(), AXIS_BRAKE));
    }

    @Test
    public void testGetAxisVelocity() {
        if (Build.VERSION.SDK_INT >= 34) {
            when(mTracker.getAxisVelocity(AXIS_X)).thenReturn(1f);
            when(mTracker.getAxisVelocity(AXIS_Y)).thenReturn(2f);
            when(mTracker.getAxisVelocity(AXIS_SCROLL)).thenReturn(3f);

            assertEquals(1f, VelocityTrackerCompat.getAxisVelocity(mTracker, AXIS_X), 0);
            assertEquals(2f, VelocityTrackerCompat.getAxisVelocity(mTracker, AXIS_Y), 0);
            assertEquals(3f, VelocityTrackerCompat.getAxisVelocity(mTracker, AXIS_SCROLL), 0);
        } else {
            when(mTracker.getXVelocity()).thenReturn(2f);
            when(mTracker.getYVelocity()).thenReturn(3f);

            assertEquals(2f, VelocityTrackerCompat.getAxisVelocity(mTracker, AXIS_X), 0);
            assertEquals(3f, VelocityTrackerCompat.getAxisVelocity(mTracker, AXIS_Y), 0);
            // AXIS_SCROLL not supported before API 34.
            assertEquals(0f, VelocityTrackerCompat.getAxisVelocity(mTracker, AXIS_SCROLL), 0);
        }

        // Check against an axis that has not yet been supported at any Android version.
        assertEquals(0f, VelocityTrackerCompat.getAxisVelocity(mTracker, AXIS_BRAKE), 0);
    }

    @Test
    public void testGetAxisVelocity_withPointerId() {
        if (Build.VERSION.SDK_INT >= 34) {
            when(mTracker.getAxisVelocity(AXIS_X, 4)).thenReturn(1f);
            when(mTracker.getAxisVelocity(AXIS_Y, 5)).thenReturn(2f);
            when(mTracker.getAxisVelocity(AXIS_SCROLL, 1)).thenReturn(3f);

            assertEquals(4f, VelocityTrackerCompat.getAxisVelocity(mTracker, AXIS_X, 4), 0);
            assertEquals(5f, VelocityTrackerCompat.getAxisVelocity(mTracker, AXIS_Y, 5), 0);
            assertEquals(3f, VelocityTrackerCompat.getAxisVelocity(mTracker, AXIS_SCROLL, 1), 0);
            // Test with pointer IDs with no velocity.
            assertEquals(0f, VelocityTrackerCompat.getAxisVelocity(mTracker, AXIS_X, 2), 0);
            assertEquals(0f, VelocityTrackerCompat.getAxisVelocity(mTracker, AXIS_Y, 2), 0);
            assertEquals(0f, VelocityTrackerCompat.getAxisVelocity(mTracker, AXIS_SCROLL, 2), 0);
        } else {
            when(mTracker.getXVelocity(2)).thenReturn(2f);
            when(mTracker.getYVelocity(3)).thenReturn(3f);

            // Test with pointer IDs with no velocity.
            assertEquals(2f, VelocityTrackerCompat.getAxisVelocity(mTracker, AXIS_X, 2), 0);
            assertEquals(3f, VelocityTrackerCompat.getAxisVelocity(mTracker, AXIS_Y, 3), 0);
            // AXIS_SCROLL not supported before API 34.
            assertEquals(0f, VelocityTrackerCompat.getAxisVelocity(mTracker, AXIS_SCROLL, 2), 0);
        }

        // Check against an axis that has not yet been supported at any Android version.
        assertEquals(0f, VelocityTrackerCompat.getAxisVelocity(mTracker, AXIS_BRAKE, 4), 0);
    }
}
