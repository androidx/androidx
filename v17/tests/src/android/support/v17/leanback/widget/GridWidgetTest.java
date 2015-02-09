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
package android.support.v17.leanback.widget;

import android.support.v17.leanback.tests.R;

import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.app.Instrumentation;
import android.content.Intent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

/**
 * @hide from javadoc
 */
public class GridWidgetTest extends ActivityInstrumentationTestCase2<GridActivity> {

    protected GridActivity mActivity;
    protected Instrumentation mInstrumentation;
    protected BaseGridView mGridView;
    protected GridLayoutManager mLayoutManager;
    protected int mOrientation;
    protected int mNumRows;

    private final Comparator<View> mRowSortComparator = new Comparator<View>() {
        public int compare(View lhs, View rhs) {
            if (mOrientation == BaseGridView.HORIZONTAL) {
                return lhs.getLeft() - rhs.getLeft();
            } else {
                return lhs.getTop() - rhs.getTop();
            }
        };
    };

    /**
     * Verify margins between items on same row are same.
     */
    private final Runnable mVerifyLayout = new Runnable() {
        @Override
        public void run() {
            verifyMargin();
        }
    };

    public GridWidgetTest() {
        super("android.support.v17.leanback.tests", GridActivity.class);
    }

    /**
     * Wait for grid view stop scroll and optionally verify state of grid view.
     */
    protected void waitForScrollIdle(Runnable verify) throws Throwable {
        while (mGridView.getLayoutManager().isSmoothScrolling() ||
                mGridView.getScrollState() != BaseGridView.SCROLL_STATE_IDLE) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                break;
            }
            if (verify != null) {
                runTestOnUiThread(verify);
            }
        }
    }

    /**
     * Wait for grid view stop animation and optionally verify state of grid view.
     */
    protected void waitForTransientStateGone(Runnable verify) throws Throwable {
        do {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                break;
            }
            if (verify != null) {
                runTestOnUiThread(verify);
            }
        } while (mGridView.hasTransientState());
    }

    /**
     * Wait for grid view stop scroll.
     */
    protected void waitForScrollIdle() throws Throwable {
        waitForScrollIdle(null);
    }

    /**
     * Scrolls using given key.
     */
    protected void scroll(int key, Runnable verify) throws Throwable {
        do {
            if (verify != null) {
                runTestOnUiThread(verify);
            }
            sendRepeatedKeys(10, key);
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                break;
            }
        } while (mGridView.getLayoutManager().isSmoothScrolling() ||
                mGridView.getScrollState() != BaseGridView.SCROLL_STATE_IDLE);
    }

    protected void scrollToBegin(Runnable verify) throws Throwable {
        int key;
        if (mOrientation == BaseGridView.HORIZONTAL) {
            if (mGridView.getLayoutDirection() == ViewGroup.LAYOUT_DIRECTION_RTL) {
                key = KeyEvent.KEYCODE_DPAD_RIGHT;
            } else {
                key = KeyEvent.KEYCODE_DPAD_LEFT;
            }
        } else {
            key = KeyEvent.KEYCODE_DPAD_UP;
        }
        scroll(key, verify);
    }

    protected void scrollToEnd(Runnable verify) throws Throwable {
        int key;
        if (mOrientation == BaseGridView.HORIZONTAL) {
            if (mGridView.getLayoutDirection() == ViewGroup.LAYOUT_DIRECTION_RTL) {
                key = KeyEvent.KEYCODE_DPAD_LEFT;
            } else {
                key = KeyEvent.KEYCODE_DPAD_RIGHT;
            }
        } else {
            key = KeyEvent.KEYCODE_DPAD_DOWN;
        }
        scroll(key, verify);
    }

    /**
     * Group and sort children by their position on each row (HORIZONTAL) or column(VERTICAL).
     */
    protected View[][] sortByRows() {
        final HashMap<Integer, ArrayList<View>> rows = new HashMap<Integer, ArrayList<View>>();
        ArrayList<Integer> rowLocations = new ArrayList();
        for (int i = 0; i < mGridView.getChildCount(); i++) {
            View v = mGridView.getChildAt(i);
            int rowLocation;
            if (mOrientation == BaseGridView.HORIZONTAL) {
                rowLocation = v.getTop();
            } else {
                rowLocation = mGridView.getLayoutDirection() == ViewGroup.LAYOUT_DIRECTION_RTL ?
                    v.getRight() : v.getLeft();
            }
            ArrayList<View> views = rows.get(rowLocation);
            if (views == null) {
                views = new ArrayList<View>();
                rows.put(rowLocation, views);
                rowLocations.add(rowLocation);
            }
            views.add(v);
        }
        Object[] sortedLocations = rowLocations.toArray();
        Arrays.sort(sortedLocations);
        if (mNumRows != rows.size()) {
            assertEquals("Dump Views by rows "+rows, mNumRows, rows.size());
        }
        View[][] sorted = new View[rows.size()][];
        for (int i = 0; i < rowLocations.size(); i++) {
            Integer rowLocation = rowLocations.get(i);
            ArrayList<View> arr = rows.get(rowLocation);
            View[] views = arr.toArray(new View[arr.size()]);
            Arrays.sort(views, mRowSortComparator);
            sorted[i] = views;
        }
        return sorted;
    }

    protected void verifyMargin() {
        View[][] sorted = sortByRows();
        for (int row = 0; row < sorted.length; row++) {
            View[] views = sorted[row];
            int margin = -1;
            for (int i = 1; i < views.length; i++) {
                if (mOrientation == BaseGridView.HORIZONTAL) {
                    if (i == 1) {
                        margin = views[i].getLeft() - views[i - 1].getRight();
                    } else {
                        assertEquals(margin, views[i].getLeft() - views[i - 1].getRight());
                    }
                } else {
                    if (i == 1) {
                        margin = views[i].getTop() - views[i - 1].getBottom();
                    } else {
                        assertEquals(margin, views[i].getTop() - views[i - 1].getBottom());
                    }
                }
            }
        }
    }

    protected void verifyBeginAligned() {
        View[][] sorted = sortByRows();
        int alignedLocation = 0;
        if (mOrientation == BaseGridView.HORIZONTAL) {
            if (mGridView.getLayoutDirection() == ViewGroup.LAYOUT_DIRECTION_RTL) {
                for (int i = 0; i < sorted.length; i++) {
                    if (i == 0) {
                        alignedLocation = sorted[i][sorted[i].length - 1].getRight();
                    } else {
                        assertEquals(alignedLocation, sorted[i][sorted[i].length - 1].getRight());
                    }
                }
            } else {
                for (int i = 0; i < sorted.length; i++) {
                    if (i == 0) {
                        alignedLocation = sorted[i][0].getLeft();
                    } else {
                        assertEquals(alignedLocation, sorted[i][0].getLeft());
                    }
                }
            }
        } else {
            for (int i = 0; i < sorted.length; i++) {
                if (i == 0) {
                    alignedLocation = sorted[i][0].getTop();
                } else {
                    assertEquals(alignedLocation, sorted[i][0].getTop());
                }
            }
        }
    }

    protected int[] getEndEdges() {
        View[][] sorted = sortByRows();
        int[] edges = new int[sorted.length];
        if (mOrientation == BaseGridView.HORIZONTAL) {
            if (mGridView.getLayoutDirection() == ViewGroup.LAYOUT_DIRECTION_RTL) {
                for (int i = 0; i < sorted.length; i++) {
                    edges[i] = sorted[i][0].getLeft();
                }
            } else {
                for (int i = 0; i < sorted.length; i++) {
                    edges[i] = sorted[i][sorted[i].length - 1].getRight();
                }
            }
        } else {
            for (int i = 0; i < sorted.length; i++) {
                edges[i] = sorted[i][sorted[i].length - 1].getBottom();
            }
        }
        return edges;
    }

    protected void verifyEdgesSame(int[] edges, int[] edges2) {
        assertEquals(edges.length, edges2.length);
        for (int i = 0; i < edges.length; i++) {
            assertEquals(edges[i], edges2[i]);
        }
    }

    protected void verifyBoundCount(int count) {
        if (mActivity.getBoundCount() != count) {
            StringBuffer b = new StringBuffer();
            b.append("ItemsLength: ");
            for (int i = 0; i < mActivity.mItemLengths.length; i++) {
                b.append(mActivity.mItemLengths[i]).append(",");
            }
            assertEquals("Bound count does not match, ItemsLengths: "+ b,
                    count, mActivity.getBoundCount());
        }
    }

    public void testThreeRowHorizontalBasic() throws Throwable {

        mInstrumentation = getInstrumentation();
        Intent intent = new Intent(mInstrumentation.getContext(), GridActivity.class);
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.horizontal_grid);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 100);
        setActivityIntent(intent);
        mActivity = getActivity();
        mGridView = mActivity.mGridView;
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 3;

        scrollToEnd(mVerifyLayout);
        verifyBoundCount(100);

        scrollToBegin(mVerifyLayout);

        verifyBeginAligned();
    }

    public void testThreeColumnVerticalBasic() throws Throwable {

        mInstrumentation = getInstrumentation();
        Intent intent = new Intent(mInstrumentation.getContext(), GridActivity.class);
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.vertical_grid);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 200);
        setActivityIntent(intent);
        mActivity = getActivity();
        mGridView = mActivity.mGridView;
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 3;

        scrollToEnd(mVerifyLayout);
        verifyBoundCount(200);

        scrollToBegin(mVerifyLayout);

        verifyBeginAligned();
    }

    public void testRedundantAppendRemove() throws Throwable {
        mInstrumentation = getInstrumentation();
        Intent intent = new Intent(mInstrumentation.getContext(), GridActivity.class);
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_grid_testredundantappendremove);
        intent.putExtra(GridActivity.EXTRA_ITEMS, new int[]{
                149,177,128,234,227,187,163,223,146,210,228,148,227,193,182,197,177,142,225,207,
                157,171,209,204,187,184,123,221,197,153,202,179,193,214,226,173,225,143,188,159,
                139,193,233,143,227,203,222,124,228,223,164,131,228,126,211,160,165,152,235,184,
                155,224,149,181,171,229,200,234,177,130,164,172,188,139,132,203,179,220,147,131,
                226,127,230,239,183,203,206,227,123,170,239,234,200,149,237,204,160,133,202,234,
                173,122,139,149,151,153,216,231,121,145,227,153,186,174,223,180,123,215,206,216,
                239,222,219,207,193,218,140,133,171,153,183,132,233,138,159,174,189,171,143,128,
                152,222,141,202,224,190,134,120,181,231,230,136,132,224,136,210,207,150,128,183,
                221,194,179,220,126,221,137,205,223,193,172,132,226,209,133,191,227,127,159,171,
                180,149,237,177,194,207,170,202,161,144,147,199,205,186,164,140,193,203,224,129});
        setActivityIntent(intent);
        mActivity = getActivity();
        mGridView = mActivity.mGridView;
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 3;

        scrollToEnd(mVerifyLayout);

        verifyBoundCount(200);

        scrollToBegin(mVerifyLayout);

        verifyBeginAligned();
    }

    public void testRedundantAppendRemove2() throws Throwable {
        mInstrumentation = getInstrumentation();
        Intent intent = new Intent(mInstrumentation.getContext(), GridActivity.class);
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_grid_testredundantappendremove2);
        intent.putExtra(GridActivity.EXTRA_ITEMS, new int[]{
                318,333,199,224,246,273,269,289,340,313,265,306,349,269,185,282,257,354,316,252,
                237,290,283,343,196,313,290,343,191,262,342,228,343,349,251,203,226,305,265,213,
                216,333,295,188,187,281,288,311,244,232,224,332,290,181,267,276,226,261,335,355,
                225,217,219,183,234,285,257,304,182,250,244,223,257,219,342,185,347,205,302,315,
                299,309,292,237,192,309,228,250,347,227,337,298,299,185,185,331,223,284,265,351});
        setActivityIntent(intent);
        mActivity = getActivity();
        mGridView = mActivity.mGridView;
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 3;
        mLayoutManager = (GridLayoutManager) mGridView.getLayoutManager();

        // test append without staggered result cache
        scrollToEnd(mVerifyLayout);

        verifyBoundCount(100);
        int[] endEdges = getEndEdges();

        scrollToBegin(mVerifyLayout);

        verifyBeginAligned();

        // now test append with staggered result cache
        runTestOnUiThread(new Runnable() {
            public void run() {
                mActivity.changeArraySize(3);
            }
        });
        Thread.sleep(500);
        assertEquals("Staggerd cache should be kept as is when no item size change",
                100, ((StaggeredGrid) mLayoutManager.mGrid).mLocations.size());

        mActivity.resetBoundCount();
        runTestOnUiThread(new Runnable() {
            public void run() {
                mActivity.changeArraySize(100);
            }
        });
        Thread.sleep(500);

        scrollToEnd(mVerifyLayout);
        verifyBoundCount(100);

        // we should get same aligned end edges
        int[] endEdges2 = getEndEdges();
        verifyEdgesSame(endEdges, endEdges2);
    }

    public void testItemMovedHorizontal() throws Throwable {

        mInstrumentation = getInstrumentation();
        Intent intent = new Intent(mInstrumentation.getContext(), GridActivity.class);
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_grid);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 200);
        setActivityIntent(intent);
        mActivity = getActivity();
        mGridView = mActivity.mGridView;
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 3;

        mGridView.setSelectedPositionSmooth(150);
        waitForScrollIdle(mVerifyLayout);
        mActivity.swap(150, 152);
        waitForTransientStateGone(null);

        runTestOnUiThread(mVerifyLayout);

        scrollToBegin(mVerifyLayout);

        verifyBeginAligned();
    }

    public void testItemMovedVertical() throws Throwable {

        mInstrumentation = getInstrumentation();
        Intent intent = new Intent(mInstrumentation.getContext(), GridActivity.class);
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_grid);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 200);
        setActivityIntent(intent);
        mActivity = getActivity();
        mGridView = mActivity.mGridView;
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 3;

        mGridView.setSelectedPositionSmooth(150);
        waitForScrollIdle(mVerifyLayout);
        mActivity.swap(150, 152);
        waitForTransientStateGone(null);

        runTestOnUiThread(mVerifyLayout);

        scrollToEnd(mVerifyLayout);
        scrollToBegin(mVerifyLayout);

        verifyBeginAligned();
    }

    public void testItemAddRemoveHorizontal() throws Throwable {

        mInstrumentation = getInstrumentation();
        Intent intent = new Intent(mInstrumentation.getContext(), GridActivity.class);
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_grid);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 200);
        setActivityIntent(intent);
        mActivity = getActivity();
        mGridView = mActivity.mGridView;
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 3;

        scrollToEnd(mVerifyLayout);
        int[] endEdges = getEndEdges();

        mGridView.setSelectedPositionSmooth(150);
        waitForScrollIdle(mVerifyLayout);
        int[] removedItems = mActivity.removeItems(151, 4);
        waitForTransientStateGone(null);

        scrollToEnd(mVerifyLayout);
        mGridView.setSelectedPositionSmooth(150);
        waitForScrollIdle(mVerifyLayout);

        mActivity.addItems(151, removedItems);
        waitForTransientStateGone(null);
        scrollToEnd(mVerifyLayout);

        // we should get same aligned end edges
        int[] endEdges2 = getEndEdges();
        verifyEdgesSame(endEdges, endEdges2);

        scrollToBegin(mVerifyLayout);
        verifyBeginAligned();
    }

}
