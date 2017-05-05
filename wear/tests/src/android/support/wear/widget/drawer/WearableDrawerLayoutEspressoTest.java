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

package android.support.wear.widget.drawer;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeDown;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static android.support.wear.widget.util.AsyncViewActions.waitForMatchingView;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.content.Intent;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.widget.RecyclerView;
import android.support.wear.test.R;
import android.support.wear.widget.drawer.DrawerTestActivity.DrawerStyle;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.ImageView;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Espresso tests for {@link WearableDrawerLayout}.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class WearableDrawerLayoutEspressoTest {

    private static final long MAX_WAIT_MS = 4000;

    @Rule public final ActivityTestRule<DrawerTestActivity> activityRule =
            new ActivityTestRule<>(
                    DrawerTestActivity.class, true /* touchMode */, false /* initialLaunch*/);

    private final Intent mSinglePageIntent =
            new DrawerTestActivity.Builder().setStyle(DrawerStyle.BOTH_DRAWER_NAV_SINGLE_PAGE)
                    .build();

    private static TypeSafeMatcher<View> isOpened(final boolean isOpened) {
        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("is opened == " + isOpened);
            }

            @Override
            public boolean matchesSafely(View view) {
                return ((WearableDrawerView) view).isOpened() == isOpened;
            }
        };
    }

    private static TypeSafeMatcher<View> isClosed(final boolean isClosed) {
        return new TypeSafeMatcher<View>() {
            @Override
            protected boolean matchesSafely(View view) {
                WearableDrawerView drawer = (WearableDrawerView) view;
                return drawer.isClosed() == isClosed;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("is closed");
            }
        };
    }

    @Test
    public void openingNavigationDrawerDoesNotCloseActionDrawer() {
        // GIVEN a drawer layout with a peeking action and navigation drawer
        activityRule.launchActivity(mSinglePageIntent);
        DrawerTestActivity activity = activityRule.getActivity();
        WearableDrawerView actionDrawer =
                (WearableDrawerView) activity.findViewById(R.id.action_drawer);
        WearableDrawerView navigationDrawer =
                (WearableDrawerView) activity.findViewById(R.id.navigation_drawer);
        assertTrue(actionDrawer.isPeeking());
        assertTrue(navigationDrawer.isPeeking());

        // WHEN the top drawer is opened
        openDrawer(navigationDrawer);
        onView(withId(R.id.navigation_drawer))
                .perform(
                        waitForMatchingView(
                                allOf(withId(R.id.navigation_drawer), isOpened(true)),
                                MAX_WAIT_MS));

        // THEN the action drawer should still be peeking
        assertTrue(actionDrawer.isPeeking());
    }

    @Test
    public void swipingDownNavigationDrawerDoesNotCloseActionDrawer() {
        // GIVEN a drawer layout with a peeking action and navigation drawer
        activityRule.launchActivity(mSinglePageIntent);
        onView(withId(R.id.action_drawer)).check(matches(isPeeking()));
        onView(withId(R.id.navigation_drawer)).check(matches(isPeeking()));

        // WHEN the top drawer is opened by swiping down
        onView(withId(R.id.drawer_layout)).perform(swipeDown());
        onView(withId(R.id.navigation_drawer))
                .perform(
                        waitForMatchingView(
                                allOf(withId(R.id.navigation_drawer), isOpened(true)),
                                MAX_WAIT_MS));

        // THEN the action drawer should still be peeking
        onView(withId(R.id.action_drawer)).check(matches(isPeeking()));
    }


    @Test
    public void firstNavDrawerItemShouldBeSelectedInitially() {
        // GIVEN a top drawer
        // WHEN it is first opened
        activityRule.launchActivity(mSinglePageIntent);
        onView(withId(R.id.drawer_layout)).perform(swipeDown());
        onView(withId(R.id.navigation_drawer))
                .perform(
                        waitForMatchingView(
                                allOf(withId(R.id.navigation_drawer), isOpened(true)),
                                MAX_WAIT_MS));

        // THEN the text should display "0".
        onView(withId(R.id.wearable_support_nav_drawer_text)).check(matches(withText("0")));
    }

    @Test
    public void selectingNavItemChangesTextAndClosedDrawer() {
        // GIVEN an open top drawer
        activityRule.launchActivity(mSinglePageIntent);
        onView(withId(R.id.drawer_layout)).perform(swipeDown());
        onView(withId(R.id.navigation_drawer))
                .perform(
                        waitForMatchingView(
                                allOf(withId(R.id.navigation_drawer), isOpened(true)),
                                MAX_WAIT_MS));

        // WHEN the second item is selected
        onView(withId(R.id.wearable_support_nav_drawer_icon_1)).perform(click());

        // THEN the text should display "1" and it should close.
        onView(withId(R.id.wearable_support_nav_drawer_text))
                .perform(
                        waitForMatchingView(
                                allOf(withId(R.id.wearable_support_nav_drawer_text), withText("1")),
                                MAX_WAIT_MS));
        onView(withId(R.id.navigation_drawer))
                .perform(
                        waitForMatchingView(
                                allOf(withId(R.id.navigation_drawer), isClosed(true)),
                                MAX_WAIT_MS));
    }

    @Test
    public void navDrawerShouldOpenWhenCalledInOnCreate() {
        // GIVEN an activity which calls openDrawer(Gravity.TOP) in onCreate
        // WHEN it is launched
        activityRule.launchActivity(
                new DrawerTestActivity.Builder()
                        .setStyle(DrawerStyle.BOTH_DRAWER_NAV_SINGLE_PAGE)
                        .openTopDrawerInOnCreate()
                        .build());

        // THEN the nav drawer should be open
        onView(withId(R.id.navigation_drawer)).check(matches(isOpened(true)));
    }

    @Test
    public void actionDrawerShouldOpenWhenCalledInOnCreate() {
        // GIVEN an activity with only an action drawer which is opened in onCreate
        // WHEN it is launched
        activityRule.launchActivity(
                new DrawerTestActivity.Builder()
                        .setStyle(DrawerStyle.ONLY_ACTION_DRAWER_WITH_TITLE)
                        .openBottomDrawerInOnCreate()
                        .build());

        // THEN the action drawer should be open
        onView(withId(R.id.action_drawer)).check(matches(isOpened(true)));
    }

    @Test
    public void navDrawerShouldOpenWhenCalledInOnCreateAndThenCloseWhenRequested() {
        // GIVEN an activity which calls openDrawer(Gravity.TOP) in onCreate, then closes it
        // WHEN it is launched
        activityRule.launchActivity(
                new DrawerTestActivity.Builder()
                        .setStyle(DrawerStyle.BOTH_DRAWER_NAV_SINGLE_PAGE)
                        .openTopDrawerInOnCreate()
                        .closeFirstDrawerOpened()
                        .build());

        // THEN the nav drawer should be open and then close
        onView(withId(R.id.navigation_drawer))
                .check(matches(isOpened(true)))
                .perform(
                        waitForMatchingView(
                                allOf(withId(R.id.navigation_drawer), isClosed(true)),
                                MAX_WAIT_MS));
    }

    @Test
    public void openedNavDrawerShouldPreventSwipeToClose() {
        // GIVEN an activity which calls openDrawer(Gravity.TOP) in onCreate
        activityRule.launchActivity(
                new DrawerTestActivity.Builder()
                        .setStyle(DrawerStyle.BOTH_DRAWER_NAV_SINGLE_PAGE)
                        .openTopDrawerInOnCreate()
                        .build());

        // THEN the view should prevent swipe to close
        onView(withId(R.id.navigation_drawer)).check(matches(not(allowsSwipeToClose())));
    }

    @Test
    public void closedNavDrawerShouldNotPreventSwipeToClose() {
        // GIVEN an activity which doesn't start with the nav drawer open
        activityRule.launchActivity(mSinglePageIntent);

        // THEN the view should allow swipe to close
        onView(withId(R.id.navigation_drawer)).check(matches(allowsSwipeToClose()));
    }

    @Test
    public void scrolledDownActionDrawerCanScrollUpWhenReOpened() {
        // GIVEN a freshly launched activity
        activityRule.launchActivity(mSinglePageIntent);
        WearableActionDrawerView actionDrawer =
                (WearableActionDrawerView) activityRule.getActivity()
                        .findViewById(R.id.action_drawer);
        RecyclerView recyclerView = (RecyclerView) actionDrawer.getDrawerContent();

        // WHEN the action drawer is opened and scrolled to the last item (Item 6)
        openDrawer(actionDrawer);
        scrollToPosition(recyclerView, 5);
        onView(withId(R.id.action_drawer))
                .perform(
                        waitForMatchingView(allOf(withId(R.id.action_drawer), isOpened(true)),
                                MAX_WAIT_MS))
                .perform(
                        waitForMatchingView(allOf(withText("Item 6"), isCompletelyDisplayed()),
                                MAX_WAIT_MS));
        // and then it is peeked
        peekDrawer(actionDrawer);
        onView(withId(R.id.action_drawer))
                .perform(waitForMatchingView(allOf(withId(R.id.action_drawer), isPeeking()),
                        MAX_WAIT_MS));
        // and re-opened
        openDrawer(actionDrawer);
        onView(withId(R.id.action_drawer))
                .perform(
                        waitForMatchingView(allOf(withId(R.id.action_drawer), isOpened(true)),
                                MAX_WAIT_MS));

        // THEN item 6 should be visible, but swiping down should scroll up, not close the drawer.
        onView(withText("Item 6")).check(matches(isDisplayed()));
        onView(withId(R.id.action_drawer)).perform(swipeDown()).check(matches(isOpened(true)));
    }

    @Test
    public void actionDrawerPeekIconShouldNotBeNull() {
        // GIVEN a drawer layout with a peeking action drawer whose menu is initialized in XML
        activityRule.launchActivity(mSinglePageIntent);
        DrawerTestActivity activity = activityRule.getActivity();
        ImageView peekIconView =
                (ImageView) activity
                        .findViewById(R.id.wearable_support_action_drawer_peek_action_icon);
        // THEN its peek icon should not be null
        assertNotNull(peekIconView.getDrawable());
    }

    @Test
    public void tappingActionDrawerPeekIconShouldTriggerFirstAction() {
        // GIVEN a drawer layout with a peeking action drawer, title, and mock click listener
        activityRule.launchActivity(
                new DrawerTestActivity.Builder()
                        .setStyle(DrawerStyle.ONLY_ACTION_DRAWER_WITH_TITLE)
                        .build());
        WearableActionDrawerView actionDrawer =
                (WearableActionDrawerView) activityRule.getActivity()
                        .findViewById(R.id.action_drawer);
        OnMenuItemClickListener mockClickListener = mock(OnMenuItemClickListener.class);
        actionDrawer.setOnMenuItemClickListener(mockClickListener);
        // WHEN the action drawer peek view is tapped
        onView(
                allOf(
                        withParent(withId(R.id.action_drawer)),
                        withId(R.id.wearable_support_drawer_view_peek_container)))
                .perform(click());
        // THEN its click listener should be notified
        verify(mockClickListener).onMenuItemClick(any(MenuItem.class));
    }

    @Test
    public void tappingActionDrawerPeekIconShouldTriggerFirstActionAfterItWasOpened() {
        // GIVEN a drawer layout with an open action drawer with a title, and mock click listener
        activityRule.launchActivity(
                new DrawerTestActivity.Builder()
                        .setStyle(DrawerStyle.ONLY_ACTION_DRAWER_WITH_TITLE)
                        .openBottomDrawerInOnCreate()
                        .build());
        WearableActionDrawerView actionDrawer =
                (WearableActionDrawerView) activityRule.getActivity()
                        .findViewById(R.id.action_drawer);
        OnMenuItemClickListener mockClickListener = mock(OnMenuItemClickListener.class);
        actionDrawer.setOnMenuItemClickListener(mockClickListener);

        // WHEN the action drawer is closed to its peek state and then tapped
        peekDrawer(actionDrawer);
        onView(withId(R.id.action_drawer))
                .perform(waitForMatchingView(allOf(withId(R.id.action_drawer), isPeeking()),
                        MAX_WAIT_MS));
        actionDrawer.getPeekContainer().callOnClick();

        // THEN its click listener should be notified
        verify(mockClickListener).onMenuItemClick(any(MenuItem.class));
    }

    private void scrollToPosition(final RecyclerView recyclerView, final int position) {
        recyclerView.post(new Runnable() {
            @Override
            public void run() {
                recyclerView.scrollToPosition(position);
            }
        });
    }

    private void peekDrawer(final WearableDrawerView drawer) {
        drawer.post(new Runnable() {
            @Override
            public void run() {
                drawer.getController().peekDrawer();
            }
        });
    }

    private void openDrawer(final WearableDrawerView drawer) {
        drawer.post(new Runnable() {
            @Override
            public void run() {
                drawer.getController().openDrawer();
            }
        });
    }

    private TypeSafeMatcher<View> isPeeking() {
        return new TypeSafeMatcher<View>() {
            @Override
            protected boolean matchesSafely(View view) {
                WearableDrawerView drawer = (WearableDrawerView) view;
                return drawer.isPeeking();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("is peeking");
            }
        };
    }

    private TypeSafeMatcher<View> allowsSwipeToClose() {
        return new TypeSafeMatcher<View>() {
            @Override
            protected boolean matchesSafely(View view) {
                return !view.canScrollHorizontally(-2) && !view.canScrollHorizontally(2);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("can be swiped closed");
            }
        };
    }
}
