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
    protected int mOrientation;

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
     * Wait for grid view stop scroll.
     */
    protected void waitForScrollIdle() throws Throwable {
        waitForScrollIdle(null);
    }

    /**
     * Group and sort children by their position on each row (HORIZONTAL) or column(VERTICAL).
     */
    protected View[][] sortByRows() {
        final HashMap<Integer, ArrayList<View>> rows = new HashMap<Integer, ArrayList<View>>();
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
            }
            views.add(v);
        }
        assertEquals(rows.size(), mActivity.mRows);
        View[][] sorted = new View[rows.size()][];
        int i = 0;
        for (Iterator<ArrayList<View>> iter = rows.values().iterator(); iter.hasNext(); ) {
            ArrayList<View> arr = iter.next();
            View[] views = arr.toArray(new View[arr.size()]);
            Arrays.sort(views, mRowSortComparator);
            sorted[i++] = views;
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

    public void testThreeRowHorizontalBasic() throws Throwable {

        mInstrumentation = getInstrumentation();
        Intent intent = new Intent(mInstrumentation.getContext(), GridActivity.class);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 100);
        intent.putExtra(GridActivity.EXTRA_ROWS, 3);
        mOrientation = BaseGridView.HORIZONTAL;
        intent.putExtra(GridActivity.EXTRA_ORIENTATION, mOrientation);
        setActivityIntent(intent);
        mActivity = getActivity();
        mGridView = mActivity.mGridView;

        sendRepeatedKeys(100, KeyEvent.KEYCODE_DPAD_RIGHT);
        waitForScrollIdle(mVerifyLayout);

        assertEquals(mActivity.getBoundCount(), 100);

        sendRepeatedKeys(100, KeyEvent.KEYCODE_DPAD_LEFT);
        waitForScrollIdle(mVerifyLayout);

        verifyBeginAligned();
    }

    public void testThreeColumnVerticalBasic() throws Throwable {

        mInstrumentation = getInstrumentation();
        Intent intent = new Intent(mInstrumentation.getContext(), GridActivity.class);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 200);
        intent.putExtra(GridActivity.EXTRA_ROWS, 3);
        mOrientation = BaseGridView.VERTICAL;
        intent.putExtra(GridActivity.EXTRA_ORIENTATION, mOrientation);
        setActivityIntent(intent);
        mActivity = getActivity();
        mGridView = mActivity.mGridView;

        sendRepeatedKeys(100, KeyEvent.KEYCODE_DPAD_DOWN);
        waitForScrollIdle(mVerifyLayout);

        assertEquals(mActivity.getBoundCount(), 200);

        sendRepeatedKeys(100, KeyEvent.KEYCODE_DPAD_UP);
        waitForScrollIdle(mVerifyLayout);

        verifyBeginAligned();
    }
}
