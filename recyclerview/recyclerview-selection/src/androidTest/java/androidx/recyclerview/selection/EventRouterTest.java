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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.view.MotionEvent;

import androidx.recyclerview.selection.testing.TestEvents;
import androidx.recyclerview.selection.testing.TestOnItemTouchListener;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

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
        mRouter.set(new ToolSourceKey(MotionEvent.TOOL_TYPE_FINGER), mListener);
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

        mRouter.onInterceptTouchEvent(mRecyclerView, TestEvents.Touch.MOVE);
        mRouter.onTouchEvent(mRecyclerView, TestEvents.Touch.MOVE);

        mListener.assertOnInterceptTouchEventCalled(0);
        mListener.assertOnTouchEventCalled(0);
    }

    @Test
    public void testResetsDisallowOnDownEvents() {
        mRouter.onRequestDisallowInterceptTouchEvent(true);
        mRouter.onInterceptTouchEvent(mRecyclerView, TestEvents.Touch.DOWN);
        mRouter.onInterceptTouchEvent(mRecyclerView, TestEvents.Touch.UP);
        mRouter.onTouchEvent(mRecyclerView, TestEvents.Touch.UP);

        // The FINGER/DOWN event that resets "disallow" is also dispatched to touch-event
        // listeners. So expect 2 calls.
        mListener.assertOnInterceptTouchEventCalled(2);
        mListener.assertOnTouchEventCalled(1);
    }

    @Test
    public void testResettable() {
        // Only resettable when disallowIntercept has been requested.
        mRouter.onRequestDisallowInterceptTouchEvent(true);
        assertTrue(mRouter.isResetRequired());

        ResetManager<?> mgr = new ResetManager<>();
        mgr.addResetHandler(mRouter);
        mgr.getSelectionObserver().onSelectionCleared();  // Results in reset.

        assertFalse(mRouter.isResetRequired());
    }
}
