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
        final HashMap<Integer, ArrayList<View>> mRows = new HashMap<Integer, ArrayList<View>>();
        @Override
        public void run() {
            mRows.clear();
            for (int i = 0; i < mGridView.getChildCount(); i++) {
                View v = mGridView.getChildAt(i);
                int rowLocation;
                if (mOrientation == BaseGridView.HORIZONTAL) {
                    rowLocation = v.getTop();
                } else {
                    rowLocation = mGridView.getLayoutDirection() == ViewGroup.LAYOUT_DIRECTION_RTL ?
                        v.getRight() : v.getLeft();
                }
                ArrayList<View> views = mRows.get(rowLocation);
                if (views == null) {
                    views = new ArrayList<View>();
                    mRows.put(rowLocation, views);
                }
                views.add(v);
            }
            assertEquals(mRows.size(), mActivity.mRows);
            for (Iterator<ArrayList<View>> iter = mRows.values().iterator(); iter.hasNext(); ) {
                ArrayList<View> arr = iter.next();
                View[] views = arr.toArray(new View[arr.size()]);
                Arrays.sort(views, mRowSortComparator);
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
            mRows.clear();
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

    public void testTwoRowBasic() throws Throwable {

        mInstrumentation = getInstrumentation();
        Intent intent = new Intent(mInstrumentation.getContext(), GridActivity.class);
        intent.putExtra(GridActivity.EXTRA_ROWS, 2);
        setActivityIntent(intent);
        mActivity = getActivity();
        mGridView = mActivity.mGridView;
        mOrientation = mGridView instanceof HorizontalGridView ? BaseGridView.HORIZONTAL
                : BaseGridView.VERTICAL;

        sendRepeatedKeys(100, KeyEvent.KEYCODE_DPAD_RIGHT);
        waitForScrollIdle(mVerifyLayout);

        sendRepeatedKeys(100, KeyEvent.KEYCODE_DPAD_LEFT);
        waitForScrollIdle(mVerifyLayout);

    }

}
