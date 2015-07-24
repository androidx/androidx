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

package android.support.v7.widget;

import android.content.Context;
import android.support.v7.util.AsyncListUtil;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.BitSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AsyncListUtilLayoutTest extends BaseRecyclerViewInstrumentationTest {

    private static final boolean DEBUG = false;

    private static final String TAG = "AsyncListUtilLayoutTest";

    private static final int ITEM_COUNT = 1000;
    private static final int TILE_SIZE = 5;

    AsyncTestAdapter mAdapter;

    WrappedLinearLayoutManager mLayoutManager;

    private TestDataCallback mDataCallback;
    private TestViewCallback mViewCallback;
    private AsyncListUtil<String> mAsyncListUtil;

    public int mStartPrefetch = 0;
    public int mEndPrefetch = 0;

    public void testAsyncListUtil() throws Throwable {
        mRecyclerView = inflateWrappedRV();
        mRecyclerView.setHasFixedSize(true);

        mAdapter = new AsyncTestAdapter();
        mRecyclerView.setAdapter(mAdapter);

        mLayoutManager = new WrappedLinearLayoutManager(
                getActivity(), LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mLayoutManager.expectLayouts(1);
        setRecyclerView(mRecyclerView);
        mLayoutManager.waitForLayout(2);

        int rangeStart = 0;
        assertEquals(rangeStart, mLayoutManager.findFirstVisibleItemPosition());

        final int rangeSize = mLayoutManager.findLastVisibleItemPosition() + 1;
        assertTrue("No visible items", rangeSize > 0);

        assertEquals("All visible items must be empty at first",
                rangeSize, getEmptyVisibleChildCount());

        mDataCallback = new TestDataCallback();
        mViewCallback = new TestViewCallback();

        mDataCallback.expectTilesInRange(rangeStart, rangeSize);
        mAdapter.expectItemsInRange(rangeStart, rangeSize);

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAsyncListUtil = new AsyncListUtil<String>(
                        String.class, TILE_SIZE, mDataCallback, mViewCallback);
            }
        });

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                mAsyncListUtil.onRangeChanged();
            }
        });
        assertAllLoaded("First load");

        rangeStart = roundUp(rangeSize);
        scrollAndAssert("Scroll with no prefetch", rangeStart, rangeSize);

        rangeStart = roundUp(rangeStart + rangeSize);
        mEndPrefetch = TILE_SIZE * 2;
        scrollAndAssert("Scroll with prefetch", rangeStart, rangeSize);

        rangeStart += mEndPrefetch;
        mEndPrefetch = 0;
        scrollAndAssert("Scroll a little down, no prefetch", rangeStart, 0);

        rangeStart = ITEM_COUNT / 2;
        mStartPrefetch = TILE_SIZE * 2;
        mEndPrefetch = TILE_SIZE * 3;
        scrollAndAssert("Scroll to middle, prefetch", rangeStart, rangeSize);

        rangeStart -= mStartPrefetch;
        mStartPrefetch = 0;
        mEndPrefetch = 0;
        scrollAndAssert("Scroll a little up, no prefetch", rangeStart, 0);

        Thread.sleep(500);  // Wait for possible spurious messages.
    }

    private void assertAllLoaded(String context)
            throws InterruptedException {
        assertTrue(context + ", timed out while waiting for items", mAdapter.waitForItems(2));
        assertTrue(context + ", timed out while waiting for tiles", mDataCallback.waitForTiles(2));
        assertEquals(context + ", empty child found", 0, getEmptyVisibleChildCount());
    }

    private void scrollAndAssert(String context, int rangeStart, int rangeSize) throws Throwable {
        if (rangeSize > 0) {
            mDataCallback.expectTilesInRange(rangeStart, rangeSize);
        } else {
            mDataCallback.expectNoNewTilesLoaded();
        }
        mAdapter.expectItemsInRange(rangeStart, rangeSize);
        mLayoutManager.expectLayouts(1);
        scrollToPositionWithOffset(rangeStart, 0);
        mLayoutManager.waitForLayout(1);
        assertAllLoaded(context);
    }

    void scrollToPositionWithOffset(final int position, final int offset) throws Throwable {
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLayoutManager.scrollToPositionWithOffset(position, offset);
            }
        });
    }

    private int roundUp(int value) {
        return value - value % TILE_SIZE + TILE_SIZE;
    }

    private int getTileCount(int start, int size) {
        return ((start + size - 1) / TILE_SIZE) - (start / TILE_SIZE) + 1;
    }

    private int getEmptyVisibleChildCount() {
        int emptyChildCount = 0;
        int firstVisible = mLayoutManager.findFirstVisibleItemPosition();
        int lastVisible = mLayoutManager.findLastVisibleItemPosition();
        for (int i = firstVisible; i <= lastVisible; i++) {
            View child = mLayoutManager.findViewByPosition(i);
            assertTrue(child instanceof TextView);
            if (((TextView) child).getText() == "") {
                emptyChildCount++;
            }
        }
        return emptyChildCount;
    }

    private class TestDataCallback extends AsyncListUtil.DataCallback<String> {

        private CountDownLatch mTilesLatch;

        @Override
        public void fillData(String[] data, int startPosition, int itemCount) {
            assertTrue("Unexpected tile load @" + startPosition, mTilesLatch.getCount() > 0);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            for (int i = 0; i < itemCount; i++) {
                data[i] = "Item #" + (startPosition + i);
            }
            mTilesLatch.countDown();
        }

        @Override
        public int refreshData() {
            return ITEM_COUNT;
        }

        private void expectTiles(int count) {
            mTilesLatch = new CountDownLatch(count);
        }

        public void expectTilesInRange(int rangeStart, int rangeSize) {
            expectTiles(getTileCount(rangeStart - mStartPrefetch,
                    rangeSize + mStartPrefetch + mEndPrefetch));
        }

        public void expectNoNewTilesLoaded() {
            expectTiles(0);
        }

        public boolean waitForTiles(long timeoutInSeconds) throws InterruptedException {
            return mTilesLatch.await(timeoutInSeconds, TimeUnit.SECONDS);
        }
    }

    private class TestViewCallback extends AsyncListUtil.ViewCallback {
        @Override
        public void getItemRangeInto(int[] outRange) {
            outRange[0] = mLayoutManager.findFirstVisibleItemPosition();
            outRange[1] = mLayoutManager.findLastVisibleItemPosition();
        }

        @Override
        public void extendRangeInto(int[] range, int[] outRange, int scrollHint) {
            outRange[0] = range[0] - mStartPrefetch;
            outRange[1] = range[1] + mEndPrefetch;
        }

        @Override
        public void onDataRefresh() {
            mRecyclerView.getAdapter().notifyDataSetChanged();
        }

        @Override
        public void onItemLoaded(int position) {
            mRecyclerView.getAdapter().notifyItemChanged(position);
        }
    }

    private static class SimpleViewHolder extends RecyclerView.ViewHolder {

        public SimpleViewHolder(Context context) {
            super(new TextView(context));
        }
    }

    private class AsyncTestAdapter extends RecyclerView.Adapter<SimpleViewHolder> {

        private BitSet mLoadedPositions;
        private BitSet mExpectedPositions;

        private CountDownLatch mItemsLatch;
        public AsyncTestAdapter() {
            mLoadedPositions = new BitSet(ITEM_COUNT);
        }

        @Override
        public SimpleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new SimpleViewHolder(parent.getContext());
        }

        @Override
        public void onBindViewHolder(SimpleViewHolder holder, int position) {
            final String item = mAsyncListUtil == null ? null : mAsyncListUtil.getItem(position);
            ((TextView) (holder.itemView)).setText(item == null ? "" : item);

            if (item != null) {
                mLoadedPositions.set(position);
                if (mExpectedPositions.get(position)) {
                    mExpectedPositions.clear(position);
                    if (mExpectedPositions.cardinality() == 0) {
                        mItemsLatch.countDown();
                    }
                }
            }
        }

        @Override
        public int getItemCount() {
            return ITEM_COUNT;
        }

        private void expectItemsInRange(int rangeStart, int rangeSize) {
            mExpectedPositions = new BitSet(rangeStart + rangeSize);
            for (int i = 0; i < rangeSize; i++) {
                if (!mLoadedPositions.get(rangeStart + i)) {
                    mExpectedPositions.set(rangeStart + i);
                }
            }
            mItemsLatch = new CountDownLatch(1);
            if (mExpectedPositions.cardinality() == 0) {
                mItemsLatch.countDown();
            }
        }

        public boolean waitForItems(long timeoutInSeconds) throws InterruptedException {
            return mItemsLatch.await(timeoutInSeconds, TimeUnit.SECONDS);
        }
    }

    class WrappedLinearLayoutManager extends LinearLayoutManager {

        CountDownLatch mLayoutLatch;

        public WrappedLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
            super(context, orientation, reverseLayout);
        }

        public void expectLayouts(int count) {
            mLayoutLatch = new CountDownLatch(count);
        }

        public void waitForLayout(long timeout) throws InterruptedException {
            mLayoutLatch.await(timeout * (DEBUG ? 100 : 1), TimeUnit.SECONDS);
            assertEquals("all expected layouts should be executed at the expected time",
                    0, mLayoutLatch.getCount());
            getInstrumentation().waitForIdleSync();
        }

        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            try {
                super.onLayoutChildren(recycler, state);
            } catch (Throwable t) {
                postExceptionToInstrumentation(t);
            }
            mLayoutLatch.countDown();
        }
    }
}
