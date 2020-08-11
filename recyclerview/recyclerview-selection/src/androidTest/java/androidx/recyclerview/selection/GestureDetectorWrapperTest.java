/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.recyclerview.selection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.recyclerview.selection.testing.TestEvents;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;


@RunWith(AndroidJUnit4.class)
@SmallTest
public final class GestureDetectorWrapperTest {

    private GestureDetectorWrapper mAdapter;
    private TestGestureDetector mDetector;
    private TestListener mListener;

    @Before
    public void setUp() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mListener = new TestListener();
        mDetector = new TestGestureDetector(
                ApplicationProvider.getApplicationContext(),
                mListener);

        mAdapter = new GestureDetectorWrapper(mDetector);
    }

    @Test
    public void testReflectsGestureDetectorReturnValue() {
        mListener.mNextReturnVal = true;
        assertTrue(mAdapter.onInterceptTouchEvent(null, TestEvents.Touch.DOWN));

        mListener.mNextReturnVal = false;
        assertFalse(mAdapter.onInterceptTouchEvent(null, TestEvents.Touch.DOWN));
    }

    @Test
    public void testRelaysActions() {
        mAdapter.onInterceptTouchEvent(/* RecyclerView */ null, TestEvents.Touch.DOWN);
        mAdapter.onInterceptTouchEvent(/* RecyclerView */ null, TestEvents.Touch.MOVE);
        mAdapter.onInterceptTouchEvent(/* RecyclerView */ null, TestEvents.Touch.UP);
        mDetector.assertOnTouchCalled(1, MotionEvent.ACTION_DOWN);
        mDetector.assertOnTouchCalled(1, MotionEvent.ACTION_MOVE);
        mDetector.assertOnTouchCalled(1, MotionEvent.ACTION_UP);

        mDetector.assertOnTouchCalled(3);
    }

    @Test
    public void testRequestDisallowSendsFakeCancelEvent() {
        mAdapter.onRequestDisallowInterceptTouchEvent(true);

        // The adapter sends a synthetic ACTION_CANCEL event to GD when disallow is called.
        mDetector.assertOnTouchCalled(1, MotionEvent.ACTION_CANCEL);
        mDetector.assertOnTouchCalled(1);
    }

    @Test
    public void testResettable() {
        ResetManager<?> mgr = new ResetManager<>();
        mgr.addResetHandler(mAdapter);
        mgr.getSelectionObserver().onSelectionCleared();  // Results in reset.

        // The adapter sends a synthetic ACTION_CANCEL event to GD when disallow is called.
        mDetector.assertOnTouchCalled(1, MotionEvent.ACTION_CANCEL);
        mDetector.assertOnTouchCalled(1);
    }

    @Test
    public void testRespectsDisallowIntercept() {
        mAdapter.onRequestDisallowInterceptTouchEvent(true);

        mAdapter.onInterceptTouchEvent(/* RecyclerView */ null, TestEvents.Touch.MOVE);
        mAdapter.onInterceptTouchEvent(/* RecyclerView */ null, TestEvents.Touch.UP);
        // Sending DOWN would reset state, so we only test with MOVE and UP.

        mDetector.assertOnTouchCalled(0, MotionEvent.ACTION_MOVE);
        mDetector.assertOnTouchCalled(0, MotionEvent.ACTION_UP);

        // Only the initial CANCEL event sent. Everything else ignored.
        mDetector.assertOnTouchCalled(1);
    }

    @Test
    public void testResetsDisallowInterceptOnDown() {
        mAdapter.onRequestDisallowInterceptTouchEvent(true);
        // Synthetic CANCEL event used to coerce GestureDetector into resetting internal state.
        // Issued whenever disallowIntercept = true
        mDetector.assertOnTouchCalled(1, MotionEvent.ACTION_CANCEL);

        // Should reset DOWN
        mAdapter.onInterceptTouchEvent(/* RecyclerView */ null, TestEvents.Touch.DOWN);
        mAdapter.onInterceptTouchEvent(/* RecyclerView */ null, TestEvents.Touch.MOVE);
        mAdapter.onInterceptTouchEvent(/* RecyclerView */ null, TestEvents.Touch.UP);
        mDetector.assertOnTouchCalled(1, MotionEvent.ACTION_DOWN);
        mDetector.assertOnTouchCalled(1, MotionEvent.ACTION_MOVE);
        mDetector.assertOnTouchCalled(1, MotionEvent.ACTION_UP);

        mDetector.assertOnTouchCalled(4); // CANCEL + DOWN,MOVE,UP
    }

    private static final class TestGestureDetector extends GestureDetector {

        private final List<MotionEvent> mDetectorEvents = new ArrayList<>();

        TestGestureDetector(Context context, SimpleOnGestureListener listener) {
            super(context, listener);
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            mDetectorEvents.add(ev);
            return super.onTouchEvent(ev);
        }

        void assertOnTouchCalled(int expectedTimes) {
            assertEquals(expectedTimes, mDetectorEvents.size());
        }

        void assertOnTouchCalled(int expectedTimes, int action) {
            int total = 0;
            for (MotionEvent e : mDetectorEvents) {
                if (e.getAction() == action) {
                    total++;
                }
            }
            assertEquals(expectedTimes, total);
        }
    }

    private static class TestListener extends GestureDetector.SimpleOnGestureListener {
        boolean mNextReturnVal = false;

        public boolean onSingleTapUp(MotionEvent e) {
            return mNextReturnVal;
        }

        public boolean onDown(MotionEvent e) {
            return mNextReturnVal;
        }
    }
}
