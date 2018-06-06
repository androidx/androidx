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

import static androidx.recyclerview.widget.LayoutState.LAYOUT_START;
import static androidx.recyclerview.widget.LinearLayoutManager.VERTICAL;
import static androidx.recyclerview.widget.StaggeredGridLayoutManager.HORIZONTAL;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import android.graphics.Rect;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.test.filters.FlakyTest;
import android.support.test.filters.LargeTest;
import android.support.test.filters.Suppress;
import android.util.Log;
import android.view.View;
import android.view.ViewParent;

import androidx.annotation.NonNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RunWith(Parameterized.class)
@LargeTest
public class StaggeredGridLayoutManagerBaseConfigSetTest
        extends BaseStaggeredGridLayoutManagerTest {

    @Parameterized.Parameters(name = "{0}")
    public static List<Config> getParams() {
        return createBaseVariations();
    }

    private final Config mConfig;

    public StaggeredGridLayoutManagerBaseConfigSetTest(Config config)
            throws CloneNotSupportedException {
        mConfig = (Config) config.clone();
    }

    @Test
    public void rTL() throws Throwable {
        rtlTest(false, false);
    }

    @Test
    public void rTLChangeAfter() throws Throwable {
        rtlTest(true, false);
    }

    @Test
    public void rTLItemWrapContent() throws Throwable {
        rtlTest(false, true);
    }

    @Test
    public void rTLChangeAfterItemWrapContent() throws Throwable {
        rtlTest(true, true);
    }

    void rtlTest(boolean changeRtlAfter, final boolean wrapContent) throws Throwable {
        if (mConfig.mSpanCount == 1) {
            mConfig.mSpanCount = 2;
        }
        String logPrefix = mConfig + ", changeRtlAfterLayout:" + changeRtlAfter;
        setupByConfig(mConfig.itemCount(5),
                new GridTestAdapter(mConfig.mItemCount, mConfig.mOrientation) {
                    @Override
                    public void onBindViewHolder(@NonNull TestViewHolder holder,
                            int position) {
                        super.onBindViewHolder(holder, position);
                        if (wrapContent) {
                            if (mOrientation == HORIZONTAL) {
                                holder.itemView.getLayoutParams().height
                                        = RecyclerView.LayoutParams.WRAP_CONTENT;
                            } else {
                                holder.itemView.getLayoutParams().width
                                        = RecyclerView.LayoutParams.MATCH_PARENT;
                            }
                        }
                    }
                });
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
        View child1 = mLayoutManager.findViewByPosition(mConfig.mOrientation == VERTICAL ? 1
                : mConfig.mSpanCount);
        assertNotNull(logPrefix + " child position 0 should be laid out", child0);
        assertNotNull(logPrefix + " child position 0 should be laid out", child1);
        logPrefix += " child1 pos:" + mLayoutManager.getPosition(child1);
        if (mConfig.mOrientation == VERTICAL || !mConfig.mReverseLayout) {
            assertTrue(logPrefix + " second child should be to the left of first child",
                    helper.getDecoratedEnd(child0) > helper.getDecoratedEnd(child1));
            assertEquals(logPrefix + " first child should be right aligned",
                    helper.getDecoratedEnd(child0), helper.getEndAfterPadding());
        } else {
            assertTrue(logPrefix + " first child should be to the left of second child",
                    helper.getDecoratedStart(child1) >= helper.getDecoratedStart(child0));
            assertEquals(logPrefix + " first child should be left aligned",
                    helper.getDecoratedStart(child0), helper.getStartAfterPadding());
        }
        checkForMainThreadException();
    }

    @Test
    public void scrollBackAndPreservePositions() throws Throwable {
        scrollBackAndPreservePositionsTest(false);
    }

    @Test
    public void scrollBackAndPreservePositionsWithRestore() throws Throwable {
        scrollBackAndPreservePositionsTest(true);
    }

    public void scrollBackAndPreservePositionsTest(final boolean saveRestoreInBetween)
            throws Throwable {
        setupByConfig(mConfig);
        mAdapter.mOnBindCallback = new OnBindCallback() {
            @Override
            public void onBoundItem(TestViewHolder vh, int position) {
                StaggeredGridLayoutManager.LayoutParams
                        lp = (StaggeredGridLayoutManager.LayoutParams) vh.itemView
                        .getLayoutParams();
                lp.setFullSpan((position * 7) % (mConfig.mSpanCount + 1) == 0);
            }
        };
        waitFirstLayout();
        final int[] globalPositions = new int[mAdapter.getItemCount()];
        Arrays.fill(globalPositions, Integer.MIN_VALUE);
        final int scrollStep = (mLayoutManager.mPrimaryOrientation.getTotalSpace() / 10)
                * (mConfig.mReverseLayout ? -1 : 1);

        final int[] globalPos = new int[1];
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int globalScrollPosition = 0;
                while (globalPositions[mAdapter.getItemCount() - 1] == Integer.MIN_VALUE) {
                    for (int i = 0; i < mRecyclerView.getChildCount(); i++) {
                        View child = mRecyclerView.getChildAt(i);
                        final int pos = mRecyclerView.getChildLayoutPosition(child);
                        if (globalPositions[pos] != Integer.MIN_VALUE) {
                            continue;
                        }
                        if (mConfig.mReverseLayout) {
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
            saveRestore(mConfig);
        }

        checkForMainThreadException();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int globalScrollPosition = globalPos[0];
                // now scroll back and make sure global positions match
                BitSet shouldTest = new BitSet(mAdapter.getItemCount());
                shouldTest.set(0, mAdapter.getItemCount() - 1, true);
                String assertPrefix = mConfig + ", restored in between:" + saveRestoreInBetween
                        + " global pos must match when scrolling in reverse for position ";
                int scrollAmount = Integer.MAX_VALUE;
                while (!shouldTest.isEmpty() && scrollAmount != 0) {
                    for (int i = 0; i < mRecyclerView.getChildCount(); i++) {
                        View child = mRecyclerView.getChildAt(i);
                        int pos = mRecyclerView.getChildLayoutPosition(child);
                        if (!shouldTest.get(pos)) {
                            continue;
                        }
                        shouldTest.clear(pos);
                        int globalPos;
                        if (mConfig.mReverseLayout) {
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

    private void saveRestore(final Config config) throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
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
                    mLayoutManager = new WrappedLayoutManager(config.mSpanCount,
                            config.mOrientation);
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
                } catch (Throwable t) {
                    postExceptionToInstrumentation(t);
                }
            }
        });
        checkForMainThreadException();
    }

    @Test
    public void getFirstLastChildrenTest() throws Throwable {
        getFirstLastChildrenTest(false);
    }

    @Test
    public void getFirstLastChildrenTestProvideArray() throws Throwable {
        getFirstLastChildrenTest(true);
    }

    public void getFirstLastChildrenTest(final boolean provideArr) throws Throwable {
        setupByConfig(mConfig);
        waitFirstLayout();
        Runnable viewInBoundsTest = new Runnable() {
            @Override
            public void run() {
                VisibleChildren visibleChildren = mLayoutManager.traverseAndFindVisibleChildren();
                final String boundsLog = mLayoutManager.getBoundsLog();
                VisibleChildren queryResult = new VisibleChildren(mLayoutManager.getSpanCount());
                queryResult.findFirstPartialVisibleClosestToStart = mLayoutManager
                        .findFirstVisibleItemClosestToStart(false);
                queryResult.findFirstPartialVisibleClosestToEnd = mLayoutManager
                        .findFirstVisibleItemClosestToEnd(false);
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
                assertEquals(mConfig + ":\nfirst visible child should match traversal result\n"
                        + "traversed:" + visibleChildren + "\n"
                        + "queried:" + queryResult + "\n"
                        + boundsLog, visibleChildren, queryResult
                );
            }
        };
        mActivityRule.runOnUiThread(viewInBoundsTest);
        // smooth scroll to end of the list and keep testing meanwhile. This will test pre-caching
        // case
        final int scrollPosition = mAdapter.getItemCount();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.smoothScrollToPosition(scrollPosition);
            }
        });
        while (mLayoutManager.isSmoothScrolling() ||
                mRecyclerView.getScrollState() != RecyclerView.SCROLL_STATE_IDLE) {
            mActivityRule.runOnUiThread(viewInBoundsTest);
            checkForMainThreadException();
            Thread.sleep(400);
        }
        // delete all items
        mLayoutManager.expectLayouts(2);
        mAdapter.deleteAndNotify(0, mAdapter.getItemCount());
        mLayoutManager.waitForLayout(2);
        // test empty case
        mActivityRule.runOnUiThread(viewInBoundsTest);
        // set a new adapter with huge items to test full bounds check
        mLayoutManager.expectLayouts(1);
        final int totalSpace = mLayoutManager.mPrimaryOrientation.getTotalSpace();
        final TestAdapter newAdapter = new TestAdapter(100) {
            @Override
            public void onBindViewHolder(@NonNull TestViewHolder holder,
                    int position) {
                super.onBindViewHolder(holder, position);
                if (mConfig.mOrientation == LinearLayoutManager.HORIZONTAL) {
                    holder.itemView.setMinimumWidth(totalSpace + 100);
                } else {
                    holder.itemView.setMinimumHeight(totalSpace + 100);
                }
            }
        };
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.setAdapter(newAdapter);
            }
        });
        mLayoutManager.waitForLayout(2);
        mActivityRule.runOnUiThread(viewInBoundsTest);
        checkForMainThreadException();

        // smooth scroll to end of the list and keep testing meanwhile. This will test pre-caching
        // case
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final int diff;
                if (mConfig.mReverseLayout) {
                    diff = -1;
                } else {
                    diff = 1;
                }
                final int distance = diff * 10;
                if (mConfig.mOrientation == HORIZONTAL) {
                    mRecyclerView.scrollBy(distance, 0);
                } else {
                    mRecyclerView.scrollBy(0, distance);
                }
            }
        });
        mActivityRule.runOnUiThread(viewInBoundsTest);
        checkForMainThreadException();
    }

    @Test
    public void viewSnapTest() throws Throwable {
        final Config config = ((Config) mConfig.clone()).itemCount(mConfig.mSpanCount + 1);
        setupByConfig(config);
        mAdapter.mOnBindCallback = new OnBindCallback() {
            @Override
            void onBoundItem(TestViewHolder vh, int position) {
                StaggeredGridLayoutManager.LayoutParams
                        lp = (StaggeredGridLayoutManager.LayoutParams) vh.itemView
                        .getLayoutParams();
                if (config.mOrientation == HORIZONTAL) {
                    lp.width = mRecyclerView.getWidth() / 3;
                } else {
                    lp.height = mRecyclerView.getHeight() / 3;
                }
            }

            @Override
            boolean assignRandomSize() {
                return false;
            }
        };
        waitFirstLayout();
        // run these tests twice. once initial layout, once after scroll
        String logSuffix = "";
        for (int i = 0; i < 2; i++) {
            Map<Item, Rect> itemRectMap = mLayoutManager.collectChildCoordinates();
            Rect recyclerViewBounds = getDecoratedRecyclerViewBounds();
            // workaround for SGLM's span distribution issue. Right now, it may leave gaps so we
            // avoid it by setting its layout params directly
            if (config.mOrientation == HORIZONTAL) {
                recyclerViewBounds.bottom -= recyclerViewBounds.height() % config.mSpanCount;
            } else {
                recyclerViewBounds.right -= recyclerViewBounds.width() % config.mSpanCount;
            }

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

    @Test
    public void scrollToPositionWithOffsetTest() throws Throwable {
        setupByConfig(mConfig);
        waitFirstLayout();
        OrientationHelper orientationHelper = OrientationHelper
                .createOrientationHelper(mLayoutManager, mConfig.mOrientation);
        Rect layoutBounds = getDecoratedRecyclerViewBounds();
        // try scrolling towards head, should not affect anything
        Map<Item, Rect> before = mLayoutManager.collectChildCoordinates();
        scrollToPositionWithOffset(0, 20);
        assertRectSetsEqual(mConfig + " trying to over scroll with offset should be no-op",
                before, mLayoutManager.collectChildCoordinates());
        // try offsetting some visible children
        int testCount = 10;
        while (testCount-- > 0) {
            // get middle child
            final View child = mLayoutManager.getChildAt(mLayoutManager.getChildCount() / 2);
            final int position = mRecyclerView.getChildLayoutPosition(child);
            final int startOffset = mConfig.mReverseLayout ?
                    orientationHelper.getEndAfterPadding() - orientationHelper
                            .getDecoratedEnd(child)
                    : orientationHelper.getDecoratedStart(child) - orientationHelper
                            .getStartAfterPadding();
            final int scrollOffset = startOffset / 2;
            mLayoutManager.expectLayouts(1);
            scrollToPositionWithOffset(position, scrollOffset);
            mLayoutManager.waitForLayout(2);
            final int finalOffset = mConfig.mReverseLayout ?
                    orientationHelper.getEndAfterPadding() - orientationHelper
                            .getDecoratedEnd(child)
                    : orientationHelper.getDecoratedStart(child) - orientationHelper
                            .getStartAfterPadding();
            assertEquals(mConfig + " scroll with offset on a visible child should work fine",
                    scrollOffset, finalOffset);
        }

        // try scrolling to invisible children
        testCount = 10;
        // we test above and below, one by one
        int offsetMultiplier = -1;
        while (testCount-- > 0) {
            final TargetTuple target = findInvisibleTarget(mConfig);
            mLayoutManager.expectLayouts(1);
            final int offset = offsetMultiplier
                    * orientationHelper.getDecoratedMeasurement(mLayoutManager.getChildAt(0)) / 3;
            scrollToPositionWithOffset(target.mPosition, offset);
            mLayoutManager.waitForLayout(2);
            final View child = mLayoutManager.findViewByPosition(target.mPosition);
            assertNotNull(mConfig + " scrolling to a mPosition with offset " + offset
                    + " should layout it", child);
            final Rect bounds = mLayoutManager.getViewBounds(child);
            if (DEBUG) {
                Log.d(TAG, mConfig + " post scroll to invisible mPosition " + bounds + " in "
                        + layoutBounds + " with offset " + offset);
            }

            if (mConfig.mReverseLayout) {
                assertEquals(mConfig + " when scrolling with offset to an invisible in reverse "
                                + "layout, its end should align with recycler view's end - offset",
                        orientationHelper.getEndAfterPadding() - offset,
                        orientationHelper.getDecoratedEnd(child)
                );
            } else {
                assertEquals(mConfig + " when scrolling with offset to an invisible child in normal"
                                + " layout its start should align with recycler view's start + "
                                + "offset",
                        orientationHelper.getStartAfterPadding() + offset,
                        orientationHelper.getDecoratedStart(child)
                );
            }
            offsetMultiplier *= -1;
        }
    }

    @Test
    public void scrollToPositionTest() throws Throwable {
        setupByConfig(mConfig);
        waitFirstLayout();
        OrientationHelper orientationHelper = OrientationHelper
                .createOrientationHelper(mLayoutManager, mConfig.mOrientation);
        Rect layoutBounds = getDecoratedRecyclerViewBounds();
        for (int i = 0; i < mLayoutManager.getChildCount(); i++) {
            View view = mLayoutManager.getChildAt(i);
            Rect bounds = mLayoutManager.getViewBounds(view);
            if (layoutBounds.contains(bounds)) {
                Map<Item, Rect> initialBounds = mLayoutManager.collectChildCoordinates();
                final int position = mRecyclerView.getChildLayoutPosition(view);
                StaggeredGridLayoutManager.LayoutParams layoutParams
                        = (StaggeredGridLayoutManager.LayoutParams) (view.getLayoutParams());
                TestViewHolder vh = (TestViewHolder) layoutParams.mViewHolder;
                assertEquals("recycler view mPosition should match adapter mPosition", position,
                        vh.mBoundItem.mAdapterIndex);
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
                        mConfig + "scroll to mPosition on fully visible child should be no-op",
                        initialBounds, mLayoutManager.collectChildCoordinates());
            } else {
                final int position = mRecyclerView.getChildLayoutPosition(view);
                if (DEBUG) {
                    Log.d(TAG,
                            "child(" + position + ") not fully visible " + bounds + " not inside "
                                    + layoutBounds
                                    + mRecyclerView.getChildLayoutPosition(view)
                    );
                }
                mLayoutManager.expectLayouts(1);
                mActivityRule.runOnUiThread(new Runnable() {
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
                assertTrue(mConfig
                                + " after scrolling to a partially visible child, it should become fully "
                                + " visible. " + bounds + " not inside " + layoutBounds,
                        layoutBounds.contains(bounds)
                );
                assertTrue(
                        mConfig + " when scrolling to a partially visible item, one of its edges "
                                + "should be on the boundaries",
                        orientationHelper.getStartAfterPadding() ==
                                orientationHelper.getDecoratedStart(view)
                                || orientationHelper.getEndAfterPadding() ==
                                orientationHelper.getDecoratedEnd(view));
            }
        }

        // try scrolling to invisible children
        int testCount = 10;
        while (testCount-- > 0) {
            final TargetTuple target = findInvisibleTarget(mConfig);
            mLayoutManager.expectLayouts(1);
            scrollToPosition(target.mPosition);
            mLayoutManager.waitForLayout(2);
            final View child = mLayoutManager.findViewByPosition(target.mPosition);
            assertNotNull(mConfig + " scrolling to a mPosition should lay it out", child);
            final Rect bounds = mLayoutManager.getViewBounds(child);
            if (DEBUG) {
                Log.d(TAG, mConfig + " post scroll to invisible mPosition " + bounds + " in "
                        + layoutBounds);
            }
            assertTrue(mConfig + " scrolling to a mPosition should make it fully visible",
                    layoutBounds.contains(bounds));
            if (target.mLayoutDirection == LAYOUT_START) {
                assertEquals(
                        mConfig + " when scrolling to an invisible child above, its start should"
                                + " align with recycler view's start",
                        orientationHelper.getStartAfterPadding(),
                        orientationHelper.getDecoratedStart(child)
                );
            } else {
                assertEquals(mConfig + " when scrolling to an invisible child below, its end "
                                + "should align with recycler view's end",
                        orientationHelper.getEndAfterPadding(),
                        orientationHelper.getDecoratedEnd(child)
                );
            }
        }
    }

    @Test
    public void scollByTest() throws Throwable {
        setupByConfig(mConfig);
        waitFirstLayout();
        // try invalid scroll. should not happen
        final View first = mLayoutManager.getChildAt(0);
        OrientationHelper primaryOrientation = OrientationHelper
                .createOrientationHelper(mLayoutManager, mConfig.mOrientation);
        int scrollDist;
        if (mConfig.mReverseLayout) {
            scrollDist = primaryOrientation.getDecoratedMeasurement(first) / 2;
        } else {
            scrollDist = -primaryOrientation.getDecoratedMeasurement(first) / 2;
        }
        Map<Item, Rect> before = mLayoutManager.collectChildCoordinates();
        scrollBy(scrollDist);
        Map<Item, Rect> after = mLayoutManager.collectChildCoordinates();
        assertRectSetsEqual(
                mConfig + " if there are no more items, scroll should not happen (dt:" + scrollDist
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
            if (mConfig.mOrientation == VERTICAL) {
                entry.getValue().offset(0, -scrollDist);
            } else {
                entry.getValue().offset(-scrollDist, 0);
            }
            if (afterRect == null || afterRect.isEmpty()) {
                // assert item is out of bounds
                int start, end;
                if (mConfig.mOrientation == VERTICAL) {
                    start = entry.getValue().top;
                    end = entry.getValue().bottom;
                } else {
                    start = entry.getValue().left;
                    end = entry.getValue().right;
                }
                assertTrue(
                        mConfig + " if item is missing after relayout, it should be out of bounds."
                                + "item start: " + start + ", end:" + end + " layout start:"
                                + layoutStart +
                                ", layout end:" + layoutEnd,
                        start <= layoutStart && end <= layoutEnd ||
                                start >= layoutEnd && end >= layoutEnd
                );
            } else {
                assertEquals(mConfig + " Item should be laid out at the scroll offset coordinates",
                        entry.getValue(),
                        afterRect);
            }
        }
        assertViewPositions(mConfig);
    }

    @Test
    public void layoutOrderTest() throws Throwable {
        setupByConfig(mConfig);
        assertViewPositions(mConfig);
    }

    @Test
    public void consistentRelayout() throws Throwable {
        consistentRelayoutTest(mConfig, false);
    }

    @Test
    public void consistentRelayoutWithFullSpanFirstChild() throws Throwable {
        consistentRelayoutTest(mConfig, true);
    }

    @Suppress
    @FlakyTest(bugId = 34158822)
    @Test
    @LargeTest
    public void dontRecycleViewsTranslatedOutOfBoundsFromStart() throws Throwable {
        final Config config = ((Config) mConfig.clone()).itemCount(1000);
        setupByConfig(config);
        waitFirstLayout();
        // pick position from child count so that it is not too far away
        int pos = mRecyclerView.getChildCount() * 2;
        smoothScrollToPosition(pos, true);
        final RecyclerView.ViewHolder vh = mRecyclerView.findViewHolderForAdapterPosition(pos);
        OrientationHelper helper = mLayoutManager.mPrimaryOrientation;
        int gap = helper.getDecoratedStart(vh.itemView);
        scrollBy(gap);
        gap = helper.getDecoratedStart(vh.itemView);
        assertThat("test sanity", gap, is(0));

        final int size = helper.getDecoratedMeasurement(vh.itemView);
        AttachDetachCollector collector = new AttachDetachCollector(mRecyclerView);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mConfig.mOrientation == HORIZONTAL) {
                    vh.itemView.setTranslationX(size * 2);
                } else {
                    vh.itemView.setTranslationY(size * 2);
                }
            }
        });
        scrollBy(size * 2);
        assertThat(collector.getDetached(), not(hasItem(sameInstance(vh.itemView))));
        assertThat(vh.itemView.getParent(), is((ViewParent) mRecyclerView));
        assertThat(vh.getAdapterPosition(), is(pos));
        scrollBy(size * 2);
        assertThat(collector.getDetached(), hasItem(sameInstance(vh.itemView)));
    }

    @Test
    public void dontRecycleViewsTranslatedOutOfBoundsFromEnd() throws Throwable {
        final Config config = ((Config) mConfig.clone()).itemCount(1000);
        setupByConfig(config);
        waitFirstLayout();
        // pick position from child count so that it is not too far away
        int pos = mRecyclerView.getChildCount() * 2;
        mLayoutManager.expectLayouts(1);
        scrollToPosition(pos);
        mLayoutManager.waitForLayout(2);
        final RecyclerView.ViewHolder vh = mRecyclerView.findViewHolderForAdapterPosition(pos);
        OrientationHelper helper = mLayoutManager.mPrimaryOrientation;
        int gap = helper.getEnd() - helper.getDecoratedEnd(vh.itemView);
        scrollBy(-gap);
        gap = helper.getEnd() - helper.getDecoratedEnd(vh.itemView);
        assertThat("test sanity", gap, is(0));

        final int size = helper.getDecoratedMeasurement(vh.itemView);
        AttachDetachCollector collector = new AttachDetachCollector(mRecyclerView);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mConfig.mOrientation == HORIZONTAL) {
                    vh.itemView.setTranslationX(-size * 2);
                } else {
                    vh.itemView.setTranslationY(-size * 2);
                }
            }
        });
        scrollBy(-size * 2);
        assertThat(collector.getDetached(), not(hasItem(sameInstance(vh.itemView))));
        assertThat(vh.itemView.getParent(), is((ViewParent) mRecyclerView));
        assertThat(vh.getAdapterPosition(), is(pos));
        scrollBy(-size * 2);
        assertThat(collector.getDetached(), hasItem(sameInstance(vh.itemView)));
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
}
