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

import android.view.View;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class RecyclerViewLayoutTest extends BaseRecyclerViewInstrumentationTest {

    private static final boolean DEBUG = false;

    public RecyclerViewLayoutTest() {
        super(DEBUG);
    }

    public void testFindViewById() throws Throwable {
        findViewByIdTest(false);
        removeRecyclerView();
        findViewByIdTest(true);
    }

    public void findViewByIdTest(final boolean supportPredictive) throws Throwable {
        final RecyclerView recyclerView = new RecyclerView(getActivity());
        final int initialAdapterSize = 20;
        final TestAdapter adapter = new TestAdapter(initialAdapterSize);
        final int deleteStart = 6;
        final int deleteCount = 5;
        recyclerView.setAdapter(adapter);
        final AtomicBoolean assertPositions = new AtomicBoolean(false);
        TestLayoutManager lm = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                super.onLayoutChildren(recycler, state);
                if (assertPositions.get()) {
                    if (state.isPreLayout()) {
                        for (int i = 0; i < deleteStart; i ++) {
                            View view = findViewByPosition(i);
                            assertNotNull("find view by position for existing items should work "
                                    + "fine", view);
                            assertFalse("view should not be marked as removed",
                                    ((RecyclerView.LayoutParams) view.getLayoutParams())
                                            .isItemRemoved());
                        }
                        for (int i = 0;  i < deleteCount; i++) {
                            View view = findViewByPosition(i + deleteStart);
                            assertNotNull("find view by position should work fine for removed "
                                    + "views in pre-layout", view);
                            assertTrue("view should be marked as removed",
                                    ((RecyclerView.LayoutParams) view.getLayoutParams())
                                            .isItemRemoved());
                        }
                        for (int i = deleteStart + deleteCount; i < 20; i++) {
                            View view = findViewByPosition(i);
                            assertNotNull(view);
                            assertFalse("view should not be marked as removed",
                                    ((RecyclerView.LayoutParams) view.getLayoutParams())
                                            .isItemRemoved());
                        }
                    } else {
                        for (int i = 0; i < initialAdapterSize - deleteCount; i ++) {
                            View view = findViewByPosition(i);
                            assertNotNull("find view by position for existing item " + i +
                                    " should work fine. child count:" + getChildCount(), view);
                            TestViewHolder viewHolder =
                                    (TestViewHolder) mRecyclerView.getChildViewHolder(view);
                            assertSame("should be the correct item " + viewHolder
                                    ,viewHolder.mBindedItem,
                                    adapter.mItems.get(viewHolder.mPosition));
                            assertFalse("view should not be marked as removed",
                                    ((RecyclerView.LayoutParams) view.getLayoutParams())
                                            .isItemRemoved());
                        }
                    }
                }
                detachAndScrapAttachedViews(recycler);
                layoutRange(recycler, state.getItemCount() - 1, -1);
                layoutLatch.countDown();
            }

            @Override
            public boolean supportsPredictiveItemAnimations() {
                return supportPredictive;
            }
        };
        recyclerView.setLayoutManager(lm);
        lm.expectLayouts(1);
        setRecyclerView(recyclerView);
        lm.waitForLayout(2);
        getInstrumentation().waitForIdleSync();

        assertPositions.set(true);
        lm.expectLayouts(supportPredictive ? 2 : 1);
        adapter.deleteAndNotify(new int[]{deleteStart, deleteCount - 1}, new int[]{deleteStart, 1});
        lm.waitForLayout(2);
    }

    public void testTypeForCache() throws Throwable {
        final AtomicInteger viewType = new AtomicInteger(1);
        final TestAdapter adapter = new TestAdapter(100) {
            @Override
            public int getItemViewType(int position) {
                return viewType.get();
            }

            @Override
            public long getItemId(int position) {
                return mItems.get(position).mId;
            }
        };
        adapter.setHasStableIds(true);
        final AtomicInteger layoutStart = new AtomicInteger(2);
        final int childCount = 10;
        final TestLayoutManager lm = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                super.onLayoutChildren(recycler, state);
                detachAndScrapAttachedViews(recycler);
                layoutRange(recycler, layoutStart.get(), layoutStart.get() + childCount);
                layoutLatch.countDown();
            }
        };
        final RecyclerView recyclerView = new RecyclerView(getActivity());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(lm);
        recyclerView.setItemViewCacheSize(10);
        lm.expectLayouts(1);
        setRecyclerView(recyclerView);
        lm.waitForLayout(2);
        getInstrumentation().waitForIdleSync();
        layoutStart.set(4); // trigger a cache for 3,4
        lm.expectLayouts(1);
        requestLayoutOnUIThread(recyclerView);
        lm.waitForLayout(2);
        //
        viewType.incrementAndGet();
        layoutStart.set(2); // go back to bring views from cache
        lm.expectLayouts(1);
        adapter.mItems.remove(1);
        adapter.notifyChange();
        lm.waitForLayout(2);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 2; i < 4; i++) {
                    RecyclerView.ViewHolder vh = recyclerView.findViewHolderForPosition(2);
                    assertEquals("View holder's type should match latest type", viewType.get(),
                            vh.getItemViewType());
                }
            }
        });
    }

    public void testTypeForExistingViews() throws Throwable {
        final AtomicInteger viewType = new AtomicInteger(1);
        final int invalidatedCount = 2;
        final int layoutStart = 2;
        final TestAdapter adapter = new TestAdapter(100) {
            @Override
            public int getItemViewType(int position) {
                return viewType.get();
            }

            @Override
            public void onBindViewHolder(TestViewHolder holder,
                    int position) {
                super.onBindViewHolder(holder, position);
                if (position >= layoutStart && position < invalidatedCount + layoutStart) {
                    try {
                        assertEquals("holder type should match current view type at position " +
                                position, viewType.get(), holder.getItemViewType());
                    } catch (Throwable t) {
                        postExceptionToInstrumentation(t);
                    }
                }
            }

            @Override
            public long getItemId(int position) {
                return mItems.get(position).mId;
            }
        };
        adapter.setHasStableIds(true);

        final int childCount = 10;
        final TestLayoutManager lm = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                super.onLayoutChildren(recycler, state);
                detachAndScrapAttachedViews(recycler);
                layoutRange(recycler, layoutStart, layoutStart + childCount);
                layoutLatch.countDown();
            }
        };
        final RecyclerView recyclerView = new RecyclerView(getActivity());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(lm);
        lm.expectLayouts(1);
        setRecyclerView(recyclerView);
        lm.waitForLayout(2);
        getInstrumentation().waitForIdleSync();
        viewType.incrementAndGet();
        lm.expectLayouts(1);
        adapter.notifyItemChange(layoutStart, invalidatedCount);
        lm.waitForLayout(2);
        checkForMainThreadException();
    }


    public void testState() throws Throwable {
        final TestAdapter adapter = new TestAdapter(10);
        final RecyclerView recyclerView = new RecyclerView(getActivity());
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(null);
        final AtomicInteger itemCount = new AtomicInteger();
        final AtomicBoolean structureChanged = new AtomicBoolean();
        TestLayoutManager testLayoutManager = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                detachAndScrapAttachedViews(recycler);
                layoutRange(recycler, 0, state.getItemCount());
                itemCount.set(state.getItemCount());
                structureChanged.set(state.didStructureChange());
                layoutLatch.countDown();
            }
        };
        recyclerView.setLayoutManager(testLayoutManager);
        testLayoutManager.expectLayouts(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                getActivity().mContainer.addView(recyclerView);
            }
        });
        testLayoutManager.waitForLayout(2, TimeUnit.SECONDS);

        assertEquals("item count in state should be correct", adapter.getItemCount()
                , itemCount.get());
        assertEquals("structure changed should be true for first layout", true,
                structureChanged.get());
        Thread.sleep(1000); //wait for other layouts.
        testLayoutManager.expectLayouts(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                recyclerView.requestLayout();
            }
        });
        testLayoutManager.waitForLayout(2);
        assertEquals("in second layout,structure changed should be false", false,
                structureChanged.get());
        testLayoutManager.expectLayouts(1); //
        adapter.deleteAndNotify(3, 2);
        testLayoutManager.waitForLayout(2);
        assertEquals("when items are removed, item count in state should be updated",
                adapter.getItemCount(),
                itemCount.get());
        assertEquals("structure changed should be true when items are removed", true,
                structureChanged.get());
        testLayoutManager.expectLayouts(1);
        adapter.addAndNotify(2, 5);
        testLayoutManager.waitForLayout(2);

        assertEquals("when items are added, item count in state should be updated",
                adapter.getItemCount(),
                itemCount.get());
        assertEquals("structure changed should be true when items are removed", true,
                structureChanged.get());

    }

}
