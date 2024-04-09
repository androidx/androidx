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

package androidx.pdf.util;


import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.util.Log;
import android.view.MotionEvent;

import androidx.pdf.util.GestureTracker.Gesture;
import androidx.pdf.util.GestureTracker.GestureHandler;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.AbstractList;
import java.util.List;

@SmallTest
@RunWith(RobolectricTestRunner.class)
public class GestureTrackerTest {

    private static final String TAG = "GestureTrackerTest";
    private static final int TAP_TIMEOUT = 200;

    private GestureTracker mGestureTracker;
    @Mock
    private GestureHandler mGestureHandler;

    private AutoCloseable mOpenMocks;

    @Before
    public void setUp() {
        mOpenMocks = MockitoAnnotations.openMocks(this);
        mGestureTracker =
                new GestureTracker("GesturesTest", ApplicationProvider.getApplicationContext());
        mGestureTracker.setDelegateHandler(mGestureHandler);
    }

    @After
    public void tearDown() throws Exception {
        mOpenMocks.close();
    }

    @Test
    public void testDrag() {
        Log.d(TAG, "Start test drag");
        for (MotionEvent event : dragSequence(0)) {
            mGestureTracker.feed(event, true);
        }

        ArgumentCaptor<Gesture> capturedGesture = ArgumentCaptor.forClass(Gesture.class);
        verify(mGestureHandler).onGestureStart();
        verify(mGestureHandler).onScroll(anyEvent(), anyEvent(), anyFloat(), anyFloat());
        verify(mGestureHandler).onGestureEnd(capturedGesture.capture());
        verifyNoMoreInteractions(mGestureHandler);
        assertThat(capturedGesture.getValue()).isEqualTo(Gesture.DRAG_X);
        assertThat(mGestureTracker.matches(Gesture.DRAG_X)).isTrue();
        Log.d(TAG, "End test drag");
    }

    @Test
    public void testSingleTap() {
        Log.d(TAG, "Start test single tap");
        for (MotionEvent event : singleTapSequence(0)) {
            mGestureTracker.feed(event, true);
        }

        verify(mGestureHandler).onGestureStart();
        verify(mGestureHandler).onSingleTapUp(anyEvent());

        assertThat(mGestureTracker.matches(Gesture.FIRST_TAP)).isTrue();

        ThreadUtils.postOnUiThreadDelayed(new Runnable() {
            @Override
            public void run() {
                verifySingleTapDelayed();
            }
        }, TAP_TIMEOUT);
    }

    private void verifySingleTapDelayed() {
        ArgumentCaptor<Gesture> capturedGesture = ArgumentCaptor.forClass(Gesture.class);

        verify(mGestureHandler).onSingleTapConfirmed(anyEvent());
        verify(mGestureHandler).onGestureEnd(capturedGesture.capture());
        verifyNoMoreInteractions(mGestureHandler);
        assertThat(mGestureTracker.matches(Gesture.SINGLE_TAP)).isTrue();
        assertThat(capturedGesture.getValue()).isEqualTo(Gesture.SINGLE_TAP);
        Log.d(TAG, "End test single tap");
    }

    @Test
    public void testDoubleTap() {
        Log.d(TAG, "Start test double tap");
        for (MotionEvent event : singleTapSequence(0)) {
            mGestureTracker.feed(event, true);
        }
        for (MotionEvent event : singleTapSequence(50)) {
            mGestureTracker.feed(event, true);
        }

        assertThat(mGestureTracker.matches(Gesture.DOUBLE_TAP)).isTrue();
        verify(mGestureHandler).onGestureStart();
        verify(mGestureHandler, times(2)).onSingleTapUp(anyEvent());

        ArgumentCaptor<Gesture> capturedGesture = ArgumentCaptor.forClass(Gesture.class);

        verify(mGestureHandler).onDoubleTap(anyEvent());
        verify(mGestureHandler).onGestureEnd(capturedGesture.capture());
        verifyNoMoreInteractions(mGestureHandler);
        assertThat(capturedGesture.getValue()).isEqualTo(Gesture.DOUBLE_TAP);
        Log.d(TAG, "End test double tap");
    }

    @Test
    public void testTapDrag() {
        Log.d(TAG, "Start test tap-drag (should not count as double-tap)");
        for (MotionEvent event : singleTapSequence(0)) {
            mGestureTracker.feed(event, true);
        }
        for (MotionEvent event : dragSequence(50)) {
            mGestureTracker.feed(event, true);
        }

        assertThat(mGestureTracker.matches(Gesture.DRAG_X)).isTrue();
        verify(mGestureHandler).onGestureStart();
        verify(mGestureHandler).onSingleTapUp(anyEvent());

        ArgumentCaptor<Gesture> capturedGesture = ArgumentCaptor.forClass(Gesture.class);

        verify(mGestureHandler).onScroll(anyEvent(), anyEvent(), anyFloat(), anyFloat());
        verify(mGestureHandler).onGestureEnd(capturedGesture.capture());
        verifyNoMoreInteractions(mGestureHandler);
        assertThat(capturedGesture.getValue()).isEqualTo(Gesture.DRAG_X);
        Log.d(TAG, "End test tap-drag");
    }

    @Test
    public void testDragTap() {
        Log.d(TAG, "Start test drag-tap (should not count as double-tap)");
        for (MotionEvent event : dragSequence(50)) {
            mGestureTracker.feed(event, true);
        }
        assertThat(mGestureTracker.matches(Gesture.DRAG_X)).isTrue();
        for (MotionEvent event : singleTapSequence(0)) {
            mGestureTracker.feed(event, true);
        }

        verify(mGestureHandler, times(2)).onGestureStart();
        verify(mGestureHandler).onSingleTapUp(anyEvent());

        ArgumentCaptor<Gesture> capturedGesture = ArgumentCaptor.forClass(Gesture.class);

        verify(mGestureHandler).onScroll(anyEvent(), anyEvent(), anyFloat(), anyFloat());
        verify(mGestureHandler).onGestureEnd(capturedGesture.capture());
        verifyNoMoreInteractions(mGestureHandler);
        assertThat(capturedGesture.getValue()).isEqualTo(Gesture.DRAG_X);
        Log.d(TAG, "End test drag-tap");
    }

    private static List<MotionEvent> dragSequence(final long downTime) {
        return new AbstractList<MotionEvent>() {

            @Override
            public MotionEvent get(int step) {
                long eventTime = downTime + step;
                switch (step) {
                    case 0:
                        return MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, 1,
                                1, 0);
                    case 1:
                        return MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_MOVE, 50,
                                1, 0);
                    case 2:
                        return MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, 50, 1,
                                0);
                    default:
                        throw new IllegalStateException("no step " + step);
                }
            }

            @Override
            public int size() {
                return 3;
            }
        };
    }

    private static List<MotionEvent> singleTapSequence(final long downTime) {
        return new AbstractList<MotionEvent>() {

            @Override
            public MotionEvent get(int step) {
                long eventTime = downTime + step;
                switch (step) {
                    case 0:
                        return MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, 1,
                                1, 0);
                    case 1:
                        return MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_MOVE, 3,
                                1, 0);
                    case 2:
                        return MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, 3, 1,
                                0);
                    default:
                        throw new IllegalStateException("no step " + step);
                }
            }

            @Override
            public int size() {
                return 3;
            }
        };
    }

    private static MotionEvent anyEvent() {
        return any();
    }

}
