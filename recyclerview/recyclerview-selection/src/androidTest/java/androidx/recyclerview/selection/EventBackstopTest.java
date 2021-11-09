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
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class EventBackstopTest {

    private EventBackstop mBackstop;

    @Before
    public void setUp() {
        mBackstop = new EventBackstop();
    }

    @Test
    public void testConsumesOneUpAfterLongPress() {
        mBackstop.onLongPress();
        assertTrue(mBackstop.onInterceptTouchEvent(null, TestEvents.Touch.UP));
    }

    @Test
    public void testIgnoresUpActionsAfterFirst() {
        mBackstop.onLongPress();
        mBackstop.onInterceptTouchEvent(null, TestEvents.Touch.UP);
        assertFalse(mBackstop.onInterceptTouchEvent(null, TestEvents.Touch.UP));
    }

    @Test
    public void testIgnoresUpWithoutLongPress() {
        assertFalse(mBackstop.onInterceptTouchEvent(null, TestEvents.Touch.UP));
    }

    @Test
    public void testIgnoresOtherActionsAfterLongPress() {
        mBackstop.onLongPress();
        assertFalse(mBackstop.onInterceptTouchEvent(null, TestEvents.Touch.DOWN));
        assertFalse(mBackstop.onInterceptTouchEvent(null, TestEvents.Touch.MOVE));
    }
}
