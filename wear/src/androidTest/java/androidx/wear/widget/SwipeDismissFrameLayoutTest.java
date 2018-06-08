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

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.swipeRight;
import static android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

import static androidx.wear.widget.util.AsyncViewActions.waitForMatchingView;
import static androidx.wear.widget.util.MoreViewAssertions.withPositiveVerticalScrollOffset;

import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Intent;
import android.graphics.RectF;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.action.GeneralLocation;
import android.support.test.espresso.action.GeneralSwipeAction;
import android.support.test.espresso.action.Press;
import android.support.test.espresso.action.Swipe;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.recyclerview.widget.RecyclerView;
import androidx.wear.test.R;
import androidx.wear.widget.util.ArcSwipe;
import androidx.wear.widget.util.WakeLockRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SwipeDismissFrameLayoutTest {

    private static final long MAX_WAIT_TIME = 4000; //ms
    private final SwipeDismissFrameLayout.Callback mDismissCallback = new DismissCallback();

    @Rule
    public final WakeLockRule wakeLock = new WakeLockRule();

    @Rule
    public final ActivityTestRule<SwipeDismissFrameLayoutTestActivity> activityRule =
            new ActivityTestRule<>(
                    SwipeDismissFrameLayoutTestActivity.class,
                    true, /** initial touch mode */
                    false /** launchActivity */
            );

    private int mLayoutWidth;
    private int mLayoutHeight;

    @Test
    @SmallTest
    public void testCanScrollHorizontally() {
        // GIVEN a freshly setup SwipeDismissFrameLayout
        setUpSimpleLayout();
        Activity activity = activityRule.getActivity();
        SwipeDismissFrameLayout testLayout =
                (SwipeDismissFrameLayout) activity.findViewById(R.id.swipe_dismiss_root);
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
    }

    @Test
    @SmallTest
    public void canScrollHorizontallyShouldBeFalseWhenInvisible() {
        // GIVEN a freshly setup SwipeDismissFrameLayout
        setUpSimpleLayout();
        Activity activity = activityRule.getActivity();
        final SwipeDismissFrameLayout testLayout = activity.findViewById(R.id.swipe_dismiss_root);
        // GIVEN the layout is invisible
        // Note: We have to run this on the main thread, because of thread checks in View.java.
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                testLayout.setVisibility(View.INVISIBLE);
            }
        });
        // WHEN we check that the layout is horizontally scrollable
        // THEN the layout is found to be NOT horizontally swipeable from left to right.
        assertFalse(testLayout.canScrollHorizontally(-20));
        // AND the layout is found to NOT be horizontally swipeable from right to left.
        assertFalse(testLayout.canScrollHorizontally(20));
    }

    @Test
    @SmallTest
    public void canScrollHorizontallyShouldBeFalseWhenGone() {
        // GIVEN a freshly setup SwipeDismissFrameLayout
        setUpSimpleLayout();
        Activity activity = activityRule.getActivity();
        final SwipeDismissFrameLayout testLayout =
                (SwipeDismissFrameLayout) activity.findViewById(R.id.swipe_dismiss_root);
        // GIVEN the layout is gone
        // Note: We have to run this on the main thread, because of thread checks in View.java.
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                testLayout.setVisibility(View.GONE);
            }
        });
        // WHEN we check that the layout is horizontally scrollable
        // THEN the layout is found to be NOT horizontally swipeable from left to right.
        assertFalse(testLayout.canScrollHorizontally(-20));
        // AND the layout is found to NOT be horizontally swipeable from right to left.
        assertFalse(testLayout.canScrollHorizontally(20));
    }

    @Test
    @SmallTest
    public void testSwipeDismissEnabledByDefault() {
        // GIVEN a freshly setup SwipeDismissFrameLayout
        setUpSimpleLayout();
        Activity activity = activityRule.getActivity();
        SwipeDismissFrameLayout testLayout =
                (SwipeDismissFrameLayout) activity.findViewById(R.id.swipe_dismiss_root);
        // WHEN we check that the layout is dismissible
        // THEN the layout is find to be dismissible
        assertTrue(testLayout.isSwipeable());
    }

    @Test
    @SmallTest
    public void testSwipeDismissesViewIfEnabled() {
        // GIVEN a freshly setup SwipeDismissFrameLayout
        setUpSimpleLayout();
        // WHEN we perform a swipe to dismiss
        onView(withId(R.id.swipe_dismiss_root)).perform(swipeRight());
        // AND hidden
        assertHidden(R.id.swipe_dismiss_root);
    }

    @Test
    @SmallTest
    public void testSwipeDoesNotDismissViewIfDisabled() {
        // GIVEN a freshly setup SwipeDismissFrameLayout with dismiss turned off.
        setUpSimpleLayout();
        Activity activity = activityRule.getActivity();
        SwipeDismissFrameLayout testLayout =
                (SwipeDismissFrameLayout) activity.findViewById(R.id.swipe_dismiss_root);
        testLayout.setSwipeable(false);
        // WHEN we perform a swipe to dismiss
        onView(withId(R.id.swipe_dismiss_root)).perform(swipeRight());
        // THEN the layout is not hidden
        assertNotHidden(R.id.swipe_dismiss_root);
    }

    @Test
    @SmallTest
    public void testAddRemoveCallback() {
        // GIVEN a freshly setup SwipeDismissFrameLayout
        setUpSimpleLayout();
        Activity activity = activityRule.getActivity();
        SwipeDismissFrameLayout testLayout = activity.findViewById(R.id.swipe_dismiss_root);
        // WHEN we remove the swipe callback
        testLayout.removeCallback(mDismissCallback);
        onView(withId(R.id.swipe_dismiss_root)).perform(swipeRight());
        // THEN the layout is not hidden
        assertNotHidden(R.id.swipe_dismiss_root);
    }

    @Test
    @SmallTest
    public void testSwipeDoesNotDismissViewIfScrollable() throws Throwable {
        // GIVEN a freshly setup SwipeDismissFrameLayout with dismiss turned off.
        setUpSwipeDismissWithHorizontalRecyclerView();
        activityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Activity activity = activityRule.getActivity();
                RecyclerView testLayout = activity.findViewById(R.id.recycler_container);
                // Scroll to a position from which the child is scrollable.
                testLayout.scrollToPosition(50);
            }
        });

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        // WHEN we perform a swipe to dismiss from the center of the screen.
        onView(withId(R.id.swipe_dismiss_root)).perform(swipeRightFromCenter());
        // THEN the layout is not hidden
        assertNotHidden(R.id.swipe_dismiss_root);
    }


    @Test
    @SmallTest
    public void testEdgeSwipeDoesDismissViewIfScrollable() {
        // GIVEN a freshly setup SwipeDismissFrameLayout with dismiss turned off.
        setUpSwipeDismissWithHorizontalRecyclerView();
        // WHEN we perform a swipe to dismiss from the left edge of the screen.
        onView(withId(R.id.swipe_dismiss_root)).perform(swipeRightFromLeftEdge());
        // THEN the layout is hidden
        assertHidden(R.id.swipe_dismiss_root);
    }

    @Test
    @SmallTest
    public void testSwipeDoesNotDismissViewIfStartsInWrongPosition() {
        // GIVEN a freshly setup SwipeDismissFrameLayout with dismiss turned on, but only for an
        // inner circle.
        setUpSwipeableRegion();
        // WHEN we perform a swipe to dismiss from the left edge of the screen.
        onView(withId(R.id.swipe_dismiss_root)).perform(swipeRightFromLeftEdge());
        // THEN the layout is not not hidden
        assertNotHidden(R.id.swipe_dismiss_root);
    }

    @Test
    @SmallTest
    public void testSwipeDoesDismissViewIfStartsInRightPosition() {
        // GIVEN a freshly setup SwipeDismissFrameLayout with dismiss turned on, but only for an
        // inner circle.
        setUpSwipeableRegion();
        // WHEN we perform a swipe to dismiss from the center of the screen.
        onView(withId(R.id.swipe_dismiss_root)).perform(swipeRightFromCenter());
        // THEN the layout is hidden
        assertHidden(R.id.swipe_dismiss_root);
    }

    /**
     @Test public void testSwipeInPreferenceFragmentAndNavDrawer() {
     // GIVEN a freshly setup SwipeDismissFrameLayout with dismiss turned on, but only for an inner
     // circle.
     setUpPreferenceFragmentAndNavDrawer();
     // WHEN we perform a swipe to dismiss from the center of the screen to the bottom.
     onView(withId(R.id.drawer_layout)).perform(swipeBottomFromCenter());
     // THEN the navigation drawer is shown.
     assertPeeking(R.id.top_drawer);
     }*/

    @Test
    @SmallTest
    public void testArcSwipeDoesNotTriggerDismiss() throws Throwable {
        // GIVEN a freshly setup SwipeDismissFrameLayout with vertically scrollable content
        setUpSwipeDismissWithVerticalRecyclerView();
        int center = mLayoutHeight / 2;
        int halfBound = mLayoutWidth / 2;
        RectF bounds = new RectF(0, center - halfBound, mLayoutWidth, center + halfBound);
        // WHEN the view is scrolled on an arc from top to bottom.
        onView(withId(R.id.swipe_dismiss_root)).perform(swipeTopFromBottomOnArc(bounds));
        // THEN the layout is not dismissed and not hidden.
        assertNotHidden(R.id.swipe_dismiss_root);
        // AND the content view is scrolled.
        assertScrolledY(R.id.recycler_container);
    }

    /**
     * Set ups the simplest possible layout for test cases - a {@link SwipeDismissFrameLayout} with
     * a single static child.
     */
    private void setUpSimpleLayout() {
        activityRule.launchActivity(
                new Intent()
                        .putExtra(
                                LayoutTestActivity.EXTRA_LAYOUT_RESOURCE_ID,
                                R.layout.swipe_dismiss_layout_testcase_1));
        setDismissCallback();
    }


    /**
     * Sets up a slightly more involved layout for testing swipe-to-dismiss with scrollable
     * containers. This layout contains a {@link SwipeDismissFrameLayout} with a horizontal {@link
     * RecyclerView} as a child, ready to accept an adapter.
     */
    private void setUpSwipeDismissWithHorizontalRecyclerView() {
        Intent launchIntent = new Intent();
        launchIntent.putExtra(LayoutTestActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.swipe_dismiss_layout_testcase_2);
        launchIntent.putExtra(SwipeDismissFrameLayoutTestActivity.EXTRA_LAYOUT_HORIZONTAL, true);
        activityRule.launchActivity(launchIntent);
        setDismissCallback();
    }

    /**
     * Sets up a slightly more involved layout for testing swipe-to-dismiss with scrollable
     * containers. This layout contains a {@link SwipeDismissFrameLayout} with a vertical {@link
     * WearableRecyclerView} as a child, ready to accept an adapter.
     */
    private void setUpSwipeDismissWithVerticalRecyclerView() {
        Intent launchIntent = new Intent();
        launchIntent.putExtra(LayoutTestActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.swipe_dismiss_layout_testcase_2);
        launchIntent.putExtra(SwipeDismissFrameLayoutTestActivity.EXTRA_LAYOUT_HORIZONTAL, false);
        activityRule.launchActivity(launchIntent);
        setDismissCallback();
    }

    /**
     * Sets up a {@link SwipeDismissFrameLayout} in which only a certain region is allowed to react
     * to swipe-dismiss gestures.
     */
    private void setUpSwipeableRegion() {
        activityRule.launchActivity(
                new Intent()
                        .putExtra(
                                LayoutTestActivity.EXTRA_LAYOUT_RESOURCE_ID,
                                R.layout.swipe_dismiss_layout_testcase_1));
        setCallback(
                new DismissCallback() {
                    @Override
                    public boolean onPreSwipeStart(SwipeDismissFrameLayout layout, float x,
                            float y) {
                        float normalizedX = x - mLayoutWidth / 2;
                        float normalizedY = y - mLayoutWidth / 2;
                        float squareX = normalizedX * normalizedX;
                        float squareY = normalizedY * normalizedY;
                        // 30 is an arbitrary number limiting the circle.
                        return Math.sqrt(squareX + squareY) < (mLayoutWidth / 2 - 30);
                    }
                });
    }

    /**
     * Sets up a more involved test case where the layout consists of a
     * {@code WearableNavigationDrawer} and a
     * {@code androidx.wear.internal.view.SwipeDismissPreferenceFragment}
     */
  /*
  private void setUpPreferenceFragmentAndNavDrawer() {
    activityRule.launchActivity(
      new Intent()
          .putExtra(
              LayoutTestActivity.EXTRA_LAYOUT_RESOURCE_ID,
              R.layout.swipe_dismiss_layout_testcase_3));
    Activity activity = activityRule.getActivity();
    InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
      WearableNavigationDrawer wearableNavigationDrawer =
              (WearableNavigationDrawer) activity.findViewById(R.id.top_drawer);
      wearableNavigationDrawer.setAdapter(
              new WearableNavigationDrawer.WearableNavigationDrawerAdapter() {
                @Override
                public String getItemText(int pos) {
                  return "test";
                }

                @Override
                public Drawable getItemDrawable(int pos) {
                  return null;
                }

                @Override
                public void onItemSelected(int pos) {
                  return;
                }

                @Override
                public int getCount() {
                  return 3;
                }
              });
    });
  }*/
    private void setDismissCallback() {
        setCallback(mDismissCallback);
    }

    private void setCallback(SwipeDismissFrameLayout.Callback callback) {
        Activity activity = activityRule.getActivity();
        SwipeDismissFrameLayout testLayout = activity.findViewById(R.id.swipe_dismiss_root);
        mLayoutWidth = testLayout.getWidth();
        mLayoutHeight = testLayout.getHeight();
        testLayout.addCallback(callback);
    }

    /**
     * private static void assertPeeking(@IdRes int layoutId) {
     * onView(withId(layoutId))
     * .perform(
     * waitForMatchingView(
     * allOf(withId(layoutId), isOpened(true)), MAX_WAIT_TIME));
     * }
     */

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

    private static ViewAction swipeRightFromLeftEdge() {
        return new GeneralSwipeAction(
                Swipe.SLOW, GeneralLocation.CENTER_LEFT, GeneralLocation.CENTER_RIGHT,
                Press.FINGER);
    }

    private static ViewAction swipeTopFromBottomOnArc(RectF bounds) {
        return new GeneralSwipeAction(
                new ArcSwipe(ArcSwipe.Gesture.SLOW_ANTICLOCKWISE, bounds),
                GeneralLocation.BOTTOM_CENTER,
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
