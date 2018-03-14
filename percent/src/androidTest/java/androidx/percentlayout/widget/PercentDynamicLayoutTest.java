/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.percentlayout.widget;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

import static org.hamcrest.CoreMatchers.allOf;

import android.support.test.annotation.UiThreadTest;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.filters.SmallTest;
import android.view.View;
import android.view.ViewStub;

import androidx.annotation.LayoutRes;
import androidx.percentlayout.test.R;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Test;

/**
 * Test cases to verify that percent layouts properly account for their own paddings.
 */
@SmallTest
public class PercentDynamicLayoutTest
        extends BaseInstrumentationTestCase<PercentDynamicLayoutActivity> {
    public PercentDynamicLayoutTest() {
        super(PercentDynamicLayoutActivity.class);
    }

    @UiThreadTest
    @After
    public void tearDown() {
        // Now that the test is done, replace the activity content view with ViewStub so
        // that it's ready to be replaced for the next test.
        final PercentDynamicLayoutActivity activity = mActivityTestRule.getActivity();
        activity.setContentView(R.layout.percent_dynamic_layout);
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
    public void testPercentFrameWithHorizontalPaddings() {
        onView(withId(R.id.percent_layout)).check(doesNotExist());
        onView(withId(R.id.percent_stub)).perform(
                inflateViewStub(R.layout.percent_frame_layout_hpaddings));

        final PercentFrameLayout percentFrameLayout =
                (PercentFrameLayout) mActivityTestRule.getActivity().findViewById(
                        R.id.percent_layout);
        final int containerWidth = percentFrameLayout.getWidth();
        final int containerHeight = percentFrameLayout.getHeight();

        final int availableWidth = containerWidth - percentFrameLayout.getPaddingLeft()
                - percentFrameLayout.getPaddingRight();
        final int availableHeight = containerHeight - percentFrameLayout.getPaddingTop()
                - percentFrameLayout.getPaddingBottom();

        final View child1 = percentFrameLayout.findViewById(R.id.child1);
        final View child2 = percentFrameLayout.findViewById(R.id.child2);

        assertFuzzyEquals("Child 1 width as 50% of the container's available width",
                0.5f * availableWidth, child1.getWidth());
        assertFuzzyEquals("Child 1 height as 100% of the container's available height",
                availableHeight, child1.getHeight());
        assertFuzzyEquals("Child 2 width as 50% of the container's available width",
                0.5f * availableWidth, child2.getWidth());
        assertFuzzyEquals("Child 2 height as 100% of the container's available height",
                availableHeight, child2.getHeight());
    }

    @Test
    public void testPercentFrameWithVerticalPaddings() {
        onView(withId(R.id.percent_layout)).check(doesNotExist());
        onView(withId(R.id.percent_stub)).perform(
                inflateViewStub(R.layout.percent_frame_layout_vpaddings));

        final PercentFrameLayout percentFrameLayout =
                (PercentFrameLayout) mActivityTestRule.getActivity().findViewById(
                        R.id.percent_layout);
        final int containerWidth = percentFrameLayout.getWidth();
        final int containerHeight = percentFrameLayout.getHeight();

        final int availableWidth = containerWidth - percentFrameLayout.getPaddingLeft()
                - percentFrameLayout.getPaddingRight();
        final int availableHeight = containerHeight - percentFrameLayout.getPaddingTop()
                - percentFrameLayout.getPaddingBottom();

        final View child1 = percentFrameLayout.findViewById(R.id.child1);
        final View child2 = percentFrameLayout.findViewById(R.id.child2);

        assertFuzzyEquals("Child 1 width as 100% of the container's available width",
                availableWidth, child1.getWidth());
        assertFuzzyEquals("Child 1 height as 50% of the container's available height",
                0.5f * availableHeight, child1.getHeight());
        assertFuzzyEquals("Child 2 width as 100% of the container's available width",
                availableWidth, child2.getWidth());
        assertFuzzyEquals("Child 2 height as 50% of the container's available height",
                0.5f* availableHeight, child2.getHeight());
    }

    @Test
    public void testPercentRelativeWithHorizontalPaddings() {
        onView(withId(R.id.percent_layout)).check(doesNotExist());
        onView(withId(R.id.percent_stub)).perform(
                inflateViewStub(R.layout.percent_relative_layout_hpaddings));

        final PercentRelativeLayout percentRelativeLayout =
                (PercentRelativeLayout) mActivityTestRule.getActivity().findViewById(
                        R.id.percent_layout);
        final int containerWidth = percentRelativeLayout.getWidth();
        final int containerHeight = percentRelativeLayout.getHeight();

        final int availableWidth = containerWidth - percentRelativeLayout.getPaddingLeft()
                - percentRelativeLayout.getPaddingRight();
        final int availableHeight = containerHeight - percentRelativeLayout.getPaddingTop()
                - percentRelativeLayout.getPaddingBottom();

        final View child1 = percentRelativeLayout.findViewById(R.id.child1);
        final View child2 = percentRelativeLayout.findViewById(R.id.child2);

        assertFuzzyEquals("Child 1 width as 50% of the container's available width",
                0.5f * availableWidth, child1.getWidth());
        assertFuzzyEquals("Child 1 height as 100% of the container's available height",
                availableHeight, child1.getHeight());
        assertFuzzyEquals("Child 2 width as 50% of the container's available width",
                0.5f * availableWidth, child2.getWidth());
        assertFuzzyEquals("Child 2 height as 100% of the container's available height",
                availableHeight, child2.getHeight());
    }

    @Test
    public void testPercentRelaticeWithVerticalPaddings() {
        onView(withId(R.id.percent_layout)).check(doesNotExist());
        onView(withId(R.id.percent_stub)).perform(
                inflateViewStub(R.layout.percent_relative_layout_vpaddings));

        final PercentRelativeLayout percentRelativeLayout =
                (PercentRelativeLayout) mActivityTestRule.getActivity().findViewById(
                        R.id.percent_layout);
        final int containerWidth = percentRelativeLayout.getWidth();
        final int containerHeight = percentRelativeLayout.getHeight();

        final int availableWidth = containerWidth - percentRelativeLayout.getPaddingLeft()
                - percentRelativeLayout.getPaddingRight();
        final int availableHeight = containerHeight - percentRelativeLayout.getPaddingTop()
                - percentRelativeLayout.getPaddingBottom();

        final View child1 = percentRelativeLayout.findViewById(R.id.child1);
        final View child2 = percentRelativeLayout.findViewById(R.id.child2);

        assertFuzzyEquals("Child 1 width as 100% of the container's available width",
                availableWidth, child1.getWidth());
        assertFuzzyEquals("Child 1 height as 50% of the container's available height",
                0.5f * availableHeight, child1.getHeight());
        assertFuzzyEquals("Child 2 width as 100% of the container's available width",
                availableWidth, child2.getWidth());
        assertFuzzyEquals("Child 2 height as 50% of the container's available height",
                0.5f* availableHeight, child2.getHeight());
    }
}
