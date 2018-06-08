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

import android.support.test.filters.SmallTest;
import android.view.View;

import androidx.percentlayout.test.R;

import org.junit.Before;
import org.junit.Test;

/**
 * The arrangement of child views in the layout class is as follows:
 *
 *  +---------------------------------------------+
 *  |                                             |
 *  |    TTTTTTTTTTTTTTTTTTTTT                    |
 *  |                                             |
 *  | L                                           |
 *  | L           CCCCCCCCCCCCCCCCCC              |
 *  | L           CCCCCCCCCCCCCCCCCC              |
 *  | L           CCCCCCCCCCCCCCCCCC           R  |
 *  | L           CCCCCCCCCCCCCCCCCC           R  |
 *  | L           CCCCCCCCCCCCCCCCCC           R  |
 *  |             CCCCCCCCCCCCCCCCCC           R  |
 *  |             CCCCCCCCCCCCCCCCCC           R  |
 *  |                                          R  |
 *  |                                             |
 *  |                    BBBBBBBBBBBBBBBBBBBBB    |
 *  |                                             |
 *  +---------------------------------------------+
 *
 * Child views are exercising the following percent-based constraints supported by
 * <code>PercentRelativeLayout</code>:
 *
 * <ul>
 *     <li>Top child (marked with T) - width, aspect ratio, top margin, left margin.</li>
 *     <li>Left child (marked with L) - height, aspect ratio, top margin, left margin.</li>
 *     <li>Bottom child (marked with B) - width, aspect ratio, bottom margin, right margin.</li>
 *     <li>Right child (marked with R) - height, aspect ratio, bottom margin, right margin.</li>
 *     <li>Center child (marked with C) - margin (all sides) from the other four children.</li>
 * </ul>
 */
@SmallTest
public class PercentRelativeTest extends BaseInstrumentationTestCase<TestRelativeActivity> {
    private PercentRelativeLayout mPercentRelativeLayout;
    private int mContainerWidth;
    private int mContainerHeight;

    public PercentRelativeTest() {
        super(TestRelativeActivity.class);
    }

    @Before
    public void setUp() throws Exception {
        final TestRelativeActivity activity = mActivityTestRule.getActivity();
        mPercentRelativeLayout = (PercentRelativeLayout) activity.findViewById(R.id.container);
        mContainerWidth = mPercentRelativeLayout.getWidth();
        mContainerHeight = mPercentRelativeLayout.getHeight();
    }

    @Test
    public void testTopChild() {
        final View childToTest = mPercentRelativeLayout.findViewById(R.id.child_top);

        final int childWidth = childToTest.getWidth();
        final int childHeight = childToTest.getHeight();

        assertFuzzyEquals("Child width as 50% of the container",
                0.5f * mContainerWidth, childWidth);
        assertFuzzyEquals("Child aspect ratio of 2000%",
                0.05f * childWidth, childHeight);

        final int childLeft = childToTest.getLeft();
        final int childTop = childToTest.getTop();

        assertFuzzyEquals("Child left margin as 20% of the container",
                0.2f * mContainerWidth, childLeft);
        assertFuzzyEquals("Child top margin as 5% of the container",
                0.05f * mContainerHeight, childTop);
    }

    @Test
    public void testLeftChild() {
        final View childToTest = mPercentRelativeLayout.findViewById(R.id.child_left);

        final int childWidth = childToTest.getWidth();
        final int childHeight = childToTest.getHeight();

        assertFuzzyEquals("Child height as 50% of the container",
                0.5f * mContainerHeight, childHeight);
        assertFuzzyEquals("Child aspect ratio of 5%",
                0.05f * childHeight, childWidth);

        final int childLeft = childToTest.getLeft();
        final int childTop = childToTest.getTop();

        assertFuzzyEquals("Child left margin as 5% of the container",
                0.05f * mContainerWidth, childLeft);
        assertFuzzyEquals("Child top margin as 20% of the container",
                0.2f * mContainerHeight, childTop);
    }

    @Test
    public void testBottomChild() {
        final View childToTest = mPercentRelativeLayout.findViewById(R.id.child_bottom);

        final int childWidth = childToTest.getWidth();
        final int childHeight = childToTest.getHeight();

        assertFuzzyEquals("Child width as 40% of the container",
                0.4f * mContainerWidth, childWidth);
        assertFuzzyEquals("Child aspect ratio of 2000%",
                0.05f * childWidth, childHeight);

        final int childRight = childToTest.getRight();
        final int childBottom = childToTest.getBottom();

        assertFuzzyEquals("Child right margin as 20% of the container",
                0.2f * mContainerWidth, mContainerWidth - childRight);
        assertFuzzyEquals("Child bottom margin as 5% of the container",
                0.05f * mContainerHeight, mContainerHeight - childBottom);
    }

    @Test
    public void testRightChild() {
        final View childToTest = mPercentRelativeLayout.findViewById(R.id.child_right);

        final int childWidth = childToTest.getWidth();
        final int childHeight = childToTest.getHeight();

        assertFuzzyEquals("Child height as 50% of the container",
                0.4f * mContainerHeight, childHeight);
        assertFuzzyEquals("Child aspect ratio of 5%",
                0.05f * childHeight, childWidth);

        final int childRight = childToTest.getRight();
        final int childBottom = childToTest.getBottom();

        assertFuzzyEquals("Child right margin as 5% of the container",
                0.05f * mContainerWidth, mContainerWidth - childRight);
        assertFuzzyEquals("Child bottom margin as 20% of the container",
                0.2f * mContainerHeight, mContainerHeight - childBottom);
    }

    @Test
    public void testCenterChild() {
        final View childToTest = mPercentRelativeLayout.findViewById(R.id.child_center);

        final int childLeft = childToTest.getLeft();
        final int childTop = childToTest.getTop();
        final int childRight = childToTest.getRight();
        final int childBottom = childToTest.getBottom();

        final View leftChild = mPercentRelativeLayout.findViewById(R.id.child_left);
        assertFuzzyEquals("Child left margin as 10% of the container",
                leftChild.getRight() + 0.1f * mContainerWidth, childLeft);

        final View topChild = mPercentRelativeLayout.findViewById(R.id.child_top);
        assertFuzzyEquals("Child top margin as 10% of the container",
                topChild.getBottom() + 0.1f * mContainerHeight, childTop);

        final View rightChild = mPercentRelativeLayout.findViewById(R.id.child_right);
        assertFuzzyEquals("Child right margin as 10% of the container",
                rightChild.getLeft() - 0.1f * mContainerWidth, childRight);

        final View bottomChild = mPercentRelativeLayout.findViewById(R.id.child_bottom);
        assertFuzzyEquals("Child bottom margin as 10% of the container",
                bottomChild.getTop() - 0.1f * mContainerHeight, childBottom);
    }
}
