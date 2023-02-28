/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.coordinatorlayout.widget;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.pressKey;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.junit.Assert.assertEquals;

import android.view.KeyEvent;

import androidx.coordinatorlayout.test.R;
import androidx.coordinatorlayout.testutils.AppBarStateChangedListener;
import androidx.coordinatorlayout.testutils.NestedScrollViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.testutils.PollingCheck;

import com.google.android.material.appbar.AppBarLayout;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings({"unchecked", "rawtypes"})
@LargeTest
@RunWith(AndroidJUnit4.class)
public class CoordinatorLayoutKeyEventTest {

    // test rule
    @Rule
    public ActivityScenarioRule<CoordinatorWithNestedScrollViewsActivity> mActivityScenarioRule =
            new ActivityScenarioRule(CoordinatorWithNestedScrollViewsActivity.class);

    private AppBarLayout mAppBarLayout;
    private AppBarStateChangedListener.State mAppBarState =
            AppBarStateChangedListener.State.UNKNOWN;

    @Before
    public void setup() {
        mActivityScenarioRule.getScenario().onActivity(activity -> {
            mAppBarLayout = activity.mAppBarLayout;
            mAppBarLayout.addOnOffsetChangedListener(new AppBarStateChangedListener() {
                @Override
                public void onStateChanged(AppBarLayout appBarLayout, State state) {
                    mAppBarState = state;
                }
            });
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    /*** Tests ***/
    @Test
    @LargeTest
    public void isCollapsingToolbarExpanded_swipeDownMultipleKeysUp_isExpanded() {

        onView(withId(R.id.top_nested_text)).check(matches(isCompletelyDisplayed()));

        // Scrolls down content and collapses the CollapsingToolbarLayout in the AppBarLayout.
        onView(withId(R.id.top_nested_text)).perform(swipeUp());
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // Espresso doesn't properly support swipeUp() with a CoordinatorLayout,
        // AppBarLayout/CollapsingToolbarLayout, and NestedScrollView. From testing, it only
        // handles waiting until the AppBarLayout/CollapsingToolbarLayout is finished with its
        // transition, NOT waiting until the NestedScrollView is finished with its scrolling.
        // This PollingCheck waits until the scroll is finished in the NestedScrollView.
        AtomicInteger previousScroll = new AtomicInteger();
        PollingCheck.waitFor(() -> {
            AtomicInteger currentScroll = new AtomicInteger();

            mActivityScenarioRule.getScenario().onActivity(activity -> {
                currentScroll.set(activity.mNestedScrollView.getScrollY());
            });

            boolean isDone = currentScroll.get() == previousScroll.get();
            previousScroll.set(currentScroll.get());

            return isDone;
        });

        // Verifies the CollapsingToolbarLayout in the AppBarLayout is collapsed.
        assertEquals(mAppBarState, AppBarStateChangedListener.State.COLLAPSED);

        // Scrolls up to the top element in the NestedScrollView.
        // NOTE: NestedScrollView requires a custom Action to work properly and the scroll does NOT
        // impact the CoordinatorLayout's CollapsingToolbarLayout (which stays collapsed).
        onView(withId(R.id.top_nested_text)).perform(NestedScrollViewActions.scrollToTop());
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        onView(withId(R.id.top_nested_text)).check(matches(isCompletelyDisplayed()));

        // First up keystroke gains focus (doesn't move any content).
        onView(withId(R.id.top_nested_text)).perform(pressKey(KeyEvent.KEYCODE_DPAD_UP));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        onView(withId(R.id.top_nested_text)).check(matches(isCompletelyDisplayed()));

        // This is a fail-safe in case the DPAD UP isn't making any changes, we break out of the
        // loop.
        float previousAppBarLayoutY = 0.0f;

        // Performs a key press until the app bar is either expanded completely or no changes are
        // made in the app bar between the previous call and the current call (failure case).
        while (mAppBarState != AppBarStateChangedListener.State.EXPANDED
                && (mAppBarLayout.getY() != previousAppBarLayoutY)
        ) {
            previousAppBarLayoutY = mAppBarLayout.getY();

            // Partially expands the CollapsingToolbarLayout.
            onView(withId(R.id.top_nested_text)).perform(pressKey(KeyEvent.KEYCODE_DPAD_UP));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        }

        // Checks CollapsingToolbarLayout (in the AppBarLayout) is fully expanded.
        assertEquals(mAppBarState, AppBarStateChangedListener.State.EXPANDED);
    }
}
