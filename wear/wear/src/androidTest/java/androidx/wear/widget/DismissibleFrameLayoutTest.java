/*
 * Copyright 2020 The Android Open Source Project
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
import static androidx.test.espresso.action.ViewActions.swipeRight;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.wear.widget.util.AsyncViewActions.waitForMatchingView;

import static org.hamcrest.Matchers.allOf;

import android.app.Activity;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.wear.test.R;
import androidx.wear.widget.util.WakeLockRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class DismissibleFrameLayoutTest {
    private static final long MAX_WAIT_TIME = 4000; //ms

    private final DismissibleFrameLayout.Callback mDismissCallback = new DismissCallback();

    @Rule
    public final WakeLockRule wakeLock = new WakeLockRule();

    @Rule
    public final ActivityTestRule<DismissibleFrameLayoutTestActivity> activityRule =
            new ActivityTestRule<>(
                    DismissibleFrameLayoutTestActivity.class,
                    true, /** initial touch mode */
                    false /** launchActivity */
            );

    @Test
    public void testBackDismiss() {
        // GIVEN a freshly setup DismissibleFrameLayout
        setUpDismissibleLayout(true, false, mDismissCallback);
        // CHECK the layout is not hidden
        assertNotHidden(R.id.dismissible_root);
        // WHEN back button pressed
        sendBackKey();
        // AND hidden
        assertHidden(R.id.dismissible_root);
    }

    @Test
    public void testBackNotDismissIfDisabled() {
        // GIVEN a freshly setup DismissibleFrameLayout
        setUpDismissibleLayout(false, false, mDismissCallback);
        // CHECK the layout is not hidden
        assertNotHidden(R.id.dismissible_root);
        // WHEN back button pressed
        sendBackKey();
        // AND the layout is still nor hidden
        assertNotHidden(R.id.dismissible_root);
    }

    @Test
    public void testSwipeDismiss() {
        // GIVEN a freshly setup DismissibleFrameLayout
        setUpDismissibleLayout(false, true, mDismissCallback);
        // CHECK the layout is not hidden
        assertNotHidden(R.id.dismissible_root);
        // WHEN perform a swipe dismiss
        onView(withId(R.id.dismissible_root)).perform(swipeRight());
        // AND hidden
        assertHidden(R.id.dismissible_root);
    }

    @Test
    public void testSwipeNotDismissIfDisabled() {
        // GIVEN a freshly setup DismissibleFrameLayout
        setUpDismissibleLayout(false, false, mDismissCallback);
        // CHECK the layout is not hidden
        assertNotHidden(R.id.dismissible_root);
        // WHEN perform a swipe dismiss
        onView(withId(R.id.dismissible_root)).perform(swipeRight());
        // AND the layout is still nor hidden
        assertNotHidden(R.id.dismissible_root);
    }


    @Test
    public void testBackDismissWithRecyclerView() {
        // GIVEN a freshly setup DismissibleFrameLayout
        setUpDismissibleLayoutWithRecyclerView(
                true, false, mDismissCallback);
        // CHECK the layout is not hidden
        assertNotHidden(R.id.dismissible_root);
        // WHEN back button pressed
        sendBackKey();
        // AND hidden
        assertHidden(R.id.dismissible_root);
    }

    @Test
    public void testSwipeDismissWithRecyclerView() {
        // GIVEN a freshly setup DismissibleFrameLayout
        setUpDismissibleLayoutWithRecyclerView(
                false, true, mDismissCallback);
        // CHECK the layout is not hidden
        assertNotHidden(R.id.dismissible_root);
        // WHEN perform a swipe dismiss
        onView(withId(R.id.dismissible_root)).perform(swipeRight());
        // AND hidden
        assertHidden(R.id.dismissible_root);
    }

    /**
     * Set ups the simplest possible layout for test cases - a {@link SwipeDismissFrameLayout} with
     * a single static child.
     */
    private void setUpDismissibleLayout(
            boolean backDismissible,
            boolean swipeable,
            @Nullable DismissibleFrameLayout.Callback callback) {
        activityRule.launchActivity(
                new Intent()
                        .putExtra(
                                LayoutTestActivity.EXTRA_LAYOUT_RESOURCE_ID,
                                androidx.wear.test.R.layout.dismissible_frame_layout_testcase));

        configureDismissibleLayout(backDismissible, swipeable, callback);
    }

    private void setUpDismissibleLayoutWithRecyclerView(
            boolean backDismissible,
            boolean swipeable,
            @Nullable DismissibleFrameLayout.Callback callback) {
        Intent launchIntent = new Intent();
        launchIntent.putExtra(LayoutTestActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.dismissible_frame_layout_recyclerview_testcase);
        launchIntent.putExtra(DismissibleFrameLayoutTestActivity.EXTRA_LAYOUT_HORIZONTAL, true);
        activityRule.launchActivity(launchIntent);

        configureDismissibleLayout(backDismissible, swipeable, callback);
    }

    private void configureDismissibleLayout(
            boolean backDismissible,
            boolean swipeable,
            @Nullable DismissibleFrameLayout.Callback callback) {
        Activity activity = activityRule.getActivity();
        DismissibleFrameLayout testLayout = activity.findViewById(R.id.dismissible_root);
        testLayout.setBackButtonDismissible(backDismissible);
        testLayout.setSwipeDismissible(swipeable);

        if (callback != null) {
            testLayout.registerCallback(callback);
        }
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

    private void sendBackKey() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
    }

    /** Helper class hiding the view after a successful swipe-to-dismiss. */
    private static class DismissCallback extends DismissibleFrameLayout.Callback {

        @Override
        public void onDismissed(DismissibleFrameLayout layout) {
            layout.setVisibility(View.GONE);
        }
    }
}
