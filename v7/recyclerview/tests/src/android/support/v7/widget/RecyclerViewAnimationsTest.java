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
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RecyclerViewAnimationsTest extends BaseRecyclerViewInstrumentationTest {

    private static final boolean DEBUG = false;

    private static final String TAG = "RecyclerViewAnimationsTest";

    AnimationLayoutManager mLayoutManager;

    TestAdapter mTestAdapter;

    public RecyclerViewAnimationsTest() {
        super(DEBUG);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    RecyclerView setupBasic(int itemCount) throws Throwable {
        return setupBasic(itemCount, 0, itemCount);
    }

    RecyclerView setupBasic(int itemCount, int firstLayoutStartIndex, int firstLayoutItemCount)
            throws Throwable {
        return setupBasic(itemCount, firstLayoutStartIndex, firstLayoutItemCount, null);
    }

    RecyclerView setupBasic(int itemCount, int firstLayoutStartIndex, int firstLayoutItemCount,
            TestAdapter testAdapter)
            throws Throwable {
        final TestRecyclerView recyclerView = new TestRecyclerView(getActivity());
        recyclerView.setHasFixedSize(true);
        if (testAdapter == null) {
            mTestAdapter = new TestAdapter(itemCount);
        } else {
            mTestAdapter = testAdapter;
        }
        recyclerView.setAdapter(mTestAdapter);
        mLayoutManager = new AnimationLayoutManager();
        recyclerView.setLayoutManager(mLayoutManager);
        mLayoutManager.mOnLayoutCallbacks.mLayoutMin = firstLayoutStartIndex;
        mLayoutManager.mOnLayoutCallbacks.mLayoutItemCount = firstLayoutItemCount;

        mLayoutManager.expectLayouts(1);
        recyclerView.expectDraw(1);
        setRecyclerView(recyclerView);
        mLayoutManager.waitForLayout(2);
        recyclerView.waitForDraw(1);
        mLayoutManager.mOnLayoutCallbacks.reset();
        getInstrumentation().waitForIdleSync();
        assertEquals("extra layouts should not happen", 1, mLayoutManager.getTotalLayoutCount());
        assertEquals("all expected children should be laid out", firstLayoutItemCount,
                mLayoutManager.getChildCount());
        return recyclerView;
    }

    public void testDetachBeforeAnimations() throws Throwable {
        setupBasic(10, 0, 5);
        final RecyclerView rv = mRecyclerView;
        waitForAnimations(2);
        final DefaultItemAnimator animator = new DefaultItemAnimator() {
            @Override
            public void runPendingAnimations() {
                super.runPendingAnimations();
            }
        };
        rv.setItemAnimator(animator);
        mLayoutManager.expectLayouts(2);
        mTestAdapter.deleteAndNotify(3, 4);
        mLayoutManager.waitForLayout(2);
        removeRecyclerView();
        assertNull("test sanity check RV should be removed", rv.getParent());
        assertEquals("no views should be hidden", 0, rv.mChildHelper.mHiddenViews.size());
        assertFalse("there should not be any animations running", animator.isRunning());
    }

    public void testPreLayoutPositionCleanup() throws Throwable {
        setupBasic(4, 0, 4);
        mLayoutManager.expectLayouts(2);
        mLayoutManager.mOnLayoutCallbacks = new OnLayoutCallbacks() {
            @Override
            void beforePreLayout(RecyclerView.Recycler recycler,
                    AnimationLayoutManager lm, RecyclerView.State state) {
                mLayoutMin = 0;
                mLayoutItemCount = 3;
            }

            @Override
            void beforePostLayout(RecyclerView.Recycler recycler,
                    AnimationLayoutManager layoutManager,
                    RecyclerView.State state) {
                mLayoutMin = 0;
                mLayoutItemCount = 4;
            }
        };
        mTestAdapter.addAndNotify(0, 1);
        mLayoutManager.waitForLayout(2);



    }

    public void testAddRemoveSamePass() throws Throwable {
        final List<RecyclerView.ViewHolder> mRecycledViews
                = new ArrayList<RecyclerView.ViewHolder>();
        TestAdapter adapter = new TestAdapter(50) {
            @Override
            public void onViewRecycled(TestViewHolder holder) {
                super.onViewRecycled(holder);
                mRecycledViews.add(holder);
            }
        };
        adapter.setHasStableIds(true);
        setupBasic(50, 3, 5, adapter);
        mRecyclerView.setItemViewCacheSize(0);
        final ArrayList<RecyclerView.ViewHolder> addVH
                = new ArrayList<RecyclerView.ViewHolder>();
        final ArrayList<RecyclerView.ViewHolder> removeVH
                = new ArrayList<RecyclerView.ViewHolder>();

        final ArrayList<RecyclerView.ViewHolder> moveVH
                = new ArrayList<RecyclerView.ViewHolder>();

        final View[] testView = new View[1];
        mRecyclerView.setItemAnimator(new DefaultItemAnimator() {
            @Override
            public boolean animateAdd(RecyclerView.ViewHolder holder) {
                addVH.add(holder);
                return true;
            }

            @Override
            public boolean animateRemove(RecyclerView.ViewHolder holder) {
                removeVH.add(holder);
                return true;
            }

            @Override
            public boolean animateMove(RecyclerView.ViewHolder holder, int fromX, int fromY,
                    int toX, int toY) {
                moveVH.add(holder);
                return true;
            }
        });
        mLayoutManager.mOnLayoutCallbacks = new OnLayoutCallbacks() {
            @Override
            void afterPreLayout(RecyclerView.Recycler recycler,
                    AnimationLayoutManager layoutManager,
                    RecyclerView.State state) {
                super.afterPreLayout(recycler, layoutManager, state);
                testView[0] = recycler.getViewForPosition(45);
                testView[0].measure(View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST),
                        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST));
                testView[0].layout(10, 10, 10 + testView[0].getMeasuredWidth(),
                        10 + testView[0].getMeasuredHeight());
                layoutManager.addView(testView[0], 4);
            }

            @Override
            void afterPostLayout(RecyclerView.Recycler recycler,
                    AnimationLayoutManager layoutManager,
                    RecyclerView.State state) {
                super.afterPostLayout(recycler, layoutManager, state);
                testView[0].layout(50, 50, 50 + testView[0].getMeasuredWidth(),
                        50 + testView[0].getMeasuredHeight());
                layoutManager.addDisappearingView(testView[0], 4);
            }
        };
        mLayoutManager.mOnLayoutCallbacks.mLayoutMin = 3;
        mLayoutManager.mOnLayoutCallbacks.mLayoutItemCount = 5;
        mRecycledViews.clear();
        mLayoutManager.expectLayouts(2);
        mTestAdapter.deleteAndNotify(3, 1);
        mLayoutManager.waitForLayout(2);

        for (RecyclerView.ViewHolder vh : addVH) {
            assertNotSame("add-remove item should not animate add", testView[0], vh.itemView);
        }
        for (RecyclerView.ViewHolder vh : moveVH) {
            assertNotSame("add-remove item should not animate move", testView[0], vh.itemView);
        }
        for (RecyclerView.ViewHolder vh : removeVH) {
            assertNotSame("add-remove item should not animate remove", testView[0], vh.itemView);
        }
        boolean found = false;
        for (RecyclerView.ViewHolder vh : mRecycledViews) {
            found |= vh.itemView == testView[0];
        }
        assertTrue("added-removed view should be recycled", found);
    }

    public void testChangeAnimations()  throws Throwable {
        final boolean[] booleans = {true, false};
        for (boolean supportsChange : booleans) {
            for (boolean changeType : booleans) {
                for (boolean hasStableIds : booleans) {
                    for (boolean deleteSomeItems : booleans) {
                        changeAnimTest(supportsChange, changeType, hasStableIds, deleteSomeItems);
                    }
                    removeRecyclerView();
                }
            }
        }
    }
    public void changeAnimTest(final boolean supportsChangeAnim, final boolean changeType,
            final boolean hasStableIds, final boolean deleteSomeItems)  throws Throwable {
        final int changedIndex = 3;
        final int defaultType = 1;
        final AtomicInteger changedIndexNewType = new AtomicInteger(defaultType);
        final String logPrefix = "supportsChangeAnim:" + supportsChangeAnim +
                ", change view type:" + changeType +
                ", has stable ids:" + hasStableIds +
                ", force predictive:" + deleteSomeItems;
        TestAdapter testAdapter = new TestAdapter(10) {
            @Override
            public int getItemViewType(int position) {
                return position == changedIndex ? changedIndexNewType.get() : defaultType;
            }

            @Override
            public TestViewHolder onCreateViewHolder(ViewGroup parent,
                    int viewType) {
                TestViewHolder vh = super.onCreateViewHolder(parent, viewType);
                if (DEBUG) {
                    Log.d(TAG, logPrefix + " onCreateVH" + vh.toString());
                }
                return vh;
            }

            @Override
            public void onBindViewHolder(TestViewHolder holder,
                    int position) {
                super.onBindViewHolder(holder, position);
                if (DEBUG) {
                    Log.d(TAG, logPrefix + " onBind to " + position + "" + holder.toString());
                }
            }
        };
        testAdapter.setHasStableIds(hasStableIds);
        setupBasic(testAdapter.getItemCount(), 0, 10, testAdapter);
        mRecyclerView.getItemAnimator().setSupportsChangeAnimations(supportsChangeAnim);

        final RecyclerView.ViewHolder toBeChangedVH =
                mRecyclerView.findViewHolderForLayoutPosition(changedIndex);
        mLayoutManager.mOnLayoutCallbacks = new OnLayoutCallbacks() {
            @Override
            void afterPreLayout(RecyclerView.Recycler recycler,
                    AnimationLayoutManager layoutManager,
                    RecyclerView.State state) {
                RecyclerView.ViewHolder vh = mRecyclerView.findViewHolderForLayoutPosition(
                        changedIndex);
                if (supportsChangeAnim) {
                    assertTrue(logPrefix + " changed view holder should have correct flag"
                            , vh.isChanged());
                } else {
                    assertFalse(logPrefix + " changed view holder should have correct flag"
                            , vh.isChanged());
                }
            }

            @Override
            void afterPostLayout(RecyclerView.Recycler recycler,
                    AnimationLayoutManager layoutManager, RecyclerView.State state) {
                RecyclerView.ViewHolder vh = mRecyclerView.findViewHolderForLayoutPosition(
                        changedIndex);
                assertFalse(logPrefix + "VH should not be marked as changed", vh.isChanged());
                if (supportsChangeAnim) {
                    assertNotSame(logPrefix + "a new VH should be given if change is supported",
                            toBeChangedVH, vh);
                } else if (!changeType && hasStableIds) {
                    assertSame(logPrefix + "if change animations are not supported but we have "
                            + "stable ids, same view holder should be returned", toBeChangedVH, vh);
                }
                super.beforePostLayout(recycler, layoutManager, state);
            }
        };
        mLayoutManager.expectLayouts(1);
        if (changeType) {
            changedIndexNewType.set(defaultType + 1);
        }
        if (deleteSomeItems) {
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mTestAdapter.deleteAndNotify(changedIndex + 2, 1);
                        mTestAdapter.notifyItemChanged(3);
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }

                }
            });
        } else {
            mTestAdapter.notifyItemChanged(3);
        }

        mLayoutManager.waitForLayout(2);
    }

    public void testRecycleDuringAnimations() throws Throwable {
        final AtomicInteger childCount = new AtomicInteger(0);
        final TestAdapter adapter = new TestAdapter(1000) {
            @Override
            public TestViewHolder onCreateViewHolder(ViewGroup parent,
                    int viewType) {
                childCount.incrementAndGet();
                return super.onCreateViewHolder(parent, viewType);
            }
        };
        setupBasic(1000, 10, 20, adapter);
        mLayoutManager.mOnLayoutCallbacks.mLayoutMin = 10;
        mLayoutManager.mOnLayoutCallbacks.mLayoutItemCount = 20;

        mRecyclerView.setRecycledViewPool(new RecyclerView.RecycledViewPool() {
            @Override
            public void putRecycledView(RecyclerView.ViewHolder scrap) {
                super.putRecycledView(scrap);
                childCount.decrementAndGet();
            }

            @Override
            public RecyclerView.ViewHolder getRecycledView(int viewType) {
                final RecyclerView.ViewHolder recycledView = super.getRecycledView(viewType);
                if (recycledView != null) {
                    childCount.incrementAndGet();
                }
                return recycledView;
            }
        });

        // now keep adding children to trigger more children being created etc.
        for (int i = 0; i < 100; i ++) {
            adapter.addAndNotify(15, 1);
            Thread.sleep(50);
        }
        getInstrumentation().waitForIdleSync();
        waitForAnimations(2);
        assertEquals("Children count should add up", childCount.get(),
                mRecyclerView.getChildCount() + mRecyclerView.mRecycler.mCachedViews.size());
    }

    public void testNotifyDataSetChanged() throws Throwable {
        setupBasic(10, 3, 4);
        int layoutCount = mLayoutManager.mTotalLayoutCount;
        mLayoutManager.expectLayouts(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    mTestAdapter.deleteAndNotify(4, 1);
                    mTestAdapter.dispatchDataSetChanged();
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }

            }
        });
        mLayoutManager.waitForLayout(2);
        getInstrumentation().waitForIdleSync();
        assertEquals("on notify data set changed, predictive animations should not run",
                layoutCount + 1, mLayoutManager.mTotalLayoutCount);
        mLayoutManager.expectLayouts(2);
        mTestAdapter.addAndNotify(4, 2);
        // make sure animations recover
        mLayoutManager.waitForLayout(2);
    }

    public void testStableIdNotifyDataSetChanged() throws Throwable {
        final int itemCount = 20;
        List<Item> initialSet = new ArrayList<Item>();
        final TestAdapter adapter = new TestAdapter(itemCount) {
            @Override
            public long getItemId(int position) {
                return mItems.get(position).mId;
            }
        };
        adapter.setHasStableIds(true);
        initialSet.addAll(adapter.mItems);
        positionStatesTest(itemCount, 5, 5, adapter, new AdapterOps() {
            @Override
            void onRun(TestAdapter testAdapter) throws Throwable {
                Item item5 = adapter.mItems.get(5);
                Item item6 = adapter.mItems.get(6);
                item5.mAdapterIndex = 6;
                item6.mAdapterIndex = 5;
                adapter.mItems.remove(5);
                adapter.mItems.add(6, item5);
                adapter.dispatchDataSetChanged();
                //hacky, we support only 1 layout pass
                mLayoutManager.layoutLatch.countDown();
            }
        }, PositionConstraint.scrap(6, -1, 5), PositionConstraint.scrap(5, -1, 6),
                PositionConstraint.scrap(7, -1, 7), PositionConstraint.scrap(8, -1, 8),
                PositionConstraint.scrap(9, -1, 9));
        // now mix items.
    }


    public void testGetItemForDeletedView() throws Throwable {
        getItemForDeletedViewTest(false);
        getItemForDeletedViewTest(true);
    }

    public void getItemForDeletedViewTest(boolean stableIds) throws Throwable {
        final Set<Integer> itemViewTypeQueries = new HashSet<Integer>();
        final Set<Integer> itemIdQueries = new HashSet<Integer>();
        TestAdapter adapter = new TestAdapter(10) {
            @Override
            public int getItemViewType(int position) {
                itemViewTypeQueries.add(position);
                return super.getItemViewType(position);
            }

            @Override
            public long getItemId(int position) {
                itemIdQueries.add(position);
                return mItems.get(position).mId;
            }
        };
        adapter.setHasStableIds(stableIds);
        setupBasic(10, 0, 10, adapter);
        assertEquals("getItemViewType for all items should be called", 10,
                itemViewTypeQueries.size());
        if (adapter.hasStableIds()) {
            assertEquals("getItemId should be called when adapter has stable ids", 10,
                    itemIdQueries.size());
        } else {
            assertEquals("getItemId should not be called when adapter does not have stable ids", 0,
                    itemIdQueries.size());
        }
        itemViewTypeQueries.clear();
        itemIdQueries.clear();
        mLayoutManager.expectLayouts(2);
        // delete last two
        final int deleteStart = 8;
        final int deleteCount = adapter.getItemCount() - deleteStart;
        adapter.deleteAndNotify(deleteStart, deleteCount);
        mLayoutManager.waitForLayout(2);
        for (int i = 0; i < deleteStart; i++) {
            assertTrue("getItemViewType for existing item " + i + " should be called",
                    itemViewTypeQueries.contains(i));
            if (adapter.hasStableIds()) {
                assertTrue("getItemId for existing item " + i
                        + " should be called when adapter has stable ids",
                        itemIdQueries.contains(i));
            }
        }
        for (int i = deleteStart; i < deleteStart + deleteCount; i++) {
            assertFalse("getItemViewType for deleted item " + i + " SHOULD NOT be called",
                    itemViewTypeQueries.contains(i));
            if (adapter.hasStableIds()) {
                assertFalse("getItemId for deleted item " + i + " SHOULD NOT be called",
                        itemIdQueries.contains(i));
            }
        }
    }

    public void testDeleteInvisibleMultiStep() throws Throwable {
        setupBasic(1000, 1, 7);
        mLayoutManager.mOnLayoutCallbacks.mLayoutMin = 1;
        mLayoutManager.mOnLayoutCallbacks.mLayoutItemCount = 7;
        mLayoutManager.expectLayouts(1);
        // try to trigger race conditions
        int targetItemCount = mTestAdapter.getItemCount();
        for (int i = 0; i < 100; i++) {
            mTestAdapter.deleteAndNotify(new int[]{0, 1}, new int[]{7, 1});
            checkForMainThreadException();
            targetItemCount -= 2;
        }
        // wait until main thread runnables are consumed
        while (targetItemCount != mTestAdapter.getItemCount()) {
            Thread.sleep(100);
        }
        mLayoutManager.waitForLayout(2);
    }

    public void testAddManyMultiStep() throws Throwable {
        setupBasic(10, 1, 7);
        mLayoutManager.mOnLayoutCallbacks.mLayoutMin = 1;
        mLayoutManager.mOnLayoutCallbacks.mLayoutItemCount = 7;
        mLayoutManager.expectLayouts(1);
        // try to trigger race conditions
        int targetItemCount = mTestAdapter.getItemCount();
        for (int i = 0; i < 100; i++) {
            mTestAdapter.addAndNotify(0, 1);
            mTestAdapter.addAndNotify(7, 1);
            targetItemCount += 2;
        }
        // wait until main thread runnables are consumed
        while (targetItemCount != mTestAdapter.getItemCount()) {
            Thread.sleep(100);
        }
        mLayoutManager.waitForLayout(2);
    }

    public void testBasicDelete() throws Throwable {
        setupBasic(10);
        final OnLayoutCallbacks callbacks = new OnLayoutCallbacks() {
            @Override
            public void postDispatchLayout() {
                // verify this only in first layout
                assertEquals("deleted views should still be children of RV",
                        mLayoutManager.getChildCount() + mDeletedViewCount
                        , mRecyclerView.getChildCount());
            }

            @Override
            void afterPreLayout(RecyclerView.Recycler recycler,
                    AnimationLayoutManager layoutManager,
                    RecyclerView.State state) {
                super.afterPreLayout(recycler, layoutManager, state);
                mLayoutItemCount = 3;
                mLayoutMin = 0;
            }
        };
        callbacks.mLayoutItemCount = 10;
        callbacks.setExpectedItemCounts(10, 3);
        mLayoutManager.setOnLayoutCallbacks(callbacks);

        mLayoutManager.expectLayouts(2);
        mTestAdapter.deleteAndNotify(0, 7);
        mLayoutManager.waitForLayout(2);
        callbacks.reset();// when animations end another layout will happen
    }


    public void testAdapterChangeDuringScrolling() throws Throwable {
        setupBasic(10);
        final AtomicInteger onLayoutItemCount = new AtomicInteger(0);
        final AtomicInteger onScrollItemCount = new AtomicInteger(0);

        mLayoutManager.setOnLayoutCallbacks(new OnLayoutCallbacks() {
            @Override
            void onLayoutChildren(RecyclerView.Recycler recycler,
                    AnimationLayoutManager lm, RecyclerView.State state) {
                onLayoutItemCount.set(state.getItemCount());
                super.onLayoutChildren(recycler, lm, state);
            }

            @Override
            public void onScroll(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
                onScrollItemCount.set(state.getItemCount());
                super.onScroll(dx, recycler, state);
            }
        });
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTestAdapter.mItems.remove(5);
                mTestAdapter.notifyItemRangeRemoved(5, 1);
                mRecyclerView.scrollBy(0, 100);
                assertTrue("scrolling while there are pending adapter updates should "
                        + "trigger a layout", mLayoutManager.mOnLayoutCallbacks.mLayoutCount > 0);
                assertEquals("scroll by should be called w/ updated adapter count",
                        mTestAdapter.mItems.size(), onScrollItemCount.get());

            }
        });
    }

    public void testNotifyDataSetChangedDuringScroll() throws Throwable {
        setupBasic(10);
        final AtomicInteger onLayoutItemCount = new AtomicInteger(0);
        final AtomicInteger onScrollItemCount = new AtomicInteger(0);

        mLayoutManager.setOnLayoutCallbacks(new OnLayoutCallbacks() {
            @Override
            void onLayoutChildren(RecyclerView.Recycler recycler,
                    AnimationLayoutManager lm, RecyclerView.State state) {
                onLayoutItemCount.set(state.getItemCount());
                super.onLayoutChildren(recycler, lm, state);
            }

            @Override
            public void onScroll(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
                onScrollItemCount.set(state.getItemCount());
                super.onScroll(dx, recycler, state);
            }
        });
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTestAdapter.mItems.remove(5);
                mTestAdapter.notifyDataSetChanged();
                mRecyclerView.scrollBy(0, 100);
                assertTrue("scrolling while there are pending adapter updates should "
                        + "trigger a layout", mLayoutManager.mOnLayoutCallbacks.mLayoutCount > 0);
                assertEquals("scroll by should be called w/ updated adapter count",
                        mTestAdapter.mItems.size(), onScrollItemCount.get());

            }
        });
    }

    public void testAddInvisibleAndVisible() throws Throwable {
        setupBasic(10, 1, 7);
        mLayoutManager.expectLayouts(2);
        mLayoutManager.mOnLayoutCallbacks.setExpectedItemCounts(10, 12);
        mTestAdapter.addAndNotify(new int[]{0, 1}, new int[]{7, 1});// add a new item 0 // invisible
        mLayoutManager.waitForLayout(2);
    }

    public void testAddInvisible() throws Throwable {
        setupBasic(10, 1, 7);
        mLayoutManager.expectLayouts(1);
        mLayoutManager.mOnLayoutCallbacks.setExpectedItemCounts(10, 12);
        mTestAdapter.addAndNotify(new int[]{0, 1}, new int[]{8, 1});// add a new item 0
        mLayoutManager.waitForLayout(2);
    }

    public void testBasicAdd() throws Throwable {
        setupBasic(10);
        mLayoutManager.expectLayouts(2);
        setExpectedItemCounts(10, 13);
        mTestAdapter.addAndNotify(2, 3);
        mLayoutManager.waitForLayout(2);
    }

    public TestRecyclerView getTestRecyclerView() {
        return (TestRecyclerView) mRecyclerView;
    }

    public void testRemoveScrapInvalidate() throws Throwable {
        setupBasic(10);
        TestRecyclerView testRecyclerView = getTestRecyclerView();
        mLayoutManager.expectLayouts(1);
        testRecyclerView.expectDraw(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTestAdapter.mItems.clear();
                mTestAdapter.notifyDataSetChanged();
            }
        });
        mLayoutManager.waitForLayout(2);
        testRecyclerView.waitForDraw(2);
    }

    public void testDeleteVisibleAndInvisible() throws Throwable {
        setupBasic(11, 3, 5); //layout items  3 4 5 6 7
        mLayoutManager.expectLayouts(2);
        setLayoutRange(3, 5); //layout previously invisible child 10 from end of the list
        setExpectedItemCounts(9, 8);
        mTestAdapter.deleteAndNotify(new int[]{4, 1}, new int[]{7, 2});// delete items 4, 8, 9
        mLayoutManager.waitForLayout(2);
    }

    public void testFindPositionOffset() throws Throwable {
        setupBasic(10);
        mLayoutManager.mOnLayoutCallbacks = new OnLayoutCallbacks() {
            @Override
            void beforePreLayout(RecyclerView.Recycler recycler,
                    AnimationLayoutManager lm, RecyclerView.State state) {
                super.beforePreLayout(recycler, lm, state);
                // [0,2,4]
                assertEquals("offset check", 0, mAdapterHelper.findPositionOffset(0));
                assertEquals("offset check", 1, mAdapterHelper.findPositionOffset(2));
                assertEquals("offset check", 2, mAdapterHelper.findPositionOffset(4));
            }
        };
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                // [0,1,2,3,4]
                // delete 1
                mTestAdapter.notifyItemRangeRemoved(1, 1);
                // delete 3
                mTestAdapter.notifyItemRangeRemoved(2, 1);
            }
        });
        mLayoutManager.waitForLayout(2);
    }

    private void setLayoutRange(int start, int count) {
        mLayoutManager.mOnLayoutCallbacks.mLayoutMin = start;
        mLayoutManager.mOnLayoutCallbacks.mLayoutItemCount = count;
    }

    private void setExpectedItemCounts(int preLayout, int postLayout) {
        mLayoutManager.mOnLayoutCallbacks.setExpectedItemCounts(preLayout, postLayout);
    }

    public void testDeleteInvisible() throws Throwable {
        setupBasic(10, 1, 7);
        mLayoutManager.mOnLayoutCallbacks.mLayoutMin = 1;
        mLayoutManager.mOnLayoutCallbacks.mLayoutItemCount = 7;
        mLayoutManager.expectLayouts(1);
        mLayoutManager.mOnLayoutCallbacks.setExpectedItemCounts(8, 8);
        mTestAdapter.deleteAndNotify(new int[]{0, 1}, new int[]{7, 1});// delete item id 0,8
        mLayoutManager.waitForLayout(2);
    }

    private CollectPositionResult findByPos(RecyclerView recyclerView,
            RecyclerView.Recycler recycler, RecyclerView.State state, int position) {
        View view = recycler.getViewForPosition(position, true);
        RecyclerView.ViewHolder vh = recyclerView.getChildViewHolder(view);
        if (vh.wasReturnedFromScrap()) {
            vh.clearReturnedFromScrapFlag(); //keep data consistent.
            return CollectPositionResult.fromScrap(vh);
        } else {
            return CollectPositionResult.fromAdapter(vh);
        }
    }

    public Map<Integer, CollectPositionResult> collectPositions(RecyclerView recyclerView,
            RecyclerView.Recycler recycler, RecyclerView.State state, int... positions) {
        Map<Integer, CollectPositionResult> positionToAdapterMapping
                = new HashMap<Integer, CollectPositionResult>();
        for (int position : positions) {
            if (position < 0) {
                continue;
            }
            positionToAdapterMapping.put(position,
                    findByPos(recyclerView, recycler, state, position));
        }
        return positionToAdapterMapping;
    }

    public void testAddDelete2() throws Throwable {
        positionStatesTest(5, 0, 5, new AdapterOps() {
            // 0 1 2 3 4
            // 0 1 2 a b 3 4
            // 0 1 b 3 4
            // pre: 0 1 2 3 4
            // pre w/ adap: 0 1 2 b 3 4
            @Override
            void onRun(TestAdapter adapter) throws Throwable {
                adapter.addDeleteAndNotify(new int[]{3, 2}, new int[]{2, -2});
            }
        }, PositionConstraint.scrap(2, 2, -1), PositionConstraint.scrap(1, 1, 1),
                PositionConstraint.scrap(3, 3, 3)
        );
    }

    public void testAddDelete1() throws Throwable {
        positionStatesTest(5, 0, 5, new AdapterOps() {
            // 0 1 2 3 4
            // 0 1 2 a b 3 4
            // 0 2 a b 3 4
            // 0 c d 2 a b 3 4
            // 0 c d 2 a 4
            // c d 2 a 4
            // pre: 0 1 2 3 4
            @Override
            void onRun(TestAdapter adapter) throws Throwable {
                adapter.addDeleteAndNotify(new int[]{3, 2}, new int[]{1, -1},
                        new int[]{1, 2}, new int[]{5, -2}, new int[]{0, -1});
            }
        }, PositionConstraint.scrap(0, 0, -1), PositionConstraint.scrap(1, 1, -1),
                PositionConstraint.scrap(2, 2, 2), PositionConstraint.scrap(3, 3, -1),
                PositionConstraint.scrap(4, 4, 4), PositionConstraint.adapter(0),
                PositionConstraint.adapter(1), PositionConstraint.adapter(3)
        );
    }

    public void testAddSameIndexTwice() throws Throwable {
        positionStatesTest(12, 2, 7, new AdapterOps() {
            @Override
            void onRun(TestAdapter adapter) throws Throwable {
                adapter.addAndNotify(new int[]{1, 2}, new int[]{5, 1}, new int[]{5, 1},
                        new int[]{11, 1});
            }
        }, PositionConstraint.adapterScrap(0, 0), PositionConstraint.adapterScrap(1, 3),
                PositionConstraint.scrap(2, 2, 4), PositionConstraint.scrap(3, 3, 7),
                PositionConstraint.scrap(4, 4, 8), PositionConstraint.scrap(7, 7, 12),
                PositionConstraint.scrap(8, 8, 13)
        );
    }

    public void testDeleteTwice() throws Throwable {
        positionStatesTest(12, 2, 7, new AdapterOps() {
            @Override
            void onRun(TestAdapter adapter) throws Throwable {
                adapter.deleteAndNotify(new int[]{0, 1}, new int[]{1, 1}, new int[]{7, 1},
                        new int[]{0, 1});// delete item ids 0,2,9,1
            }
        }, PositionConstraint.scrap(2, 0, -1), PositionConstraint.scrap(3, 1, 0),
                PositionConstraint.scrap(4, 2, 1), PositionConstraint.scrap(5, 3, 2),
                PositionConstraint.scrap(6, 4, 3), PositionConstraint.scrap(8, 6, 5),
                PositionConstraint.adapterScrap(7, 6), PositionConstraint.adapterScrap(8, 7)
        );
    }


    public void positionStatesTest(int itemCount, int firstLayoutStartIndex,
            int firstLayoutItemCount, AdapterOps adapterChanges,
            final PositionConstraint... constraints) throws Throwable {
        positionStatesTest(itemCount, firstLayoutStartIndex, firstLayoutItemCount, null,
                adapterChanges,  constraints);
    }
    public void positionStatesTest(int itemCount, int firstLayoutStartIndex,
            int firstLayoutItemCount,TestAdapter adapter, AdapterOps adapterChanges,
            final PositionConstraint... constraints) throws Throwable {
        setupBasic(itemCount, firstLayoutStartIndex, firstLayoutItemCount, adapter);
        mLayoutManager.expectLayouts(2);
        mLayoutManager.mOnLayoutCallbacks = new OnLayoutCallbacks() {
            @Override
            void beforePreLayout(RecyclerView.Recycler recycler, AnimationLayoutManager lm,
                    RecyclerView.State state) {
                super.beforePreLayout(recycler, lm, state);
                //harmless
                lm.detachAndScrapAttachedViews(recycler);
                final int[] ids = new int[constraints.length];
                for (int i = 0; i < constraints.length; i++) {
                    ids[i] = constraints[i].mPreLayoutPos;
                }
                Map<Integer, CollectPositionResult> positions
                        = collectPositions(lm.mRecyclerView, recycler, state, ids);
                for (PositionConstraint constraint : constraints) {
                    if (constraint.mPreLayoutPos != -1) {
                        constraint.validate(state, positions.get(constraint.mPreLayoutPos),
                                lm.getLog());
                    }
                }
            }

            @Override
            void beforePostLayout(RecyclerView.Recycler recycler, AnimationLayoutManager lm,
                    RecyclerView.State state) {
                super.beforePostLayout(recycler, lm, state);
                lm.detachAndScrapAttachedViews(recycler);
                final int[] ids = new int[constraints.length];
                for (int i = 0; i < constraints.length; i++) {
                    ids[i] = constraints[i].mPostLayoutPos;
                }
                Map<Integer, CollectPositionResult> positions
                        = collectPositions(lm.mRecyclerView, recycler, state, ids);
                for (PositionConstraint constraint : constraints) {
                    if (constraint.mPostLayoutPos >= 0) {
                        constraint.validate(state, positions.get(constraint.mPostLayoutPos),
                                lm.getLog());
                    }
                }
            }
        };
        adapterChanges.run(mTestAdapter);
        mLayoutManager.waitForLayout(2);
        checkForMainThreadException();
        for (PositionConstraint constraint : constraints) {
            constraint.assertValidate();
        }
    }

    class AnimationLayoutManager extends TestLayoutManager {

        private int mTotalLayoutCount = 0;
        private String log;

        OnLayoutCallbacks mOnLayoutCallbacks = new OnLayoutCallbacks() {
        };



        @Override
        public boolean supportsPredictiveItemAnimations() {
            return true;
        }

        public String getLog() {
            return log;
        }

        private String prepareLog(RecyclerView.Recycler recycler, RecyclerView.State state, boolean done) {
            StringBuilder builder = new StringBuilder();
            builder.append("is pre layout:").append(state.isPreLayout()).append(", done:").append(done);
            builder.append("\nViewHolders:\n");
            for (RecyclerView.ViewHolder vh : ((TestRecyclerView)mRecyclerView).collectViewHolders()) {
                builder.append(vh).append("\n");
            }
            builder.append("scrap:\n");
            for (RecyclerView.ViewHolder vh : recycler.getScrapList()) {
                builder.append(vh).append("\n");
            }

            if (state.isPreLayout() && !done) {
                log = "\n" + builder.toString();
            } else {
                log += "\n" + builder.toString();
            }
            return log;
        }

        @Override
        public void expectLayouts(int count) {
            super.expectLayouts(count);
            mOnLayoutCallbacks.mLayoutCount = 0;
        }

        public void setOnLayoutCallbacks(OnLayoutCallbacks onLayoutCallbacks) {
            mOnLayoutCallbacks = onLayoutCallbacks;
        }

        @Override
        public final void onLayoutChildren(RecyclerView.Recycler recycler,
                RecyclerView.State state) {
            try {
                mTotalLayoutCount++;
                prepareLog(recycler, state, false);
                if (state.isPreLayout()) {
                    validateOldPositions(recycler, state);
                } else {
                    validateClearedOldPositions(recycler, state);
                }
                mOnLayoutCallbacks.onLayoutChildren(recycler, this, state);
                prepareLog(recycler, state, true);
            } finally {
                layoutLatch.countDown();
            }
        }

        private void validateClearedOldPositions(RecyclerView.Recycler recycler,
                RecyclerView.State state) {
            if (getTestRecyclerView() == null) {
                return;
            }
            for (RecyclerView.ViewHolder viewHolder : getTestRecyclerView().collectViewHolders()) {
                assertEquals("there should NOT be an old position in post layout",
                        RecyclerView.NO_POSITION, viewHolder.mOldPosition);
                assertEquals("there should NOT be a pre layout position in post layout",
                        RecyclerView.NO_POSITION, viewHolder.mPreLayoutPosition);
            }
        }

        private void validateOldPositions(RecyclerView.Recycler recycler,
                RecyclerView.State state) {
            if (getTestRecyclerView() == null) {
                return;
            }
            for (RecyclerView.ViewHolder viewHolder : getTestRecyclerView().collectViewHolders()) {
                if (!viewHolder.isRemoved() && !viewHolder.isInvalid()) {
                    assertTrue("there should be an old position in pre-layout",
                            viewHolder.mOldPosition != RecyclerView.NO_POSITION);
                }
            }
        }

        public int getTotalLayoutCount() {
            return mTotalLayoutCount;
        }

        @Override
        public boolean canScrollVertically() {
            return true;
        }

        @Override
        public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler,
                RecyclerView.State state) {
            mOnLayoutCallbacks.onScroll(dy, recycler, state);
            return super.scrollVerticallyBy(dy, recycler, state);
        }

        public void onPostDispatchLayout() {
            mOnLayoutCallbacks.postDispatchLayout();
        }

        @Override
        public void waitForLayout(long timeout, TimeUnit timeUnit) throws Throwable {
            super.waitForLayout(timeout, timeUnit);
            checkForMainThreadException();
        }
    }

    abstract class OnLayoutCallbacks {

        int mLayoutMin = Integer.MIN_VALUE;

        int mLayoutItemCount = Integer.MAX_VALUE;

        int expectedPreLayoutItemCount = -1;

        int expectedPostLayoutItemCount = -1;

        int mDeletedViewCount;

        int mLayoutCount = 0;

        void setExpectedItemCounts(int preLayout, int postLayout) {
            expectedPreLayoutItemCount = preLayout;
            expectedPostLayoutItemCount = postLayout;
        }

        void reset() {
            mLayoutMin = Integer.MIN_VALUE;
            mLayoutItemCount = Integer.MAX_VALUE;
            expectedPreLayoutItemCount = -1;
            expectedPostLayoutItemCount = -1;
            mLayoutCount = 0;
        }

        void beforePreLayout(RecyclerView.Recycler recycler,
                AnimationLayoutManager lm, RecyclerView.State state) {
            mDeletedViewCount = 0;
            for (int i = 0; i < lm.getChildCount(); i++) {
                View v = lm.getChildAt(i);
                if (lm.getLp(v).isItemRemoved()) {
                    mDeletedViewCount++;
                }
            }
        }

        void doLayout(RecyclerView.Recycler recycler, AnimationLayoutManager lm,
                RecyclerView.State state) {
            if (DEBUG) {
                Log.d(TAG, "item count " + state.getItemCount());
            }
            lm.detachAndScrapAttachedViews(recycler);
            final int start = mLayoutMin == Integer.MIN_VALUE ? 0 : mLayoutMin;
            final int count = mLayoutItemCount
                    == Integer.MAX_VALUE ? state.getItemCount() : mLayoutItemCount;
            lm.layoutRange(recycler, start, start + count);
            assertEquals("correct # of children should be laid out",
                    count, lm.getChildCount());
            lm.assertVisibleItemPositions();
        }

        private void assertNoPreLayoutPosition(RecyclerView.Recycler recycler) {
            for (RecyclerView.ViewHolder vh : recycler.mAttachedScrap) {
                assertPreLayoutPosition(vh);
            }
        }

        private void assertNoPreLayoutPosition(RecyclerView.LayoutManager lm) {
            for (int i = 0; i < lm.getChildCount(); i ++) {
                final RecyclerView.ViewHolder vh = mRecyclerView
                        .getChildViewHolder(lm.getChildAt(i));
                assertPreLayoutPosition(vh);
            }
        }

        private void assertPreLayoutPosition(RecyclerView.ViewHolder vh) {
            assertEquals("in post layout, there should not be a view holder w/ a pre "
                    + "layout position", RecyclerView.NO_POSITION, vh.mPreLayoutPosition);
            assertEquals("in post layout, there should not be a view holder w/ an old "
                    + "layout position", RecyclerView.NO_POSITION, vh.mOldPosition);
        }

        void onLayoutChildren(RecyclerView.Recycler recycler, AnimationLayoutManager lm,
                RecyclerView.State state) {

            if (state.isPreLayout()) {
                if (expectedPreLayoutItemCount != -1) {
                    assertEquals("on pre layout, state should return abstracted adapter size",
                            expectedPreLayoutItemCount, state.getItemCount());
                }
                beforePreLayout(recycler, lm, state);
            } else {
                if (expectedPostLayoutItemCount != -1) {
                    assertEquals("on post layout, state should return real adapter size",
                            expectedPostLayoutItemCount, state.getItemCount());
                }
                beforePostLayout(recycler, lm, state);
            }
            if (!state.isPreLayout()) {
                assertNoPreLayoutPosition(recycler);
            }
            doLayout(recycler, lm, state);
            if (state.isPreLayout()) {
                afterPreLayout(recycler, lm, state);
            } else {
                afterPostLayout(recycler, lm, state);
                assertNoPreLayoutPosition(lm);
            }
            mLayoutCount++;
        }

        void afterPreLayout(RecyclerView.Recycler recycler, AnimationLayoutManager layoutManager,
                RecyclerView.State state) {
        }

        void beforePostLayout(RecyclerView.Recycler recycler, AnimationLayoutManager layoutManager,
                RecyclerView.State state) {
        }

        void afterPostLayout(RecyclerView.Recycler recycler, AnimationLayoutManager layoutManager,
                RecyclerView.State state) {
        }

        void postDispatchLayout() {
        }

        public void onScroll(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {

        }
    }

    class TestRecyclerView extends RecyclerView {

        CountDownLatch drawLatch;

        public TestRecyclerView(Context context) {
            super(context);
        }

        public TestRecyclerView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public TestRecyclerView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        @Override
        void initAdapterManager() {
            super.initAdapterManager();
            mAdapterHelper.mOnItemProcessedCallback = new Runnable() {
                @Override
                public void run() {
                    validatePostUpdateOp();
                }
            };
        }

        public void expectDraw(int count) {
            drawLatch = new CountDownLatch(count);
        }

        public void waitForDraw(long timeout) throws Throwable {
            drawLatch.await(timeout * (DEBUG ? 100 : 1), TimeUnit.SECONDS);
            assertEquals("all expected draws should happen at the expected time frame",
                    0, drawLatch.getCount());
        }

        List<ViewHolder> collectViewHolders() {
            List<ViewHolder> holders = new ArrayList<ViewHolder>();
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                ViewHolder holder = getChildViewHolderInt(getChildAt(i));
                if (holder != null) {
                    holders.add(holder);
                }
            }
            return holders;
        }


        private void validateViewHolderPositions() {
            final Set<Integer> existingOffsets = new HashSet<Integer>();
            int childCount = getChildCount();
            StringBuilder log = new StringBuilder();
            for (int i = 0; i < childCount; i++) {
                ViewHolder vh = getChildViewHolderInt(getChildAt(i));
                TestViewHolder tvh = (TestViewHolder) vh;
                log.append(tvh.mBoundItem).append(vh)
                        .append(" hidden:")
                        .append(mChildHelper.mHiddenViews.contains(vh.itemView))
                        .append("\n");
            }
            for (int i = 0; i < childCount; i++) {
                ViewHolder vh = getChildViewHolderInt(getChildAt(i));
                if (vh.isInvalid()) {
                    continue;
                }
                if (vh.getLayoutPosition() < 0) {
                    LayoutManager lm = getLayoutManager();
                    for (int j = 0; j < lm.getChildCount(); j ++) {
                        assertNotSame("removed view holder should not be in LM's child list",
                                vh.itemView, lm.getChildAt(j));
                    }
                } else if (!mChildHelper.mHiddenViews.contains(vh.itemView)) {
                    if (!existingOffsets.add(vh.getLayoutPosition())) {
                        throw new IllegalStateException("view holder position conflict for "
                                + "existing views " + vh + "\n" + log);
                    }
                }
            }
        }

        void validatePostUpdateOp() {
            try {
                validateViewHolderPositions();
                if (super.mState.isPreLayout()) {
                    validatePreLayoutSequence((AnimationLayoutManager) getLayoutManager());
                }
                validateAdapterPosition((AnimationLayoutManager) getLayoutManager());
            } catch (Throwable t) {
                postExceptionToInstrumentation(t);
            }
        }



        private void validateAdapterPosition(AnimationLayoutManager lm) {
            for (ViewHolder vh : collectViewHolders()) {
                if (!vh.isRemoved() && vh.mPreLayoutPosition >= 0) {
                    assertEquals("adapter position calculations should match view holder "
                            + "pre layout:" + mState.isPreLayout()
                            + " positions\n" + vh + "\n" + lm.getLog(),
                            mAdapterHelper.findPositionOffset(vh.mPreLayoutPosition), vh.mPosition);
                }
            }
        }

        // ensures pre layout positions are continuous block. This is not necessarily a case
        // but valid in test RV
        private void validatePreLayoutSequence(AnimationLayoutManager lm) {
            Set<Integer> preLayoutPositions = new HashSet<Integer>();
            for (ViewHolder vh : collectViewHolders()) {
                assertTrue("pre layout positions should be distinct " + lm.getLog(),
                        preLayoutPositions.add(vh.mPreLayoutPosition));
            }
            int minPos = Integer.MAX_VALUE;
            for (Integer pos : preLayoutPositions) {
                if (pos < minPos) {
                    minPos = pos;
                }
            }
            for (int i = 1; i < preLayoutPositions.size(); i++) {
                assertNotNull("next position should exist " + lm.getLog(),
                        preLayoutPositions.contains(minPos + i));
            }
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            super.dispatchDraw(canvas);
            if (drawLatch != null) {
                drawLatch.countDown();
            }
        }

        @Override
        void dispatchLayout() {
            try {
                super.dispatchLayout();
                if (getLayoutManager() instanceof AnimationLayoutManager) {
                    ((AnimationLayoutManager) getLayoutManager()).onPostDispatchLayout();
                }
            } catch (Throwable t) {
                postExceptionToInstrumentation(t);
            }

        }


    }

    abstract class AdapterOps {

        final public void run(TestAdapter adapter) throws Throwable {
            onRun(adapter);
        }

        abstract void onRun(TestAdapter testAdapter) throws Throwable;
    }

    static class CollectPositionResult {

        // true if found in scrap
        public RecyclerView.ViewHolder scrapResult;

        public RecyclerView.ViewHolder adapterResult;

        static CollectPositionResult fromScrap(RecyclerView.ViewHolder viewHolder) {
            CollectPositionResult cpr = new CollectPositionResult();
            cpr.scrapResult = viewHolder;
            return cpr;
        }

        static CollectPositionResult fromAdapter(RecyclerView.ViewHolder viewHolder) {
            CollectPositionResult cpr = new CollectPositionResult();
            cpr.adapterResult = viewHolder;
            return cpr;
        }
    }

    static class PositionConstraint {

        public static enum Type {
            scrap,
            adapter,
            adapterScrap /*first pass adapter, second pass scrap*/
        }

        Type mType;

        int mOldPos; // if VH

        int mPreLayoutPos;

        int mPostLayoutPos;

        int mValidateCount = 0;

        public static PositionConstraint scrap(int oldPos, int preLayoutPos, int postLayoutPos) {
            PositionConstraint constraint = new PositionConstraint();
            constraint.mType = Type.scrap;
            constraint.mOldPos = oldPos;
            constraint.mPreLayoutPos = preLayoutPos;
            constraint.mPostLayoutPos = postLayoutPos;
            return constraint;
        }

        public static PositionConstraint adapterScrap(int preLayoutPos, int position) {
            PositionConstraint constraint = new PositionConstraint();
            constraint.mType = Type.adapterScrap;
            constraint.mOldPos = RecyclerView.NO_POSITION;
            constraint.mPreLayoutPos = preLayoutPos;
            constraint.mPostLayoutPos = position;// adapter pos does not change
            return constraint;
        }

        public static PositionConstraint adapter(int position) {
            PositionConstraint constraint = new PositionConstraint();
            constraint.mType = Type.adapter;
            constraint.mPreLayoutPos = RecyclerView.NO_POSITION;
            constraint.mOldPos = RecyclerView.NO_POSITION;
            constraint.mPostLayoutPos = position;// adapter pos does not change
            return constraint;
        }

        public void assertValidate() {
            int expectedValidate = 0;
            if (mPreLayoutPos >= 0) {
                expectedValidate ++;
            }
            if (mPostLayoutPos >= 0) {
                expectedValidate ++;
            }
            assertEquals("should run all validates", expectedValidate, mValidateCount);
        }

        @Override
        public String toString() {
            return "Cons{" +
                    "t=" + mType.name() +
                    ", old=" + mOldPos +
                    ", pre=" + mPreLayoutPos +
                    ", post=" + mPostLayoutPos +
                    '}';
        }

        public void validate(RecyclerView.State state, CollectPositionResult result, String log) {
            mValidateCount ++;
            assertNotNull(this + ": result should not be null\n" + log, result);
            RecyclerView.ViewHolder viewHolder;
            if (mType == Type.scrap || (mType == Type.adapterScrap && !state.isPreLayout())) {
                assertNotNull(this + ": result should come from scrap\n" + log, result.scrapResult);
                viewHolder = result.scrapResult;
            } else {
                assertNotNull(this + ": result should come from adapter\n"  + log,
                        result.adapterResult);
                assertEquals(this + ": old position should be none when it came from adapter\n" + log,
                        RecyclerView.NO_POSITION, result.adapterResult.getOldPosition());
                viewHolder = result.adapterResult;
            }
            if (state.isPreLayout()) {
                assertEquals(this + ": pre-layout position should match\n" + log, mPreLayoutPos,
                        viewHolder.mPreLayoutPosition == -1 ? viewHolder.mPosition :
                        viewHolder.mPreLayoutPosition);
                assertEquals(this + ": pre-layout getPosition should match\n" + log, mPreLayoutPos,
                        viewHolder.getLayoutPosition());
                if (mType == Type.scrap) {
                    assertEquals(this + ": old position should match\n" + log, mOldPos,
                            result.scrapResult.getOldPosition());
                }
            } else if (mType == Type.adapter || mType == Type.adapterScrap || !result.scrapResult
                    .isRemoved()) {
                assertEquals(this + ": post-layout position should match\n" + log + "\n\n"
                        + viewHolder, mPostLayoutPos, viewHolder.getLayoutPosition());
            }
        }
    }
}
