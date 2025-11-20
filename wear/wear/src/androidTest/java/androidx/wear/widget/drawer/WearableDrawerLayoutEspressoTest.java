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

package androidx.wear.widget.drawer;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.swipeDown;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.wear.widget.util.AsyncViewActions.waitForMatchingView;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.util.HumanReadables;
import androidx.test.espresso.util.TreeIterables;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.rule.ActivityTestRule;
import androidx.wear.test.R;
import androidx.wear.widget.drawer.DrawerTestActivity.DrawerStyle;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.concurrent.TimeoutException;

import javax.annotation.Nullable;

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

    @Rule public final MockitoRule mocks = MockitoJUnit.rule();

    @Mock WearableNavigationDrawerView.OnItemSelectedListener mNavDrawerItemSelectedListener;

    @SdkSuppress(maxSdkVersion = 34) // b/427565061
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

    @SdkSuppress(maxSdkVersion = 34) // b/427565061
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

    @SdkSuppress(maxSdkVersion = 34) // b/427565061
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
        onView(withId(androidx.wear.R.id.ws_nav_drawer_text)).check(matches(withText("0")));
    }

    @SdkSuppress(maxSdkVersion = 33) // b/322538394
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
        onView(withId(androidx.wear.R.id.ws_nav_drawer_icon_1)).perform(click());

        // THEN the text should display "1" and it should close.
        onView(withId(androidx.wear.R.id.ws_nav_drawer_text))
                .perform(
                        waitForMatchingView(
                                allOf(withId(androidx.wear.R.id.ws_nav_drawer_text), withText("1")),
                                MAX_WAIT_MS));
        onView(withId(R.id.navigation_drawer))
                .perform(
                        waitForMatchingView(
                                allOf(withId(R.id.navigation_drawer), isClosed(true)),
                                MAX_WAIT_MS));
    }

    @Test
    public void programmaticallySelectingNavItemChangesTextInSinglePage() {
        // GIVEN an open top drawer
        activityRule.launchActivity(new DrawerTestActivity.Builder()
                .setStyle(DrawerStyle.BOTH_DRAWER_NAV_SINGLE_PAGE)
                .openTopDrawerInOnCreate()
                .build());
        final WearableNavigationDrawerView navDrawer =
                activityRule.getActivity().findViewById(R.id.navigation_drawer);
        navDrawer.addOnItemSelectedListener(mNavDrawerItemSelectedListener);

        // WHEN the second item is selected programmatically
        selectNavItem(navDrawer, 1);

        // THEN the text should display "1" and the listener should be notified.
        onView(withId(androidx.wear.R.id.ws_nav_drawer_text))
                .check(matches(withText("1")));
        verify(mNavDrawerItemSelectedListener).onItemSelected(1);
    }

    @Test
    public void programmaticallySelectingNavItemChangesTextInMultiPage() {
        // GIVEN an open top drawer
        activityRule.launchActivity(new DrawerTestActivity.Builder()
                .setStyle(DrawerStyle.BOTH_DRAWER_NAV_MULTI_PAGE)
                .openTopDrawerInOnCreate()
                .build());
        final WearableNavigationDrawerView navDrawer =
                activityRule.getActivity().findViewById(R.id.navigation_drawer);
        navDrawer.addOnItemSelectedListener(mNavDrawerItemSelectedListener);

        // WHEN the second item is selected programmatically
        selectNavItem(navDrawer, 1);

        // THEN the text should display "1" and the listener should be notified.
        onView(allOf(withId(androidx.wear.R.id.ws_navigation_drawer_item_text), isDisplayed()))
                .check(matches(withText("1")));
        verify(mNavDrawerItemSelectedListener).onItemSelected(1);
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

    @SdkSuppress(maxSdkVersion = 34) // b/427565061
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

    @SdkSuppress(maxSdkVersion = 34) // b/427565061
    @Test
    public void drawerContentViewShouldOnlyInflateOnceOpened() {
        // GIVEN a launched activity with only an action drawer
        activityRule.launchActivity(
                new DrawerTestActivity.Builder()
                        .setStyle(DrawerStyle.ONLY_ACTION_DRAWER_WITH_TITLE)
                        .build());

        final RecyclerView actionList =
                activityRule.getActivity().findViewById(androidx.wear.R.id.action_list);

        // WHEN it is opened
        WearableDrawerView actionDrawer = activityRule.getActivity().findViewById(
                R.id.action_drawer);
        openDrawer(actionDrawer);

        // THEN the action drawer should be open and the items should all exist in the actionList
        // adapter
        onView(withId(R.id.action_drawer))
                .perform(
                        waitForMatchingView(
                                allOf(withId(R.id.action_drawer), isOpened(true)),
                                MAX_WAIT_MS));
        assertEquals(7, actionList.getAdapter().getItemCount());
    }

    @Test
    public void drawerContentViewShouldNotInflateAfterLaunch() {
        // GIVEN a launched activity with only an action drawer
        activityRule.launchActivity(
                new DrawerTestActivity.Builder()
                        .setStyle(DrawerStyle.ONLY_ACTION_DRAWER_WITH_TITLE)
                        .build());

        final RecyclerView actionList =
                activityRule.getActivity().findViewById(androidx.wear.R.id.action_list);

        // THEN the drawer should not be visible and the draw action list should not have an
        // adapter set
        onView(allOf(withId(androidx.wear.R.id.action_list), not(isDisplayed())));
        assertNull(actionList.getAdapter());
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

    @SdkSuppress(maxSdkVersion = 34) // b/427565061
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

    @SdkSuppress(maxSdkVersion = 34) // b/427565061
    @Test
    public void closedNavDrawerShouldNotPreventSwipeToClose() {
        // GIVEN an activity which doesn't start with the nav drawer open
        activityRule.launchActivity(mSinglePageIntent);

        // THEN the view should allow swipe to close
        onView(withId(R.id.navigation_drawer)).check(matches(allowsSwipeToClose()));
    }

    @SdkSuppress(maxSdkVersion = 34) // b/427565061
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

    @SdkSuppress(maxSdkVersion = 33) // b/322538394
    @Test
    public void actionDrawerPeekIconShouldNotBeNull() {
        // GIVEN a drawer layout with a peeking action drawer whose menu is initialized in XML
        activityRule.launchActivity(mSinglePageIntent);
        DrawerTestActivity activity = activityRule.getActivity();
        ImageView peekIconView =
                (ImageView) activity
                        .findViewById(androidx.wear.R.id.ws_action_drawer_peek_action_icon);
        // THEN its peek icon should not be null
        assertNotNull(peekIconView.getDrawable());
    }

    @SdkSuppress(maxSdkVersion = 33) // b/322538394
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
        onView(withId(androidx.wear.R.id.ws_drawer_view_peek_container))
                .perform(waitForMatchingView(
                        allOf(
                                withId(androidx.wear.R.id.ws_drawer_view_peek_container),
                                isCompletelyDisplayed()),
                        MAX_WAIT_MS))
                .perform(click());
        // THEN its click listener should be notified
        verify(mockClickListener).onMenuItemClick(any(MenuItem.class));
    }

    @SdkSuppress(maxSdkVersion = 33) // b/322538394
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

    @SdkSuppress(maxSdkVersion = 34) // b/427565061
    @Test
    public void changingActionDrawerItemShouldUpdateView() {
        // GIVEN a drawer layout with an open action drawer
        activityRule.launchActivity(
                new DrawerTestActivity.Builder()
                        .setStyle(DrawerStyle.ONLY_ACTION_DRAWER_WITH_TITLE)
                        .openBottomDrawerInOnCreate()
                        .build());
        WearableActionDrawerView actionDrawer =
                activityRule.getActivity().findViewById(R.id.action_drawer);
        final MenuItem secondItem = actionDrawer.getMenu().getItem(1);

        // WHEN its second item is changed
        actionDrawer.post(new Runnable() {
            @Override
            public void run() {
                secondItem.setTitle("Modified item");
            }
        });

        // THEN the new item should be displayed
        onView(withText("Modified item")).check(matches(isDisplayed()));
    }

    @SdkSuppress(maxSdkVersion = 34) // b/427565061
    @Test
    public void removingActionDrawerItemShouldUpdateView() {
        // GIVEN a drawer layout with an open action drawer
        activityRule.launchActivity(
                new DrawerTestActivity.Builder()
                        .setStyle(DrawerStyle.ONLY_ACTION_DRAWER_WITH_TITLE)
                        .openBottomDrawerInOnCreate()
                        .build());
        final WearableActionDrawerView actionDrawer =
                activityRule.getActivity().findViewById(R.id.action_drawer);
        MenuItem secondItem = actionDrawer.getMenu().getItem(1);
        final int itemId = secondItem.getItemId();
        final String title = secondItem.getTitle().toString();
        final int initialSize = getChildByType(actionDrawer, RecyclerView.class)
                .getAdapter()
                .getItemCount();

        // WHEN its second item is removed
        actionDrawer.post(new Runnable() {
            @Override
            public void run() {
                actionDrawer.getMenu().removeItem(itemId);
            }
        });

        // THEN it should decrease by 1 in size and it should no longer contain the item's text
        onView(allOf(withParent(withId(R.id.action_drawer)), isAssignableFrom(RecyclerView.class)))
                .perform(waitForRecyclerToBeSize(initialSize - 1, MAX_WAIT_MS))
                .perform(waitForMatchingView(recyclerWithoutText(is(title)), MAX_WAIT_MS));
    }

    @Test
    public void addingActionDrawerItemShouldUpdateView() {
        // GIVEN a drawer layout with an open action drawer
        activityRule.launchActivity(
                new DrawerTestActivity.Builder()
                        .setStyle(DrawerStyle.ONLY_ACTION_DRAWER_WITH_TITLE)
                        .openBottomDrawerInOnCreate()
                        .build());
        final WearableActionDrawerView actionDrawer =
                activityRule.getActivity().findViewById(R.id.action_drawer);

        RecyclerView recycler = getChildByType(actionDrawer, RecyclerView.class);
        final RecyclerView.LayoutManager layoutManager = recycler.getLayoutManager();
        final int initialSize = recycler.getAdapter().getItemCount();

        // WHEN an item is added and the view is scrolled down (to make sure the view is created)
        actionDrawer.post(new Runnable() {
            @Override
            public void run() {
                actionDrawer.getMenu().add(0, 42, Menu.NONE, "New Item");
                layoutManager.scrollToPosition(initialSize);
            }
        });

        // THEN it should decrease by 1 in size and the there should be a view with the item's text
        onView(allOf(withParent(withId(R.id.action_drawer)), isAssignableFrom(RecyclerView.class)))
                .perform(waitForRecyclerToBeSize(initialSize + 1, MAX_WAIT_MS))
                .perform(waitForMatchingView(withText("New Item"), MAX_WAIT_MS));
    }

    @SdkSuppress(maxSdkVersion = 34) // b/427565061
    @Test
    public void flingInHorizontalScrollerDoesNotPeekDrawers() {
        // Note that this test uses a different activity to implement the list...
        ActivityScenario<DrawerRecyclerViewTestActivity> scenario =
                ActivityScenario.launch(DrawerRecyclerViewTestActivity.class);

        // Nothing should be peeking
        onView(withId(R.id.recycler_navigation_drawer)).check(matches(isPeeking(false)));

        onView(withId(R.id.recycler)).perform(swipeLeft());
        onView(withId(R.id.recycler)).perform(waitForRecyclerToSettle(MAX_WAIT_MS));

        // Drawers should not be peeking...
        onView(withId(R.id.recycler_navigation_drawer)).check(matches(isPeeking(false)));
    }

    private void scrollToPosition(final RecyclerView recyclerView, final int position) {
        recyclerView.post(new Runnable() {
            @Override
            public void run() {
                recyclerView.scrollToPosition(position);
            }
        });
    }

    private void selectNavItem(final WearableNavigationDrawerView navDrawer, final int index) {
        navDrawer.post(new Runnable() {
            @Override
            public void run() {
                navDrawer.setCurrentItem(index, false);
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

    private TypeSafeMatcher<View> isPeeking() {
        return isPeeking(true);
    }

    private TypeSafeMatcher<View> isPeeking(boolean peeking) {
        return new TypeSafeMatcher<View>() {
            @Override
            protected boolean matchesSafely(View view) {
                WearableDrawerView drawer = (WearableDrawerView) view;
                return drawer.isPeeking() == peeking;
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

    /**
     * Returns a {@link TypeSafeMatcher} that returns {@code true} when the {@link RecyclerView}
     * does not contain a {@link TextView} with text matched by {@code textMatcher}.
     */
    private TypeSafeMatcher<View> recyclerWithoutText(final Matcher<String> textMatcher) {
        return new TypeSafeMatcher<View>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("Without recycler text ");
                textMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                if (!(view instanceof RecyclerView)) {
                    return false;
                }

                RecyclerView recycler = ((RecyclerView) view);
                if (recycler.isAnimating()) {
                    // While the RecyclerView is animating, it will return null ViewHolders and we
                    // won't be able to tell whether the item has been removed or not.
                    return false;
                }

                for (int i = 0; i < recycler.getAdapter().getItemCount(); i++) {
                    RecyclerView.ViewHolder holder = recycler.findViewHolderForAdapterPosition(i);
                    if (holder != null) {
                        TextView text = getChildByType(holder.itemView, TextView.class);
                        if (text != null && textMatcher.matches(text.getText())) {
                            return false;
                        }
                    }
                }

                return true;
            }
        };
    }

    /**
     * Waits for the {@link RecyclerView} to contain {@code targetCount} items, up to {@code millis}
     * milliseconds. Throws exception if the time limit is reached before reaching the desired
     * number of items.
     */
    public ViewAction waitForRecyclerToBeSize(final int targetCount, final long millis) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(RecyclerView.class);
            }

            @Override
            public String getDescription() {
                return "Waiting for recycler to be size=" + targetCount;
            }

            @Override
            public void perform(UiController uiController, View view) {
                if (!(view instanceof RecyclerView)) {
                    return;
                }

                RecyclerView recycler = (RecyclerView) view;
                uiController.loopMainThreadUntilIdle();
                final long startTime = System.currentTimeMillis();
                final long endTime = startTime + millis;
                do {
                    if (recycler.getAdapter().getItemCount() == targetCount) {
                        return;
                    }
                    uiController.loopMainThreadForAtLeast(100); // at least 3 frames
                } while (System.currentTimeMillis() < endTime);

                // timeout happens
                throw new PerformException.Builder()
                        .withActionDescription(this.getDescription())
                        .withViewDescription(HumanReadables.describe(view))
                        .withCause(new TimeoutException())
                        .build();
            }
        };
    }

    public ViewAction waitForRecyclerToSettle(final long millis) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(RecyclerView.class);
            }

            @Override
            public String getDescription() {
                return "Waiting for Recycler ScrollState to be SCROLL_STATE_IDLE";
            }

            @Override
            public void perform(UiController uiController, View view) {
                if (!(view instanceof RecyclerView)) {
                    return;
                }

                RecyclerView recycler = (RecyclerView) view;
                uiController.loopMainThreadUntilIdle();
                final long startTime = System.currentTimeMillis();
                final long endTime = startTime + millis;
                do {
                    if (recycler.getScrollState() == RecyclerView.SCROLL_STATE_IDLE) {
                        return;
                    }
                    uiController.loopMainThreadForAtLeast(100); // at least 3 frames
                } while (System.currentTimeMillis() < endTime);

                // timeout happens
                throw new PerformException.Builder()
                        .withActionDescription(this.getDescription())
                        .withViewDescription(HumanReadables.describe(view))
                        .withCause(new TimeoutException())
                        .build();
            }
        };
    }

    /**
     * Returns the first child of {@code root} to be an instance of class {@code T}, or {@code null}
     * if none were found.
     */
    @SuppressWarnings("unchecked")
    @Nullable
    private <T> T getChildByType(View root, Class<T> classOfChildToFind) {
        for (View child : TreeIterables.breadthFirstViewTraversal(root)) {
            if (classOfChildToFind.isInstance(child)) {
                return (T) child;
            }
        }

        return null;
    }
}
