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
package androidx.leanback.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Parcelable;
import android.text.Selection;
import android.text.Spannable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.TextView;

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.leanback.test.R;
import androidx.leanback.testutils.PollingCheck;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerViewAccessibilityDelegate;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.FlakyTest;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.testutils.AnimationActivityTestRule;
import androidx.testutils.AnimationTest;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class GridWidgetTest {

    private static final String TAG = "GridWidgetTest";
    private static final float DELTA = 1f;
    private static final boolean HUMAN_DELAY = false;
    private static final long WAIT_FOR_SCROLL_IDLE_TIMEOUT_MS = 60000;
    private static final int WAIT_FOR_LAYOUT_PASS_TIMEOUT_MS = 2000;
    private static final int WAIT_FOR_ITEM_ANIMATION_FINISH_TIMEOUT_MS = 6000;

    @Rule
    public final AnimationActivityTestRule<GridActivity> mActivityTestRule =
            new AnimationActivityTestRule<GridActivity>(GridActivity.class, false, false);;

    protected GridActivity mActivity;
    protected BaseGridView mGridView;
    protected GridLayoutManager mLayoutManager;
    private BaseGridView.OnLayoutCompletedListener mWaitLayoutListener;
    protected int mOrientation;
    protected int mNumRows;
    protected int[] mRemovedItems;

    private final Comparator<View> mRowSortComparator = new Comparator<View>() {
        @Override
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

    @Rule public TestName testName = new TestName();

    public static void sendKey(int keyCode) {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(keyCode);
    }

    public static void sendRepeatedKeys(int repeats, int keyCode) {
        for (int i = 0; i < repeats; i++) {
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(keyCode);
        }
    }

    private void humanDelay(int delay) throws InterruptedException {
        if (HUMAN_DELAY) Thread.sleep(delay);
    }
    /**
     * Change size of the Adapter and notifyDataSetChanged.
     */
    private void changeArraySize(final int size) throws Throwable {
        performAndWaitForAnimation(new Runnable() {
            @Override
            public void run() {
                mActivity.changeArraySize(size);
            }
        });
    }

    static String dumpGridView(BaseGridView gridView) {
        return "findFocus:" + gridView.getRootView().findFocus()
                + " isLayoutRequested:" + gridView.isLayoutRequested()
                + " selectedPosition:" + gridView.getSelectedPosition()
                + " adapter.itemCount:" + gridView.getAdapter().getItemCount()
                + " itemAnimator.isRunning:" + gridView.getItemAnimator().isRunning()
                + " scrollState:" + gridView.getScrollState();
    }

    /**
     * Change selected position.
     */
    private void setSelectedPosition(final int position, final int scrollExtra) throws Throwable {
        startWaitLayout();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setSelectedPosition(position, scrollExtra);
            }
        });
        waitForLayout(false);
    }

    private void setSelectedPosition(final int position) throws Throwable {
        setSelectedPosition(position, 0);
    }

    private void setSelectedPositionSmooth(final int position) throws Throwable {
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setSelectedPositionSmooth(position);
            }
        });
    }

    /**
     * Scrolls using given key.
     */
    protected void scroll(int key, Runnable verify) throws Throwable {
        // Keep pressing keys until the GridView will no longer scroll.
        int retryCount = 0;
        do {
            if (verify != null) {
                mActivityTestRule.runOnUiThread(verify);
            }
            sendRepeatedKeys(10, key);
            try {
                Thread.sleep(300);
            } catch (InterruptedException ex) {
                break;
            }
            if (mGridView.getLayoutManager().isSmoothScrolling()
                    || mGridView.getScrollState() != BaseGridView.SCROLL_STATE_IDLE) {
                retryCount = 0;
                continue;
            }
            // It's possible that scroll stops after pressing a key, retry resume scrolling
            retryCount++;
        } while (retryCount < 4);
    }

    protected void scrollToBegin(Runnable verify) throws Throwable {
        int key;
        // first move to first column/row
        if (mOrientation == BaseGridView.HORIZONTAL) {
            key = KeyEvent.KEYCODE_DPAD_UP;
        } else {
            if (mGridView.getLayoutDirection() == ViewGroup.LAYOUT_DIRECTION_RTL) {
                key = KeyEvent.KEYCODE_DPAD_RIGHT;
            } else {
                key = KeyEvent.KEYCODE_DPAD_LEFT;
            }
        }
        scroll(key, null);
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
        // first move to first column/row
        if (mOrientation == BaseGridView.HORIZONTAL) {
            key = KeyEvent.KEYCODE_DPAD_UP;
        } else {
            if (mGridView.getLayoutDirection() == ViewGroup.LAYOUT_DIRECTION_RTL) {
                key = KeyEvent.KEYCODE_DPAD_RIGHT;
            } else {
                key = KeyEvent.KEYCODE_DPAD_LEFT;
            }
        }
        scroll(key, null);
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
        ArrayList<Integer> rowLocations = new ArrayList<>();
        for (int i = 0; i < mGridView.getChildCount(); i++) {
            View v = mGridView.getChildAt(i);
            int rowLocation;
            if (mOrientation == BaseGridView.HORIZONTAL) {
                rowLocation = v.getTop();
            } else {
                rowLocation = mGridView.getLayoutDirection() == ViewGroup.LAYOUT_DIRECTION_RTL
                        ? v.getRight() : v.getLeft();
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
                    assertEquals(mGridView.getHorizontalMargin(),
                            views[i].getLeft() - views[i - 1].getRight());
                } else {
                    assertEquals(mGridView.getVerticalMargin(),
                            views[i].getTop() - views[i - 1].getBottom());
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

    private static int getCenterY(View v) {
        return (v.getTop() + v.getBottom())/2;
    }

    private static int getCenterX(View v) {
        return (v.getLeft() + v.getRight())/2;
    }

    private void initActivity(Intent intent) throws Throwable {
        mActivity = mActivityTestRule.launchActivity(intent);
        mActivityTestRule.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mActivity.setTitle(testName.getMethodName());
                }
            });
        Thread.sleep(1000);
        mGridView = mActivity.mGridView;
        mLayoutManager = (GridLayoutManager) mGridView.getLayoutManager();
    }

    @After
    public void clearTest() {
        mWaitLayoutListener = null;
        mLayoutManager = null;
        mGridView = null;
        mActivity = null;
    }

    /**
     * Must be called before waitForLayout() to prepare layout listener.
     */
    protected void startWaitLayout() {
        if (mWaitLayoutListener != null) {
            throw new IllegalStateException("startWaitLayout() already called");
        }
        if (mLayoutManager.mOnLayoutCompletedListeners != null
                && !mLayoutManager.mOnLayoutCompletedListeners.isEmpty()) {
            throw new IllegalStateException("Cannot startWaitLayout()");
        }
        mWaitLayoutListener = mock(BaseGridView.OnLayoutCompletedListener.class);
        mLayoutManager.addOnLayoutCompletedListener(mWaitLayoutListener);
    }

    /**
     * wait layout to be called and remove the listener.
     */
    protected void waitForLayout() {
        waitForLayout(true);
    }

    /**
     * wait layout to be called and remove the listener.
     * @param force True if always wait regardless if layout requested
     */
    protected void waitForLayout(boolean force) {
        if (mWaitLayoutListener == null) {
            throw new IllegalStateException("startWaitLayout() not called");
        }
        if (!mLayoutManager.mOnLayoutCompletedListeners.contains(mWaitLayoutListener)) {
            throw new IllegalStateException("layout listener inconistent");
        }
        try {
            if (force || mGridView.isLayoutRequested()) {
                verify(mWaitLayoutListener, timeout(WAIT_FOR_LAYOUT_PASS_TIMEOUT_MS).atLeastOnce())
                        .onLayoutCompleted(any(RecyclerView.State.class));
            }
        } finally {
            mLayoutManager.removeOnLayoutCompletedListener(mWaitLayoutListener);
            mWaitLayoutListener = null;
        }
    }

    /**
     * If currently running animator, wait for it to finish, otherwise return immediately.
     * To wait the ItemAnimator start, you can use waitForLayout() to make sure layout pass has
     * processed adapter change.
     */
    protected void waitForItemAnimation(int timeoutMs) throws Throwable {
        final RecyclerView.ItemAnimator.ItemAnimatorFinishedListener listener = mock(
                RecyclerView.ItemAnimator.ItemAnimatorFinishedListener.class);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.getItemAnimator().isRunning(listener);
            }
        });
        verify(listener, timeout(timeoutMs).atLeastOnce()).onAnimationsFinished();
    }

    protected void waitForItemAnimation() throws Throwable {
        waitForItemAnimation(WAIT_FOR_ITEM_ANIMATION_FINISH_TIMEOUT_MS);
    }

    /**
     * wait animation start
     */
    protected void waitForItemAnimationStart() throws Throwable {
        long totalWait = 0;
        while (!mGridView.getItemAnimator().isRunning()) {
            Thread.sleep(10);
            if ((totalWait += 10) > WAIT_FOR_ITEM_ANIMATION_FINISH_TIMEOUT_MS) {
                throw new RuntimeException("waitForItemAnimationStart Timeout");
            }
        }
    }

    /**
     * Run task in UI thread and wait for layout and ItemAnimator finishes.
     */
    protected void performAndWaitForAnimation(Runnable task) throws Throwable {
        startWaitLayout();
        mActivityTestRule.runOnUiThread(task);
        waitForLayout();
        waitForItemAnimation();
    }

    protected void waitForScrollIdle() throws Throwable {
        waitForScrollIdle(null);
    }

    /**
     * Wait for grid view stop scroll and optionally verify state of grid view.
     */
    protected void waitForScrollIdle(Runnable verify) throws Throwable {
        Thread.sleep(100);
        int total = 0;
        while (mGridView.getLayoutManager().isSmoothScrolling()
                || mGridView.getScrollState() != BaseGridView.SCROLL_STATE_IDLE) {
            if ((total += 100) >= WAIT_FOR_SCROLL_IDLE_TIMEOUT_MS) {
                throw new RuntimeException("waitForScrollIdle Timeout");
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                break;
            }
            if (verify != null) {
                mActivityTestRule.runOnUiThread(verify);
            }
        }
    }

    @Test
    public void testThreeRowHorizontalBasic() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.horizontal_grid);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 100);
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 3;

        scrollToEnd(mVerifyLayout);

        scrollToBegin(mVerifyLayout);

        verifyBeginAligned();
    }

    static class DividerDecoration extends RecyclerView.ItemDecoration {

        private ColorDrawable mTopDivider;
        private ColorDrawable mBottomDivider;
        private int mLeftOffset;
        private int mRightOffset;
        private int mTopOffset;
        private int mBottomOffset;

        DividerDecoration(int leftOffset, int topOffset, int rightOffset, int bottomOffset) {
            mLeftOffset = leftOffset;
            mTopOffset = topOffset;
            mRightOffset = rightOffset;
            mBottomOffset = bottomOffset;
        }

        @Override
        public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
            if (mTopDivider == null) {
                mTopDivider = new ColorDrawable(Color.RED);
            }
            if (mBottomDivider == null) {
                mBottomDivider = new ColorDrawable(Color.BLUE);
            }
            final int childCount = parent.getChildCount();
            final int width = parent.getWidth();
            for (int childViewIndex = 0; childViewIndex < childCount; childViewIndex++) {
                final View view = parent.getChildAt(childViewIndex);
                mTopDivider.setBounds(0, (int) view.getY() - mTopOffset, width, (int) view.getY());
                mTopDivider.draw(c);
                mBottomDivider.setBounds(0, (int) view.getY() + view.getHeight(), width,
                        (int) view.getY() + view.getHeight() + mBottomOffset);
                mBottomDivider.draw(c);
            }
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                                   RecyclerView.State state) {
            outRect.left = mLeftOffset;
            outRect.top = mTopOffset;
            outRect.right = mRightOffset;
            outRect.bottom = mBottomOffset;
        }
    }

    @Test
    public void testItemDecorationAndMargins() throws Throwable {

        final int leftMargin = 3;
        final int topMargin = 4;
        final int rightMargin = 7;
        final int bottomMargin = 8;
        final int itemHeight = 100;

        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.vertical_linear);
        intent.putExtra(GridActivity.EXTRA_ITEMS, new int[]{itemHeight, itemHeight, itemHeight});
        intent.putExtra(GridActivity.EXTRA_LAYOUT_MARGINS,
                new int[]{leftMargin, topMargin, rightMargin, bottomMargin});
        initActivity(intent);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        final int paddingLeft = mGridView.getPaddingLeft();
        final int paddingTop = mGridView.getPaddingTop();
        final int verticalSpace = mGridView.getVerticalMargin();
        final int decorationLeft = 17;
        final int decorationTop = 1;
        final int decorationRight = 19;
        final int decorationBottom = 2;

        performAndWaitForAnimation(new Runnable() {
            @Override
            public void run() {
                mGridView.addItemDecoration(new DividerDecoration(decorationLeft, decorationTop,
                        decorationRight, decorationBottom));
            }
        });

        View child0 = mGridView.getChildAt(0);
        View child1 = mGridView.getChildAt(1);
        View child2 = mGridView.getChildAt(2);

        assertEquals(itemHeight, child0.getBottom() - child0.getTop());

        // verify left margins
        assertEquals(paddingLeft + leftMargin + decorationLeft, child0.getLeft());
        assertEquals(paddingLeft + leftMargin + decorationLeft, child1.getLeft());
        assertEquals(paddingLeft + leftMargin + decorationLeft, child2.getLeft());
        // verify top bottom margins and decoration offset
        assertEquals(paddingTop + topMargin + decorationTop, child0.getTop());
        assertEquals(bottomMargin + decorationBottom + verticalSpace + decorationTop + topMargin,
                child1.getTop() - child0.getBottom());
        assertEquals(bottomMargin + decorationBottom + verticalSpace + decorationTop + topMargin,
                child2.getTop() - child1.getBottom());

    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    @Test
    public void testItemDecorationAndMarginsAndOpticalBounds() throws Throwable {
        final int leftMargin = 3;
        final int topMargin = 4;
        final int rightMargin = 7;
        final int bottomMargin = 8;
        final int itemHeight = 100;
        final int ninePatchDrawableResourceId = R.drawable.lb_card_shadow_focused;

        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.vertical_linear);
        intent.putExtra(GridActivity.EXTRA_ITEMS, new int[]{itemHeight, itemHeight, itemHeight});
        intent.putExtra(GridActivity.EXTRA_CHILD_LAYOUT_ID, R.layout.relative_layout);
        intent.putExtra(GridActivity.EXTRA_LAYOUT_MARGINS,
                new int[]{leftMargin, topMargin, rightMargin, bottomMargin});
        intent.putExtra(GridActivity.EXTRA_NINEPATCH_SHADOW, ninePatchDrawableResourceId);
        initActivity(intent);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        final int paddingLeft = mGridView.getPaddingLeft();
        final int paddingTop = mGridView.getPaddingTop();
        final int verticalSpace = mGridView.getVerticalMargin();
        final int decorationLeft = 17;
        final int decorationTop = 1;
        final int decorationRight = 19;
        final int decorationBottom = 2;

        final Rect opticalPaddings = new Rect();
        mGridView.getResources().getDrawable(ninePatchDrawableResourceId)
                .getPadding(opticalPaddings);
        final int opticalInsetsLeft = opticalPaddings.left;
        final int opticalInsetsTop = opticalPaddings.top;
        final int opticalInsetsRight = opticalPaddings.right;
        final int opticalInsetsBottom = opticalPaddings.bottom;
        assertTrue(opticalInsetsLeft > 0);
        assertTrue(opticalInsetsTop > 0);
        assertTrue(opticalInsetsRight > 0);
        assertTrue(opticalInsetsBottom > 0);

        performAndWaitForAnimation(new Runnable() {
            @Override
            public void run() {
                mGridView.addItemDecoration(new DividerDecoration(decorationLeft, decorationTop,
                        decorationRight, decorationBottom));
            }
        });

        View child0 = mGridView.getChildAt(0);
        View child1 = mGridView.getChildAt(1);
        View child2 = mGridView.getChildAt(2);

        assertEquals(itemHeight + opticalInsetsTop + opticalInsetsBottom,
                child0.getBottom() - child0.getTop());

        // verify left margins decoration and optical insets
        assertEquals(paddingLeft + leftMargin + decorationLeft - opticalInsetsLeft,
                child0.getLeft());
        assertEquals(paddingLeft + leftMargin + decorationLeft - opticalInsetsLeft,
                child1.getLeft());
        assertEquals(paddingLeft + leftMargin + decorationLeft - opticalInsetsLeft,
                child2.getLeft());
        // verify top bottom margins decoration offset and optical insets
        assertEquals(paddingTop + topMargin + decorationTop, child0.getTop() + opticalInsetsTop);
        assertEquals(bottomMargin + decorationBottom + verticalSpace + decorationTop + topMargin,
                (child1.getTop() + opticalInsetsTop) - (child0.getBottom() - opticalInsetsBottom));
        assertEquals(bottomMargin + decorationBottom + verticalSpace + decorationTop + topMargin,
                (child2.getTop() + opticalInsetsTop) - (child1.getBottom() - opticalInsetsBottom));

    }

    @Test(expected = ClassCastException.class)
    public void testSetInvalidLayoutManager() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.vertical_grid);
        initActivity(intent);

        // BaseGridView only accepts Leanback's GridLayoutManager for its layout manager.
        RecyclerView.LayoutManager baseLayoutManager =
                Mockito.mock(RecyclerView.LayoutManager.class);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.mGridView.setLayoutManager(baseLayoutManager);
            }
        });
    }

    @Test
    public void testSwitchLayoutManagerVertical() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.vertical_grid);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 50);
        initActivity(intent);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 3;

        scrollToEnd(mVerifyLayout);

        GridLayoutManager newGridLayoutManager = new GridLayoutManager();
        newGridLayoutManager.setOrientation(RecyclerView.VERTICAL);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setLayoutManager(newGridLayoutManager);
            }
        });
        mNumRows = 1;
        waitOneUiCycle();

        // Resetting the layout manager should bring focus back to the first element.
        verifyBeginAligned();
        scrollToEnd(mVerifyLayout);
        scrollToBegin(mVerifyLayout);
        verifyBeginAligned();
    }

    @Test
    public void testSetFocusOutLayoutManagerVertical() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_linear_with_button);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 1);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        initActivity(intent);

        final View endView = new View(mGridView.getContext());

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ViewGroup.LayoutParams lp = mGridView.getLayoutParams();
                lp.height = 300;
                mGridView.setLayoutParams(lp);

                endView.setFocusable(true);
                endView.setLayoutParams(new ViewGroup.LayoutParams(100, 100));
                ((ViewGroup) mGridView.getParent()).addView(endView);
            }
        });

        waitOneUiCycle();

        GridLayoutManager gridLayoutManager = (GridLayoutManager) mGridView.getLayoutManager();
        gridLayoutManager.setFocusOutAllowed(false, true);

        sendKey(KeyEvent.KEYCODE_DPAD_DOWN);
        sendKey(KeyEvent.KEYCODE_DPAD_UP);

        // Focus should be in the grid view because focus is not allowed out the front.
        assertTrue(mGridView.hasFocus());

        sendKey(KeyEvent.KEYCODE_DPAD_DOWN);

        // Focus should be on endView because we allowed focus out the back.
        assertTrue(endView.isFocused());

        gridLayoutManager.setFocusOutAllowed(true, false);

        sendKey(KeyEvent.KEYCODE_DPAD_UP);
        sendKey(KeyEvent.KEYCODE_DPAD_DOWN);

        // Focus should be in the grid view because focus is not allowed out the back.
        assertTrue(mGridView.hasFocus());

        sendKey(KeyEvent.KEYCODE_DPAD_UP);

        final View button = mActivity.findViewById(R.id.button);

        // Button should be in focus because we allow focus out of the front.
        assertTrue(button.isFocused());
    }

    @Test
    public void testSetFocusOutLayoutManagerHorizontal() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_linear_front_back_buttons);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 1);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        initActivity(intent);

        GridLayoutManager gridLayoutManager = (GridLayoutManager) mGridView.getLayoutManager();
        gridLayoutManager.setFocusOutAllowed(false, true);

        final View frontButton = mActivity.findViewById(R.id.button_front);
        final View backButton = mActivity.findViewById(R.id.button_back);

        assertTrue(frontButton.isFocused());

        sendKey(KeyEvent.KEYCODE_DPAD_RIGHT);
        sendKey(KeyEvent.KEYCODE_DPAD_RIGHT);

        // Focus should be on the back button as we allow focus out the back.
        assertTrue(backButton.isFocused());

        sendKey(KeyEvent.KEYCODE_DPAD_LEFT);
        sendKey(KeyEvent.KEYCODE_DPAD_LEFT);

        // Focus should be on the grid as we don't allow focus out the front.
        assertTrue(mGridView.hasFocus());

        gridLayoutManager.setFocusOutAllowed(true, false);

        sendKey(KeyEvent.KEYCODE_DPAD_LEFT);

        // Focus should be on the front button as we now allow focus out the front.
        assertTrue(frontButton.hasFocus());

        sendKey(KeyEvent.KEYCODE_DPAD_RIGHT);
        sendKey(KeyEvent.KEYCODE_DPAD_RIGHT);

        // Focus should be in the grid view as we no longer allow focus out the back.
        assertTrue(mGridView.hasFocus());
    }

    @Test
    public void testSwitchLayoutManagerHorizontal() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.horizontal_grid);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 50);
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 3;

        scrollToEnd(mVerifyLayout);

        GridLayoutManager newGridLayoutManager = new GridLayoutManager();
        newGridLayoutManager.setOrientation(RecyclerView.HORIZONTAL);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setLayoutManager(newGridLayoutManager);
                ((HorizontalGridView) mGridView).setNumRows(4);
            }
        });
        mNumRows = 4;
        waitOneUiCycle();

        verifyBeginAligned();
        scrollToEnd(mVerifyLayout);
        scrollToBegin(mVerifyLayout);
        verifyBeginAligned();
    }

    @Test
    public void testRestoreLayoutManager() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.vertical_grid);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 200);
        initActivity(intent);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 3;

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.scrollToPosition(29);
            }
        });

        GridLayoutManager layout = (GridLayoutManager) mGridView.getLayoutManager();

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.mGridView.setLayoutManager(null);
            }
        });

        waitOneUiCycle();

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.mGridView.setLayoutManager(layout);
            }
        });

        waitOneUiCycle();

        assertEquals(29, mGridView.getSelectedPosition());
    }

    @Ignore // b/266757643
    @Test
    public void testThreeColumnVerticalBasic() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.vertical_grid);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 200);
        initActivity(intent);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 3;

        scrollToEnd(mVerifyLayout);

        scrollToBegin(mVerifyLayout);

        verifyBeginAligned();
    }

    @Ignore // b/269352002
    @Test
    public void testRedundantAppendRemove() throws Throwable {
        Intent intent = new Intent();
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
        initActivity(intent);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 3;

        scrollToEnd(mVerifyLayout);

        scrollToBegin(mVerifyLayout);

        verifyBeginAligned();
    }

    @Test
    public void testRedundantAppendRemove2() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_grid_testredundantappendremove2);
        intent.putExtra(GridActivity.EXTRA_ITEMS, new int[]{
                318,333,199,224,246,273,269,289,340,313,265,306,349,269,185,282,257,354,316,252,
                237,290,283,343,196,313,290,343,191,262,342,228,343,349,251,203,226,305,265,213,
                216,333,295,188,187,281,288,311,244,232,224,332,290,181,267,276,226,261,335,355,
                225,217,219,183,234,285,257,304,182,250,244,223,257,219,342,185,347,205,302,315,
                299,309,292,237,192,309,228,250,347,227,337,298,299,185,185,331,223,284,265,351});
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 3;
        mLayoutManager = (GridLayoutManager) mGridView.getLayoutManager();

        // test append without staggered result cache
        scrollToEnd(mVerifyLayout);

        int[] endEdges = getEndEdges();

        scrollToBegin(mVerifyLayout);

        verifyBeginAligned();

        // now test append with staggered result cache
        changeArraySize(3);
        assertEquals("Staggerd cache should be kept as is when no item size change",
                100, ((StaggeredGrid) mLayoutManager.mGrid).mLocations.size());

        changeArraySize(100);

        scrollToEnd(mVerifyLayout);

        // we should get same aligned end edges
        int[] endEdges2 = getEndEdges();
        verifyEdgesSame(endEdges, endEdges2);
    }


    @Test
    public void testLayoutWhenAViewIsInvalidated() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.vertical_linear);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 1000);
        intent.putExtra(GridActivity.EXTRA_HAS_STABLE_IDS, true);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mNumRows = 1;
        initActivity(intent);
        mOrientation = BaseGridView.VERTICAL;
        waitOneUiCycle();

        // push views to cache.
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.mItemLengths[0] = mActivity.mItemLengths[0] * 3;
                mActivity.mGridView.getAdapter().notifyItemChanged(0);
            }
        });
        waitForItemAnimation();

        // notifyDataSetChange will mark the cached views FLAG_INVALID
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.mGridView.getAdapter().notifyDataSetChanged();
            }
        });
        waitForItemAnimation();

        // Cached views will be added in prelayout with FLAG_INVALID, in post layout we should
        // handle it properly
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.mItemLengths[0] = mActivity.mItemLengths[0] / 3;
                mActivity.mGridView.getAdapter().notifyItemChanged(0);
            }
        });

        waitForItemAnimation();
    }

    @Test
    @AnimationTest
    public void testWrongInsertViewIndexInFastRelayout() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.vertical_linear);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 2);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mNumRows = 1;
        initActivity(intent);
        mOrientation = BaseGridView.VERTICAL;

        // removing two children, they will be hidden views as first 2 children of RV.
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.getItemAnimator().setRemoveDuration(2000);
                mActivity.removeItems(0, 2);
            }
        });
        waitForItemAnimationStart();

        // add three views and notify change of the first item.
        startWaitLayout();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.addItems(0, new int[]{161, 161, 161});
            }
        });
        waitForLayout();
        startWaitLayout();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.getAdapter().notifyItemChanged(0);
            }
        });
        waitForLayout();
        // after layout, the viewholder should still be the first child of LayoutManager.
        assertEquals(0, mGridView.getChildAdapterPosition(
                mGridView.getLayoutManager().getChildAt(0)));
    }

    @Test
    public void testMoveIntoPrelayoutItems() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.vertical_linear);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 1000);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mNumRows = 1;
        initActivity(intent);
        mOrientation = BaseGridView.VERTICAL;

        final int lastItemPos = mGridView.getChildCount() - 1;
        assertTrue(mGridView.getChildCount() >= 4);
        // notify change of 3 items, so prelayout will layout extra 3 items, then move an item
        // into the extra layout range. Post layout's fastRelayout() should handle this properly.
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.getAdapter().notifyItemChanged(lastItemPos - 3);
                mGridView.getAdapter().notifyItemChanged(lastItemPos - 2);
                mGridView.getAdapter().notifyItemChanged(lastItemPos - 1);
                mActivity.moveItem(900, lastItemPos + 2, true);
            }
        });
        waitForItemAnimation();
    }

    @Test
    @AnimationTest
    public void testMoveIntoPrelayoutItems2() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.vertical_linear);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 1000);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mNumRows = 1;
        initActivity(intent);
        mOrientation = BaseGridView.VERTICAL;

        setSelectedPosition(999);
        final int firstItemPos = mGridView.getChildAdapterPosition(mGridView.getChildAt(0));
        assertTrue(mGridView.getChildCount() >= 4);
        // notify change of 3 items, so prelayout will layout extra 3 items, then move an item
        // into the extra layout range. Post layout's fastRelayout() should handle this properly.
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.getAdapter().notifyItemChanged(firstItemPos + 1);
                mGridView.getAdapter().notifyItemChanged(firstItemPos + 2);
                mGridView.getAdapter().notifyItemChanged(firstItemPos + 3);
                mActivity.moveItem(0, firstItemPos - 2, true);
            }
        });
        waitForItemAnimation();
    }

    void preparePredictiveLayout() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.horizontal_linear);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 100);
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.getItemAnimator().setAddDuration(1000);
                mGridView.getItemAnimator().setRemoveDuration(1000);
                mGridView.getItemAnimator().setMoveDuration(1000);
                mGridView.getItemAnimator().setChangeDuration(1000);
                mGridView.setSelectedPositionSmooth(50);
            }
        });
        waitForScrollIdle(mVerifyLayout);
    }

    @Test
    @AnimationTest
    public void testPredictiveLayoutAdd1() throws Throwable {
        preparePredictiveLayout();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.addItems(51, new int[]{300, 300, 300, 300});
            }
        });
        waitForItemAnimationStart();
        waitForItemAnimation();
        assertEquals(50, mGridView.getSelectedPosition());
        assertEquals(RecyclerView.SCROLL_STATE_IDLE, mGridView.getScrollState());
    }

    @Test
    @AnimationTest
    public void testPredictiveLayoutAdd2() throws Throwable {
        preparePredictiveLayout();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.addItems(50, new int[]{300, 300, 300, 300});
            }
        });
        waitForItemAnimationStart();
        waitForItemAnimation();
        assertEquals(54, mGridView.getSelectedPosition());
        assertEquals(RecyclerView.SCROLL_STATE_IDLE, mGridView.getScrollState());
    }

    @Test
    @AnimationTest
    public void testPredictiveLayoutRemove1() throws Throwable {
        preparePredictiveLayout();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.removeItems(51, 3);
            }
        });
        waitForItemAnimationStart();
        waitForItemAnimation();
        assertEquals(50, mGridView.getSelectedPosition());
        assertEquals(RecyclerView.SCROLL_STATE_IDLE, mGridView.getScrollState());
    }

    @Test
    @AnimationTest
    public void testPredictiveLayoutRemove2() throws Throwable {
        preparePredictiveLayout();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.removeItems(47, 3);
            }
        });
        waitForItemAnimationStart();
        waitForItemAnimation();
        assertEquals(47, mGridView.getSelectedPosition());
        assertEquals(RecyclerView.SCROLL_STATE_IDLE, mGridView.getScrollState());
    }

    @Test
    @AnimationTest
    public void testPredictiveLayoutRemove3() throws Throwable {
        preparePredictiveLayout();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.removeItems(0, 51);
            }
        });
        waitForItemAnimationStart();
        waitForItemAnimation();
        assertEquals(0, mGridView.getSelectedPosition());
        assertEquals(RecyclerView.SCROLL_STATE_IDLE, mGridView.getScrollState());
    }

    @Test
    public void testPredictiveOnMeasureWrapContent() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_linear_wrap_content);
        int count = 50;
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, count);
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;

        waitForScrollIdle(mVerifyLayout);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setHasFixedSize(false);
            }
        });

        for (int i = 0; i < 30; i++) {
            final int oldCount = count;
            final int newCount = i;
            mActivityTestRule.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (oldCount > 0) {
                        mActivity.removeItems(0, oldCount);
                    }
                    if (newCount > 0) {
                        int[] newItems = new int[newCount];
                        for (int i = 0; i < newCount; i++) {
                            newItems[i] = 400;
                        }
                        mActivity.addItems(0, newItems);
                    }
                }
            });
            waitForItemAnimationStart();
            waitForItemAnimation();
            count = newCount;
        }

    }

    @Test
    public void testPredictiveLayoutRemove4() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_grid);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 200);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 3;

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setSelectedPositionSmooth(50);
            }
        });
        waitForScrollIdle();
        performAndWaitForAnimation(new Runnable() {
            @Override
            public void run() {
                mActivity.removeItems(0, 49);
            }
        });
        assertEquals(1, mGridView.getSelectedPosition());
    }

    @Test
    public void testPredictiveLayoutRemove5() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_grid);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 200);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, true);
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 3;

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setSelectedPositionSmooth(50);
            }
        });
        waitForScrollIdle();
        performAndWaitForAnimation(new Runnable() {
            @Override
            public void run() {
                mActivity.removeItems(50, 40);
            }
        });
        assertEquals(50, mGridView.getSelectedPosition());
        scrollToBegin(mVerifyLayout);
        verifyBeginAligned();
    }

    void waitOneUiCycle() throws Throwable {
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    @Test
    @AnimationTest
    public void testDontPruneMovingItem() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.horizontal_linear);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 2000);
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.getItemAnimator().setMoveDuration(2000);
                mGridView.setSelectedPosition(50);
            }
        });
        waitForScrollIdle();
        final ArrayList<RecyclerView.ViewHolder> moveViewHolders = new ArrayList<>();
        for (int i = 51;; i++) {
            RecyclerView.ViewHolder vh = mGridView.findViewHolderForAdapterPosition(i);
            if (vh == null) {
                break;
            }
            moveViewHolders.add(vh);
        }

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // add a lot of items, so we will push everything to right of 51 out side window
                int[] lots_items = new int[1000];
                for (int i = 0; i < lots_items.length; i++) {
                    lots_items[i] = 300;
                }
                mActivity.addItems(51, lots_items);
            }
        });
        waitOneUiCycle();
        // run a scroll pass, the scroll pass should not remove the animating views even they are
        // outside visible areas.
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.scrollBy(-3, 0);
            }
        });
        waitOneUiCycle();
        for (int i = 0; i < moveViewHolders.size(); i++) {
            assertSame(mGridView, moveViewHolders.get(i).itemView.getParent());
        }
    }

    @Test
    @AnimationTest
    public void testMoveItemToTheRight() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.horizontal_linear);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 2000);
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.getItemAnimator().setAddDuration(2000);
                mGridView.getItemAnimator().setMoveDuration(2000);
                mGridView.setSelectedPosition(50);
            }
        });
        waitForScrollIdle();
        RecyclerView.ViewHolder moveViewHolder = mGridView.findViewHolderForAdapterPosition(51);

        int lastPos = mGridView.getChildAdapterPosition(mGridView.getChildAt(
                mGridView.getChildCount() - 1));
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.moveItem(51, 1000, true);
            }
        });
        final ArrayList<View> moveInViewHolders = new ArrayList<>();
        waitForItemAnimationStart();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < mGridView.getLayoutManager().getChildCount(); i++) {
                    View v = mGridView.getLayoutManager().getChildAt(i);
                    if (mGridView.getChildAdapterPosition(v) >= 51) {
                        moveInViewHolders.add(v);
                    }
                }
            }
        });
        waitOneUiCycle();
        assertTrue("prelayout should layout extra items to slide in",
                moveInViewHolders.size() > lastPos - 51);
        // run a scroll pass, the scroll pass should not remove the animating views even they are
        // outside visible areas.
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.scrollBy(-3, 0);
            }
        });
        waitOneUiCycle();
        for (int i = 0; i < moveInViewHolders.size(); i++) {
            assertSame(mGridView, moveInViewHolders.get(i).getParent());
        }
        assertSame(mGridView, moveViewHolder.itemView.getParent());
        assertFalse(moveViewHolder.isRecyclable());
        waitForItemAnimation();
        assertNull(moveViewHolder.itemView.getParent());
        assertTrue(moveViewHolder.isRecyclable());
    }

    @Test
    @AnimationTest
    public void testMoveItemToTheLeft() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.horizontal_linear);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 2000);
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.getItemAnimator().setAddDuration(2000);
                mGridView.getItemAnimator().setMoveDuration(2000);
                mGridView.setSelectedPosition(1500);
            }
        });
        waitForScrollIdle();
        RecyclerView.ViewHolder moveViewHolder = mGridView.findViewHolderForAdapterPosition(1499);

        int firstPos = mGridView.getChildAdapterPosition(mGridView.getChildAt(0));
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.moveItem(1499, 1, true);
            }
        });
        final ArrayList<View> moveInViewHolders = new ArrayList<>();
        waitForItemAnimationStart();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < mGridView.getLayoutManager().getChildCount(); i++) {
                    View v = mGridView.getLayoutManager().getChildAt(i);
                    if (mGridView.getChildAdapterPosition(v) <= 1499) {
                        moveInViewHolders.add(v);
                    }
                }
            }
        });
        waitOneUiCycle();
        assertTrue("prelayout should layout extra items to slide in ",
                moveInViewHolders.size() > 1499 - firstPos);
        // run a scroll pass, the scroll pass should not remove the animating views even they are
        // outside visible areas.
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.scrollBy(3, 0);
            }
        });
        waitOneUiCycle();
        for (int i = 0; i < moveInViewHolders.size(); i++) {
            assertSame(mGridView, moveInViewHolders.get(i).getParent());
        }
        assertSame(mGridView, moveViewHolder.itemView.getParent());
        assertFalse(moveViewHolder.isRecyclable());
        waitForItemAnimation();
        assertNull(moveViewHolder.itemView.getParent());
        assertTrue(moveViewHolder.isRecyclable());
    }

    @Test
    public void testContinuousSwapForward() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_linear);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 200);
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setSelectedPositionSmooth(150);
            }
        });
        waitForScrollIdle(mVerifyLayout);
        for (int i = 150; i < 199; i++) {
            final int swapIndex = i;
            mActivityTestRule.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mActivity.swap(swapIndex, swapIndex + 1);
                }
            });
            Thread.sleep(10);
        }
        waitForItemAnimation();
        assertEquals(199, mGridView.getSelectedPosition());
        // check if ItemAnimation finishes at aligned positions:
        int leftEdge = mGridView.getLayoutManager().findViewByPosition(199).getLeft();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.requestLayout();
            }
        });
        waitForScrollIdle();
        assertEquals(leftEdge, mGridView.getLayoutManager().findViewByPosition(199).getLeft());
    }

    @FlakyTest(bugId = 249493545)
    @Test
    public void testContinuousSwapBackward() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_linear);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 200);
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setSelectedPositionSmooth(50);
            }
        });
        waitForScrollIdle(mVerifyLayout);
        for (int i = 50; i > 0; i--) {
            final int swapIndex = i;
            mActivityTestRule.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mActivity.swap(swapIndex, swapIndex - 1);
                }
            });
            Thread.sleep(10);
        }
        waitForItemAnimation();
        assertEquals(0, mGridView.getSelectedPosition());
        // check if ItemAnimation finishes at aligned positions:
        int leftEdge = mGridView.getLayoutManager().findViewByPosition(0).getLeft();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.requestLayout();
            }
        });
        waitForScrollIdle();
        assertEquals(leftEdge, mGridView.getLayoutManager().findViewByPosition(0).getLeft());
    }

    void testSetSelectedPosition(final boolean inSmoothScroll, final boolean layoutRequested,
            final boolean viewVisible, final boolean smooth,
            final boolean resultLayoutRequested, final boolean resultSmoothScroller,
            final int resultScrollState) throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.vertical_linear);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 1500);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mNumRows = 1;
        initActivity(intent);
        mOrientation = BaseGridView.VERTICAL;

        if (inSmoothScroll) {
            setSelectedPositionSmooth(500);
        }
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (layoutRequested) {
                    mGridView.requestLayout();
                }
                final int position;
                if (viewVisible) {
                    position = mGridView.getChildAdapterPosition(mGridView.getChildAt(
                            mGridView.getChildCount() - 1));
                } else {
                    position = 1000;
                }
                if (smooth) {
                    mGridView.setSelectedPositionSmooth(position);
                } else {
                    mGridView.setSelectedPosition(position);
                }
                assertEquals("isLayoutRequested", resultLayoutRequested,
                        mGridView.isLayoutRequested());
                assertEquals("isSmoothScrolling", resultSmoothScroller,
                        mGridView.getLayoutManager().isSmoothScrolling());
                if (!resultSmoothScroller) {
                    // getScrollState() only matters when is not running smoothScroller
                    assertEquals("getScrollState", resultScrollState,
                            mGridView.getScrollState());
                }
                assertEquals("isLayoutRequested", resultLayoutRequested,
                        mGridView.isLayoutRequested());
            }
        });
    }

    @Test
    public void testSelectedPosition01() throws Throwable {
        testSetSelectedPosition(false, false, false, false,
                true, false, RecyclerView.SCROLL_STATE_IDLE);
    }

    @Test
    public void testSelectedPosition02() throws Throwable {
        testSetSelectedPosition(false, false, false, true,
                false, true, RecyclerView.SCROLL_STATE_IDLE);
    }

    @Test
    public void testSelectedPosition03() throws Throwable {
        testSetSelectedPosition(false, false, true, false,
                false, false, RecyclerView.SCROLL_STATE_IDLE);
    }

    @Test
    public void testSelectedPosition04() throws Throwable {
        testSetSelectedPosition(false, false, true, true,
                false, false, RecyclerView.SCROLL_STATE_SETTLING);
    }

    @Test
    public void testSelectedPosition05() throws Throwable {
        testSetSelectedPosition(false, true, false, false,
                true, false, RecyclerView.SCROLL_STATE_IDLE);
    }

    @Test
    public void testSelectedPosition06() throws Throwable {
        testSetSelectedPosition(false, true, false, true,
                true, false, RecyclerView.SCROLL_STATE_IDLE);
    }

    @Test
    public void testSelectedPosition07() throws Throwable {
        testSetSelectedPosition(false, true, true, false,
                true, false, RecyclerView.SCROLL_STATE_IDLE);
    }

    @Test
    public void testSelectedPosition08() throws Throwable {
        testSetSelectedPosition(false, true, true, true,
                true, false, RecyclerView.SCROLL_STATE_IDLE);
    }

    @Test
    public void testSelectedPosition09() throws Throwable {
        testSetSelectedPosition(true, false, false, false,
                true, false, RecyclerView.SCROLL_STATE_IDLE);
    }

    @Test
    public void testSelectedPosition10() throws Throwable {
        testSetSelectedPosition(true, false, false, true,
                false, true, RecyclerView.SCROLL_STATE_IDLE);
    }

    @Test
    public void testSelectedPosition11() throws Throwable {
        testSetSelectedPosition(true, false, true, false,
                false, false, RecyclerView.SCROLL_STATE_IDLE);
    }

    @Test
    public void testSelectedPosition12() throws Throwable {
        testSetSelectedPosition(true, false, true, true,
                false, true, RecyclerView.SCROLL_STATE_IDLE);
    }

    @Test
    public void testSelectedPosition13() throws Throwable {
        testSetSelectedPosition(true, true, false, false,
                true, false, RecyclerView.SCROLL_STATE_IDLE);
    }

    @Test
    public void testSelectedPosition14() throws Throwable {
        testSetSelectedPosition(true, true, false, true,
                true, false, RecyclerView.SCROLL_STATE_IDLE);
    }

    @Test
    public void testSelectedPosition15() throws Throwable {
        testSetSelectedPosition(true, true, true, false,
                true, false, RecyclerView.SCROLL_STATE_IDLE);
    }

    @Test
    public void testSelectedPosition16() throws Throwable {
        testSetSelectedPosition(true, true, true, true,
                true, false, RecyclerView.SCROLL_STATE_IDLE);
    }

    @Test
    public void testScrollAndStuck() throws Throwable {
        // see b/67370222 fastRelayout() may be stuck.
        final int numItems = 19;
        final int[] itemsLength = new int[numItems];
        for (int i = 0; i < numItems; i++) {
            itemsLength[i] = 288;
        }
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_linear);
        intent.putExtra(GridActivity.EXTRA_ITEMS, itemsLength);
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;

        // set left right padding to 112, space between items to be 16.
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ViewGroup.LayoutParams lp = mGridView.getLayoutParams();
                lp.width = 1920;
                mGridView.setLayoutParams(lp);
                mGridView.setPadding(112, mGridView.getPaddingTop(), 112,
                        mGridView.getPaddingBottom());
                mGridView.setItemSpacing(16);
            }
        });
        waitOneUiCycle();

        int scrollPos = 0;
        while (true) {
            final View view = mGridView.getChildAt(mGridView.getChildCount() - 1);
            final int pos = mGridView.getChildViewHolder(view).getAbsoluteAdapterPosition();
            if (scrollPos != pos) {
                scrollPos = pos;
                mActivityTestRule.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mGridView.smoothScrollToPosition(pos);
                    }
                });
            }
            // wait until we see 2nd from last:
            if (pos >= 17) {
                if (pos == 17) {
                    // great we can test fastRelayout() bug.
                    Thread.sleep(50);
                    mActivityTestRule.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            view.requestLayout();
                        }
                    });
                }
                break;
            }
            Thread.sleep(16);
        }
        waitForScrollIdle();
    }

    @Test
    @AnimationTest
    public void testSwapAfterScroll() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_linear);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 200);
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.getItemAnimator().setMoveDuration(1000);
                mGridView.setSelectedPositionSmooth(150);
            }
        });
        waitForScrollIdle();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setSelectedPositionSmooth(151);
            }
        });
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // we want to swap and select new target which is at 150 before swap
                mGridView.setSelectedPositionSmooth(150);
                mActivity.swap(150, 151);
            }
        });
        waitForItemAnimation();
        waitForScrollIdle();
        assertEquals(151, mGridView.getSelectedPosition());
        // check if ItemAnimation finishes at aligned positions:
        int leftEdge = mGridView.getLayoutManager().findViewByPosition(151).getLeft();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.requestLayout();
            }
        });
        waitForScrollIdle();
        assertEquals(leftEdge, mGridView.getLayoutManager().findViewByPosition(151).getLeft());
    }

    void testScrollInSmoothScrolling(final boolean smooth, final boolean scrollToInvisible,
            final boolean useRecyclerViewMethod) throws Throwable {
        final int numItems = 100;
        final int[] itemsLength = new int[numItems];
        for (int i = 0; i < numItems; i++) {
            itemsLength[i] = 288;
        }
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_linear);
        intent.putExtra(GridActivity.EXTRA_ITEMS, itemsLength);
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;

        // start a smoothScroller
        final int selectedPosition = 99;
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.smoothScrollToPosition(selectedPosition);
            }
        });
        Thread.sleep(50);
        // while smoothScroller is still running, scroll to a different position
        final int[] existing_position = new int[1];
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                existing_position[0] = mGridView.getChildAdapterPosition(
                        mGridView.getChildAt(mGridView.getChildCount() - 1));
                if (scrollToInvisible) {
                    existing_position[0] = existing_position[0] + 3;
                }
                if (useRecyclerViewMethod) {
                    if (smooth) {
                        mGridView.smoothScrollToPosition(existing_position[0]);
                    } else {
                        mGridView.scrollToPosition(existing_position[0]);
                    }
                } else {
                    if (smooth) {
                        mGridView.setSelectedPositionSmooth(existing_position[0]);
                    } else {
                        mGridView.setSelectedPosition(existing_position[0]);
                    }
                }
            }
        });
        waitForScrollIdle();
        assertEquals(existing_position[0], mGridView.getSelectedPosition());
        assertTrue(mGridView.findViewHolderForAdapterPosition(existing_position[0])
                .itemView.hasFocus());
    }

    @Test
    public void testScrollInSmoothScrolling1() throws Throwable {
        testScrollInSmoothScrolling(false, false, false);
    }

    @Test
    public void testScrollInSmoothScrolling2() throws Throwable {
        testScrollInSmoothScrolling(false, false, true);
    }

    @Test
    public void testScrollInSmoothScrolling3() throws Throwable {
        testScrollInSmoothScrolling(false, true, false);
    }

    @Test
    public void testScrollInSmoothScrolling4() throws Throwable {
        testScrollInSmoothScrolling(false, true, true);
    }

    @Test
    public void testScrollInSmoothScrolling5() throws Throwable {
        testScrollInSmoothScrolling(true, false, false);
    }

    @Test
    public void testScrollInSmoothScrolling6() throws Throwable {
        testScrollInSmoothScrolling(true, false, true);
    }

    @Test
    public void testScrollInSmoothScrolling7() throws Throwable {
        testScrollInSmoothScrolling(true, true, false);
    }

    @Test
    public void testScrollInSmoothScrolling8() throws Throwable {
        testScrollInSmoothScrolling(true, true, true);
    }

    @Test
    public void testSmoothScrollInScrolling() throws Throwable {
        final int numItems = 1000;
        final int[] itemsLength = new int[numItems];
        for (int i = 0; i < numItems; i++) {
            itemsLength[i] = 288;
        }
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_linear);
        intent.putExtra(GridActivity.EXTRA_ITEMS, itemsLength);
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setWindowAlignment(BaseGridView.WINDOW_ALIGN_NO_EDGE);
                mGridView.setWindowAlignmentOffsetPercent(10);
            }
        });
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int position = mGridView.getChildAdapterPosition(
                        mGridView.getChildAt(mGridView.getChildCount() - 1));
                mGridView.setSelectedPositionSmooth(position);
            }
        });
        final List<Integer> scrollStateChanges = new ArrayList<>();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertTrue(mGridView.getScrollState() == BaseGridView.SCROLL_STATE_SETTLING);
                mGridView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                        scrollStateChanges.add(newState);
                    }
                });
                // start a PendingMoveSmoothScroller
                mGridView.mLayoutManager.processPendingMovement(true);
            }
        });
        assertTrue(mGridView.getLayoutManager().isSmoothScrolling());
        assertTrue("PendingMoveSmoothScroller shouldn't change scrollState from "
                        + "SCROLL_STATE_SETTLING", scrollStateChanges.isEmpty());
    }

    @Test
    public void testScrollAfterRequestLayout() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_linear);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 10);
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setHasFixedSize(false);
                mGridView.setWindowAlignment(BaseGridView.WINDOW_ALIGN_NO_EDGE);
                mGridView.setWindowAlignmentOffsetPercent(30);
            }
        });
        waitOneUiCycle();

        final boolean[] scrolled = new boolean[1];
        mGridView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dx != 0)  scrolled[0] = true;
            }
        });
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.requestLayout();
                mGridView.setSelectedPosition(1);
            }
        });
        waitOneUiCycle();
        assertFalse(scrolled[0]);
    }

    @Test
    public void testScrollAfterItemAnimator() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_linear);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 10);
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setHasFixedSize(false);
                mGridView.setWindowAlignment(BaseGridView.WINDOW_ALIGN_NO_EDGE);
                mGridView.setWindowAlignmentOffsetPercent(30);
            }
        });
        waitOneUiCycle();

        final boolean[] scrolled = new boolean[1];
        mGridView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dx != 0)  scrolled[0] = true;
            }
        });
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.changeItem(0, 10);
                mGridView.setSelectedPosition(1);
            }
        });
        waitOneUiCycle();
        assertFalse(scrolled[0]);
    }

    @Ignore // b/268680302
    @Test
    public void testItemMovedHorizontal() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_grid);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 200);
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 3;

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setSelectedPositionSmooth(150);
            }
        });
        waitForScrollIdle(mVerifyLayout);
        performAndWaitForAnimation(new Runnable() {
            @Override
            public void run() {
                mActivity.swap(150, 152);
            }
        });
        mActivityTestRule.runOnUiThread(mVerifyLayout);

        scrollToBegin(mVerifyLayout);

        verifyBeginAligned();
    }

    @Ignore // This is a long running test that can take hours, improper for a presubmit check.
    @Test
    @AnimationTest
    @SuppressWarnings("unchecked")
    public void testRandomChangeAdapterAndScroll() throws Throwable {
        // Randomly run adapter change and scrolling repeatedly, in order to capture a potential
        // crash.
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_linear);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        intent.putExtra(GridActivity.EXTRA_ITEMS, new int[] {});
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;

        ArrayObjectAdapter adapter = new ArrayObjectAdapter(new TestPresenter());
        ItemBridgeAdapter itemBridgeAdapter = new ItemBridgeAdapter();
        itemBridgeAdapter.setAdapter(adapter);
        mActivityTestRule.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        // Setup a fixed size RV to match exactly the number of Views we want.
                        TestPresenter.setupHorizontalGridView(mGridView);
                        mGridView.setAdapter(itemBridgeAdapter);
                    }
                }
        );

        for (int i = 0; i < 10000; i++) {
            final List<TestPresenter.Item>[] itemsHolder = (List<TestPresenter.Item>[]) new List[1];
            itemsHolder[0] = TestPresenter.generateItems(0, 20);
            mActivityTestRule.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "Setup initial items: " + itemsHolder[0].toString());
                            adapter.setItems(itemsHolder[0], TestPresenter.DIFF_CALLBACK);
                        }
                    }
            );

            for (int j = 0; j < 20; j++) {
                // Either change adapter or scrolling
                boolean changeAdapter = TestPresenter.randomBoolean();
                if (changeAdapter) {
                    itemsHolder[0] = TestPresenter.randomChange(itemsHolder[0]);
                    mActivityTestRule.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "Random change items: " + itemsHolder[0].toString());
                            adapter.setItems(itemsHolder[0], TestPresenter.DIFF_CALLBACK);
                        }
                    });
                } else {
                    mActivityTestRule.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (TestPresenter.randomBoolean()) {
                                Log.d(TAG, "Scrolling to first item");
                                mGridView.setSelectedPositionSmooth(0);
                            } else {
                                Log.d(TAG, "Scrolling to last item");
                                mGridView.setSelectedPositionSmooth(itemsHolder[0].size() - 1);
                            }
                        }
                    });
                }
                Thread.sleep(100);
            }
        }
    }

    @Test
    @AnimationTest
    @SuppressWarnings("unchecked")
    public void testCrashOnRVChildHelperBug292114537() throws Throwable {
        // see b/292114537
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_linear);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        intent.putExtra(GridActivity.EXTRA_ITEMS, new int[] {});
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;

        ArrayObjectAdapter adapter = new ArrayObjectAdapter(new TestPresenter());
        ItemBridgeAdapter itemBridgeAdapter = new ItemBridgeAdapter();
        itemBridgeAdapter.setAdapter(adapter);

        mActivityTestRule.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        // Setup a fixed size RV to match exactly the number of Views we want.
                        TestPresenter.setupHorizontalGridView(mGridView);
                        mGridView.setAdapter(itemBridgeAdapter);
                        mGridView.getItemAnimator().setAddDuration(1000);
                        mGridView.getItemAnimator().setMoveDuration(1000);
                        mGridView.getItemAnimator().setRemoveDuration(1000);
                        adapter.setItems(
                                TestPresenter.generateItems(new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8}),
                                TestPresenter.DIFF_CALLBACK);
                    }
                }
        );
        // showing 0 1(selected) 2 3 4 5(peek)
        setSelectedPosition(1);

        // swap 5 and 6, showing 0 1(selected) 2 3 4 6(slide in) 5(slide out)
        mActivityTestRule.runOnUiThread(() ->
                adapter.setItems(TestPresenter.generateItems(new int[]{0, 1, 2, 3, 4, 6, 5, 7, 8}),
                        TestPresenter.DIFF_CALLBACK));

        // Wait a little bit for the ItemAnimation to be started
        waitForItemAnimationStart();

        // Scroll to 0 to remove "6", this can break ChildHelper and scroll to 2 may failed to find
        // the View and crash.
        setSelectedPosition(0);
        setSelectedPosition(2);
    }

    @Test
    public void testItemMovedHorizontalRtl() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_linear_rtl);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        intent.putExtra(GridActivity.EXTRA_ITEMS, new int[] {40, 40, 40});
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;

        performAndWaitForAnimation(new Runnable() {
            @Override
            public void run() {
                mActivity.moveItem(0, 1, true);
            }
        });
        assertEquals(mGridView.getWidth() - mGridView.getPaddingRight(),
                mGridView.findViewHolderForAdapterPosition(0).itemView.getRight());
    }

    @Test
    public void testScrollSecondaryCannotScroll() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_grid);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 2000);
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 3;
        final int topPadding = 2;
        final int bottomPadding = 2;
        final int height = mGridView.getHeight();
        final int spacing = 2;
        final int rowHeight = (height - topPadding - bottomPadding) / 4 - spacing;
        final HorizontalGridView horizontalGridView = (HorizontalGridView) mGridView;

        startWaitLayout();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                horizontalGridView.setPadding(0, topPadding, 0, bottomPadding);
                horizontalGridView.setItemSpacing(spacing);
                horizontalGridView.setNumRows(mNumRows);
                horizontalGridView.setRowHeight(rowHeight);
            }
        });
        waitForLayout();
        // navigate vertically in first column, first row should always be aligned to top padding
        for (int i = 0; i < 3; i++) {
            setSelectedPosition(i);
            assertEquals(topPadding, mGridView.findViewHolderForAdapterPosition(0).itemView
                    .getTop());
        }
        // navigate vertically in 100th column, first row should always be aligned to top padding
        for (int i = 300; i < 301; i++) {
            setSelectedPosition(i);
            assertEquals(topPadding, mGridView.findViewHolderForAdapterPosition(300).itemView
                    .getTop());
        }
    }

    @Test
    public void testScrollSecondaryNeedScroll() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_grid);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 2000);
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        // test a lot of rows so we have to scroll vertically to reach
        mNumRows = 9;
        final int topPadding = 2;
        final int bottomPadding = 2;
        final int height = mGridView.getHeight();
        final int spacing = 2;
        final int rowHeight = (height - topPadding - bottomPadding) / 4 - spacing;
        final HorizontalGridView horizontalGridView = (HorizontalGridView) mGridView;

        startWaitLayout();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                horizontalGridView.setPadding(0, topPadding, 0, bottomPadding);
                horizontalGridView.setItemSpacing(spacing);
                horizontalGridView.setNumRows(mNumRows);
                horizontalGridView.setRowHeight(rowHeight);
            }
        });
        waitForLayout();
        View view;
        // first row should be aligned to top padding
        setSelectedPosition(0);
        assertEquals(topPadding, mGridView.findViewHolderForAdapterPosition(0).itemView.getTop());
        // middle row should be aligned to keyline (1/2 of screen height)
        setSelectedPosition(4);
        view = mGridView.findViewHolderForAdapterPosition(4).itemView;
        assertEquals(height / 2, (view.getTop() + view.getBottom()) / 2);
        // last row should be aligned to bottom padding.
        setSelectedPosition(8);
        view = mGridView.findViewHolderForAdapterPosition(8).itemView;
        assertEquals(height, view.getTop() + rowHeight + bottomPadding);
        setSelectedPositionSmooth(4);
        waitForScrollIdle();
        // middle row should be aligned to keyline (1/2 of screen height)
        setSelectedPosition(4);
        view = mGridView.findViewHolderForAdapterPosition(4).itemView;
        assertEquals(height / 2, (view.getTop() + view.getBottom()) / 2);
        // first row should be aligned to top padding
        setSelectedPositionSmooth(0);
        waitForScrollIdle();
        assertEquals(topPadding, mGridView.findViewHolderForAdapterPosition(0).itemView.getTop());
    }

    @Ignore // b/268680302
    @Test
    public void testItemMovedVertical() throws Throwable {

        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_grid);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 200);
        initActivity(intent);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 3;

        mGridView.setSelectedPositionSmooth(150);
        waitForScrollIdle(mVerifyLayout);
        performAndWaitForAnimation(new Runnable() {
            @Override
            public void run() {
                mActivity.swap(150, 152);
            }
        });
        mActivityTestRule.runOnUiThread(mVerifyLayout);

        scrollToEnd(mVerifyLayout);
        scrollToBegin(mVerifyLayout);

        verifyBeginAligned();
    }

    @Test
    public void testAddLastItemHorizontal() throws Throwable {

        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_linear);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 50);
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;

        mActivityTestRule.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        mGridView.setSelectedPositionSmooth(49);
                    }
                }
        );
        waitForScrollIdle(mVerifyLayout);
        performAndWaitForAnimation(new Runnable() {
            @Override
            public void run() {
                mActivity.addItems(50, new int[]{150});
            }
        });

        // assert new added item aligned to right edge
        assertEquals(mGridView.getWidth() - mGridView.getPaddingRight(),
                mGridView.getLayoutManager().findViewByPosition(50).getRight());
    }

    @Test
    public void testAddMultipleLastItemsHorizontal() throws Throwable {

        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_linear);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 50);
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;

        mActivityTestRule.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        mGridView.setWindowAlignment(BaseGridView.WINDOW_ALIGN_BOTH_EDGE);
                        mGridView.setWindowAlignmentOffsetPercent(50);
                        mGridView.setSelectedPositionSmooth(49);
                    }
                }
        );
        waitForScrollIdle(mVerifyLayout);
        performAndWaitForAnimation(new Runnable() {
            @Override
            public void run() {
                mActivity.addItems(50, new int[]{150, 150, 150, 150, 150, 150, 150, 150, 150,
                        150, 150, 150, 150, 150});
            }
        });

        // The focused item will be at center of window
        View view = mGridView.getLayoutManager().findViewByPosition(49);
        assertEquals(mGridView.getWidth() / 2, (view.getLeft() + view.getRight()) / 2);
    }

    @Ignore // b/266757643
    @Test
    public void testItemAddRemoveHorizontal() throws Throwable {

        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_grid);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 200);
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 3;

        scrollToEnd(mVerifyLayout);
        int[] endEdges = getEndEdges();

        mGridView.setSelectedPositionSmooth(150);
        waitForScrollIdle(mVerifyLayout);
        performAndWaitForAnimation(new Runnable() {
            @Override
            public void run() {
                mRemovedItems = mActivity.removeItems(151, 4);
            }
        });

        scrollToEnd(mVerifyLayout);
        mGridView.setSelectedPositionSmooth(150);
        waitForScrollIdle(mVerifyLayout);

        performAndWaitForAnimation(new Runnable() {
            @Override
            public void run() {
                mActivity.addItems(151, mRemovedItems);
            }
        });
        scrollToEnd(mVerifyLayout);

        // we should get same aligned end edges
        int[] endEdges2 = getEndEdges();
        verifyEdgesSame(endEdges, endEdges2);

        scrollToBegin(mVerifyLayout);
        verifyBeginAligned();
    }

    @Ignore("b/283480313")
    @Test
    public void testSetSelectedPositionDetached() throws Throwable {

        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_linear);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 50);
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;

        final int focusToIndex = 49;
        final ViewGroup parent = (ViewGroup) mGridView.getParent();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                parent.removeView(mGridView);
            }
        });
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setSelectedPositionSmooth(focusToIndex);
            }
        });
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                parent.addView(mGridView);
                mGridView.requestFocus();
            }
        });
        waitForScrollIdle();
        assertEquals(mGridView.getSelectedPosition(), focusToIndex);
        assertTrue(mGridView.getLayoutManager().findViewByPosition(focusToIndex).hasFocus());

        final int focusToIndex2 = 0;
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                parent.removeView(mGridView);
            }
        });
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setSelectedPosition(focusToIndex2);
            }
        });
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                parent.addView(mGridView);
                mGridView.requestFocus();
            }
        });
        assertEquals(mGridView.getSelectedPosition(), focusToIndex2);
        waitForScrollIdle();
        assertTrue(mGridView.getLayoutManager().findViewByPosition(focusToIndex2).hasFocus());
    }

    @Test
    public void testBug22209986() throws Throwable {

        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_linear);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 50);
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;

        final int focusToIndex = mGridView.getChildCount() - 1;
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setSelectedPositionSmooth(focusToIndex);
            }
        });

        waitForScrollIdle();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setSelectedPositionSmooth(focusToIndex + 1);
            }
        });
        // let the scroll running for a while and requestLayout during scroll
        Thread.sleep(80);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertEquals(mGridView.getScrollState(), BaseGridView.SCROLL_STATE_SETTLING);
                mGridView.requestLayout();
            }
        });
        waitForScrollIdle();

        int leftEdge = mGridView.getLayoutManager().findViewByPosition(focusToIndex).getLeft();

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.requestLayout();
            }
        });
        waitForScrollIdle();
        assertEquals(leftEdge,
                mGridView.getLayoutManager().findViewByPosition(focusToIndex).getLeft());
    }

    void testScrollAndRemove(int[] itemsLength, int numItems) throws Throwable {

        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_linear);
        if (itemsLength != null) {
            intent.putExtra(GridActivity.EXTRA_ITEMS, itemsLength);
        } else {
            intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, numItems);
        }
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;

        final int focusToIndex = mGridView.getChildCount() - 1;
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setSelectedPositionSmooth(focusToIndex);
            }
        });

        performAndWaitForAnimation(new Runnable() {
            @Override
            public void run() {
                mActivity.removeItems(focusToIndex, 1);
            }
        });

        waitOneUiCycle();
        waitForScrollIdle();
        int leftEdge = mGridView.getLayoutManager().findViewByPosition(focusToIndex).getLeft();

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.requestLayout();
            }
        });
        waitForScrollIdle();
        assertEquals(leftEdge,
                mGridView.getLayoutManager().findViewByPosition(focusToIndex).getLeft(), DELTA);
    }

    @Test
    public void testScrollAndRemove() throws Throwable {
        // test random lengths for 50 items
        testScrollAndRemove(null, 50);
    }

    /**
     * This test verifies if scroll limits are ignored when onLayoutChildren compensate remaining
     * scroll distance. b/64931938
     * In the test, second child is long, other children are short.
     * Test scrolls to the long child, and when scrolling, remove the long child. We made it long
     * to have enough remaining scroll distance when the layout pass kicks in.
     * The onLayoutChildren() would compensate the remaining scroll distance, moving all items
     * toward right, which will make the first item's left edge bigger than left padding,
     * which would violate the "scroll limit of left" in a regular scroll case, but
     * in layout pass, we still honor that scroll request, ignoring the scroll limit.
     */
    @Test
    public void testScrollAndRemoveSample1() throws Throwable {
        DisplayMetrics dm = InstrumentationRegistry.getInstrumentation().getTargetContext()
                .getResources().getDisplayMetrics();
        // screen width for long item and 4DP for other items
        int longItemLength = dm.widthPixels;
        int shortItemLength = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, dm);
        int[] items = new int[1000];
        for (int i = 0; i < items.length; i++) {
            items[i] = shortItemLength;
        }
        items[1] = longItemLength;
        testScrollAndRemove(items, 0);
    }

    @Test
    public void testScrollAndInsert() throws Throwable {
        testScrollAndInsert(3);
    }

    @Test
    public void testScrollAndInsertAlot() throws Throwable {
        testScrollAndInsert(20);
    }

    public void testScrollAndInsert(int insertedItems) throws Throwable {

        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_grid);
        int[] items = new int[1000];
        for (int i = 0; i < items.length; i++) {
            items[i] = 300 + (int)(Math.random() * 100);
        }
        final int[] newItems = new int[insertedItems];
        for (int i = 0; i < insertedItems; i++) {
            newItems[i] = 300;
        }
        intent.putExtra(GridActivity.EXTRA_ITEMS, items);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, true);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 3;

        initActivity(intent);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setSelectedPositionSmooth(150);
            }
        });
        waitForScrollIdle(mVerifyLayout);

        View view =  mGridView.getChildAt(mGridView.getChildCount() - 1);
        final int focusToIndex = mGridView.getChildAdapterPosition(view);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setSelectedPositionSmooth(focusToIndex);
            }
        });

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.addItems(0, newItems);
            }
        });
        waitForScrollIdle();
        int newFocusToIndex = focusToIndex + insertedItems;
        assertEquals(newFocusToIndex, mGridView.getSelectedPosition());
        int topEdge = mGridView.getLayoutManager().findViewByPosition(newFocusToIndex).getTop();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.requestLayout();
            }
        });
        waitForScrollIdle();
        assertEquals(topEdge,
                mGridView.getLayoutManager().findViewByPosition(newFocusToIndex).getTop());
    }

    @Test
    public void testScrollAndInsertBeforeVisibleItem() throws Throwable {

        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_grid);
        int[] items = new int[1000];
        for (int i = 0; i < items.length; i++) {
            items[i] = 300 + (int)(Math.random() * 100);
        }
        intent.putExtra(GridActivity.EXTRA_ITEMS, items);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, true);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 3;

        initActivity(intent);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setSelectedPositionSmooth(150);
            }
        });
        waitForScrollIdle(mVerifyLayout);

        View view =  mGridView.getChildAt(mGridView.getChildCount() - 1);
        final int focusToIndex = mGridView.getChildAdapterPosition(view);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setSelectedPositionSmooth(focusToIndex);
            }
        });

        performAndWaitForAnimation(new Runnable() {
            @Override
            public void run() {
                int[] newItems = new int[]{300, 300, 300};
                mActivity.addItems(focusToIndex, newItems);
            }
        });
    }

    @Test
    public void testSmoothScrollStuck() throws Throwable {

        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_linear);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 30);

        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setWindowAlignment(BaseGridView.WINDOW_ALIGN_NO_EDGE);
                mGridView.setWindowAlignmentOffset(0);
                mGridView.setWindowAlignmentOffsetPercent(50);
                mGridView.setHasFixedSize(true);
                mGridView.setSelectedPosition(29);
            }
        });
        waitOneUiCycle();
        // adding few items immediately consumes the pending moves
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.addItems(30, new int[]{200, 200});
                // simulate pressing DPAD keys that creates a PendingMoveSmoothScroller
                mGridView.mLayoutManager.processPendingMovement(true);
            }
        });
        waitForScrollIdle();
    }

    @Test
    public void testSmoothScrollAndRemove() throws Throwable {

        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_linear);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 300);
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;

        final int focusToIndex = 200;
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setSelectedPositionSmooth(focusToIndex);
            }
        });

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.removeItems(focusToIndex, 1);
            }
        });

        assertTrue("removing the index of not attached child should not affect smooth scroller",
                mGridView.getLayoutManager().isSmoothScrolling());
        waitForScrollIdle();
        int leftEdge = mGridView.getLayoutManager().findViewByPosition(focusToIndex).getLeft();

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.requestLayout();
            }
        });
        waitForScrollIdle();
        assertEquals(leftEdge,
                mGridView.getLayoutManager().findViewByPosition(focusToIndex).getLeft());
    }

    @Test
    public void testSmoothScrollAndRemove2() throws Throwable {

        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_linear);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 300);
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;

        final int focusToIndex = 200;
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setSelectedPositionSmooth(focusToIndex);
            }
        });

        startWaitLayout();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final int removeIndex = mGridView.getChildViewHolder(
                        mGridView.getChildAt(
                                mGridView.getChildCount() - 1)).getAbsoluteAdapterPosition();
                mActivity.removeItems(removeIndex, 1);
            }
        });
        waitForLayout();

        assertTrue("removing the index of attached child should not kill smooth scroller",
                mGridView.getLayoutManager().isSmoothScrolling());
        waitForItemAnimation();
        waitForScrollIdle();
        int leftEdge = mGridView.getLayoutManager().findViewByPosition(focusToIndex).getLeft();

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.requestLayout();
            }
        });
        waitForScrollIdle();
        assertEquals(leftEdge,
                mGridView.getLayoutManager().findViewByPosition(focusToIndex).getLeft());
    }

    void testRemoveVisibleItemsInSmoothScrollingForward(final boolean focusOnGridView)
            throws Throwable {
        final int numItems = 200;
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_linear_with_button_onleft);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, numItems);
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;
        final View button = mActivity.findViewById(R.id.button);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (focusOnGridView) {
                    mGridView.requestFocus();
                } else {
                    button.requestFocus();
                }
                mGridView.setSelectedPositionSmooth(numItems - 1);
            }
        });
        waitOneUiCycle();
        final int numRemoved = 4;
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int position = mGridView.getChildAdapterPosition(mGridView.getChildAt(0));
                mActivity.removeItems(position, numRemoved);
            }
        });

        waitOneUiCycle();
        waitForScrollIdle();
        assertTrue("Should not keep lots of views", mGridView.getChildCount() < numItems / 2);
        assertEquals(mGridView.getSelectedPosition(), numItems - 1 - numRemoved);
        if (focusOnGridView) {
            assertTrue("GridView should retain focus", mGridView.hasFocus());
            assertFalse("Gridview should pass focus to its child", mGridView.isFocused());
        } else {
            assertTrue("button should has focus", button.hasFocus());
        }
    }

    void testRemoveVisibleItemsInSmoothScrollingBackward(final boolean focusOnGridView)
            throws Throwable {
        final int numItems = 200;
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_linear_with_button_onleft);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, numItems);
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;
        final View button = mActivity.findViewById(R.id.button);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setSelectedPosition(numItems - 1);
            }
        });
        waitOneUiCycle();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (focusOnGridView) {
                    mGridView.requestFocus();
                } else {
                    button.requestFocus();
                }
                mGridView.setSelectedPositionSmooth(0);
            }
        });
        waitOneUiCycle();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int position = mGridView.getChildAdapterPosition(mGridView.getChildAt(0));
                int numRemoved = numItems - 1 - position;
                mActivity.removeItems(position, numRemoved);
            }
        });

        waitOneUiCycle();
        waitForScrollIdle();
        assertTrue("Should not keep lots of views", mGridView.getChildCount() < numItems / 2);
        assertEquals(mGridView.getSelectedPosition(), 0);
        if (focusOnGridView) {
            assertTrue("GridView should retain focus", mGridView.hasFocus());
            assertFalse("Gridview should pass focus to its child", mGridView.isFocused());
        } else {
            assertTrue("button should has focus", button.hasFocus());
        }
    }

    @Test
    public void testRemoveVisibleItemsInSmoothScrollingForwardWithFocus() throws Throwable {
        testRemoveVisibleItemsInSmoothScrollingForward(/*focusOnGridView=*/ true);
    }

    @Test
    public void testRemoveVisibleItemsInSmoothScrollingForwardWithoutFocus() throws Throwable {
        testRemoveVisibleItemsInSmoothScrollingForward(/*focusOnGridView=*/ false);
    }

    @Test
    public void testRemoveVisibleItemsInSmoothScrollingBackwardWithFocus() throws Throwable {
        testRemoveVisibleItemsInSmoothScrollingBackward(/*focusOnGridView=*/ true);
    }

    @Test
    public void testRemoveVisibleItemsInSmoothScrollingBackwardWithoutFocus() throws Throwable {
        testRemoveVisibleItemsInSmoothScrollingBackward(/*focusOnGridView=*/ false);
    }

    @Ignore // b/266757643
    @Test
    public void testPendingSmoothScrollAndRemove() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_linear);
        intent.putExtra(GridActivity.EXTRA_REQUEST_FOCUS_ONLAYOUT, true);
        int[] items = new int[100];
        for (int i = 0; i < items.length; i++) {
            items[i] = 630 + (int)(Math.random() * 100);
        }
        intent.putExtra(GridActivity.EXTRA_ITEMS, items);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, true);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        initActivity(intent);

        mGridView.setSelectedPositionSmooth(0);
        waitForScrollIdle(mVerifyLayout);
        assertTrue(mGridView.getChildAt(0).hasFocus());

        // Pressing lots of key to make sure smooth scroller is running
        mGridView.mLayoutManager.mMaxPendingMoves = 100;
        for (int i = 0; i < 100; i++) {
            sendKey(KeyEvent.KEYCODE_DPAD_DOWN);
        }

        assertTrue(mGridView.getLayoutManager().isSmoothScrolling());
        startWaitLayout();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final int removeIndex = mGridView.getChildViewHolder(
                        mGridView.getChildAt(
                                mGridView.getChildCount() - 1)).getAbsoluteAdapterPosition();
                mActivity.removeItems(removeIndex, 1);
            }
        });
        waitForLayout();

        assertTrue("removing the index of attached child should not kill smooth scroller",
                mGridView.getLayoutManager().isSmoothScrolling());

        waitForItemAnimation();
        waitForScrollIdle();
        int focusIndex = mGridView.getSelectedPosition();
        int topEdge = mGridView.getLayoutManager().findViewByPosition(focusIndex).getTop();

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.requestLayout();
            }
        });
        waitForScrollIdle();
        assertEquals(topEdge,
                mGridView.getLayoutManager().findViewByPosition(focusIndex).getTop());
    }

    @Test
    public void testFocusToFirstItem() throws Throwable {

        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_grid);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 200);
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 3;

        performAndWaitForAnimation(new Runnable() {
            @Override
            public void run() {
                mRemovedItems = mActivity.removeItems(0, 200);
            }
        });

        humanDelay(500);
        performAndWaitForAnimation(new Runnable() {
            @Override
            public void run() {
                mActivity.addItems(0, mRemovedItems);
            }
        });

        humanDelay(500);
        assertTrue(mGridView.getLayoutManager().findViewByPosition(0).hasFocus());

        changeArraySize(0);

        changeArraySize(200);
        assertTrue(mGridView.getLayoutManager().findViewByPosition(0).hasFocus());
    }

    @Test
    public void testFocusFirstFocusable() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_grid);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 1);
        intent.putExtra(GridActivity.EXTRA_ITEMS_FOCUSABLE, new boolean[] {false, true});
        initActivity(intent);

        humanDelay(500);
        assertTrue(mGridView.hasFocus());

        performAndWaitForAnimation(new Runnable() {
            @Override
            public void run() {
                mActivity.addItems(1, new int[]{1});
            }
        });
        humanDelay(500);

        assertTrue(mGridView.getLayoutManager().findViewByPosition(1).hasFocus());
    }

    @Test
    public void testNonFocusableHorizontal() throws Throwable {
        final int numItems = 200;
        final int startPos = 45;
        final int skips = 20;
        final int numColumns = 3;
        final int endPos = startPos + numColumns * (skips + 1);

        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_grid);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, numItems);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = numColumns;
        boolean[] focusable = new boolean[numItems];
        for (int i = 0; i < focusable.length; i++) {
            focusable[i] = true;
        }
        for (int i = startPos + mNumRows, j = 0; j < skips; i += mNumRows, j++) {
            focusable[i] = false;
        }
        intent.putExtra(GridActivity.EXTRA_ITEMS_FOCUSABLE, focusable);
        initActivity(intent);

        mGridView.setSelectedPositionSmooth(startPos);
        waitForScrollIdle(mVerifyLayout);

        if (mGridView.getLayoutDirection() == ViewGroup.LAYOUT_DIRECTION_RTL) {
            sendKey(KeyEvent.KEYCODE_DPAD_LEFT);
        } else {
            sendKey(KeyEvent.KEYCODE_DPAD_RIGHT);
        }
        waitForScrollIdle(mVerifyLayout);
        assertEquals(endPos, mGridView.getSelectedPosition());

        if (mGridView.getLayoutDirection() == ViewGroup.LAYOUT_DIRECTION_RTL) {
            sendKey(KeyEvent.KEYCODE_DPAD_RIGHT);
        } else {
            sendKey(KeyEvent.KEYCODE_DPAD_LEFT);
        }
        waitForScrollIdle(mVerifyLayout);
        assertEquals(startPos, mGridView.getSelectedPosition());

    }

    @Test
    public void testNoInitialFocusable() throws Throwable {

        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_linear);
        final int numItems = 100;
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, numItems);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;
        boolean[] focusable = new boolean[numItems];
        final int firstFocusableIndex = 10;
        for (int i = 0; i < firstFocusableIndex; i++) {
            focusable[i] = false;
        }
        for (int i = firstFocusableIndex; i < focusable.length; i++) {
            focusable[i] = true;
        }
        intent.putExtra(GridActivity.EXTRA_ITEMS_FOCUSABLE, focusable);
        initActivity(intent);
        assertTrue(mGridView.isFocused());

        if (mGridView.getLayoutDirection() == ViewGroup.LAYOUT_DIRECTION_RTL) {
            sendKey(KeyEvent.KEYCODE_DPAD_LEFT);
        } else {
            sendKey(KeyEvent.KEYCODE_DPAD_RIGHT);
        }
        waitForScrollIdle(mVerifyLayout);
        assertEquals(firstFocusableIndex, mGridView.getSelectedPosition());
        assertTrue(mGridView.getLayoutManager().findViewByPosition(firstFocusableIndex).hasFocus());
    }

    @Test
    public void testFocusOutOfEmptyListView() throws Throwable {

        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_linear);
        final int numItems = 100;
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, numItems);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;
        initActivity(intent);

        final View horizontalGridView = new HorizontalGridViewEx(mGridView.getContext());
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                horizontalGridView.setFocusable(true);
                horizontalGridView.setFocusableInTouchMode(true);
                horizontalGridView.setLayoutParams(new ViewGroup.LayoutParams(100, 100));
                ((ViewGroup) mGridView.getParent()).addView(horizontalGridView, 0);
                horizontalGridView.requestFocus();
            }
        });

        assertTrue(horizontalGridView.isFocused());

        sendKey(KeyEvent.KEYCODE_DPAD_DOWN);

        assertTrue(mGridView.hasFocus());
    }

    @Test
    public void testTransferFocusToChildWhenGainFocus() throws Throwable {

        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_linear);
        final int numItems = 100;
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, numItems);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;
        boolean[] focusable = new boolean[numItems];
        final int firstFocusableIndex = 1;
        for (int i = 0; i < firstFocusableIndex; i++) {
            focusable[i] = false;
        }
        for (int i = firstFocusableIndex; i < focusable.length; i++) {
            focusable[i] = true;
        }
        intent.putExtra(GridActivity.EXTRA_ITEMS_FOCUSABLE, focusable);
        initActivity(intent);

        assertEquals(firstFocusableIndex, mGridView.getSelectedPosition());
        assertTrue(mGridView.getLayoutManager().findViewByPosition(firstFocusableIndex).hasFocus());
    }

    @Test
    public void testFocusFromSecondChild() throws Throwable {

        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_linear);
        final int numItems = 100;
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, numItems);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;
        boolean[] focusable = new boolean[numItems];
        for (int i = 0; i < focusable.length; i++) {
            focusable[i] = false;
        }
        intent.putExtra(GridActivity.EXTRA_ITEMS_FOCUSABLE, focusable);
        initActivity(intent);

        // switching Adapter to cause a full rebind,  test if it will focus to second item.
        performAndWaitForAnimation(new Runnable() {
            @Override
            public void run() {
                mActivity.mNumItems = numItems;
                mActivity.mItemFocusables[1] = true;
                mActivity.rebindToNewAdapter();
            }
        });
        assertTrue(mGridView.findViewHolderForAdapterPosition(1).itemView.hasFocus());
    }

    @Test
    public void removeFocusableItemAndFocusableRecyclerViewGetsFocus() throws Throwable {
        final int numItems = 100;
        final int numColumns = 3;
        final int focusableIndex = 2;

        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_grid);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, numItems);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = numColumns;
        boolean[] focusable = new boolean[numItems];
        for (int i = 0; i < focusable.length; i++) {
            focusable[i] = false;
        }
        focusable[focusableIndex] = true;
        intent.putExtra(GridActivity.EXTRA_ITEMS_FOCUSABLE, focusable);
        initActivity(intent);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setSelectedPositionSmooth(focusableIndex);
            }
        });
        waitForScrollIdle(mVerifyLayout);
        assertEquals(focusableIndex, mGridView.getSelectedPosition());

        performAndWaitForAnimation(new Runnable() {
            @Override
            public void run() {
                mActivity.removeItems(focusableIndex, 1);
            }
        });
        assertTrue(dumpGridView(mGridView), mGridView.isFocused());
    }

    @Test
    public void removeFocusableItemAndUnFocusableRecyclerViewLosesFocus() throws Throwable {
        final int numItems = 100;
        final int numColumns = 3;
        final int focusableIndex = 2;

        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_grid);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, numItems);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = numColumns;
        boolean[] focusable = new boolean[numItems];
        for (int i = 0; i < focusable.length; i++) {
            focusable[i] = false;
        }
        focusable[focusableIndex] = true;
        intent.putExtra(GridActivity.EXTRA_ITEMS_FOCUSABLE, focusable);
        initActivity(intent);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setFocusableInTouchMode(false);
                mGridView.setFocusable(false);
                mGridView.setSelectedPositionSmooth(focusableIndex);
            }
        });
        waitForScrollIdle(mVerifyLayout);
        assertEquals(focusableIndex, mGridView.getSelectedPosition());

        performAndWaitForAnimation(new Runnable() {
            @Override
            public void run() {
                mActivity.removeItems(focusableIndex, 1);
            }
        });
        assertFalse(dumpGridView(mGridView), mGridView.hasFocus());
    }

    @Test
    public void testNonFocusableVertical() throws Throwable {
        final int numItems = 200;
        final int startPos = 44;
        final int skips = 20;
        final int numColumns = 3;
        final int endPos = startPos + numColumns * (skips + 1);

        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_grid);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, numItems);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = numColumns;
        boolean[] focusable = new boolean[numItems];
        for (int i = 0; i < focusable.length; i++) {
            focusable[i] = true;
        }
        for (int i = startPos + mNumRows, j = 0; j < skips; i += mNumRows, j++) {
            focusable[i] = false;
        }
        intent.putExtra(GridActivity.EXTRA_ITEMS_FOCUSABLE, focusable);
        initActivity(intent);

        mGridView.setSelectedPositionSmooth(startPos);
        waitForScrollIdle(mVerifyLayout);

        sendKey(KeyEvent.KEYCODE_DPAD_DOWN);
        waitForScrollIdle(mVerifyLayout);
        assertEquals(endPos, mGridView.getSelectedPosition());

        sendKey(KeyEvent.KEYCODE_DPAD_UP);
        waitForScrollIdle(mVerifyLayout);
        assertEquals(startPos, mGridView.getSelectedPosition());

    }

    @Test
    public void testLtrFocusOutStartDisabled() throws Throwable {
        final int numItems = 200;

        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.vertical_grid_ltr);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, numItems);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 2;
        initActivity(intent);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.requestFocus();
                mGridView.setSelectedPositionSmooth(0);
            }
        });
        waitForScrollIdle(mVerifyLayout);

        sendKey(KeyEvent.KEYCODE_DPAD_LEFT);
        waitForScrollIdle(mVerifyLayout);
        assertTrue(mGridView.hasFocus());
    }

    @Test
    public void testVerticalGridRtl() throws Throwable {
        final int numItems = 200;

        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.vertical_grid_rtl);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, numItems);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 2;
        initActivity(intent);

        waitForScrollIdle(mVerifyLayout);

        View item0 = mGridView.findViewHolderForAdapterPosition(0).itemView;
        View item1 = mGridView.findViewHolderForAdapterPosition(1).itemView;
        assertEquals(mGridView.getWidth() - mGridView.getPaddingRight(), item0.getRight());
        assertEquals(item0.getLeft(), item1.getRight() + mGridView.getHorizontalSpacing());
    }

    @Test
    public void testRtlFocusOutStartDisabled() throws Throwable {
        final int numItems = 200;

        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.vertical_grid_rtl);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, numItems);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;
        initActivity(intent);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.requestFocus();
                mGridView.setSelectedPositionSmooth(0);
            }
        });
        waitForScrollIdle(mVerifyLayout);

        sendKey(KeyEvent.KEYCODE_DPAD_RIGHT);
        waitForScrollIdle(mVerifyLayout);
        assertTrue(mGridView.hasFocus());
    }

    @Test
    public void testTransferFocusable() throws Throwable {
        final int numItems = 200;
        final int numColumns = 3;
        final int startPos = 1;

        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_grid);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, numItems);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = numColumns;
        boolean[] focusable = new boolean[numItems];
        for (int i = 0; i < focusable.length; i++) {
            focusable[i] = true;
        }
        for (int i = 0; i < startPos; i++) {
            focusable[i] = false;
        }
        intent.putExtra(GridActivity.EXTRA_ITEMS_FOCUSABLE, focusable);
        initActivity(intent);

        changeArraySize(0);
        assertTrue(mGridView.isFocused());

        changeArraySize(numItems);
        assertTrue(mGridView.getLayoutManager().findViewByPosition(startPos).hasFocus());
    }

    @Test
    public void testTransferFocusable2() throws Throwable {
        final int numItems = 200;
        final int numColumns = 3;
        final int startPos = 3; // make sure view at startPos is in visible area.

        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_grid);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, numItems);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, true);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = numColumns;
        boolean[] focusable = new boolean[numItems];
        for (int i = 0; i < focusable.length; i++) {
            focusable[i] = true;
        }
        for (int i = 0; i < startPos; i++) {
            focusable[i] = false;
        }
        intent.putExtra(GridActivity.EXTRA_ITEMS_FOCUSABLE, focusable);
        initActivity(intent);

        assertTrue(mGridView.getLayoutManager().findViewByPosition(startPos).hasFocus());

        changeArraySize(0);
        assertTrue(mGridView.isFocused());

        changeArraySize(numItems);
        assertTrue(mGridView.getLayoutManager().findViewByPosition(startPos).hasFocus());
    }

    @Test
    public void testNonFocusableLoseInFastLayout() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_linear);
        int[] items = new int[300];
        for (int i = 0; i < items.length; i++) {
            items[i] = 480;
        }
        intent.putExtra(GridActivity.EXTRA_ITEMS, items);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        intent.putExtra(GridActivity.EXTRA_REQUEST_LAYOUT_ONFOCUS, true);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;
        int pressDown = 15;

        initActivity(intent);

        mGridView.setSelectedPositionSmooth(0);
        waitForScrollIdle(mVerifyLayout);

        for (int i = 0; i < pressDown; i++) {
            sendKey(KeyEvent.KEYCODE_DPAD_DOWN);
        }
        waitForScrollIdle(mVerifyLayout);
        assertFalse(mGridView.isFocused());

    }

    @Test
    public void testFocusableViewAvailable() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_linear);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 0);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        intent.putExtra(GridActivity.EXTRA_ITEMS_FOCUSABLE,
                new boolean[]{false, false, true, false, false});
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        initActivity(intent);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // RecyclerView does not respect focusable and focusableInTouchMode flag, so
                // set flags in code.
                mGridView.setFocusableInTouchMode(false);
                mGridView.setFocusable(false);
            }
        });

        assertFalse(mGridView.isFocused());

        final boolean[] scrolled = new boolean[]{false};
        mGridView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy){
                if (dy > 0) {
                    scrolled[0] = true;
                }
            }
        });
        performAndWaitForAnimation(new Runnable() {
            @Override
            public void run() {
                mActivity.addItems(0, new int[]{200, 300, 500, 500, 200});
            }
        });
        waitForScrollIdle(mVerifyLayout);

        assertFalse("GridView should not be scrolled", scrolled[0]);
        assertTrue(mGridView.getLayoutManager().findViewByPosition(2).hasFocus());

    }

    @Test
    public void testSetSelectionWithDelta() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_linear);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 300);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        initActivity(intent);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setSelectedPositionSmooth(3);
            }
        });
        waitForScrollIdle(mVerifyLayout);
        int top1 = mGridView.getLayoutManager().findViewByPosition(3).getTop();

        humanDelay(1000);

        // scroll to position with delta
        setSelectedPosition(3, 100);
        int top2 = mGridView.getLayoutManager().findViewByPosition(3).getTop();
        assertEquals(top1 - 100, top2);

        // scroll to same position without delta, it will be reset
        setSelectedPosition(3, 0);
        int top3 = mGridView.getLayoutManager().findViewByPosition(3).getTop();
        assertEquals(top1, top3);

        // scroll invisible item after last visible item
        final int lastVisiblePos = ((GridLayoutManager)mGridView.getLayoutManager())
                .mGrid.getLastVisibleIndex();
        setSelectedPosition(lastVisiblePos + 1, 100);
        int top4 = mGridView.getLayoutManager().findViewByPosition(lastVisiblePos + 1).getTop();
        assertEquals(top1 - 100, top4);

        // scroll invisible item before first visible item
        final int firstVisiblePos = ((GridLayoutManager)mGridView.getLayoutManager())
                .mGrid.getFirstVisibleIndex();
        setSelectedPosition(firstVisiblePos - 1, 100);
        int top5 = mGridView.getLayoutManager().findViewByPosition(firstVisiblePos - 1).getTop();
        assertEquals(top1 - 100, top5);

        // scroll to invisible item that is far away.
        setSelectedPosition(50, 100);
        int top6 = mGridView.getLayoutManager().findViewByPosition(50).getTop();
        assertEquals(top1 - 100, top6);

        // scroll to invisible item that is far away.
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setSelectedPositionSmooth(100);
            }
        });
        waitForScrollIdle(mVerifyLayout);
        int top7 = mGridView.getLayoutManager().findViewByPosition(100).getTop();
        assertEquals(top1, top7);

        // scroll to invisible item that is far away.
        setSelectedPosition(10, 50);
        int top8 = mGridView.getLayoutManager().findViewByPosition(10).getTop();
        assertEquals(top1 - 50, top8);
    }

    @Test
    public void testSetSelectionWithDeltaInGrid() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_grid);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 500);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, true);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 3;

        initActivity(intent);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setSelectedPositionSmooth(10);
            }
        });
        waitForScrollIdle(mVerifyLayout);
        int top1 = getCenterY(mGridView.getLayoutManager().findViewByPosition(10));

        humanDelay(500);

        // scroll to position with delta
        setSelectedPosition(20, 100);
        int top2 = getCenterY(mGridView.getLayoutManager().findViewByPosition(20));
        assertEquals(top1 - 100, top2);

        // scroll to same position without delta, it will be reset
        setSelectedPosition(20, 0);
        int top3 = getCenterY(mGridView.getLayoutManager().findViewByPosition(20));
        assertEquals(top1, top3);

        // scroll invisible item after last visible item
        final int lastVisiblePos = ((GridLayoutManager)mGridView.getLayoutManager())
                .mGrid.getLastVisibleIndex();
        setSelectedPosition(lastVisiblePos + 1, 100);
        int top4 = getCenterY(mGridView.getLayoutManager().findViewByPosition(lastVisiblePos + 1));
        verifyMargin();
        assertEquals(top1 - 100, top4);

        // scroll invisible item before first visible item
        final int firstVisiblePos = ((GridLayoutManager)mGridView.getLayoutManager())
                .mGrid.getFirstVisibleIndex();
        setSelectedPosition(firstVisiblePos - 1, 100);
        int top5 = getCenterY(mGridView.getLayoutManager().findViewByPosition(firstVisiblePos - 1));
        assertEquals(top1 - 100, top5);

        // scroll to invisible item that is far away.
        setSelectedPosition(100, 100);
        int top6 = getCenterY(mGridView.getLayoutManager().findViewByPosition(100));
        assertEquals(top1 - 100, top6);

        // scroll to invisible item that is far away.
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setSelectedPositionSmooth(200);
            }
        });
        waitForScrollIdle(mVerifyLayout);
        Thread.sleep(500);
        int top7 = getCenterY(mGridView.getLayoutManager().findViewByPosition(200));
        assertEquals(top1, top7);

        // scroll to invisible item that is far away.
        setSelectedPosition(10, 50);
        int top8 = getCenterY(mGridView.getLayoutManager().findViewByPosition(10));
        assertEquals(top1 - 50, top8);
    }


    @Test
    public void testSetSelectionWithDeltaInGrid1() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_grid);
        intent.putExtra(GridActivity.EXTRA_ITEMS, new int[]{
                193,176,153,141,203,184,232,139,177,206,222,136,132,237,172,137,
                188,172,163,213,158,219,209,147,133,229,170,197,138,215,188,205,
                223,192,225,170,195,127,229,229,210,195,134,142,160,139,130,222,
                150,163,180,176,157,137,234,169,159,167,182,150,224,231,202,236,
                123,140,181,223,120,185,183,221,123,210,134,158,166,208,149,128,
                192,214,212,198,133,140,158,133,229,173,226,141,180,128,127,218,
                192,235,183,213,216,150,143,193,125,141,219,210,195,195,192,191,
                212,236,157,189,160,220,147,158,220,199,233,231,201,180,168,141,
                156,204,191,183,190,153,123,210,238,151,139,221,223,200,175,191,
                132,184,197,204,236,157,230,151,195,219,212,143,172,149,219,184,
                164,211,132,187,172,142,174,146,127,147,206,238,188,129,199,226,
                132,220,210,159,235,153,208,182,196,123,180,159,131,135,175,226,
                127,134,237,211,133,225,132,124,160,226,224,200,173,137,217,169,
                182,183,176,185,122,168,195,159,172,129,126,129,166,136,149,220,
                178,191,192,238,180,208,234,154,222,206,239,228,129,140,203,125,
                214,175,125,169,196,132,234,138,192,142,234,190,215,232,239,122,
                188,158,128,221,159,237,207,157,232,138,132,214,122,199,121,191,
                199,209,126,164,175,187,173,186,194,224,191,196,146,208,213,210,
                164,176,202,213,123,157,179,138,217,129,186,166,237,211,157,130,
                137,132,171,232,216,239,180,151,137,132,190,133,218,155,171,227,
                193,147,197,164,120,218,193,154,170,196,138,222,161,235,143,154,
                192,178,228,195,178,133,203,178,173,206,178,212,136,157,169,124,
                172,121,128,223,238,125,217,187,184,156,169,215,231,124,210,174,
                146,226,185,134,223,228,183,182,136,133,199,146,180,233,226,225,
                174,233,145,235,216,170,192,171,132,132,134,223,233,148,154,162,
                192,179,197,203,139,197,174,187,135,132,180,136,192,195,124,221,
                120,189,233,233,146,225,234,163,215,143,132,198,156,205,151,190,
                204,239,221,229,123,138,134,217,219,136,218,215,167,139,195,125,
                202,225,178,226,145,208,130,194,228,197,157,215,124,147,174,123,
                237,140,172,181,161,151,229,216,199,199,179,213,146,122,222,162,
                139,173,165,150,160,217,207,137,165,175,129,158,134,133,178,199,
                215,213,122,197
        });
        intent.putExtra(GridActivity.EXTRA_STAGGERED, true);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 3;

        initActivity(intent);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setSelectedPositionSmooth(10);
            }
        });
        waitForScrollIdle(mVerifyLayout);
        int top1 = getCenterY(mGridView.getLayoutManager().findViewByPosition(10));

        humanDelay(500);

        // scroll to position with delta
        setSelectedPosition(20, 100);
        int top2 = getCenterY(mGridView.getLayoutManager().findViewByPosition(20));
        assertEquals(top1 - 100, top2);

        // scroll to same position without delta, it will be reset
        setSelectedPosition(20, 0);
        int top3 = getCenterY(mGridView.getLayoutManager().findViewByPosition(20));
        assertEquals(top1, top3);

        // scroll invisible item after last visible item
        final int lastVisiblePos = ((GridLayoutManager)mGridView.getLayoutManager())
                .mGrid.getLastVisibleIndex();
        setSelectedPosition(lastVisiblePos + 1, 100);
        int top4 = getCenterY(mGridView.getLayoutManager().findViewByPosition(lastVisiblePos + 1));
        verifyMargin();
        assertEquals(top1 - 100, top4);

        // scroll invisible item before first visible item
        final int firstVisiblePos = ((GridLayoutManager)mGridView.getLayoutManager())
                .mGrid.getFirstVisibleIndex();
        setSelectedPosition(firstVisiblePos - 1, 100);
        int top5 = getCenterY(mGridView.getLayoutManager().findViewByPosition(firstVisiblePos - 1));
        assertEquals(top1 - 100, top5);

        // scroll to invisible item that is far away.
        setSelectedPosition(100, 100);
        int top6 = getCenterY(mGridView.getLayoutManager().findViewByPosition(100));
        assertEquals(top1 - 100, top6);

        // scroll to invisible item that is far away.
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setSelectedPositionSmooth(200);
            }
        });
        waitForScrollIdle(mVerifyLayout);
        Thread.sleep(500);
        int top7 = getCenterY(mGridView.getLayoutManager().findViewByPosition(200));
        assertEquals(top1, top7);

        // scroll to invisible item that is far away.
        setSelectedPosition(10, 50);
        int top8 = getCenterY(mGridView.getLayoutManager().findViewByPosition(10));
        assertEquals(top1 - 50, top8);
    }

    @Test
    public void testSmoothScrollSelectionEvents() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_grid);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 500);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 3;
        initActivity(intent);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setSelectedPositionSmooth(30);
            }
        });
        waitForScrollIdle(mVerifyLayout);
        humanDelay(500);

        final ArrayList<Integer> selectedPositions = new ArrayList<Integer>();
        mGridView.setOnChildSelectedListener(new OnChildSelectedListener() {
            @Override
            public void onChildSelected(
                    @NonNull ViewGroup parent,
                    @Nullable View view,
                    int position,
                    long id
            ) {
                selectedPositions.add(position);
            }
        });

        sendRepeatedKeys(100, KeyEvent.KEYCODE_DPAD_UP);
        humanDelay(500);
        waitForScrollIdle(mVerifyLayout);
        // should only get childselected event for item 0 once
        assertTrue(selectedPositions.size() > 0);
        assertEquals(0, selectedPositions.get(selectedPositions.size() - 1).intValue());
        for (int i = selectedPositions.size() - 2; i >= 0; i--) {
            assertFalse(0 == selectedPositions.get(i).intValue());
        }

    }

    @Test
    public void testSmoothScrollSelectionEventsLinear() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_linear);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 500);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;
        initActivity(intent);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setSelectedPositionSmooth(10);
            }
        });
        waitForScrollIdle(mVerifyLayout);
        humanDelay(500);

        final ArrayList<Integer> selectedPositions = new ArrayList<Integer>();
        mGridView.setOnChildSelectedListener(new OnChildSelectedListener() {
            @Override
            public void onChildSelected(
                    @NonNull ViewGroup parent,
                    @Nullable View view,
                    int position,
                    long id
            ) {
                selectedPositions.add(position);
            }
        });

        sendRepeatedKeys(100, KeyEvent.KEYCODE_DPAD_UP);
        humanDelay(500);
        waitForScrollIdle(mVerifyLayout);
        // should only get childselected event for item 0 once
        assertTrue(selectedPositions.size() > 0);
        assertEquals(0, selectedPositions.get(selectedPositions.size() - 1).intValue());
        for (int i = selectedPositions.size() - 2; i >= 0; i--) {
            assertFalse(0 == selectedPositions.get(i).intValue());
        }

    }

    @Test
    public void testScrollToNoneExisting() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_grid);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 100);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 3;
        initActivity(intent);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setSelectedPositionSmooth(99);
            }
        });
        waitForScrollIdle(mVerifyLayout);
        humanDelay(500);


        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setSelectedPositionSmooth(50);
            }
        });
        Thread.sleep(100);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.requestLayout();
                mGridView.setSelectedPositionSmooth(0);
            }
        });
        waitForScrollIdle(mVerifyLayout);
        humanDelay(500);

    }

    @Test
    public void testSmoothscrollerInterrupted() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_linear);
        intent.putExtra(GridActivity.EXTRA_REQUEST_FOCUS_ONLAYOUT, true);
        int[] items = new int[100];
        for (int i = 0; i < items.length; i++) {
            items[i] = 680;
        }
        intent.putExtra(GridActivity.EXTRA_ITEMS, items);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        initActivity(intent);

        mGridView.setSelectedPositionSmooth(0);
        waitForScrollIdle(mVerifyLayout);
        assertTrue(mGridView.getChildAt(0).hasFocus());

        // Pressing lots of key to make sure smooth scroller is running
        for (int i = 0; i < 20; i++) {
            sendKey(KeyEvent.KEYCODE_DPAD_DOWN);
        }
        while (mGridView.getLayoutManager().isSmoothScrolling()
                || mGridView.getScrollState() != BaseGridView.SCROLL_STATE_IDLE) {
            // Repeatedly pressing to make sure pending keys does not drop to zero.
            sendKey(KeyEvent.KEYCODE_DPAD_DOWN);
        }
    }

    @Test
    public void testSmoothscrollerCancelled() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_linear);
        intent.putExtra(GridActivity.EXTRA_REQUEST_FOCUS_ONLAYOUT, true);
        int[] items = new int[100];
        for (int i = 0; i < items.length; i++) {
            items[i] = 680;
        }
        intent.putExtra(GridActivity.EXTRA_ITEMS, items);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        initActivity(intent);

        mGridView.setSelectedPositionSmooth(0);
        waitForScrollIdle(mVerifyLayout);
        assertTrue(mGridView.getChildAt(0).hasFocus());

        int targetPosition = items.length - 1;
        mGridView.setSelectedPositionSmooth(targetPosition);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.stopScroll();
            }
        });
        waitForScrollIdle();
        waitForItemAnimation();
        assertEquals(mGridView.getSelectedPosition(), targetPosition);
        assertSame(mGridView.getLayoutManager().findViewByPosition(targetPosition),
                mGridView.findFocus());
    }

    @Test
    public void testSetNumRowsAndAddItem() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_linear);
        intent.putExtra(GridActivity.EXTRA_REQUEST_FOCUS_ONLAYOUT, true);
        final int[] items = new int[2];
        for (int i = 0; i < items.length; i++) {
            items[i] = 300;
        }
        intent.putExtra(GridActivity.EXTRA_ITEMS, items);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        initActivity(intent);

        mGridView.setSelectedPositionSmooth(0);
        waitForScrollIdle(mVerifyLayout);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.addItems(items.length, new int[]{300});
            }
        });

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((VerticalGridView) mGridView).setNumColumns(2);
            }
        });
        Thread.sleep(1000);
        assertTrue(mGridView.getChildAt(2).getLeft() != mGridView.getChildAt(1).getLeft());
    }


    @Test
    public void testRequestLayoutBugInLayout() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_linear);
        intent.putExtra(GridActivity.EXTRA_CHILD_LAYOUT_ID, R.layout.relative_layout);
        intent.putExtra(GridActivity.EXTRA_REQUEST_FOCUS_ONLAYOUT, true);
        int[] items = new int[100];
        for (int i = 0; i < items.length; i++) {
            items[i] = 300;
        }
        intent.putExtra(GridActivity.EXTRA_ITEMS, items);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        initActivity(intent);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setSelectedPositionSmooth(1);
            }
        });
        waitForScrollIdle(mVerifyLayout);

        sendKey(KeyEvent.KEYCODE_DPAD_UP);
        waitForScrollIdle(mVerifyLayout);

        assertEquals("Line 2", ((TextView) mGridView.findFocus()).getText().toString());
    }


    @Test
    public void testChangeLayoutInChild() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_linear_wrap_content);
        intent.putExtra(GridActivity.EXTRA_REQUEST_LAYOUT_ONFOCUS, true);
        int[] items = new int[2];
        for (int i = 0; i < items.length; i++) {
            items[i] = 300;
        }
        intent.putExtra(GridActivity.EXTRA_ITEMS, items);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        initActivity(intent);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setSelectedPositionSmooth(0);
            }
        });
        waitForScrollIdle(mVerifyLayout);
        verifyMargin();

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setSelectedPositionSmooth(1);
            }
        });
        waitForScrollIdle(mVerifyLayout);
        verifyMargin();
    }

    @Test
    public void testWrapContent() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_grid_wrap);
        int[] items = new int[200];
        for (int i = 0; i < items.length; i++) {
            items[i] = 300;
        }
        intent.putExtra(GridActivity.EXTRA_ITEMS, items);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;

        initActivity(intent);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.attachToNewAdapter(new int[0]);
            }
        });

    }

    @Test
    public void testZeroFixedSecondarySize() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_linear_measured_with_zero);
        intent.putExtra(GridActivity.EXTRA_SECONDARY_SIZE_ZERO, true);
        int[] items = new int[2];
        for (int i = 0; i < items.length; i++) {
            items[i] = 0;
        }
        intent.putExtra(GridActivity.EXTRA_ITEMS, items);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        initActivity(intent);

    }

    @Test
    public void testChildStates() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.vertical_linear);
        int[] items = new int[100];
        for (int i = 0; i < items.length; i++) {
            items[i] = 200;
        }
        intent.putExtra(GridActivity.EXTRA_ITEMS, items);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        intent.putExtra(GridActivity.EXTRA_REQUEST_LAYOUT_ONFOCUS, true);
        intent.putExtra(GridActivity.EXTRA_CHILD_LAYOUT_ID, R.layout.selectable_text_view);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        initActivity(intent);
        mGridView.setSaveChildrenPolicy(VerticalGridView.SAVE_ALL_CHILD);

        final SparseArray<Parcelable> container = new SparseArray<Parcelable>();

        // 1 Save view states
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Selection.setSelection((Spannable)(((TextView) mGridView.getChildAt(0))
                        .getText()), 0, 1);
                Selection.setSelection((Spannable)(((TextView) mGridView.getChildAt(1))
                        .getText()), 0, 1);
                mGridView.saveHierarchyState(container);
            }
        });

        // 2 Change view states
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Selection.setSelection((Spannable)(((TextView) mGridView.getChildAt(0))
                        .getText()), 1, 2);
                Selection.setSelection((Spannable)(((TextView) mGridView.getChildAt(1))
                        .getText()), 1, 2);
            }
        });

        // 3 Detached and re-attached,  should still maintain state of (2)
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setSelectedPositionSmooth(1);
            }
        });
        waitForScrollIdle(mVerifyLayout);
        assertEquals(((TextView) mGridView.getChildAt(0)).getSelectionStart(), 1);
        assertEquals(((TextView) mGridView.getChildAt(0)).getSelectionEnd(), 2);
        assertEquals(((TextView) mGridView.getChildAt(1)).getSelectionStart(), 1);
        assertEquals(((TextView) mGridView.getChildAt(1)).getSelectionEnd(), 2);

        // 4 Recycled and rebound, should load state from (2)
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setSelectedPositionSmooth(20);
            }
        });
        waitForScrollIdle(mVerifyLayout);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setSelectedPositionSmooth(0);
            }
        });
        waitForScrollIdle(mVerifyLayout);
        assertEquals(((TextView) mGridView.getChildAt(0)).getSelectionStart(), 1);
        assertEquals(((TextView) mGridView.getChildAt(0)).getSelectionEnd(), 2);
        assertEquals(((TextView) mGridView.getChildAt(1)).getSelectionStart(), 1);
        assertEquals(((TextView) mGridView.getChildAt(1)).getSelectionEnd(), 2);
    }


    @Test
    public void testNoDispatchSaveChildState() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.vertical_linear);
        int[] items = new int[100];
        for (int i = 0; i < items.length; i++) {
            items[i] = 200;
        }
        intent.putExtra(GridActivity.EXTRA_ITEMS, items);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        intent.putExtra(GridActivity.EXTRA_CHILD_LAYOUT_ID, R.layout.selectable_text_view);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        initActivity(intent);
        mGridView.setSaveChildrenPolicy(VerticalGridView.SAVE_NO_CHILD);

        final SparseArray<Parcelable> container = new SparseArray<Parcelable>();

        // 1. Set text selection, save view states should do nothing on child
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < mGridView.getChildCount(); i++) {
                    Selection.setSelection((Spannable)(((TextView) mGridView.getChildAt(i))
                            .getText()), 0, 1);
                }
                mGridView.saveHierarchyState(container);
            }
        });

        // 2. clear the text selection
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < mGridView.getChildCount(); i++) {
                    Selection.removeSelection((Spannable)(((TextView) mGridView.getChildAt(i))
                            .getText()));
                }
            }
        });

        // 3. Restore view states should be a no-op for child
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.restoreHierarchyState(container);
                for (int i = 0; i < mGridView.getChildCount(); i++) {
                    assertEquals(-1, ((TextView) mGridView.getChildAt(i)).getSelectionStart());
                    assertEquals(-1, ((TextView) mGridView.getChildAt(i)).getSelectionEnd());
                }
            }
        });
    }


    static interface ViewTypeProvider {
        public int getViewType(int position);
    }

    static interface ItemAlignmentFacetProvider {
        public ItemAlignmentFacet getItemAlignmentFacet(int viewType);
    }

    static class TwoViewTypesProvider implements ViewTypeProvider {
        static int VIEW_TYPE_FIRST = 1;
        static int VIEW_TYPE_DEFAULT = 0;
        @Override
        public int getViewType(int position) {
            if (position == 0) {
                return VIEW_TYPE_FIRST;
            } else {
                return VIEW_TYPE_DEFAULT;
            }
        }
    }

    static class ChangeableViewTypesProvider implements ViewTypeProvider {
        static SparseIntArray sViewTypes = new SparseIntArray();
        @Override
        public int getViewType(int position) {
            return sViewTypes.get(position);
        }
        public static void clear() {
            sViewTypes.clear();
        }
        public static void setViewType(int position, int type) {
            sViewTypes.put(position, type);
        }
    }

    static class PositionItemAlignmentFacetProviderForRelativeLayout1
            implements ItemAlignmentFacetProvider {
        ItemAlignmentFacet mMultipleFacet;

        PositionItemAlignmentFacetProviderForRelativeLayout1() {
            mMultipleFacet = new ItemAlignmentFacet();
            ItemAlignmentFacet.ItemAlignmentDef[] defs =
                    new ItemAlignmentFacet.ItemAlignmentDef[2];
            defs[0] = new ItemAlignmentFacet.ItemAlignmentDef();
            defs[0].setItemAlignmentViewId(R.id.t1);
            defs[1] = new ItemAlignmentFacet.ItemAlignmentDef();
            defs[1].setItemAlignmentViewId(R.id.t2);
            defs[1].setItemAlignmentOffsetPercent(100);
            defs[1].setItemAlignmentOffset(-10);
            mMultipleFacet.setAlignmentDefs(defs);
        }

        @Override
        public ItemAlignmentFacet getItemAlignmentFacet(int position) {
            if (position == 0) {
                return mMultipleFacet;
            } else {
                return null;
            }
        }
    }

    @Test
    public void testMultipleScrollPosition1() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_linear);
        intent.putExtra(GridActivity.EXTRA_CHILD_LAYOUT_ID, R.layout.relative_layout);
        intent.putExtra(GridActivity.EXTRA_REQUEST_FOCUS_ONLAYOUT, true);
        int[] items = new int[100];
        for (int i = 0; i < items.length; i++) {
            items[i] = 300;
        }
        intent.putExtra(GridActivity.EXTRA_ITEMS, items);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        intent.putExtra(GridActivity.EXTRA_VIEWTYPEPROVIDER_CLASS,
                TwoViewTypesProvider.class.getName());
        // Set ItemAlignment for each ViewHolder and view type,  ViewHolder should
        // override the view type settings.
        intent.putExtra(GridActivity.EXTRA_ITEMALIGNMENTPROVIDER_CLASS,
                PositionItemAlignmentFacetProviderForRelativeLayout1.class.getName());
        intent.putExtra(GridActivity.EXTRA_ITEMALIGNMENTPROVIDER_VIEWTYPE_CLASS,
                ViewTypePositionItemAlignmentFacetProviderForRelativeLayout2.class.getName());
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        initActivity(intent);

        assertEquals("First view is aligned with padding top",
                mGridView.getPaddingTop(), mGridView.getChildAt(0).getTop());

        sendKey(KeyEvent.KEYCODE_DPAD_DOWN);
        waitForScrollIdle(mVerifyLayout);

        final View v = mGridView.getChildAt(0);
        View t1 = v.findViewById(R.id.t1);
        int t1align = (t1.getTop() + t1.getBottom()) / 2;
        View t2 = v.findViewById(R.id.t2);
        int t2align = t2.getBottom() - 10;
        assertEquals("Expected alignment for 2nd textview",
                mGridView.getPaddingTop() - (t2align - t1align),
                v.getTop());
    }

    static class PositionItemAlignmentFacetProviderForRelativeLayout2 implements
            ItemAlignmentFacetProvider {
        ItemAlignmentFacet mMultipleFacet;

        PositionItemAlignmentFacetProviderForRelativeLayout2() {
            mMultipleFacet = new ItemAlignmentFacet();
            ItemAlignmentFacet.ItemAlignmentDef[] defs = new ItemAlignmentFacet.ItemAlignmentDef[2];
            defs[0] = new ItemAlignmentFacet.ItemAlignmentDef();
            defs[0].setItemAlignmentViewId(R.id.t1);
            defs[0].setItemAlignmentOffsetPercent(0);
            defs[1] = new ItemAlignmentFacet.ItemAlignmentDef();
            defs[1].setItemAlignmentViewId(R.id.t2);
            defs[1].setItemAlignmentOffsetPercent(ItemAlignmentFacet.ITEM_ALIGN_OFFSET_PERCENT_DISABLED);
            defs[1].setItemAlignmentOffset(-10);
            mMultipleFacet.setAlignmentDefs(defs);
        }

        @Override
        public ItemAlignmentFacet getItemAlignmentFacet(int position) {
            if (position == 0) {
                return mMultipleFacet;
            } else {
                return null;
            }
        }
    }

    @Test
    public void testMultipleScrollPosition2() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.vertical_linear);
        intent.putExtra(GridActivity.EXTRA_CHILD_LAYOUT_ID, R.layout.relative_layout);
        intent.putExtra(GridActivity.EXTRA_REQUEST_FOCUS_ONLAYOUT, true);
        int[] items = new int[100];
        for (int i = 0; i < items.length; i++) {
            items[i] = 300;
        }
        intent.putExtra(GridActivity.EXTRA_ITEMS, items);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        intent.putExtra(GridActivity.EXTRA_VIEWTYPEPROVIDER_CLASS,
                TwoViewTypesProvider.class.getName());
        intent.putExtra(GridActivity.EXTRA_ITEMALIGNMENTPROVIDER_CLASS,
                PositionItemAlignmentFacetProviderForRelativeLayout2.class.getName());
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        initActivity(intent);

        assertEquals("First view is aligned with padding top", mGridView.getPaddingTop(),
                mGridView.getChildAt(0).getTop());

        sendKey(KeyEvent.KEYCODE_DPAD_DOWN);
        waitForScrollIdle(mVerifyLayout);

        final View v = mGridView.getChildAt(0);
        View t1 = v.findViewById(R.id.t1);
        int t1align = t1.getTop();
        View t2 = v.findViewById(R.id.t2);
        int t2align = t2.getTop() - 10;
        assertEquals("Expected alignment for 2nd textview",
                mGridView.getPaddingTop() - (t2align - t1align), v.getTop());
    }

    static class ViewTypePositionItemAlignmentFacetProviderForRelativeLayout2 implements
            ItemAlignmentFacetProvider {
        ItemAlignmentFacet mMultipleFacet;

        ViewTypePositionItemAlignmentFacetProviderForRelativeLayout2() {
            mMultipleFacet = new ItemAlignmentFacet();
            ItemAlignmentFacet.ItemAlignmentDef[] defs = new ItemAlignmentFacet.ItemAlignmentDef[2];
            defs[0] = new ItemAlignmentFacet.ItemAlignmentDef();
            defs[0].setItemAlignmentViewId(R.id.t1);
            defs[0].setItemAlignmentOffsetPercent(0);
            defs[1] = new ItemAlignmentFacet.ItemAlignmentDef();
            defs[1].setItemAlignmentViewId(R.id.t2);
            defs[1].setItemAlignmentOffsetPercent(100);
            defs[1].setItemAlignmentOffset(-10);
            mMultipleFacet.setAlignmentDefs(defs);
        }

        @Override
        public ItemAlignmentFacet getItemAlignmentFacet(int viewType) {
            if (viewType == TwoViewTypesProvider.VIEW_TYPE_FIRST) {
                return mMultipleFacet;
            } else {
                return null;
            }
        }
    }

    @Test
    public void testMultipleScrollPosition3() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.vertical_linear);
        intent.putExtra(GridActivity.EXTRA_CHILD_LAYOUT_ID, R.layout.relative_layout);
        intent.putExtra(GridActivity.EXTRA_REQUEST_FOCUS_ONLAYOUT, true);
        int[] items = new int[100];
        for (int i = 0; i < items.length; i++) {
            items[i] = 300;
        }
        intent.putExtra(GridActivity.EXTRA_ITEMS, items);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        intent.putExtra(GridActivity.EXTRA_VIEWTYPEPROVIDER_CLASS,
                TwoViewTypesProvider.class.getName());
        intent.putExtra(GridActivity.EXTRA_ITEMALIGNMENTPROVIDER_VIEWTYPE_CLASS,
                ViewTypePositionItemAlignmentFacetProviderForRelativeLayout2.class.getName());
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        initActivity(intent);

        assertEquals("First view is aligned with padding top", mGridView.getPaddingTop(),
                mGridView.getChildAt(0).getTop());

        sendKey(KeyEvent.KEYCODE_DPAD_DOWN);
        waitForScrollIdle(mVerifyLayout);

        final View v = mGridView.getChildAt(0);
        View t1 = v.findViewById(R.id.t1);
        int t1align = t1.getTop();
        View t2 = v.findViewById(R.id.t2);
        int t2align = t2.getBottom() - 10;
        assertEquals("Expected alignment for 2nd textview",
                mGridView.getPaddingTop() - (t2align - t1align), v.getTop());
    }

    static class TestFastLayoutWithItemAlignmentDefProvider implements
            ItemAlignmentFacetProvider {
        ItemAlignmentFacet mItemAlignmentFacet;

        TestFastLayoutWithItemAlignmentDefProvider() {
            mItemAlignmentFacet = new ItemAlignmentFacet();
            ItemAlignmentFacet.ItemAlignmentDef[] defs = new ItemAlignmentFacet.ItemAlignmentDef[1];
            defs[0] = new ItemAlignmentFacet.ItemAlignmentDef();
            defs[0].setItemAlignmentOffsetPercent(0);
            defs[0].setItemAlignmentOffset(-500);
            mItemAlignmentFacet.setAlignmentDefs(defs);
        }

        @Override
        public ItemAlignmentFacet getItemAlignmentFacet(int viewType) {
            return mItemAlignmentFacet;
        }
    }

    private void testFastLayoutNotifyItemChange(boolean supportsChangeAnimation) throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.vertical_linear);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 1);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        intent.putExtra(GridActivity.EXTRA_ITEMALIGNMENTPROVIDER_VIEWTYPE_CLASS,
                TestFastLayoutWithItemAlignmentDefProvider.class.getName());
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        initActivity(intent);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setWindowAlignment(BaseGridView.WINDOW_ALIGN_NO_EDGE);
                mGridView.setWindowAlignmentOffset(0);
                mGridView.setWindowAlignmentOffsetPercent(BaseGridView
                        .WINDOW_ALIGN_OFFSET_PERCENT_DISABLED);
                ((DefaultItemAnimator) mGridView.getItemAnimator()).setSupportsChangeAnimations(
                        supportsChangeAnimation);
            }
        });
        waitOneUiCycle();
        assertEquals(500, mGridView.findViewHolderForAdapterPosition(0).itemView.getTop());

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.getAdapter().notifyItemChanged(0);
            }
        });
        waitOneUiCycle();
        assertEquals(500, mGridView.findViewHolderForAdapterPosition(0).itemView.getTop());
    }

    @Test
    public void testFastLayoutNotifyItemChangeWithoutChangeAnimation() throws Throwable {
        testFastLayoutNotifyItemChange(/* supportsChangeAnimation= */ false);
    }

    @Test
    public void testFastLayoutNotifyItemChangeWithChangeAnimation() throws Throwable {
        testFastLayoutNotifyItemChange(/* supportsChangeAnimation= */ true);
    }

    @Test
    public void testSelectionAndAddItemInOneCycle() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_linear);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 0);
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;

        performAndWaitForAnimation(new Runnable() {
            @Override
            public void run() {
                mActivity.addItems(0, new int[]{300, 300});
                mGridView.setSelectedPosition(0);
            }
        });
        assertEquals(0, mGridView.getSelectedPosition());
    }

    @Test
    public void testSelectViewTaskSmoothWithAdapterChange() throws Throwable {
        testSelectViewTaskWithAdapterChange(true /*smooth*/);
    }

    @Test
    public void testSelectViewTaskWithAdapterChange() throws Throwable {
        testSelectViewTaskWithAdapterChange(false /*smooth*/);
    }

    private void testSelectViewTaskWithAdapterChange(final boolean smooth) throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_linear);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 2);
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;

        final View firstView = mGridView.getLayoutManager().findViewByPosition(0);
        final View[] selectedViewByTask = new View[1];
        final ViewHolderTask task = new ViewHolderTask() {
            @Override
            public void run(RecyclerView.ViewHolder viewHolder) {
                selectedViewByTask[0] = viewHolder.itemView;
            }
        };
        performAndWaitForAnimation(new Runnable() {
            @Override
            public void run() {
                mActivity.removeItems(0, 1);
                if (smooth) {
                    mGridView.setSelectedPositionSmooth(0, task);
                } else {
                    mGridView.setSelectedPosition(0, task);
                }
            }
        });
        assertEquals(0, mGridView.getSelectedPosition());
        assertNotNull(selectedViewByTask[0]);
        assertNotSame(firstView, selectedViewByTask[0]);
        assertSame(mGridView.getLayoutManager().findViewByPosition(0), selectedViewByTask[0]);
    }

    @Test
    public void testNotifyItemTypeChangedSelectionEvent() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_linear);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 10);
        intent.putExtra(GridActivity.EXTRA_VIEWTYPEPROVIDER_CLASS,
                ChangeableViewTypesProvider.class.getName());
        ChangeableViewTypesProvider.clear();
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;

        final ArrayList<Integer> selectedLog = new ArrayList<Integer>();
        mGridView.setOnChildSelectedListener(new OnChildSelectedListener() {
            @Override
            public void onChildSelected(
                    @NonNull ViewGroup parent,
                    @Nullable View view,
                    int position,
                    long id
            ) {
                selectedLog.add(position);
            }
        });

        performAndWaitForAnimation(new Runnable() {
            @Override
            public void run() {
                ChangeableViewTypesProvider.setViewType(0, 1);
                mGridView.getAdapter().notifyItemChanged(0, 1);
            }
        });
        assertEquals(0, mGridView.getSelectedPosition());
        assertEquals(selectedLog.size(), 1);
        assertEquals((int) selectedLog.get(0), 0);
    }

    @Test
    public void testNotifyItemChangedSelectionEvent() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_linear);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 10);
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;

        OnChildViewHolderSelectedListener listener =
                Mockito.mock(OnChildViewHolderSelectedListener.class);
        mGridView.setOnChildViewHolderSelectedListener(listener);

        performAndWaitForAnimation(new Runnable() {
            @Override
            public void run() {
                mGridView.getAdapter().notifyItemChanged(0, 1);
            }
        });
        Mockito.verify(listener, times(1)).onChildViewHolderSelected(any(RecyclerView.class),
                any(RecyclerView.ViewHolder.class), anyInt(), anyInt());
        assertEquals(0, mGridView.getSelectedPosition());
    }

    @Test
    public void testSelectionSmoothAndAddItemInOneCycle() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_linear);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 0);
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;

        performAndWaitForAnimation(new Runnable() {
            @Override
            public void run() {
                mActivity.addItems(0, new int[]{300, 300});
                mGridView.setSelectedPositionSmooth(0);
            }
        });
        assertEquals(0, mGridView.getSelectedPosition());
    }

    @FlakyTest(bugId = 187191618)
    @Test
    public void testExtraLayoutSpace() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_linear);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 1000);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        initActivity(intent);

        final int windowSize = mGridView.getHeight();
        final int extraLayoutSize = windowSize;
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        // add extra layout space
        startWaitLayout();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setExtraLayoutSpace(extraLayoutSize);
            }
        });
        waitForLayout();
        View v;
        v = mGridView.getChildAt(mGridView.getChildCount() - 1);
        assertTrue(v.getTop() < windowSize + extraLayoutSize);
        assertTrue(v.getBottom() >= windowSize + extraLayoutSize - mGridView.getVerticalMargin());

        mGridView.setSelectedPositionSmooth(150);
        waitForScrollIdle(mVerifyLayout);
        v = mGridView.getChildAt(0);
        assertTrue(v.getBottom() > - extraLayoutSize);
        assertTrue(v.getTop() <= -extraLayoutSize + mGridView.getVerticalMargin());

        // clear extra layout space
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setExtraLayoutSpace(0);
                verifyMargin();
            }
        });
        Thread.sleep(50);
        v = mGridView.getChildAt(mGridView.getChildCount() - 1);
        assertTrue(v.getTop() < windowSize);
        assertTrue(v.getBottom() >= windowSize - mGridView.getVerticalMargin());
    }

    @Test
    public void testFocusFinder() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_linear_with_button);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 3);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        initActivity(intent);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        // test focus from button to vertical grid view
        final View button = mActivity.findViewById(R.id.button);
        assertTrue(button.isFocused());
        sendKey(KeyEvent.KEYCODE_DPAD_DOWN);
        assertFalse(mGridView.isFocused());
        assertTrue(mGridView.hasFocus());

        // FocusFinder should find last focused(2nd) item on DPAD_DOWN
        final View secondChild = mGridView.getChildAt(1);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                secondChild.requestFocus();
                button.requestFocus();
            }
        });
        assertTrue(button.isFocused());
        sendKey(KeyEvent.KEYCODE_DPAD_DOWN);
        assertTrue(secondChild.isFocused());

        // Bug 26918143 Even VerticalGridView is not focusable, FocusFinder should find last focused
        // (2nd) item on DPAD_DOWN.
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                button.requestFocus();
            }
        });
        mGridView.setFocusable(false);
        mGridView.setFocusableInTouchMode(false);
        assertTrue(button.isFocused());
        sendKey(KeyEvent.KEYCODE_DPAD_DOWN);
        assertTrue(secondChild.isFocused());
    }

    @Test
    public void testRestoreIndexAndAddItems() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_linear);
        intent.putExtra(GridActivity.EXTRA_CHILD_LAYOUT_ID, R.layout.horizontal_item);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 4);
        initActivity(intent);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        assertEquals(mGridView.getSelectedPosition(), 0);
        final SparseArray<Parcelable> states = new SparseArray<>();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.saveHierarchyState(states);
                mGridView.setAdapter(null);
            }

        });
        performAndWaitForAnimation(new Runnable() {
            @Override
            public void run() {
                mGridView.restoreHierarchyState(states);
                mActivity.attachToNewAdapter(new int[0]);
                mActivity.addItems(0, new int[]{100, 100, 100, 100});
            }

        });
        assertEquals(mGridView.getSelectedPosition(), 0);
    }

    @Test
    public void testRestoreIndexAndAddItemsSelect1() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_linear);
        intent.putExtra(GridActivity.EXTRA_CHILD_LAYOUT_ID, R.layout.horizontal_item);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 4);
        initActivity(intent);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setSelectedPosition(1);
            }

        });
        assertEquals(mGridView.getSelectedPosition(), 1);
        final SparseArray<Parcelable> states = new SparseArray<>();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.saveHierarchyState(states);
                mGridView.setAdapter(null);
            }

        });
        performAndWaitForAnimation(new Runnable() {
            @Override
            public void run() {
                mGridView.restoreHierarchyState(states);
                mActivity.attachToNewAdapter(new int[0]);
                mActivity.addItems(0, new int[]{100, 100, 100, 100});
            }

        });
        assertEquals(mGridView.getSelectedPosition(), 1);
    }

    @Test
    public void testRestoreStateAfterAdapterChange() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_linear);
        intent.putExtra(GridActivity.EXTRA_CHILD_LAYOUT_ID, R.layout.selectable_text_view);
        intent.putExtra(GridActivity.EXTRA_ITEMS, new int[]{50, 50, 50, 50});
        initActivity(intent);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setSelectedPosition(1);
                mGridView.setSaveChildrenPolicy(VerticalGridView.SAVE_ALL_CHILD);
            }

        });
        assertEquals(mGridView.getSelectedPosition(), 1);
        final SparseArray<Parcelable> states = new SparseArray<>();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Selection.setSelection((Spannable) (((TextView) mGridView.getChildAt(0))
                        .getText()), 1, 2);
                Selection.setSelection((Spannable) (((TextView) mGridView.getChildAt(1))
                        .getText()), 0, 1);
                mGridView.saveHierarchyState(states);
                mGridView.setAdapter(null);
            }

        });
        performAndWaitForAnimation(new Runnable() {
            @Override
            public void run() {
                mGridView.restoreHierarchyState(states);
                mActivity.attachToNewAdapter(new int[]{50, 50, 50, 50});
            }

        });
        assertEquals(mGridView.getSelectedPosition(), 1);
        assertEquals(1, ((TextView) mGridView.getChildAt(0)).getSelectionStart());
        assertEquals(2, ((TextView) mGridView.getChildAt(0)).getSelectionEnd());
        assertEquals(0, ((TextView) mGridView.getChildAt(1)).getSelectionStart());
        assertEquals(1, ((TextView) mGridView.getChildAt(1)).getSelectionEnd());
    }

    @Test
    @AnimationTest
    public void test27766012() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_linear_with_button_onleft);
        intent.putExtra(GridActivity.EXTRA_CHILD_LAYOUT_ID, R.layout.horizontal_item);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 2);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        intent.putExtra(GridActivity.EXTRA_UPDATE_SIZE, false);
        initActivity(intent);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        // set remove animator two seconds
        mGridView.getItemAnimator().setRemoveDuration(2000);
        final View view = mGridView.getChildAt(1);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                view.requestFocus();
            }
        });
        assertTrue(view.hasFocus());
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.removeItems(0, 2);
            }

        });
        // wait one second, removing second view is still attached to parent
        Thread.sleep(1000);
        assertSame(view.getParent(), mGridView);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // refocus to the removed item and do a focus search.
                view.requestFocus();
                view.focusSearch(View.FOCUS_UP);
            }

        });
    }

    @Test
    public void testBug27258366() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_linear_with_button_onleft);
        intent.putExtra(GridActivity.EXTRA_CHILD_LAYOUT_ID, R.layout.horizontal_item);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 10);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        intent.putExtra(GridActivity.EXTRA_UPDATE_SIZE, false);
        initActivity(intent);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        // move item1 500 pixels right, when focus is on item1, default focus finder will pick
        // item0 and item2 for the best match of focusSearch(FOCUS_LEFT).  The grid widget
        // must override default addFocusables(), not to add item0 or item2.
        mActivity.mAdapterListener = new GridActivity.AdapterListener() {
            @Override
            public void onBind(RecyclerView.ViewHolder vh, int position) {
                if (position == 1) {
                    vh.itemView.setPaddingRelative(500, 0, 0, 0);
                } else {
                    vh.itemView.setPaddingRelative(0, 0, 0, 0);
                }
            }
        };
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.getAdapter().notifyDataSetChanged();
            }
        });
        Thread.sleep(100);

        final ViewGroup secondChild = (ViewGroup) mGridView.getChildAt(1);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                secondChild.requestFocus();
            }
        });
        sendKey(KeyEvent.KEYCODE_DPAD_LEFT);
        Thread.sleep(100);
        final View button = mActivity.findViewById(R.id.button);
        assertTrue(button.isFocused());
    }

    @Test
    public void testBug161359022() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_linear_with_scrollview);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 3);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        intent.putExtra(GridActivity.EXTRA_UPDATE_SIZE, false);
        boolean[] focusable = new boolean[] {false, true, false};
        intent.putExtra(GridActivity.EXTRA_ITEMS_FOCUSABLE, focusable);
        initActivity(intent);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        // Notify second item with payload, causes clearFocus() of the itemView.  On P and above
        // clearFocus() will trigger a top-down requestFocus(), and the ScrollView will call
        // RecyclerView.addFocusables() where our RecyclerView is in a transitioning-state that
        // hasFocusable() is true but findFocus() is null.
        performAndWaitForAnimation(new Runnable() {
            @Override
            public void run() {
                mActivity.mItemFocusables[1] = false;
                mActivity.mGridView.getAdapter().notifyItemChanged(1, new Object());
            }
        });
    }

    @Test
    public void testUpdateHeightScrollHorizontal() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_linear);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 30);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        intent.putExtra(GridActivity.EXTRA_REQUEST_LAYOUT_ONFOCUS, true);
        intent.putExtra(GridActivity.EXTRA_UPDATE_SIZE, false);
        intent.putExtra(GridActivity.EXTRA_UPDATE_SIZE_SECONDARY, true);
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;

        final int childTop = mGridView.getChildAt(0).getTop();
        // scroll to end, all children's top should not change.
        scrollToEnd(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < mGridView.getChildCount(); i++) {
                    assertEquals(childTop, mGridView.getChildAt(i).getTop());
                }
            }
        });
        // sanity check last child has focus with a larger height.
        assertTrue(mGridView.getChildAt(0).getHeight()
                < mGridView.getChildAt(mGridView.getChildCount() - 1).getHeight());
    }

    @Test
    public void testUpdateWidthScrollHorizontal() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_linear);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 30);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        intent.putExtra(GridActivity.EXTRA_REQUEST_LAYOUT_ONFOCUS, true);
        intent.putExtra(GridActivity.EXTRA_UPDATE_SIZE, true);
        intent.putExtra(GridActivity.EXTRA_UPDATE_SIZE_SECONDARY, false);
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;

        final int childTop = mGridView.getChildAt(0).getTop();
        // scroll to end, all children's top should not change.
        scrollToEnd(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < mGridView.getChildCount(); i++) {
                    assertEquals(childTop, mGridView.getChildAt(i).getTop());
                }
            }
        });
        // sanity check last child has focus with a larger width.
        assertTrue(mGridView.getChildAt(0).getWidth()
                < mGridView.getChildAt(mGridView.getChildCount() - 1).getWidth());
        if (mGridView.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
            assertEquals(mGridView.getPaddingLeft(),
                    mGridView.getChildAt(mGridView.getChildCount() - 1).getLeft());
        } else {
            assertEquals(mGridView.getWidth() - mGridView.getPaddingRight(),
                    mGridView.getChildAt(mGridView.getChildCount() - 1).getRight());
        }
    }

    @Test
    public void testUpdateWidthScrollHorizontalRtl() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_linear_rtl);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 30);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        intent.putExtra(GridActivity.EXTRA_REQUEST_LAYOUT_ONFOCUS, true);
        intent.putExtra(GridActivity.EXTRA_UPDATE_SIZE, true);
        intent.putExtra(GridActivity.EXTRA_UPDATE_SIZE_SECONDARY, false);
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;

        final int childTop = mGridView.getChildAt(0).getTop();
        // scroll to end, all children's top should not change.
        scrollToEnd(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < mGridView.getChildCount(); i++) {
                    assertEquals(childTop, mGridView.getChildAt(i).getTop());
                }
            }
        });
        // sanity check last child has focus with a larger width.
        assertTrue(mGridView.getChildAt(0).getWidth()
                < mGridView.getChildAt(mGridView.getChildCount() - 1).getWidth());
        assertEquals(mGridView.getPaddingLeft(),
                mGridView.getChildAt(mGridView.getChildCount() - 1).getLeft());
    }

    @Test
    public void testAccessibility() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_linear);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 1000);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        initActivity(intent);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        assertTrue(0 == mGridView.getSelectedPosition());

        final RecyclerViewAccessibilityDelegate delegateCompat = mGridView
                .getCompatAccessibilityDelegate();
        final AccessibilityNodeInfoCompat info = AccessibilityNodeInfoCompat.obtain();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                delegateCompat.onInitializeAccessibilityNodeInfo(mGridView, info);
            }
        });
        assertTrue("test sanity", info.isScrollable());
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                delegateCompat.performAccessibilityAction(mGridView,
                        AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD, null);
            }
        });
        waitForScrollIdle(mVerifyLayout);
        int selectedPosition1 = mGridView.getSelectedPosition();
        assertTrue(0 < selectedPosition1);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                delegateCompat.onInitializeAccessibilityNodeInfo(mGridView, info);
            }
        });
        assertTrue("test sanity", info.isScrollable());
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                delegateCompat.performAccessibilityAction(mGridView,
                        AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD, null);
            }
        });
        waitForScrollIdle(mVerifyLayout);
        int selectedPosition2 = mGridView.getSelectedPosition();
        assertTrue(selectedPosition2 < selectedPosition1);
    }

    @SdkSuppress(minSdkVersion = 23) // b/271599830
    @Test
    public void testAccessibilityFocusOutFrontEnd_actionsAvailable() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_linear);
        int[] items = new int[5];
        for (int i = 0; i < items.length; i++) {
            items[i] = 300;
        }
        intent.putExtra(GridActivity.EXTRA_ITEMS, items);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        initActivity(intent);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;
        final RecyclerViewAccessibilityDelegate delegateCompat = mGridView
                .getCompatAccessibilityDelegate();
        final AccessibilityNodeInfoCompat info1 = AccessibilityNodeInfoCompat.obtain();
        // Test not allowing going out both ends
        mLayoutManager.setFocusOutAllowed(/* throughFront= */ false,
                /* throughEnd= */ false);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                delegateCompat.onInitializeAccessibilityNodeInfo(mGridView, info1);
            }
        });
        // When not allowing jumping out both end, handle action scroll backward/forward to block
        // it.
        if (Build.VERSION.SDK_INT >= 21) {
            assertTrue(hasAction(info1,
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_RIGHT));
            assertTrue(hasAction(info1,
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_LEFT));
        } else {
            assertTrue(hasAction(info1,
                    AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD));
            assertTrue(hasAction(info1,
                    AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD));
        }
        final AccessibilityNodeInfoCompat info2 = AccessibilityNodeInfoCompat.obtain();
        // Test allowing focus to jump out at front when reaching front.
        mLayoutManager.setFocusOutAllowed(/* throughFront= */ true,
                /* throughEnd= */ false);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                delegateCompat.onInitializeAccessibilityNodeInfo(mGridView, info2);
            }
        });
        // When only allowing jumping out front, block action scroll backward when reaching front
        // for Talkback to jump focus out.
        if (Build.VERSION.SDK_INT >= 21) {
            assertFalse(hasAction(info2,
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_LEFT));
            assertTrue(hasAction(info2,
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_RIGHT));
        } else {
            assertFalse(hasAction(info2,
                    AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD));
            assertTrue(hasAction(info2,
                    AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD));
        }
    }

    @Test
    public void testAccessibilitySaveContextCrash() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_linear);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 1000);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        initActivity(intent);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        // Returns 0 causes smoothScrollBy becomes a scrollBy to entry saveContext.
        mGridView.setSmoothScrollByBehavior(new BaseGridView.SmoothScrollByBehavior() {
            @Override
            public int configSmoothScrollByDuration(int dx, int dy) {
                return 0;
            }

            @Override
            public Interpolator configSmoothScrollByInterpolator(int dx, int dy) {
                return null;
            }
        });
        final RecyclerViewAccessibilityDelegate delegateCompat = mGridView
                .getCompatAccessibilityDelegate();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                delegateCompat.performAccessibilityAction(mGridView,
                        AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD, null);
            }
        });
        waitForScrollIdle(mVerifyLayout);
    }

    @Test
    public void testAccessibilityScrollForwardHalfVisible() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.vertical_linear);
        intent.putExtra(GridActivity.EXTRA_CHILD_LAYOUT_ID, R.layout.item_button_at_bottom);
        intent.putExtra(GridActivity.EXTRA_ITEMS,  new int[]{});
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        initActivity(intent);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        int height = mGridView.getHeight() - mGridView.getPaddingTop()
                - mGridView.getPaddingBottom();
        final int childHeight = height - mGridView.getVerticalSpacing() - 100;
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setWindowAlignment(BaseGridView.WINDOW_ALIGN_NO_EDGE);
                mGridView.setWindowAlignmentOffset(100);
                mGridView.setWindowAlignmentOffsetPercent(BaseGridView
                        .WINDOW_ALIGN_OFFSET_PERCENT_DISABLED);
                mGridView.setItemAlignmentOffset(0);
                mGridView.setItemAlignmentOffsetPercent(BaseGridView
                        .ITEM_ALIGN_OFFSET_PERCENT_DISABLED);
            }
        });
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.addItems(0, new int[]{childHeight, childHeight});
            }
        });
        waitForItemAnimation();
        setSelectedPosition(0);

        final RecyclerViewAccessibilityDelegate delegateCompat = mGridView
                .getCompatAccessibilityDelegate();
        final AccessibilityNodeInfoCompat info = AccessibilityNodeInfoCompat.obtain();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                delegateCompat.onInitializeAccessibilityNodeInfo(mGridView, info);
            }
        });
        assertTrue("test sanity", info.isScrollable());
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                delegateCompat.performAccessibilityAction(mGridView,
                        AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD, null);
            }
        });
        waitForScrollIdle(mVerifyLayout);
        assertEquals(1, mGridView.getSelectedPosition());
    }

    // Temporarily disabled due to b/110377468
    // @Test
    public void testAccessibilityBug77292190() throws Throwable {
        Intent intent = new Intent();
        final int numItems = 1000;
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.vertical_linear);
        intent.putExtra(GridActivity.EXTRA_CHILD_LAYOUT_ID, R.layout.item_full_width);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS,  1000);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        initActivity(intent);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        setSelectedPosition(0);

        final RecyclerViewAccessibilityDelegate delegateCompat = mGridView
                .getCompatAccessibilityDelegate();
        final AccessibilityNodeInfoCompat info = AccessibilityNodeInfoCompat.obtain();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                delegateCompat.onInitializeAccessibilityNodeInfo(mGridView, info);
            }
        });
        if (Build.VERSION.SDK_INT >= 21) {
            assertFalse(hasAction(info,
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_UP));
            assertTrue(hasAction(info,
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_DOWN));
        } else {
            assertFalse(hasAction(info, AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD));
            assertTrue(hasAction(info, AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD));
        }

        setSelectedPosition(numItems - 1);
        final AccessibilityNodeInfoCompat info2 = AccessibilityNodeInfoCompat.obtain();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                delegateCompat.onInitializeAccessibilityNodeInfo(mGridView, info2);
            }
        });
        if (Build.VERSION.SDK_INT >= 21) {
            assertTrue(hasAction(info2,
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_UP));
            assertFalse(hasAction(info2,
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_DOWN));
        } else {
            assertTrue(hasAction(info2, AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD));
            assertFalse(hasAction(info2, AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD));
        }
    }

    @Test
    public void testAccessibilityWhenScrollDisabled() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.vertical_linear);
        intent.putExtra(GridActivity.EXTRA_CHILD_LAYOUT_ID, R.layout.item_button_at_bottom);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS,  1000);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        initActivity(intent);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        setSelectedPosition(0);

        final RecyclerViewAccessibilityDelegate delegateCompat = mGridView
                .getCompatAccessibilityDelegate();
        final AccessibilityNodeInfoCompat info = AccessibilityNodeInfoCompat.obtain();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                delegateCompat.onInitializeAccessibilityNodeInfo(mGridView, info);
            }
        });
        mGridView.setScrollEnabled(false);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (int i  = 0; i < 100; i++) {
                    assertTrue(delegateCompat.performAccessibilityAction(mGridView,
                            AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD, null));
                }
            }
        });
        assertEquals(RecyclerView.SCROLL_STATE_IDLE, mGridView.getScrollState());
    }
    private boolean hasAction(AccessibilityNodeInfoCompat info, Object action) {
        if (Build.VERSION.SDK_INT >= 21) {
            AccessibilityNodeInfoCompat.AccessibilityActionCompat convertedAction =
                    (AccessibilityNodeInfoCompat.AccessibilityActionCompat) action;
            List<AccessibilityNodeInfoCompat.AccessibilityActionCompat> actions =
                    info.getActionList();
            for (int i = 0; i < actions.size(); i++) {
                if (actions.get(i).getId() == convertedAction.getId()) {
                    return true;
                }
            }
            return false;
        } else {
            int convertedAction = (int) action;
            return ((info.getActions() & convertedAction) != 0);
        }
    }

    private void setUpActivityForScrollingTest(final boolean isRTL, boolean isHorizontal,
            int numChildViews, boolean isSiblingViewVisible) throws Throwable {
        Intent intent = new Intent();
        int layout;
        if (isHorizontal) {
            layout = isRTL ? R.layout.horizontal_linear_rtl : R.layout.horizontal_linear;
        } else {
            layout = R.layout.vertical_linear;
        }
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, layout);
        intent.putExtra(GridActivity.EXTRA_CHILD_LAYOUT_ID, R.layout.item_button_at_bottom);
        intent.putExtra(GridActivity.EXTRA_ITEMS,  new int[]{});
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        initActivity(intent);
        mOrientation = isHorizontal ? BaseGridView.HORIZONTAL : BaseGridView.VERTICAL;
        mNumRows = 1;

        final int offset = (isSiblingViewVisible ? 2 : 1) * (isHorizontal
                ? mGridView.getHorizontalSpacing() : mGridView.getVerticalSpacing());
        final int childSize = (isHorizontal ? mGridView.getWidth() : mGridView.getHeight())
                - offset - (isHorizontal ? 2 * mGridView.getHorizontalSpacing() :
                mGridView.getVerticalSpacing());
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isRTL) {
                    mGridView.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                }
                mGridView.setWindowAlignment(BaseGridView.WINDOW_ALIGN_NO_EDGE);
                mGridView.setWindowAlignmentOffset(offset);
                mGridView.setWindowAlignmentOffsetPercent(BaseGridView
                        .WINDOW_ALIGN_OFFSET_PERCENT_DISABLED);
                mGridView.setItemAlignmentOffset(0);
                mGridView.setItemAlignmentOffsetPercent(BaseGridView
                        .ITEM_ALIGN_OFFSET_PERCENT_DISABLED);
            }
        });
        final int[] widthArrays = new int[numChildViews];
        Arrays.fill(widthArrays, childSize);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.addItems(0, widthArrays);
            }
        });
    }

    private void testScrollingAction(boolean isRTL, boolean isHorizontal) throws Throwable {
        waitForItemAnimation();
        setSelectedPosition(1);
        final RecyclerViewAccessibilityDelegate delegateCompat = mGridView
                .getCompatAccessibilityDelegate();
        final AccessibilityNodeInfoCompat info = AccessibilityNodeInfoCompat.obtain();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                delegateCompat.onInitializeAccessibilityNodeInfo(mGridView, info);
            }
        });
        // We are currently focusing on item 1, calculating the direction to get me to item 0
        final AccessibilityNodeInfoCompat.AccessibilityActionCompat itemZeroDirection;
        if (isHorizontal) {
            itemZeroDirection = isRTL
                    ? AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_RIGHT :
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_LEFT;
        } else {
            itemZeroDirection =
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_UP;
        }
        final int translatedItemZeroDirection = AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD;

        assertTrue("test sanity", info.isScrollable());
        if (Build.VERSION.SDK_INT >= 23) {
            assertTrue("test sanity", hasAction(info, itemZeroDirection));
        } else {
            assertTrue("test sanity", hasAction(info, translatedItemZeroDirection));
        }

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= 23) {
                    delegateCompat.performAccessibilityAction(mGridView, itemZeroDirection.getId(),
                            null);
                } else {
                    delegateCompat.performAccessibilityAction(mGridView,
                            translatedItemZeroDirection, null);
                }
            }
        });
        waitForScrollIdle(mVerifyLayout);
        assertEquals(0, mGridView.getSelectedPosition());
        setSelectedPosition(0);
        // We are at item 0, calculate the direction that lead us to the item 1
        final AccessibilityNodeInfoCompat.AccessibilityActionCompat itemOneDirection;
        if (isHorizontal) {
            itemOneDirection = isRTL
                    ? AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_LEFT
                    : AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_RIGHT;
        } else {
            itemOneDirection =
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_DOWN;
        }
        final int translatedItemOneDirection = AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD;

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                delegateCompat.onInitializeAccessibilityNodeInfo(mGridView, info);
            }
        });
        if (Build.VERSION.SDK_INT >= 23) {
            assertTrue("test sanity", hasAction(info, itemOneDirection));
        } else {
            assertTrue("test sanity", hasAction(info, translatedItemOneDirection));
        }
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= 23) {
                    delegateCompat.performAccessibilityAction(mGridView, itemOneDirection.getId(),
                            null);
                } else {
                    delegateCompat.performAccessibilityAction(mGridView, translatedItemOneDirection,
                            null);
                }
            }
        });
        waitForScrollIdle(mVerifyLayout);
        assertEquals(1, mGridView.getSelectedPosition());
    }

    // Temporarily disabled due to b/110377468
    // @Test
    public void testAccessibilityRespondToLeftRightInvisible() throws Throwable {
        boolean isRTL = false;
        boolean isHorizontal = true;
        setUpActivityForScrollingTest(isRTL, isHorizontal, 2 /* numChild */,
                false /* next child partially visible */);
        testScrollingAction(isRTL, isHorizontal);
    }

    // Temporarily disabled due to b/110377468
    // @Test
    public void testAccessibilityRespondToLeftRightPartiallyVisible() throws Throwable {
        boolean isRTL = false;
        boolean isHorizontal = true;
        setUpActivityForScrollingTest(isRTL, isHorizontal, 2 /* numChild */,
                true /* next child partially visible */);
        testScrollingAction(isRTL, isHorizontal);
    }

    // Temporarily disabled due to b/110377468
    // @Test
    public void testAccessibilityRespondToLeftRightRtlInvisible()
            throws Throwable {
        boolean isRTL = true;
        boolean isHorizontal = true;
        setUpActivityForScrollingTest(isRTL, isHorizontal, 2 /* numChild */,
                false /* next child partially visible */);
        testScrollingAction(isRTL, isHorizontal);
    }

    // Temporarily disabled due to b/110377468
    // @Test
    public void testAccessibilityRespondToLeftRightRtlPartiallyVisible() throws Throwable {
        boolean isRTL = true;
        boolean isHorizontal = true;
        setUpActivityForScrollingTest(isRTL, isHorizontal, 2 /* numChild */,
                true /* next child partially visible */);
        testScrollingAction(isRTL, isHorizontal);
    }

    // Temporarily disabled due to b/110377468
    // @Test
    public void testAccessibilityRespondToScrollUpDownActionInvisible() throws Throwable {
        boolean isRTL = false;
        boolean isHorizontal = false;
        setUpActivityForScrollingTest(isRTL, isHorizontal, 2 /* numChild */,
                false /* next child partially visible */);
        testScrollingAction(isRTL, isHorizontal);
    }

    // Temporarily disabled due to b/110377468
    // @Test
    public void testAccessibilityRespondToScrollUpDownActionPartiallyVisible() throws Throwable {
        boolean isRTL = false;
        boolean isHorizontal = false;
        setUpActivityForScrollingTest(isRTL, isHorizontal, 2 /* numChild */,
                true /* next child partially visible */);
        testScrollingAction(isRTL, isHorizontal);
    }

    @Test
    public void testAccessibilityScrollBackwardHalfVisible() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.vertical_linear);
        intent.putExtra(GridActivity.EXTRA_CHILD_LAYOUT_ID, R.layout.item_button_at_top);
        intent.putExtra(GridActivity.EXTRA_ITEMS,  new int[]{});
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        initActivity(intent);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        int height = mGridView.getHeight() - mGridView.getPaddingTop()
                - mGridView.getPaddingBottom();
        final int childHeight = height - mGridView.getVerticalSpacing() - 100;
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setWindowAlignment(BaseGridView.WINDOW_ALIGN_NO_EDGE);
                mGridView.setWindowAlignmentOffset(100);
                mGridView.setWindowAlignmentOffsetPercent(BaseGridView
                        .WINDOW_ALIGN_OFFSET_PERCENT_DISABLED);
                mGridView.setItemAlignmentOffset(0);
                mGridView.setItemAlignmentOffsetPercent(BaseGridView
                        .ITEM_ALIGN_OFFSET_PERCENT_DISABLED);
            }
        });
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.addItems(0, new int[]{childHeight, childHeight});
            }
        });
        waitForItemAnimation();
        setSelectedPosition(1);

        final RecyclerViewAccessibilityDelegate delegateCompat = mGridView
                .getCompatAccessibilityDelegate();
        final AccessibilityNodeInfoCompat info = AccessibilityNodeInfoCompat.obtain();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                delegateCompat.onInitializeAccessibilityNodeInfo(mGridView, info);
            }
        });
        assertTrue("test sanity", info.isScrollable());
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                delegateCompat.performAccessibilityAction(mGridView,
                        AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD, null);
            }
        });
        waitForScrollIdle(mVerifyLayout);
        assertEquals(0, mGridView.getSelectedPosition());
    }

    void slideInAndWaitIdle() throws Throwable {
        slideInAndWaitIdle(5000);
    }

    void slideInAndWaitIdle(long timeout) throws Throwable {
        // animateIn() would reset position
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.animateIn();
            }
        });
        PollingCheck.waitFor(timeout, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return !mGridView.getLayoutManager().isSmoothScrolling()
                        && mGridView.getScrollState() == RecyclerView.SCROLL_STATE_IDLE;
            }
        });
    }

    @Test
    public void testAnimateOutBlockScrollTo() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_linear_with_button_onleft);
        int[] items = new int[100];
        for (int i = 0; i < items.length; i++) {
            items[i] = 300;
        }
        intent.putExtra(GridActivity.EXTRA_ITEMS, items);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        initActivity(intent);

        assertEquals("First view is aligned with padding top", mGridView.getPaddingTop(),
                mGridView.getChildAt(0).getTop());

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.animateOut();
            }
        });
        // wait until sliding out.
        PollingCheck.waitFor(new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return mGridView.getChildAt(0).getTop() > mGridView.getPaddingTop();
            }
        });
        // scrollToPosition() should not affect slideOut status
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.scrollToPosition(0);
            }
        });
        PollingCheck.waitFor(new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return mGridView.getScrollState() == RecyclerView.SCROLL_STATE_IDLE;
            }
        });
        assertTrue("First view slided Out", mGridView.getChildAt(0).getTop()
                >= mGridView.getHeight());

        slideInAndWaitIdle();
        assertEquals("First view is aligned with padding top", mGridView.getPaddingTop(),
                mGridView.getChildAt(0).getTop());
    }

    @Test
    public void testAnimateOutBlockSmoothScrolling() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_linear_with_button_onleft);
        int[] items = new int[30];
        for (int i = 0; i < items.length; i++) {
            items[i] = 300;
        }
        intent.putExtra(GridActivity.EXTRA_ITEMS, items);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        initActivity(intent);

        assertEquals("First view is aligned with padding top", mGridView.getPaddingTop(),
                mGridView.getChildAt(0).getTop());

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.animateOut();
            }
        });
        // wait until sliding out.
        PollingCheck.waitFor(new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return mGridView.getChildAt(0).getTop() > mGridView.getPaddingTop();
            }
        });
        // smoothScrollToPosition() should not affect slideOut status
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.smoothScrollToPosition(29);
            }
        });
        PollingCheck.waitFor(10000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return mGridView.getScrollState() == RecyclerView.SCROLL_STATE_IDLE;
            }
        });
        assertTrue("First view slided Out", mGridView.getChildAt(0).getTop()
                >= mGridView.getHeight());

        slideInAndWaitIdle();
        View lastChild = mGridView.getChildAt(mGridView.getChildCount() - 1);
        assertSame("Scrolled to last child",
                mGridView.findViewHolderForAdapterPosition(29).itemView, lastChild);
    }

    @Test
    public void testAnimateOutBlockLongScrollTo() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_linear_with_button_onleft);
        int[] items = new int[30];
        for (int i = 0; i < items.length; i++) {
            items[i] = 300;
        }
        intent.putExtra(GridActivity.EXTRA_ITEMS, items);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        initActivity(intent);

        assertEquals("First view is aligned with padding top", mGridView.getPaddingTop(),
                mGridView.getChildAt(0).getTop());

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.animateOut();
            }
        });
        // wait until sliding out.
        PollingCheck.waitFor(new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return mGridView.getChildAt(0).getTop() > mGridView.getPaddingTop();
            }
        });
        // smoothScrollToPosition() should not affect slideOut status
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.scrollToPosition(29);
            }
        });
        PollingCheck.waitFor(10000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return mGridView.getScrollState() == RecyclerView.SCROLL_STATE_IDLE;
            }
        });
        assertTrue("First view slided Out", mGridView.getChildAt(0).getTop()
                >= mGridView.getHeight());

        slideInAndWaitIdle();
        View lastChild = mGridView.getChildAt(mGridView.getChildCount() - 1);
        assertSame("Scrolled to last child",
                mGridView.findViewHolderForAdapterPosition(29).itemView, lastChild);
    }

    @FlakyTest
    @Test
    public void testAnimateOutBlockLayout() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_linear_with_button_onleft);
        int[] items = new int[100];
        for (int i = 0; i < items.length; i++) {
            items[i] = 300;
        }
        intent.putExtra(GridActivity.EXTRA_ITEMS, items);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        initActivity(intent);

        assertEquals("First view is aligned with padding top", mGridView.getPaddingTop(),
                mGridView.getChildAt(0).getTop());

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.animateOut();
            }
        });
        // wait until sliding out.
        PollingCheck.waitFor(new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return mGridView.getChildAt(0).getTop() > mGridView.getPaddingTop();
            }
        });
        // change adapter should not affect slideOut status
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.changeItem(0, 200);
            }
        });
        PollingCheck.waitFor(new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return mGridView.getScrollState() == RecyclerView.SCROLL_STATE_IDLE;
            }
        });
        assertTrue("First view slided Out", mGridView.getChildAt(0).getTop()
                >= mGridView.getHeight());
        assertEquals("onLayout suppressed during slide out", 300,
                mGridView.getChildAt(0).getHeight());

        slideInAndWaitIdle();
        assertEquals("First view is aligned with padding top", mGridView.getPaddingTop(),
                mGridView.getChildAt(0).getTop());
        // size of item should be updated immediately after slide in animation finishes:
        PollingCheck.waitFor(1000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return 200 == mGridView.getChildAt(0).getHeight();
            }
        });
    }

    @Test
    public void testAnimateOutBlockFocusChange() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_linear_with_button_onleft);
        int[] items = new int[100];
        for (int i = 0; i < items.length; i++) {
            items[i] = 300;
        }
        intent.putExtra(GridActivity.EXTRA_ITEMS, items);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        initActivity(intent);

        assertEquals("First view is aligned with padding top", mGridView.getPaddingTop(),
                mGridView.getChildAt(0).getTop());

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.animateOut();
                mActivity.findViewById(R.id.button).requestFocus();
            }
        });
        assertTrue(mActivity.findViewById(R.id.button).hasFocus());
        PollingCheck.waitFor(new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return mGridView.getChildAt(0).getTop() > mGridView.getPaddingTop();
            }
        });
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.requestFocus();
            }
        });
        PollingCheck.waitFor(new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return mGridView.getScrollState() == RecyclerView.SCROLL_STATE_IDLE;
            }
        });
        assertTrue("First view slided Out", mGridView.getChildAt(0).getTop()
                >= mGridView.getHeight());

        slideInAndWaitIdle();
        assertEquals("First view is aligned with padding top", mGridView.getPaddingTop(),
                mGridView.getChildAt(0).getTop());
    }

    @Test
    public void testHorizontalAnimateOutBlockScrollTo() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_linear);
        int[] items = new int[100];
        for (int i = 0; i < items.length; i++) {
            items[i] = 300;
        }
        intent.putExtra(GridActivity.EXTRA_ITEMS, items);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;

        initActivity(intent);

        assertEquals("First view is aligned with padding left", mGridView.getPaddingLeft(),
                mGridView.getChildAt(0).getLeft());

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.animateOut();
            }
        });
        PollingCheck.waitFor(new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return mGridView.getChildAt(0).getLeft() > mGridView.getPaddingLeft();
            }
        });
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.scrollToPosition(0);
            }
        });
        PollingCheck.waitFor(new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return mGridView.getScrollState() == RecyclerView.SCROLL_STATE_IDLE;
            }
        });

        assertTrue("First view is slided out", mGridView.getChildAt(0).getLeft()
                > mGridView.getWidth());

        slideInAndWaitIdle();
        assertEquals("First view is aligned with padding left", mGridView.getPaddingLeft(),
                mGridView.getChildAt(0).getLeft());

    }

    @Test
    public void testHorizontalAnimateOutRtl() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.horizontal_linear_rtl);
        int[] items = new int[100];
        for (int i = 0; i < items.length; i++) {
            items[i] = 300;
        }
        intent.putExtra(GridActivity.EXTRA_ITEMS, items);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;

        initActivity(intent);

        assertEquals("First view is aligned with padding right",
                mGridView.getWidth() - mGridView.getPaddingRight(),
                mGridView.getChildAt(0).getRight());

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.animateOut();
            }
        });
        PollingCheck.waitFor(new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return mGridView.getChildAt(0).getRight()
                        < mGridView.getWidth() - mGridView.getPaddingRight();
            }
        });
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.smoothScrollToPosition(0);
            }
        });
        PollingCheck.waitFor(new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return mGridView.getScrollState() == RecyclerView.SCROLL_STATE_IDLE;
            }
        });

        assertTrue("First view is slided out", mGridView.getChildAt(0).getRight() < 0);

        slideInAndWaitIdle();
        assertEquals("First view is aligned with padding right",
                mGridView.getWidth() - mGridView.getPaddingRight(),
                mGridView.getChildAt(0).getRight());
    }

    @Test
    public void testSmoothScrollerOutRange() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.vertical_linear_with_button_onleft);
        intent.putExtra(GridActivity.EXTRA_REQUEST_FOCUS_ONLAYOUT, true);
        int[] items = new int[30];
        for (int i = 0; i < items.length; i++) {
            items[i] = 680;
        }
        intent.putExtra(GridActivity.EXTRA_ITEMS, items);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        initActivity(intent);

        final View button = mActivity.findViewById(R.id.button);
        mActivityTestRule.runOnUiThread(new Runnable() {
            public void run() {
                button.requestFocus();
            }
        });

        mGridView.setSelectedPositionSmooth(0);
        waitForScrollIdle(mVerifyLayout);

        mActivityTestRule.runOnUiThread(new Runnable() {
            public void run() {
                mGridView.setSelectedPositionSmooth(120);
            }
        });
        waitForScrollIdle(mVerifyLayout);
        assertTrue(button.hasFocus());
        int key;
        if (mGridView.getLayoutDirection() == ViewGroup.LAYOUT_DIRECTION_RTL) {
            key = KeyEvent.KEYCODE_DPAD_LEFT;
        } else {
            key = KeyEvent.KEYCODE_DPAD_RIGHT;
        }
        sendKey(key);
        // the GridView should has focus in its children
        assertTrue(mGridView.hasFocus());
        assertFalse(mGridView.isFocused());
        assertEquals(29, mGridView.getSelectedPosition());
    }

    @Test
    @AnimationTest
    public void testRemoveLastItemWithStableId() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.vertical_linear);
        intent.putExtra(GridActivity.EXTRA_HAS_STABLE_IDS, true);
        int[] items = new int[1];
        for (int i = 0; i < items.length; i++) {
            items[i] = 680;
        }
        intent.putExtra(GridActivity.EXTRA_ITEMS, items);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        initActivity(intent);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.getItemAnimator().setRemoveDuration(2000);
                mActivity.removeItems(0, 1, false);
                mGridView.getAdapter().notifyDataSetChanged();
            }
        });
        Thread.sleep(500);
        assertEquals(-1, mGridView.getSelectedPosition());
    }

    @Test
    public void testUpdateAndSelect1() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.vertical_linear);
        intent.putExtra(GridActivity.EXTRA_HAS_STABLE_IDS, false);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 10);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        initActivity(intent);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.getAdapter().notifyDataSetChanged();
                mGridView.setSelectedPosition(1);
            }
        });
        waitOneUiCycle();
        assertEquals(1, mGridView.getSelectedPosition());
    }

    @Test
    public void testUpdateAndSelect2() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.vertical_linear);
        intent.putExtra(GridActivity.EXTRA_HAS_STABLE_IDS, false);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 100);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        initActivity(intent);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.getAdapter().notifyDataSetChanged();
                mGridView.setSelectedPosition(50);
            }
        });
        waitOneUiCycle();
        assertEquals(50, mGridView.getSelectedPosition());
    }

    @Test
    public void testUpdateAndSelect3() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.vertical_linear);
        intent.putExtra(GridActivity.EXTRA_HAS_STABLE_IDS, false);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 10);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        initActivity(intent);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int[] newItems = new int[100];
                for (int i = 0; i < newItems.length; i++) {
                    newItems[i] = mActivity.mItemLengths[0];
                }
                mActivity.addItems(0, newItems, false);
                mGridView.getAdapter().notifyDataSetChanged();
                mGridView.setSelectedPosition(50);
            }
        });
        waitOneUiCycle();
        assertEquals(50, mGridView.getSelectedPosition());
    }

    @Test
    public void testFocusedPositonAfterRemoved1() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.vertical_linear);
        final int[] items = new int[2];
        for (int i = 0; i < items.length; i++) {
            items[i] = 300;
        }
        intent.putExtra(GridActivity.EXTRA_ITEMS, items);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        initActivity(intent);
        setSelectedPosition(1);
        assertEquals(1, mGridView.getSelectedPosition());

        final int[] newItems = new int[3];
        for (int i = 0; i < newItems.length; i++) {
            newItems[i] = 300;
        }
        performAndWaitForAnimation(new Runnable() {
            @Override
            public void run() {
                mActivity.removeItems(0, 2, true);
                mActivity.addItems(0, newItems, true);
            }
        });
        assertEquals(0, mGridView.getSelectedPosition());
    }

    @Test
    public void testFocusedPositonAfterRemoved2() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.vertical_linear);
        final int[] items = new int[2];
        for (int i = 0; i < items.length; i++) {
            items[i] = 300;
        }
        intent.putExtra(GridActivity.EXTRA_ITEMS, items);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        initActivity(intent);
        setSelectedPosition(1);
        assertEquals(1, mGridView.getSelectedPosition());

        final int[] newItems = new int[3];
        for (int i = 0; i < newItems.length; i++) {
            newItems[i] = 300;
        }
        performAndWaitForAnimation(new Runnable() {
            @Override
            public void run() {
                mActivity.removeItems(1, 1, true);
                mActivity.addItems(1, newItems, true);
            }
        });
        assertEquals(1, mGridView.getSelectedPosition());
    }

    static void assertNoCollectionItemInfo(AccessibilityNodeInfoCompat info) {
        AccessibilityNodeInfoCompat.CollectionItemInfoCompat nodeInfoCompat =
                info.getCollectionItemInfo();
        if (nodeInfoCompat == null) {
            return;
        }
        assertTrue(nodeInfoCompat.getRowIndex() < 0);
        assertTrue(nodeInfoCompat.getColumnIndex() < 0);
    }

    /**
     * This test would need talkback on.
     */
    @Test
    @AnimationTest
    public void testAccessibilityOfItemsBeingPushedOut() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.horizontal_grid);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 100);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 3;

        initActivity(intent);

        final int lastPos = mGridView.getChildAdapterPosition(
                mGridView.getChildAt(mGridView.getChildCount() - 1));
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.getLayoutManager().setItemPrefetchEnabled(false);
            }
        });
        final int numItemsToPushOut = mNumRows;
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // set longer enough so that accessibility service will initialize node
                // within setImportantForAccessibility().
                mGridView.getItemAnimator().setRemoveDuration(2000);
                mGridView.getItemAnimator().setAddDuration(2000);
                final int[] newItems = new int[numItemsToPushOut];
                final int newItemValue = mActivity.mItemLengths[0];
                for (int i = 0; i < newItems.length; i++) {
                    newItems[i] = newItemValue;
                }
                mActivity.addItems(lastPos - numItemsToPushOut + 1, newItems);
            }
        });
        waitForItemAnimation();
    }

    /**
     * This test simulates talkback by calling setImportanceForAccessibility at end of animation
     */
    @Test
    public void simulatesAccessibilityOfItemsBeingPushedOut() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.horizontal_grid);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 100);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 3;

        initActivity(intent);

        final HashSet<View> moveAnimationViews = new HashSet<>();
        mActivity.mImportantForAccessibilityListener =
                new GridActivity.ImportantForAccessibilityListener() {
            RecyclerView.LayoutManager mLM = mGridView.getLayoutManager();
            @Override
            public void onImportantForAccessibilityChanged(View view, int newValue) {
                // simulates talkack, having setImportantForAccessibility to call
                // onInitializeAccessibilityNodeInfoForItem() for the DISAPPEARING items.
                if (moveAnimationViews.contains(view)) {
                    AccessibilityNodeInfoCompat info = AccessibilityNodeInfoCompat.obtain();
                    mLM.onInitializeAccessibilityNodeInfoForItem(
                            null, null, view, info);
                }
            }
        };
        final int lastPos = mGridView.getChildAdapterPosition(
                mGridView.getChildAt(mGridView.getChildCount() - 1));
        final int numItemsToPushOut = mNumRows;
        for (int i = 0; i < numItemsToPushOut; i++) {
            moveAnimationViews.add(
                    mGridView.getChildAt(mGridView.getChildCount() - 1 - i));
        }
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setItemAnimator(new DefaultItemAnimator() {
                    @Override
                    public void onMoveFinished(RecyclerView.ViewHolder item) {
                        moveAnimationViews.remove(item.itemView);
                    }
                });
                mGridView.getLayoutManager().setItemPrefetchEnabled(false);
            }
        });
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final int[] newItems = new int[numItemsToPushOut];
                final int newItemValue = mActivity.mItemLengths[0] + 1;
                for (int i = 0; i < newItems.length; i++) {
                    newItems[i] = newItemValue;
                }
                mActivity.addItems(lastPos - numItemsToPushOut + 1, newItems);
            }
        });
        while (moveAnimationViews.size() != 0) {
            Thread.sleep(100);
        }
    }

    @Test
    @AnimationTest
    public void testAccessibilityNodeInfoOnRemovedFirstItem() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.horizontal_grid);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 6);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 3;

        initActivity(intent);

        final View lastView = mGridView.findViewHolderForAdapterPosition(0).itemView;
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.getItemAnimator().setRemoveDuration(20000);
                mActivity.removeItems(0, 1);
            }
        });
        waitForItemAnimationStart();
        AccessibilityNodeInfoCompat info = AccessibilityNodeInfoCompat.obtain(lastView);
        mGridView.getLayoutManager().onInitializeAccessibilityNodeInfoForItem(null, null,
                lastView, info);
        assertNoCollectionItemInfo(info);
    }

    @Test
    @AnimationTest
    public void testAccessibilityNodeInfoOnRemovedLastItem() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.horizontal_grid);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 6);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 3;

        initActivity(intent);

        final View lastView = mGridView.findViewHolderForAdapterPosition(5).itemView;
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.getItemAnimator().setRemoveDuration(20000);
                mActivity.removeItems(5, 1);
            }
        });
        waitForItemAnimationStart();
        AccessibilityNodeInfoCompat info = AccessibilityNodeInfoCompat.obtain(lastView);
        mGridView.getLayoutManager().onInitializeAccessibilityNodeInfoForItem(null, null,
                lastView, info);
        assertNoCollectionItemInfo(info);
    }

    static class FiveViewTypesProvider implements ViewTypeProvider {

        @Override
        public int getViewType(int position) {
            switch (position) {
                case 0:
                    return 0;
                case 1:
                    return 1;
                case 2:
                    return 2;
                case 3:
                    return 3;
                case 4:
                    return 4;
            }
            return 199;
        }
    }

    // Used by testItemAlignmentVertical() testItemAlignmentHorizontal()
    static class ItemAlignmentWithPaddingFacetProvider implements
            ItemAlignmentFacetProvider {
        final ItemAlignmentFacet mFacet0;
        final ItemAlignmentFacet mFacet1;
        final ItemAlignmentFacet mFacet2;
        final ItemAlignmentFacet mFacet3;
        final ItemAlignmentFacet mFacet4;

        ItemAlignmentWithPaddingFacetProvider() {
            ItemAlignmentFacet.ItemAlignmentDef[] defs;
            mFacet0 = new ItemAlignmentFacet();
            defs = new ItemAlignmentFacet.ItemAlignmentDef[1];
            defs[0] = new ItemAlignmentFacet.ItemAlignmentDef();
            defs[0].setItemAlignmentViewId(R.id.t1);
            defs[0].setItemAlignmentOffsetPercent(0);
            defs[0].setItemAlignmentOffsetWithPadding(false);
            mFacet0.setAlignmentDefs(defs);
            mFacet1 = new ItemAlignmentFacet();
            defs = new ItemAlignmentFacet.ItemAlignmentDef[1];
            defs[0] = new ItemAlignmentFacet.ItemAlignmentDef();
            defs[0].setItemAlignmentViewId(R.id.t1);
            defs[0].setItemAlignmentOffsetPercent(0);
            defs[0].setItemAlignmentOffsetWithPadding(true);
            mFacet1.setAlignmentDefs(defs);
            mFacet2 = new ItemAlignmentFacet();
            defs = new ItemAlignmentFacet.ItemAlignmentDef[1];
            defs[0] = new ItemAlignmentFacet.ItemAlignmentDef();
            defs[0].setItemAlignmentViewId(R.id.t2);
            defs[0].setItemAlignmentOffsetPercent(100);
            defs[0].setItemAlignmentOffsetWithPadding(true);
            mFacet2.setAlignmentDefs(defs);
            mFacet3 = new ItemAlignmentFacet();
            defs = new ItemAlignmentFacet.ItemAlignmentDef[1];
            defs[0] = new ItemAlignmentFacet.ItemAlignmentDef();
            defs[0].setItemAlignmentViewId(R.id.t2);
            defs[0].setItemAlignmentOffsetPercent(50);
            defs[0].setItemAlignmentOffsetWithPadding(true);
            mFacet3.setAlignmentDefs(defs);
            mFacet4 = new ItemAlignmentFacet();
            defs = new ItemAlignmentFacet.ItemAlignmentDef[1];
            defs[0] = new ItemAlignmentFacet.ItemAlignmentDef();
            defs[0].setItemAlignmentViewId(R.id.t2);
            defs[0].setItemAlignmentOffsetPercent(50);
            defs[0].setItemAlignmentOffsetWithPadding(false);
            mFacet4.setAlignmentDefs(defs);
        }

        @Override
        public ItemAlignmentFacet getItemAlignmentFacet(int viewType) {
            switch (viewType) {
                case 0:
                    return mFacet0;
                case 1:
                    return mFacet1;
                case 2:
                    return mFacet2;
                case 3:
                    return mFacet3;
                case 4:
                    return mFacet4;
            }
            return null;
        }
    }

    @Test
    public void testItemAlignmentVertical() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.vertical_linear);
        intent.putExtra(GridActivity.EXTRA_CHILD_LAYOUT_ID, R.layout.relative_layout2);
        int[] items = new int[5];
        for (int i = 0; i < items.length; i++) {
            items[i] = 300;
        }
        intent.putExtra(GridActivity.EXTRA_ITEMS, items);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        intent.putExtra(GridActivity.EXTRA_VIEWTYPEPROVIDER_CLASS,
                FiveViewTypesProvider.class.getName());
        intent.putExtra(GridActivity.EXTRA_ITEMALIGNMENTPROVIDER_CLASS,
                ItemAlignmentWithPaddingFacetProvider.class.getName());
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        initActivity(intent);
        startWaitLayout();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setWindowAlignment(BaseGridView.WINDOW_ALIGN_NO_EDGE);
                mGridView.setWindowAlignmentOffsetPercent(50);
                mGridView.setWindowAlignmentOffset(0);
            }
        });
        waitForLayout();

        final float windowAlignCenter = mGridView.getHeight() / 2f;
        Rect rect = new Rect();
        View textView;

        // test 1: does not include padding
        textView = mGridView.findViewHolderForAdapterPosition(0).itemView.findViewById(R.id.t1);
        rect.set(0, 0, textView.getWidth(), textView.getHeight());
        mGridView.offsetDescendantRectToMyCoords(textView, rect);
        assertEquals(windowAlignCenter, rect.top, DELTA);

        // test 2: including low padding
        setSelectedPosition(1);
        textView = mGridView.findViewHolderForAdapterPosition(1).itemView.findViewById(R.id.t1);
        assertTrue(textView.getPaddingTop() > 0);
        rect.set(0, textView.getPaddingTop(), textView.getWidth(), textView.getHeight());
        mGridView.offsetDescendantRectToMyCoords(textView, rect);
        assertEquals(windowAlignCenter, rect.top, DELTA);

        // test 3: including high padding
        setSelectedPosition(2);
        textView = mGridView.findViewHolderForAdapterPosition(2).itemView.findViewById(R.id.t2);
        assertTrue(textView.getPaddingBottom() > 0);
        rect.set(0, 0, textView.getWidth(),
                textView.getHeight() - textView.getPaddingBottom());
        mGridView.offsetDescendantRectToMyCoords(textView, rect);
        assertEquals(windowAlignCenter, rect.bottom, DELTA);

        // test 4: including padding will be ignored if offsetPercent is not 0 or 100
        setSelectedPosition(3);
        textView = mGridView.findViewHolderForAdapterPosition(3).itemView.findViewById(R.id.t2);
        assertTrue(textView.getPaddingTop() != textView.getPaddingBottom());
        rect.set(0, 0, textView.getWidth(), textView.getHeight() / 2);
        mGridView.offsetDescendantRectToMyCoords(textView, rect);
        assertEquals(windowAlignCenter, rect.bottom, DELTA);

        // test 5: does not include padding
        setSelectedPosition(4);
        textView = mGridView.findViewHolderForAdapterPosition(4).itemView.findViewById(R.id.t2);
        assertTrue(textView.getPaddingTop() != textView.getPaddingBottom());
        rect.set(0, 0, textView.getWidth(), textView.getHeight() / 2);
        mGridView.offsetDescendantRectToMyCoords(textView, rect);
        assertEquals(windowAlignCenter, rect.bottom, DELTA);
    }

    @Test
    public void testItemAlignmentHorizontal() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.horizontal_linear);
        intent.putExtra(GridActivity.EXTRA_CHILD_LAYOUT_ID, R.layout.relative_layout3);
        int[] items = new int[5];
        for (int i = 0; i < items.length; i++) {
            items[i] = 300;
        }
        intent.putExtra(GridActivity.EXTRA_ITEMS, items);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        intent.putExtra(GridActivity.EXTRA_VIEWTYPEPROVIDER_CLASS,
                FiveViewTypesProvider.class.getName());
        intent.putExtra(GridActivity.EXTRA_ITEMALIGNMENTPROVIDER_CLASS,
                ItemAlignmentWithPaddingFacetProvider.class.getName());
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        initActivity(intent);
        startWaitLayout();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setWindowAlignment(BaseGridView.WINDOW_ALIGN_NO_EDGE);
                mGridView.setWindowAlignmentOffsetPercent(50);
                mGridView.setWindowAlignmentOffset(0);
            }
        });
        waitForLayout();

        final float windowAlignCenter = mGridView.getWidth() / 2f;
        Rect rect = new Rect();
        View textView;

        // test 1: does not include padding
        textView = mGridView.findViewHolderForAdapterPosition(0).itemView.findViewById(R.id.t1);
        rect.set(0, 0, textView.getWidth(), textView.getHeight());
        mGridView.offsetDescendantRectToMyCoords(textView, rect);
        assertEquals(windowAlignCenter, rect.left, DELTA);

        // test 2: including low padding
        setSelectedPosition(1);
        textView = mGridView.findViewHolderForAdapterPosition(1).itemView.findViewById(R.id.t1);
        assertTrue(textView.getPaddingLeft() > 0);
        rect.set(textView.getPaddingLeft(), 0, textView.getWidth(), textView.getHeight());
        mGridView.offsetDescendantRectToMyCoords(textView, rect);
        assertEquals(windowAlignCenter, rect.left, DELTA);

        // test 3: including high padding
        setSelectedPosition(2);
        textView = mGridView.findViewHolderForAdapterPosition(2).itemView.findViewById(R.id.t2);
        assertTrue(textView.getPaddingRight() > 0);
        rect.set(0, 0, textView.getWidth() - textView.getPaddingRight(),
                textView.getHeight());
        mGridView.offsetDescendantRectToMyCoords(textView, rect);
        assertEquals(windowAlignCenter, rect.right, DELTA);

        // test 4: including padding will be ignored if offsetPercent is not 0 or 100
        setSelectedPosition(3);
        textView = mGridView.findViewHolderForAdapterPosition(3).itemView.findViewById(R.id.t2);
        assertTrue(textView.getPaddingLeft() != textView.getPaddingRight());
        rect.set(0, 0, textView.getWidth() / 2, textView.getHeight());
        mGridView.offsetDescendantRectToMyCoords(textView, rect);
        assertEquals(windowAlignCenter, rect.right, DELTA);

        // test 5: does not include padding
        setSelectedPosition(4);
        textView = mGridView.findViewHolderForAdapterPosition(4).itemView.findViewById(R.id.t2);
        assertTrue(textView.getPaddingLeft() != textView.getPaddingRight());
        rect.set(0, 0, textView.getWidth() / 2, textView.getHeight());
        mGridView.offsetDescendantRectToMyCoords(textView, rect);
        assertEquals(windowAlignCenter, rect.right, DELTA);
    }

    @Test
    public void testItemAlignmentHorizontalRtl() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.horizontal_linear);
        intent.putExtra(GridActivity.EXTRA_CHILD_LAYOUT_ID, R.layout.relative_layout3);
        int[] items = new int[5];
        for (int i = 0; i < items.length; i++) {
            items[i] = 300;
        }
        intent.putExtra(GridActivity.EXTRA_ITEMS, items);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        intent.putExtra(GridActivity.EXTRA_VIEWTYPEPROVIDER_CLASS,
                FiveViewTypesProvider.class.getName());
        intent.putExtra(GridActivity.EXTRA_ITEMALIGNMENTPROVIDER_CLASS,
                ItemAlignmentWithPaddingFacetProvider.class.getName());
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 1;

        initActivity(intent);
        startWaitLayout();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGridView.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                mGridView.setWindowAlignment(BaseGridView.WINDOW_ALIGN_NO_EDGE);
                mGridView.setWindowAlignmentOffsetPercent(50);
                mGridView.setWindowAlignmentOffset(0);
            }
        });
        waitForLayout();

        final float windowAlignCenter = mGridView.getWidth() / 2f;
        Rect rect = new Rect();
        View textView;

        // test 1: does not include padding
        textView = mGridView.findViewHolderForAdapterPosition(0).itemView.findViewById(R.id.t1);
        rect.set(0, 0, textView.getWidth(), textView.getHeight());
        mGridView.offsetDescendantRectToMyCoords(textView, rect);
        assertEquals(windowAlignCenter, rect.right, DELTA);

        // test 2: including low padding
        setSelectedPosition(1);
        textView = mGridView.findViewHolderForAdapterPosition(1).itemView.findViewById(R.id.t1);
        assertTrue(textView.getPaddingRight() > 0);
        rect.set(0, 0, textView.getWidth() - textView.getPaddingRight(),
                textView.getHeight());
        mGridView.offsetDescendantRectToMyCoords(textView, rect);
        assertEquals(windowAlignCenter, rect.right, DELTA);

        // test 3: including high padding
        setSelectedPosition(2);
        textView = mGridView.findViewHolderForAdapterPosition(2).itemView.findViewById(R.id.t2);
        assertTrue(textView.getPaddingLeft() > 0);
        rect.set(textView.getPaddingLeft(), 0, textView.getWidth(),
                textView.getHeight());
        mGridView.offsetDescendantRectToMyCoords(textView, rect);
        assertEquals(windowAlignCenter, rect.left, DELTA);

        // test 4: including padding will be ignored if offsetPercent is not 0 or 100
        setSelectedPosition(3);
        textView = mGridView.findViewHolderForAdapterPosition(3).itemView.findViewById(R.id.t2);
        assertTrue(textView.getPaddingLeft() != textView.getPaddingRight());
        rect.set(0, 0, textView.getWidth() / 2, textView.getHeight());
        mGridView.offsetDescendantRectToMyCoords(textView, rect);
        assertEquals(windowAlignCenter, rect.right, DELTA);

        // test 5: does not include padding
        setSelectedPosition(4);
        textView = mGridView.findViewHolderForAdapterPosition(4).itemView.findViewById(R.id.t2);
        assertTrue(textView.getPaddingLeft() != textView.getPaddingRight());
        rect.set(0, 0, textView.getWidth() / 2, textView.getHeight());
        mGridView.offsetDescendantRectToMyCoords(textView, rect);
        assertEquals(windowAlignCenter, rect.right, DELTA);
    }

    enum ItemLocation {
        ITEM_AT_LOW,
        ITEM_AT_KEY_LINE,
        ITEM_AT_HIGH
    };

    static class ItemAt {
        final int mScrollPosition;
        final int mPosition;
        final ItemLocation mLocation;

        ItemAt(int scrollPosition, int position, ItemLocation loc) {
            mScrollPosition = scrollPosition;
            mPosition = position;
            mLocation = loc;
        }

        ItemAt(int position, ItemLocation loc) {
            mScrollPosition = position;
            mPosition = position;
            mLocation = loc;
        }
    }

    /**
     * When scroll to position, item at position is expected at given location.
     */
    static ItemAt itemAt(int position, ItemLocation location) {
        return new ItemAt(position, location);
    }

    /**
     * When scroll to scrollPosition, item at position is expected at given location.
     */
    static ItemAt itemAt(int scrollPosition, int position, ItemLocation location) {
        return new ItemAt(scrollPosition, position, location);
    }

    void prepareKeyLineTest(int numItems) throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.horizontal_linear);
        int[] items = new int[numItems];
        for (int i = 0; i < items.length; i++) {
            items[i] = 32;
        }
        intent.putExtra(GridActivity.EXTRA_ITEMS, items);
        intent.putExtra(GridActivity.EXTRA_STAGGERED, false);
        mOrientation = BaseGridView.HORIZONTAL;
        mNumRows = 1;

        initActivity(intent);
    }

    public void testPreferKeyLine(final int windowAlignment,
            final boolean preferKeyLineOverLow,
            final boolean preferKeyLineOverHigh,
            ItemLocation assertFirstItemLocation,
            ItemLocation assertLastItemLocation) throws Throwable {
        testPreferKeyLine(windowAlignment, preferKeyLineOverLow, preferKeyLineOverHigh,
                itemAt(0, assertFirstItemLocation),
                itemAt(mActivity.mNumItems - 1, assertLastItemLocation));
    }

    public void testPreferKeyLine(final int windowAlignment,
            final boolean preferKeyLineOverLow,
            final boolean preferKeyLineOverHigh,
            ItemLocation assertFirstItemLocation,
            ItemAt assertLastItemLocation) throws Throwable {
        testPreferKeyLine(windowAlignment, preferKeyLineOverLow, preferKeyLineOverHigh,
                itemAt(0, assertFirstItemLocation),
                assertLastItemLocation);
    }

    public void testPreferKeyLine(final int windowAlignment,
            final boolean preferKeyLineOverLow,
            final boolean preferKeyLineOverHigh,
            ItemAt assertFirstItemLocation,
            ItemLocation assertLastItemLocation) throws Throwable {
        testPreferKeyLine(windowAlignment, preferKeyLineOverLow, preferKeyLineOverHigh,
                assertFirstItemLocation,
                itemAt(mActivity.mNumItems - 1, assertLastItemLocation));
    }

    public void testPreferKeyLine(final int windowAlignment,
            final boolean preferKeyLineOverLow,
            final boolean preferKeyLineOverHigh,
            ItemAt assertFirstItemLocation,
            ItemAt assertLastItemLocation) throws Throwable {
        TestPreferKeyLineOptions options = new TestPreferKeyLineOptions();
        options.mAssertItemLocations = new ItemAt[] {assertFirstItemLocation,
                assertLastItemLocation};
        options.mPreferKeyLineOverLow = preferKeyLineOverLow;
        options.mPreferKeyLineOverHigh = preferKeyLineOverHigh;
        options.mWindowAlignment = windowAlignment;

        options.mRtl = false;
        testPreferKeyLine(options);

        options.mRtl = true;
        testPreferKeyLine(options);
    }

    static class TestPreferKeyLineOptions {
        int mWindowAlignment;
        boolean mPreferKeyLineOverLow;
        boolean mPreferKeyLineOverHigh;
        ItemAt[] mAssertItemLocations;
        boolean mRtl;
    }

    public void testPreferKeyLine(final TestPreferKeyLineOptions options) throws Throwable {
        startWaitLayout();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (options.mRtl) {
                    mGridView.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                } else {
                    mGridView.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
                }
                mGridView.setWindowAlignment(options.mWindowAlignment);
                mGridView.setWindowAlignmentOffsetPercent(50);
                mGridView.setWindowAlignmentOffset(0);
                mGridView.setWindowAlignmentPreferKeyLineOverLowEdge(options.mPreferKeyLineOverLow);
                mGridView.setWindowAlignmentPreferKeyLineOverHighEdge(
                        options.mPreferKeyLineOverHigh);
            }
        });
        waitForLayout();

        final int paddingStart = mGridView.getPaddingStart();
        final int paddingEnd = mGridView.getPaddingEnd();
        final int windowAlignCenter = mGridView.getWidth() / 2;

        for (int i = 0; i < options.mAssertItemLocations.length; i++) {
            ItemAt assertItemLocation = options.mAssertItemLocations[i];
            setSelectedPosition(assertItemLocation.mScrollPosition);
            View view = mGridView.findViewHolderForAdapterPosition(assertItemLocation.mPosition)
                    .itemView;
            switch (assertItemLocation.mLocation) {
                case ITEM_AT_LOW:
                    if (options.mRtl) {
                        assertEquals(mGridView.getWidth() - paddingStart, view.getRight());
                    } else {
                        assertEquals(paddingStart, view.getLeft());
                    }
                    break;
                case ITEM_AT_HIGH:
                    if (options.mRtl) {
                        assertEquals(paddingEnd, view.getLeft());
                    } else {
                        assertEquals(mGridView.getWidth() - paddingEnd, view.getRight());
                    }
                    break;
                case ITEM_AT_KEY_LINE:
                    assertEquals(windowAlignCenter, (view.getLeft() + view.getRight()) / 2, DELTA);
                    break;
            }
        }
    }

    @Test
    public void testPreferKeyLine1() throws Throwable {
        prepareKeyLineTest(1);
        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_NO_EDGE, false, false,
                ItemLocation.ITEM_AT_KEY_LINE, ItemLocation.ITEM_AT_KEY_LINE);
        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_NO_EDGE, false, true,
                ItemLocation.ITEM_AT_KEY_LINE, ItemLocation.ITEM_AT_KEY_LINE);
        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_NO_EDGE, true, false,
                ItemLocation.ITEM_AT_KEY_LINE, ItemLocation.ITEM_AT_KEY_LINE);
        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_NO_EDGE, true, true,
                ItemLocation.ITEM_AT_KEY_LINE, ItemLocation.ITEM_AT_KEY_LINE);

        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_LOW_EDGE, false, false,
                ItemLocation.ITEM_AT_LOW, ItemLocation.ITEM_AT_LOW);
        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_LOW_EDGE, false, true,
                ItemLocation.ITEM_AT_LOW, ItemLocation.ITEM_AT_LOW);
        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_LOW_EDGE, true, false,
                ItemLocation.ITEM_AT_KEY_LINE, ItemLocation.ITEM_AT_KEY_LINE);
        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_LOW_EDGE, true, true,
                ItemLocation.ITEM_AT_KEY_LINE, ItemLocation.ITEM_AT_KEY_LINE);

        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_HIGH_EDGE, false, false,
                ItemLocation.ITEM_AT_HIGH, ItemLocation.ITEM_AT_HIGH);
        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_HIGH_EDGE, false, true,
                ItemLocation.ITEM_AT_KEY_LINE, ItemLocation.ITEM_AT_KEY_LINE);
        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_HIGH_EDGE, true, false,
                ItemLocation.ITEM_AT_HIGH, ItemLocation.ITEM_AT_HIGH);
        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_HIGH_EDGE, true, true,
                ItemLocation.ITEM_AT_KEY_LINE, ItemLocation.ITEM_AT_KEY_LINE);

        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_BOTH_EDGE, false, false,
                ItemLocation.ITEM_AT_LOW, ItemLocation.ITEM_AT_LOW);
        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_BOTH_EDGE, false, true,
                ItemLocation.ITEM_AT_LOW, ItemLocation.ITEM_AT_LOW);
        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_BOTH_EDGE, true, false,
                ItemLocation.ITEM_AT_KEY_LINE, ItemLocation.ITEM_AT_KEY_LINE);
        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_BOTH_EDGE, true, true,
                ItemLocation.ITEM_AT_KEY_LINE, ItemLocation.ITEM_AT_KEY_LINE);
    }

    @Test
    public void testPreferKeyLine2() throws Throwable {
        prepareKeyLineTest(2);
        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_NO_EDGE, false, false,
                ItemLocation.ITEM_AT_KEY_LINE, ItemLocation.ITEM_AT_KEY_LINE);
        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_NO_EDGE, false, true,
                ItemLocation.ITEM_AT_KEY_LINE, ItemLocation.ITEM_AT_KEY_LINE);
        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_NO_EDGE, true, false,
                ItemLocation.ITEM_AT_KEY_LINE, ItemLocation.ITEM_AT_KEY_LINE);
        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_NO_EDGE, true, true,
                ItemLocation.ITEM_AT_KEY_LINE, ItemLocation.ITEM_AT_KEY_LINE);

        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_LOW_EDGE, false, false,
                ItemLocation.ITEM_AT_LOW, itemAt(1, 0, ItemLocation.ITEM_AT_LOW));
        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_LOW_EDGE, false, true,
                ItemLocation.ITEM_AT_LOW, itemAt(1, 0, ItemLocation.ITEM_AT_LOW));
        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_LOW_EDGE, true, false,
                itemAt(0, 1, ItemLocation.ITEM_AT_KEY_LINE),
                itemAt(1, 1, ItemLocation.ITEM_AT_KEY_LINE));
        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_LOW_EDGE, true, true,
                itemAt(0, 1, ItemLocation.ITEM_AT_KEY_LINE),
                itemAt(1, 1, ItemLocation.ITEM_AT_KEY_LINE));

        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_HIGH_EDGE, false, false,
                itemAt(0, 1, ItemLocation.ITEM_AT_HIGH),
                itemAt(1, 1, ItemLocation.ITEM_AT_HIGH));
        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_HIGH_EDGE, false, true,
                itemAt(0, 0, ItemLocation.ITEM_AT_KEY_LINE),
                itemAt(1, 0, ItemLocation.ITEM_AT_KEY_LINE));
        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_HIGH_EDGE, true, false,
                itemAt(0, 1, ItemLocation.ITEM_AT_HIGH),
                itemAt(1, 1, ItemLocation.ITEM_AT_HIGH));
        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_HIGH_EDGE, true, true,
                itemAt(0, 0, ItemLocation.ITEM_AT_KEY_LINE),
                itemAt(1, 0, ItemLocation.ITEM_AT_KEY_LINE));

        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_BOTH_EDGE, false, false,
                ItemLocation.ITEM_AT_LOW, itemAt(1, 0, ItemLocation.ITEM_AT_LOW));
        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_BOTH_EDGE, false, true,
                ItemLocation.ITEM_AT_LOW, itemAt(1, 0, ItemLocation.ITEM_AT_LOW));
        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_BOTH_EDGE, true, false,
                itemAt(0, 1, ItemLocation.ITEM_AT_KEY_LINE),
                itemAt(1, 1, ItemLocation.ITEM_AT_KEY_LINE));
        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_BOTH_EDGE, true, true,
                itemAt(0, 1, ItemLocation.ITEM_AT_KEY_LINE),
                itemAt(1, 1, ItemLocation.ITEM_AT_KEY_LINE));
    }

    @Test
    public void testPreferKeyLine10000() throws Throwable {
        prepareKeyLineTest(10000);
        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_NO_EDGE, false, false,
                ItemLocation.ITEM_AT_KEY_LINE, ItemLocation.ITEM_AT_KEY_LINE);
        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_NO_EDGE, false, true,
                ItemLocation.ITEM_AT_KEY_LINE, ItemLocation.ITEM_AT_KEY_LINE);
        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_NO_EDGE, true, false,
                ItemLocation.ITEM_AT_KEY_LINE, ItemLocation.ITEM_AT_KEY_LINE);
        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_NO_EDGE, true, true,
                ItemLocation.ITEM_AT_KEY_LINE, ItemLocation.ITEM_AT_KEY_LINE);

        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_LOW_EDGE, false, false,
                ItemLocation.ITEM_AT_LOW, ItemLocation.ITEM_AT_KEY_LINE);
        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_LOW_EDGE, false, true,
                ItemLocation.ITEM_AT_LOW, ItemLocation.ITEM_AT_KEY_LINE);
        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_LOW_EDGE, true, false,
                ItemLocation.ITEM_AT_LOW, ItemLocation.ITEM_AT_KEY_LINE);
        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_LOW_EDGE, true, true,
                ItemLocation.ITEM_AT_LOW, ItemLocation.ITEM_AT_KEY_LINE);

        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_HIGH_EDGE, false, false,
                ItemLocation.ITEM_AT_KEY_LINE, ItemLocation.ITEM_AT_HIGH);
        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_HIGH_EDGE, false, true,
                ItemLocation.ITEM_AT_KEY_LINE, ItemLocation.ITEM_AT_HIGH);
        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_HIGH_EDGE, true, false,
                ItemLocation.ITEM_AT_KEY_LINE, ItemLocation.ITEM_AT_HIGH);
        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_HIGH_EDGE, true, true,
                ItemLocation.ITEM_AT_KEY_LINE, ItemLocation.ITEM_AT_HIGH);

        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_BOTH_EDGE, false, false,
                ItemLocation.ITEM_AT_LOW, ItemLocation.ITEM_AT_HIGH);
        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_BOTH_EDGE, false, true,
                ItemLocation.ITEM_AT_LOW, ItemLocation.ITEM_AT_HIGH);
        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_BOTH_EDGE, true, false,
                ItemLocation.ITEM_AT_LOW, ItemLocation.ITEM_AT_HIGH);
        testPreferKeyLine(VerticalGridView.WINDOW_ALIGN_BOTH_EDGE, true, true,
                ItemLocation.ITEM_AT_LOW, ItemLocation.ITEM_AT_HIGH);
    }

    @Test
    @FlakyTest(bugId = 213630586)
    public void testConcat() throws Throwable {

        Intent intent = new Intent();
        intent.putExtra(GridActivity.EXTRA_LAYOUT_RESOURCE_ID, R.layout.vertical_grid);
        intent.putExtra(GridActivity.EXTRA_NUM_ITEMS, 80);
        intent.putExtra(GridActivity.EXTRA_CONCAT_ADAPTER, true);
        initActivity(intent);
        mOrientation = BaseGridView.VERTICAL;
        mNumRows = 3;

        scrollToEnd(mVerifyLayout);

        scrollToBegin(mVerifyLayout);

        verifyBeginAligned();
    }

}
