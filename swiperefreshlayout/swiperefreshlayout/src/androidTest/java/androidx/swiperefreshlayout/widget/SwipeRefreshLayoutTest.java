/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.swiperefreshlayout.widget;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.view.ContextThemeWrapper;
import android.view.View;

import androidx.swiperefreshlayout.test.R;
import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.FlakyTest;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SmallTest;
import androidx.testutils.PollingCheck;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests SwipeRefreshLayout widget.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class SwipeRefreshLayoutTest {
    @SuppressWarnings("deprecation")
    @Rule
    public final androidx.test.rule.ActivityTestRule<SwipeRefreshLayoutActivity> mActivityTestRule =
            new androidx.test.rule.ActivityTestRule<>(SwipeRefreshLayoutActivity.class);

    private static final long TIMEOUT = 1000;
    private static final int INVALID_SIZE = 1000;

    private SwipeRefreshLayout mSwipeRefresh;

    @Before
    public void setUp() {
        mSwipeRefresh = mActivityTestRule.getActivity().findViewById(R.id.swipe_refresh);
    }

    @FlakyTest(bugId = 113347851)
    @Test
    @LargeTest
    public void testStartAndStopRefreshing() throws Throwable {
        SwipeRefreshLayout.OnRefreshListener mockListener =
                mock(SwipeRefreshLayout.OnRefreshListener.class);
        mSwipeRefresh.setOnRefreshListener(mockListener);

        assertFalse(mSwipeRefresh.isRefreshing());
        for (int i = 0; i < 5; i++) {
            onView(withId(R.id.swipe_refresh)).perform(SwipeRefreshLayoutActions.setRefreshing());
            assertTrue(mSwipeRefresh.isRefreshing());

            // onView(..).perform(..) does not work when views are animated.
            // Therefore this is using a posted task to turn off refreshing.
            mSwipeRefresh.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    mSwipeRefresh.setRefreshing(false);
                }
            });

            PollingCheck.waitFor(TIMEOUT, new PollingCheck.PollingCheckCondition() {
                @Override
                public boolean canProceed() {
                    return !mSwipeRefresh.isRefreshing();
                }
            });
        }
        verify(mockListener, times(0)).onRefresh();
    }

    @Test
    @SmallTest
    public void testStyles_setBackgroundColor() {
        mSwipeRefresh.setProgressBackgroundColorSchemeColor(0xFFFBFBFB);
        assertEquals(mSwipeRefresh.mCircleView.getBackgroundColor(), 0xFFFBFBFB);
    }

    @Test
    @SmallTest
    public void testStyles_defaultBackgroundColor() {
        assertEquals(mSwipeRefresh.mCircleView.getBackgroundColor(), 0xFFFAFAFA);
    }

    @Test
    @SmallTest
    public void testStyles_backgroundColor() {
        ContextThemeWrapper newContext = new ContextThemeWrapper(
                mSwipeRefresh.getContext(), R.style.TestActivityThemeWithBackground);
        SwipeRefreshLayout swipeRefreshLayout = new SwipeRefreshLayout(newContext);
        assertEquals(swipeRefreshLayout.mCircleView.getBackgroundColor(), 0xFF808080);
    }

    @Test
    @LargeTest
    public void testSwipeDownToRefresh() throws Throwable {
        assertFalse(mSwipeRefresh.isRefreshing());

        swipeToRefreshVerifyThenStopRefreshing(true);
    }

    @Test
    @SmallTest
    @FlakyTest(bugId = 113347851)
    public void testSetSize() throws Throwable {
        float density = mSwipeRefresh.getResources().getDisplayMetrics().density;
        assertEquals((int) (SwipeRefreshLayout.CIRCLE_DIAMETER * density),
                mSwipeRefresh.getProgressCircleDiameter());
        onView(withId(R.id.swipe_refresh)).perform(
                SwipeRefreshLayoutActions.setSize(SwipeRefreshLayout.LARGE));
        assertEquals((int) (SwipeRefreshLayout.CIRCLE_DIAMETER_LARGE * density),
                mSwipeRefresh.getProgressCircleDiameter());
        onView(withId(R.id.swipe_refresh)).perform(
                SwipeRefreshLayoutActions.setSize(SwipeRefreshLayout.DEFAULT));
        assertEquals((int) (SwipeRefreshLayout.CIRCLE_DIAMETER * density),
                mSwipeRefresh.getProgressCircleDiameter());
        onView(withId(R.id.swipe_refresh)).perform(
                SwipeRefreshLayoutActions.setSize(SwipeRefreshLayout.DEFAULT));
        onView(withId(R.id.swipe_refresh)).perform(SwipeRefreshLayoutActions.setSize(INVALID_SIZE));
        assertEquals((int) (SwipeRefreshLayout.CIRCLE_DIAMETER * density),
                mSwipeRefresh.getProgressCircleDiameter());
    }

    @Test
    @SmallTest
    public void testSetOnChildScrollUpCallback() throws Throwable {
        SwipeRefreshLayout.OnChildScrollUpCallback mockCallback =
                mock(SwipeRefreshLayout.OnChildScrollUpCallback.class);
        when(mockCallback.canChildScrollUp(eq(mSwipeRefresh), any(View.class)))
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(false)
                .thenReturn(false);
        mSwipeRefresh.setOnChildScrollUpCallback(mockCallback);
        assertTrue(mSwipeRefresh.canChildScrollUp());
        assertTrue(mSwipeRefresh.canChildScrollUp());
        assertFalse(mSwipeRefresh.canChildScrollUp());
        assertFalse(mSwipeRefresh.canChildScrollUp());
    }

    @Test
    @LargeTest
    public void testSwipeDownToRefreshInitiallyDisabled() throws Throwable {
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivityTestRule.getActivity().setContentView(
                        R.layout.swipe_refresh_layout_disabled_activity);
            }
        });
        mSwipeRefresh = (SwipeRefreshLayout) mActivityTestRule.getActivity().findViewById(
                R.id.swipe_refresh);

        assertFalse(mSwipeRefresh.isRefreshing());

        swipeToRefreshVerifyThenStopRefreshing(false);

        onView(withId(R.id.swipe_refresh)).perform(SwipeRefreshLayoutActions.setEnabled(true));

        swipeToRefreshVerifyThenStopRefreshing(true);
    }

    @Test
    public void testRefreshStatePersists() throws Throwable {

        assertFalse(mSwipeRefresh.isRefreshing());

        onView(withId(R.id.swipe_refresh)).perform(SwipeRefreshLayoutActions.setRefreshing());

        assertTrue(mSwipeRefresh.isRefreshing());

        final SwipeRefreshLayoutActivity activity = mActivityTestRule.getActivity();

        mSwipeRefresh.getHandler().post(new Runnable() {
            @Override
            public void run() {
                activity.recreate();
            }
        });

        PollingCheck.waitFor(TIMEOUT, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return activity != mActivityTestRule.getActivity();
            }
        });

        mSwipeRefresh = mActivityTestRule.getActivity().findViewById(R.id.swipe_refresh);

        assertTrue(mSwipeRefresh.isRefreshing());

    }

    private void swipeToRefreshVerifyThenStopRefreshing(boolean expectRefreshing) throws Throwable {
        final CountDownLatch latch = new CountDownLatch(1);
        SwipeRefreshLayout.OnRefreshListener listener = new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                latch.countDown();
                assertTrue(mSwipeRefresh.isRefreshing());
                mSwipeRefresh.setRefreshing(false);
            }
        };
        mSwipeRefresh.setOnRefreshListener(listener);
        onView(withId(R.id.content)).perform(ViewActions.swipeDown());
        if (expectRefreshing) {
            assertTrue("SwipeRefreshLayout never started refreshing",
                    latch.await(500, TimeUnit.MILLISECONDS));
        } else {
            assertFalse("SwipeRefreshLayout unexpectedly started refreshing",
                    latch.await(500, TimeUnit.MILLISECONDS));
        }
    }
}
