/*
 * Copyright (C) 2014 The Android Open Source Project
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


package android.support.v7.widget;


import android.graphics.Rect;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.AccessibilityDelegateCompat;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.support.v4.view.accessibility.AccessibilityRecordCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static android.support.v7.widget.LayoutState.*;
import static android.support.v7.widget.LinearLayoutManager.VERTICAL;
import static android.support.v7.widget.StaggeredGridLayoutManager.*;

public class StaggeredGridLayoutManagerTest extends BaseRecyclerViewInstrumentationTest {

    private static final boolean DEBUG = false;

    private static final String TAG = "StaggeredGridLayoutManagerTest";

    volatile WrappedLayoutManager mLayoutManager;

    GridTestAdapter mAdapter;

    final List<Config> mBaseVariations = new ArrayList<Config>();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        for (int orientation : new int[]{VERTICAL, HORIZONTAL}) {
            for (boolean reverseLayout : new boolean[]{false, true}) {
                for (int spanCount : new int[]{1, 3}) {
                    for (int gapStrategy : new int[]{GAP_HANDLING_NONE,
                            GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS}) {
                        mBaseVariations.add(new Config(orientation, reverseLayout, spanCount,
                                gapStrategy));
                    }
                }
            }
        }
    }

    void setupByConfig(Config config) throws Throwable {
        mAdapter = new GridTestAdapter(config.mItemCount, config.mOrientation);
        mRecyclerView = new RecyclerView(getActivity());
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new WrappedLayoutManager(config.mSpanCount,
                config.mOrientation);
        mLayoutManager.setGapStrategy(config.mGapStrategy);
        mLayoutManager.setReverseLayout(config.mReverseLayout);
        mRecyclerView.setLayoutManager(mLayoutManager);
    }

    public void testRTL() throws Throwable {
        for (boolean changeRtlAfter : new boolean[]{false, true}) {
            for (Config config : mBaseVariations) {
                rtlTest(config, changeRtlAfter);
                removeRecyclerView();
            }
        }
    }

    void rtlTest(Config config, boolean changeRtlAfter) throws Throwable {
        if (config.mSpanCount == 1) {
            config.mSpanCount = 2;
        }
        String logPrefix = config + ", changeRtlAfterLayout:" + changeRtlAfter;
        setupByConfig(config.itemCount(5));
        if (changeRtlAfter) {
            waitFirstLayout();
            mLayoutManager.expectLayouts(1);
            mLayoutManager.setFakeRtl(true);
            mLayoutManager.waitForLayout(2);
        } else {
            mLayoutManager.mFakeRTL = true;
            waitFirstLayout();
        }

        assertEquals("view should become rtl", true, mLayoutManager.isLayoutRTL());
        OrientationHelper helper = OrientationHelper.createHorizontalHelper(mLayoutManager);
        View child0 = mLayoutManager.findViewByPosition(0);
        View child1 = mLayoutManager.findViewByPosition(config.mOrientation == VERTICAL ? 1
            : config.mSpanCount);
        assertNotNull(logPrefix + " child position 0 should be laid out", child0);
        assertNotNull(logPrefix + " child position 0 should be laid out", child1);
        if (config.mOrientation == VERTICAL || !config.mReverseLayout) {
            assertTrue(logPrefix + " second child should be to the left of first child",
                    helper.getDecoratedStart(child0) >= helper.getDecoratedEnd(child1));
            assertEquals(logPrefix + " first child should be right aligned",
                    helper.getDecoratedEnd(child0), helper.getEndAfterPadding());
        } else {
            assertTrue(logPrefix + " first child should be to the left of second child",
                    helper.getDecoratedStart(child1) >= helper.getDecoratedEnd(child0));
            assertEquals(logPrefix + " first child should be left aligned",
                    helper.getDecoratedStart(child0), helper.getStartAfterPadding());
        }
        checkForMainThreadException();
    }

    public void testScrollBackAndPreservePositions() throws Throwable {
        for (boolean saveRestore : new boolean[]{false, true}) {
            for (Config config : mBaseVariations) {
                scrollBackAndPreservePositionsTest(config, saveRestore);
                removeRecyclerView();
            }
        }
    }

    public void scrollBackAndPreservePositionsTest(final Config config, final boolean saveRestoreInBetween)
            throws Throwable {
        setupByConfig(config);
        mAdapter.mOnBindHandler = new OnBindHandler() {
            @Override
            public void onBoundItem(TestViewHolder vh, int postion) {
                LayoutParams lp = (LayoutParams) vh.itemView.getLayoutParams();
                lp.setFullSpan((postion * 7) % (config.mSpanCount + 1) == 0);
            }
        };
        waitFirstLayout();
        final int[] globalPositions = new int[mAdapter.getItemCount()];
        Arrays.fill(globalPositions, Integer.MIN_VALUE);
        final int scrollStep = (mLayoutManager.mPrimaryOrientation.getTotalSpace() / 10)
                * (config.mReverseLayout ? -1 : 1);


        final int[] globalPos = new int[1];
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                int globalScrollPosition = 0;
                while (globalPositions[mAdapter.getItemCount() - 1] == Integer.MIN_VALUE) {
                    for (int i = 0; i < mRecyclerView.getChildCount(); i++) {
                        View child = mRecyclerView.getChildAt(i);
                        final int pos = mRecyclerView.getChildPosition(child);
                        if (globalPositions[pos] != Integer.MIN_VALUE) {
                            continue;
                        }
                        if (config.mReverseLayout) {
                            globalPositions[pos] = globalScrollPosition +
                                    mLayoutManager.mPrimaryOrientation.getDecoratedEnd(child);
                        } else {
                            globalPositions[pos] = globalScrollPosition +
                                    mLayoutManager.mPrimaryOrientation.getDecoratedStart(child);
                        }
                    }
                    globalScrollPosition += mLayoutManager.scrollBy(scrollStep,
                            mRecyclerView.mRecycler, mRecyclerView.mState);
                }
                if (DEBUG) {
                    Log.d(TAG, "done recording positions " + Arrays.toString(globalPositions));
                }
                globalPos[0] = globalScrollPosition;
            }
        });
        checkForMainThreadException();

        if (saveRestoreInBetween) {
            saveRestore(config);
        }

        checkForMainThreadException();
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                int globalScrollPosition = globalPos[0];
                // now scroll back and make sure global positions match
                BitSet shouldTest = new BitSet(mAdapter.getItemCount());
                shouldTest.set(0, mAdapter.getItemCount() - 1, true);
                String assertPrefix = config + ", restored in between:" + saveRestoreInBetween
                        + " global pos must match when scrolling in reverse for position ";
                int scrollAmount = Integer.MAX_VALUE;
                while (!shouldTest.isEmpty() && scrollAmount != 0) {
                    for (int i = 0; i < mRecyclerView.getChildCount(); i++) {
                        View child = mRecyclerView.getChildAt(i);
                        int pos = mRecyclerView.getChildPosition(child);
                        if (!shouldTest.get(pos)) {
                            continue;
                        }
                        shouldTest.clear(pos);
                        int globalPos;
                        if (config.mReverseLayout) {
                            globalPos = globalScrollPosition +
                                    mLayoutManager.mPrimaryOrientation.getDecoratedEnd(child);
                        } else {
                            globalPos = globalScrollPosition +
                                    mLayoutManager.mPrimaryOrientation.getDecoratedStart(child);
                        }
                        assertEquals(assertPrefix + pos,
                                globalPositions[pos], globalPos);
                    }
                    scrollAmount = mLayoutManager.scrollBy(-scrollStep,
                            mRecyclerView.mRecycler, mRecyclerView.mState);
                    globalScrollPosition += scrollAmount;
                }
                assertTrue("all views should be seen", shouldTest.isEmpty());
            }
        });
        checkForMainThreadException();
    }

    public void testScrollToPositionWithPredictive() throws Throwable {
        scrollToPositionWithPredictive(0, LinearLayoutManager.INVALID_OFFSET);
        removeRecyclerView();
        scrollToPositionWithPredictive(Config.DEFAULT_ITEM_COUNT / 2,
                LinearLayoutManager.INVALID_OFFSET);
        removeRecyclerView();
        scrollToPositionWithPredictive(9, 20);
        removeRecyclerView();
        scrollToPositionWithPredictive(Config.DEFAULT_ITEM_COUNT / 2, 10);

    }

    public void scrollToPositionWithPredictive(final int scrollPosition, final int scrollOffset)
            throws Throwable {
        setupByConfig(new Config(StaggeredGridLayoutManager.VERTICAL,
                false, 3, StaggeredGridLayoutManager.GAP_HANDLING_NONE));
        waitFirstLayout();
        mLayoutManager.mOnLayoutListener = new OnLayoutListener() {
            @Override
            void after(RecyclerView.Recycler recycler, RecyclerView.State state) {
                RecyclerView rv = mLayoutManager.mRecyclerView;
                if (state.isPreLayout()) {
                    assertEquals("pending scroll position should still be pending",
                            scrollPosition, mLayoutManager.mPendingScrollPosition);
                    if (scrollOffset != LinearLayoutManager.INVALID_OFFSET) {
                        assertEquals("pending scroll position offset should still be pending",
                                scrollOffset, mLayoutManager.mPendingScrollPositionOffset);
                    }
                } else {
                    RecyclerView.ViewHolder vh = rv.findViewHolderForPosition(scrollPosition);
                    assertNotNull("scroll to position should work", vh);
                    if (scrollOffset != LinearLayoutManager.INVALID_OFFSET) {
                        assertEquals("scroll offset should be applied properly",
                                mLayoutManager.getPaddingTop() + scrollOffset
                                        + ((RecyclerView.LayoutParams) vh.itemView
                                            .getLayoutParams()).topMargin,
                                mLayoutManager.getDecoratedTop(vh.itemView));
                    }
                }
            }
        };
        mLayoutManager.expectLayouts(2);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    mAdapter.addAndNotify(0, 1);
                    if (scrollOffset == LinearLayoutManager.INVALID_OFFSET) {
                        mLayoutManager.scrollToPosition(scrollPosition);
                    } else {
                        mLayoutManager.scrollToPositionWithOffset(scrollPosition,
                                scrollOffset);
                    }

                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }

            }
        });
        mLayoutManager.waitForLayout(2);
        checkForMainThreadException();
    }

    LayoutParams getLp(View view) {
        return (LayoutParams) view.getLayoutParams();
    }

    public void testGetFirstLastChildrenTest() throws Throwable {
        for (boolean provideArr : new boolean[]{true, false}) {
            for (Config config : mBaseVariations) {
                getFirstLastChildrenTest(config, provideArr);
                removeRecyclerView();
            }
        }
    }

    public void getFirstLastChildrenTest(final Config config, final boolean provideArr)
            throws Throwable {
        setupByConfig(config);
        waitFirstLayout();
        Runnable viewInBoundsTest = new Runnable() {
            @Override
            public void run() {
                VisibleChildren visibleChildren = mLayoutManager.traverseAndFindVisibleChildren();
                final String boundsLog = mLayoutManager.getBoundsLog();
                VisibleChildren queryResult = new VisibleChildren(mLayoutManager.getSpanCount());
                queryResult.firstFullyVisiblePositions = mLayoutManager
                        .findFirstCompletelyVisibleItemPositions(
                                provideArr ? new int[mLayoutManager.getSpanCount()] : null);
                queryResult.firstVisiblePositions = mLayoutManager
                        .findFirstVisibleItemPositions(
                                provideArr ? new int[mLayoutManager.getSpanCount()] : null);
                queryResult.lastFullyVisiblePositions = mLayoutManager
                        .findLastCompletelyVisibleItemPositions(
                                provideArr ? new int[mLayoutManager.getSpanCount()] : null);
                queryResult.lastVisiblePositions = mLayoutManager
                        .findLastVisibleItemPositions(
                                provideArr ? new int[mLayoutManager.getSpanCount()] : null);
                assertEquals(config + ":\nfirst visible child should match traversal result\n"
                                + "traversed:" + visibleChildren + "\n"
                                + "queried:" + queryResult + "\n"
                                + boundsLog, visibleChildren, queryResult
                );
            }
        };
        runTestOnUiThread(viewInBoundsTest);
        // smooth scroll to end of the list and keep testing meanwhile. This will test pre-caching
        // case
        final int scrollPosition = mAdapter.getItemCount();
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.smoothScrollToPosition(scrollPosition);
            }
        });
        while (mLayoutManager.isSmoothScrolling() ||
                mRecyclerView.getScrollState() != RecyclerView.SCROLL_STATE_IDLE) {
            runTestOnUiThread(viewInBoundsTest);
            Thread.sleep(400);
        }
        // delete all items
        mLayoutManager.expectLayouts(2);
        mAdapter.deleteAndNotify(0, mAdapter.getItemCount());
        mLayoutManager.waitForLayout(2);
        // test empty case
        runTestOnUiThread(viewInBoundsTest);
        // set a new adapter with huge items to test full bounds check
        mLayoutManager.expectLayouts(1);
        final int totalSpace = mLayoutManager.mPrimaryOrientation.getTotalSpace();
        final TestAdapter newAdapter = new TestAdapter(100) {
            @Override
            public void onBindViewHolder(TestViewHolder holder,
                    int position) {
                super.onBindViewHolder(holder, position);
                if (config.mOrientation == LinearLayoutManager.HORIZONTAL) {
                    holder.itemView.setMinimumWidth(totalSpace + 5);
                } else {
                    holder.itemView.setMinimumHeight(totalSpace + 5);
                }
            }
        };
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.setAdapter(newAdapter);
            }
        });
        mLayoutManager.waitForLayout(2);
        runTestOnUiThread(viewInBoundsTest);
    }

    public void testInnerGapHandling() throws Throwable {
        innerGapHandlingTest(StaggeredGridLayoutManager.GAP_HANDLING_NONE);
        innerGapHandlingTest(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
    }

    public void innerGapHandlingTest(int strategy) throws Throwable {
        Config config = new Config().spanCount(3).itemCount(500);
        setupByConfig(config);
        mLayoutManager.setGapStrategy(strategy);
        mAdapter.mFullSpanItems.add(100);
        mAdapter.mFullSpanItems.add(104);
        mAdapter.mViewsHaveEqualSize = true;
        waitFirstLayout();
        mLayoutManager.expectLayouts(1);
        scrollToPosition(400);
        mLayoutManager.waitForLayout(2);
        mLayoutManager.expectLayouts(2);
        mAdapter.addAndNotify(101, 1);
        mLayoutManager.waitForLayout(2);
        if (strategy == GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS) {
            mLayoutManager.expectLayouts(1);
        }
        // state
        // now smooth scroll to 99 to trigger a layout around 100
        smoothScrollToPosition(99);
        switch (strategy) {
            case GAP_HANDLING_NONE:
                assertSpans("gap handling:" + Config.gapStrategyName(strategy), new int[]{100, 0},
                        new int[]{101, 2}, new int[]{102, 0}, new int[]{103, 1}, new int[]{104, 2},
                        new int[]{105, 0});
                break;
            case GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS:
                mLayoutManager.waitForLayout(2);
                assertSpans("swap items between spans", new int[]{100, 0}, new int[]{101, 0},
                        new int[]{102, 1}, new int[]{103, 2}, new int[]{104, 0}, new int[]{105, 0});
                break;
        }

    }

    public void testFullSizeSpans() throws Throwable {
        Config config = new Config().spanCount(5).itemCount(30);
        setupByConfig(config);
        mAdapter.mFullSpanItems.add(3);
        waitFirstLayout();
        assertSpans("Testing full size span", new int[]{0, 0}, new int[]{1, 1}, new int[]{2, 2},
                new int[]{3, 0}, new int[]{4, 0}, new int[]{5, 1}, new int[]{6, 2},
                new int[]{7, 3}, new int[]{8, 4});
    }

    void assertSpans(String msg, int[]... childSpanTuples) {
        for (int i = 0; i < childSpanTuples.length; i++) {
            assertSpan(msg, childSpanTuples[i][0], childSpanTuples[i][1]);
        }
    }

    void assertSpan(String msg, int childPosition, int expectedSpan) {
        View view = mLayoutManager.findViewByPosition(childPosition);
        assertNotNull(msg + "view at position " + childPosition + " should exists", view);
        assertEquals(msg + "[child:" + childPosition + "]", expectedSpan,
                getLp(view).mSpan.mIndex);
    }

    public void gapInTheMiddle(Config config) throws Throwable {

    }

    public void testGapAtTheBeginning() throws Throwable {
        for (Config config : mBaseVariations) {
            for (int deleteCount = 1; deleteCount < config.mSpanCount * 2; deleteCount ++) {
                for (int deletePosition = config.mSpanCount - 1;
                        deletePosition < config.mSpanCount + 2; deletePosition ++) {
                    gapAtTheBeginningOfTheListTest(config, deletePosition, deleteCount);
                    removeRecyclerView();
                }
            }
        }
    }

    public void gapAtTheBeginningOfTheListTest(final Config config, int deletePosition,
            int deleteCount) throws Throwable {
        if (config.mSpanCount < 2 || config.mGapStrategy == GAP_HANDLING_NONE) {
            return;
        }
        if (config.mItemCount < 100) {
            config.itemCount(100);
        }
        final String logPrefix = config + ", deletePos:" + deletePosition + ", deleteCount:"
                + deleteCount;
        setupByConfig(config);
        final RecyclerView.Adapter adapter = mAdapter;
        waitFirstLayout();
        // scroll far away
        smoothScrollToPosition(config.mItemCount / 2);
        // assert to be deleted child is not visible
        assertNull(logPrefix + " test sanity, to be deleted child should be invisible",
                mRecyclerView.findViewHolderForPosition(deletePosition));
        // delete the child and notify
        mAdapter.deleteAndNotify(deletePosition, deleteCount);
        getInstrumentation().waitForIdleSync();
        mLayoutManager.expectLayouts(1);
        smoothScrollToPosition(0);
        mLayoutManager.waitForLayout(2);
        // due to data changes, first item may become visible before others which will cause
        // smooth scrolling to stop. Triggering it twice more is a naive hack.
        // Until we have time to consider it as a bug, this is the only workaround.
        smoothScrollToPosition(0);
        Thread.sleep(300);
        smoothScrollToPosition(0);
        Thread.sleep(500);
        // some animations should happen and we should recover layout
        final Map<Item, Rect> actualCoords = mLayoutManager.collectChildCoordinates();
        // now layout another RV with same adapter
        removeRecyclerView();
        setupByConfig(config);
        mRecyclerView.setAdapter(adapter);// use same adapter so that items can be matched
        waitFirstLayout();
        final Map<Item, Rect> desiredCoords = mLayoutManager.collectChildCoordinates();
        assertRectSetsEqual(logPrefix + " when an item from the start of the list is deleted, "
                        + "layout should recover the state once scrolling is stopped",
                desiredCoords, actualCoords);
    }

    public void testPartialSpanInvalidation() throws Throwable {
        Config config = new Config().spanCount(5).itemCount(100);
        setupByConfig(config);
        for (int i = 20; i < mAdapter.getItemCount(); i += 20) {
            mAdapter.mFullSpanItems.add(i);
        }
        waitFirstLayout();
        smoothScrollToPosition(50);
        int prevSpanId = mLayoutManager.mLazySpanLookup.mData[30];
        mAdapter.changeAndNotify(15, 2);
        Thread.sleep(200);
        assertEquals("Invalidation should happen within full span item boundaries", prevSpanId,
                mLayoutManager.mLazySpanLookup.mData[30]);
        assertEquals("item in invalidated range should have clear span id",
                LayoutParams.INVALID_SPAN_ID, mLayoutManager.mLazySpanLookup.mData[16]);
        smoothScrollToPosition(85);
        int[] prevSpans = copyOfRange(mLayoutManager.mLazySpanLookup.mData, 62, 85);
        mAdapter.deleteAndNotify(55, 2);
        Thread.sleep(200);
        assertEquals("item in invalidated range should have clear span id",
                LayoutParams.INVALID_SPAN_ID, mLayoutManager.mLazySpanLookup.mData[16]);
        int[] newSpans = copyOfRange(mLayoutManager.mLazySpanLookup.mData, 60, 83);
        assertSpanAssignmentEquality("valid spans should be shifted for deleted item", prevSpans,
                newSpans, 0, 0, newSpans.length);
    }

    // Same as Arrays.copyOfRange but for API 7
    private int[] copyOfRange(int[] original, int from, int to) {
        int newLength = to - from;
        if (newLength < 0)
            throw new IllegalArgumentException(from + " > " + to);
        int[] copy = new int[newLength];
        System.arraycopy(original, from, copy, 0,
                Math.min(original.length - from, newLength));
        return copy;
    }

    public void testSpanReassignmentsOnItemChange() throws Throwable {
        Config config = new Config().spanCount(5);
        setupByConfig(config);
        waitFirstLayout();
        smoothScrollToPosition(mAdapter.getItemCount() / 2);
        final int changePosition = mAdapter.getItemCount() / 4;
        mLayoutManager.expectLayouts(1);
        mAdapter.changeAndNotify(changePosition, 1);
        mLayoutManager.assertNoLayout("no layout should happen when an invisible child is updated",
                1);
        // delete an item before visible area
        int deletedPosition = mLayoutManager.getPosition(mLayoutManager.getChildAt(0)) - 2;
        Map<Item, Rect> before = mLayoutManager.collectChildCoordinates();
        if (DEBUG) {
            Log.d(TAG, "before:");
            for (Map.Entry<Item, Rect> entry : before.entrySet()) {
                Log.d(TAG, entry.getKey().mAdapterIndex + ":" + entry.getValue());
            }
        }
        mLayoutManager.expectLayouts(1);
        mAdapter.deleteAndNotify(deletedPosition, 1);
        mLayoutManager.waitForLayout(2);
        assertRectSetsEqual(config + " when an item towards the head of the list is deleted, it "
                        + "should not affect the layout if it is not visible", before,
                mLayoutManager.collectChildCoordinates()
        );
        deletedPosition = mLayoutManager.getPosition(mLayoutManager.getChildAt(2));
        mLayoutManager.expectLayouts(1);
        mAdapter.deleteAndNotify(deletedPosition, 1);
        mLayoutManager.waitForLayout(2);
        assertRectSetsNotEqual(config + " when a visible item is deleted, it should affect the "
                + "layout", before, mLayoutManager.collectChildCoordinates());
    }

    void assertSpanAssignmentEquality(String msg, int[] set1, int[] set2, int start, int end) {
        for (int i = start; i < end; i++) {
            assertEquals(msg + " ind:" + i, set1[i], set2[i]);
        }
    }

    void assertSpanAssignmentEquality(String msg, int[] set1, int[] set2, int start1, int start2,
            int length) {
        for (int i = 0; i < length; i++) {
            assertEquals(msg + " ind1:" + (start1 + i) + ", ind2:" + (start2 + i), set1[start1 + i],
                    set2[start2 + i]);
        }
    }

    public void testViewSnapping() throws Throwable {
        for (Config config : mBaseVariations) {
            viewSnapTest(config.itemCount(config.mSpanCount + 1));
            removeRecyclerView();
        }
    }

    public void viewSnapTest(Config config) throws Throwable {
        setupByConfig(config);
        waitFirstLayout();
        // run these tests twice. once initial layout, once after scroll
        String logSuffix = "";
        for (int i = 0; i < 2; i++) {
            Map<Item, Rect> itemRectMap = mLayoutManager.collectChildCoordinates();
            Rect recyclerViewBounds = getDecoratedRecyclerViewBounds();
            Rect usedLayoutBounds = new Rect();
            for (Rect rect : itemRectMap.values()) {
                usedLayoutBounds.union(rect);
            }
            if (DEBUG) {
                Log.d(TAG, "testing view snapping (" + logSuffix + ") for config " + config);
            }
            if (config.mOrientation == VERTICAL) {
                assertEquals(config + " there should be no gap on left" + logSuffix,
                        usedLayoutBounds.left, recyclerViewBounds.left);
                assertEquals(config + " there should be no gap on right" + logSuffix,
                        usedLayoutBounds.right, recyclerViewBounds.right);
                if (config.mReverseLayout) {
                    assertEquals(config + " there should be no gap on bottom" + logSuffix,
                            usedLayoutBounds.bottom, recyclerViewBounds.bottom);
                    assertTrue(config + " there should be some gap on top" + logSuffix,
                            usedLayoutBounds.top > recyclerViewBounds.top);
                } else {
                    assertEquals(config + " there should be no gap on top" + logSuffix,
                            usedLayoutBounds.top, recyclerViewBounds.top);
                    assertTrue(config + " there should be some gap at the bottom" + logSuffix,
                            usedLayoutBounds.bottom < recyclerViewBounds.bottom);
                }
            } else {
                assertEquals(config + " there should be no gap on top" + logSuffix,
                        usedLayoutBounds.top, recyclerViewBounds.top);
                assertEquals(config + " there should be no gap at the bottom" + logSuffix,
                        usedLayoutBounds.bottom, recyclerViewBounds.bottom);
                if (config.mReverseLayout) {
                    assertEquals(config + " there should be no on right" + logSuffix,
                            usedLayoutBounds.right, recyclerViewBounds.right);
                    assertTrue(config + " there should be some gap on left" + logSuffix,
                            usedLayoutBounds.left > recyclerViewBounds.left);
                } else {
                    assertEquals(config + " there should be no gap on left" + logSuffix,
                            usedLayoutBounds.left, recyclerViewBounds.left);
                    assertTrue(config + " there should be some gap on right" + logSuffix,
                            usedLayoutBounds.right < recyclerViewBounds.right);
                }
            }
            final int scroll = config.mReverseLayout ? -500 : 500;
            scrollBy(scroll);
            logSuffix = " scrolled " + scroll;
        }

    }

    public void testSpanCountChangeOnRestoreSavedState() throws Throwable {
        Config config = new Config(HORIZONTAL, true, 5, GAP_HANDLING_NONE);
        setupByConfig(config);
        waitFirstLayout();

        int beforeChildCount = mLayoutManager.getChildCount();
        Parcelable savedState = mRecyclerView.onSaveInstanceState();
        // we append a suffix to the parcelable to test out of bounds
        String parcelSuffix = UUID.randomUUID().toString();
        Parcel parcel = Parcel.obtain();
        savedState.writeToParcel(parcel, 0);
        parcel.writeString(parcelSuffix);
        removeRecyclerView();
        // reset for reading
        parcel.setDataPosition(0);
        // re-create
        savedState = RecyclerView.SavedState.CREATOR.createFromParcel(parcel);
        removeRecyclerView();

        RecyclerView restored = new RecyclerView(getActivity());
        mLayoutManager = new WrappedLayoutManager(config.mSpanCount, config.mOrientation);
        mLayoutManager.setReverseLayout(config.mReverseLayout);
        mLayoutManager.setGapStrategy(config.mGapStrategy);
        restored.setLayoutManager(mLayoutManager);
        // use the same adapter for Rect matching
        restored.setAdapter(mAdapter);
        restored.onRestoreInstanceState(savedState);
        mLayoutManager.setSpanCount(1);
        mLayoutManager.expectLayouts(1);
        setRecyclerView(restored);
        mLayoutManager.waitForLayout(2);
        assertEquals("on saved state, reverse layout should be preserved",
                config.mReverseLayout, mLayoutManager.getReverseLayout());
        assertEquals("on saved state, orientation should be preserved",
                config.mOrientation, mLayoutManager.getOrientation());
        assertEquals("after setting new span count, layout manager should keep new value",
                1, mLayoutManager.getSpanCount());
        assertEquals("on saved state, gap strategy should be preserved",
                config.mGapStrategy, mLayoutManager.getGapStrategy());
        assertTrue("when span count is dramatically changed after restore, # of child views "
                + "should change", beforeChildCount > mLayoutManager.getChildCount());
        // make sure LLM can layout all children. is some span info is leaked, this would crash
        smoothScrollToPosition(mAdapter.getItemCount() - 1);
    }

    public void testSavedState() throws Throwable {
        PostLayoutRunnable[] postLayoutOptions = new PostLayoutRunnable[]{
                new PostLayoutRunnable() {
                    @Override
                    public void run() throws Throwable {
                        // do nothing
                    }

                    @Override
                    public String describe() {
                        return "doing nothing";
                    }
                },
                new PostLayoutRunnable() {
                    @Override
                    public void run() throws Throwable {
                        mLayoutManager.expectLayouts(1);
                        scrollToPosition(mAdapter.getItemCount() * 3 / 4);
                        mLayoutManager.waitForLayout(2);
                    }

                    @Override
                    public String describe() {
                        return "scroll to position " + (mAdapter == null ? "" :
                                mAdapter.getItemCount() * 3 / 4);
                    }
                },
                new PostLayoutRunnable() {
                    @Override
                    public void run() throws Throwable {
                        mLayoutManager.expectLayouts(1);
                        scrollToPositionWithOffset(mAdapter.getItemCount() / 3,
                                50);
                        mLayoutManager.waitForLayout(2);
                    }

                    @Override
                    public String describe() {
                        return "scroll to position " + (mAdapter == null ? "" :
                                mAdapter.getItemCount() / 3) + "with positive offset";
                    }
                },
                new PostLayoutRunnable() {
                    @Override
                    public void run() throws Throwable {
                        mLayoutManager.expectLayouts(1);
                        scrollToPositionWithOffset(mAdapter.getItemCount() * 2 / 3,
                                -50);
                        mLayoutManager.waitForLayout(2);
                    }

                    @Override
                    public String describe() {
                        return "scroll to position with negative offset";
                    }
                }
        };
        boolean[] waitForLayoutOptions = new boolean[]{false, true};
        List<Config> testVariations = new ArrayList<Config>();
        testVariations.addAll(mBaseVariations);
        for (Config config : mBaseVariations) {
            if (config.mSpanCount < 2) {
                continue;
            }
            final Config clone = (Config) config.clone();
            clone.mItemCount = clone.mSpanCount - 1;
            testVariations.add(clone);
        }

        for (Config config : testVariations) {
            for (PostLayoutRunnable runnable : postLayoutOptions) {
                for (boolean waitForLayout : waitForLayoutOptions) {
                    savedStateTest(config, waitForLayout, runnable);
                    removeRecyclerView();
                }
            }
        }
    }

    private void saveRestore(Config config) throws Throwable {
        Parcelable savedState = mRecyclerView.onSaveInstanceState();
        // we append a suffix to the parcelable to test out of bounds
        String parcelSuffix = UUID.randomUUID().toString();
        Parcel parcel = Parcel.obtain();
        savedState.writeToParcel(parcel, 0);
        parcel.writeString(parcelSuffix);
        removeRecyclerView();
        // reset for reading
        parcel.setDataPosition(0);
        // re-create
        savedState = RecyclerView.SavedState.CREATOR.createFromParcel(parcel);
        RecyclerView restored = new RecyclerView(getActivity());
        mLayoutManager = new WrappedLayoutManager(config.mSpanCount, config.mOrientation);
        mLayoutManager.setGapStrategy(config.mGapStrategy);
        restored.setLayoutManager(mLayoutManager);
        // use the same adapter for Rect matching
        restored.setAdapter(mAdapter);
        restored.onRestoreInstanceState(savedState);
        if (Looper.myLooper() == Looper.getMainLooper()) {
            mLayoutManager.expectLayouts(1);
            setRecyclerView(restored);
        } else {
            mLayoutManager.expectLayouts(1);
            setRecyclerView(restored);
            mLayoutManager.waitForLayout(2);
        }
    }

    public void savedStateTest(Config config, boolean waitForLayout,
            PostLayoutRunnable postLayoutOperations)
            throws Throwable {
        if (DEBUG) {
            Log.d(TAG, "testing saved state with wait for layout = " + waitForLayout + " config "
                    + config + " post layout action " + postLayoutOperations.describe());
        }
        setupByConfig(config);
        waitFirstLayout();
        if (waitForLayout) {
            postLayoutOperations.run();
        }
        final int firstCompletelyVisiblePosition = mLayoutManager.findFirstVisibleItemPositionInt();
        Map<Item, Rect> before = mLayoutManager.collectChildCoordinates();
        Parcelable savedState = mRecyclerView.onSaveInstanceState();
        // we append a suffix to the parcelable to test out of bounds
        String parcelSuffix = UUID.randomUUID().toString();
        Parcel parcel = Parcel.obtain();
        savedState.writeToParcel(parcel, 0);
        parcel.writeString(parcelSuffix);
        removeRecyclerView();
        // reset for reading
        parcel.setDataPosition(0);
        // re-create
        savedState = RecyclerView.SavedState.CREATOR.createFromParcel(parcel);
        removeRecyclerView();

        RecyclerView restored = new RecyclerView(getActivity());
        mLayoutManager = new WrappedLayoutManager(config.mSpanCount, config.mOrientation);
        mLayoutManager.setGapStrategy(config.mGapStrategy);
        restored.setLayoutManager(mLayoutManager);
        // use the same adapter for Rect matching
        restored.setAdapter(mAdapter);
        restored.onRestoreInstanceState(savedState);
        assertEquals("Parcel reading should not go out of bounds", parcelSuffix,
                parcel.readString());
        mLayoutManager.expectLayouts(1);
        setRecyclerView(restored);
        mLayoutManager.waitForLayout(2);
        assertEquals(config + " on saved state, reverse layout should be preserved",
                config.mReverseLayout, mLayoutManager.getReverseLayout());
        assertEquals(config + " on saved state, orientation should be preserved",
                config.mOrientation, mLayoutManager.getOrientation());
        assertEquals(config + " on saved state, span count should be preserved",
                config.mSpanCount, mLayoutManager.getSpanCount());
        assertEquals(config + " on saved state, gap strategy should be preserved",
                config.mGapStrategy, mLayoutManager.getGapStrategy());
        assertEquals(config + " on saved state, first completely visible child position should"
                + " be preserved", firstCompletelyVisiblePosition,
                mLayoutManager.findFirstVisibleItemPositionInt());
        if (waitForLayout) {
            assertRectSetsEqual(config + "\npost layout op:" + postLayoutOperations.describe()
                            + ": on restore, previous view positions should be preserved",
                    before, mLayoutManager.collectChildCoordinates()
            );
        }
        // TODO add tests for changing values after restore before layout
    }

    public void testScrollToPositionWithOffset() throws Throwable {
        for (Config config : mBaseVariations) {
            scrollToPositionWithOffsetTest(config);
            removeRecyclerView();
        }
    }

    public void scrollToPositionWithOffsetTest(Config config) throws Throwable {
        setupByConfig(config);
        waitFirstLayout();
        OrientationHelper orientationHelper = OrientationHelper
                .createOrientationHelper(mLayoutManager, config.mOrientation);
        Rect layoutBounds = getDecoratedRecyclerViewBounds();
        // try scrolling towards head, should not affect anything
        Map<Item, Rect> before = mLayoutManager.collectChildCoordinates();
        scrollToPositionWithOffset(0, 20);
        assertRectSetsEqual(config + " trying to over scroll with offset should be no-op",
                before, mLayoutManager.collectChildCoordinates());
        // try offsetting some visible children
        int testCount = 10;
        while (testCount-- > 0) {
            // get middle child
            final View child = mLayoutManager.getChildAt(mLayoutManager.getChildCount() / 2);
            final int position = mRecyclerView.getChildPosition(child);
            final int startOffset = config.mReverseLayout ?
                    orientationHelper.getEndAfterPadding() - orientationHelper
                            .getDecoratedEnd(child)
                    : orientationHelper.getDecoratedStart(child) - orientationHelper
                            .getStartAfterPadding();
            final int scrollOffset = startOffset / 2;
            mLayoutManager.expectLayouts(1);
            scrollToPositionWithOffset(position, scrollOffset);
            mLayoutManager.waitForLayout(2);
            final int finalOffset = config.mReverseLayout ?
                    orientationHelper.getEndAfterPadding() - orientationHelper
                            .getDecoratedEnd(child)
                    : orientationHelper.getDecoratedStart(child) - orientationHelper
                            .getStartAfterPadding();
            assertEquals(config + " scroll with offset on a visible child should work fine",
                    scrollOffset, finalOffset);
        }

        // try scrolling to invisible children
        testCount = 10;
        // we test above and below, one by one
        int offsetMultiplier = -1;
        while (testCount-- > 0) {
            final TargetTuple target = findInvisibleTarget(config);
            mLayoutManager.expectLayouts(1);
            final int offset = offsetMultiplier
                    * orientationHelper.getDecoratedMeasurement(mLayoutManager.getChildAt(0)) / 3;
            scrollToPositionWithOffset(target.mPosition, offset);
            mLayoutManager.waitForLayout(2);
            final View child = mLayoutManager.findViewByPosition(target.mPosition);
            assertNotNull(config + " scrolling to a mPosition with offset " + offset
                    + " should layout it", child);
            final Rect bounds = mLayoutManager.getViewBounds(child);
            if (DEBUG) {
                Log.d(TAG, config + " post scroll to invisible mPosition " + bounds + " in "
                        + layoutBounds + " with offset " + offset);
            }

            if (config.mReverseLayout) {
                assertEquals(config + " when scrolling with offset to an invisible in reverse "
                                + "layout, its end should align with recycler view's end - offset",
                        orientationHelper.getEndAfterPadding() - offset,
                        orientationHelper.getDecoratedEnd(child)
                );
            } else {
                assertEquals(config + " when scrolling with offset to an invisible child in normal"
                                + " layout its start should align with recycler view's start + "
                                + "offset",
                        orientationHelper.getStartAfterPadding() + offset,
                        orientationHelper.getDecoratedStart(child)
                );
            }
            offsetMultiplier *= -1;
        }
    }

    public void testScrollToPosition() throws Throwable {
        for (Config config : mBaseVariations) {
            scrollToPositionTest(config);
            removeRecyclerView();
        }
    }

    private TargetTuple findInvisibleTarget(Config config) {
        int minPosition = Integer.MAX_VALUE, maxPosition = Integer.MIN_VALUE;
        for (int i = 0; i < mLayoutManager.getChildCount(); i++) {
            View child = mLayoutManager.getChildAt(i);
            int position = mRecyclerView.getChildPosition(child);
            if (position < minPosition) {
                minPosition = position;
            }
            if (position > maxPosition) {
                maxPosition = position;
            }
        }
        final int tailTarget = maxPosition + (mAdapter.getItemCount() - maxPosition) / 2;
        final int headTarget = minPosition / 2;
        final int target;
        // where will the child come from ?
        final int itemLayoutDirection;
        if (Math.abs(tailTarget - maxPosition) > Math.abs(headTarget - minPosition)) {
            target = tailTarget;
            itemLayoutDirection = config.mReverseLayout ? LAYOUT_START : LAYOUT_END;
        } else {
            target = headTarget;
            itemLayoutDirection = config.mReverseLayout ? LAYOUT_END : LAYOUT_START;
        }
        if (DEBUG) {
            Log.d(TAG,
                    config + " target:" + target + " min:" + minPosition + ", max:" + maxPosition);
        }
        return new TargetTuple(target, itemLayoutDirection);
    }

    public void scrollToPositionTest(Config config) throws Throwable {
        setupByConfig(config);
        waitFirstLayout();
        OrientationHelper orientationHelper = OrientationHelper
                .createOrientationHelper(mLayoutManager, config.mOrientation);
        Rect layoutBounds = getDecoratedRecyclerViewBounds();
        for (int i = 0; i < mLayoutManager.getChildCount(); i++) {
            View view = mLayoutManager.getChildAt(i);
            Rect bounds = mLayoutManager.getViewBounds(view);
            if (layoutBounds.contains(bounds)) {
                Map<Item, Rect> initialBounds = mLayoutManager.collectChildCoordinates();
                final int position = mRecyclerView.getChildPosition(view);
                LayoutParams layoutParams
                        = (LayoutParams) (view.getLayoutParams());
                TestViewHolder vh = (TestViewHolder) layoutParams.mViewHolder;
                assertEquals("recycler view mPosition should match adapter mPosition", position,
                        vh.mBindedItem.mAdapterIndex);
                if (DEBUG) {
                    Log.d(TAG, "testing scroll to visible mPosition at " + position
                            + " " + bounds + " inside " + layoutBounds);
                }
                mLayoutManager.expectLayouts(1);
                scrollToPosition(position);
                mLayoutManager.waitForLayout(2);
                if (DEBUG) {
                    view = mLayoutManager.findViewByPosition(position);
                    Rect newBounds = mLayoutManager.getViewBounds(view);
                    Log.d(TAG, "after scrolling to visible mPosition " +
                            bounds + " equals " + newBounds);
                }

                assertRectSetsEqual(
                        config + "scroll to mPosition on fully visible child should be no-op",
                        initialBounds, mLayoutManager.collectChildCoordinates());
            } else {
                final int position = mRecyclerView.getChildPosition(view);
                if (DEBUG) {
                    Log.d(TAG,
                            "child(" + position + ") not fully visible " + bounds + " not inside "
                                    + layoutBounds
                                    + mRecyclerView.getChildPosition(view)
                    );
                }
                mLayoutManager.expectLayouts(1);
                runTestOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mLayoutManager.scrollToPosition(position);
                    }
                });
                mLayoutManager.waitForLayout(2);
                view = mLayoutManager.findViewByPosition(position);
                bounds = mLayoutManager.getViewBounds(view);
                if (DEBUG) {
                    Log.d(TAG, "after scroll to partially visible child " + bounds + " in "
                            + layoutBounds);
                }
                assertTrue(config
                                + " after scrolling to a partially visible child, it should become fully "
                                + " visible. " + bounds + " not inside " + layoutBounds,
                        layoutBounds.contains(bounds)
                );
                assertTrue(config + " when scrolling to a partially visible item, one of its edges "
                        + "should be on the boundaries", orientationHelper.getStartAfterPadding() ==
                        orientationHelper.getDecoratedStart(view)
                        || orientationHelper.getEndAfterPadding() ==
                        orientationHelper.getDecoratedEnd(view));
            }
        }

        // try scrolling to invisible children
        int testCount = 10;
        while (testCount-- > 0) {
            final TargetTuple target = findInvisibleTarget(config);
            mLayoutManager.expectLayouts(1);
            scrollToPosition(target.mPosition);
            mLayoutManager.waitForLayout(2);
            final View child = mLayoutManager.findViewByPosition(target.mPosition);
            assertNotNull(config + " scrolling to a mPosition should lay it out", child);
            final Rect bounds = mLayoutManager.getViewBounds(child);
            if (DEBUG) {
                Log.d(TAG, config + " post scroll to invisible mPosition " + bounds + " in "
                        + layoutBounds);
            }
            assertTrue(config + " scrolling to a mPosition should make it fully visible",
                    layoutBounds.contains(bounds));
            if (target.mLayoutDirection == LAYOUT_START) {
                assertEquals(
                        config + " when scrolling to an invisible child above, its start should"
                                + " align with recycler view's start",
                        orientationHelper.getStartAfterPadding(),
                        orientationHelper.getDecoratedStart(child)
                );
            } else {
                assertEquals(config + " when scrolling to an invisible child below, its end "
                                + "should align with recycler view's end",
                        orientationHelper.getEndAfterPadding(),
                        orientationHelper.getDecoratedEnd(child)
                );
            }
        }
    }

    private void scrollToPositionWithOffset(final int position, final int offset) throws Throwable {
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLayoutManager.scrollToPositionWithOffset(position, offset);
            }
        });
    }

    public void testLayoutOrder() throws Throwable {
        for (Config config : mBaseVariations) {
            layoutOrderTest(config);
            removeRecyclerView();
        }
    }

    public void layoutOrderTest(Config config) throws Throwable {
        setupByConfig(config);
        assertViewPositions(config);
    }

    void assertViewPositions(Config config) {
        ArrayList<ArrayList<View>> viewsBySpan = mLayoutManager.collectChildrenBySpan();
        OrientationHelper orientationHelper = OrientationHelper
                .createOrientationHelper(mLayoutManager, config.mOrientation);
        for (ArrayList<View> span : viewsBySpan) {
            // validate all children's order. first child should have min start mPosition
            final int count = span.size();
            for (int i = 0, j = 1; j < count; i++, j++) {
                View prev = span.get(i);
                View next = span.get(j);
                assertTrue(config + " prev item should be above next item",
                        orientationHelper.getDecoratedEnd(prev) <= orientationHelper
                                .getDecoratedStart(next)
                );

            }
        }
    }

    public void testScrollBy() throws Throwable {
        for (Config config : mBaseVariations) {
            scrollByTest(config);
            removeRecyclerView();
        }
    }

    void waitFirstLayout() throws Throwable {
        mLayoutManager.expectLayouts(1);
        setRecyclerView(mRecyclerView);
        mLayoutManager.waitForLayout(2);
        getInstrumentation().waitForIdleSync();
    }

    public void scrollByTest(Config config) throws Throwable {
        setupByConfig(config);
        waitFirstLayout();
        // try invalid scroll. should not happen
        final View first = mLayoutManager.getChildAt(0);
        OrientationHelper primaryOrientation = OrientationHelper
                .createOrientationHelper(mLayoutManager, config.mOrientation);
        int scrollDist;
        if (config.mReverseLayout) {
            scrollDist = primaryOrientation.getDecoratedMeasurement(first) / 2;
        } else {
            scrollDist = -primaryOrientation.getDecoratedMeasurement(first) / 2;
        }
        Map<Item, Rect> before = mLayoutManager.collectChildCoordinates();
        scrollBy(scrollDist);
        Map<Item, Rect> after = mLayoutManager.collectChildCoordinates();
        assertRectSetsEqual(
                config + " if there are no more items, scroll should not happen (dt:" + scrollDist
                        + ")",
                before, after
        );

        scrollDist = -scrollDist * 3;
        before = mLayoutManager.collectChildCoordinates();
        scrollBy(scrollDist);
        after = mLayoutManager.collectChildCoordinates();
        int layoutStart = primaryOrientation.getStartAfterPadding();
        int layoutEnd = primaryOrientation.getEndAfterPadding();
        for (Map.Entry<Item, Rect> entry : before.entrySet()) {
            Rect afterRect = after.get(entry.getKey());
            // offset rect
            if (config.mOrientation == VERTICAL) {
                entry.getValue().offset(0, -scrollDist);
            } else {
                entry.getValue().offset(-scrollDist, 0);
            }
            if (afterRect == null || afterRect.isEmpty()) {
                // assert item is out of bounds
                int start, end;
                if (config.mOrientation == VERTICAL) {
                    start = entry.getValue().top;
                    end = entry.getValue().bottom;
                } else {
                    start = entry.getValue().left;
                    end = entry.getValue().right;
                }
                assertTrue(
                        config + " if item is missing after relayout, it should be out of bounds."
                                + "item start: " + start + ", end:" + end + " layout start:"
                                + layoutStart +
                                ", layout end:" + layoutEnd,
                        start <= layoutStart && end <= layoutEnd ||
                                start >= layoutEnd && end >= layoutEnd
                );
            } else {
                assertEquals(config + " Item should be laid out at the scroll offset coordinates",
                        entry.getValue(),
                        afterRect);
            }
        }
        assertViewPositions(config);
    }

    public void testAccessibilityPositions() throws Throwable {
        setupByConfig(new Config(VERTICAL, false, 3, GAP_HANDLING_NONE));
        waitFirstLayout();
        final AccessibilityDelegateCompat delegateCompat = mRecyclerView
                .getCompatAccessibilityDelegate();
        final AccessibilityEvent event = AccessibilityEvent.obtain();
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                delegateCompat.onInitializeAccessibilityEvent(mRecyclerView, event);
            }
        });
        final AccessibilityRecordCompat record = AccessibilityEventCompat
                .asRecord(event);
        final int start = mRecyclerView
                .getChildPosition(mLayoutManager.findFirstVisibleItemClosestToStart(false));
        final int end = mRecyclerView
                .getChildPosition(mLayoutManager.findFirstVisibleItemClosestToEnd(false));
        assertEquals("first item position should match",
                Math.min(start, end), record.getFromIndex());
        assertEquals("last item position should match",
                Math.max(start, end), record.getToIndex());

    }

    public void testConsistentRelayout() throws Throwable {
        for (Config config : mBaseVariations) {
            for (boolean firstChildMultiSpan : new boolean[]{false, true}) {
                consistentRelayoutTest(config, firstChildMultiSpan);
            }
            removeRecyclerView();
        }
    }

    public void consistentRelayoutTest(Config config, boolean firstChildMultiSpan)
            throws Throwable {
        setupByConfig(config);
        if (firstChildMultiSpan) {
            mAdapter.mFullSpanItems.add(0);
        }
        waitFirstLayout();
        // record all child positions
        Map<Item, Rect> before = mLayoutManager.collectChildCoordinates();
        requestLayoutOnUIThread(mRecyclerView);
        Map<Item, Rect> after = mLayoutManager.collectChildCoordinates();
        assertRectSetsEqual(
                config + " simple re-layout, firstChildMultiSpan:" + firstChildMultiSpan, before,
                after);
        // scroll some to create inconsistency
        View firstChild = mLayoutManager.getChildAt(0);
        final int firstChildStartBeforeScroll = mLayoutManager.mPrimaryOrientation
                .getDecoratedStart(firstChild);
        int distance = mLayoutManager.mPrimaryOrientation.getDecoratedMeasurement(firstChild) / 2;
        if (config.mReverseLayout) {
            distance *= -1;
        }
        scrollBy(distance);
        waitForMainThread(2);
        assertTrue("scroll by should move children", firstChildStartBeforeScroll !=
                mLayoutManager.mPrimaryOrientation.getDecoratedStart(firstChild));
        before = mLayoutManager.collectChildCoordinates();
        mLayoutManager.expectLayouts(1);
        requestLayoutOnUIThread(mRecyclerView);
        mLayoutManager.waitForLayout(2);
        after = mLayoutManager.collectChildCoordinates();
        assertRectSetsEqual(config + " simple re-layout after scroll", before, after);
    }

    /**
     * enqueues an empty runnable to main thread so that we can be assured it did run
     *
     * @param count Number of times to run
     */
    private void waitForMainThread(int count) throws Throwable {
        final AtomicInteger i = new AtomicInteger(count);
        while (i.get() > 0) {
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    i.decrementAndGet();
                }
            });
        }
    }

    public void assertRectSetsNotEqual(String message, Map<Item, Rect> before,
            Map<Item, Rect> after) {
        Throwable throwable = null;
        try {
            assertRectSetsEqual("NOT " + message, before, after);
        } catch (Throwable t) {
            throwable = t;
        }
        assertNotNull(message + " two layout should be different", throwable);
    }

    public void assertRectSetsEqual(String message, Map<Item, Rect> before, Map<Item, Rect> after) {
        StringBuilder log = new StringBuilder();
        if (DEBUG) {
            log.append("checking rectangle equality.\n");
            log.append("before:");
            for (Map.Entry<Item, Rect> entry : before.entrySet()) {
                log.append("\n").append(entry.getKey().mAdapterIndex).append(":")
                        .append(entry.getValue());
            }
            log.append("\nafter:");
            for (Map.Entry<Item, Rect> entry : after.entrySet()) {
                log.append("\n").append(entry.getKey().mAdapterIndex).append(":")
                        .append(entry.getValue());
            }
            message += "\n\n" + log.toString();
        }
        assertEquals(message + ": item counts should be equal", before.size()
                , after.size());
        for (Map.Entry<Item, Rect> entry : before.entrySet()) {
            Rect afterRect = after.get(entry.getKey());
            assertNotNull(message + ": Same item should be visible after simple re-layout",
                    afterRect);
            assertEquals(message + ": Item should be laid out at the same coordinates",
                    entry.getValue(),
                    afterRect);
        }
    }

    // test layout params assignment

    static class OnLayoutListener {
        void before(RecyclerView.Recycler recycler, RecyclerView.State state){}
        void after(RecyclerView.Recycler recycler, RecyclerView.State state){}
    }

    class WrappedLayoutManager extends StaggeredGridLayoutManager {

        CountDownLatch layoutLatch;
        OnLayoutListener mOnLayoutListener;
        // gradle does not yet let us customize manifest for tests which is necessary to test RTL.
        // until bug is fixed, we'll fake it.
        // public issue id: 57819
        Boolean mFakeRTL;

        @Override
        boolean isLayoutRTL() {
            return mFakeRTL == null ? super.isLayoutRTL() : mFakeRTL;
        }

        public void expectLayouts(int count) {
            layoutLatch = new CountDownLatch(count);
        }

        public void waitForLayout(long timeout) throws InterruptedException {
            waitForLayout(timeout * (DEBUG ? 1000 : 1), TimeUnit.SECONDS);
        }

        public void waitForLayout(long timeout, TimeUnit timeUnit) throws InterruptedException {
            layoutLatch.await(timeout, timeUnit);
            assertEquals("all expected layouts should be executed at the expected time",
                    0, layoutLatch.getCount());
        }

        public void assertNoLayout(String msg, long timeout) throws Throwable {
            layoutLatch.await(timeout, TimeUnit.SECONDS);
            assertFalse(msg, layoutLatch.getCount() == 0);
        }

        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            try {
                if (mOnLayoutListener != null) {
                    mOnLayoutListener.before(recycler, state);
                }
                super.onLayoutChildren(recycler, state);
                if (mOnLayoutListener != null) {
                    mOnLayoutListener.after(recycler, state);
                }
            } catch (Throwable t) {
                postExceptionToInstrumentation(t);
            }
            layoutLatch.countDown();
        }

        public WrappedLayoutManager(int spanCount, int orientation) {
            super(spanCount, orientation);
        }

        ArrayList<ArrayList<View>> collectChildrenBySpan() {
            ArrayList<ArrayList<View>> viewsBySpan = new ArrayList<ArrayList<View>>();
            for (int i = 0; i < getSpanCount(); i++) {
                viewsBySpan.add(new ArrayList<View>());
            }
            for (int i = 0; i < getChildCount(); i++) {
                View view = getChildAt(i);
                LayoutParams lp
                        = (LayoutParams) view
                        .getLayoutParams();
                viewsBySpan.get(lp.mSpan.mIndex).add(view);
            }
            return viewsBySpan;
        }

        Rect getViewBounds(View view) {
            if (getOrientation() == HORIZONTAL) {
                return new Rect(
                        mPrimaryOrientation.getDecoratedStart(view),
                        mSecondaryOrientation.getDecoratedStart(view),
                        mPrimaryOrientation.getDecoratedEnd(view),
                        mSecondaryOrientation.getDecoratedEnd(view));
            } else {
                return new Rect(
                        mSecondaryOrientation.getDecoratedStart(view),
                        mPrimaryOrientation.getDecoratedStart(view),
                        mSecondaryOrientation.getDecoratedEnd(view),
                        mPrimaryOrientation.getDecoratedEnd(view));
            }
        }

        public String getBoundsLog() {
            StringBuilder sb = new StringBuilder();
            sb.append("view bounds:[start:").append(mPrimaryOrientation.getStartAfterPadding())
                    .append(",").append(" end").append(mPrimaryOrientation.getEndAfterPadding());
            sb.append("\nchildren bounds\n");
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                sb.append("child (ind:").append(i).append(", pos:").append(getPosition(child))
                        .append("[").append("start:").append(
                        mPrimaryOrientation.getDecoratedStart(child)).append(", end:")
                        .append(mPrimaryOrientation.getDecoratedEnd(child)).append("]\n");
            }
            return sb.toString();
        }

        public VisibleChildren traverseAndFindVisibleChildren() {
            int childCount = getChildCount();
            final VisibleChildren visibleChildren = new VisibleChildren(getSpanCount());
            final int start = mPrimaryOrientation.getStartAfterPadding();
            final int end = mPrimaryOrientation.getEndAfterPadding();
            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                final int childStart = mPrimaryOrientation.getDecoratedStart(child);
                final int childEnd = mPrimaryOrientation.getDecoratedEnd(child);
                final boolean fullyVisible = childStart >= start && childEnd <= end;
                final boolean hidden = childEnd <= start || childStart >= end;
                if (hidden) {
                    continue;
                }
                final int position = getPosition(child);
                final int span = getLp(child).getSpanIndex();
                if (fullyVisible) {
                    if (position < visibleChildren.firstFullyVisiblePositions[span] ||
                            visibleChildren.firstFullyVisiblePositions[span]
                                    == RecyclerView.NO_POSITION) {
                        visibleChildren.firstFullyVisiblePositions[span] = position;
                    }

                    if (position > visibleChildren.lastFullyVisiblePositions[span]) {
                        visibleChildren.lastFullyVisiblePositions[span] = position;
                    }
                }

                if (position < visibleChildren.firstVisiblePositions[span] ||
                        visibleChildren.firstVisiblePositions[span] == RecyclerView.NO_POSITION) {
                    visibleChildren.firstVisiblePositions[span] = position;
                }

                if (position > visibleChildren.lastVisiblePositions[span]) {
                    visibleChildren.lastVisiblePositions[span] = position;
                }

            }
            return visibleChildren;
        }

        Map<Item, Rect> collectChildCoordinates() throws Throwable {
            final Map<Item, Rect> items = new LinkedHashMap<Item, Rect>();
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final int childCount = getChildCount();
                    for (int i = 0; i < childCount; i++) {
                        View child = getChildAt(i);
                        // do it if and only if child is visible
                        if (child.getRight() < 0 || child.getBottom() < 0 ||
                                child.getLeft() >= getWidth() || child.getTop() >= getHeight()) {
                            // invisible children may be drawn in cases like scrolling so we should
                            // ignore them
                            continue;
                        }
                        LayoutParams lp = (LayoutParams) child
                                .getLayoutParams();
                        TestViewHolder vh = (TestViewHolder) lp.mViewHolder;
                        items.put(vh.mBindedItem, getViewBounds(child));
                    }
                }
            });
            return items;
        }


        public void setFakeRtl(Boolean fakeRtl) {
            mFakeRTL = fakeRtl;
            try {
                requestLayoutOnUIThread(mRecyclerView);
            } catch (Throwable throwable) {
                postExceptionToInstrumentation(throwable);
            }
        }
    }

    static class VisibleChildren {

        int[] firstVisiblePositions;

        int[] firstFullyVisiblePositions;

        int[] lastVisiblePositions;

        int[] lastFullyVisiblePositions;

        VisibleChildren(int spanCount) {
            firstFullyVisiblePositions = new int[spanCount];
            firstVisiblePositions = new int[spanCount];
            lastVisiblePositions = new int[spanCount];
            lastFullyVisiblePositions = new int[spanCount];
            for (int i = 0; i < spanCount; i++) {
                firstFullyVisiblePositions[i] = RecyclerView.NO_POSITION;
                firstVisiblePositions[i] = RecyclerView.NO_POSITION;
                lastVisiblePositions[i] = RecyclerView.NO_POSITION;
                lastFullyVisiblePositions[i] = RecyclerView.NO_POSITION;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            VisibleChildren that = (VisibleChildren) o;

            if (!Arrays.equals(firstFullyVisiblePositions, that.firstFullyVisiblePositions)) {
                return false;
            }
            if (!Arrays.equals(firstVisiblePositions, that.firstVisiblePositions)) {
                return false;
            }
            if (!Arrays.equals(lastFullyVisiblePositions, that.lastFullyVisiblePositions)) {
                return false;
            }
            if (!Arrays.equals(lastVisiblePositions, that.lastVisiblePositions)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = firstVisiblePositions != null ? Arrays.hashCode(firstVisiblePositions) : 0;
            result = 31 * result + (firstFullyVisiblePositions != null ? Arrays
                    .hashCode(firstFullyVisiblePositions) : 0);
            result = 31 * result + (lastVisiblePositions != null ? Arrays
                    .hashCode(lastVisiblePositions)
                    : 0);
            result = 31 * result + (lastFullyVisiblePositions != null ? Arrays
                    .hashCode(lastFullyVisiblePositions) : 0);
            return result;
        }

        @Override
        public String toString() {
            return "VisibleChildren{" +
                    "firstVisiblePositions=" + Arrays.toString(firstVisiblePositions) +
                    ", firstFullyVisiblePositions=" + Arrays.toString(firstFullyVisiblePositions) +
                    ", lastVisiblePositions=" + Arrays.toString(lastVisiblePositions) +
                    ", lastFullyVisiblePositions=" + Arrays.toString(lastFullyVisiblePositions) +
                    '}';
        }
    }

    class GridTestAdapter extends TestAdapter {

        int mOrientation;

        // original ids of items that should be full span
        HashSet<Integer> mFullSpanItems = new HashSet<Integer>();

        private boolean mViewsHaveEqualSize = false; // size in the scrollable direction

        private OnBindHandler mOnBindHandler;

        GridTestAdapter(int count, int orientation) {
            super(count);
            mOrientation = orientation;
        }

        @Override
        public void offsetOriginalIndices(int start, int offset) {
            if (mFullSpanItems.size() > 0) {
                HashSet<Integer> old = mFullSpanItems;
                mFullSpanItems = new HashSet<Integer>();
                for (Integer i : old) {
                    if (i < start) {
                        mFullSpanItems.add(i);
                    } else if (offset > 0 || (start + Math.abs(offset)) <= i) {
                        mFullSpanItems.add(i + offset);
                    } else if (DEBUG) {
                        Log.d(TAG, "removed full span item " + i);
                    }
                }
            }
            super.offsetOriginalIndices(start, offset);
        }

        @Override
        public void onBindViewHolder(TestViewHolder holder,
                int position) {
            super.onBindViewHolder(holder, position);
            Item item = mItems.get(position);
            final int minSize = mViewsHaveEqualSize ? 200 : 200 + 20 * (position % 10);
            if (mOrientation == OrientationHelper.HORIZONTAL) {
                holder.itemView.setMinimumWidth(minSize);
            } else {
                holder.itemView.setMinimumHeight(minSize);
            }
            RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) holder.itemView
                    .getLayoutParams();
            if (lp instanceof LayoutParams) {
                ((LayoutParams) lp).setFullSpan(mFullSpanItems.contains(item.mAdapterIndex));
            } else {
                LayoutParams slp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                holder.itemView.setLayoutParams(slp);
                slp.setFullSpan(mFullSpanItems.contains(item.mAdapterIndex));
                lp = slp;
            }
            lp.topMargin = 3;
            lp.leftMargin = 5;
            lp.rightMargin = 7;
            lp.bottomMargin = 9;

            if (mOnBindHandler != null) {
                mOnBindHandler.onBoundItem(holder, position);
            }
        }
    }

    static interface OnBindHandler {
        void onBoundItem(TestViewHolder vh, int postion);
    }

    static class Config implements Cloneable {

        private static final int DEFAULT_ITEM_COUNT = 300;

        int mOrientation = OrientationHelper.VERTICAL;

        boolean mReverseLayout = false;

        int mSpanCount = 3;

        int mGapStrategy = GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS;

        int mItemCount = DEFAULT_ITEM_COUNT;

        Config(int orientation, boolean reverseLayout, int spanCount, int gapStrategy) {
            mOrientation = orientation;
            mReverseLayout = reverseLayout;
            mSpanCount = spanCount;
            mGapStrategy = gapStrategy;
        }

        public Config() {

        }

        Config orientation(int orientation) {
            mOrientation = orientation;
            return this;
        }

        Config reverseLayout(boolean reverseLayout) {
            mReverseLayout = reverseLayout;
            return this;
        }

        Config spanCount(int spanCount) {
            mSpanCount = spanCount;
            return this;
        }

        Config gapStrategy(int gapStrategy) {
            mGapStrategy = gapStrategy;
            return this;
        }

        public Config itemCount(int itemCount) {
            mItemCount = itemCount;
            return this;
        }

        @Override
        public String toString() {
            return "[CONFIG:" +
                    " span:" + mSpanCount + "," +
                    " orientation:" + (mOrientation == HORIZONTAL ? "horz," : "vert,") +
                    " reverse:" + (mReverseLayout ? "T" : "F") +
                    " itemCount:" + mItemCount +
                    " gap strategy: " + gapStrategyName(mGapStrategy);
        }

        private static String gapStrategyName(int gapStrategy) {
            switch (gapStrategy) {
                case GAP_HANDLING_NONE:
                    return "none";
                case GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS:
                    return "move spans";
            }
            return "gap strategy: unknown";
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }

    private interface PostLayoutRunnable {

        void run() throws Throwable;

        String describe();
    }

}
