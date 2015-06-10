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

package android.support.v7.util;

import android.support.test.runner.AndroidJUnit4;
import android.util.SparseBooleanArray;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class AsyncListUtilTest {

    private static final int TILE_SIZE = 10;

    private TestDataCallback mDataCallback;
    private TestViewCallback mViewCallback;

    AsyncListUtil<String> mAsyncListUtil;


    @Before
    public void setUp() throws Exception {
        mDataCallback = new TestDataCallback();
        mViewCallback = new TestViewCallback();

        mDataCallback.expectTiles(0, 10, 20);
        mAsyncListUtil = new AsyncListUtil<String>(String.class, TILE_SIZE, mDataCallback,
                mViewCallback);
        mDataCallback.waitForTiles("initial load");
    }

    @After
    public void tearDown() throws Exception {
        /// Wait a little extra to catch spurious messages.
        new CountDownLatch(1).await(500, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testWithNoPreload() throws InterruptedException {
        scrollAndExpectTiles(10, "scroll to 10", 30);
        scrollAndExpectTiles(25, "scroll to 25", 40);
        scrollAndExpectTiles(45, "scroll to 45", 50, 60);
        scrollAndExpectTiles(70, "scroll to 70", 70, 80, 90);
    }

    @Test
    public void testWithPreload() throws InterruptedException {
        mViewCallback.mStartPreload = 5;
        mViewCallback.mEndPreload = 15;
        scrollAndExpectTiles(50, "scroll down a lot", 40, 50, 60, 70, 80);

        mViewCallback.mStartPreload = 0;
        mViewCallback.mEndPreload = 0;
        scrollAndExpectTiles(60, "scroll down a little, no new tiles loaded");
        scrollAndExpectTiles(40, "scroll up a little, no new tiles loaded");
    }

    @Test
    public void testTileCaching() throws InterruptedException {
        scrollAndExpectTiles(25, "next screen", 30, 40);

        scrollAndExpectTiles(0, "back at top, no new page loads");
        scrollAndExpectTiles(25, "next screen again, no new page loads");

        mDataCallback.mCacheSize = 3;
        scrollAndExpectTiles(50, "scroll down more, all pages should load", 50, 60, 70);
        scrollAndExpectTiles(0, "scroll back to top, all pages should reload", 0, 10, 20);
    }

    @Test
    public void testDataRefresh() throws InterruptedException {
        mViewCallback.expectDataSetChanged(40);
        mDataCallback.expectTiles(0, 10, 20);
        mAsyncListUtil.refresh();
        mViewCallback.waitForDataSetChanged("increasing item count");
        mDataCallback.waitForTiles("increasing item count");

        mViewCallback.expectDataSetChanged(15);
        mDataCallback.expectTiles(0, 10);
        mAsyncListUtil.refresh();
        mViewCallback.waitForDataSetChanged("decreasing item count");
        mDataCallback.waitForTiles("decreasing item count");
    }

    @Test
    public void testItemChanged() throws InterruptedException {
        final int position = 30;
        final int count = 20;

        assertEquals("no new items should be loaded", 0, getLoadedItemCount(position, count));

        mViewCallback.expectItemRangeChanged(position, count);
        scrollAndExpectTiles(20, "scrolling to missing items", 30, 40);
        mViewCallback.waitForItems();

        assertEquals("all new items should be loaded", count, getLoadedItemCount(position, count));
    }

    private int getLoadedItemCount(int startPosition, int itemCount) {
        int loaded = 0;
        for (int i = 0; i < itemCount; i++) {
            if (mAsyncListUtil.getItem(startPosition + i) != null) {
                loaded++;
            }
        }
        return loaded;
    }

    private void scrollAndExpectTiles(int position, String context, int... positions)
            throws InterruptedException {
        mDataCallback.expectTiles(positions);
        mViewCallback.scrollTo(position);
        mDataCallback.waitForTiles(context);
    }

    private static void waitForLatch(String context, CountDownLatch latch)
            throws InterruptedException {
        assertTrue("timed out waiting for " + context, latch.await(1, TimeUnit.SECONDS));
    }

    private class TestDataCallback extends AsyncListUtil.DataCallback<String> {
        private int mCacheSize = 10;

        int mDataItemCount = 100;

        final PositionSetLatch mTilesFilledLatch = new PositionSetLatch("filled");

        @Override
        public void fillData(String[] data, int startPosition, int itemCount) {
            synchronized (mTilesFilledLatch) {
                assertEquals(Math.min(TILE_SIZE, mDataItemCount - startPosition), itemCount);
                mTilesFilledLatch.countDown(startPosition);
            }
            for (int i = 0; i < itemCount; i++) {
                data[i] = "item #" + startPosition;
            }
        }

        @Override
        public int refreshData() {
            return mDataItemCount;
        }

        public int getMaxCachedTiles() {
            return mCacheSize;
        }

        public void expectTiles(int... positions) {
            synchronized (mTilesFilledLatch) {
                mTilesFilledLatch.expect(positions);
            }
        }

        private void waitForTiles(String context) throws InterruptedException {
            waitForLatch("filled tiles (" + context + ")", mTilesFilledLatch.mLatch);
        }
    }

    private class TestViewCallback extends AsyncListUtil.ViewCallback {
        public static final int VIEWPORT_SIZE = 25;
        private int mStartPreload;
        private int mEndPreload;

        int mFirstVisibleItem;
        int mLastVisibleItem = VIEWPORT_SIZE - 1;

        private int mExpectedItemCount;
        CountDownLatch mDataRefreshLatch;

        PositionSetLatch mItemsChangedLatch = new PositionSetLatch("item changed");

        @Override
        public void getItemRangeInto(int[] outRange) {
            outRange[0] = mFirstVisibleItem;
            outRange[1] = mLastVisibleItem;
        }

        @Override
        public void extendRangeInto(int[] range, int[] outRange, int scrollHint) {
            outRange[0] = range[0] - mStartPreload;
            outRange[1] = range[1] + mEndPreload;
        }

        @Override
        public void onDataRefresh() {
            if (mDataRefreshLatch == null) {
                return;
            }
            assertTrue("unexpected onDataRefresh notification", mDataRefreshLatch.getCount() == 1);
            assertEquals(mExpectedItemCount, mAsyncListUtil.getItemCount());
            mDataRefreshLatch.countDown();
            updateViewport();
        }

        @Override
        public void onItemLoaded(int position) {
            mItemsChangedLatch.countDown(position);
        }

        public void expectDataSetChanged(int expectedItemCount) {
            mDataCallback.mDataItemCount = expectedItemCount;
            mExpectedItemCount = expectedItemCount;
            mDataRefreshLatch = new CountDownLatch(1);
        }

        public void waitForDataSetChanged(String context) throws InterruptedException {
            waitForLatch("timed out waiting for data set change (" + context + ")",
                    mDataRefreshLatch);
        }

        public void expectItemRangeChanged(int startPosition, int itemCount) {
            mItemsChangedLatch.expectRange(startPosition, itemCount);
        }

        public void waitForItems() throws InterruptedException {
            waitForLatch("onItemChanged", mItemsChangedLatch.mLatch);
        }

        public void scrollTo(int position) {
            mLastVisibleItem += position - mFirstVisibleItem;
            mFirstVisibleItem = position;
            mAsyncListUtil.onRangeChanged();
        }

        private void updateViewport() {
            int itemCount = mAsyncListUtil.getItemCount();
            if (mLastVisibleItem < itemCount) {
                return;
            }
            mLastVisibleItem = itemCount - 1;
            mFirstVisibleItem = Math.max(0, mLastVisibleItem - VIEWPORT_SIZE + 1);
        }
    }

    private static class PositionSetLatch {
        public CountDownLatch mLatch = new CountDownLatch(0);

        final private SparseBooleanArray mExpectedPositions = new SparseBooleanArray();
        final private String mKind;

        PositionSetLatch(String kind) {
            this.mKind = kind;
        }

        void expect(int ... positions) {
            mExpectedPositions.clear();
            for (int position : positions) {
                mExpectedPositions.put(position, true);
            }
            createLatch();
        }

        void expectRange(int position, int count) {
            mExpectedPositions.clear();
            for (int i = 0; i < count; i++) {
                mExpectedPositions.put(position + i, true);
            }
            createLatch();
        }

        void countDown(int position) {
            if (mLatch == null) {
                return;
            }
            assertTrue("unexpected " + mKind + " @" + position, mExpectedPositions.get(position));
            mExpectedPositions.delete(position);
            if (mExpectedPositions.size() == 0) {
                mLatch.countDown();
            }
        }

        private void createLatch() {
            mLatch = new CountDownLatch(1);
            if (mExpectedPositions.size() == 0) {
                mLatch.countDown();
            }
        }
    }
}
