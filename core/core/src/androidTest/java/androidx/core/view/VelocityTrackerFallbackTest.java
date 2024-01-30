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

import static androidx.core.view.MotionEventCompat.AXIS_SCROLL;

import static com.google.common.truth.Truth.assertThat;

import android.view.InputDevice;
import android.view.MotionEvent;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Unit tests for {@link VelocityTrackerFallback}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class VelocityTrackerFallbackTest {

    private static final float TOLERANCE = 0.05f; // 5% tolerance

    private long mTime;
    private long mLastTime;
    private float mScrollAmount;
    private float mVelocity;
    private float mAcceleration;

    private VelocityTrackerFallback mTracker;

    @Before
    public void setup() {
        mTracker = new VelocityTrackerFallback();
    }

    @Test
    public void testLinearMovement() {
        mVelocity = 3.0f;
        move(100, 10);

        mTracker.computeCurrentVelocity(/* units= */ 1);

        assertVelocityWithTolerance(mTracker.getAxisVelocity(AXIS_SCROLL), mVelocity);
    }

    @Test
    public void testAcceleratingMovement() {
        mVelocity = 3.0f;
        mAcceleration = 2.0f;
        move(200, 10);

        mTracker.computeCurrentVelocity(/* units= */ 1);

        assertVelocityWithTolerance(mTracker.getAxisVelocity(AXIS_SCROLL), mVelocity);
    }

    @Test
    public void testDeceleratingMovement() {
        mVelocity = 7.0f;
        mAcceleration = -2.0f;
        move(200, 10);

        mTracker.computeCurrentVelocity(/* units= */ 1);

        assertVelocityWithTolerance(mTracker.getAxisVelocity(AXIS_SCROLL), mVelocity);
    }

    @Test
    public void testNegativeVelocity() {
        mVelocity = -3.0f;
        move(100, 10);

        mTracker.computeCurrentVelocity(/* units= */ 1);

        assertVelocityWithTolerance(mTracker.getAxisVelocity(AXIS_SCROLL), mVelocity);
    }

    @Test
    public void testZeroVelocity() {
        mVelocity = 0f;
        move(100, 10);

        mTracker.computeCurrentVelocity(/* units= */ 1);

        assertVelocityWithTolerance(mTracker.getAxisVelocity(AXIS_SCROLL), mVelocity);
    }

    @Test
    public void testVelocityDataExpiration() {
        mVelocity = 20f;
        move(100, 10);
        mVelocity = 200f;
        mTime += 10000;
        move(10, 10);

        mTracker.computeCurrentVelocity(/* units= */ 1);

        assertThat(mTracker.getAxisVelocity(AXIS_SCROLL)).isEqualTo(200);
    }

    @Test
    public void testOneDataPoint() {
        mVelocity = 30f;
        move(100, 100);

        mTracker.computeCurrentVelocity(/* units= */ 1);

        assertThat(mTracker.getAxisVelocity(AXIS_SCROLL)).isZero();
    }

    @Test
    public void testTwoDataPoints() {
        mVelocity = 30f;
        move(100, 30);

        mTracker.computeCurrentVelocity(/* units= */ 1);

        assertVelocityWithTolerance(mTracker.getAxisVelocity(AXIS_SCROLL), mVelocity);
    }

    @Test
    public void testPointerMovementStopped() {
        mVelocity = 30f;
        move(50, 2);
        // Add one last event, where the time-step (50ms) exceeds 40ms, which is the pointer stopped
        // assumption duration.
        move(50, 50);

        mTracker.computeCurrentVelocity(/* units= */ 1);

        assertThat(mTracker.getAxisVelocity(AXIS_SCROLL)).isZero();
    }

    @Test
    public void testUnits() {
        mVelocity = 3.0f;
        move(100, 10);

        mTracker.computeCurrentVelocity(/* units= */ 100);

        assertVelocityWithTolerance(mTracker.getAxisVelocity(AXIS_SCROLL), 100 * mVelocity);
    }

    @Test
    public void testMaxVelocity() {
        mVelocity = 3.0f;
        move(100, 10);

        mTracker.computeCurrentVelocity(/* units= */ 1, /* maxVelocity= */ 2.4f);

        assertThat(mTracker.getAxisVelocity(AXIS_SCROLL)).isEqualTo(2.4f);
    }

    @Test
    public void testUnitsWithMaxVelocity() {
        mVelocity = 3.0f;
        move(100, 10);

        mTracker.computeCurrentVelocity(/* units= */ 100, /* maxVelocity= */ 75f);

        assertThat(mTracker.getAxisVelocity(AXIS_SCROLL)).isEqualTo(75f);
    }

    @Test
    public void testNoMovement() {
        assertThat(new VelocityTrackerFallback().getAxisVelocity(AXIS_SCROLL)).isZero();
    }

    @Test
    public void testTwoMovementsWithSameTime() {
        addMovement(/* scrollAmount= */ 2, /* time= */ 10);
        addMovement(/* scrollAmount= */ 200, /* time= */ 10);

        mTracker.computeCurrentVelocity(/* units= */ 1);

        assertThat(mTracker.getAxisVelocity(AXIS_SCROLL)).isZero();
    }

    @Test
    public void testMoreDataPointsThanHistorySize() {
        mVelocity = 7.0f;
        mAcceleration = 2.0f;
        move(300, 10); // 30 data points

        mTracker.computeCurrentVelocity(/* units= */ 1);

        assertVelocityWithTolerance(mTracker.getAxisVelocity(AXIS_SCROLL), mVelocity);
    }

    private void move(long duration, long step) {
        addMovement();
        while (duration > 0) {
            duration -= step;
            mTime += step;

            mScrollAmount = (mAcceleration / 2 * step + mVelocity) * step;
            mVelocity += mAcceleration * step;
            addMovement();
        }
    }

    private void addMovement() {
        if (mTime <= mLastTime) {
            return;
        }
        addMovement(mScrollAmount, mTime);
        mLastTime = mTime;
    }

    private void addMovement(float scrollAmount, long time) {
        MotionEvent.PointerProperties[] props =
                new MotionEvent.PointerProperties[] {new MotionEvent.PointerProperties()};
        props[0].id = 0;

        MotionEvent.PointerCoords[] coords =
                new MotionEvent.PointerCoords[] {new MotionEvent.PointerCoords()};
        coords[0].setAxisValue(MotionEvent.AXIS_SCROLL, scrollAmount);

        MotionEvent ev =
                MotionEvent.obtain(
                        /* downTime= */ 0,
                        time,
                        MotionEvent.ACTION_SCROLL,
                        /* pointerCount= */ 1,
                        props,
                        coords,
                        /* metaState= */ 0,
                        /* buttonState= */ 0,
                        /* xPrecision= */ 0,
                        /* yPrecision= */ 0,
                        /* deviceId= */ 1,
                        /* edgeFlags= */ 0,
                        InputDevice.SOURCE_ROTARY_ENCODER,
                        /* flags= */ 0);
        mTracker.addMovement(ev);
        ev.recycle();
    }

    private void assertVelocityWithTolerance(float actualVelocity, float expectedVelocity) {
        assertThat(actualVelocity)
                .isWithin(Math.abs(TOLERANCE * expectedVelocity))
                .of(expectedVelocity);
    }
}
