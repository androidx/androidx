/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.v4.widget.SwipeRefreshLayoutActions.setRefreshing;
import static android.support.v4.widget.SwipeRefreshLayoutActions.setSize;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.support.coreui.test.R;
import android.support.v4.BaseInstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests SwipeRefreshLayout widget.
 */
public class SwipeRefreshLayoutTest
        extends BaseInstrumentationTestCase<SwipeRefreshLayoutActivity> {

    private SwipeRefreshLayout mSwipeRefresh;

    public SwipeRefreshLayoutTest() {
        super(SwipeRefreshLayoutActivity.class);
    }

    @Before
    public void setUp() {
        mSwipeRefresh = (SwipeRefreshLayout) mActivityTestRule.getActivity().findViewById(
                R.id.swipe_refresh);
    }

    @Test
    @MediumTest
    public void testStartAndStopRefreshing() throws Throwable {
        assertFalse(mSwipeRefresh.isRefreshing());
        for (int i = 0; i < 5; i++) {
            onView(withId(R.id.swipe_refresh)).perform(setRefreshing());
            assertTrue(mSwipeRefresh.isRefreshing());

            // onView(..).perform(..) does not work when views are animated.
            // Therefore this is using a posted task to turn off refreshing.
            mSwipeRefresh.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    mSwipeRefresh.setRefreshing(false);
                }
            });
            long waitTime = 1000;
            while (mSwipeRefresh.isRefreshing()) {
                Thread.sleep(20);
                waitTime -= 20;
                assertTrue("Timed out while waiting for SwipeRefreshLayout to stop refreshing",
                        waitTime > 0);
            }
        }
    }

    @Test
    @SmallTest
    public void testSetSize() throws Throwable {
        float density = mSwipeRefresh.getResources().getDisplayMetrics().density;
        assertEquals((int) (SwipeRefreshLayout.CIRCLE_DIAMETER * density),
                mSwipeRefresh.getProgressCircleDiameter());
        onView(withId(R.id.swipe_refresh)).perform(setSize(SwipeRefreshLayout.LARGE));
        assertEquals((int) (SwipeRefreshLayout.CIRCLE_DIAMETER_LARGE * density),
                mSwipeRefresh.getProgressCircleDiameter());
        onView(withId(R.id.swipe_refresh)).perform(setSize(SwipeRefreshLayout.DEFAULT));
        assertEquals((int) (SwipeRefreshLayout.CIRCLE_DIAMETER * density),
                mSwipeRefresh.getProgressCircleDiameter());
        onView(withId(R.id.swipe_refresh)).perform(setSize(SwipeRefreshLayout.DEFAULT));
    }
}
