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
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.wear.widget.util.AsyncViewActions.waitForMatchingView;
import static androidx.wear.widget.util.MoreViewAssertions.withPositiveVerticalScrollOffset;

import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.graphics.RectF;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.action.GeneralLocation;
import androidx.test.espresso.action.GeneralSwipeAction;
import androidx.test.espresso.action.Press;
import androidx.test.espresso.action.Swipe;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.FlakyTest;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.wear.test.R;
import androidx.wear.widget.util.ArcSwipe;
import androidx.wear.widget.util.FrameLocationAvoidingEdges;
import androidx.wear.widget.util.WakeLockRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class SwipeDismissFrameLayoutTest {

    private static final long MAX_WAIT_TIME = 4000; //ms

    private final SwipeDismissFrameLayout.Callback mDismissCallback = new DismissCallback();

    @Rule
    public final WakeLockRule wakeLock = new WakeLockRule();

    private int mLayoutWidth;
    private int mLayoutHeight;
    private int mXPositionOnScreen;
    private int mYPositionOnScreen;

    @Test
    public void testCanScrollHorizontally() {
        // GIVEN a freshly setup SwipeDismissFrameLayout
        try (ActivityScenario<DismissibleFrameLayoutTestActivity> scenario =
                     ActivityScenario.launch(createSimpleLayoutLaunchIntent())) {
            scenario.onActivity(activity -> {
                SwipeDismissFrameLayout testLayout =
                        (SwipeDismissFrameLayout) activity.findViewById(R.id.swipe_dismiss_root);
                testLayout.setSwipeable(true);
                // WHEN we check that the layout is horizontally scrollable from left to right.
                // THEN the layout is found to be horizontally swipeable from left to right.
                assertTrue(testLayout.canScrollHorizontally(-20));
                // AND the layout is found to NOT be horizontally swipeable from right to left.
                assertFalse(testLayout.canScrollHorizontally(20));

                // WHEN we switch off the swipe-to-dismiss functionality for the layout
                testLayout.setSwipeable(false);
                // THEN the layout is found NOT to be horizontally swipeable from left to right.
                assertFalse(testLayout.canScrollHorizontally(-20));
                // AND the layout is found to NOT be horizontally swipeable from right to left.
                assertFalse(testLayout.canScrollHorizontally(20));
            });
        }
    }

    @Test
    public void canScrollHorizontallyShouldBeFalseWhenInvisible() {
        // GIVEN a freshly setup SwipeDismissFrameLayout
        try (ActivityScenario<DismissibleFrameLayoutTestActivity> scenario =
                     ActivityScenario.launch(createSimpleLayoutLaunchIntent())) {
            final SwipeDismissFrameLayout[] testLayout = new SwipeDismissFrameLayout[1];
            scenario.onActivity(activity -> {
                testLayout[0] =
                        (SwipeDismissFrameLayout) activity.findViewById(R.id.swipe_dismiss_root);
            });
            // GIVEN the layout is invisible
            // Note: We have to run this on the main thread, because of thread checks in View.java.
            InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    testLayout[0].setVisibility(View.INVISIBLE);
                }
            });
            // WHEN we check that the layout is horizontally scrollable
            // THEN the layout is found to be NOT horizontally swipeable from left to right.
            assertFalse(testLayout[0].canScrollHorizontally(-20));
            // AND the layout is found to NOT be horizontally swipeable from right to left.
            assertFalse(testLayout[0].canScrollHorizontally(20));
        }
    }

    @Test
    public void canScrollHorizontallyShouldBeFalseWhenGone() {
        // GIVEN a freshly setup SwipeDismissFrameLayout
        try (ActivityScenario<DismissibleFrameLayoutTestActivity> scenario =
                     ActivityScenario.launch(createSimpleLayoutLaunchIntent())) {
            final SwipeDismissFrameLayout[] testLayout = new SwipeDismissFrameLayout[1];
            scenario.onActivity(activity -> {
                testLayout[0] =
                        (SwipeDismissFrameLayout) activity.findViewById(R.id.swipe_dismiss_root);
            });
            // GIVEN the layout is gone
            // Note: We have to run this on the main thread, because of thread checks in View.java.
            InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    testLayout[0].setVisibility(View.GONE);
                }
            });
            // WHEN we check that the layout is horizontally scrollable
            // THEN the layout is found to be NOT horizontally swipeable from left to right.
            assertFalse(testLayout[0].canScrollHorizontally(-20));
            // AND the layout is found to NOT be horizontally swipeable from right to left.
            assertFalse(testLayout[0].canScrollHorizontally(20));
        }
    }

    @Test
    public void testSwipeDismissDisabledByDefault() {
        // GIVEN a freshly setup SwipeDismissFrameLayout
        try (ActivityScenario<DismissibleFrameLayoutTestActivity> scenario =
                     ActivityScenario.launch(createSimpleLayoutLaunchIntent())) {
            scenario.onActivity(activity -> {
                SwipeDismissFrameLayout testLayout =
                        (SwipeDismissFrameLayout) activity.findViewById(R.id.swipe_dismiss_root);
                // WHEN we check that the layout is dismissible
                // THEN the layout is find to be dismissible
                assertFalse(testLayout.isSwipeable());
            });
        }
    }

    @Test
    public void testSwipeDismissesViewIfEnabled() {
        // GIVEN a freshly setup SwipeDismissFrameLayout
        try (ActivityScenario<DismissibleFrameLayoutTestActivity> scenario =
                     ActivityScenario.launch(createSimpleLayoutLaunchIntent())) {
            setUpSwipeableAndCallback(scenario, true);
            // WHEN we perform a swipe to dismiss
            onView(withId(R.id.swipe_dismiss_root)).perform(swipeRightFromLeftCenterAvoidingEdge());
            // AND hidden
            assertHidden(R.id.swipe_dismiss_root);
        }
    }

    @Test
    public void testSwipeDoesNotDismissViewIfDisabled() {
        // GIVEN a freshly setup SwipeDismissFrameLayout with dismiss turned off.
        try (ActivityScenario<DismissibleFrameLayoutTestActivity> scenario =
                     ActivityScenario.launch(createSimpleLayoutLaunchIntent())) {
            setUpSwipeableAndCallback(scenario, false);
            // WHEN we perform a swipe to dismiss
            onView(withId(R.id.swipe_dismiss_root)).perform(swipeRightFromLeftCenterAvoidingEdge());
            // THEN the layout is not hidden
            assertNotHidden(R.id.swipe_dismiss_root);
        }
    }

    @Test
    public void testAddRemoveCallback() {
        // GIVEN a freshly setup SwipeDismissFrameLayout
        try (ActivityScenario<DismissibleFrameLayoutTestActivity> scenario =
                     ActivityScenario.launch(createSimpleLayoutLaunchIntent())) {
            setUpSwipeableAndCallback(scenario, true);
            // WHEN we remove the swipe callback
            scenario.onActivity(activity -> {
                SwipeDismissFrameLayout testLayout =
                        (SwipeDismissFrameLayout) activity.findViewById(R.id.swipe_dismiss_root);
                testLayout.removeCallback(mDismissCallback);
            });
            onView(withId(R.id.swipe_dismiss_root)).perform(swipeRightFromLeftCenterAvoidingEdge());
            // THEN the layout is not hidden
            assertNotHidden(R.id.swipe_dismiss_root);
        }
    }

    @Test
    public void testSwipeDoesNotDismissViewIfScrollable() throws Throwable {
        // GIVEN a freshly setup SwipeDismissFrameLayout with dismiss turned off.
        try (ActivityScenario<DismissibleFrameLayoutTestActivity> scenario =
                     ActivityScenario.launch(
                             createSwipeDismissWithHorizontalRecyclerViewLaunchIntent()
                     )) {
            setUpSwipeableAndCallback(scenario, true);
            scenario.onActivity(activity -> {
                SwipeDismissFrameLayout testLayout =
                        (SwipeDismissFrameLayout) activity.findViewById(R.id.swipe_dismiss_root);
                RecyclerView testRecyclerView = activity.findViewById(R.id.recycler_container);
                testRecyclerView.scrollToPosition(50);
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            // WHEN we perform a swipe to dismiss from the center of the screen.
            onView(withId(R.id.swipe_dismiss_root)).perform(swipeRightFromCenter());
            // THEN the layout is not hidden
            assertNotHidden(R.id.swipe_dismiss_root);
        }
    }


    @Test
    public void testEdgeSwipeDoesDismissViewIfScrollable() {
        // GIVEN a freshly setup SwipeDismissFrameLayout with dismiss turned off.
        try (ActivityScenario<DismissibleFrameLayoutTestActivity> scenario =
                     ActivityScenario.launch(
                             createSwipeDismissWithHorizontalRecyclerViewLaunchIntent()
                     )) {
            setUpSwipeableAndCallback(scenario, true);
            // WHEN we perform a swipe to dismiss from the left edge of the screen.
            onView(withId(R.id.swipe_dismiss_root)).perform(swipeRightFromLeftCenterAvoidingEdge());
            // THEN the layout is hidden
            assertHidden(R.id.swipe_dismiss_root);
        }
    }

    @Test
    @FlakyTest
    public void testArcSwipeDoesNotTriggerDismiss() {
        // GIVEN a freshly setup SwipeDismissFrameLayout with vertically scrollable content
        try (ActivityScenario<DismissibleFrameLayoutTestActivity> scenario =
                     ActivityScenario.launch(
                             createSwipeDismissWithVerticalRecyclerViewLaunchIntent()
                     )) {
            setUpSwipeableAndCallback(scenario, true);
            int center = mLayoutHeight / 2;
            int halfBound = mLayoutWidth / 2;
            RectF bounds = new RectF(0, center - halfBound, mLayoutWidth, center + halfBound);
            // WHEN the view is scrolled on an arc from top to bottom.
            onView(withId(R.id.swipe_dismiss_root)).perform(
                    swipeTopFromBottomOnArcAvoidingEdge(bounds));
            // THEN the layout is not dismissed and not hidden.
            assertNotHidden(R.id.swipe_dismiss_root);
            // AND the content view is scrolled.
            assertScrolledY(R.id.recycler_container);
        }
    }

    /**
     * Creates intent for launching the simplest possible layout for test cases - a
     * {@link SwipeDismissFrameLayout} with a single static child.
     */
    private Intent createSimpleLayoutLaunchIntent() {
        return new Intent()
                .setClass(ApplicationProvider.getApplicationContext(),
                        DismissibleFrameLayoutTestActivity.class)
                .putExtra(
                        LayoutTestActivity.EXTRA_LAYOUT_RESOURCE_ID,
                        R.layout.swipe_dismiss_layout_testcase_1);
    }


    /**
     * Creates intent for launching a slightly more involved layout for testing swipe-to-dismiss
     * with scrollable containers. This layout contains a {@link SwipeDismissFrameLayout} with a
     * horizontal {@link RecyclerView} as a child, ready to accept an adapter.
     */
    private Intent createSwipeDismissWithHorizontalRecyclerViewLaunchIntent() {
        return new Intent()
                .setClass(ApplicationProvider.getApplicationContext(),
                        DismissibleFrameLayoutTestActivity.class)
                .putExtra(
                        LayoutTestActivity.EXTRA_LAYOUT_RESOURCE_ID,
                        R.layout.swipe_dismiss_layout_testcase_2)
                .putExtra(DismissibleFrameLayoutTestActivity.EXTRA_LAYOUT_HORIZONTAL, true);
    }

    /**
     * Creates intent for launching slightly more involved layout for testing swipe-to-dismiss
     * with scrollable containers. This layout contains a {@link SwipeDismissFrameLayout} with a
     * vertical {@link WearableRecyclerView} as a child, ready to accept an adapter.
     */
    private Intent createSwipeDismissWithVerticalRecyclerViewLaunchIntent() {
        Intent launchIntent = new Intent()
                .setClass(ApplicationProvider.getApplicationContext(),
                        DismissibleFrameLayoutTestActivity.class)
                .putExtra(
                        LayoutTestActivity.EXTRA_LAYOUT_RESOURCE_ID,
                        R.layout.swipe_dismiss_layout_testcase_2);
        launchIntent.putExtra(DismissibleFrameLayoutTestActivity.EXTRA_LAYOUT_HORIZONTAL, false);

        return launchIntent;
    }

    private void setDismissCallback(SwipeDismissFrameLayout testLayout) {
        int[] locationOnScreen = new int[2];
        testLayout.getLocationOnScreen(locationOnScreen);
        mXPositionOnScreen = locationOnScreen[0];
        mYPositionOnScreen = locationOnScreen[1];
        mLayoutWidth = testLayout.getWidth();
        mLayoutHeight = testLayout.getHeight();
        testLayout.addCallback(mDismissCallback);
    }

    private void setUpSwipeableAndCallback(
            ActivityScenario<DismissibleFrameLayoutTestActivity> scenario,
            boolean swipeable
    ) {
        scenario.onActivity(activity -> {
            SwipeDismissFrameLayout testLayout =
                    (SwipeDismissFrameLayout) activity.findViewById(R.id.swipe_dismiss_root);
            setDismissCallback(testLayout);
            testLayout.setSwipeable(swipeable);
        });
    }

    private static void assertHidden(@IdRes int layoutId) {
        onView(withId(layoutId))
                .perform(
                        waitForMatchingView(
                                allOf(withId(layoutId),
                                        withEffectiveVisibility(ViewMatchers.Visibility.GONE)),
                                MAX_WAIT_TIME));
    }

    private static void assertNotHidden(@IdRes int layoutId) {
        onView(withId(layoutId))
                .perform(
                        waitForMatchingView(
                                allOf(withId(layoutId),
                                        withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)),
                                MAX_WAIT_TIME));
    }

    private static void assertScrolledY(@IdRes int layoutId) {
        onView(withId(layoutId))
                .perform(
                        waitForMatchingView(
                                allOf(withId(layoutId), withPositiveVerticalScrollOffset()),
                                MAX_WAIT_TIME));
    }

    private static ViewAction swipeRightFromCenter() {
        return new GeneralSwipeAction(
                Swipe.SLOW, GeneralLocation.CENTER, GeneralLocation.CENTER_RIGHT, Press.FINGER);
    }


    private static ViewAction swipeRightFromLeftCenterAvoidingEdge() {
        return new GeneralSwipeAction(
                Swipe.SLOW, FrameLocationAvoidingEdges.CENTER_LEFT_AVOIDING_EDGE,
                GeneralLocation.CENTER_RIGHT,
                Press.FINGER);
    }

    private static ViewAction swipeTopFromBottomOnArcAvoidingEdge(RectF bounds) {
        return new GeneralSwipeAction(
                new ArcSwipe(ArcSwipe.Gesture.SLOW_ANTICLOCKWISE, bounds),
                FrameLocationAvoidingEdges.BOTTOM_CENTER_AVOIDING_EDGE,
                GeneralLocation.TOP_CENTER,
                Press.FINGER);
    }

    /** Helper class hiding the view after a successful swipe-to-dismiss. */
    private static class DismissCallback extends SwipeDismissFrameLayout.Callback {

        @Override
        public void onDismissed(SwipeDismissFrameLayout layout) {
            layout.setVisibility(View.GONE);
        }
    }
}
