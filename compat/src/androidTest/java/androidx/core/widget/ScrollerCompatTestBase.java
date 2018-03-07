/*
 * Copyright (C) 2014 The Android Open Source Project
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;
import android.view.animation.Interpolator;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.InvocationTargetException;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class ScrollerCompatTestBase {

    private static final boolean DEBUG = false;

    private static final String TAG = "ScrollerCompatTest";

    private ScrollerCompat mScroller;


    protected void createScroller(Interpolator interpolator)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException,
            InstantiationException {
        mScroller = new ScrollerCompat(InstrumentationRegistry.getContext(), interpolator);
    }

    @Test
    public void testTargetReached() throws Throwable {
        if (DEBUG) {
            Log.d(TAG, "testing if target is reached");
        }
        createScroller(null);
        mScroller.fling(0, 0, 0, 1000,
                Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
        int target = mScroller.getFinalY();
        while (mScroller.computeScrollOffset()) {
            Thread.sleep(100);
        }
        assertEquals("given enough time, scroller should reach target position", target,
                mScroller.getCurrY());
    }

    @Test
    public void testAbort() throws Throwable {
        if (DEBUG) {
            Log.d(TAG, "testing abort");
        }
        createScroller(null);
        mScroller.fling(0, 0, 0, 10000,
                Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
        assertTrue("Scroller should have some offset", mScroller.computeScrollOffset());
        mScroller.abortAnimation();
        assertFalse("Scroller should clear offset after being aborted",
                mScroller.computeScrollOffset());
    }
}
