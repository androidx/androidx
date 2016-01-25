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
package android.support.v7.app;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Test;

import android.support.annotation.LayoutRes;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.v7.appcompat.test.R;
import android.test.suitebuilder.annotation.SmallTest;
import android.view.View;
import android.view.ViewStub;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.core.AllOf.allOf;

/**
 * Test cases to verify that <code>DrawerLayout</code> only supports configurations
 * with at most one drawer child along each vertical (left / right) edge.
 */
public class DrawerDynamicLayoutTest
        extends BaseInstrumentationTestCase<DrawerDynamicLayoutActivity> {
    public DrawerDynamicLayoutTest() {
        super(DrawerDynamicLayoutActivity.class);
    }

    @After
    public void tearDown() throws Exception {
        // Now that the test is done, replace the activity content view with ViewStub so
        // that it's ready to be replaced for the next test.
        try {
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final DrawerDynamicLayoutActivity activity = mActivityTestRule.getActivity();
                    activity.setContentView(R.layout.drawer_dynamic_layout);
                }
            });
        } catch (Throwable t) {
            throw new Exception(t);
        }
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

    /**
     * Inflates the <code>ViewStub</code> with the passed layout resource.
     */
    private ViewAction inflateViewStub(final @LayoutRes int layoutResId) {
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
                viewStub.inflate();

                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    @Test
    @SmallTest
    public void testSingleStartDrawer() {
        onView(withId(R.id.drawer_layout)).check(doesNotExist());
        onView(withId(R.id.drawer_stub)).perform(
                inflateViewStub(R.layout.drawer_dynamic_content_single_start));
    }

    @Test(expected=IllegalStateException.class)
    @SmallTest
    public void testDoubleStartDrawers() {
        onView(withId(R.id.drawer_layout)).check(doesNotExist());
        // Note the expected exception in the @Test annotation, as we expect the DrawerLayout
        // to throw exception during the measure pass as it detects two start drawers.
        onView(withId(R.id.drawer_stub)).perform(
                inflateViewStub(R.layout.drawer_dynamic_content_double_start));
    }

    @Test
    @SmallTest
    public void testSingleEndDrawer() {
        onView(withId(R.id.drawer_layout)).check(doesNotExist());
        onView(withId(R.id.drawer_stub)).perform(
                inflateViewStub(R.layout.drawer_dynamic_content_single_end));
    }

    @Test(expected=IllegalStateException.class)
    @SmallTest
    public void testDoubleEndDrawers() {
        onView(withId(R.id.drawer_layout)).check(doesNotExist());
        // Note the expected exception in the @Test annotation, as we expect the DrawerLayout
        // to throw exception during the measure pass as it detects two end drawers.
        onView(withId(R.id.drawer_stub)).perform(
                inflateViewStub(R.layout.drawer_dynamic_content_double_end));
    }

    @Test
    @SmallTest
    public void testSingleStartDrawerSingleEndDrawer() {
        onView(withId(R.id.drawer_layout)).check(doesNotExist());
        onView(withId(R.id.drawer_stub)).perform(
                inflateViewStub(R.layout.drawer_dynamic_content_start_end));
    }

    @Test(expected=IllegalStateException.class)
    @SmallTest
    public void testDoubleStartDrawersSingleEndDrawer() {
        onView(withId(R.id.drawer_layout)).check(doesNotExist());
        // Note the expected exception in the @Test annotation, as we expect the DrawerLayout
        // to throw exception during the measure pass as it detects two start drawers.
        onView(withId(R.id.drawer_stub)).perform(
                inflateViewStub(R.layout.drawer_dynamic_content_double_start_single_end));
    }

    @Test(expected=IllegalStateException.class)
    @SmallTest
    public void testDoubleEndDrawersSingleStartDrawer() {
        onView(withId(R.id.drawer_layout)).check(doesNotExist());
        // Note the expected exception in the @Test annotation, as we expect the DrawerLayout
        // to throw exception during the measure pass as it detects two start drawers.
        onView(withId(R.id.drawer_stub)).perform(
                inflateViewStub(R.layout.drawer_dynamic_content_double_end_single_start));
    }
}
