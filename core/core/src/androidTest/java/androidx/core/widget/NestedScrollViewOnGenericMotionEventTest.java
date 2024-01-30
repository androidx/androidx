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

package androidx.core.widget;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.view.MotionEvent;

import androidx.core.view.DifferentialMotionFlingController;
import androidx.core.view.InputDeviceCompat;
import androidx.core.view.MotionEventCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class NestedScrollViewOnGenericMotionEventTest {

    private DifferentialMotionFlingController mSpyDifferentialFlingController;

    private NestedScrollView mNestedScrollView;

    @Before
    public void setUp() throws Exception {
        mNestedScrollView = new NestedScrollView(ApplicationProvider.getApplicationContext());

        mSpyDifferentialFlingController = spy(mNestedScrollView.mDifferentialMotionFlingController);
        mNestedScrollView.mDifferentialMotionFlingController = mSpyDifferentialFlingController;
    }

    @Test
    public void rotaryEncoderMotion_attemptsFling() {
        MotionEvent event = createRotaryEncoderEvent(20);

        mNestedScrollView.onGenericMotionEvent(event);

        verify(mSpyDifferentialFlingController).onMotionEvent(event, MotionEventCompat.AXIS_SCROLL);
    }

    @Test
    public void pointerScrollMotion_attemptsFling() {
        MotionEvent event = createPointerScrollEvent(20);

        mNestedScrollView.onGenericMotionEvent(event);

        verify(mSpyDifferentialFlingController).onMotionEvent(event, MotionEvent.AXIS_VSCROLL);
    }

    @Test
    public void differentialMotionScrollFactor() {
        assertEquals(-mNestedScrollView.getVerticalScrollFactorCompat(),
                mNestedScrollView.mDifferentialMotionFlingTarget.getScaledScrollFactor(),
                0 /* delta */);
    }

    private static MotionEvent createRotaryEncoderEvent(float value) {
        return createScrollMotionEvent(
                InputDeviceCompat.SOURCE_ROTARY_ENCODER, MotionEventCompat.AXIS_SCROLL, value);
    }

    private static MotionEvent createPointerScrollEvent(float value) {
        return createScrollMotionEvent(
                InputDeviceCompat.SOURCE_CLASS_POINTER, MotionEvent.AXIS_VSCROLL, value);
    }

    private static MotionEvent createScrollMotionEvent(int source, int axis, float value) {
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
                4 /* deviceId */,
                0 /* edgeFlags */,
                source,
                0 /* flags */);
    }
}
