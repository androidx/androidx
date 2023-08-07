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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.os.Build;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.VelocityTracker;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class VelocityTrackerCompatTest {
    /** Arbitrarily chosen velocities across different supported dimensions and some pointer IDs. */
    private static final float X_VEL_POINTER_ID_1 = 5;
    private static final float X_VEL_POINTER_ID_2 = 6;
    private static final float Y_VEL_POINTER_ID_1 = 7;
    private static final float Y_VEL_POINTER_ID_2 = 8;
    private static final float SCROLL_VEL_POINTER_ID_1 = 9;
    private static final float SCROLL_VEL_POINTER_ID_2 = 10;

    /**
     * A small enough step time stamp (ms), that the VelocityTracker wouldn't consider big enough to
     * assume a pointer has stopped.
     */
    private static final long TIME_STEP_MS = 10;

    /**
     * An arbitrarily chosen value for the number of times a movement particular type of movement
     * is added to a tracker. For velocities to be non-zero, we should generally have 2/3 movements,
     * so 4 is a good value to use.
     */
    private static final int NUM_MOVEMENTS = 8;

    private VelocityTracker mPlanarTracker;
    private VelocityTracker mScrollTracker;

    @Before
    public void setup() {
        mPlanarTracker = VelocityTracker.obtain();
        mScrollTracker = VelocityTracker.obtain();

        long time = 0;
        float xPointer1 = 0;
        float yPointer1 = 0;
        float scrollPointer1 = 0;
        float xPointer2 = 0;
        float yPointer2 = 0;
        float scrollPointer2 = 0;

        // Add MotionEvents to create some velocity!
        // Note that: the goal of these tests is not to check the specific values of the velocities,
        // but instead, compare the outputs of the Compat tracker against the platform tracker.
        for (int i = 0; i < NUM_MOVEMENTS; i++) {
            time += TIME_STEP_MS;
            xPointer1 += X_VEL_POINTER_ID_1 * TIME_STEP_MS;
            yPointer1 += Y_VEL_POINTER_ID_1 * TIME_STEP_MS;
            scrollPointer1 = SCROLL_VEL_POINTER_ID_1 * TIME_STEP_MS;

            xPointer2 += X_VEL_POINTER_ID_2 * TIME_STEP_MS;
            yPointer2 += Y_VEL_POINTER_ID_2 * TIME_STEP_MS;
            scrollPointer2 = SCROLL_VEL_POINTER_ID_2 * TIME_STEP_MS;

            addPlanarMotionEvent(1, time, xPointer1, yPointer1);
            addPlanarMotionEvent(2, time, xPointer2, yPointer2);
            addScrollMotionEvent(1, time, scrollPointer1);
            addScrollMotionEvent(2, time, scrollPointer2);
        }

        // Assert that all velocity is 0 before compute is called.
        assertEquals(0, VelocityTrackerCompat.getAxisVelocity(mPlanarTracker, AXIS_X), 0);
        assertEquals(0, VelocityTrackerCompat.getAxisVelocity(mPlanarTracker, AXIS_Y), 0);
        assertEquals(
                0, VelocityTrackerCompat.getAxisVelocity(mScrollTracker, AXIS_SCROLL), 0);

        VelocityTrackerCompat.computeCurrentVelocity(mPlanarTracker, 1000);
        VelocityTrackerCompat.computeCurrentVelocity(mScrollTracker, 1000);
    }

    @Test
    public void testIsAxisSupported_planarAxes() {
        assertTrue(VelocityTrackerCompat.isAxisSupported(VelocityTracker.obtain(), AXIS_X));
        assertTrue(VelocityTrackerCompat.isAxisSupported(VelocityTracker.obtain(), AXIS_Y));
    }

    @Test
    public void testIsAxisSupported_axisScroll() {
        assertTrue(VelocityTrackerCompat.isAxisSupported(VelocityTracker.obtain(), AXIS_SCROLL));
    }

    @Test
    public void testIsAxisSupported_nonPlanarAxes() {
        // Check against an axis that has not yet been supported at any Android version.
        assertFalse(VelocityTrackerCompat.isAxisSupported(VelocityTracker.obtain(), AXIS_BRAKE));
    }

    @Test
    public void testGetAxisVelocity_planarAxes_noPointerId_againstEquivalentPlatformApis() {
        if (Build.VERSION.SDK_INT >= 34) {
            float compatXVelocity = VelocityTrackerCompat.getAxisVelocity(mPlanarTracker, AXIS_X);
            float compatYVelocity = VelocityTrackerCompat.getAxisVelocity(mPlanarTracker, AXIS_Y);

            assertEquals(mPlanarTracker.getAxisVelocity(AXIS_X), compatXVelocity, 0);
            assertEquals(mPlanarTracker.getAxisVelocity(AXIS_Y), compatYVelocity, 0);
        }
    }

    @Test
    public void testGetAxisVelocity_planarAxes_withPointerId_againstEquivalentPlatformApis() {
        if (Build.VERSION.SDK_INT >= 34) {
            float compatXVelocity =
                    VelocityTrackerCompat.getAxisVelocity(mPlanarTracker, AXIS_X, 2);
            float compatYVelocity =
                    VelocityTrackerCompat.getAxisVelocity(mPlanarTracker, AXIS_Y, 2);

            assertEquals(mPlanarTracker.getAxisVelocity(AXIS_X, 2), compatXVelocity, 0);
            assertEquals(mPlanarTracker.getAxisVelocity(AXIS_Y, 2), compatYVelocity, 0);
        }
    }

    @Test
    public void testGetAxisVelocity_planarAxes_noPointerId_againstGenericXAndYVelocityApis() {
        float compatXVelocity = VelocityTrackerCompat.getAxisVelocity(mPlanarTracker, AXIS_X);
        float compatYVelocity = VelocityTrackerCompat.getAxisVelocity(mPlanarTracker, AXIS_Y);

        assertEquals(mPlanarTracker.getXVelocity(), compatXVelocity, 0);
        assertEquals(mPlanarTracker.getYVelocity(), compatYVelocity, 0);
    }

    @Test
    public void testGetAxisVelocity_planarAxes_withPointerId_againstGenericXAndYVelocityApis() {
        float compatXVelocity =
                VelocityTrackerCompat.getAxisVelocity(mPlanarTracker, AXIS_X, 2);
        float compatYVelocity =
                VelocityTrackerCompat.getAxisVelocity(mPlanarTracker, AXIS_Y, 2);

        assertEquals(mPlanarTracker.getXVelocity(2), compatXVelocity, 0);
        assertEquals(mPlanarTracker.getYVelocity(2), compatYVelocity, 0);
    }

    @Test
    public void testGetAxisVelocity_axisScroll_noPointerId() {
        float compatVelocity = VelocityTrackerCompat.getAxisVelocity(mScrollTracker, AXIS_SCROLL);

        assertEquals(SCROLL_VEL_POINTER_ID_1 * 1000, compatVelocity, 0);

        compatVelocity = VelocityTrackerCompat.getAxisVelocity(mScrollTracker, AXIS_SCROLL);

        assertEquals(SCROLL_VEL_POINTER_ID_1 * 1000, compatVelocity, 0);

        VelocityTrackerCompat.clear(mScrollTracker);
        compatVelocity = VelocityTrackerCompat.getAxisVelocity(mScrollTracker, AXIS_SCROLL);

        assertEquals(0, compatVelocity, 0);
    }

    @Test
    public void testGetAxisVelocity_axisScroll_withPointerId() {
        float compatScrollVelocity =
                VelocityTrackerCompat.getAxisVelocity(mScrollTracker, AXIS_SCROLL, 2);

        if (Build.VERSION.SDK_INT >= 34) {
            assertNotEquals(0, compatScrollVelocity);
            assertEquals(mScrollTracker.getAxisVelocity(AXIS_SCROLL, 2), compatScrollVelocity, 0);
        } else {
            assertEquals(0, compatScrollVelocity, 0);
        }
    }


    private void addPlanarMotionEvent(int pointerId, long time, float x, float y) {
        MotionEvent ev = MotionEvent.obtain(0L, time, MotionEvent.ACTION_MOVE, x, y, 0);
        VelocityTrackerCompat.addMovement(mPlanarTracker, ev);
        ev.recycle();
    }
    private void addScrollMotionEvent(int pointerId, long time, float scrollAmount) {
        MotionEvent.PointerProperties props = new MotionEvent.PointerProperties();
        props.id = pointerId;

        MotionEvent.PointerCoords coords = new MotionEvent.PointerCoords();
        coords.setAxisValue(MotionEvent.AXIS_SCROLL, scrollAmount);

        MotionEvent ev = MotionEvent.obtain(0 /* downTime */,
                time,
                MotionEvent.ACTION_SCROLL,
                1 /* pointerCount */,
                new MotionEvent.PointerProperties[] {props},
                new MotionEvent.PointerCoords[] {coords},
                0 /* metaState */,
                0 /* buttonState */,
                0 /* xPrecision */,
                0 /* yPrecision */,
                1 /* deviceId */,
                0 /* edgeFlags */,
                InputDevice.SOURCE_ROTARY_ENCODER,
                0 /* flags */);
        VelocityTrackerCompat.addMovement(mScrollTracker, ev);
        ev.recycle();
    }
}
