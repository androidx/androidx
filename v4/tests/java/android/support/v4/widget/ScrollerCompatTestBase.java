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
package android.support.v4.widget;

import android.content.Context;
import android.test.AndroidTestCase;
import android.util.Log;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

/**
 * @hide
 */
abstract public class ScrollerCompatTestBase extends AndroidTestCase {

    private static final boolean DEBUG = false;

    private final String TAG;

    private final int mApiLevel;

    private ScrollerCompat mScroller;

    public ScrollerCompatTestBase(int apiLevel) {
        mApiLevel = apiLevel;
        TAG = "ScrollerCompatTest api:" + apiLevel;
    }

    protected void createScroller(Interpolator interpolator)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException,
            InstantiationException {
        Constructor<ScrollerCompat> constructor = ScrollerCompat.class
                .getDeclaredConstructor(int.class, Context.class, Interpolator.class);
        constructor.setAccessible(true);
        mScroller = constructor.newInstance(mApiLevel, getContext(), interpolator);
    }

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
