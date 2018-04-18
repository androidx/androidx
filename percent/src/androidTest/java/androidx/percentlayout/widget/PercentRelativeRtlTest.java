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

/**
 * The arrangement of child views in the layout class in the default LTR (left-to-right) direction
 * is as follows:
 *
 *  +---------------------------------------------+
 *  |                                             |
 *  |    TTTTTTTTTTTTTTTTTTTTT                    |
 *  |                                             |
 *  | S                                           |
 *  | S           CCCCCCCCCCCCCCCCCC              |
 *  | S           CCCCCCCCCCCCCCCCCC              |
 *  | S           CCCCCCCCCCCCCCCCCC           E  |
 *  | S           CCCCCCCCCCCCCCCCCC           E  |
 *  | S           CCCCCCCCCCCCCCCCCC           E  |
 *  |             CCCCCCCCCCCCCCCCCC           E  |
 *  |             CCCCCCCCCCCCCCCCCC           E  |
 *  |                                          E  |
 *  |                                             |
 *  |                    BBBBBBBBBBBBBBBBBBBBB    |
 *  |                                             |
 *  +---------------------------------------------+
 *
 * The arrangement of child views in the layout class in the RTL (right-to-left) direction
 * is as follows:
 *
 *  +---------------------------------------------+
 *  |                                             |
 *  |                    TTTTTTTTTTTTTTTTTTTTT    |
 *  |                                             |
 *  |                                          S  |
 *  |             CCCCCCCCCCCCCCCCCC           S  |
 *  |             CCCCCCCCCCCCCCCCCC           S  |
 *  | E           CCCCCCCCCCCCCCCCCC           S  |
 *  | E           CCCCCCCCCCCCCCCCCC           S  |
 *  | E           CCCCCCCCCCCCCCCCCC           S  |
 *  | E           CCCCCCCCCCCCCCCCCC              |
 *  | E           CCCCCCCCCCCCCCCCCC              |
 *  | E                                           |
 *  |                                             |
 *  |    BBBBBBBBBBBBBBBBBBBBB                    |
 *  |                                             |
 *  +---------------------------------------------+
 *
 * Child views are exercising the following percent-based constraints supported by
 * <code>PercentRelativeLayout</code>:
 *
 * <ul>
 *     <li>Top child (marked with T) - width, aspect ratio, top margin, start margin.</li>
 *     <li>Start child (marked with S) - height, aspect ratio, top margin, start margin.</li>
 *     <li>Bottom child (marked with B) - width, aspect ratio, bottom margin, end margin.</li>
 *     <li>Right child (marked with E) - height, aspect ratio, bottom margin, end margin.</li>
 *     <li>Center child (marked with C) - margin (all sides) from the other four children.</li>
 * </ul>
 *
 * Under LTR direction (pre-v17 devices and v17+ with default direction of en-US locale) we are
 * testing the same assertions as <code>PercentRelativeTest</code>. Under RTL direction (on v17+
 * devices with Espresso-powered direction switch) we are testing the reverse assertions along the
 * X axis for all child views.
 *
 * Note that due to a bug in the core {@link RelativeLayout} (base class of
 * {@link PercentRelativeLayout}) in how it treats end margin of child views on v17 devices, we are
 * skipping all tests in this class for v17 devices. This is in line with the overall contract
 * of percent-based layouts provided by the support library - we do not work around / fix bugs in
 * the core classes, but rather just provide a translation layer between percentage-based values
 * and pixel-based ones.
 */
@SmallTest
public class PercentRelativeRtlTest extends BaseInstrumentationTestCase<TestRelativeRtlActivity> {
    private PercentRelativeLayout mPercentRelativeLayout;
    private int mContainerWidth;
    private int mContainerHeight;

    public PercentRelativeRtlTest() {
        super(TestRelativeRtlActivity.class);
    }

    @Before
    public void setUp() throws Exception {
        final TestRelativeRtlActivity activity = mActivityTestRule.getActivity();
        mPercentRelativeLayout = (PercentRelativeLayout) activity.findViewById(R.id.container);
        mContainerWidth = mPercentRelativeLayout.getWidth();
        mContainerHeight = mPercentRelativeLayout.getHeight();
    }

    private void switchToRtl() {
        // Force the container to RTL mode
        onView(withId(R.id.container)).perform(
                LayoutDirectionActions.setLayoutDirection(ViewCompat.LAYOUT_DIRECTION_RTL));

        // Force a full measure + layout pass on the container
        mPercentRelativeLayout.measure(
                View.MeasureSpec.makeMeasureSpec(mContainerWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(mContainerHeight, View.MeasureSpec.EXACTLY));
        mPercentRelativeLayout.layout(mPercentRelativeLayout.getLeft(),
                mPercentRelativeLayout.getTop(), mPercentRelativeLayout.getRight(),
                mPercentRelativeLayout.getBottom());
    }

    @Test
    public void testTopChild() {
        if (Build.VERSION.SDK_INT == 17) {
            return;
        }
        final View childToTest = mPercentRelativeLayout.findViewById(R.id.child_top);

        if (Build.VERSION.SDK_INT >= 17) {
            switchToRtl();

            final int childRight = childToTest.getRight();
            assertFuzzyEquals("Child start margin as 20% of the container",
                    0.2f * mContainerWidth, mContainerWidth - childRight);
        } else {
            final int childLeft = childToTest.getLeft();
            assertFuzzyEquals("Child start margin as 20% of the container",
                    0.2f * mContainerWidth, childLeft);
        }

        final int childTop = childToTest.getTop();
        assertFuzzyEquals("Child top margin as 5% of the container",
                0.05f * mContainerHeight, childTop);

        final int childWidth = childToTest.getWidth();
        final int childHeight = childToTest.getHeight();

        assertFuzzyEquals("Child width as 50% of the container",
                0.5f * mContainerWidth, childWidth);
        assertFuzzyEquals("Child aspect ratio of 2000%",
                0.05f * childWidth, childHeight);
    }

    @Test
    public void testStartChild() {
        if (Build.VERSION.SDK_INT == 17) {
            return;
        }
        final View childToTest = mPercentRelativeLayout.findViewById(R.id.child_start);

        if (Build.VERSION.SDK_INT >= 17) {
            switchToRtl();

            final int childRight = childToTest.getRight();
            assertFuzzyEquals("Child start margin as 5% of the container",
                    0.05f * mContainerWidth, mContainerWidth - childRight);
        } else {
            final int childLeft = childToTest.getLeft();
            assertFuzzyEquals("Child start margin as 5% of the container",
                    0.05f * mContainerWidth, childLeft);
        }

        final int childWidth = childToTest.getWidth();
        final int childHeight = childToTest.getHeight();

        assertFuzzyEquals("Child height as 50% of the container",
                0.5f * mContainerHeight, childHeight);
        assertFuzzyEquals("Child aspect ratio of 5%",
                0.05f * childHeight, childWidth);

        final int childTop = childToTest.getTop();

        assertFuzzyEquals("Child top margin as 20% of the container",
                0.2f * mContainerHeight, childTop);
    }

    @Test
    public void testBottomChild() {
        if (Build.VERSION.SDK_INT == 17) {
            return;
        }
        final View childToTest = mPercentRelativeLayout.findViewById(R.id.child_bottom);

        if (Build.VERSION.SDK_INT >= 17) {
            switchToRtl();

            final int childLeft = childToTest.getLeft();
            assertFuzzyEquals("Child end margin as 20% of the container",
                    0.2f * mContainerWidth, childLeft);
        } else {
            final int childRight = childToTest.getRight();
            assertFuzzyEquals("Child end margin as 20% of the container",
                    0.2f * mContainerWidth, mContainerWidth - childRight);
        }


        final int childWidth = childToTest.getWidth();
        final int childHeight = childToTest.getHeight();

        assertFuzzyEquals("Child width as 40% of the container",
                0.4f * mContainerWidth, childWidth);
        assertFuzzyEquals("Child aspect ratio of 2000%",
                0.05f * childWidth, childHeight);

        final int childBottom = childToTest.getBottom();

        assertFuzzyEquals("Child bottom margin as 5% of the container",
                0.05f * mContainerHeight, mContainerHeight - childBottom);
    }

    @Test
    public void testEndChild() {
        if (Build.VERSION.SDK_INT == 17) {
            return;
        }
        final View childToTest = mPercentRelativeLayout.findViewById(R.id.child_end);

        if (Build.VERSION.SDK_INT >= 17) {
            switchToRtl();

            final int childLeft = childToTest.getLeft();
            assertFuzzyEquals("Child end margin as 5% of the container",
                    0.05f * mContainerWidth, childLeft);
        } else {
            final int childRight = childToTest.getRight();
            assertFuzzyEquals("Child end margin as 5% of the container",
                    0.05f * mContainerWidth, mContainerWidth - childRight);
        }

        final int childWidth = childToTest.getWidth();
        final int childHeight = childToTest.getHeight();

        assertFuzzyEquals("Child height as 50% of the container",
                0.4f * mContainerHeight, childHeight);
        assertFuzzyEquals("Child aspect ratio of 5%",
                0.05f * childHeight, childWidth);

        final int childBottom = childToTest.getBottom();

        assertFuzzyEquals("Child bottom margin as 20% of the container",
                0.2f * mContainerHeight, mContainerHeight - childBottom);
    }

    @Test
    public void testCenterChild() {
        if (Build.VERSION.SDK_INT == 17) {
            return;
        }
        final View childToTest = mPercentRelativeLayout.findViewById(R.id.child_center);

        boolean supportsRtl = Build.VERSION.SDK_INT >= 17;
        if (supportsRtl) {
            switchToRtl();
        }

        final int childLeft = childToTest.getLeft();
        final int childTop = childToTest.getTop();
        final int childRight = childToTest.getRight();
        final int childBottom = childToTest.getBottom();

        final View leftChild = supportsRtl
                ? mPercentRelativeLayout.findViewById(R.id.child_end)
                : mPercentRelativeLayout.findViewById(R.id.child_start);
        assertFuzzyEquals("Child left margin as 10% of the container",
                leftChild.getRight() + 0.1f * mContainerWidth, childLeft);

        final View topChild = mPercentRelativeLayout.findViewById(R.id.child_top);
        assertFuzzyEquals("Child top margin as 10% of the container",
                topChild.getBottom() + 0.1f * mContainerHeight, childTop);

        final View rightChild = supportsRtl
                ? mPercentRelativeLayout.findViewById(R.id.child_start)
                : mPercentRelativeLayout.findViewById(R.id.child_end);
        assertFuzzyEquals("Child right margin as 10% of the container",
                rightChild.getLeft() - 0.1f * mContainerWidth, childRight);

        final View bottomChild = mPercentRelativeLayout.findViewById(R.id.child_bottom);
        assertFuzzyEquals("Child bottom margin as 10% of the container",
                bottomChild.getTop() - 0.1f * mContainerHeight, childBottom);
    }
}
