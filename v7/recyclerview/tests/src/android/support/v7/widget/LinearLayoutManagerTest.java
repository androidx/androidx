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
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class LinearLayoutManagerTest extends BaseRecyclerViewInstrumentationTest {

    WrappedLinearLayoutManager mLayoutManager;

    TestAdapter mTestAdapter;

    final List<Config> mBaseVariations = new ArrayList<Config>();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        for (int orientation : new int[]{LinearLayoutManager.VERTICAL,
                LinearLayoutManager.HORIZONTAL}) {
            for (boolean reverseLayout : new boolean[]{false, true}) {
                for (boolean stackFromBottom : new boolean[]{false, true}) {
                    mBaseVariations.add(new Config(orientation, reverseLayout, stackFromBottom));
                }
            }
        }
    }

    RecyclerView setupByConfig(Config config) throws Throwable {
        final RecyclerView recyclerView = new RecyclerView(getActivity());
        recyclerView.setHasFixedSize(true);
        mTestAdapter = new TestAdapter(config.mItemCount);
        recyclerView.setAdapter(mTestAdapter);
        mLayoutManager = new WrappedLinearLayoutManager(getActivity(), config.mOrientation,
                config.mReverseLayout);
        mLayoutManager.setStackFromEnd(config.mStackFromEnd);
        recyclerView.setLayoutManager(mLayoutManager);
        mLayoutManager.expectLayouts(1);
        setRecyclerView(recyclerView);
        mLayoutManager.waitForLayout(2);
        return recyclerView;
    }


    public void testGetFirstLastChildrenTest() throws Throwable {
        for (Config config : mBaseVariations) {
            getFirstLastChildrenTest(config);
        }
    }

    public void getFirstLastChildrenTest(final Config config) throws Throwable {
        setupByConfig(config);
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
            Thread.sleep(200);
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

    static class WrappedLinearLayoutManager extends LinearLayoutManager {

        CountDownLatch layoutLatch;

        public WrappedLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
            super(context, orientation, reverseLayout);
        }

        public void expectLayouts(int count) {
            layoutLatch = new CountDownLatch(count);
        }

        public void waitForLayout(long timeout) throws InterruptedException {
            waitForLayout(timeout, TimeUnit.SECONDS);
        }

        private void waitForLayout(long timeout, TimeUnit timeUnit) throws InterruptedException {
            layoutLatch.await(timeout, timeUnit);
            assertEquals("all expected layouts should be executed at the expected time",
                    0, layoutLatch.getCount());
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

        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            super.onLayoutChildren(recycler, state);
            layoutLatch.countDown();
        }
    }

    static class Config {

        private static final int DEFAULT_ITEM_COUNT = 300;

        private boolean mStackFromEnd;

        int mOrientation = LinearLayoutManager.VERTICAL;

        boolean mReverseLayout = false;

        int mItemCount = DEFAULT_ITEM_COUNT;

        Config(int orientation, boolean reverseLayout, boolean stackFromEnd) {
            mOrientation = orientation;
            mReverseLayout = reverseLayout;
            mStackFromEnd = stackFromEnd;
        }

        public Config() {

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

        @Override
        public String toString() {
            return "Config{" +
                    "mStackFromEnd=" + mStackFromEnd +
                    ", mOrientation=" + mOrientation +
                    ", mReverseLayout=" + mReverseLayout +
                    ", mItemCount=" + mItemCount +
                    '}';
        }
    }
}
