/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Build;
import android.os.Parcelable;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.view.ViewGroup;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@SmallTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
@RunWith(AndroidJUnit4.class)
public class RecyclerViewCacheTest {
    TimeMockingRecyclerView mRecyclerView;
    RecyclerView.Recycler mRecycler;

    private class TimeMockingRecyclerView extends RecyclerView {
        private long mMockNanoTime = 0;

        TimeMockingRecyclerView(Context context) {
            super(context);
        }

        public void registerTimePassingMs(long ms) {
            mMockNanoTime += TimeUnit.MILLISECONDS.toNanos(ms);
        }

        @Override
        long getNanoTime() {
            return mMockNanoTime;
        }
    }

    @Before
    public void setup() throws Exception {
        mRecyclerView = new TimeMockingRecyclerView(getContext());
        mRecyclerView.onAttachedToWindow();
        mRecycler = mRecyclerView.mRecycler;
    }

    @After
    public void teardown() throws Exception {
        if (mRecyclerView.isAttachedToWindow()) {
            mRecyclerView.onDetachedFromWindow();
        }
        GapWorker gapWorker = GapWorker.sGapWorker.get();
        if (gapWorker != null) {
            assertTrue(gapWorker.mRecyclerViews.isEmpty());
        }
    }

    private Context getContext() {
        return InstrumentationRegistry.getContext();
    }

    private void layout(int width, int height) {
        mRecyclerView.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        mRecyclerView.layout(0, 0, width, height);
    }

    @Test
    public void prefetchReusesCacheItems() {
        RecyclerView.LayoutManager prefetchingLayoutManager = new RecyclerView.LayoutManager() {
            @Override
            public RecyclerView.LayoutParams generateDefaultLayoutParams() {
                return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
            }

            @Override
            public void collectAdjacentPrefetchPositions(int dx, int dy, RecyclerView.State state,
                    RecyclerView.PrefetchRegistry prefetchManager) {
                prefetchManager.addPosition(0, 0);
                prefetchManager.addPosition(1, 0);
                prefetchManager.addPosition(2, 0);
            }

            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            }
        };
        mRecyclerView.setLayoutManager(prefetchingLayoutManager);

        RecyclerView.Adapter mockAdapter = mock(RecyclerView.Adapter.class);
        when(mockAdapter.onCreateViewHolder(any(ViewGroup.class), anyInt()))
                .thenAnswer(new Answer<RecyclerView.ViewHolder>() {
                    @Override
                    public RecyclerView.ViewHolder answer(InvocationOnMock invocation)
                            throws Throwable {
                        return new RecyclerView.ViewHolder(new View(getContext())) {};
                    }
                });
        when(mockAdapter.getItemCount()).thenReturn(10);
        mRecyclerView.setAdapter(mockAdapter);

        layout(320, 320);

        verify(mockAdapter, never()).onCreateViewHolder(any(ViewGroup.class), anyInt());
        verify(mockAdapter, never()).onBindViewHolder(
                any(RecyclerView.ViewHolder.class), anyInt(), any(List.class));
        assertTrue(mRecycler.mCachedViews.isEmpty());

        // Prefetch multiple times...
        for (int i = 0; i < 4; i++) {
            mRecyclerView.mGapWorker.prefetch(RecyclerView.FOREVER_NS);

            // ...but should only see the same three items fetched/bound once each
            verify(mockAdapter, times(3)).onCreateViewHolder(any(ViewGroup.class), anyInt());
            verify(mockAdapter, times(3)).onBindViewHolder(
                    any(RecyclerView.ViewHolder.class), anyInt(), any(List.class));

            assertTrue(mRecycler.mCachedViews.size() == 3);
            CacheUtils.verifyCacheContainsPrefetchedPositions(mRecyclerView, 0, 1, 2);
        }
    }

    @Test
    public void prefetchItemsNotEvictedWithInserts() {
        mRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));

        RecyclerView.Adapter mockAdapter = mock(RecyclerView.Adapter.class);
        when(mockAdapter.onCreateViewHolder(any(ViewGroup.class), anyInt()))
                .thenAnswer(new Answer<RecyclerView.ViewHolder>() {
                    @Override
                    public RecyclerView.ViewHolder answer(InvocationOnMock invocation)
                            throws Throwable {
                        View view = new View(getContext());
                        view.setMinimumWidth(100);
                        view.setMinimumHeight(100);
                        return new RecyclerView.ViewHolder(view) {};
                    }
                });
        when(mockAdapter.getItemCount()).thenReturn(100);
        mRecyclerView.setAdapter(mockAdapter);

        layout(300, 100);

        assertEquals(2, mRecyclerView.mRecycler.mViewCacheMax);
        mRecyclerView.mPrefetchRegistry.setPrefetchVector(0, 1);
        mRecyclerView.mGapWorker.prefetch(RecyclerView.FOREVER_NS);
        assertEquals(5, mRecyclerView.mRecycler.mViewCacheMax);

        CacheUtils.verifyCacheContainsPrefetchedPositions(mRecyclerView, 3, 4, 5);

        // further views recycled, as though from scrolling, shouldn't evict prefetched views:
        mRecycler.recycleView(mRecycler.getViewForPosition(10));
        CacheUtils.verifyCacheContainsPositions(mRecyclerView, 3, 4, 5, 10);

        mRecycler.recycleView(mRecycler.getViewForPosition(20));
        CacheUtils.verifyCacheContainsPositions(mRecyclerView, 3, 4, 5, 10, 20);

        mRecycler.recycleView(mRecycler.getViewForPosition(30));
        CacheUtils.verifyCacheContainsPositions(mRecyclerView, 3, 4, 5, 20, 30);

        mRecycler.recycleView(mRecycler.getViewForPosition(40));
        CacheUtils.verifyCacheContainsPositions(mRecyclerView, 3, 4, 5, 30, 40);

        // After clearing the cache, the prefetch priorities should be cleared as well:
        mRecyclerView.mRecycler.recycleAndClearCachedViews();
        for (int i : new int[] {3, 4, 5, 50, 60, 70, 80, 90}) {
            mRecycler.recycleView(mRecycler.getViewForPosition(i));
        }

        // cache only contains most recent positions, no priority for previous prefetches:
        CacheUtils.verifyCacheContainsPositions(mRecyclerView, 50, 60, 70, 80, 90);
    }

    @Test
    public void prefetchItemsNotEvictedOnScroll() {
        mRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));

        // 100x100 pixel views
        RecyclerView.Adapter mockAdapter = mock(RecyclerView.Adapter.class);
        when(mockAdapter.onCreateViewHolder(any(ViewGroup.class), anyInt()))
                .thenAnswer(new Answer<RecyclerView.ViewHolder>() {
                    @Override
                    public RecyclerView.ViewHolder answer(InvocationOnMock invocation)
                            throws Throwable {
                        View view = new View(getContext());
                        view.setMinimumWidth(100);
                        view.setMinimumHeight(100);
                        return new RecyclerView.ViewHolder(view) {};
                    }
                });
        when(mockAdapter.getItemCount()).thenReturn(100);
        mRecyclerView.setAdapter(mockAdapter);

        // NOTE: requested cache size must be smaller than span count so two rows cannot fit
        mRecyclerView.setItemViewCacheSize(2);

        layout(300, 150);
        mRecyclerView.scrollBy(0, 75);
        assertTrue(mRecycler.mCachedViews.isEmpty());

        // rows 0, 1, and 2 are all attached and visible. Prefetch row 3:
        mRecyclerView.mPrefetchRegistry.setPrefetchVector(0, 1);
        mRecyclerView.mGapWorker.prefetch(RecyclerView.FOREVER_NS);

        // row 3 is cached:
        CacheUtils.verifyCacheContainsPositions(mRecyclerView, 9, 10, 11);
        assertTrue(mRecycler.mCachedViews.size() == 3);

        // Scroll so 1 falls off (though 3 is still not on screen)
        mRecyclerView.scrollBy(0, 50);

        // row 3 is still cached, with a couple other recycled views:
        CacheUtils.verifyCacheContainsPositions(mRecyclerView, 9, 10, 11);
        assertTrue(mRecycler.mCachedViews.size() == 5);
    }

    @Test
    public void prefetchItemsRespectDeadline() {
        mRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));

        // 100x100 pixel views
        RecyclerView.Adapter adapter = new RecyclerView.Adapter() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                mRecyclerView.registerTimePassingMs(5);
                View view = new View(getContext());
                view.setMinimumWidth(100);
                view.setMinimumHeight(100);
                return new RecyclerView.ViewHolder(view) {};
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                mRecyclerView.registerTimePassingMs(5);
            }

            @Override
            public int getItemCount() {
                return 100;
            }
        };
        mRecyclerView.setAdapter(adapter);

        layout(300, 300);

        // offset scroll so that no prefetch-able views are directly adjacent to viewport
        mRecyclerView.scrollBy(0, 50);

        assertTrue(mRecycler.mCachedViews.size() == 0);
        assertTrue(mRecyclerView.getRecycledViewPool().getRecycledViewCount(0) == 0);

        // Should take 15 ms to inflate, bind, inflate, so give 19 to be safe
        final long deadlineNs = mRecyclerView.getNanoTime() + TimeUnit.MILLISECONDS.toNanos(19);

        // Timed prefetch
        mRecyclerView.mPrefetchRegistry.setPrefetchVector(0, 1);
        mRecyclerView.mGapWorker.prefetch(deadlineNs);

        // will have enough time to inflate/bind one view, and inflate another
        assertTrue(mRecycler.mCachedViews.size() == 1);
        assertTrue(mRecyclerView.getRecycledViewPool().getRecycledViewCount(0) == 1);
        // Note: order/view below is an implementation detail
        CacheUtils.verifyCacheContainsPositions(mRecyclerView, 12);


        // Unbounded prefetch this time
        mRecyclerView.mGapWorker.prefetch(RecyclerView.FOREVER_NS);

        // Should finish all work
        assertTrue(mRecycler.mCachedViews.size() == 3);
        assertTrue(mRecyclerView.getRecycledViewPool().getRecycledViewCount(0) == 0);
        CacheUtils.verifyCacheContainsPositions(mRecyclerView, 12, 13, 14);
    }

    @Test
    public void partialPrefetchAvoidsViewRecycledCallback() {
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 100x100 pixel views
        RecyclerView.Adapter adapter = new RecyclerView.Adapter() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                mRecyclerView.registerTimePassingMs(5);
                View view = new View(getContext());
                view.setMinimumWidth(100);
                view.setMinimumHeight(100);
                return new RecyclerView.ViewHolder(view) {};
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                mRecyclerView.registerTimePassingMs(5);
            }

            @Override
            public int getItemCount() {
                return 100;
            }

            @Override
            public void onViewRecycled(RecyclerView.ViewHolder holder) {
                // verify unbound view doesn't get
                assertNotEquals(RecyclerView.NO_POSITION, holder.getAdapterPosition());
            }
        };
        mRecyclerView.setAdapter(adapter);

        layout(100, 300);

        // offset scroll so that no prefetch-able views are directly adjacent to viewport
        mRecyclerView.scrollBy(0, 50);

        assertTrue(mRecycler.mCachedViews.size() == 0);
        assertTrue(mRecyclerView.getRecycledViewPool().getRecycledViewCount(0) == 0);

        // Should take 10 ms to inflate + bind, so just give it 9 so it doesn't have time to bind
        final long deadlineNs = mRecyclerView.getNanoTime() + TimeUnit.MILLISECONDS.toNanos(9);

        // Timed prefetch
        mRecyclerView.mPrefetchRegistry.setPrefetchVector(0, 1);
        mRecyclerView.mGapWorker.prefetch(deadlineNs);

        // will have enough time to inflate but not bind one view
        assertTrue(mRecycler.mCachedViews.size() == 0);
        assertTrue(mRecyclerView.getRecycledViewPool().getRecycledViewCount(0) == 1);
        RecyclerView.ViewHolder pooledHolder = mRecyclerView.getRecycledViewPool()
                .mScrap.get(0).mScrapHeap.get(0);
        assertEquals(RecyclerView.NO_POSITION, pooledHolder.getAdapterPosition());
    }

    @Test
    public void prefetchStaggeredItemsPriority() {
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(sglm);

        // first view 50x100 pixels, rest are 100x100 so second column is offset
        mRecyclerView.setAdapter(new RecyclerView.Adapter() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new RecyclerView.ViewHolder(new View(getContext())) {};
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                holder.itemView.setMinimumWidth(100);
                holder.itemView.setMinimumHeight(position == 0 ? 50 : 100);
            }

            @Override
            public int getItemCount() {
                return 100;
            }
        });

        layout(200, 200);

        /* Each row is 50 pixels:
         * ------------- *
         *   0   |   1   *
         *   2   |   1   *
         *   2   |   3   *
         *___4___|___3___*
         *   4   |   5   *
         *   6   |   5   *
         *      ...      *
         */
        assertEquals(5, mRecyclerView.getChildCount());
        assertEquals(0, sglm.getFirstChildPosition());
        assertEquals(4, sglm.getLastChildPosition());

        // prefetching down shows 5 at 0 pixels away, 6 at 50 pixels away
        CacheUtils.verifyPositionsPrefetched(mRecyclerView, 0, 10,
                new Integer[] {5, 0}, new Integer[] {6, 50});

        // Prefetch upward shows nothing
        CacheUtils.verifyPositionsPrefetched(mRecyclerView, 0, -10);

        mRecyclerView.scrollBy(0, 100);

        /* Each row is 50 pixels:
         * ------------- *
         *   0   |   1   *
         *___2___|___1___*
         *   2   |   3   *
         *   4   |   3   *
         *   4   |   5   *
         *___6___|___5___*
         *   6   |   7   *
         *   8   |   7   *
         *      ...      *
         */

        assertEquals(5, mRecyclerView.getChildCount());
        assertEquals(2, sglm.getFirstChildPosition());
        assertEquals(6, sglm.getLastChildPosition());

        // prefetching down shows 7 at 0 pixels away, 8 at 50 pixels away
        CacheUtils.verifyPositionsPrefetched(mRecyclerView, 0, 10,
                new Integer[] {7, 0}, new Integer[] {8, 50});

        // prefetching up shows 1 is 0 pixels away, 0 at 50 pixels away
        CacheUtils.verifyPositionsPrefetched(mRecyclerView, 0, -10,
                new Integer[] {1, 0}, new Integer[] {0, 50});
    }

    @Test
    public void prefetchItemsSkipAnimations() {
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(llm);
        final int[] expandedPosition = new int[] {-1};

        final RecyclerView.Adapter adapter = new RecyclerView.Adapter() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new RecyclerView.ViewHolder(new View(parent.getContext())) {};
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                int height = expandedPosition[0] == position ? 400 : 100;
                holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(200, height));
            }

            @Override
            public int getItemCount() {
                return 10;
            }
        };

        // make move duration long enough to be able to see the effects
        RecyclerView.ItemAnimator itemAnimator = mRecyclerView.getItemAnimator();
        itemAnimator.setMoveDuration(10000);
        mRecyclerView.setAdapter(adapter);

        layout(200, 400);

        expandedPosition[0] = 1;
        // insert payload to avoid creating a new view
        adapter.notifyItemChanged(1, new Object());

        layout(200, 400);
        layout(200, 400);

        assertTrue(itemAnimator.isRunning());
        assertEquals(2, llm.getChildCount());
        assertEquals(4, mRecyclerView.getChildCount());

        // animating view should be observable as hidden, uncached...
        CacheUtils.verifyCacheDoesNotContainPositions(mRecyclerView, 2);
        assertNotNull("Animating view should be found, hidden",
                mRecyclerView.mChildHelper.findHiddenNonRemovedView(2));
        assertTrue(GapWorker.isPrefetchPositionAttached(mRecyclerView, 2));

        // ...but must not be removed for prefetch
        mRecyclerView.mPrefetchRegistry.setPrefetchVector(0, 1);
        mRecyclerView.mGapWorker.prefetch(RecyclerView.FOREVER_NS);

        assertEquals("Prefetch must target one view", 1, mRecyclerView.mPrefetchRegistry.mCount);
        int prefetchTarget = mRecyclerView.mPrefetchRegistry.mPrefetchArray[0];
        assertEquals("Prefetch must target view 2", 2, prefetchTarget);

        // animating view still observable as hidden, uncached
        CacheUtils.verifyCacheDoesNotContainPositions(mRecyclerView, 2);
        assertNotNull("Animating view should be found, hidden",
                mRecyclerView.mChildHelper.findHiddenNonRemovedView(2));
        assertTrue(GapWorker.isPrefetchPositionAttached(mRecyclerView, 2));

        assertTrue(itemAnimator.isRunning());
        assertEquals(2, llm.getChildCount());
        assertEquals(4, mRecyclerView.getChildCount());
    }

    @Test
    public void viewHolderFindsNestedRecyclerViews() {
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(llm);

        RecyclerView.Adapter mockAdapter = mock(RecyclerView.Adapter.class);
        when(mockAdapter.onCreateViewHolder(any(ViewGroup.class), anyInt()))
                .thenAnswer(new Answer<RecyclerView.ViewHolder>() {
                    @Override
                    public RecyclerView.ViewHolder answer(InvocationOnMock invocation)
                            throws Throwable {
                        View view = new RecyclerView(getContext());
                        view.setLayoutParams(new RecyclerView.LayoutParams(100, 100));
                        return new RecyclerView.ViewHolder(view) {};
                    }
                });
        when(mockAdapter.getItemCount()).thenReturn(100);
        mRecyclerView.setAdapter(mockAdapter);

        layout(100, 200);

        verify(mockAdapter, times(2)).onCreateViewHolder(any(ViewGroup.class), anyInt());
        verify(mockAdapter, times(2)).onBindViewHolder(
                argThat(new BaseMatcher<RecyclerView.ViewHolder>() {
                    @Override
                    public boolean matches(Object item) {
                        RecyclerView.ViewHolder holder = (RecyclerView.ViewHolder) item;
                        return holder.itemView == holder.mNestedRecyclerView;
                    }

                    @Override
                    public void describeTo(Description description) { }
                }),
                anyInt(),
                any(List.class));
    }

    static class InnerAdapter extends RecyclerView.Adapter<InnerAdapter.ViewHolder> {
        private static final int INNER_ITEM_COUNT = 20;
        static class ViewHolder extends RecyclerView.ViewHolder {
            ViewHolder(View itemView) {
                super(itemView);
            }
        }

        InnerAdapter() {}

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = new View(parent.getContext());
            view.setLayoutParams(new RecyclerView.LayoutParams(100, 100));
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {}

        @Override
        public int getItemCount() {
            return INNER_ITEM_COUNT;
        }
    }

    static class OuterAdapter extends RecyclerView.Adapter<OuterAdapter.ViewHolder> {
        private static final int OUTER_ITEM_COUNT = 10;

        private boolean mReverseInner;

        static class ViewHolder extends RecyclerView.ViewHolder {
            private final RecyclerView mRecyclerView;
            ViewHolder(RecyclerView itemView) {
                super(itemView);
                mRecyclerView = itemView;
            }
        }

        ArrayList<InnerAdapter> mAdapters = new ArrayList<>();
        ArrayList<Parcelable> mSavedStates = new ArrayList<>();
        RecyclerView.RecycledViewPool mSharedPool = new RecyclerView.RecycledViewPool();

        OuterAdapter() {
            this(false);
        }

        OuterAdapter(boolean reverseInner) {
            mReverseInner = reverseInner;
            for (int i = 0; i <= OUTER_ITEM_COUNT; i++) {
                mAdapters.add(new InnerAdapter());
                mSavedStates.add(null);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            RecyclerView rv = new RecyclerView(parent.getContext());
            rv.setLayoutManager(new LinearLayoutManager(parent.getContext(),
                    LinearLayoutManager.HORIZONTAL, mReverseInner));
            rv.setRecycledViewPool(mSharedPool);
            rv.setLayoutParams(new RecyclerView.LayoutParams(200, 100));
            return new ViewHolder(rv);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            // Tests may rely on bound holders not being shared between inner adapters,
            // since we force recycle here
            holder.mRecyclerView.swapAdapter(mAdapters.get(position), true);

            Parcelable savedState = mSavedStates.get(position);
            if (savedState != null) {
                holder.mRecyclerView.getLayoutManager().onRestoreInstanceState(savedState);
                mSavedStates.set(position, null);
            }
        }

        @Override
        public void onViewRecycled(ViewHolder holder) {
            mSavedStates.set(holder.getAdapterPosition(),
                    holder.mRecyclerView.getLayoutManager().onSaveInstanceState());
        }

        @Override
        public int getItemCount() {
            return OUTER_ITEM_COUNT;
        }
    }

    @Test
    public void nestedPrefetchSimple() {
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        assertEquals(2, llm.getInitialItemPrefetchCount());

        mRecyclerView.setLayoutManager(llm);
        mRecyclerView.setAdapter(new OuterAdapter());

        layout(200, 200);
        mRecyclerView.mPrefetchRegistry.setPrefetchVector(0, 1);

        // prefetch 2 (default)
        mRecyclerView.mGapWorker.prefetch(RecyclerView.FOREVER_NS);
        RecyclerView.ViewHolder holder = CacheUtils.peekAtCachedViewForPosition(mRecyclerView, 2);
        assertNotNull(holder);
        assertNotNull(holder.mNestedRecyclerView);
        CacheUtils.verifyCacheContainsPrefetchedPositions(holder.mNestedRecyclerView, 0, 1);

        // prefetch 4
        ((LinearLayoutManager) holder.mNestedRecyclerView.getLayoutManager())
                .setInitialPrefetchItemCount(4);
        mRecyclerView.mGapWorker.prefetch(RecyclerView.FOREVER_NS);
        CacheUtils.verifyCacheContainsPrefetchedPositions(holder.mNestedRecyclerView, 0, 1, 2, 3);
    }

    @Test
    public void nestedPrefetchReverseInner() {
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setAdapter(new OuterAdapter(/* reverseInner = */ true));

        layout(200, 200);
        mRecyclerView.mPrefetchRegistry.setPrefetchVector(0, 1);

        mRecyclerView.mGapWorker.prefetch(RecyclerView.FOREVER_NS);
        RecyclerView.ViewHolder holder = CacheUtils.peekAtCachedViewForPosition(mRecyclerView, 2);

        // anchor from right side, should see last two positions
        CacheUtils.verifyCacheContainsPrefetchedPositions(holder.mNestedRecyclerView, 18, 19);
    }

    @Test
    public void nestedPrefetchOffset() {
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setAdapter(new OuterAdapter());

        layout(200, 200);

        // Scroll top row by 5.5 items, verify positions 5, 6, 7 showing
        RecyclerView inner = (RecyclerView) mRecyclerView.getChildAt(0);
        inner.scrollBy(550, 0);
        assertEquals(5, RecyclerView.getChildViewHolderInt(inner.getChildAt(0)).mPosition);
        assertEquals(6, RecyclerView.getChildViewHolderInt(inner.getChildAt(1)).mPosition);
        assertEquals(7, RecyclerView.getChildViewHolderInt(inner.getChildAt(2)).mPosition);

        // scroll down 4 rows, up 3 so row 0 is adjacent but uncached
        mRecyclerView.scrollBy(0, 400);
        mRecyclerView.scrollBy(0, -300);

        // top row no longer present
        CacheUtils.verifyCacheDoesNotContainPositions(mRecyclerView, 0);

        // prefetch upward, and validate that we've gotten the top row with correct offsets
        mRecyclerView.mPrefetchRegistry.setPrefetchVector(0, -1);
        mRecyclerView.mGapWorker.prefetch(RecyclerView.FOREVER_NS);
        inner = (RecyclerView) CacheUtils.peekAtCachedViewForPosition(mRecyclerView, 0).itemView;
        CacheUtils.verifyCacheContainsPrefetchedPositions(inner, 5, 6);

        // prefetch 4
        ((LinearLayoutManager) inner.getLayoutManager()).setInitialPrefetchItemCount(4);
        mRecyclerView.mGapWorker.prefetch(RecyclerView.FOREVER_NS);
        CacheUtils.verifyCacheContainsPrefetchedPositions(inner, 5, 6, 7, 8);
    }
}
