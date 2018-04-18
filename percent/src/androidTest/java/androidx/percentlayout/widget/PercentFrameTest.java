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
import static android.support.test.espresso.matcher.ViewMatchers.withId;

import android.os.Build;
import android.support.test.filters.SmallTest;
import android.view.View;

import androidx.core.view.ViewCompat;
import androidx.percentlayout.test.R;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class PercentFrameTest extends BaseInstrumentationTestCase<TestFrameActivity> {
    private PercentFrameLayout mPercentFrameLayout;
    private int mContainerWidth;
    private int mContainerHeight;

    public PercentFrameTest() {
        super(TestFrameActivity.class);
    }

    @Before
    public void setUp() throws Exception {
        final TestFrameActivity activity = mActivityTestRule.getActivity();
        mPercentFrameLayout = (PercentFrameLayout) activity.findViewById(R.id.container);
        mContainerWidth = mPercentFrameLayout.getWidth();
        mContainerHeight = mPercentFrameLayout.getHeight();
    }

    @Test
    public void testWidthHeight() {
        View childToTest = mPercentFrameLayout.findViewById(R.id.child_width_height);

        int childWidth = childToTest.getWidth();
        int childHeight = childToTest.getHeight();

        assertFuzzyEquals("Child width as 50% of the container",
                0.5f * mContainerWidth, childWidth);
        assertFuzzyEquals("Child height as 50% of the container",
                0.5f * mContainerHeight, childHeight);
    }

    @Test
    public void testWidthAspectRatio() {
        View childToTest = mPercentFrameLayout.findViewById(R.id.child_width_ratio);

        int childWidth = childToTest.getWidth();
        int childHeight = childToTest.getHeight();

        assertFuzzyEquals("Child width as 60% of the container",
                0.6f * mContainerWidth, childWidth);
        assertFuzzyEquals("Child aspect ratio of 120%",
                childWidth / 1.2f, childHeight);
    }

    @Test
    public void testHeightAspectRatio() {
        View childToTest = mPercentFrameLayout.findViewById(R.id.child_height_ratio);

        int childWidth = childToTest.getWidth();
        int childHeight = childToTest.getHeight();

        assertFuzzyEquals("Child height as 50% of the container",
                0.5f * mContainerHeight, childHeight);
        assertFuzzyEquals("Child aspect ratio of 150%",
                1.5f * childHeight, childWidth);
    }

    @Test
    public void testMarginsSingle() {
        View childToTest = mPercentFrameLayout.findViewById(R.id.child_margins_single);

        int childLeft = childToTest.getLeft();
        int childTop = childToTest.getTop();
        int childRight = childToTest.getRight();
        int childBottom = childToTest.getBottom();

        assertFuzzyEquals("Child left margin as 30% of the container",
                0.3f * mContainerWidth, childLeft);
        assertFuzzyEquals("Child top margin as 30% of the container",
                0.3f * mContainerHeight, childTop);
        assertFuzzyEquals("Child right margin as 30% of the container",
                0.3f * mContainerWidth, mContainerWidth - childRight);
        assertFuzzyEquals("Child bottom margin as 30% of the container",
                0.3f * mContainerHeight, mContainerHeight - childBottom);
    }

    @Test
    public void testMarginsMultiple() {
        View childToTest = mPercentFrameLayout.findViewById(R.id.child_margins_multiple);

        int childLeft = childToTest.getLeft();
        int childTop = childToTest.getTop();
        int childRight = childToTest.getRight();
        int childBottom = childToTest.getBottom();

        assertFuzzyEquals("Child top margin as 10% of the container",
                0.1f * mContainerHeight, childTop);
        assertFuzzyEquals("Child left margin as 15% of the container",
                0.15f * mContainerWidth, childLeft);
        assertFuzzyEquals("Child bottom margin as 20% of the container",
                0.2f * mContainerHeight, mContainerHeight - childBottom);
        assertFuzzyEquals("Child right margin as 25% of the container",
                0.25f * mContainerWidth, mContainerWidth - childRight);
    }

    @Test
    public void testMarginsTopLeft() {
        View childToTest = mPercentFrameLayout.findViewById(R.id.child_margins_top_left);

        int childWidth = childToTest.getWidth();
        int childHeight = childToTest.getHeight();
        int childLeft = childToTest.getLeft();
        int childTop = childToTest.getTop();

        assertFuzzyEquals("Child width as 50% of the container",
                0.5f * mContainerWidth, childWidth);
        assertFuzzyEquals("Child height as 50% of the container",
                0.5f * mContainerHeight, childHeight);
        assertFuzzyEquals("Child left margin as 20% of the container",
                0.2f * mContainerWidth, childLeft);
        assertFuzzyEquals("Child top margin as 20% of the container",
                0.2f * mContainerHeight, childTop);
    }

    @Test
    public void testMarginsBottomRight() {
        View childToTest = mPercentFrameLayout.findViewById(R.id.child_margins_bottom_right);

        int childWidth = childToTest.getWidth();
        int childHeight = childToTest.getHeight();
        int childRight = childToTest.getRight();
        int childBottom = childToTest.getBottom();

        assertFuzzyEquals("Child width as 60% of the container",
                0.6f * mContainerWidth, childWidth);
        assertFuzzyEquals("Child height as 60% of the container",
                0.6f * mContainerHeight, childHeight);
        assertFuzzyEquals("Child right margin as 10% of the container",
                0.1f * mContainerWidth, mContainerWidth - childRight);
        assertFuzzyEquals("Child bottom margin as 10% of the container",
                0.1f * mContainerHeight, mContainerHeight - childBottom);
    }

    @Test
    public void testMarginStart() throws Throwable {
        View childToTest = mPercentFrameLayout.findViewById(R.id.child_margin_start);

        // Under LTR test that start is treated as left
        int childLeft = childToTest.getLeft();
        assertFuzzyEquals("Child start margin as 20% of the container",
                0.2f * mContainerWidth, childLeft);
    }

    @Test
    public void testMarginStartRtl() throws Throwable {
        View childToTest = mPercentFrameLayout.findViewById(R.id.child_margin_start);

        if (Build.VERSION.SDK_INT >= 17) {
            // Force our child to inherit parent's layout direction
            onView(withId(R.id.child_margin_start)).perform(
                    LayoutDirectionActions.setLayoutDirection(ViewCompat.LAYOUT_DIRECTION_INHERIT));
            // And force the container to RTL mode
            onView(withId(R.id.container)).perform(
                    LayoutDirectionActions.setLayoutDirection(ViewCompat.LAYOUT_DIRECTION_RTL));

            // Force a full measure + layout pass on the container
            mPercentFrameLayout.measure(
                    View.MeasureSpec.makeMeasureSpec(mContainerWidth, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(mContainerHeight, View.MeasureSpec.EXACTLY));
            mPercentFrameLayout.layout(mPercentFrameLayout.getLeft(),
                    mPercentFrameLayout.getTop(), mPercentFrameLayout.getRight(),
                    mPercentFrameLayout.getBottom());

            // Start under RTL should be treated as right
            int childRight = childToTest.getRight();
            assertFuzzyEquals("Child start margin as 20% of the container",
                    0.2f * mContainerWidth, mContainerWidth - childRight);
        } else {
            // On pre-v17 devices test that start is treated as left
            int childLeft = childToTest.getLeft();
            assertFuzzyEquals("Child start margin as 20% of the container",
                    0.2f * mContainerWidth, childLeft);
        }
    }

    @Test
    public void testMarginEnd() throws Throwable {
        View childToTest = mPercentFrameLayout.findViewById(R.id.child_margin_end);

        // Under LTR test that end is treated as right
        int childRight = childToTest.getRight();
        assertFuzzyEquals("Child end margin as 30% of the container",
                0.3f * mContainerWidth, mContainerWidth - childRight);
    }

    @Test
    public void testMarginEndRtl() throws Throwable {
        View childToTest = mPercentFrameLayout.findViewById(R.id.child_margin_end);

        if (Build.VERSION.SDK_INT >= 17) {
            // Force our child to inherit parent's layout direction
            onView(withId(R.id.child_margin_end)).perform(
                    LayoutDirectionActions.setLayoutDirection(ViewCompat.LAYOUT_DIRECTION_INHERIT));
            // And force the container to RTL mode
            onView(withId(R.id.container)).perform(
                    LayoutDirectionActions.setLayoutDirection(ViewCompat.LAYOUT_DIRECTION_RTL));

            // Force a full measure + layout pass on the container
            mPercentFrameLayout.measure(
                    View.MeasureSpec.makeMeasureSpec(mContainerWidth, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(mContainerHeight, View.MeasureSpec.EXACTLY));
            mPercentFrameLayout.layout(mPercentFrameLayout.getLeft(),
                    mPercentFrameLayout.getTop(), mPercentFrameLayout.getRight(),
                    mPercentFrameLayout.getBottom());

            // End under RTL should be treated as left
            int childLeft = childToTest.getLeft();
            assertFuzzyEquals("Child end margin as 30% of the container",
                    0.3f * mContainerWidth, childLeft);
        } else {
            // On pre-v17 devices test that end is treated as right
            int childRight = childToTest.getRight();
            assertFuzzyEquals("Child end margin as 30% of the container",
                    0.3f * mContainerWidth, mContainerWidth - childRight);
        }
    }
}
