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

import static androidx.recyclerview.widget.LayoutState.LAYOUT_END;
import static androidx.recyclerview.widget.LayoutState.LAYOUT_START;
import static androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import android.graphics.Rect;
import android.support.test.filters.LargeTest;
import android.util.Log;
import android.view.View;
import android.view.ViewParent;

import androidx.annotation.NonNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tests that rely on the basic configuration and does not do any additions / removals
 */
@RunWith(Parameterized.class)
@LargeTest
public class LinearLayoutManagerBaseConfigSetTest extends BaseLinearLayoutManagerTest {

    private final Config mConfig;

    public LinearLayoutManagerBaseConfigSetTest(Config config) {
        mConfig = config;
    }


    @Parameterized.Parameters(name = "{0}")
    public static List<Config> configs() throws CloneNotSupportedException {
        List<Config> result = new ArrayList<>();
        for (Config config : createBaseVariations()) {
            result.add(config);
        }
        return result;
    }

    @Test
    public void scrollToPositionWithOffsetTest() throws Throwable {
        Config config = ((Config) mConfig.clone()).itemCount(300);
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
                assertEquals(
                        logPrefix + " when scrolling with offset to an invisible child in normal"
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
    public void getFirstLastChildrenTest() throws Throwable {
        final Config config = ((Config) mConfig.clone()).itemCount(300);
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
        mActivityRule.runOnUiThread(viewInBoundsTest);
        // smooth scroll to end of the list and keep testing meanwhile. This will test pre-caching
        // case
        final int scrollPosition = config.mStackFromEnd ? 0 : mTestAdapter.getItemCount();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.smoothScrollToPosition(scrollPosition);
            }
        });
        while (mLayoutManager.isSmoothScrolling() ||
                mRecyclerView.getScrollState() != RecyclerView.SCROLL_STATE_IDLE) {
            mActivityRule.runOnUiThread(viewInBoundsTest);
            Thread.sleep(400);
        }
        // delete all items
        mLayoutManager.expectLayouts(2);
        mTestAdapter.deleteAndNotify(0, mTestAdapter.getItemCount());
        mLayoutManager.waitForLayout(2);
        // test empty case
        mActivityRule.runOnUiThread(viewInBoundsTest);
        // set a new adapter with huge items to test full bounds check
        mLayoutManager.expectLayouts(1);
        final int totalSpace = mLayoutManager.mOrientationHelper.getTotalSpace();
        final TestAdapter newAdapter = new TestAdapter(100) {
            @Override
            public void onBindViewHolder(@NonNull TestViewHolder holder,
                    int position) {
                super.onBindViewHolder(holder, position);
                if (config.mOrientation == HORIZONTAL) {
                    holder.itemView.setMinimumWidth(totalSpace + 5);
                } else {
                    holder.itemView.setMinimumHeight(totalSpace + 5);
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
    }

    @Test
    public void dontRecycleViewsTranslatedOutOfBoundsFromStart() throws Throwable {
        final Config config = ((Config) mConfig.clone()).itemCount(1000);
        setupByConfig(config, true);
        mLayoutManager.expectLayouts(1);
        scrollToPosition(500);
        mLayoutManager.waitForLayout(2);
        final RecyclerView.ViewHolder vh = mRecyclerView.findViewHolderForAdapterPosition(500);
        OrientationHelper helper = mLayoutManager.mOrientationHelper;
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
        assertThat(vh.getAdapterPosition(), is(500));
        scrollBy(size * 2);
        assertThat(collector.getDetached(), hasItem(sameInstance(vh.itemView)));
    }

    @Test
    public void dontRecycleViewsTranslatedOutOfBoundsFromEnd() throws Throwable {
        final Config config = ((Config) mConfig.clone()).itemCount(1000);
        setupByConfig(config, true);
        mLayoutManager.expectLayouts(1);
        scrollToPosition(500);
        mLayoutManager.waitForLayout(2);
        final RecyclerView.ViewHolder vh = mRecyclerView.findViewHolderForAdapterPosition(500);
        OrientationHelper helper = mLayoutManager.mOrientationHelper;
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
        assertThat(vh.getAdapterPosition(), is(500));
        scrollBy(-size * 2);
        assertThat(collector.getDetached(), hasItem(sameInstance(vh.itemView)));
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
}
