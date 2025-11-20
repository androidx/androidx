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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Parcelable;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unchecked")
@MediumTest
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

        @Override
        public int getWindowVisibility() {
            // Pretend to be visible to avoid being filtered out
            return View.VISIBLE;
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
        return ApplicationProvider.getApplicationContext();
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
                    LayoutPrefetchRegistry prefetchManager) {
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
    public void prefetchIsComputingLayout() {
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

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
                        assertTrue(mRecyclerView.isComputingLayout());
                        return new RecyclerView.ViewHolder(view) {};
                    }
                });
        when(mockAdapter.getItemCount()).thenReturn(100);
        mRecyclerView.setAdapter(mockAdapter);

        layout(100, 100);

        verify(mockAdapter, times(1)).onCreateViewHolder(mRecyclerView, 0);

        // prefetch an item, should still observe isComputingLayout in that create
        mRecyclerView.mPrefetchRegistry.setPrefetchVector(0, 1);
        mRecyclerView.mGapWorker.prefetch(RecyclerView.FOREVER_NS);

        verify(mockAdapter, times(2)).onCreateViewHolder(mRecyclerView, 0);
    }

    @Test
    public void prefetchAfterOrientationChange() {
        LinearLayoutManager layout = new LinearLayoutManager(getContext(),
                LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(layout);

        // 100x100 pixel views
        mRecyclerView.setAdapter(new RecyclerView.Adapter() {
            @Override
            public RecyclerView.@NonNull ViewHolder onCreateViewHolder(
                    @NonNull ViewGroup parent, int viewType) {
                View view = new View(getContext());
                view.setMinimumWidth(100);
                view.setMinimumHeight(100);
                assertTrue(mRecyclerView.isComputingLayout());
                return new RecyclerView.ViewHolder(view) {};
            }

            @Override
            public void onBindViewHolder(RecyclerView.@NonNull ViewHolder holder, int position) {}

            @Override
            public int getItemCount() {
                return 100;
            }
        });

        layout(100, 100);

        layout.setOrientation(LinearLayoutManager.HORIZONTAL);

        // Prefetch an item after changing orientation, before layout - shouldn't crash
        mRecyclerView.mPrefetchRegistry.setPrefetchVector(1, 1);
        mRecyclerView.mGapWorker.prefetch(RecyclerView.FOREVER_NS);
    }

    @Test
    public void prefetchDrag() {
        // event dispatch requires a parent
        ViewGroup parent = new FrameLayout(getContext());
        parent.addView(mRecyclerView);


        mRecyclerView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));

        // 1000x1000 pixel views
        RecyclerView.Adapter adapter = new RecyclerView.Adapter() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(
                    @NonNull ViewGroup parent, int viewType) {
                mRecyclerView.registerTimePassingMs(5);
                View view = new View(getContext());
                view.setMinimumWidth(1000);
                view.setMinimumHeight(1000);
                return new RecyclerView.ViewHolder(view) {};
            }

            @Override
            public void onBindViewHolder(RecyclerView.@NonNull ViewHolder holder, int position) {
                mRecyclerView.registerTimePassingMs(5);
            }

            @Override
            public int getItemCount() {
                return 100;
            }
        };
        mRecyclerView.setAdapter(adapter);

        layout(1000, 1000);

        long time = SystemClock.uptimeMillis();
        mRecyclerView.onTouchEvent(
                MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, 500, 1000, 0));

        assertEquals(0, mRecyclerView.mPrefetchRegistry.mPrefetchDx);
        assertEquals(0, mRecyclerView.mPrefetchRegistry.mPrefetchDy);

        // Consume slop
        mRecyclerView.onTouchEvent(
                MotionEvent.obtain(time, time, MotionEvent.ACTION_MOVE, 50, 500, 0));

        // move by 0,30
        mRecyclerView.onTouchEvent(
                MotionEvent.obtain(time, time, MotionEvent.ACTION_MOVE, 50, 470, 0));
        assertEquals(0, mRecyclerView.mPrefetchRegistry.mPrefetchDx);
        assertEquals(30, mRecyclerView.mPrefetchRegistry.mPrefetchDy);

        // move by 10,15
        mRecyclerView.onTouchEvent(
                MotionEvent.obtain(time, time, MotionEvent.ACTION_MOVE, 40, 455, 0));
        assertEquals(10, mRecyclerView.mPrefetchRegistry.mPrefetchDx);
        assertEquals(15, mRecyclerView.mPrefetchRegistry.mPrefetchDy);

        // move by 0,0 - IGNORED
        mRecyclerView.onTouchEvent(
                MotionEvent.obtain(time, time, MotionEvent.ACTION_MOVE, 40, 455, 0));
        assertEquals(10, mRecyclerView.mPrefetchRegistry.mPrefetchDx); // same as prev
        assertEquals(15, mRecyclerView.mPrefetchRegistry.mPrefetchDy); // same as prev
    }

    @Test
    public void prefetchItemsRespectDeadline() {
        mRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));

        // 100x100 pixel views
        RecyclerView.Adapter adapter = new RecyclerView.Adapter() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(
                    @NonNull ViewGroup parent, int viewType) {
                mRecyclerView.registerTimePassingMs(5);
                View view = new View(getContext());
                view.setMinimumWidth(100);
                view.setMinimumHeight(100);
                return new RecyclerView.ViewHolder(view) {};
            }

            @Override
            public void onBindViewHolder(
                    RecyclerView.@NonNull ViewHolder holder, int position) {
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
            public RecyclerView.ViewHolder onCreateViewHolder(
                    @NonNull ViewGroup parent, int viewType) {
                mRecyclerView.registerTimePassingMs(5);
                View view = new View(getContext());
                view.setMinimumWidth(100);
                view.setMinimumHeight(100);
                return new RecyclerView.ViewHolder(view) {};
            }

            @Override
            public void onBindViewHolder(RecyclerView.@NonNull ViewHolder holder, int position) {
                mRecyclerView.registerTimePassingMs(5);
            }

            @Override
            public int getItemCount() {
                return 100;
            }

            @Override
            public void onViewRecycled(RecyclerView.@NonNull ViewHolder holder) {
                // verify unbound view doesn't get
                assertNotEquals(RecyclerView.NO_POSITION, holder.getAbsoluteAdapterPosition());
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
        assertEquals(RecyclerView.NO_POSITION, pooledHolder.getAbsoluteAdapterPosition());
    }

    @Test
    public void prefetchStaggeredItemsPriority() {
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(sglm);

        // first view 50x100 pixels, rest are 100x100 so second column is offset
        mRecyclerView.setAdapter(new RecyclerView.Adapter() {
            @Override
            public RecyclerView.@NonNull ViewHolder onCreateViewHolder(
                    @NonNull ViewGroup parent, int viewType) {
                return new RecyclerView.ViewHolder(new View(getContext())) {};
            }

            @Override
            public void onBindViewHolder(RecyclerView.@NonNull ViewHolder holder, int position) {
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
    public void prefetchStaggeredPastBoundary() {
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(sglm);
        mRecyclerView.setAdapter(new RecyclerView.Adapter() {
            @Override
            public RecyclerView.@NonNull ViewHolder onCreateViewHolder(
                    @NonNull ViewGroup parent, int viewType) {
                return new RecyclerView.ViewHolder(new View(getContext())) {};
            }

            @Override
            public void onBindViewHolder(RecyclerView.@NonNull ViewHolder holder, int position) {
                holder.itemView.setMinimumWidth(100);
                holder.itemView.setMinimumHeight(position == 0 ? 100 : 200);
            }

            @Override
            public int getItemCount() {
                return 2;
            }
        });

        layout(200, 100);
        mRecyclerView.scrollBy(0, 50);

        /* Each row is 50 pixels:
         * ------------- *
         *___0___|___1___*
         *   0   |   1   *
         *_______|___1___*
         *       |   1   *
         */
        assertEquals(2, mRecyclerView.getChildCount());
        assertEquals(0, sglm.getFirstChildPosition());
        assertEquals(1, sglm.getLastChildPosition());

        // prefetch upward gets nothing
        CacheUtils.verifyPositionsPrefetched(mRecyclerView, 0, -10);

        // prefetch downward gets nothing (and doesn't crash...)
        CacheUtils.verifyPositionsPrefetched(mRecyclerView, 0, 10);
    }

    @Test
    public void prefetchItemsSkipAnimations() {
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(llm);
        final int[] expandedPosition = new int[] {-1};

        final RecyclerView.Adapter adapter = new RecyclerView.Adapter() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(
                    @NonNull ViewGroup parent, int viewType) {
                return new RecyclerView.ViewHolder(new View(parent.getContext())) {};
            }

            @Override
            public void onBindViewHolder(RecyclerView.@NonNull ViewHolder holder, int position) {
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
                argThat(new ArgumentMatcher<RecyclerView.ViewHolder>() {
                    @Override
                    public boolean matches(RecyclerView.ViewHolder holder) {
                        return holder.itemView == holder.mNestedRecyclerView.get();
                    }
                }),
                anyInt(),
                any(List.class));
    }

    class InnerAdapter extends RecyclerView.Adapter<InnerAdapter.ViewHolder> {
        private static final int INNER_ITEM_COUNT = 20;
        int mItemsBound = 0;

        class ViewHolder extends RecyclerView.ViewHolder {
            ViewHolder(View itemView) {
                super(itemView);
            }
        }

        InnerAdapter() {}

        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            mRecyclerView.registerTimePassingMs(5);
            View view = new View(parent.getContext());
            view.setLayoutParams(new RecyclerView.LayoutParams(100, 100));
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            mRecyclerView.registerTimePassingMs(5);
            mItemsBound++;
        }

        @Override
        public int getItemCount() {
            return INNER_ITEM_COUNT;
        }
    }

    class OuterAdapter extends RecyclerView.Adapter<OuterAdapter.ViewHolder> {

        private boolean mReverseInner;

        class ViewHolder extends RecyclerView.ViewHolder {
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
            this(reverseInner, 10);
        }

        OuterAdapter(boolean reverseInner, int itemCount) {
            mReverseInner = reverseInner;
            for (int i = 0; i < itemCount; i++) {
                mAdapters.add(new InnerAdapter());
                mSavedStates.add(null);
            }
        }

        void addItem() {
            int index = getItemCount();
            mAdapters.add(new InnerAdapter());
            mSavedStates.add(null);
            notifyItemInserted(index);
        }

        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            mRecyclerView.registerTimePassingMs(5);

            RecyclerView rv = new RecyclerView(parent.getContext()) {
                @Override
                public int getWindowVisibility() {
                    // Pretend to be visible to avoid being filtered out
                    return View.VISIBLE;
                }
            };
            rv.setLayoutManager(new LinearLayoutManager(parent.getContext(),
                    LinearLayoutManager.HORIZONTAL, mReverseInner));
            rv.setRecycledViewPool(mSharedPool);
            rv.setLayoutParams(new RecyclerView.LayoutParams(200, 100));
            return new ViewHolder(rv);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            mRecyclerView.registerTimePassingMs(5);

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
        public void onViewRecycled(@NonNull ViewHolder holder) {
            mSavedStates.set(holder.getAbsoluteAdapterPosition(),
                    holder.mRecyclerView.getLayoutManager().onSaveInstanceState());
        }

        @Override
        public int getItemCount() {
            return mAdapters.size();
        }
    }

    @Test
    public void nestedPrefetchSimple() {
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        assertEquals(2, llm.getInitialPrefetchItemCount());

        mRecyclerView.setLayoutManager(llm);
        mRecyclerView.setAdapter(new OuterAdapter());

        layout(200, 200);
        mRecyclerView.mPrefetchRegistry.setPrefetchVector(0, 1);

        // prefetch 2 (default)
        mRecyclerView.mGapWorker.prefetch(RecyclerView.FOREVER_NS);
        RecyclerView.ViewHolder holder = CacheUtils.peekAtCachedViewForPosition(mRecyclerView, 2);
        assertNotNull(holder);
        assertNotNull(holder.mNestedRecyclerView);
        RecyclerView innerView = holder.mNestedRecyclerView.get();
        CacheUtils.verifyCacheContainsPrefetchedPositions(innerView, 0, 1);

        // prefetch 4
        ((LinearLayoutManager) innerView.getLayoutManager())
                .setInitialPrefetchItemCount(4);
        mRecyclerView.mGapWorker.prefetch(RecyclerView.FOREVER_NS);
        CacheUtils.verifyCacheContainsPrefetchedPositions(innerView, 0, 1, 2, 3);
    }

    @Test
    public void nestedPrefetchNotClearInnerStructureChangeFlag() {
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        assertEquals(2, llm.getInitialPrefetchItemCount());

        mRecyclerView.setLayoutManager(llm);
        mRecyclerView.setAdapter(new OuterAdapter());

        layout(200, 200);
        mRecyclerView.mPrefetchRegistry.setPrefetchVector(0, 1);

        // prefetch 2 (default)
        mRecyclerView.mGapWorker.prefetch(RecyclerView.FOREVER_NS);
        RecyclerView.ViewHolder holder = CacheUtils.peekAtCachedViewForPosition(mRecyclerView, 2);
        assertNotNull(holder);
        assertNotNull(holder.mNestedRecyclerView);
        RecyclerView innerView = holder.mNestedRecyclerView.get();
        RecyclerView.Adapter innerAdapter = innerView.getAdapter();
        CacheUtils.verifyCacheContainsPrefetchedPositions(innerView, 0, 1);
        // mStructureChanged is initially true before first layout pass.
        assertTrue(innerView.mState.mStructureChanged);
        assertTrue(innerView.hasPendingAdapterUpdates());

        // layout position 2 and clear mStructureChanged
        mRecyclerView.scrollToPosition(2);
        layout(200, 200);
        mRecyclerView.scrollToPosition(0);
        layout(200, 200);
        assertFalse(innerView.mState.mStructureChanged);
        assertFalse(innerView.hasPendingAdapterUpdates());

        // notify change on the cached innerView.
        innerAdapter.notifyDataSetChanged();
        assertTrue(innerView.mState.mStructureChanged);
        assertTrue(innerView.hasPendingAdapterUpdates());

        // prefetch again
        mRecyclerView.mPrefetchRegistry.setPrefetchVector(0, 1);
        ((LinearLayoutManager) innerView.getLayoutManager())
                .setInitialPrefetchItemCount(2);
        mRecyclerView.mGapWorker.prefetch(RecyclerView.FOREVER_NS);
        CacheUtils.verifyCacheContainsPrefetchedPositions(innerView, 0, 1);

        // The re-prefetch is not necessary get the same inner view but we will get same Adapter
        holder = CacheUtils.peekAtCachedViewForPosition(mRecyclerView, 2);
        innerView = holder.mNestedRecyclerView.get();
        assertSame(innerAdapter, innerView.getAdapter());
        // prefetch shouldn't clear the mStructureChanged flag
        assertTrue(innerView.mState.mStructureChanged);
        assertTrue(innerView.hasPendingAdapterUpdates());
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
        CacheUtils.verifyCacheContainsPrefetchedPositions(holder.mNestedRecyclerView.get(), 18, 19);
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

    @Test
    public void nestedPrefetchNotReset() {
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        OuterAdapter outerAdapter = new OuterAdapter();
        mRecyclerView.setAdapter(outerAdapter);

        layout(200, 200);

        mRecyclerView.mPrefetchRegistry.setPrefetchVector(0, 1);

        // prefetch row 2, items 0 & 1
        assertEquals(0, outerAdapter.mAdapters.get(2).mItemsBound);
        mRecyclerView.mGapWorker.prefetch(RecyclerView.FOREVER_NS);
        RecyclerView.ViewHolder holder = CacheUtils.peekAtCachedViewForPosition(mRecyclerView, 2);
        RecyclerView innerRecyclerView = holder.mNestedRecyclerView.get();

        assertNotNull(innerRecyclerView);
        CacheUtils.verifyCacheContainsPrefetchedPositions(innerRecyclerView, 0, 1);
        assertEquals(2, outerAdapter.mAdapters.get(2).mItemsBound);

        // new row comes on, triggers layout...
        mRecyclerView.scrollBy(0, 50);

        // ... which shouldn't require new items to be bound,
        // as prefetch has already done that work
        assertEquals(2, outerAdapter.mAdapters.get(2).mItemsBound);
    }

    static void validateRvChildrenValid(RecyclerView recyclerView, int childCount) {
        ChildHelper childHelper = recyclerView.mChildHelper;

        assertEquals(childCount, childHelper.getUnfilteredChildCount());
        for (int i = 0; i < childHelper.getUnfilteredChildCount(); i++) {
            assertFalse(recyclerView.getChildViewHolder(
                    childHelper.getUnfilteredChildAt(i)).isInvalid());
        }
    }

    @Test
    public void nestedPrefetchCacheNotTouched() {
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        OuterAdapter outerAdapter = new OuterAdapter();
        mRecyclerView.setAdapter(outerAdapter);

        layout(200, 200);
        mRecyclerView.scrollBy(0, 100);

        // item 0 is cached
        assertEquals(2, outerAdapter.mAdapters.get(0).mItemsBound);
        RecyclerView.ViewHolder holder = CacheUtils.peekAtCachedViewForPosition(mRecyclerView, 0);
        validateRvChildrenValid(holder.mNestedRecyclerView.get(), 2);

        // try and prefetch it
        mRecyclerView.mPrefetchRegistry.setPrefetchVector(0, -1);
        mRecyclerView.mGapWorker.prefetch(RecyclerView.FOREVER_NS);

        // make sure cache's inner items aren't rebound unnecessarily
        assertEquals(2, outerAdapter.mAdapters.get(0).mItemsBound);
        validateRvChildrenValid(holder.mNestedRecyclerView.get(), 2);
    }

    @Test
    public void nestedRemoveAnimatingView() {
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        OuterAdapter outerAdapter = new OuterAdapter(false, 1);
        mRecyclerView.setAdapter(outerAdapter);
        mRecyclerView.getItemAnimator().setAddDuration(TimeUnit.MILLISECONDS.toNanos(30));

        layout(200, 200);

        // Insert 3 items - only first one in viewport, so only it animates
        for (int i = 0; i < 3; i++) {
            outerAdapter.addItem();
        }
        layout(200, 200); // layout again to kick off animation


        // item 1 is animating, so scroll it out of viewport
        mRecyclerView.scrollBy(0, 200);

        // 2 items attached, 2 cached
        assertEquals(2, mRecyclerView.mChildHelper.getUnfilteredChildCount());
        assertEquals(2, mRecycler.mCachedViews.size());
        CacheUtils.verifyCacheContainsPositions(mRecyclerView, 0, 1);
        assertEquals(0, mRecyclerView.getRecycledViewPool().getRecycledViewCount(0));

        // The animation should automatically stop when removing the addition view
        assertFalse(mRecyclerView.getItemAnimator().isRunning());

        for (RecyclerView.ViewHolder viewHolder : mRecycler.mCachedViews) {
            assertNotNull(viewHolder.mNestedRecyclerView);
        }
    }

    @Test
    public void nestedExpandCacheCorrectly() {
        final int DEFAULT_CACHE_SIZE = RecyclerView.Recycler.DEFAULT_CACHE_SIZE;

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        OuterAdapter outerAdapter = new OuterAdapter();
        mRecyclerView.setAdapter(outerAdapter);

        layout(200, 200);

        mRecyclerView.mPrefetchRegistry.setPrefetchVector(0, 1);
        mRecyclerView.mGapWorker.prefetch(RecyclerView.FOREVER_NS);

        // after initial prefetch, view cache max expanded by number of inner items prefetched (2)
        RecyclerView.ViewHolder holder = CacheUtils.peekAtCachedViewForPosition(mRecyclerView, 2);
        RecyclerView innerView = holder.mNestedRecyclerView.get();
        assertTrue(innerView.getLayoutManager().mPrefetchMaxObservedInInitialPrefetch);
        assertEquals(2, innerView.getLayoutManager().mPrefetchMaxCountObserved);
        assertEquals(2 + DEFAULT_CACHE_SIZE, innerView.mRecycler.mViewCacheMax);

        try {
            // Note: As a hack, we not only must manually dispatch attachToWindow(), but we
            // also have to be careful to call innerView.mGapWorker below. mRecyclerView.mGapWorker
            // is registered to the wrong thread, since @setup is called on a different thread
            // from @Test. Assert this, so this test can be fixed when setup == test thread.
            assertEquals(1, mRecyclerView.mGapWorker.mRecyclerViews.size());
            assertFalse(innerView.isAttachedToWindow());
            innerView.onAttachedToWindow();

            // bring prefetch view into viewport, at which point it shouldn't have cache expanded...
            mRecyclerView.scrollBy(0, 100);
            assertFalse(innerView.getLayoutManager().mPrefetchMaxObservedInInitialPrefetch);
            assertEquals(0, innerView.getLayoutManager().mPrefetchMaxCountObserved);
            assertEquals(DEFAULT_CACHE_SIZE, innerView.mRecycler.mViewCacheMax);

            // until a valid horizontal prefetch caches an item, and expands view count by one
            innerView.mPrefetchRegistry.setPrefetchVector(1, 0);
            innerView.mGapWorker.prefetch(RecyclerView.FOREVER_NS); // NB: must be innerView.mGapWorker
            assertFalse(innerView.getLayoutManager().mPrefetchMaxObservedInInitialPrefetch);
            assertEquals(1, innerView.getLayoutManager().mPrefetchMaxCountObserved);
            assertEquals(1 + DEFAULT_CACHE_SIZE, innerView.mRecycler.mViewCacheMax);
        } finally {
            if (innerView.isAttachedToWindow()) {
                innerView.onDetachedFromWindow();
            }
        }
    }

    /**
     * Similar to OuterAdapter above, but uses notifyDataSetChanged() instead of set/swapAdapter
     * to update data for the inner RecyclerViews when containing ViewHolder is bound.
     */
    class OuterNotifyAdapter extends RecyclerView.Adapter<OuterNotifyAdapter.ViewHolder> {
        private static final int OUTER_ITEM_COUNT = 10;

        private boolean mReverseInner;

        class ViewHolder extends RecyclerView.ViewHolder {
            private final RecyclerView mRecyclerView;
            private final InnerAdapter mAdapter;
            ViewHolder(RecyclerView itemView) {
                super(itemView);
                mRecyclerView = itemView;
                mAdapter = new InnerAdapter();
                mRecyclerView.setAdapter(mAdapter);
            }
        }

        ArrayList<Parcelable> mSavedStates = new ArrayList<>();
        RecyclerView.RecycledViewPool mSharedPool = new RecyclerView.RecycledViewPool();

        OuterNotifyAdapter() {
            this(false);
        }

        OuterNotifyAdapter(boolean reverseInner) {
            mReverseInner = reverseInner;
            for (int i = 0; i <= OUTER_ITEM_COUNT; i++) {
                mSavedStates.add(null);
            }
        }

        @Override
        public @NonNull ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            mRecyclerView.registerTimePassingMs(5);
            RecyclerView rv = new RecyclerView(parent.getContext());
            rv.setLayoutManager(new LinearLayoutManager(parent.getContext(),
                    LinearLayoutManager.HORIZONTAL, mReverseInner));
            rv.setRecycledViewPool(mSharedPool);
            rv.setLayoutParams(new RecyclerView.LayoutParams(200, 100));
            return new ViewHolder(rv);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            mRecyclerView.registerTimePassingMs(5);
            // if we had actual data to put into our adapter, this is where we'd do it...

            // ... then notify the adapter that it has new content:
            holder.mAdapter.notifyDataSetChanged();

            Parcelable savedState = mSavedStates.get(position);
            if (savedState != null) {
                holder.mRecyclerView.getLayoutManager().onRestoreInstanceState(savedState);
                mSavedStates.set(position, null);
            }
        }

        @Override
        public void onViewRecycled(@NonNull ViewHolder holder) {
            if (holder.getAbsoluteAdapterPosition() >= 0) {
                mSavedStates.set(holder.getAbsoluteAdapterPosition(),
                        holder.mRecyclerView.getLayoutManager().onSaveInstanceState());
            }
        }

        @Override
        public int getItemCount() {
            return OUTER_ITEM_COUNT;
        }
    }

    @Test
    public void nestedPrefetchDiscardStaleChildren() {
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        assertEquals(2, llm.getInitialPrefetchItemCount());

        mRecyclerView.setLayoutManager(llm);
        OuterNotifyAdapter outerAdapter = new OuterNotifyAdapter();
        mRecyclerView.setAdapter(outerAdapter);

        // zero cache, so item we prefetch can't already be ready
        mRecyclerView.setItemViewCacheSize(0);

        // layout 3 items, then resize to 2...
        layout(200, 300);
        layout(200, 200);

        // so 1 item is evicted into the RecycledViewPool (bypassing cache)
        assertEquals(1, mRecycler.mRecyclerPool.getRecycledViewCount(0));
        assertEquals(0, mRecycler.mCachedViews.size());

        // This is a simple imitation of other behavior (namely, varied types in the outer adapter)
        // that results in the same initial state to test: items in the pool with attached children
        for (RecyclerView.ViewHolder holder : mRecycler.mRecyclerPool.mScrap.get(0).mScrapHeap) {
            // verify that children are attached and valid, since the RVs haven't been rebound
            assertNotNull(holder.mNestedRecyclerView);
            assertFalse(holder.mNestedRecyclerView.get().mDataSetHasChangedAfterLayout);
            validateRvChildrenValid(holder.mNestedRecyclerView.get(), 2);
        }

        // prefetch the outer item bind, but without enough time to do any inner binds
        final long deadlineNs = mRecyclerView.getNanoTime() + TimeUnit.MILLISECONDS.toNanos(9);
        mRecyclerView.mPrefetchRegistry.setPrefetchVector(0, 1);
        mRecyclerView.mGapWorker.prefetch(deadlineNs);

        // 2 is prefetched without children
        CacheUtils.verifyCacheContainsPrefetchedPositions(mRecyclerView, 2);
        RecyclerView.ViewHolder holder = CacheUtils.peekAtCachedViewForPosition(mRecyclerView, 2);
        assertNotNull(holder);
        assertNotNull(holder.mNestedRecyclerView);
        assertEquals(0, holder.mNestedRecyclerView.get().mChildHelper.getUnfilteredChildCount());
        assertEquals(0, holder.mNestedRecyclerView.get().mRecycler.mCachedViews.size());

        // but if we give it more time to bind items, it'll now acquire its inner items
        mRecyclerView.mGapWorker.prefetch(RecyclerView.FOREVER_NS);
        CacheUtils.verifyCacheContainsPrefetchedPositions(holder.mNestedRecyclerView.get(), 0, 1);
    }


    @Test
    public void nestedPrefetchDiscardStalePrefetch() {
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        OuterNotifyAdapter outerAdapter = new OuterNotifyAdapter();
        mRecyclerView.setAdapter(outerAdapter);

        // zero cache, so item we prefetch can't already be ready
        mRecyclerView.setItemViewCacheSize(0);

        // layout as 2x2, starting on row index 2, with empty cache
        layout(200, 200);
        mRecyclerView.scrollBy(0, 200);

        // no views cached, or previously used (so we can trust number in mItemsBound)
        mRecycler.mRecyclerPool.clear();
        assertEquals(0, mRecycler.mRecyclerPool.getRecycledViewCount(0));
        assertEquals(0, mRecycler.mCachedViews.size());

        // prefetch the outer item and its inner children
        mRecyclerView.mPrefetchRegistry.setPrefetchVector(0, 1);
        mRecyclerView.mGapWorker.prefetch(RecyclerView.FOREVER_NS);

        // 4 is prefetched with 2 inner children, first two binds
        CacheUtils.verifyCacheContainsPrefetchedPositions(mRecyclerView, 4);
        RecyclerView.ViewHolder holder = CacheUtils.peekAtCachedViewForPosition(mRecyclerView, 4);
        assertNotNull(holder);
        assertNotNull(holder.mNestedRecyclerView);
        RecyclerView innerRecyclerView = holder.mNestedRecyclerView.get();
        assertEquals(0, innerRecyclerView.mChildHelper.getUnfilteredChildCount());
        assertEquals(2, innerRecyclerView.mRecycler.mCachedViews.size());
        assertEquals(2, ((InnerAdapter) innerRecyclerView.getAdapter()).mItemsBound);

        // notify data set changed, so any previously prefetched items invalid, and re-prefetch
        innerRecyclerView.getAdapter().notifyDataSetChanged();
        mRecyclerView.mPrefetchRegistry.setPrefetchVector(0, 1);
        mRecyclerView.mGapWorker.prefetch(RecyclerView.FOREVER_NS);

        // 4 is prefetched again...
        CacheUtils.verifyCacheContainsPrefetchedPositions(mRecyclerView, 4);

        // reusing the same instance with 2 inner children...
        assertSame(holder, CacheUtils.peekAtCachedViewForPosition(mRecyclerView, 4));
        assertSame(innerRecyclerView, holder.mNestedRecyclerView.get());
        assertEquals(0, innerRecyclerView.mChildHelper.getUnfilteredChildCount());
        assertEquals(2, innerRecyclerView.mRecycler.mCachedViews.size());

        // ... but there should be two new binds
        assertEquals(4, ((InnerAdapter) innerRecyclerView.getAdapter()).mItemsBound);
    }

    @Test
    public void setRecycledViewPool_followedByTwoSetAdapters_clearsRecycledViewPool() {
        RecyclerView.ViewHolder viewHolder = new RecyclerView.ViewHolder(new View(getContext())) {};
        viewHolder.mItemViewType = 123;
        RecyclerView.Adapter<?> adapter = mock(RecyclerView.Adapter.class);
        RecyclerView.RecycledViewPool recycledViewPool = new RecyclerView.RecycledViewPool();
        recycledViewPool.putRecycledView(viewHolder);

        mRecyclerView.setRecycledViewPool(recycledViewPool);
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.setAdapter(adapter);

        assertThat(recycledViewPool.getRecycledViewCount(123), is(equalTo(0)));
    }

    @Test
    public void setRecycledViewPool_followedByTwoSwapAdapters_doesntClearRecycledViewPool() {
        RecyclerView.ViewHolder viewHolder = new RecyclerView.ViewHolder(new View(getContext())) {};
        viewHolder.mItemViewType = 123;
        RecyclerView.Adapter<?> adapter = mock(RecyclerView.Adapter.class);
        RecyclerView.RecycledViewPool recycledViewPool = new RecyclerView.RecycledViewPool();
        recycledViewPool.putRecycledView(viewHolder);

        mRecyclerView.setRecycledViewPool(recycledViewPool);
        mRecyclerView.swapAdapter(adapter, false);
        mRecyclerView.swapAdapter(adapter, false);

        assertThat(recycledViewPool.getRecycledViewCount(123), is(equalTo(1)));
    }
}
