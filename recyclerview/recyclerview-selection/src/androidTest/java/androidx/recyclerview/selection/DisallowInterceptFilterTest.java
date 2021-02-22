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

import androidx.recyclerview.selection.testing.TestEvents;
import androidx.recyclerview.selection.testing.TestOnItemTouchListener;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class DisallowInterceptFilterTest {

    private TestOnItemTouchListener mListener;
    private DisallowInterceptFilter mFilter;

    @Before
    public void setUp() {
        mListener = new TestOnItemTouchListener();
        mFilter = new DisallowInterceptFilter(mListener);
    }

    @Test
    public void testForwardsEventsToDelegate() {
        mFilter.onInterceptTouchEvent(null, TestEvents.Touch.DOWN);
        mListener.assertOnInterceptTouchEventCalled(1);
    }

    @Test
    public void testReflectsDelegateReturnValue() {
        assertFalse(mFilter.onInterceptTouchEvent(null, TestEvents.Touch.UP));
        mListener.consumeEvents(true);
        assertTrue(mFilter.onInterceptTouchEvent(null, TestEvents.Touch.MOVE));
    }

    @Test
    public void testRespectsDisallowRequest() {
        mFilter.onRequestDisallowInterceptTouchEvent(true);
        mFilter.onInterceptTouchEvent(null, TestEvents.Touch.UP);
        mListener.assertOnInterceptTouchEventCalled(0);
    }

    @Test
    public void testResetsDisallowOnDown() {
        mFilter.onRequestDisallowInterceptTouchEvent(true);
        mFilter.onInterceptTouchEvent(null, TestEvents.Touch.DOWN);
        mListener.assertOnInterceptTouchEventCalled(1);
    }

    @Test
    public void testIsResettable() {
        assertFalse(mFilter.isResetRequired());
        mFilter.onRequestDisallowInterceptTouchEvent(true);
        assertTrue(mFilter.isResetRequired());
        mFilter.reset();
        assertFalse(mFilter.isResetRequired());
    }

    @Test
    public void testResetClearsDisallowBit() {
        mFilter.onRequestDisallowInterceptTouchEvent(true);
        mFilter.reset();
        mFilter.onInterceptTouchEvent(null, TestEvents.Touch.MOVE);
        mListener.assertOnInterceptTouchEventCalled(1);
    }
}
