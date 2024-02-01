/*
 * Copyright (C) 2015 The Android Open Source Project
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
package androidx.appcompat.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.hamcrest.CoreMatchers.allOf;
import static org.mockito.Mockito.mock;

import android.util.Log;
import android.view.View;
import android.view.ViewStub;

import androidx.annotation.LayoutRes;
import androidx.appcompat.test.R;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.test.annotation.UiThreadTest;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test cases to verify that <code>DrawerLayout</code> only supports configurations
 * with at most one drawer child along each vertical (left / right) edge.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class DrawerDynamicLayoutTest {
    @Rule
    public final ActivityTestRule<DrawerDynamicLayoutActivity> mActivityTestRule =
            new ActivityTestRule<>(DrawerDynamicLayoutActivity.class);

    @UiThreadTest
    @After
    public void tearDown() {
        // Now that the test is done, replace the activity content view with ViewStub so
        // that it's ready to be replaced for the next test.
        final DrawerDynamicLayoutActivity activity = mActivityTestRule.getActivity();
        activity.setContentView(R.layout.drawer_dynamic_layout);
    }

    /**
     * Matches views that have parents.
     */
    private Matcher<View> hasParent() {
        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("has parent");
            }

            @Override
            public boolean matchesSafely(View view) {
                return view.getParent() != null;
            }
        };
    }

    private ViewAction inflateViewStub(final @LayoutRes int layoutResId) {
        return inflateViewStub(layoutResId, false);
    }

    /**
     * Inflates the <code>ViewStub</code> with the passed layout resource.
     */
    private ViewAction inflateViewStub(final @LayoutRes int layoutResId, final boolean log) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return allOf(isAssignableFrom(ViewStub.class), hasParent());
            }

            @Override
            public String getDescription() {
                return "Inflates view stub";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                ViewStub viewStub = (ViewStub) view;
                viewStub.setLayoutResource(layoutResId);
                View drawer = viewStub.inflate();
                uiController.loopMainThreadUntilIdle();
                if (log) {
                    logGravity(drawer);
                }
            }
        };
    }

    public static void logGravity(View view) {
        DrawerLayout drawer = (DrawerLayout) view;
        for (int i = 0; i < drawer.getChildCount(); i++) {
            View child = drawer.getChildAt(i);
            final int gravity = ((DrawerLayout.LayoutParams) child.getLayoutParams()).gravity;
            final int absGravity = GravityCompat.getAbsoluteGravity(gravity,
                    child.getLayoutDirection());
            final int gravityInParent = GravityCompat.getAbsoluteGravity(gravity,
                    drawer.getLayoutDirection());
            Log.e("DrawerDynamicLayoutTest", "gravity of child[" + i + "] "
                    + " = " + absGravity +  "; gravity in parent " + gravityInParent);
        }
    }

    @Test
    public void testSingleStartDrawer() {
        onView(withId(R.id.drawer_layout)).check(doesNotExist());
        onView(withId(R.id.drawer_stub)).perform(
                inflateViewStub(R.layout.drawer_dynamic_content_single_start));
    }

    @Test
    public void testSingleEndDrawer() {
        onView(withId(R.id.drawer_layout)).check(doesNotExist());
        onView(withId(R.id.drawer_stub)).perform(
                inflateViewStub(R.layout.drawer_dynamic_content_single_end));
    }

    @Test
    public void testSingleStartDrawerSingleEndDrawer() {
        onView(withId(R.id.drawer_layout)).check(doesNotExist());
        onView(withId(R.id.drawer_stub)).perform(
                inflateViewStub(R.layout.drawer_dynamic_content_start_end));
    }

    @Test
    public void testRemoveUnregisteredListener() {
        onView(withId(R.id.drawer_stub)).perform(
                inflateViewStub(R.layout.drawer_dynamic_content_single_start));

        // We do this test here and not in DrawerLayoutTest since we want to be sure that the
        // call to DrawerLayout.removeDrawerLayout() didn't have any calls to addDrawerLayout()
        // before it. DrawerLayoutTest and its DrawerLayoutActivity register listeners as part
        // of their initial setup flow.
        final DrawerLayout startDrawer =
                (DrawerLayout) mActivityTestRule.getActivity().findViewById(R.id.drawer_layout);
        DrawerLayout.DrawerListener mockedListener = mock(DrawerLayout.DrawerListener.class);
        startDrawer.removeDrawerListener(mockedListener);
    }
}
