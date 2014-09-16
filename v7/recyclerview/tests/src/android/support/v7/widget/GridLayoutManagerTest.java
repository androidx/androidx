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
import android.support.v4.view.AccessibilityDelegateCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.util.Log;
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
        final int position = recyclerView.getChildPosition(chosen);
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
        assertEquals(firstRowSize, helper.getDecoratedMeasurement(mGlm.findViewByPosition(0)));
        assertEquals(firstRowSize, helper.getDecoratedMeasurement(mGlm.findViewByPosition(1)));
        assertEquals(firstRowSize, helper.getDecoratedMeasurement(mGlm.findViewByPosition(2)));
        assertEquals(firstRowSize, getSize(mGlm.findViewByPosition(0)));
        assertEquals(firstRowSize, getSize(mGlm.findViewByPosition(1)));
        assertEquals(firstRowSize, getSize(mGlm.findViewByPosition(2)));

        final int secondRowSize = Math.max(200, getSize(mGlm.findViewByPosition(3)));
        assertEquals(secondRowSize, helper.getDecoratedMeasurement(mGlm.findViewByPosition(3)));
        assertEquals(secondRowSize, helper.getDecoratedMeasurement(mGlm.findViewByPosition(4)));
        assertEquals(secondRowSize, helper.getDecoratedMeasurement(mGlm.findViewByPosition(5)));
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
        state.mItemCount = 1000;
        glm.onAnchorReady(state, glm.mAnchorInfo);
        assertEquals("gm should keep anchor in first span", 11, glm.mAnchorInfo.mPosition);

        glm.mAnchorInfo.mPosition = 13;
        glm.onAnchorReady(state, glm.mAnchorInfo);
        assertEquals("gm should move anchor to first span", 11, glm.mAnchorInfo.mPosition);

        glm.mAnchorInfo.mPosition = 23;
        glm.onAnchorReady(state, glm.mAnchorInfo);
        assertEquals("gm should move anchor to first span", 21, glm.mAnchorInfo.mPosition);

        glm.mAnchorInfo.mPosition = 35;
        glm.onAnchorReady(state, glm.mAnchorInfo);
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
                int globalScrollPosition = 0;
                int visited = 0;
                while (visited < mAdapter.getItemCount()) {
                    for (int i = 0; i < mRecyclerView.getChildCount(); i++) {
                        View child = mRecyclerView.getChildAt(i);
                        final int pos = mRecyclerView.getChildPosition(child);
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
                        int pos = mRecyclerView.getChildPosition(child);
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

        public WrappedGridLayoutManager(Context context, int spanCount) {
            super(context, spanCount);
        }

        public WrappedGridLayoutManager(Context context, int spanCount, int orientation,
                boolean reverseLayout) {
            super(context, spanCount, orientation, reverseLayout);
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
    }

    class GridTestAdapter extends TestAdapter {

        Set<Integer> mFullSpanItems = new HashSet<Integer>();

        GridTestAdapter(int count) {
            super(count);
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
                    return mFullSpanItems.contains(position) ? glm.getSpanCount() : 1;
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
