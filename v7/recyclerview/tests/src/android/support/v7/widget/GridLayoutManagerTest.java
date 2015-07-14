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
import android.os.Debug;
import android.support.v4.view.AccessibilityDelegateCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.support.v7.widget.LinearLayoutManager.HORIZONTAL;
import static android.support.v7.widget.LinearLayoutManager.VERTICAL;
import static java.util.concurrent.TimeUnit.SECONDS;

public class GridLayoutManagerTest extends BaseRecyclerViewInstrumentationTest {

    static final String TAG = "GridLayoutManagerTest";

    static final boolean DEBUG = false;

    WrappedGridLayoutManager mGlm;

    GridTestAdapter mAdapter;

    final List<Config> mBaseVariations = new ArrayList<Config>();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        for (int orientation : new int[]{VERTICAL, HORIZONTAL}) {
            for (boolean reverseLayout : new boolean[]{false, true}) {
                for (int spanCount : new int[]{1, 3, 4}) {
                    mBaseVariations.add(new Config(spanCount, orientation, reverseLayout));
                }
            }
        }
    }

    public RecyclerView setupBasic(Config config) throws Throwable {
        return setupBasic(config, new GridTestAdapter(config.mItemCount));
    }

    public RecyclerView setupBasic(Config config, GridTestAdapter testAdapter) throws Throwable {
        RecyclerView recyclerView = new RecyclerView(getActivity());
        mAdapter = testAdapter;
        mGlm = new WrappedGridLayoutManager(getActivity(), config.mSpanCount, config.mOrientation,
                config.mReverseLayout);
        mAdapter.assignSpanSizeLookup(mGlm);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setLayoutManager(mGlm);
        return recyclerView;
    }

    public void waitForFirstLayout(RecyclerView recyclerView) throws Throwable {
        mGlm.expectLayout(1);
        setRecyclerView(recyclerView);
        mGlm.waitForLayout(2);
    }

    public void testPredictiveSpanLookup1() throws Throwable {
        predictiveSpanLookupTest(0, false);
    }

    public void testPredictiveSpanLookup2() throws Throwable {
        predictiveSpanLookupTest(0, true);
    }

    public void testPredictiveSpanLookup3() throws Throwable {
        predictiveSpanLookupTest(1, false);
    }

    public void testPredictiveSpanLookup4() throws Throwable {
        predictiveSpanLookupTest(1, true);
    }

    public void predictiveSpanLookupTest(int remaining, boolean removeFromStart) throws Throwable {
        RecyclerView recyclerView = setupBasic(new Config(3, 10));
        mGlm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position < 0 || position >= mAdapter.getItemCount()) {
                    postExceptionToInstrumentation(new AssertionError("position is not within " +
                            "adapter range. pos:" + position + ", adapter size:" +
                            mAdapter.getItemCount()));
                }
                return 1;
            }

            @Override
            public int getSpanIndex(int position, int spanCount) {
                if (position < 0 || position >= mAdapter.getItemCount()) {
                    postExceptionToInstrumentation(new AssertionError("position is not within " +
                            "adapter range. pos:" + position + ", adapter size:" +
                            mAdapter.getItemCount()));
                }
                return super.getSpanIndex(position, spanCount);
            }
        });
        waitForFirstLayout(recyclerView);
        checkForMainThreadException();
        assertTrue("test sanity", mGlm.supportsPredictiveItemAnimations());
        mGlm.expectLayout(2);
        int deleteCnt = 10 - remaining;
        int deleteStart = removeFromStart ? 0 : remaining;
        mAdapter.deleteAndNotify(deleteStart, deleteCnt);
        mGlm.waitForLayout(2);
        checkForMainThreadException();
    }

    public void testCustomWidthInHorizontal() throws Throwable {
        customSizeInScrollDirectionTest(new Config(3, HORIZONTAL, false));
    }

    public void testCustomHeightInVertical() throws Throwable {
        customSizeInScrollDirectionTest(new Config(3, VERTICAL, false));
    }

    public void customSizeInScrollDirectionTest(final Config config) throws Throwable {
        Boolean[] options = new Boolean[]{true, false};
        for (boolean addMargins : options) {
            for (boolean addDecorOffsets : options) {
                customSizeInScrollDirectionTest(config, addDecorOffsets, addMargins);
            }
        }
    }

    public void customSizeInScrollDirectionTest(final Config config, boolean addDecorOffsets,
            boolean addMarigns) throws Throwable {
        final int decorOffset = addDecorOffsets ? 7 : 0;
        final int margin = addMarigns ? 11 : 0;
        final int[] sizePerPosition = new int[]{3, 5, 9, 21, 3, 5, 9, 6, 9, 1};
        final int[] expectedSizePerPosition = new int[]{9, 9, 9, 21, 3, 5, 9, 9, 9, 1};

        final GridTestAdapter testAdapter = new GridTestAdapter(10) {
            @Override
            public void onBindViewHolder(TestViewHolder holder,
                    int position) {
                super.onBindViewHolder(holder, position);
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams)
                        holder.itemView.getLayoutParams();
                if (layoutParams == null) {
                    layoutParams = new ViewGroup.MarginLayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                    holder.itemView.setLayoutParams(layoutParams);
                }
                final int size = sizePerPosition[position];
                if (config.mOrientation == HORIZONTAL) {
                    layoutParams.width = size;
                    layoutParams.leftMargin = margin;
                    layoutParams.rightMargin = margin;
                } else {
                    layoutParams.height = size;
                    layoutParams.topMargin = margin;
                    layoutParams.bottomMargin = margin;
                }
            }
        };
        testAdapter.setFullSpan(3, 5);
        final RecyclerView rv = setupBasic(config, testAdapter);
        if (addDecorOffsets) {
            rv.addItemDecoration(new RecyclerView.ItemDecoration() {
                @Override
                public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                        RecyclerView.State state) {
                    if (config.mOrientation == HORIZONTAL) {
                        outRect.set(decorOffset, 0, decorOffset, 0);
                    } else {
                        outRect.set(0, decorOffset, 0, decorOffset);
                    }
                }
            });
        }
        waitForFirstLayout(rv);

        assertTrue("[test sanity] some views should be laid out", mRecyclerView.getChildCount() > 0);
        for (int i = 0; i < mRecyclerView.getChildCount(); i++) {
            View child = mRecyclerView.getChildAt(i);
            final int size = config.mOrientation == HORIZONTAL ? child.getWidth()
                    : child.getHeight();
            assertEquals("child " + i + " should have the size specified in its layout params",
                    expectedSizePerPosition[i], size);
        }
        checkForMainThreadException();
    }

    public void testRTL() throws Throwable {
        for (boolean changeRtlAfter : new boolean[]{false, true}) {
            for (boolean oneLine : new boolean[]{false, true}) {
                for (Config config : mBaseVariations) {
                    rtlTest(config, changeRtlAfter, oneLine);
                    removeRecyclerView();
                }
            }
        }
    }

    void rtlTest(Config config, boolean changeRtlAfter, boolean oneLine) throws Throwable {
        if (oneLine && config.mOrientation != VERTICAL) {
            return;// nothing to test
        }
        if (config.mSpanCount == 1) {
            config.mSpanCount = 2;
        }
        String logPrefix = config + ", changeRtlAfterLayout:" + changeRtlAfter + ", oneLine:" + oneLine;
        config.mItemCount = 5;
        if (oneLine) {
            config.mSpanCount = config.mItemCount + 1;
        } else {
            config.mSpanCount = Math.min(config.mItemCount - 1, config.mSpanCount);
        }

        RecyclerView rv = setupBasic(config);
        if (changeRtlAfter) {
            waitForFirstLayout(rv);
            mGlm.expectLayout(1);
            mGlm.setFakeRtl(true);
            mGlm.waitForLayout(2);
        } else {
            mGlm.mFakeRTL = true;
            waitForFirstLayout(rv);
        }

        assertEquals("view should become rtl", true, mGlm.isLayoutRTL());
        OrientationHelper helper = OrientationHelper.createHorizontalHelper(mGlm);
        View child0 = mGlm.findViewByPosition(0);
        final int secondChildPos = config.mOrientation == VERTICAL ? 1
                : config.mSpanCount;
        View child1 = mGlm.findViewByPosition(secondChildPos);
        assertNotNull(logPrefix + " child position 0 should be laid out", child0);
        assertNotNull(
                logPrefix + " second child position " + (secondChildPos) + " should be laid out",
                child1);
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

    public void testMovingAGroupOffScreenForAddedItems() throws Throwable {
        final RecyclerView rv = setupBasic(new Config(3, 100));
        final int[] maxId = new int[1];
        maxId[0] = -1;
        final SparseIntArray spanLookups = new SparseIntArray();
        final AtomicBoolean enableSpanLookupLogging = new AtomicBoolean(false);
        mGlm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (maxId[0] > 0 && mAdapter.getItemAt(position).mId > maxId[0]) {
                    return 1;
                } else if (enableSpanLookupLogging.get() && !rv.mState.isPreLayout()) {
                    spanLookups.put(position, spanLookups.get(position, 0) + 1);
                }
                return 3;
            }
        });
        rv.getItemAnimator().setSupportsChangeAnimations(true);
        waitForFirstLayout(rv);
        View lastView = rv.getChildAt(rv.getChildCount() - 1);
        final int lastPos = rv.getChildAdapterPosition(lastView);
        maxId[0] = mAdapter.getItemAt(mAdapter.getItemCount() - 1).mId;
        // now add a lot of items below this and those new views should have span size 3
        enableSpanLookupLogging.set(true);
        mGlm.expectLayout(2);
        mAdapter.addAndNotify(lastPos - 2, 30);
        mGlm.waitForLayout(2);
        checkForMainThreadException();

        assertEquals("last items span count should be queried twice", 2,
                spanLookups.get(lastPos + 30));

    }

    public void testCachedBorders() throws Throwable {
        List<Config> testConfigurations = new ArrayList<Config>(mBaseVariations);
        testConfigurations.addAll(cachedBordersTestConfigs());
        for (Config config : testConfigurations) {
            gridCachedBorderstTest(config);
        }
    }

    private void gridCachedBorderstTest(Config config) throws Throwable {
        RecyclerView recyclerView = setupBasic(config);
        waitForFirstLayout(recyclerView);
        final boolean vertical = config.mOrientation == GridLayoutManager.VERTICAL;
        final int expectedSizeSum = vertical ? recyclerView.getWidth() : recyclerView.getHeight();
        final int lastVisible = mGlm.findLastVisibleItemPosition();
        for (int i = 0; i < lastVisible; i += config.mSpanCount) {
            if ((i+1)*config.mSpanCount - 1 < lastVisible) {
                int childrenSizeSum = 0;
                for (int j = 0; j < config.mSpanCount; j++) {
                    View child = recyclerView.getChildAt(i * config.mSpanCount + j);
                    childrenSizeSum += vertical ? child.getWidth() : child.getHeight();
                }
                assertEquals(expectedSizeSum, childrenSizeSum);
            }
        }
        removeRecyclerView();
    }

    private List<Config> cachedBordersTestConfigs() {
        ArrayList<Config> configs = new ArrayList<Config>();
        final int [] spanCounts = new int[]{88, 279, 741};
        final int [] spanPerItem = new int[]{11, 9, 13};
        for (int orientation : new int[]{VERTICAL, HORIZONTAL}) {
            for (boolean reverseLayout : new boolean[]{false, true}) {
                for (int i = 0 ; i < spanCounts.length; i++) {
                    Config config = new Config(spanCounts[i], orientation, reverseLayout);
                    config.mSpanPerItem = spanPerItem[i];
                    configs.add(config);
                }
            }
        }
        return configs;
    }

    public void testLayoutParams() throws Throwable {
        layoutParamsTest(GridLayoutManager.HORIZONTAL);
        removeRecyclerView();
        layoutParamsTest(GridLayoutManager.VERTICAL);
    }

    public void testHorizontalAccessibilitySpanIndices() throws Throwable {
        accessibilitySpanIndicesTest(HORIZONTAL);
    }

    public void testVerticalAccessibilitySpanIndices() throws Throwable {
        accessibilitySpanIndicesTest(VERTICAL);
    }

    public void accessibilitySpanIndicesTest(int orientation) throws Throwable {
        final RecyclerView recyclerView = setupBasic(new Config(3, orientation, false));
        waitForFirstLayout(recyclerView);
        final AccessibilityDelegateCompat delegateCompat = mRecyclerView
                .getCompatAccessibilityDelegate().getItemDelegate();
        final AccessibilityNodeInfoCompat info = AccessibilityNodeInfoCompat.obtain();
        final View chosen = recyclerView.getChildAt(recyclerView.getChildCount() - 2);
        final int position = recyclerView.getChildLayoutPosition(chosen);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                delegateCompat.onInitializeAccessibilityNodeInfo(chosen, info);
            }
        });
        GridLayoutManager.SpanSizeLookup ssl = mGlm.mSpanSizeLookup;
        AccessibilityNodeInfoCompat.CollectionItemInfoCompat itemInfo = info
                .getCollectionItemInfo();
        assertNotNull(itemInfo);
        assertEquals("result should have span group position",
                ssl.getSpanGroupIndex(position, mGlm.getSpanCount()),
                orientation == HORIZONTAL ? itemInfo.getColumnIndex() : itemInfo.getRowIndex());
        assertEquals("result should have span index",
                ssl.getSpanIndex(position, mGlm.getSpanCount()),
                orientation == HORIZONTAL ? itemInfo.getRowIndex() :  itemInfo.getColumnIndex());
        assertEquals("result should have span size",
                ssl.getSpanSize(position),
                orientation == HORIZONTAL ? itemInfo.getRowSpan() :  itemInfo.getColumnSpan());
    }

    public GridLayoutManager.LayoutParams ensureGridLp(View view) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        GridLayoutManager.LayoutParams glp;
        if (lp instanceof GridLayoutManager.LayoutParams) {
            glp = (GridLayoutManager.LayoutParams) lp;
        } else if (lp == null) {
            glp = (GridLayoutManager.LayoutParams) mGlm
                    .generateDefaultLayoutParams();
            view.setLayoutParams(glp);
        } else {
            glp = (GridLayoutManager.LayoutParams) mGlm.generateLayoutParams(lp);
            view.setLayoutParams(glp);
        }
        return glp;
    }

    public void layoutParamsTest(final int orientation) throws Throwable {
        final RecyclerView rv = setupBasic(new Config(3, 100).orientation(orientation),
                new GridTestAdapter(100) {
                    @Override
                    public void onBindViewHolder(TestViewHolder holder,
                            int position) {
                        super.onBindViewHolder(holder, position);
                        GridLayoutManager.LayoutParams glp = ensureGridLp(holder.itemView);
                        int val = 0;
                        switch (position % 5) {
                            case 0:
                                val = 10;
                                break;
                            case 1:
                                val = 30;
                                break;
                            case 2:
                                val = GridLayoutManager.LayoutParams.WRAP_CONTENT;
                                break;
                            case 3:
                                val = GridLayoutManager.LayoutParams.FILL_PARENT;
                                break;
                            case 4:
                                val = 200;
                                break;
                        }
                        if (orientation == GridLayoutManager.VERTICAL) {
                            glp.height = val;
                        } else {
                            glp.width = val;
                        }
                        holder.itemView.setLayoutParams(glp);
                    }
                });
        waitForFirstLayout(rv);
        final OrientationHelper helper = mGlm.mOrientationHelper;
        final int firstRowSize = Math.max(30, getSize(mGlm.findViewByPosition(2)));
        assertEquals(firstRowSize,
                helper.getDecoratedMeasurement(mGlm.findViewByPosition(0)));
        assertEquals(firstRowSize,
                helper.getDecoratedMeasurement(mGlm.findViewByPosition(1)));
        assertEquals(firstRowSize,
                helper.getDecoratedMeasurement(mGlm.findViewByPosition(2)));
        assertEquals(firstRowSize, getSize(mGlm.findViewByPosition(0)));
        assertEquals(firstRowSize, getSize(mGlm.findViewByPosition(1)));
        assertEquals(firstRowSize, getSize(mGlm.findViewByPosition(2)));

        final int secondRowSize = Math.max(200, getSize(mGlm.findViewByPosition(3)));
        assertEquals(secondRowSize,
                helper.getDecoratedMeasurement(mGlm.findViewByPosition(3)));
        assertEquals(secondRowSize,
                helper.getDecoratedMeasurement(mGlm.findViewByPosition(4)));
        assertEquals(secondRowSize,
                helper.getDecoratedMeasurement(mGlm.findViewByPosition(5)));
        assertEquals(secondRowSize, getSize(mGlm.findViewByPosition(3)));
        assertEquals(secondRowSize, getSize(mGlm.findViewByPosition(4)));
        assertEquals(secondRowSize, getSize(mGlm.findViewByPosition(5)));
    }

    private int getSize(View view) {
        if (mGlm.getOrientation() == GridLayoutManager.HORIZONTAL) {
            return view.getWidth();
        }
        return view.getHeight();
    }

    public void testAnchorUpdate() throws InterruptedException {
        GridLayoutManager glm = new GridLayoutManager(getActivity(), 11);
        final GridLayoutManager.SpanSizeLookup spanSizeLookup
                = new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position > 200) {
                    return 100;
                }
                if (position > 20) {
                    return 2;
                }
                return 1;
            }
        };
        glm.setSpanSizeLookup(spanSizeLookup);
        glm.mAnchorInfo.mPosition = 11;
        RecyclerView.State state = new RecyclerView.State();
        mRecyclerView = new RecyclerView(getActivity());
        state.mItemCount = 1000;
        glm.onAnchorReady(mRecyclerView.mRecycler, state, glm.mAnchorInfo);
        assertEquals("gm should keep anchor in first span", 11, glm.mAnchorInfo.mPosition);

        glm.mAnchorInfo.mPosition = 13;
        glm.onAnchorReady(mRecyclerView.mRecycler, state, glm.mAnchorInfo);
        assertEquals("gm should move anchor to first span", 11, glm.mAnchorInfo.mPosition);

        glm.mAnchorInfo.mPosition = 23;
        glm.onAnchorReady(mRecyclerView.mRecycler, state, glm.mAnchorInfo);
        assertEquals("gm should move anchor to first span", 21, glm.mAnchorInfo.mPosition);

        glm.mAnchorInfo.mPosition = 35;
        glm.onAnchorReady(mRecyclerView.mRecycler, state, glm.mAnchorInfo);
        assertEquals("gm should move anchor to first span", 31, glm.mAnchorInfo.mPosition);
    }

    public void testSpanLookup() {
        spanLookupTest(false);
    }

    public void testSpanLookupWithCache() {
        spanLookupTest(true);
    }

    public void testSpanLookupCache() {
        final GridLayoutManager.SpanSizeLookup ssl
                = new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position > 6) {
                    return 2;
                }
                return 1;
            }
        };
        ssl.setSpanIndexCacheEnabled(true);
        assertEquals("reference child non existent", -1, ssl.findReferenceIndexFromCache(2));
        ssl.getCachedSpanIndex(4, 5);
        assertEquals("reference child non existent", -1, ssl.findReferenceIndexFromCache(3));
        // this should not happen and if happens, it is better to return -1
        assertEquals("reference child itself", -1, ssl.findReferenceIndexFromCache(4));
        assertEquals("reference child before", 4, ssl.findReferenceIndexFromCache(5));
        assertEquals("reference child before", 4, ssl.findReferenceIndexFromCache(100));
        ssl.getCachedSpanIndex(6, 5);
        assertEquals("reference child before", 6, ssl.findReferenceIndexFromCache(7));
        assertEquals("reference child before", 4, ssl.findReferenceIndexFromCache(6));
        assertEquals("reference child itself", -1, ssl.findReferenceIndexFromCache(4));
        ssl.getCachedSpanIndex(12, 5);
        assertEquals("reference child before", 12, ssl.findReferenceIndexFromCache(13));
        assertEquals("reference child before", 6, ssl.findReferenceIndexFromCache(12));
        assertEquals("reference child before", 6, ssl.findReferenceIndexFromCache(7));
        for (int i = 0; i < 6; i++) {
            ssl.getCachedSpanIndex(i, 5);
        }

        for (int i = 1; i < 7; i++) {
            assertEquals("reference child right before " + i, i - 1,
                    ssl.findReferenceIndexFromCache(i));
        }
        assertEquals("reference child before 0 ", -1, ssl.findReferenceIndexFromCache(0));
    }

    public void spanLookupTest(boolean enableCache) {
        final GridLayoutManager.SpanSizeLookup ssl
                = new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position > 200) {
                    return 100;
                }
                if (position > 6) {
                    return 2;
                }
                return 1;
            }
        };
        ssl.setSpanIndexCacheEnabled(enableCache);
        assertEquals(0, ssl.getCachedSpanIndex(0, 5));
        assertEquals(4, ssl.getCachedSpanIndex(4, 5));
        assertEquals(0, ssl.getCachedSpanIndex(5, 5));
        assertEquals(1, ssl.getCachedSpanIndex(6, 5));
        assertEquals(2, ssl.getCachedSpanIndex(7, 5));
        assertEquals(2, ssl.getCachedSpanIndex(9, 5));
        assertEquals(0, ssl.getCachedSpanIndex(8, 5));
    }

    public void testRemoveAnchorItem() throws Throwable {
        removeAnchorItemTest(
                new Config(3, 0).orientation(VERTICAL).reverseLayout(false), 100, 0);
    }

    public void testRemoveAnchorItemReverse() throws Throwable {
        removeAnchorItemTest(
                new Config(3, 0).orientation(VERTICAL).reverseLayout(true), 100,
                0);
    }

    public void testRemoveAnchorItemHorizontal() throws Throwable {
        removeAnchorItemTest(
                new Config(3, 0).orientation(HORIZONTAL).reverseLayout(
                        false), 100, 0);
    }

    public void testRemoveAnchorItemReverseHorizontal() throws Throwable {
        removeAnchorItemTest(
                new Config(3, 0).orientation(HORIZONTAL).reverseLayout(true),
                100, 0);
    }

    /**
     * This tests a regression where predictive animations were not working as expected when the
     * first item is removed and there aren't any more items to add from that direction.
     * First item refers to the default anchor item.
     */
    public void removeAnchorItemTest(final Config config, int adapterSize,
            final int removePos) throws Throwable {
        GridTestAdapter adapter = new GridTestAdapter(adapterSize) {
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
        };
        RecyclerView recyclerView = setupBasic(config, adapter);
        waitForFirstLayout(recyclerView);
        final int childCount = mGlm.getChildCount();
        RecyclerView.ViewHolder toBeRemoved = null;
        List<RecyclerView.ViewHolder> toBeMoved = new ArrayList<RecyclerView.ViewHolder>();
        for (int i = 0; i < childCount; i++) {
            View child = mGlm.getChildAt(i);
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
        mGlm.expectLayout(2);
        adapter.deleteAndNotify(removePos, 1);
        mGlm.waitForLayout(1);
        loggingItemAnimator.waitForPendingAnimationsCall(2);
        assertTrue("removed child should receive remove animation",
                loggingItemAnimator.mRemoveVHs.contains(toBeRemoved));
        for (RecyclerView.ViewHolder vh : toBeMoved) {
            assertTrue("view holder should be in moved list",
                    loggingItemAnimator.mMoveVHs.contains(vh));
        }
        List<RecyclerView.ViewHolder> newHolders = new ArrayList<RecyclerView.ViewHolder>();
        for (int i = 0; i < mGlm.getChildCount(); i++) {
            View child = mGlm.getChildAt(i);
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
        // for removed view, 3 for new row
        assertTrue("control against adding too many children due to bad layout state preparation."
                        + " initial:" + childCount + ", current:" + mRecyclerView.getChildCount(),
                mRecyclerView.getChildCount() <= childCount + 1 + 3);
    }

    public void testSpanGroupIndex() {
        final GridLayoutManager.SpanSizeLookup ssl
                = new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position > 200) {
                    return 100;
                }
                if (position > 6) {
                    return 2;
                }
                return 1;
            }
        };
        assertEquals(0, ssl.getSpanGroupIndex(0, 5));
        assertEquals(0, ssl.getSpanGroupIndex(4, 5));
        assertEquals(1, ssl.getSpanGroupIndex(5, 5));
        assertEquals(1, ssl.getSpanGroupIndex(6, 5));
        assertEquals(1, ssl.getSpanGroupIndex(7, 5));
        assertEquals(2, ssl.getSpanGroupIndex(9, 5));
        assertEquals(2, ssl.getSpanGroupIndex(8, 5));
    }

    public void testNotifyDataSetChange() throws Throwable {
        final RecyclerView recyclerView = setupBasic(new Config(3, 100));
        final GridLayoutManager.SpanSizeLookup ssl = mGlm.getSpanSizeLookup();
        ssl.setSpanIndexCacheEnabled(true);
        waitForFirstLayout(recyclerView);
        assertTrue("some positions should be cached", ssl.mSpanIndexCache.size() > 0);
        final Callback callback = new Callback() {
            @Override
            public void onBeforeLayout(RecyclerView.Recycler recycler, RecyclerView.State state) {
                if (!state.isPreLayout()) {
                    assertEquals("cache should be empty", 0, ssl.mSpanIndexCache.size());
                }
            }

            @Override
            public void onAfterLayout(RecyclerView.Recycler recycler, RecyclerView.State state) {
                if (!state.isPreLayout()) {
                    assertTrue("some items should be cached", ssl.mSpanIndexCache.size() > 0);
                }
            }
        };
        mGlm.mCallbacks.add(callback);
        mGlm.expectLayout(2);
        mAdapter.deleteAndNotify(2, 3);
        mGlm.waitForLayout(2);
        checkForMainThreadException();
    }

    public void testUnevenHeights() throws Throwable {
        final Map<Integer, RecyclerView.ViewHolder> viewHolderMap =
                new HashMap<Integer, RecyclerView.ViewHolder>();
        RecyclerView recyclerView = setupBasic(new Config(3, 3), new GridTestAdapter(3) {
            @Override
            public void onBindViewHolder(TestViewHolder holder,
                    int position) {
                super.onBindViewHolder(holder, position);
                final GridLayoutManager.LayoutParams glp = ensureGridLp(holder.itemView);
                glp.height = 50 + position * 50;
                viewHolderMap.put(position, holder);
            }
        });
        waitForFirstLayout(recyclerView);
        for (RecyclerView.ViewHolder vh : viewHolderMap.values()) {
            assertEquals("all items should get max height", 150,
                    vh.itemView.getHeight());
        }

        for (RecyclerView.ViewHolder vh : viewHolderMap.values()) {
            assertEquals("all items should have measured the max height", 150,
                    vh.itemView.getMeasuredHeight());
        }
    }

    public void testUnevenWidths() throws Throwable {
        final Map<Integer, RecyclerView.ViewHolder> viewHolderMap =
                new HashMap<Integer, RecyclerView.ViewHolder>();
        RecyclerView recyclerView = setupBasic(new Config(3, HORIZONTAL, false),
                new GridTestAdapter(3) {
                    @Override
                    public void onBindViewHolder(TestViewHolder holder,
                            int position) {
                        super.onBindViewHolder(holder, position);
                        final GridLayoutManager.LayoutParams glp = ensureGridLp(holder.itemView);
                        glp.width = 50 + position * 50;
                        viewHolderMap.put(position, holder);
                    }
                });
        waitForFirstLayout(recyclerView);
        for (RecyclerView.ViewHolder vh : viewHolderMap.values()) {
            assertEquals("all items should get max width", 150,
                    vh.itemView.getWidth());
        }

        for (RecyclerView.ViewHolder vh : viewHolderMap.values()) {
            assertEquals("all items should have measured the max width", 150,
                    vh.itemView.getMeasuredWidth());
        }
    }

    public void testScrollBackAndPreservePositions() throws Throwable {
        for (Config config : mBaseVariations) {
            config.mItemCount = 150;
            scrollBackAndPreservePositionsTest(config);
            removeRecyclerView();
        }
    }

    public void testSpanSizeChange() throws Throwable {
        final RecyclerView rv = setupBasic(new Config(3, 100));
        waitForFirstLayout(rv);
        assertTrue(mGlm.supportsPredictiveItemAnimations());
        mGlm.expectLayout(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGlm.setSpanCount(5);
                assertFalse(mGlm.supportsPredictiveItemAnimations());
            }
        });
        checkForMainThreadException();
        mGlm.waitForLayout(2);
        mGlm.expectLayout(2);
        mAdapter.deleteAndNotify(3, 2);
        mGlm.waitForLayout(2);
        assertTrue(mGlm.supportsPredictiveItemAnimations());
    }

    public void testCacheSpanIndices() throws Throwable {
        final RecyclerView rv = setupBasic(new Config(3, 100));
        mGlm.mSpanSizeLookup.setSpanIndexCacheEnabled(true);
        waitForFirstLayout(rv);
        GridLayoutManager.SpanSizeLookup ssl = mGlm.mSpanSizeLookup;
        assertTrue("cache should be filled", mGlm.mSpanSizeLookup.mSpanIndexCache.size() > 0);
        assertEquals("item index 5 should be in span 2", 2,
                getLp(mGlm.findViewByPosition(5)).getSpanIndex());
        mGlm.expectLayout(2);
        mAdapter.mFullSpanItems.add(4);
        mAdapter.changeAndNotify(4, 1);
        mGlm.waitForLayout(2);
        assertEquals("item index 5 should be in span 2", 0,
                getLp(mGlm.findViewByPosition(5)).getSpanIndex());
    }

    GridLayoutManager.LayoutParams getLp(View view) {
        return (GridLayoutManager.LayoutParams) view.getLayoutParams();
    }

    public void scrollBackAndPreservePositionsTest(final Config config) throws Throwable {
        final RecyclerView rv = setupBasic(config);
        for (int i = 1; i < mAdapter.getItemCount(); i += config.mSpanCount + 2) {
            mAdapter.setFullSpan(i);
        }
        waitForFirstLayout(rv);
        final int[] globalPositions = new int[mAdapter.getItemCount()];
        Arrays.fill(globalPositions, Integer.MIN_VALUE);
        final int scrollStep = (mGlm.mOrientationHelper.getTotalSpace() / 20)
                * (config.mReverseLayout ? -1 : 1);
        final String logPrefix = config.toString();
        final int[] globalPos = new int[1];
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertSame("test sanity", mRecyclerView, rv);
                int globalScrollPosition = 0;
                int visited = 0;
                while (visited < mAdapter.getItemCount()) {
                    for (int i = 0; i < mRecyclerView.getChildCount(); i++) {
                        View child = mRecyclerView.getChildAt(i);
                        final int pos = mRecyclerView.getChildLayoutPosition(child);
                        if (globalPositions[pos] != Integer.MIN_VALUE) {
                            continue;
                        }
                        visited++;
                        GridLayoutManager.LayoutParams lp = (GridLayoutManager.LayoutParams)
                                child.getLayoutParams();
                        if (config.mReverseLayout) {
                            globalPositions[pos] = globalScrollPosition +
                                    mGlm.mOrientationHelper.getDecoratedEnd(child);
                        } else {
                            globalPositions[pos] = globalScrollPosition +
                                    mGlm.mOrientationHelper.getDecoratedStart(child);
                        }
                        assertEquals(logPrefix + " span index should match",
                                mGlm.getSpanSizeLookup().getSpanIndex(pos, mGlm.getSpanCount()),
                                lp.getSpanIndex());
                    }
                    int scrolled = mGlm.scrollBy(scrollStep,
                            mRecyclerView.mRecycler, mRecyclerView.mState);
                    globalScrollPosition += scrolled;
                    if (scrolled == 0) {
                        assertEquals(
                                logPrefix + " If scroll is complete, all views should be visited",
                                visited, mAdapter.getItemCount());
                    }
                }
                if (DEBUG) {
                    Log.d(TAG, "done recording positions " + Arrays.toString(globalPositions));
                }
                globalPos[0] = globalScrollPosition;
            }
        });
        checkForMainThreadException();
        // test sanity, ensure scroll happened
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                final int childCount = mGlm.getChildCount();
                final BitSet expectedPositions = new BitSet();
                for (int i = 0; i < childCount; i ++) {
                    expectedPositions.set(mAdapter.getItemCount() - i - 1);
                }
                for (int i = 0; i <childCount; i ++) {
                    final View view = mGlm.getChildAt(i);
                    int position = mGlm.getPosition(view);
                    assertTrue("child position should be in last page", expectedPositions.get(position));
                }
            }
        });
        getInstrumentation().waitForIdleSync();
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                int globalScrollPosition = globalPos[0];
                // now scroll back and make sure global positions match
                BitSet shouldTest = new BitSet(mAdapter.getItemCount());
                shouldTest.set(0, mAdapter.getItemCount() - 1, true);
                String assertPrefix = config
                        + " global pos must match when scrolling in reverse for position ";
                int scrollAmount = Integer.MAX_VALUE;
                while (!shouldTest.isEmpty() && scrollAmount != 0) {
                    for (int i = 0; i < mRecyclerView.getChildCount(); i++) {
                        View child = mRecyclerView.getChildAt(i);
                        int pos = mRecyclerView.getChildLayoutPosition(child);
                        if (!shouldTest.get(pos)) {
                            continue;
                        }
                        GridLayoutManager.LayoutParams lp = (GridLayoutManager.LayoutParams)
                                child.getLayoutParams();
                        shouldTest.clear(pos);
                        int globalPos;
                        if (config.mReverseLayout) {
                            globalPos = globalScrollPosition +
                                    mGlm.mOrientationHelper.getDecoratedEnd(child);
                        } else {
                            globalPos = globalScrollPosition +
                                    mGlm.mOrientationHelper.getDecoratedStart(child);
                        }
                        assertEquals(assertPrefix + pos,
                                globalPositions[pos], globalPos);
                        assertEquals("span index should match",
                                mGlm.getSpanSizeLookup().getSpanIndex(pos, mGlm.getSpanCount()),
                                lp.getSpanIndex());
                    }
                    scrollAmount = mGlm.scrollBy(-scrollStep,
                            mRecyclerView.mRecycler, mRecyclerView.mState);
                    globalScrollPosition += scrollAmount;
                }
                assertTrue("all views should be seen", shouldTest.isEmpty());
            }
        });
        checkForMainThreadException();
    }

    class WrappedGridLayoutManager extends GridLayoutManager {

        CountDownLatch mLayoutLatch;

        List<Callback> mCallbacks = new ArrayList<Callback>();

        Boolean mFakeRTL;

        public WrappedGridLayoutManager(Context context, int spanCount) {
            super(context, spanCount);
        }

        public WrappedGridLayoutManager(Context context, int spanCount, int orientation,
                boolean reverseLayout) {
            super(context, spanCount, orientation, reverseLayout);
        }

        @Override
        protected boolean isLayoutRTL() {
            return mFakeRTL == null ? super.isLayoutRTL() : mFakeRTL;
        }

        public void setFakeRtl(Boolean fakeRtl) {
            mFakeRTL = fakeRtl;
            try {
                requestLayoutOnUIThread(mRecyclerView);
            } catch (Throwable throwable) {
                postExceptionToInstrumentation(throwable);
            }
        }

        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            try {
                for (Callback callback : mCallbacks) {
                    callback.onBeforeLayout(recycler, state);
                }
                super.onLayoutChildren(recycler, state);
                for (Callback callback : mCallbacks) {
                    callback.onAfterLayout(recycler, state);
                }
            } catch (Throwable t) {
                postExceptionToInstrumentation(t);
            }
            mLayoutLatch.countDown();
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

        public void expectLayout(int layoutCount) {
            mLayoutLatch = new CountDownLatch(layoutCount);
        }

        public void waitForLayout(int seconds) throws InterruptedException {
            mLayoutLatch.await(seconds, SECONDS);
        }
    }

    class Config {

        int mSpanCount;
        int mOrientation = GridLayoutManager.VERTICAL;
        int mItemCount = 1000;
        int mSpanPerItem = 1;
        boolean mReverseLayout = false;

        Config(int spanCount, int itemCount) {
            mSpanCount = spanCount;
            mItemCount = itemCount;
        }

        public Config(int spanCount, int orientation, boolean reverseLayout) {
            mSpanCount = spanCount;
            mOrientation = orientation;
            mReverseLayout = reverseLayout;
        }

        Config orientation(int orientation) {
            mOrientation = orientation;
            return this;
        }

        @Override
        public String toString() {
            return "Config{" +
                    "mSpanCount=" + mSpanCount +
                    ", mOrientation=" + (mOrientation == GridLayoutManager.HORIZONTAL ? "h" : "v") +
                    ", mItemCount=" + mItemCount +
                    ", mReverseLayout=" + mReverseLayout +
                    '}';
        }

        public Config reverseLayout(boolean reverseLayout) {
            mReverseLayout = reverseLayout;
            return this;
        }


    }

    class GridTestAdapter extends TestAdapter {

        Set<Integer> mFullSpanItems = new HashSet<Integer>();
        int mSpanPerItem = 1;

        GridTestAdapter(int count) {
            super(count);
        }

        GridTestAdapter(int count, int spanPerItem) {
            super(count);
            mSpanPerItem = spanPerItem;
        }

        void setFullSpan(int... items) {
            for (int i : items) {
                mFullSpanItems.add(i);
            }
        }

        void assignSpanSizeLookup(final GridLayoutManager glm) {
            glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    return mFullSpanItems.contains(position) ? glm.getSpanCount() : mSpanPerItem;
                }
            });
        }
    }

    class Callback {

        public void onBeforeLayout(RecyclerView.Recycler recycler, RecyclerView.State state) {
        }

        public void onAfterLayout(RecyclerView.Recycler recycler, RecyclerView.State state) {
        }
    }
}
