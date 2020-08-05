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

package androidx.recyclerview.selection;

import static org.junit.Assert.assertEquals;

import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.recyclerview.selection.testing.TestEvents;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener;
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
public class EventRouterTest {

    private EventRouter mRouter;
    private RecyclerView mRecyclerView;
    private TestOnItemTouchListener mListener;

    @Before
    public void setUp() {
        mListener = new TestOnItemTouchListener();
        mRouter = new EventRouter();
        mRecyclerView = new RecyclerView(ApplicationProvider.getApplicationContext());
        mRouter.set(MotionEvent.TOOL_TYPE_FINGER, mListener);
    }

    @Test
    public void testRoutesEventsByDefault() {
        mRouter.onInterceptTouchEvent(mRecyclerView, TestEvents.Touch.DOWN);
        mRouter.onTouchEvent(mRecyclerView, TestEvents.Touch.DOWN);

        mListener.assertOnInterceptTouchEventCalled(1);
        mListener.assertOnTouchEventCalled(1);
    }

    @Test
    public void testDropsEventAfterDisallowCalled() {
        mRouter.onRequestDisallowInterceptTouchEvent(true);

        mRouter.onInterceptTouchEvent(mRecyclerView, TestEvents.Touch.DOWN);
        mRouter.onTouchEvent(mRecyclerView, TestEvents.Touch.DOWN);

        mListener.assertOnInterceptTouchEventCalled(0);
        mListener.assertOnTouchEventCalled(0);
    }

    @Test
    public void testResetsDisallowOnUplEvents() {
        mRouter.onRequestDisallowInterceptTouchEvent(true);
        mRouter.onInterceptTouchEvent(mRecyclerView, TestEvents.Touch.UP);
        mRouter.onInterceptTouchEvent(mRecyclerView, TestEvents.Touch.DOWN);
        mRouter.onTouchEvent(mRecyclerView, TestEvents.Touch.DOWN);

        // The FINGER/UP event that resets "disallow" is also dispatched to touch-event
        // listeners. So expect 2 calls.
        mListener.assertOnInterceptTouchEventCalled(2);
        mListener.assertOnTouchEventCalled(1);
    }

    @Test
    public void testResetsDisallowOnCancelEvents() {
        mRouter.onRequestDisallowInterceptTouchEvent(true);
        mRouter.onInterceptTouchEvent(mRecyclerView,
                TestEvents.builder().action(MotionEvent.ACTION_CANCEL).build());
        mRouter.onInterceptTouchEvent(mRecyclerView, TestEvents.Touch.DOWN);
        mRouter.onTouchEvent(mRecyclerView, TestEvents.Touch.DOWN);

        // The UNKNOWN/CANCEL event that resets "disallow" is not dispatched to any listeners
        // (because there are no listeners registered for "UNKNOWN" tooltype. So expect 1 call.
        mListener.assertOnInterceptTouchEventCalled(1);
        mListener.assertOnTouchEventCalled(1);
    }

    private static final class TestOnItemTouchListener implements OnItemTouchListener {

        private List<MotionEvent> mOnInterceptTouchEventCalls = new ArrayList<>();
        private List<MotionEvent> mOnTouchEventCalls = new ArrayList<>();

        @Override
        public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
            mOnInterceptTouchEventCalls.add(e);
            return false;
        }

        @Override
        public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
            mOnTouchEventCalls.add(e);
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        }

        void assertOnInterceptTouchEventCalled(int expectedTimesCalled) {
            assertEquals(expectedTimesCalled, mOnInterceptTouchEventCalls.size());
        }

        void assertOnTouchEventCalled(int expectedTimesCalled) {
            assertEquals(expectedTimesCalled, mOnTouchEventCalls.size());
        }
    }
}
