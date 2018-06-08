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

package androidx.recyclerview.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.view.View;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;


@SmallTest
@RunWith(JUnit4.class)
public class ViewBoundsCheckTest {


    private static final String TAG = "ViewBoundsCheckTest";
    private Context mContext;

    /** Case #1:
     * Parent:                    [2.......................8]
     Views: [-3...-1] [-1...1] [1...3] [3...5] [5...7] [7...9] [9...11] [11...13]
     */
    int[] mParentBound1 = {2, 8};
    int[][] mChildrenBound1 = {{-3, -1}, {-1, 1}, {1, 3}, {3, 5}, {5, 7}, {7, 9}, {9, 11},
            {11, 13}};

    /** Case #2:
     * Parent:                  [1...................7]
     Views: [-3...-1] [-1...1][1...3] [3...5] [5...7] [7...9] [9...11]
     */
    int[] mParentBound2 = {1, 7};
    int[][] mChildrenBound2 = {{-3, -1}, {-1, 1}, {1, 3}, {3, 5}, {5, 7}, {7, 9}, {9, 11}};

    View mParent;
    View[] mChildren;

    private final ViewBoundsCheck.Callback mBoundCheckCallback =
            new ViewBoundsCheck.Callback() {
                @Override
                public int getChildCount() {
                    return mChildren.length;
                }

                @Override
                public View getParent() {
                    return mParent;
                }

                @Override
                public View getChildAt(int index) {
                    return mChildren[index];
                }

                @Override
                public int getParentStart() {
                    return mParent.getLeft();
                }

                @Override
                public int getParentEnd() {
                    return mParent.getRight();
                }

                @Override
                public int getChildStart(View view) {
                    return view.getLeft();
                }

                @Override
                public int getChildEnd(View view) {
                    return view.getRight();
                }
            };

    ViewBoundsCheck mBoundCheck = new ViewBoundsCheck(mBoundCheckCallback);

    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getContext();
    }

    private void setUpViews(int[] parentBound, int[][] childrenBound) {
        mParent = new View(mContext);
        mParent.setLeft(parentBound[0]);
        mParent.setRight(parentBound[1]);
        mChildren = new View[childrenBound.length];
        for (int i = 0; i < childrenBound.length; i++) {
            mChildren[i] = new View(mContext);
            mChildren[i].setLeft(childrenBound[i][0]);
            mChildren[i].setRight(childrenBound[i][1]);
        }
    }

    @Test
    public void firstFullyVisibleChildFromStart() {
        setUpViews(mParentBound1, mChildrenBound1);
        @ViewBoundsCheck.ViewBounds int preferredBoundsFlag = ViewBoundsCheck.FLAG_CVS_GT_PVS
                | ViewBoundsCheck.FLAG_CVS_EQ_PVS | ViewBoundsCheck.FLAG_CVE_LT_PVE
                | ViewBoundsCheck.FLAG_CVE_EQ_PVE;
        @ViewBoundsCheck.ViewBounds int acceptableBoundsFlag = 0;
        View view = mBoundCheck.findOneViewWithinBoundFlags(0, mChildren.length,
                preferredBoundsFlag, acceptableBoundsFlag);
        assertEquals("The first fully visible child from start should be returned", 3,
                view.getLeft());
        assertEquals("The first fully visible child from start should be returned", 5,
                view.getRight());
    }

    @Test
    public void firstFullyVisibleChildFromEnd() {
        setUpViews(mParentBound1, mChildrenBound1);
        @ViewBoundsCheck.ViewBounds int preferredBoundsFlag = ViewBoundsCheck.FLAG_CVS_GT_PVS
                | ViewBoundsCheck.FLAG_CVS_EQ_PVS | ViewBoundsCheck.FLAG_CVE_LT_PVE
                | ViewBoundsCheck.FLAG_CVE_EQ_PVE;
        @ViewBoundsCheck.ViewBounds int acceptableBoundsFlag = 0;
        View view = mBoundCheck.findOneViewWithinBoundFlags(mChildren.length - 1, -1,
                preferredBoundsFlag, acceptableBoundsFlag);
        assertEquals("The first fully visible child from end should be returned", 5,
                view.getLeft());
        assertEquals("The first fully visible child from end should be returned", 7,
                view.getRight());
    }

    @Test
    public void firstPartiallyOrFullyVisibleChildFromStartWithViewBoundsNotAligned() {
        setUpViews(mParentBound1, mChildrenBound1);
        // These set of flags are used in LinearLayoutManager#findOneVisibleChild
        @ViewBoundsCheck.ViewBounds int preferredBoundsFlag = (ViewBoundsCheck.FLAG_CVS_LT_PVE
                | ViewBoundsCheck.FLAG_CVE_GT_PVS);
        @ViewBoundsCheck.ViewBounds int acceptableBoundsFlag = (ViewBoundsCheck.FLAG_CVS_LT_PVE
                | ViewBoundsCheck.FLAG_CVE_GT_PVS);
        View view = mBoundCheck.findOneViewWithinBoundFlags(0, mChildren.length,
                preferredBoundsFlag, acceptableBoundsFlag);
        assertEquals("The first partially visible child from start should be returned", 1,
                view.getLeft());
        assertEquals("The first partially visible child from start should be returned", 3,
                view.getRight());
    }

    @Test
    public void firstPartiallyOrFullyVisibleChildFromStartWithViewBoundsAligned() {
        setUpViews(mParentBound2, mChildrenBound2);
        // These set of flags are used in LinearLayoutManager#findOneVisibleChild
        @ViewBoundsCheck.ViewBounds int preferredBoundsFlag = (ViewBoundsCheck.FLAG_CVS_LT_PVE
                | ViewBoundsCheck.FLAG_CVE_GT_PVS);
        @ViewBoundsCheck.ViewBounds int acceptableBoundsFlag = (ViewBoundsCheck.FLAG_CVS_LT_PVE
                | ViewBoundsCheck.FLAG_CVE_GT_PVS);
        View view = mBoundCheck.findOneViewWithinBoundFlags(0, mChildren.length,
                preferredBoundsFlag, acceptableBoundsFlag);
        assertEquals("The first partially visible child from start should be returned", 1,
                view.getLeft());
        assertEquals("The first partially visible child from start should be returned", 3,
                view.getRight());
    }

    @Test
    public void firstPartiallyOrFullyVisibleChildFromEndWithViewBoundsNotAligned() {
        setUpViews(mParentBound1, mChildrenBound1);
        // These set of flags are used in LinearLayoutManager#findOneVisibleChild
        @ViewBoundsCheck.ViewBounds int preferredBoundsFlag = (ViewBoundsCheck.FLAG_CVS_LT_PVE
                | ViewBoundsCheck.FLAG_CVE_GT_PVS);
        @ViewBoundsCheck.ViewBounds int acceptableBoundsFlag = (ViewBoundsCheck.FLAG_CVS_LT_PVE
                | ViewBoundsCheck.FLAG_CVE_GT_PVS);
        View view = mBoundCheck.findOneViewWithinBoundFlags(mChildren.length - 1, -1,
                preferredBoundsFlag, acceptableBoundsFlag);
        assertEquals("The first partially visible child from end should be returned", 7,
                view.getLeft());
        assertEquals("The first partially visible child from end should be returned", 9,
                view.getRight());
    }

    @Test
    public void firstPartiallyOrFullyVisibleChildFromEndWithViewBoundsAligned() {
        setUpViews(mParentBound2, mChildrenBound2);
        // These set of flags are used in LinearLayoutManager#findOneVisibleChild
        @ViewBoundsCheck.ViewBounds int preferredBoundsFlag = (ViewBoundsCheck.FLAG_CVS_LT_PVE
                | ViewBoundsCheck.FLAG_CVE_GT_PVS);
        @ViewBoundsCheck.ViewBounds int acceptableBoundsFlag = (ViewBoundsCheck.FLAG_CVS_LT_PVE
                | ViewBoundsCheck.FLAG_CVE_GT_PVS);
        View view = mBoundCheck.findOneViewWithinBoundFlags(mChildren.length - 1, -1,
                preferredBoundsFlag, acceptableBoundsFlag);
        assertEquals("The first partially visible child from end should be returned", 5,
                view.getLeft());
        assertEquals("The first partially visible child from end should be returned", 7,
                view.getRight());
    }

    @Test
    public void lastFullyInvisibleChildFromStart() {
        setUpViews(mParentBound2, mChildrenBound2);
        @ViewBoundsCheck.ViewBounds int  preferredBoundsFlag = (ViewBoundsCheck.FLAG_CVS_LT_PVS
                | ViewBoundsCheck.FLAG_CVE_LT_PVE | ViewBoundsCheck.FLAG_CVE_GT_PVS);
        @ViewBoundsCheck.ViewBounds int  acceptableBoundsFlag = (ViewBoundsCheck.FLAG_CVS_LT_PVS
                | ViewBoundsCheck.FLAG_CVE_LT_PVE);
        View view = mBoundCheck.findOneViewWithinBoundFlags(0, mChildren.length,
                preferredBoundsFlag, acceptableBoundsFlag);
        assertEquals("The last fully invisible child from start should be returned", -1,
                view.getLeft());
        assertEquals("TThe last fully invisible child from start should be returned", 1,
                view.getRight());
    }

    @Test
    public void lastFullyInvisibleChildFromEnd() {
        setUpViews(mParentBound2, mChildrenBound2);
        @ViewBoundsCheck.ViewBounds int preferredBoundsFlag = (ViewBoundsCheck.FLAG_CVE_GT_PVE
                | ViewBoundsCheck.FLAG_CVS_GT_PVS | ViewBoundsCheck.FLAG_CVS_LT_PVE);
        @ViewBoundsCheck.ViewBounds int acceptableBoundsFlag = (ViewBoundsCheck.FLAG_CVE_GT_PVE
                | ViewBoundsCheck.FLAG_CVS_GT_PVS);
        View view = mBoundCheck.findOneViewWithinBoundFlags(mChildren.length - 1, -1,
                preferredBoundsFlag, acceptableBoundsFlag);
        assertEquals("The last fully invisible child from end should be returned", 7,
                view.getLeft());
        assertEquals("TThe last fully invisible child from end should be returned", 9,
                view.getRight());
    }

    @Test
    public void noViewsFoundWithinGivenBounds() {
        setUpViews(mParentBound1, mChildrenBound1);
        // create a view whose bounds cover its parent. Since no such view exist in the example
        // layout, null should be returned.
        @ViewBoundsCheck.ViewBounds int preferredBoundsFlag = (ViewBoundsCheck.FLAG_CVS_LT_PVS
                | ViewBoundsCheck.FLAG_CVE_GT_PVE);
        @ViewBoundsCheck.ViewBounds int acceptableBoundsFlag = preferredBoundsFlag;
        View view = mBoundCheck.findOneViewWithinBoundFlags(0, mChildren.length,
                preferredBoundsFlag, acceptableBoundsFlag);
        assertNull("Null should be returned since no views are within the given bounds",
                view);
    }

}
