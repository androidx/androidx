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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
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

    @Test
    public void testBackDismiss() {
        // GIVEN a freshly setup DismissibleFrameLayout
        try (ActivityScenario<DismissibleFrameLayoutTestActivity> scenario =
                     ActivityScenario.launch(createDismissibleLayoutIntent())) {
            configureDismissibleLayout(scenario, true, false, mDismissCallback);
            // CHECK the layout is not hidden
            assertNotHidden(R.id.dismissible_root);
            // WHEN back button pressed
            sendBackKey();
            // AND hidden
            assertHidden(R.id.dismissible_root);
            // Back button up event is consumed, and not pass to the activity
            scenario.onActivity(activity -> {
                assertFalse(activity.mConsumeBackButtonUp);
            });
        }
    }

    @Test
    public void testBackNotDismissIfDisabled() {
        // GIVEN a freshly setup DismissibleFrameLayout
        try (ActivityScenario<DismissibleFrameLayoutTestActivity> scenario =
                     ActivityScenario.launch(createDismissibleLayoutIntent())) {
            configureDismissibleLayout(scenario, false, false, mDismissCallback);
            // CHECK the layout is not hidden
            assertNotHidden(R.id.dismissible_root);
            // WHEN back button pressed
            sendBackKey();
            // AND the layout is still not hidden
            assertNotHidden(R.id.dismissible_root);
            // Back button up event is not consumed, and continue to pass to the activity
            scenario.onActivity(activity -> {
                assertTrue(activity.mConsumeBackButtonUp);
            });
        }
    }

    @Test
    public void testSwipeDismiss() {
        // GIVEN a freshly setup DismissibleFrameLayout
        try (ActivityScenario<DismissibleFrameLayoutTestActivity> scenario =
                     ActivityScenario.launch(createDismissibleLayoutIntent())) {
            configureDismissibleLayout(scenario, false, true, mDismissCallback);
            // CHECK the layout is not hidden
            assertNotHidden(R.id.dismissible_root);
            // WHEN perform a swipe dismiss
            onView(withId(R.id.dismissible_root)).perform(swipeRight());
            // AND hidden
            assertHidden(R.id.dismissible_root);
        }
    }

    @Test
    public void testSwipeNotDismissIfDisabled() {
        // GIVEN a freshly setup DismissibleFrameLayout
        try (ActivityScenario<DismissibleFrameLayoutTestActivity> scenario =
                     ActivityScenario.launch(createDismissibleLayoutIntent())) {
            configureDismissibleLayout(scenario, false, false, mDismissCallback);
            // CHECK the layout is not hidden
            assertNotHidden(R.id.dismissible_root);
            // WHEN perform a swipe dismiss
            onView(withId(R.id.dismissible_root)).perform(swipeRight());
            // AND the layout is still nor hidden
            assertNotHidden(R.id.dismissible_root);
        }
    }

    @Test
    public void testDisableThenEnableBackDismiss() {
        // GIVEN a freshly setup DismissibleFrameLayout
        try (ActivityScenario<DismissibleFrameLayoutTestActivity> scenario =
                     ActivityScenario.launch(createDismissibleLayoutIntent())) {
            final DismissibleFrameLayout[] testLayout = new DismissibleFrameLayout[1];
            final DismissibleFrameLayoutTestActivity[] testActivity =
                    new DismissibleFrameLayoutTestActivity[1];
            scenario.onActivity(activity -> {
                testActivity[0] = activity;
                testLayout[0] =
                        (DismissibleFrameLayout) activity.findViewById(R.id.dismissible_root);
                testLayout[0].registerCallback(mDismissCallback);
                // Disable back button dismiss
                testLayout[0].setBackButtonDismissible(false);
            });

            // CHECK the layout is not hidden
            assertNotHidden(R.id.dismissible_root);
            // The layout is not focused
            assertFalse(testActivity[0].getCurrentFocus() == testLayout[0]);
            // WHEN back button pressed
            testActivity[0].mConsumeBackButtonUp = false;
            sendBackKey();
            // AND the layout is still not hidden
            assertNotHidden(R.id.dismissible_root);
            // Back button up event is not consumed, and continue to pass to the activity
            assertTrue(testActivity[0].mConsumeBackButtonUp);

            // Enable backButton dismiss, we have to run this on the main thread
            InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    testLayout[0].setBackButtonDismissible(true);
                }
            });
            // CHECK the layout is not hidden
            assertNotHidden(R.id.dismissible_root);
            // The layout is focused
            assertTrue(testActivity[0].getCurrentFocus() == testLayout[0]);
            // WHEN back button pressed
            testActivity[0].mConsumeBackButtonUp = false;
            sendBackKey();
            // AND the layout is hidden
            assertHidden(R.id.dismissible_root);
            // Back button up event is consumed without passing up to the activity
            assertFalse(testActivity[0].mConsumeBackButtonUp);
        }
    }


    @Test
    public void testBackDismissWithRecyclerView() {
        // GIVEN a freshly setup DismissibleFrameLayout
        try (ActivityScenario<DismissibleFrameLayoutTestActivity> scenario =
                     ActivityScenario.launch(createDismissibleLayoutWithRecyclerViewIntent())) {
            configureDismissibleLayout(scenario, true, false, mDismissCallback);
            // CHECK the layout is not hidden
            assertNotHidden(R.id.dismissible_root);
            // WHEN back button pressed
            sendBackKey();
            // AND hidden
            assertHidden(R.id.dismissible_root);
            // Back button up event is consumed, and not pass to the activity
            scenario.onActivity(activity -> {
                assertFalse(activity.mConsumeBackButtonUp);
            });
        }
    }

    @Test
    public void testSwipeDismissWithRecyclerView() {
        // GIVEN a freshly setup DismissibleFrameLayout
        try (ActivityScenario<DismissibleFrameLayoutTestActivity> scenario =
                     ActivityScenario.launch(createDismissibleLayoutWithRecyclerViewIntent())) {
            configureDismissibleLayout(scenario, false, true, mDismissCallback);
            // CHECK the layout is not hidden
            assertNotHidden(R.id.dismissible_root);
            // WHEN perform a swipe dismiss
            onView(withId(R.id.dismissible_root)).perform(swipeRight());
            // AND hidden
            assertHidden(R.id.dismissible_root);
        }
    }

    /**
     * Creates intent for launching an activity for test cases - a {@link SwipeDismissFrameLayout}
     * with a single static child.
     */
    private Intent createDismissibleLayoutIntent() {
        return new Intent()
                .setClass(ApplicationProvider.getApplicationContext(),
                        DismissibleFrameLayoutTestActivity.class)
                .putExtra(
                        LayoutTestActivity.EXTRA_LAYOUT_RESOURCE_ID,
                        R.layout.dismissible_frame_layout_testcase);
    }

    /**
     * Creates intent for launching an activity for test cases - a {@link SwipeDismissFrameLayout}
     * with a child of scrollable container.
     */
    private Intent createDismissibleLayoutWithRecyclerViewIntent() {
        return new Intent()
                .setClass(ApplicationProvider.getApplicationContext(),
                        DismissibleFrameLayoutTestActivity.class)
                .putExtra(LayoutTestActivity.EXTRA_LAYOUT_RESOURCE_ID,
                        R.layout.dismissible_frame_layout_recyclerview_testcase)
                .putExtra(DismissibleFrameLayoutTestActivity.EXTRA_LAYOUT_HORIZONTAL, true);
    }

    private void configureDismissibleLayout(
            ActivityScenario<DismissibleFrameLayoutTestActivity> scenario,
            boolean backDismissible,
            boolean swipeable,
            @Nullable DismissibleFrameLayout.Callback callback) {
        scenario.onActivity(activity -> {
            DismissibleFrameLayout testLayout = activity.findViewById(R.id.dismissible_root);
            testLayout.setBackButtonDismissible(backDismissible);
            testLayout.setSwipeDismissible(swipeable);
            if (callback != null) {
                testLayout.registerCallback(callback);
            }
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

    private void sendBackKey() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
    }

    /** Helper class hiding the view after a successful swipe-to-dismiss. */
    private static class DismissCallback extends DismissibleFrameLayout.Callback {

        @Override
        public void onDismissFinished(DismissibleFrameLayout layout) {
            layout.setVisibility(View.GONE);
        }
    }
}
