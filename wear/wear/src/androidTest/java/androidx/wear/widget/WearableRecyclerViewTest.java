/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.wear.widget;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.wear.widget.util.AsyncViewActions.waitForMatchingView;
import static androidx.wear.widget.util.MoreViewAssertions.withNoVerticalScrollOffset;
import static androidx.wear.widget.util.MoreViewAssertions.withPositiveVerticalScrollOffset;

import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.res.Configuration;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.action.GeneralLocation;
import androidx.test.espresso.action.GeneralSwipeAction;
import androidx.test.espresso.action.Press;
import androidx.test.espresso.action.Swipe;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.wear.test.R;
import androidx.wear.widget.util.WakeLockRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class WearableRecyclerViewTest {

    private static final long MAX_WAIT_TIME = 10000;
    @Mock
    WearableRecyclerView.LayoutManager mMockChildLayoutManager;

    @Rule
    public final WakeLockRule wakeLock = new WakeLockRule();

    @Rule
    public final ActivityScenarioRule<WearableRecyclerViewTestActivity> mActivityRule =
            new ActivityScenarioRule<>(WearableRecyclerViewTestActivity.class);

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testCaseInitState() {
        mActivityRule.getScenario().onActivity(activity -> {
            WearableRecyclerView wrv = new WearableRecyclerView(activity);
            wrv.setLayoutManager(new WearableLinearLayoutManager(wrv.getContext()));

            assertFalse(wrv.isEdgeItemsCenteringEnabled());
            assertFalse(wrv.isCircularScrollingGestureEnabled());
            assertEquals(1.0f, wrv.getBezelFraction(), 0.01f);
            assertEquals(180.0f, wrv.getScrollDegreesPerScreen(), 0.01f);
        });
    }

    @Test
    public void testEdgeItemsCenteringOnAndOff() throws Throwable {

        mActivityRule.getScenario().onActivity(activity -> {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    WearableRecyclerView wrv =
                            (WearableRecyclerView) activity.findViewById(R.id.wrv);
                    wrv.setEdgeItemsCenteringEnabled(true);
                }
            });
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        mActivityRule.getScenario().onActivity(activity -> {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    WearableRecyclerView wrv =
                            (WearableRecyclerView) activity.findViewById(
                                    R.id.wrv);
                    View child = wrv.getChildAt(0);
                    assertNotNull("child", child);
                    Configuration configuration = activity.getResources().getConfiguration();
                    if (configuration.isScreenRound()) {
                        assertEquals((wrv.getHeight() - child.getHeight()) / 2, child.getTop());
                    } else {
                        assertEquals(0, child.getTop());
                    }
                }
            });
        });

        mActivityRule.getScenario().onActivity(activity -> {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    WearableRecyclerView wrv =
                            (WearableRecyclerView) activity.findViewById(
                                    R.id.wrv);
                    wrv.setEdgeItemsCenteringEnabled(false);
                }
            });
        });

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        mActivityRule.getScenario().onActivity(activity -> {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    WearableRecyclerView wrv =
                            (WearableRecyclerView) activity.findViewById(
                                    R.id.wrv);
                    View child = wrv.getChildAt(0);
                    assertNotNull("child", child);
                    assertEquals(0, child.getTop());

                }
            });
        });
    }

    @Test
    public void testEdgeItemsCenteringBeforeChildrenDrawn() throws Throwable {
        mActivityRule.getScenario().onActivity(activity -> {
            activity.runOnUiThread(new Runnable() {
                @Override
                @SuppressWarnings("unchecked")
                public void run() {
                    WearableRecyclerView wrv = (WearableRecyclerView) activity.findViewById(
                            R.id.wrv);
                    RecyclerView.Adapter<WearableRecyclerView.ViewHolder> adapter =
                            wrv.getAdapter();
                    wrv.setAdapter(null);
                    wrv.setEdgeItemsCenteringEnabled(true);
                    wrv.setAdapter(adapter);
                }
            });
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        mActivityRule.getScenario().onActivity(activity -> {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    WearableRecyclerView wrv =
                            (WearableRecyclerView) activity.findViewById(
                                    R.id.wrv);
                    // Verify the first child
                    View child = wrv.getChildAt(0);
                    assertNotNull("child", child);
                    Configuration configuration = activity.getResources().getConfiguration();
                    if (configuration.isScreenRound()) {
                        assertEquals((wrv.getHeight() - child.getHeight()) / 2, child.getTop());
                    } else {
                        assertEquals(0, child.getTop());
                    }
                }
            });
        });
    }

    @Test
    public void testCircularScrollingGesture() throws Throwable {
        onView(withId(R.id.wrv)).perform(swipeDownFromTopRight());
        assertNotScrolledY(R.id.wrv);
        mActivityRule.getScenario().onActivity(activity -> {
            final WearableRecyclerView wrv =
                    (WearableRecyclerView) activity.findViewById(
                            R.id.wrv);
            assertFalse(wrv.isCircularScrollingGestureEnabled());
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    WearableRecyclerView wrv = (WearableRecyclerView)
                            activity.findViewById(R.id.wrv);
                    wrv.setCircularScrollingGestureEnabled(true);
                }
            });
            assertTrue(wrv.isCircularScrollingGestureEnabled());
        });
        // Explicitly set the swipe to SLOW here to avoid problems with test failures on phone AVDs
        // with "Gesture navigation" enabled. This is not a particularly satisfactory fix to this
        // problem and ideally we should look to move these tests to use a watch AVD which should
        // not be susceptible to phone gesture issues. b/151202035 raised to track.
        onView(withId(R.id.wrv)).perform(swipeDownFromTopRightSlowly());
        assertScrolledY(R.id.wrv);
    }

    @Test
    public void testCurvedOffsettingHelper() throws Throwable {
        mActivityRule.getScenario().onActivity(activity -> {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    WearableRecyclerView wrv =
                            (WearableRecyclerView) activity.findViewById(
                                    R.id.wrv);
                    wrv.setLayoutManager(new WearableLinearLayoutManager(wrv.getContext()));
                }
            });
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        onView(withId(R.id.wrv)).perform(swipeDownFromTopRight());

        mActivityRule.getScenario().onActivity(activity -> {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    WearableRecyclerView wrv = (WearableRecyclerView) activity.findViewById(
                            R.id.wrv);
                    if (activity.getResources().getConfiguration().isScreenRound()) {
                        View child = wrv.getChildAt(0);
                        assertTrue(child.getLeft() > 0);
                    } else {
                        for (int i = 0; i < wrv.getChildCount(); i++) {
                            assertEquals(0, wrv.getChildAt(i).getLeft());
                        }
                    }
                }
            });
        });
    }
    private static ViewAction swipeDownFromTopRightSlowly() {
        return new GeneralSwipeAction(
                Swipe.SLOW, GeneralLocation.TOP_RIGHT,
                GeneralLocation.BOTTOM_RIGHT, Press.FINGER);
    }

    private static ViewAction swipeDownFromTopRight() {
        return new GeneralSwipeAction(
                Swipe.FAST, GeneralLocation.TOP_RIGHT,
                GeneralLocation.BOTTOM_RIGHT, Press.FINGER);
    }

    private void assertScrolledY(@IdRes int layoutId) {
        onView(withId(layoutId)).perform(waitForMatchingView(
                allOf(withId(layoutId), withPositiveVerticalScrollOffset()), MAX_WAIT_TIME));
    }

    private void assertNotScrolledY(@IdRes int layoutId) {
        onView(withId(layoutId)).perform(waitForMatchingView(
                allOf(withId(layoutId), withNoVerticalScrollOffset()), MAX_WAIT_TIME));
    }
}
