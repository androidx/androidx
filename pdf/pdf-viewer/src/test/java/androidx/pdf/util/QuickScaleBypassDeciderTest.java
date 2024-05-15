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

import android.view.MotionEvent;

import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Unit tests for {@link QuickScaleBypassDecider}. */
@SmallTest
@RunWith(RobolectricTestRunner.class)
public class QuickScaleBypassDeciderTest {

    private static final long FIRST_DOWN_TIME_MS = 0;
    private static final long FIRST_UP_TIME_MS = 5;
    /**
     * We must have at least 40 ms between first up and second down, as {@link
     * android.view.ViewConfiguration} has a {@code DOUBLE_TAP_MIN_TIME} of 40ms.
     */
    private static final long SECOND_DOWN_TIME_MS = 50;
    /**
     * Down time for a later event that happens far enough in the future that it should not be
     * considered a double tap.
     */
    private static final long NON_DOUBLE_TAP_DOWN_TIME_MS = 400;

    // These MotionEvents can't be static because Robolectric can't find the classes until after
    // this
    // class is instantiated.
    private final GestureTracker.EventId mFirstUpEventId =
            new GestureTracker.EventId(
                    MotionEvent.obtain(
                            FIRST_DOWN_TIME_MS,
                            FIRST_UP_TIME_MS,
                            MotionEvent.ACTION_UP,
                            1 /* x */,
                            1 /* y */,
                            0 /* metaState */));
    private final MotionEvent mSecondDownEvent =
            MotionEvent.obtain(
                    SECOND_DOWN_TIME_MS,
                    SECOND_DOWN_TIME_MS,
                    MotionEvent.ACTION_DOWN,
                    1 /* x */,
                    1 /* y */,
                    0 /* metaState */);
    private final GestureTracker.EventId mFirstMoveEventId =
            new GestureTracker.EventId(
                    MotionEvent.obtain(
                            FIRST_DOWN_TIME_MS,
                            FIRST_DOWN_TIME_MS,
                            MotionEvent.ACTION_MOVE,
                            1 /* x */,
                            1 /* y */,
                            0 /* metaState */));
    /**
     * A subsequent {@link MotionEvent#ACTION_DOWN} event that occurs after the double tap
     * threshold.
     */
    private final MotionEvent mLaterDownEvent =
            MotionEvent.obtain(
                    NON_DOUBLE_TAP_DOWN_TIME_MS,
                    NON_DOUBLE_TAP_DOWN_TIME_MS,
                    MotionEvent.ACTION_DOWN,
                    1 /* x */,
                    1 /* y */,
                    0 /* metaState */);

    private QuickScaleBypassDecider mQuickscalebypassdecider;

    @Before
    public void setUp() {
        mQuickscalebypassdecider = new QuickScaleBypassDecider();
    }

    @Test
    public void testShouldSkipZoomDetector_whenScrollEventFollowedByDown_returnsTrue() {
        mQuickscalebypassdecider.setLastGesture(GestureTracker.Gesture.DRAG);
        assertThat(
                mQuickscalebypassdecider.shouldSkipZoomDetector(mSecondDownEvent, mFirstUpEventId))
                .isTrue();
        mQuickscalebypassdecider.setLastGesture(GestureTracker.Gesture.DRAG_X);
        assertThat(
                mQuickscalebypassdecider.shouldSkipZoomDetector(mSecondDownEvent, mFirstUpEventId))
                .isTrue();
        mQuickscalebypassdecider.setLastGesture(GestureTracker.Gesture.DRAG_Y);
        assertThat(
                mQuickscalebypassdecider.shouldSkipZoomDetector(mSecondDownEvent, mFirstUpEventId))
                .isTrue();
        mQuickscalebypassdecider.setLastGesture(GestureTracker.Gesture.FLING);
        assertThat(
                mQuickscalebypassdecider.shouldSkipZoomDetector(mSecondDownEvent, mFirstUpEventId))
                .isTrue();
    }

    @Test
    public void testShouldSkipZoomDetector_whenNonScrollEventFollowedByDown_returnsFalse() {
        mQuickscalebypassdecider.setLastGesture(null);
        assertThat(
                mQuickscalebypassdecider.shouldSkipZoomDetector(mSecondDownEvent, mFirstUpEventId))
                .isFalse();
        mQuickscalebypassdecider.setLastGesture(GestureTracker.Gesture.TOUCH);
        assertThat(
                mQuickscalebypassdecider.shouldSkipZoomDetector(mSecondDownEvent, mFirstUpEventId))
                .isFalse();
        mQuickscalebypassdecider.setLastGesture(GestureTracker.Gesture.FIRST_TAP);
        assertThat(
                mQuickscalebypassdecider.shouldSkipZoomDetector(mSecondDownEvent, mFirstUpEventId))
                .isFalse();
        mQuickscalebypassdecider.setLastGesture(GestureTracker.Gesture.SINGLE_TAP);
        assertThat(
                mQuickscalebypassdecider.shouldSkipZoomDetector(mSecondDownEvent, mFirstUpEventId))
                .isFalse();
        mQuickscalebypassdecider.setLastGesture(GestureTracker.Gesture.DOUBLE_TAP);
        assertThat(
                mQuickscalebypassdecider.shouldSkipZoomDetector(mSecondDownEvent, mFirstUpEventId))
                .isFalse();
        mQuickscalebypassdecider.setLastGesture(GestureTracker.Gesture.LONG_PRESS);
        assertThat(
                mQuickscalebypassdecider.shouldSkipZoomDetector(mSecondDownEvent, mFirstUpEventId))
                .isFalse();
        mQuickscalebypassdecider.setLastGesture(GestureTracker.Gesture.ZOOM);
        assertThat(
                mQuickscalebypassdecider.shouldSkipZoomDetector(mSecondDownEvent, mFirstUpEventId))
                .isFalse();
    }

    @Test
    public void testShouldSkipZoomDetector_whenLastEventNull_returnsFalse() {
        mQuickscalebypassdecider.setLastGesture(GestureTracker.Gesture.DRAG);
        assertThat(
                mQuickscalebypassdecider.shouldSkipZoomDetector(mSecondDownEvent, null)).isFalse();
    }

    @Test
    public void testShouldSkipZoomDetector_whenLastEventIsNotActionup_returnsFalse() {
        mQuickscalebypassdecider.setLastGesture(GestureTracker.Gesture.DRAG);
        assertThat(
                mQuickscalebypassdecider.shouldSkipZoomDetector(mSecondDownEvent,
                        mFirstMoveEventId))
                .isFalse();
    }

    @Test
    public void testShouldSkipZoomDetector_whenTapsLaterThanDoubleTapThreshold_returnsFalse() {
        mQuickscalebypassdecider.setLastGesture(GestureTracker.Gesture.DRAG);
        assertThat(
                mQuickscalebypassdecider.shouldSkipZoomDetector(mLaterDownEvent, mFirstUpEventId))
                .isFalse();
    }
}
