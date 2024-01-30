/*
 * Copyright 2023 The Android Open Source Project
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.view.InputDevice;
import android.view.MotionEvent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class DifferentialMotionFlingControllerTest {
    private int mMinVelocity = 0;
    private int mMaxVelocity = Integer.MAX_VALUE;
    /** A fake velocity value that's going to be returned from the velocity provider. */
    private float mVelocity;
    private boolean mVelocityCalculated;

    private final DifferentialMotionFlingController.DifferentialVelocityProvider mVelocityProvider =
            (vt, event, axis) -> {
                mVelocityCalculated = true;
                return mVelocity;
            };

    private final DifferentialMotionFlingController.FlingVelocityThresholdCalculator
            mVelocityThresholdCalculator =
                    (ctx, buffer, event, axis) -> {
                        buffer[0] = mMinVelocity;
                        buffer[1] = mMaxVelocity;
                    };

    private final TestDifferentialMotionFlingTarget mFlingTarget =
            new TestDifferentialMotionFlingTarget();

    private DifferentialMotionFlingController mFlingController;

    @Before
    public void setUp() throws Exception {
        mFlingController = new DifferentialMotionFlingController(
                ApplicationProvider.getApplicationContext(),
                mFlingTarget,
                mVelocityThresholdCalculator,
                mVelocityProvider);
    }

    @Test
    public void deviceDoesNotSupportFling_noVelocityCalculated() {
        mMinVelocity = Integer.MAX_VALUE;
        mMaxVelocity = Integer.MIN_VALUE;

        deliverEventWithVelocity(createPointerEvent(), MotionEvent.AXIS_VSCROLL, 60);

        assertFalse(mVelocityCalculated);
    }

    @Test
    public void flingVelocityOppositeToPrevious_stopsOngoingFling() {
        deliverEventWithVelocity(createRotaryEncoderEvent(), MotionEvent.AXIS_SCROLL, 50);
        deliverEventWithVelocity(createRotaryEncoderEvent(), MotionEvent.AXIS_SCROLL, -10);

        // One stop on the initial event, and second stop due to opposite velocities.
        assertEquals(2, mFlingTarget.mNumStops);
    }

    @Test
    public void flingParamsChanged_stopsOngoingFling() {
        deliverEventWithVelocity(createPointerEvent(), MotionEvent.AXIS_VSCROLL, 50);
        deliverEventWithVelocity(createRotaryEncoderEvent(), MotionEvent.AXIS_SCROLL, 10);

        // One stop on the initial event, and second stop due to changed axis/source.
        assertEquals(2, mFlingTarget.mNumStops);
    }

    @Test
    public void positiveFlingVelocityTooLow_doesNotGenerateFling() {
        mMinVelocity = 50;
        mMaxVelocity = 100;
        deliverEventWithVelocity(createPointerEvent(), MotionEvent.AXIS_VSCROLL, 20);

        assertEquals(0, mFlingTarget.mLastFlingVelocity, /* delta= */ 0);
    }

    @Test
    public void negativeFlingVelocityTooLow_doesNotGenerateFling() {
        mMinVelocity = 50;
        mMaxVelocity = 100;
        deliverEventWithVelocity(createPointerEvent(), MotionEvent.AXIS_VSCROLL, -20);

        assertEquals(0, mFlingTarget.mLastFlingVelocity, /* delta= */ 0);
    }

    @Test
    public void positiveFlingVelocityAboveMinimum_generateFlings() {
        mMinVelocity = 50;
        mMaxVelocity = 100;
        deliverEventWithVelocity(createPointerEvent(), MotionEvent.AXIS_VSCROLL, 60);

        assertEquals(60, mFlingTarget.mLastFlingVelocity, /* delta= */ 0);
    }

    @Test
    public void negativeFlingVelocityAboveMinimum_generateFlings() {
        mMinVelocity = 50;
        mMaxVelocity = 100;
        deliverEventWithVelocity(createPointerEvent(), MotionEvent.AXIS_VSCROLL, -60);

        assertEquals(-60, mFlingTarget.mLastFlingVelocity, /* delta= */ 0);
    }

    @Test
    public void positiveFlingVelocityAboveMaximum_velocityClamped() {
        mMinVelocity = 50;
        mMaxVelocity = 100;
        deliverEventWithVelocity(createPointerEvent(), MotionEvent.AXIS_VSCROLL, 3000);

        assertEquals(100, mFlingTarget.mLastFlingVelocity, /* delta= */ 0);
    }

    @Test
    public void negativeFlingVelocityAboveMaximum_velocityClamped() {
        mMinVelocity = 50;
        mMaxVelocity = 100;
        deliverEventWithVelocity(createPointerEvent(), MotionEvent.AXIS_VSCROLL, -3000);

        assertEquals(-100, mFlingTarget.mLastFlingVelocity, /* delta= */ 0);
    }

    private MotionEvent createRotaryEncoderEvent() {
        return createMotionEvent(
                /* inputDeviceId= */ 3,
                InputDevice.SOURCE_ROTARY_ENCODER,
                MotionEvent.AXIS_SCROLL,
                /* axisValue= */ 10);
    }

    private MotionEvent createPointerEvent() {
        return createMotionEvent(
                /* inputDeviceId= */ 4,
                InputDevice.SOURCE_CLASS_POINTER,
                MotionEvent.AXIS_VSCROLL,
                /* axisValue= */ 10);
    }

    private void deliverEventWithVelocity(MotionEvent ev, int axis, float velocity) {
        mVelocity = velocity;
        mFlingController.onMotionEvent(ev, axis);
        ev.recycle();
    }

    private static MotionEvent createMotionEvent(
            int inputDeviceId, int source, int axis, float value) {
        MotionEvent.PointerProperties props = new MotionEvent.PointerProperties();
        props.id = 0;

        MotionEvent.PointerCoords coords = new MotionEvent.PointerCoords();
        coords.setAxisValue(axis, value);

        return MotionEvent.obtain(0 /* downTime */,
                12,
                MotionEvent.ACTION_SCROLL,
                1 /* pointerCount */,
                new MotionEvent.PointerProperties[] {props},
                new MotionEvent.PointerCoords[] {coords},
                0 /* metaState */,
                0 /* buttonState */,
                0 /* xPrecision */,
                0 /* yPrecision */,
                inputDeviceId,
                0 /* edgeFlags */,
                source,
                0 /* flags */);
    }
}
