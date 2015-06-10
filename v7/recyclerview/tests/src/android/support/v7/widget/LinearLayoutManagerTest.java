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

import android.content.Context;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.AccessibilityDelegateCompat;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.support.v4.view.accessibility.AccessibilityRecordCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;

import static android.support.v7.widget.LayoutState.LAYOUT_END;
import static android.support.v7.widget.LayoutState.LAYOUT_START;
import static android.support.v7.widget.LinearLayoutManager.HORIZONTAL;
import static android.support.v7.widget.LinearLayoutManager.VERTICAL;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Includes tests for {@link LinearLayoutManager}.
 * <p>
 * Since most UI tests are not practical, these tests are focused on internal data representation
 * and stability of LinearLayoutManager in response to different events (state change, scrolling
 * etc) where it is very hard to do manual testing.
 */
public class LinearLayoutManagerTest extends BaseRecyclerViewInstrumentationTest {

    private static final boolean DEBUG = false;

    private static final String TAG = "LinearLayoutManagerTest";

    WrappedLinearLayoutManager mLayoutManager;

    TestAdapter mTestAdapter;

    final List<Config> mBaseVariations = new ArrayList<Config>();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        for (int orientation : new int[]{VERTICAL, HORIZONTAL}) {
            for (boolean reverseLayout : new boolean[]{false, true}) {
                for (boolean stackFromBottom : new boolean[]{false, true}) {
                    mBaseVariations.add(new Config(orientation, reverseLayout, stackFromBottom));
                }
            }
        }
    }

    protected List<Config> addConfigVariation(List<Config> base, String fieldName,
            Object... variations)
            throws CloneNotSupportedException, NoSuchFieldException, IllegalAccessException {
        List<Config> newConfigs = new ArrayList<Config>();
        Field field = Config.class.getDeclaredField(fieldName);
        for (Config config : base) {
            for (Object variation : variations) {
                Config newConfig = (Config) config.clone();
                field.set(newConfig, variation);
                newConfigs.add(newConfig);
            }
        }
        return newConfigs;
    }

    void setupByConfig(Config config, boolean waitForFirstLayout) throws Throwable {
        mRecyclerView = inflateWrappedRV();

        mRecyclerView.setHasFixedSize(true);
        mTestAdapter = config.mTestAdapter == null ? new TestAdapter(config.mItemCount)
                : config.mTestAdapter;
        mRecyclerView.setAdapter(mTestAdapter);
        mLayoutManager = new WrappedLinearLayoutManager(getActivity(), config.mOrientation,
                config.mReverseLayout);
        mLayoutManager.setStackFromEnd(config.mStackFromEnd);
        mLayoutManager.setRecycleChildrenOnDetach(config.mRecycleChildrenOnDetach);
        mRecyclerView.setLayoutManager(mLayoutManager);
        if (waitForFirstLayout) {
            waitForFirstLayout();
        }
    }

    public void testRemoveAnchorItem() throws Throwable {
        removeAnchorItemTest(
                new Config().orientation(VERTICAL).stackFromBottom(false).reverseLayout(
                        false), 100, 0);
    }

    public void testRemoveAnchorItemReverse() throws Throwable {
        removeAnchorItemTest(
                new Config().orientation(VERTICAL).stackFromBottom(false).reverseLayout(true), 100,
                0);
    }

    public void testRemoveAnchorItemStackFromEnd() throws Throwable {
        removeAnchorItemTest(
                new Config().orientation(VERTICAL).stackFromBottom(true).reverseLayout(false), 100,
                99);
    }

    public void testRemoveAnchorItemStackFromEndAndReverse() throws Throwable {
        removeAnchorItemTest(
                new Config().orientation(VERTICAL).stackFromBottom(true).reverseLayout(true), 100,
                99);
    }

    public void testRemoveAnchorItemHorizontal() throws Throwable {
        removeAnchorItemTest(
                new Config().orientation(HORIZONTAL).stackFromBottom(false).reverseLayout(
                        false), 100, 0);
    }

    public void testRemoveAnchorItemReverseHorizontal() throws Throwable {
        removeAnchorItemTest(
                new Config().orientation(HORIZONTAL).stackFromBottom(false).reverseLayout(true),
                100, 0);
    }

    public void testRemoveAnchorItemStackFromEndHorizontal() throws Throwable {
        removeAnchorItemTest(
                new Config().orientation(HORIZONTAL).stackFromBottom(true).reverseLayout(false),
                100, 99);
    }

    public void testRemoveAnchorItemStackFromEndAndReverseHorizontal() throws Throwable {
        removeAnchorItemTest(
                new Config().orientation(HORIZONTAL).stackFromBottom(true).reverseLayout(true), 100,
                99);
    }

    /**
     * This tests a regression where predictive animations were not working as expected when the
     * first item is removed and there aren't any more items to add from that direction.
     * First item refers to the default anchor item.
     */
    public void removeAnchorItemTest(final Config config, int adapterSize,
            final int removePos) throws Throwable {
        config.adapter(new TestAdapter(adapterSize) {
            @Override
            public void onBindViewHolder(TestViewHolder holder,
                    int position) {
                super.onBindViewHolder(holder, position);
                ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
                if (!(lp instanceof ViewGroup.MarginLayoutParams)) {
                    lp = new ViewGroup.MarginLayoutParams(0, 0);
                    holder.itemView.setLayoutParams(lp);
                }
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
                final int maxSize;
                if (config.mOrientation == HORIZONTAL) {
                    maxSize = mRecyclerView.getWidth();
                    mlp.height = ViewGroup.MarginLayoutParams.FILL_PARENT;
                } else {
                    maxSize = mRecyclerView.getHeight();
                    mlp.width = ViewGroup.MarginLayoutParams.FILL_PARENT;
                }

                final int desiredSize;
                if (position == removePos) {
                    // make it large
                    desiredSize = maxSize / 4;
                } else {
                    // make it small
                    desiredSize = maxSize / 8;
                }
                if (config.mOrientation == HORIZONTAL) {
                    mlp.width = desiredSize;
                } else {
                    mlp.height = desiredSize;
                }
            }
        });
        setupByConfig(config, true);
        final int childCount = mLayoutManager.getChildCount();
        RecyclerView.ViewHolder toBeRemoved = null;
        List<RecyclerView.ViewHolder> toBeMoved = new ArrayList<RecyclerView.ViewHolder>();
        for (int i = 0; i < childCount; i++) {
            View child = mLayoutManager.getChildAt(i);
            RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(child);
            if (holder.getAdapterPosition() == removePos) {
                toBeRemoved = holder;
            } else {
                toBeMoved.add(holder);
            }
        }
        assertNotNull("test sanity", toBeRemoved);
        assertEquals("test sanity", childCount - 1, toBeMoved.size());
        LoggingItemAnimator loggingItemAnimator = new LoggingItemAnimator();
        mRecyclerView.setItemAnimator(loggingItemAnimator);
        loggingItemAnimator.reset();
        loggingItemAnimator.expectRunPendingAnimationsCall(1);
        mLayoutManager.expectLayouts(2);
        mTestAdapter.deleteAndNotify(removePos, 1);
        mLayoutManager.waitForLayout(1);
        loggingItemAnimator.waitForPendingAnimationsCall(2);
        assertTrue("removed child should receive remove animation",
                loggingItemAnimator.mRemoveVHs.contains(toBeRemoved));
        for (RecyclerView.ViewHolder vh : toBeMoved) {
            assertTrue("view holder should be in moved list",
                    loggingItemAnimator.mMoveVHs.contains(vh));
        }
        List<RecyclerView.ViewHolder> newHolders = new ArrayList<RecyclerView.ViewHolder>();
        for (int i = 0; i < mLayoutManager.getChildCount(); i++) {
            View child = mLayoutManager.getChildAt(i);
            RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(child);
            if (toBeRemoved != holder && !toBeMoved.contains(holder)) {
                newHolders.add(holder);
            }
        }
        assertTrue("some new children should show up for the new space", newHolders.size() > 0);
        assertEquals("no items should receive animate add since they are not new", 0,
                loggingItemAnimator.mAddVHs.size());
        for (RecyclerView.ViewHolder holder : newHolders) {
            assertTrue("new holder should receive a move animation",
                    loggingItemAnimator.mMoveVHs.contains(holder));
        }
        assertTrue("control against adding too many children due to bad layout state preparation."
                        + " initial:" + childCount + ", current:" + mRecyclerView.getChildCount(),
                mRecyclerView.getChildCount() <= childCount + 3 /*1 for removed view, 2 for its size*/);
    }

    public void testKeepFocusOnRelayout() throws Throwable {
        setupByConfig(new Config(VERTICAL, false, false).itemCount(500), true);
        int center = (mLayoutManager.findLastVisibleItemPosition()
                - mLayoutManager.findFirstVisibleItemPosition()) / 2;
        final RecyclerView.ViewHolder vh = mRecyclerView.findViewHolderForLayoutPosition(center);
        final int top = mLayoutManager.mOrientationHelper.getDecoratedStart(vh.itemView);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                vh.itemView.requestFocus();
            }
        });
        assertTrue("view should have the focus", vh.itemView.hasFocus());
        // add a bunch of items right before that view, make sure it keeps its position
        mLayoutManager.expectLayouts(2);
        final int childCountToAdd = mRecyclerView.getChildCount() * 2;
        mTestAdapter.addAndNotify(center, childCountToAdd);
        center += childCountToAdd; // offset item
        mLayoutManager.waitForLayout(2);
        mLayoutManager.waitForAnimationsToEnd(20);
        final RecyclerView.ViewHolder postVH = mRecyclerView.findViewHolderForLayoutPosition(center);
        assertNotNull("focused child should stay in layout", postVH);
        assertSame("same view holder should be kept for unchanged child", vh, postVH);
        assertEquals("focused child's screen position should stay unchanged", top,
                mLayoutManager.mOrientationHelper.getDecoratedStart(postVH.itemView));
    }

    public void testKeepFullFocusOnResize() throws Throwable {
        keepFocusOnResizeTest(new Config(VERTICAL, false, false).itemCount(500), true);
    }

    public void testKeepPartialFocusOnResize() throws Throwable {
        keepFocusOnResizeTest(new Config(VERTICAL, false, false).itemCount(500), false);
    }

    public void testKeepReverseFullFocusOnResize() throws Throwable {
        keepFocusOnResizeTest(new Config(VERTICAL, true, false).itemCount(500), true);
    }

    public void testKeepReversePartialFocusOnResize() throws Throwable {
        keepFocusOnResizeTest(new Config(VERTICAL, true, false).itemCount(500), false);
    }

    public void testKeepStackFromEndFullFocusOnResize() throws Throwable {
        keepFocusOnResizeTest(new Config(VERTICAL, false, true).itemCount(500), true);
    }

    public void testKeepStackFromEndPartialFocusOnResize() throws Throwable {
        keepFocusOnResizeTest(new Config(VERTICAL, false, true).itemCount(500), false);
    }

    public void keepFocusOnResizeTest(final Config config, boolean fullyVisible) throws Throwable {
        setupByConfig(config, true);
        final int targetPosition;
        if (config.mStackFromEnd) {
            targetPosition = mLayoutManager.findFirstVisibleItemPosition();
        } else {
            targetPosition = mLayoutManager.findLastVisibleItemPosition();
        }
        final OrientationHelper helper = mLayoutManager.mOrientationHelper;
        final RecyclerView.ViewHolder vh = mRecyclerView
                .findViewHolderForLayoutPosition(targetPosition);

        // scroll enough to offset the child
        int startMargin = helper.getDecoratedStart(vh.itemView) -
                helper.getStartAfterPadding();
        int endMargin = helper.getEndAfterPadding() -
                helper.getDecoratedEnd(vh.itemView);
        Log.d(TAG, "initial start margin " + startMargin + " , end margin:" + endMargin);
        requestFocus(vh.itemView);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertTrue("view should gain the focus", vh.itemView.hasFocus());
            }
        });
        do {
            Thread.sleep(100);
        } while (mRecyclerView.getScrollState() != RecyclerView.SCROLL_STATE_IDLE);
        // scroll enough to offset the child
        startMargin = helper.getDecoratedStart(vh.itemView) -
                helper.getStartAfterPadding();
        endMargin = helper.getEndAfterPadding() -
                helper.getDecoratedEnd(vh.itemView);

        Log.d(TAG, "start margin " + startMargin + " , end margin:" + endMargin);
        assertTrue("View should become fully visible", startMargin >= 0 && endMargin >= 0);

        int expectedOffset = 0;
        boolean offsetAtStart = false;
        if (!fullyVisible) {
            // move it a bit such that it is no more fully visible
            final int childSize = helper
                    .getDecoratedMeasurement(vh.itemView);
            expectedOffset = childSize / 3;
            if (startMargin < endMargin) {
                scrollBy(expectedOffset);
                offsetAtStart = true;
            } else {
                scrollBy(-expectedOffset);
                offsetAtStart = false;
            }
            startMargin = helper.getDecoratedStart(vh.itemView) -
                    helper.getStartAfterPadding();
            endMargin = helper.getEndAfterPadding() -
                    helper.getDecoratedEnd(vh.itemView);
            assertTrue("test sanity, view should not be fully visible", startMargin < 0
                    || endMargin < 0);
        }

        mLayoutManager.expectLayouts(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                final ViewGroup.LayoutParams layoutParams = mRecyclerView.getLayoutParams();
                if (config.mOrientation == HORIZONTAL) {
                    layoutParams.width = mRecyclerView.getWidth() / 2;
                } else {
                    layoutParams.height = mRecyclerView.getHeight() / 2;
                }
                mRecyclerView.setLayoutParams(layoutParams);
            }
        });
        Thread.sleep(100);
        // add a bunch of items right before that view, make sure it keeps its position
        mLayoutManager.waitForLayout(2);
        mLayoutManager.waitForAnimationsToEnd(20);
        assertTrue("view should preserve the focus", vh.itemView.hasFocus());
        final RecyclerView.ViewHolder postVH = mRecyclerView
                .findViewHolderForLayoutPosition(targetPosition);
        assertNotNull("focused child should stay in layout", postVH);
        assertSame("same view holder should be kept for unchanged child", vh, postVH);
        View focused = postVH.itemView;

        startMargin = helper.getDecoratedStart(focused) - helper.getStartAfterPadding();
        endMargin = helper.getEndAfterPadding() - helper.getDecoratedEnd(focused);

        assertTrue("focused child should be somewhat visible",
                helper.getDecoratedStart(focused) < helper.getEndAfterPadding()
                        && helper.getDecoratedEnd(focused) > helper.getStartAfterPadding());
        if (fullyVisible) {
            assertTrue("focused child end should stay fully visible",
                    endMargin >= 0);
            assertTrue("focused child start should stay fully visible",
                    startMargin >= 0);
        } else {
            if (offsetAtStart) {
                assertTrue("start should preserve its offset", startMargin < 0);
                assertTrue("end should be visible", endMargin >= 0);
            } else {
                assertTrue("end should preserve its offset", endMargin < 0);
                assertTrue("start should be visible", startMargin >= 0);
            }
        }
    }

    public void testResize() throws Throwable {
        for(Config config : addConfigVariation(mBaseVariations, "mItemCount", 5
                , Config.DEFAULT_ITEM_COUNT)) {
            stackFromEndTest(config);
            removeRecyclerView();
        }
    }

    public void testScrollToPositionWithOffset() throws Throwable {
        for (Config config : mBaseVariations) {
            scrollToPositionWithOffsetTest(config.itemCount(300));
            removeRecyclerView();
        }
    }

    public void scrollToPositionWithOffsetTest(Config config) throws Throwable {
        setupByConfig(config, true);
        OrientationHelper orientationHelper = OrientationHelper
                .createOrientationHelper(mLayoutManager, config.mOrientation);
        Rect layoutBounds = getDecoratedRecyclerViewBounds();
        // try scrolling towards head, should not affect anything
        Map<Item, Rect> before = mLayoutManager.collectChildCoordinates();
        if (config.mStackFromEnd) {
            scrollToPositionWithOffset(mTestAdapter.getItemCount() - 1,
                    mLayoutManager.mOrientationHelper.getEnd() - 500);
        } else {
            scrollToPositionWithOffset(0, 20);
        }
        assertRectSetsEqual(config + " trying to over scroll with offset should be no-op",
                before, mLayoutManager.collectChildCoordinates());
        // try offsetting some visible children
        int testCount = 10;
        while (testCount-- > 0) {
            // get middle child
            final View child = mLayoutManager.getChildAt(mLayoutManager.getChildCount() / 2);
            final int position = mRecyclerView.getChildLayoutPosition(child);
            final int startOffset = config.mReverseLayout ?
                    orientationHelper.getEndAfterPadding() - orientationHelper
                            .getDecoratedEnd(child)
                    : orientationHelper.getDecoratedStart(child) - orientationHelper
                            .getStartAfterPadding();
            final int scrollOffset = config.mStackFromEnd ? startOffset + startOffset / 2
                    : startOffset / 2;
            mLayoutManager.expectLayouts(1);
            scrollToPositionWithOffset(position, scrollOffset);
            mLayoutManager.waitForLayout(2);
            final int finalOffset = config.mReverseLayout ?
                    orientationHelper.getEndAfterPadding() - orientationHelper
                            .getDecoratedEnd(child)
                    : orientationHelper.getDecoratedStart(child) - orientationHelper
                            .getStartAfterPadding();
            assertEquals(config + " scroll with offset on a visible child should work fine " +
                    " offset:" + finalOffset + " , existing offset:" + startOffset + ", "
                            + "child " + position,
                    scrollOffset, finalOffset);
        }

        // try scrolling to invisible children
        testCount = 10;
        // we test above and below, one by one
        int offsetMultiplier = -1;
        while (testCount-- > 0) {
            final TargetTuple target = findInvisibleTarget(config);
            final String logPrefix = config + " " + target;
            mLayoutManager.expectLayouts(1);
            final int offset = offsetMultiplier
                    * orientationHelper.getDecoratedMeasurement(mLayoutManager.getChildAt(0)) / 3;
            scrollToPositionWithOffset(target.mPosition, offset);
            mLayoutManager.waitForLayout(2);
            final View child = mLayoutManager.findViewByPosition(target.mPosition);
            assertNotNull(logPrefix + " scrolling to a mPosition with offset " + offset
                    + " should layout it", child);
            final Rect bounds = mLayoutManager.getViewBounds(child);
            if (DEBUG) {
                Log.d(TAG, logPrefix + " post scroll to invisible mPosition " + bounds + " in "
                        + layoutBounds + " with offset " + offset);
            }

            if (config.mReverseLayout) {
                assertEquals(logPrefix + " when scrolling with offset to an invisible in reverse "
                                + "layout, its end should align with recycler view's end - offset",
                        orientationHelper.getEndAfterPadding() - offset,
                        orientationHelper.getDecoratedEnd(child)
                );
            } else {
                assertEquals(logPrefix + " when scrolling with offset to an invisible child in normal"
                                + " layout its start should align with recycler view's start + "
                                + "offset",
                        orientationHelper.getStartAfterPadding() + offset,
                        orientationHelper.getDecoratedStart(child)
                );
            }
            offsetMultiplier *= -1;
        }
    }

    private TargetTuple findInvisibleTarget(Config config) {
        int minPosition = Integer.MAX_VALUE, maxPosition = Integer.MIN_VALUE;
        for (int i = 0; i < mLayoutManager.getChildCount(); i++) {
            View child = mLayoutManager.getChildAt(i);
            int position = mRecyclerView.getChildLayoutPosition(child);
            if (position < minPosition) {
                minPosition = position;
            }
            if (position > maxPosition) {
                maxPosition = position;
            }
        }
        final int tailTarget = maxPosition +
                (mRecyclerView.getAdapter().getItemCount() - maxPosition) / 2;
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

    public void stackFromEndTest(final Config config) throws Throwable {
        final FrameLayout container = getRecyclerViewContainer();
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                container.setPadding(0, 0, 0, 0);
            }
        });

        setupByConfig(config, true);
        int lastVisibleItemPosition = mLayoutManager.findLastVisibleItemPosition();
        int firstVisibleItemPosition = mLayoutManager.findFirstVisibleItemPosition();
        int lastCompletelyVisibleItemPosition = mLayoutManager.findLastCompletelyVisibleItemPosition();
        int firstCompletelyVisibleItemPosition = mLayoutManager.findFirstCompletelyVisibleItemPosition();
        mLayoutManager.expectLayouts(1);
        // resize the recycler view to half
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (config.mOrientation == HORIZONTAL) {
                    container.setPadding(0, 0, container.getWidth() / 2, 0);
                } else {
                    container.setPadding(0, 0, 0, container.getWidth() / 2);
                }
            }
        });
        mLayoutManager.waitForLayout(1);
        if (config.mStackFromEnd) {
            assertEquals("[" + config + "]: last visible position should not change.",
                    lastVisibleItemPosition, mLayoutManager.findLastVisibleItemPosition());
            assertEquals("[" + config + "]: last completely visible position should not change",
                    lastCompletelyVisibleItemPosition,
                    mLayoutManager.findLastCompletelyVisibleItemPosition());
        } else {
            assertEquals("[" + config + "]: first visible position should not change.",
                    firstVisibleItemPosition, mLayoutManager.findFirstVisibleItemPosition());
            assertEquals("[" + config + "]: last completely visible position should not change",
                    firstCompletelyVisibleItemPosition,
                    mLayoutManager.findFirstCompletelyVisibleItemPosition());
        }
    }

    public void testScrollToPositionWithPredictive() throws Throwable {
        scrollToPositionWithPredictive(0, LinearLayoutManager.INVALID_OFFSET);
        removeRecyclerView();
        scrollToPositionWithPredictive(3, 20);
        removeRecyclerView();
        scrollToPositionWithPredictive(Config.DEFAULT_ITEM_COUNT / 2,
                LinearLayoutManager.INVALID_OFFSET);
        removeRecyclerView();
        scrollToPositionWithPredictive(Config.DEFAULT_ITEM_COUNT / 2, 10);
    }

    public void scrollToPositionWithPredictive(final int scrollPosition, final int scrollOffset)
            throws Throwable {
        setupByConfig(new Config(VERTICAL, false, false), true);

        mLayoutManager.mOnLayoutListener = new OnLayoutListener() {
            @Override
            void after(RecyclerView.Recycler recycler, RecyclerView.State state) {
                if (state.isPreLayout()) {
                    assertEquals("pending scroll position should still be pending",
                            scrollPosition, mLayoutManager.mPendingScrollPosition);
                    if (scrollOffset != LinearLayoutManager.INVALID_OFFSET) {
                        assertEquals("pending scroll position offset should still be pending",
                                scrollOffset, mLayoutManager.mPendingScrollPositionOffset);
                    }
                } else {
                    RecyclerView.ViewHolder vh =
                            mRecyclerView.findViewHolderForLayoutPosition(scrollPosition);
                    assertNotNull("scroll to position should work", vh);
                    if (scrollOffset != LinearLayoutManager.INVALID_OFFSET) {
                        assertEquals("scroll offset should be applied properly",
                                mLayoutManager.getPaddingTop() + scrollOffset +
                                        ((RecyclerView.LayoutParams) vh.itemView
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
                    mTestAdapter.addAndNotify(0, 1);
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

    private void waitForFirstLayout() throws Throwable {
        mLayoutManager.expectLayouts(1);
        setRecyclerView(mRecyclerView);
        mLayoutManager.waitForLayout(2);
    }

    public void testRecycleDuringAnimations() throws Throwable {
        final AtomicInteger childCount = new AtomicInteger(0);
        final TestAdapter adapter = new TestAdapter(300) {
            @Override
            public TestViewHolder onCreateViewHolder(ViewGroup parent,
                    int viewType) {
                final int cnt = childCount.incrementAndGet();
                final TestViewHolder testViewHolder = super.onCreateViewHolder(parent, viewType);
                if (DEBUG) {
                    Log.d(TAG, "CHILD_CNT(create):" + cnt + ", " + testViewHolder);
                }
                return testViewHolder;
            }
        };
        setupByConfig(new Config(VERTICAL, false, false).itemCount(300)
                .adapter(adapter), true);

        final RecyclerView.RecycledViewPool pool = new RecyclerView.RecycledViewPool() {
            @Override
            public void putRecycledView(RecyclerView.ViewHolder scrap) {
                super.putRecycledView(scrap);
                int cnt = childCount.decrementAndGet();
                if (DEBUG) {
                    Log.d(TAG, "CHILD_CNT(put):" + cnt + ", " + scrap);
                }
            }

            @Override
            public RecyclerView.ViewHolder getRecycledView(int viewType) {
                final RecyclerView.ViewHolder recycledView = super.getRecycledView(viewType);
                if (recycledView != null) {
                    final int cnt = childCount.incrementAndGet();
                    if (DEBUG) {
                        Log.d(TAG, "CHILD_CNT(get):" + cnt + ", " + recycledView);
                    }
                }
                return recycledView;
            }
        };
        pool.setMaxRecycledViews(mTestAdapter.getItemViewType(0), 500);
        mRecyclerView.setRecycledViewPool(pool);


        // now keep adding children to trigger more children being created etc.
        for (int i = 0; i < 100; i ++) {
            adapter.addAndNotify(15, 1);
            Thread.sleep(15);
        }
        getInstrumentation().waitForIdleSync();
        waitForAnimations(2);
        assertEquals("Children count should add up", childCount.get(),
                mRecyclerView.getChildCount() + mRecyclerView.mRecycler.mCachedViews.size());

        // now trigger lots of add again, followed by a scroll to position
        for (int i = 0; i < 100; i ++) {
            adapter.addAndNotify(5 + (i % 3) * 3, 1);
            Thread.sleep(25);
        }
        smoothScrollToPosition(mLayoutManager.findLastVisibleItemPosition() + 20);
        waitForAnimations(2);
        getInstrumentation().waitForIdleSync();
        assertEquals("Children count should add up", childCount.get(),
                mRecyclerView.getChildCount() + mRecyclerView.mRecycler.mCachedViews.size());
    }


    public void testGetFirstLastChildrenTest() throws Throwable {
        for (Config config : mBaseVariations) {
            getFirstLastChildrenTest(config);
        }
    }

    public void testDontRecycleChildrenOnDetach() throws Throwable {
        setupByConfig(new Config().recycleChildrenOnDetach(false), true);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                int recyclerSize = mRecyclerView.mRecycler.getRecycledViewPool().size();
                mRecyclerView.setLayoutManager(new TestLayoutManager());
                assertEquals("No views are recycled", recyclerSize,
                        mRecyclerView.mRecycler.getRecycledViewPool().size());
            }
        });
    }

    public void testRecycleChildrenOnDetach() throws Throwable {
        setupByConfig(new Config().recycleChildrenOnDetach(true), true);
        final int childCount = mLayoutManager.getChildCount();
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                int recyclerSize = mRecyclerView.mRecycler.getRecycledViewPool().size();
                mRecyclerView.mRecycler.getRecycledViewPool().setMaxRecycledViews(
                        mTestAdapter.getItemViewType(0), recyclerSize + childCount);
                mRecyclerView.setLayoutManager(new TestLayoutManager());
                assertEquals("All children should be recycled", childCount + recyclerSize,
                        mRecyclerView.mRecycler.getRecycledViewPool().size());
            }
        });
    }

    public void getFirstLastChildrenTest(final Config config) throws Throwable {
        setupByConfig(config, true);
        Runnable viewInBoundsTest = new Runnable() {
            @Override
            public void run() {
                VisibleChildren visibleChildren = mLayoutManager.traverseAndFindVisibleChildren();
                final String boundsLog = mLayoutManager.getBoundsLog();
                assertEquals(config + ":\nfirst visible child should match traversal result\n"
                                + boundsLog, visibleChildren.firstVisiblePosition,
                        mLayoutManager.findFirstVisibleItemPosition()
                );
                assertEquals(
                        config + ":\nfirst fully visible child should match traversal result\n"
                                + boundsLog, visibleChildren.firstFullyVisiblePosition,
                        mLayoutManager.findFirstCompletelyVisibleItemPosition()
                );

                assertEquals(config + ":\nlast visible child should match traversal result\n"
                                + boundsLog, visibleChildren.lastVisiblePosition,
                        mLayoutManager.findLastVisibleItemPosition()
                );
                assertEquals(
                        config + ":\nlast fully visible child should match traversal result\n"
                                + boundsLog, visibleChildren.lastFullyVisiblePosition,
                        mLayoutManager.findLastCompletelyVisibleItemPosition()
                );
            }
        };
        runTestOnUiThread(viewInBoundsTest);
        // smooth scroll to end of the list and keep testing meanwhile. This will test pre-caching
        // case
        final int scrollPosition = config.mStackFromEnd ? 0 : mTestAdapter.getItemCount();
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
        mTestAdapter.deleteAndNotify(0, mTestAdapter.getItemCount());
        mLayoutManager.waitForLayout(2);
        // test empty case
        runTestOnUiThread(viewInBoundsTest);
        // set a new adapter with huge items to test full bounds check
        mLayoutManager.expectLayouts(1);
        final int totalSpace = mLayoutManager.mOrientationHelper.getTotalSpace();
        final TestAdapter newAdapter = new TestAdapter(100) {
            @Override
            public void onBindViewHolder(TestViewHolder holder,
                    int position) {
                super.onBindViewHolder(holder, position);
                if (config.mOrientation == HORIZONTAL) {
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
                        scrollToPosition(mTestAdapter.getItemCount() * 3 / 4);
                        mLayoutManager.waitForLayout(2);
                    }

                    @Override
                    public String describe() {
                        return "scroll to position";
                    }
                },
                new PostLayoutRunnable() {
                    @Override
                    public void run() throws Throwable {
                        mLayoutManager.expectLayouts(1);
                        scrollToPositionWithOffset(mTestAdapter.getItemCount() * 1 / 3,
                                50);
                        mLayoutManager.waitForLayout(2);
                    }

                    @Override
                    public String describe() {
                        return "scroll to position with positive offset";
                    }
                },
                new PostLayoutRunnable() {
                    @Override
                    public void run() throws Throwable {
                        mLayoutManager.expectLayouts(1);
                        scrollToPositionWithOffset(mTestAdapter.getItemCount() * 2 / 3,
                                -10);  // Some tests break if this value is below the item height.
                        mLayoutManager.waitForLayout(2);
                    }

                    @Override
                    public String describe() {
                        return "scroll to position with negative offset";
                    }
                }
        };

        PostRestoreRunnable[] postRestoreOptions = new PostRestoreRunnable[]{
                new PostRestoreRunnable() {
                    @Override
                    public String describe() {
                        return "Doing nothing";
                    }
                },
                new PostRestoreRunnable() {
                    @Override
                    void onAfterRestore(Config config) throws Throwable {
                        // update config as well so that restore assertions will work
                        config.mOrientation = 1 - config.mOrientation;
                        mLayoutManager.setOrientation(config.mOrientation);
                    }

                    @Override
                    boolean shouldLayoutMatch(Config config) {
                        return config.mItemCount == 0;
                    }

                    @Override
                    public String describe() {
                        return "Changing orientation";
                    }
                },
                new PostRestoreRunnable() {
                    @Override
                    void onAfterRestore(Config config) throws Throwable {
                        config.mStackFromEnd = !config.mStackFromEnd;
                        mLayoutManager.setStackFromEnd(config.mStackFromEnd);
                    }

                    @Override
                    boolean shouldLayoutMatch(Config config) {
                        return true; //stack from end should not move items on change
                    }

                    @Override
                    public String describe() {
                        return "Changing stack from end";
                    }
                },
                new PostRestoreRunnable() {
                    @Override
                    void onAfterRestore(Config config) throws Throwable {
                        config.mReverseLayout = !config.mReverseLayout;
                        mLayoutManager.setReverseLayout(config.mReverseLayout);
                    }

                    @Override
                    boolean shouldLayoutMatch(Config config) {
                        return config.mItemCount == 0;
                    }

                    @Override
                    public String describe() {
                        return "Changing reverse layout";
                    }
                },
                new PostRestoreRunnable() {
                    @Override
                    void onAfterRestore(Config config) throws Throwable {
                        config.mRecycleChildrenOnDetach = !config.mRecycleChildrenOnDetach;
                        mLayoutManager.setRecycleChildrenOnDetach(config.mRecycleChildrenOnDetach);
                    }

                    @Override
                    boolean shouldLayoutMatch(Config config) {
                        return true;
                    }

                    @Override
                    String describe() {
                        return "Change should recycle children";
                    }
                },
                new PostRestoreRunnable() {
                    int position;
                    @Override
                    void onAfterRestore(Config config) throws Throwable {
                        position = mTestAdapter.getItemCount() / 2;
                        mLayoutManager.scrollToPosition(position);
                    }

                    @Override
                    boolean shouldLayoutMatch(Config config) {
                        return mTestAdapter.getItemCount() == 0;
                    }

                    @Override
                    String describe() {
                        return "Scroll to position " + position ;
                    }

                    @Override
                    void onAfterReLayout(Config config) {
                        if (mTestAdapter.getItemCount() > 0) {
                            assertEquals(config + ":scrolled view should be last completely visible",
                                    position,
                                    config.mStackFromEnd ?
                                            mLayoutManager.findLastCompletelyVisibleItemPosition()
                                        : mLayoutManager.findFirstCompletelyVisibleItemPosition());
                        }
                    }
                }
        };
        boolean[] waitForLayoutOptions = new boolean[]{true, false};
        boolean[] loadDataAfterRestoreOptions = new boolean[]{true, false};
        List<Config> variations = addConfigVariation(mBaseVariations, "mItemCount", 0, 300);
        variations = addConfigVariation(variations, "mRecycleChildrenOnDetach", true);
        for (Config config : variations) {
            for (PostLayoutRunnable postLayoutRunnable : postLayoutOptions) {
                for (boolean waitForLayout : waitForLayoutOptions) {
                    for (PostRestoreRunnable postRestoreRunnable : postRestoreOptions) {
                        for (boolean loadDataAfterRestore : loadDataAfterRestoreOptions) {
                            savedStateTest((Config) config.clone(), waitForLayout,
                                    loadDataAfterRestore, postLayoutRunnable, postRestoreRunnable);
                            removeRecyclerView();
                        }
                    }

                }
            }
        }
    }

    public void savedStateTest(Config config, boolean waitForLayout, boolean loadDataAfterRestore,
            PostLayoutRunnable postLayoutOperation, PostRestoreRunnable postRestoreOperation)
            throws Throwable {
        if (DEBUG) {
            Log.d(TAG, "testing saved state with wait for layout = " + waitForLayout + " config " +
                    config + " post layout action " + postLayoutOperation.describe() +
                    "post restore action " + postRestoreOperation.describe());
        }
        setupByConfig(config, false);
        if (waitForLayout) {
            waitForFirstLayout();
            postLayoutOperation.run();
        }
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

        final int itemCount = mTestAdapter.getItemCount();
        if (loadDataAfterRestore) {
            mTestAdapter.deleteAndNotify(0, itemCount);
        }

        RecyclerView restored = new RecyclerView(getActivity());
        // this config should be no op.
        mLayoutManager = new WrappedLinearLayoutManager(getActivity(),
                config.mOrientation, config.mReverseLayout);
        mLayoutManager.setStackFromEnd(config.mStackFromEnd);
        restored.setLayoutManager(mLayoutManager);
        // use the same adapter for Rect matching
        restored.setAdapter(mTestAdapter);
        restored.onRestoreInstanceState(savedState);

        if (loadDataAfterRestore) {
            mTestAdapter.addAndNotify(itemCount);
        }

        postRestoreOperation.onAfterRestore(config);
        assertEquals("Parcel reading should not go out of bounds", parcelSuffix,
                parcel.readString());
        mLayoutManager.expectLayouts(1);
        setRecyclerView(restored);
        mLayoutManager.waitForLayout(2);
        // calculate prefix here instead of above to include post restore changes
        final String logPrefix = config + "\npostLayout:" + postLayoutOperation.describe() +
                "\npostRestore:" + postRestoreOperation.describe() + "\n";
        assertEquals(logPrefix + " on saved state, reverse layout should be preserved",
                config.mReverseLayout, mLayoutManager.getReverseLayout());
        assertEquals(logPrefix + " on saved state, orientation should be preserved",
                config.mOrientation, mLayoutManager.getOrientation());
        assertEquals(logPrefix + " on saved state, stack from end should be preserved",
                config.mStackFromEnd, mLayoutManager.getStackFromEnd());
        if (waitForLayout) {
            final boolean strictItemEquality = !loadDataAfterRestore;
            if (postRestoreOperation.shouldLayoutMatch(config)) {
                assertRectSetsEqual(
                        logPrefix + ": on restore, previous view positions should be preserved",
                        before, mLayoutManager.collectChildCoordinates(), strictItemEquality);
            } else {
                assertRectSetsNotEqual(
                        logPrefix
                                + ": on restore with changes, previous view positions should NOT "
                                + "be preserved",
                        before, mLayoutManager.collectChildCoordinates(), strictItemEquality);
            }
            postRestoreOperation.onAfterReLayout(config);
        }
    }

    public void testScrollAndClear() throws Throwable {
        setupByConfig(new Config(), true);

        assertTrue("Children not laid out", mLayoutManager.collectChildCoordinates().size() > 0);

        mLayoutManager.expectLayouts(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLayoutManager.scrollToPositionWithOffset(1, 0);
                mTestAdapter.clearOnUIThread();
            }
        });
        mLayoutManager.waitForLayout(2);

        assertEquals("Remaining children", 0, mLayoutManager.collectChildCoordinates().size());
    }


    void scrollToPositionWithOffset(final int position, final int offset) throws Throwable {
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLayoutManager.scrollToPositionWithOffset(position, offset);
            }
        });
    }

    public void assertRectSetsNotEqual(String message, Map<Item, Rect> before,
            Map<Item, Rect> after, boolean strictItemEquality) {
        Throwable throwable = null;
        try {
            assertRectSetsEqual("NOT " + message, before, after, strictItemEquality);
        } catch (Throwable t) {
            throwable = t;
        }
        assertNotNull(message + "\ntwo layout should be different", throwable);
    }

    public void assertRectSetsEqual(String message, Map<Item, Rect> before, Map<Item, Rect> after) {
        assertRectSetsEqual(message, before, after, true);
    }

    public void assertRectSetsEqual(String message, Map<Item, Rect> before, Map<Item, Rect> after,
            boolean strictItemEquality) {
        StringBuilder sb = new StringBuilder();
        sb.append("checking rectangle equality.\n");
        sb.append("before:\n");
        for (Map.Entry<Item, Rect> entry : before.entrySet()) {
            sb.append(entry.getKey().mAdapterIndex + ":" + entry.getValue()).append("\n");
        }
        sb.append("after:\n");
        for (Map.Entry<Item, Rect> entry : after.entrySet()) {
            sb.append(entry.getKey().mAdapterIndex + ":" + entry.getValue()).append("\n");
        }
        message = message + "\n" + sb.toString();
        assertEquals(message + ":\nitem counts should be equal", before.size()
                , after.size());
        for (Map.Entry<Item, Rect> entry : before.entrySet()) {
            final Item beforeItem = entry.getKey();
            Rect afterRect = null;
            if (strictItemEquality) {
                afterRect = after.get(beforeItem);
                assertNotNull(message + ":\nSame item should be visible after simple re-layout",
                        afterRect);
            } else {
                for (Map.Entry<Item, Rect> afterEntry : after.entrySet()) {
                    final Item afterItem = afterEntry.getKey();
                    if (afterItem.mAdapterIndex == beforeItem.mAdapterIndex) {
                        afterRect = afterEntry.getValue();
                        break;
                    }
                }
                assertNotNull(message + ":\nItem with same adapter index should be visible " +
                                "after simple re-layout",
                        afterRect);
            }
            assertEquals(message + ":\nItem should be laid out at the same coordinates",
                    entry.getValue(), afterRect);
        }
    }

    public void testAccessibilityPositions() throws Throwable {
        setupByConfig(new Config(VERTICAL, false, false), true);
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
        assertEquals("result should have first position",
                record.getFromIndex(),
                mLayoutManager.findFirstVisibleItemPosition());
        assertEquals("result should have last position",
                record.getToIndex(),
                mLayoutManager.findLastVisibleItemPosition());
    }

    public void testPrepareForDrop() throws Throwable {
        SelectTargetChildren[] selectors = new SelectTargetChildren[] {
                new SelectTargetChildren() {
                    @Override
                    public int[] selectTargetChildren(int childCount) {
                        return new int[]{1, 0};
                    }
                },
                new SelectTargetChildren() {
                    @Override
                    public int[] selectTargetChildren(int childCount) {
                        return new int[]{0, 1};
                    }
                },
                new SelectTargetChildren() {
                    @Override
                    public int[] selectTargetChildren(int childCount) {
                        return new int[]{childCount - 1, childCount - 2};
                    }
                },
                new SelectTargetChildren() {
                    @Override
                    public int[] selectTargetChildren(int childCount) {
                        return new int[]{childCount - 2, childCount - 1};
                    }
                },
                new SelectTargetChildren() {
                    @Override
                    public int[] selectTargetChildren(int childCount) {
                        return new int[]{childCount / 2, childCount / 2 + 1};
                    }
                },
                new SelectTargetChildren() {
                    @Override
                    public int[] selectTargetChildren(int childCount) {
                        return new int[]{childCount / 2 + 1, childCount / 2};
                    }
                }
        };
        for (SelectTargetChildren selector : selectors) {
            for (Config config : mBaseVariations) {
                prepareForDropTest(config, selector);
                removeRecyclerView();
            }
        }
    }

    public void prepareForDropTest(final Config config, SelectTargetChildren selectTargetChildren)
            throws Throwable {
        config.mTestAdapter = new TestAdapter(100) {
            @Override
            public void onBindViewHolder(TestViewHolder holder,
                    int position) {
                super.onBindViewHolder(holder, position);
                if (config.mOrientation == HORIZONTAL) {
                    final int base = mRecyclerView.getWidth() / 5;
                    final int itemRand = holder.mBoundItem.mText.hashCode() % base;
                    holder.itemView.setMinimumWidth(base + itemRand);
                } else {
                    final int base = mRecyclerView.getHeight() / 5;
                    final int itemRand = holder.mBoundItem.mText.hashCode() % base;
                    holder.itemView.setMinimumHeight(base + itemRand);
                }
            }
        };
        setupByConfig(config, true);
        mLayoutManager.expectLayouts(1);
        scrollToPosition(mTestAdapter.getItemCount() / 2);
        mLayoutManager.waitForLayout(1);
        int[] positions = selectTargetChildren.selectTargetChildren(mRecyclerView.getChildCount());
        final View fromChild = mLayoutManager.getChildAt(positions[0]);
        final int fromPos = mLayoutManager.getPosition(fromChild);
        final View onChild = mLayoutManager.getChildAt(positions[1]);
        final int toPos = mLayoutManager.getPosition(onChild);
        final OrientationHelper helper = mLayoutManager.mOrientationHelper;
        final int dragCoordinate;
        final boolean towardsHead = toPos < fromPos;
        final int referenceLine;
        if (config.mReverseLayout == towardsHead) {
            referenceLine = helper.getDecoratedEnd(onChild);
            dragCoordinate = referenceLine + 3 -
                    helper.getDecoratedMeasurement(fromChild);
        } else {
            referenceLine = helper.getDecoratedStart(onChild);
            dragCoordinate = referenceLine - 3;
        }
        mLayoutManager.expectLayouts(2);

        final int x,y;
        if (config.mOrientation == HORIZONTAL) {
            x = dragCoordinate;
            y = fromChild.getTop();
        } else {
            y = dragCoordinate;
            x = fromChild.getLeft();
        }
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTestAdapter.moveInUIThread(fromPos, toPos);
                mTestAdapter.notifyItemMoved(fromPos, toPos);
                mLayoutManager.prepareForDrop(fromChild, onChild, x, y);
            }
        });
        mLayoutManager.waitForLayout(2);

        assertSame(fromChild, mRecyclerView.findViewHolderForAdapterPosition(toPos).itemView);
        // make sure it has the position we wanted
        if (config.mReverseLayout == towardsHead) {
            assertEquals(referenceLine, helper.getDecoratedEnd(fromChild));
        } else {
            assertEquals(referenceLine, helper.getDecoratedStart(fromChild));
        }
    }

    static class VisibleChildren {

        int firstVisiblePosition = RecyclerView.NO_POSITION;

        int firstFullyVisiblePosition = RecyclerView.NO_POSITION;

        int lastVisiblePosition = RecyclerView.NO_POSITION;

        int lastFullyVisiblePosition = RecyclerView.NO_POSITION;

        @Override
        public String toString() {
            return "VisibleChildren{" +
                    "firstVisiblePosition=" + firstVisiblePosition +
                    ", firstFullyVisiblePosition=" + firstFullyVisiblePosition +
                    ", lastVisiblePosition=" + lastVisiblePosition +
                    ", lastFullyVisiblePosition=" + lastFullyVisiblePosition +
                    '}';
        }
    }

    abstract private class PostLayoutRunnable {

        abstract void run() throws Throwable;

        abstract String describe();
    }

    abstract private class PostRestoreRunnable {

        void onAfterRestore(Config config) throws Throwable {
        }

        abstract String describe();

        boolean shouldLayoutMatch(Config config) {
            return true;
        }

        void onAfterReLayout(Config config) {

        };
    }

    class WrappedLinearLayoutManager extends LinearLayoutManager {

        CountDownLatch layoutLatch;

        OrientationHelper mSecondaryOrientation;

        OnLayoutListener mOnLayoutListener;

        public WrappedLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
            super(context, orientation, reverseLayout);
        }

        public void expectLayouts(int count) {
            layoutLatch = new CountDownLatch(count);
        }

        public void waitForLayout(long timeout) throws InterruptedException {
            waitForLayout(timeout, TimeUnit.SECONDS);
        }

        @Override
        public void setOrientation(int orientation) {
            super.setOrientation(orientation);
            mSecondaryOrientation = null;
        }

        @Override
        public void removeAndRecycleView(View child, RecyclerView.Recycler recycler) {
            if (DEBUG) {
                Log.d(TAG, "recycling view " + mRecyclerView.getChildViewHolder(child));
            }
            super.removeAndRecycleView(child, recycler);
        }

        @Override
        public void removeAndRecycleViewAt(int index, RecyclerView.Recycler recycler) {
            if (DEBUG) {
                Log.d(TAG, "recycling view at" + mRecyclerView.getChildViewHolder(getChildAt(index)));
            }
            super.removeAndRecycleViewAt(index, recycler);
        }

        @Override
        void ensureLayoutState() {
            super.ensureLayoutState();
            if (mSecondaryOrientation == null) {
                mSecondaryOrientation = OrientationHelper.createOrientationHelper(this,
                        1 - getOrientation());
            }
        }

        private void waitForLayout(long timeout, TimeUnit timeUnit) throws InterruptedException {
            layoutLatch.await(timeout * (DEBUG ? 100 : 1), timeUnit);
            assertEquals("all expected layouts should be executed at the expected time",
                    0, layoutLatch.getCount());
            getInstrumentation().waitForIdleSync();
        }

        @Override
        LayoutState createLayoutState() {
            return new LayoutState() {
                @Override
                View next(RecyclerView.Recycler recycler) {
                    final boolean hadMore = hasMore(mRecyclerView.mState);
                    final int position = mCurrentPosition;
                    View next = super.next(recycler);
                    assertEquals("if has more, should return a view", hadMore, next != null);
                    assertEquals("position of the returned view must match current position",
                            position, RecyclerView.getChildViewHolderInt(next).getLayoutPosition());
                    return next;
                }
            };
        }

        public String getBoundsLog() {
            StringBuilder sb = new StringBuilder();
            sb.append("view bounds:[start:").append(mOrientationHelper.getStartAfterPadding())
                    .append(",").append(" end").append(mOrientationHelper.getEndAfterPadding());
            sb.append("\nchildren bounds\n");
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                sb.append("child (ind:").append(i).append(", pos:").append(getPosition(child))
                        .append("[").append("start:").append(
                        mOrientationHelper.getDecoratedStart(child)).append(", end:")
                        .append(mOrientationHelper.getDecoratedEnd(child)).append("]\n");
            }
            return sb.toString();
        }

        public void waitForAnimationsToEnd(int timeoutInSeconds) throws InterruptedException {
            RecyclerView.ItemAnimator itemAnimator = mRecyclerView.getItemAnimator();
            if (itemAnimator == null) {
                return;
            }
            final CountDownLatch latch = new CountDownLatch(1);
            final boolean running = itemAnimator.isRunning(
                    new RecyclerView.ItemAnimator.ItemAnimatorFinishedListener() {
                        @Override
                        public void onAnimationsFinished() {
                            latch.countDown();
                        }
                    }
            );
            if (running) {
                latch.await(timeoutInSeconds, TimeUnit.SECONDS);
            }
        }

        public VisibleChildren traverseAndFindVisibleChildren() {
            int childCount = getChildCount();
            final VisibleChildren visibleChildren = new VisibleChildren();
            final int start = mOrientationHelper.getStartAfterPadding();
            final int end = mOrientationHelper.getEndAfterPadding();
            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                final int childStart = mOrientationHelper.getDecoratedStart(child);
                final int childEnd = mOrientationHelper.getDecoratedEnd(child);
                final boolean fullyVisible = childStart >= start && childEnd <= end;
                final boolean hidden = childEnd <= start || childStart >= end;
                if (hidden) {
                    continue;
                }
                final int position = getPosition(child);
                if (fullyVisible) {
                    if (position < visibleChildren.firstFullyVisiblePosition ||
                            visibleChildren.firstFullyVisiblePosition == RecyclerView.NO_POSITION) {
                        visibleChildren.firstFullyVisiblePosition = position;
                    }

                    if (position > visibleChildren.lastFullyVisiblePosition) {
                        visibleChildren.lastFullyVisiblePosition = position;
                    }
                }

                if (position < visibleChildren.firstVisiblePosition ||
                        visibleChildren.firstVisiblePosition == RecyclerView.NO_POSITION) {
                    visibleChildren.firstVisiblePosition = position;
                }

                if (position > visibleChildren.lastVisiblePosition) {
                    visibleChildren.lastVisiblePosition = position;
                }

            }
            return visibleChildren;
        }

        Rect getViewBounds(View view) {
            if (getOrientation() == HORIZONTAL) {
                return new Rect(
                        mOrientationHelper.getDecoratedStart(view),
                        mSecondaryOrientation.getDecoratedStart(view),
                        mOrientationHelper.getDecoratedEnd(view),
                        mSecondaryOrientation.getDecoratedEnd(view));
            } else {
                return new Rect(
                        mSecondaryOrientation.getDecoratedStart(view),
                        mOrientationHelper.getDecoratedStart(view),
                        mSecondaryOrientation.getDecoratedEnd(view),
                        mOrientationHelper.getDecoratedEnd(view));
            }

        }

        Map<Item, Rect> collectChildCoordinates() throws Throwable {
            final Map<Item, Rect> items = new LinkedHashMap<Item, Rect>();
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final int childCount = getChildCount();
                    Rect layoutBounds = new Rect(0, 0,
                            mLayoutManager.getWidth(), mLayoutManager.getHeight());
                    for (int i = 0; i < childCount; i++) {
                        View child = getChildAt(i);
                        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) child
                                .getLayoutParams();
                        TestViewHolder vh = (TestViewHolder) lp.mViewHolder;
                        Rect childBounds = getViewBounds(child);
                        if (new Rect(childBounds).intersect(layoutBounds)) {
                            items.put(vh.mBoundItem, childBounds);
                        }
                    }
                }
            });
            return items;
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


    }

    static class OnLayoutListener {
        void before(RecyclerView.Recycler recycler, RecyclerView.State state){}
        void after(RecyclerView.Recycler recycler, RecyclerView.State state){}
    }

    static class Config implements Cloneable {

        private static final int DEFAULT_ITEM_COUNT = 100;

        private boolean mStackFromEnd;

        int mOrientation = VERTICAL;

        boolean mReverseLayout = false;

        boolean mRecycleChildrenOnDetach = false;

        int mItemCount = DEFAULT_ITEM_COUNT;

        TestAdapter mTestAdapter;

        Config(int orientation, boolean reverseLayout, boolean stackFromEnd) {
            mOrientation = orientation;
            mReverseLayout = reverseLayout;
            mStackFromEnd = stackFromEnd;
        }

        public Config() {

        }

        Config adapter(TestAdapter adapter) {
            mTestAdapter = adapter;
            return this;
        }

        Config recycleChildrenOnDetach(boolean recycleChildrenOnDetach) {
            mRecycleChildrenOnDetach = recycleChildrenOnDetach;
            return this;
        }

        Config orientation(int orientation) {
            mOrientation = orientation;
            return this;
        }

        Config stackFromBottom(boolean stackFromBottom) {
            mStackFromEnd = stackFromBottom;
            return this;
        }

        Config reverseLayout(boolean reverseLayout) {
            mReverseLayout = reverseLayout;
            return this;
        }

        public Config itemCount(int itemCount) {
            mItemCount = itemCount;
            return this;
        }

        // required by convention
        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        @Override
        public String toString() {
            return "Config{" +
                    "mStackFromEnd=" + mStackFromEnd +
                    ", mOrientation=" + mOrientation +
                    ", mReverseLayout=" + mReverseLayout +
                    ", mRecycleChildrenOnDetach=" + mRecycleChildrenOnDetach +
                    ", mItemCount=" + mItemCount +
                    '}';
        }
    }

    private interface SelectTargetChildren {
        int[] selectTargetChildren(int childCount);
    }
}
