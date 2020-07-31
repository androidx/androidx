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
public final class GestureDetectorOnItemTouchListenerAdapterTest {

    private GestureDetectorOnItemTouchListenerAdapter mAdapter;
    private TestGestureDetector mDetector;

    @Before
    public void setUp() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mDetector = new TestGestureDetector(
                ApplicationProvider.getApplicationContext(),
                new GestureDetector.SimpleOnGestureListener()  // do nothing listener
        );
        mAdapter = new GestureDetectorOnItemTouchListenerAdapter(mDetector);
    }

    @Test
    public void testReflectsGestureDetectorReturnValue() {
        mDetector.mReturnValue = true;
        assertTrue(mAdapter.onInterceptTouchEvent(null, TestEvents.Mouse.SECONDARY_CLICK));

        mDetector.mReturnValue = false;
        assertFalse(mAdapter.onInterceptTouchEvent(null, TestEvents.Mouse.SECONDARY_CLICK));
    }

    @Test
    public void testAdapterRespectsDisallowIntercept() {
        mAdapter.onInterceptTouchEvent(/* RecyclerView */ null, TestEvents.Touch.DOWN);
        mDetector.assertOnTouchCalled(1);

        // After disallow intercept, no more calls to onTouch should be forwarded.
        mAdapter.onRequestDisallowInterceptTouchEvent(true);
        mAdapter.onInterceptTouchEvent(/* RecyclerView */ null, TestEvents.Touch.MOVE);
        mDetector.assertOnTouchCalled(1);

        // UP event should reset event handling, allowing forwarding of events again.
        // Num touch events forwarded to GestureDetector should increase by 1 (for the DOWN)
        mAdapter.onInterceptTouchEvent(/* RecyclerView */ null, TestEvents.Touch.UP);
        mAdapter.onInterceptTouchEvent(/* RecyclerView */ null, TestEvents.Touch.DOWN);
        mDetector.assertOnTouchCalled(2);
    }

    private static final class TestGestureDetector extends GestureDetector {

        private final List<MotionEvent> mDetectorEvents = new ArrayList<>();
        boolean mReturnValue;

        TestGestureDetector(Context context, SimpleOnGestureListener listener) {
            super(context, listener);
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            mDetectorEvents.add(ev);
            return mReturnValue;
        }

        void assertOnTouchCalled(int expectedTimes) {
            assertEquals(expectedTimes, mDetectorEvents.size());
        }
    }

    ;
}
