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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.view.ViewGroup;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

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
    };

    @Before
    public void setUp() throws Exception {
        mRecyclerView = new TimeMockingRecyclerView(getContext());
        mRecycler = mRecyclerView.mRecycler;
    }

    private Context getContext() {
        return InstrumentationRegistry.getContext();
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
            public void collectPrefetchPositions(int dx, int dy, RecyclerView.State state,
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

        mRecyclerView.measure(View.MeasureSpec.AT_MOST | 320, View.MeasureSpec.AT_MOST | 240);
        mRecyclerView.layout(0, 0, 320, 320);

        verify(mockAdapter, never()).onCreateViewHolder(any(ViewGroup.class), anyInt());
        verify(mockAdapter, never()).onBindViewHolder(
                any(RecyclerView.ViewHolder.class), anyInt(), any(List.class));
        assertTrue(mRecycler.mCachedViews.isEmpty());

        // Prefetch multiple times...
        for (int i = 0; i < 4; i++) {
            GapWorker.layoutPrefetch(RecyclerView.FOREVER_NS, mRecyclerView);

            // ...but should only see the same three items fetched/bound once each
            verify(mockAdapter, times(3)).onCreateViewHolder(any(ViewGroup.class), anyInt());
            verify(mockAdapter, times(3)).onBindViewHolder(
                    any(RecyclerView.ViewHolder.class), anyInt(), any(List.class));

            assertTrue(mRecycler.mCachedViews.size() == 3);
            verifyCacheContainsPositions(0, 1, 2);
        }
    }

    private void verifyCacheContainsPosition(int position) {
        for (int i = 0; i < mRecycler.mCachedViews.size(); i++) {
            if (mRecycler.mCachedViews.get(i).mPosition == position) return;
        }
        fail("Cache does not contain position " + position);
    }

    private void verifyCacheContainsPositions(Integer... positions) {
        for (int i = 0; i < positions.length; i++) {
            verifyCacheContainsPosition(positions[i]);
        }
    }

    @Test
    public void prefetchItemsNotEvictedWithInserts() {
        mRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3) {
            @Override
            public void collectPrefetchPositions(int dx, int dy, RecyclerView.State state,
                    RecyclerView.PrefetchRegistry prefetchRegistry) {
                prefetchRegistry.addPosition(0, 0);
                prefetchRegistry.addPosition(1, 0);
                prefetchRegistry.addPosition(2, 0);
            }
        });

        RecyclerView.Adapter mockAdapter = mock(RecyclerView.Adapter.class);
        when(mockAdapter.onCreateViewHolder(any(ViewGroup.class), anyInt()))
                .thenAnswer(new Answer<RecyclerView.ViewHolder>() {
                    @Override
                    public RecyclerView.ViewHolder answer(InvocationOnMock invocation)
                            throws Throwable {
                        return new RecyclerView.ViewHolder(new View(getContext())) {};
                    }
                });
        when(mockAdapter.getItemCount()).thenReturn(100);
        mRecyclerView.setAdapter(mockAdapter);


        mRecyclerView.measure(View.MeasureSpec.AT_MOST | 320, View.MeasureSpec.AT_MOST | 320);
        mRecyclerView.layout(0, 0, 320, 320);

        assertEquals(2, mRecyclerView.mRecycler.mViewCacheMax);
        GapWorker.layoutPrefetch(RecyclerView.FOREVER_NS, mRecyclerView);
        assertEquals(5, mRecyclerView.mRecycler.mViewCacheMax);

        verifyCacheContainsPositions(0, 1, 2);

        // further views recycled, as though from scrolling, shouldn't evict prefetched views:
        mRecycler.recycleView(mRecycler.getViewForPosition(10));
        verifyCacheContainsPositions(0, 1, 2, 10);

        mRecycler.recycleView(mRecycler.getViewForPosition(20));
        verifyCacheContainsPositions(0, 1, 2, 10, 20);

        mRecycler.recycleView(mRecycler.getViewForPosition(30));
        verifyCacheContainsPositions(0, 1, 2, 20, 30);

        mRecycler.recycleView(mRecycler.getViewForPosition(40));
        verifyCacheContainsPositions(0, 1, 2, 30, 40);


        // After clearing the cache, the prefetch priorities should be cleared as well:
        mRecyclerView.mRecycler.recycleAndClearCachedViews();
        for (int i : new int[] {0, 1, 2, 50, 60, 70, 80, 90}) {
            mRecycler.recycleView(mRecycler.getViewForPosition(i));
        }

        // cache only contains most recent positions, no priority for previous prefetches:
        verifyCacheContainsPositions(50, 60, 70, 80, 90);

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

        mRecyclerView.measure(View.MeasureSpec.AT_MOST | 300, View.MeasureSpec.AT_MOST | 200);
        mRecyclerView.layout(0, 0, 300, 150);
        mRecyclerView.scrollBy(0, 75);
        assertTrue(mRecycler.mCachedViews.isEmpty());

        // rows 0, 1, and 2 are all attached and visible. Prefetch row 3:
        mRecyclerView.mPrefetchRegistry.setPrefetchVector(0, 1);
        GapWorker.layoutPrefetch(RecyclerView.FOREVER_NS, mRecyclerView);

        // row 3 is cached:
        verifyCacheContainsPositions(9, 10, 11);
        assertTrue(mRecycler.mCachedViews.size() == 3);

        // Scroll so 1 falls off (though 3 is still not on screen)
        mRecyclerView.scrollBy(0, 50);

        // row 3 is still cached, with a couple other recycled views:
        verifyCacheContainsPositions(9, 10, 11);
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

        mRecyclerView.measure(View.MeasureSpec.AT_MOST | 300, View.MeasureSpec.AT_MOST | 300);
        mRecyclerView.layout(0, 0, 300, 300);

        assertTrue(mRecycler.mCachedViews.size() == 0);
        assertTrue(mRecyclerView.getRecycledViewPool().getRecycledViewCount(0) == 0);

        // Should take 15 ms to inflate, bind, inflate, so give 19 to be safe
        final long deadlineNs = mRecyclerView.getNanoTime() + TimeUnit.MILLISECONDS.toNanos(19);

        // Timed prefetch
        mRecyclerView.mPrefetchRegistry.setPrefetchVector(0, 1);
        GapWorker.layoutPrefetch(deadlineNs, mRecyclerView);

        // will have enough time to inflate/bind one view, and inflate another
        assertTrue(mRecycler.mCachedViews.size() == 1);
        assertTrue(mRecyclerView.getRecycledViewPool().getRecycledViewCount(0) == 1);
        verifyCacheContainsPositions(9); // Note: order/view here is an implementation detail


        // Unbounded prefetch this time
        GapWorker.layoutPrefetch(RecyclerView.FOREVER_NS, mRecyclerView);

        // Should finish all work
        assertTrue(mRecycler.mCachedViews.size() == 3);
        assertTrue(mRecyclerView.getRecycledViewPool().getRecycledViewCount(0) == 0);
        verifyCacheContainsPositions(9, 10, 11);
    }

    private void verifyPositionsPrefetched(RecyclerView.LayoutManager lm, int dx, int dy,
            Integer[] ... positionData) {
        RecyclerView.PrefetchRegistry prefetchRegistry = mock(RecyclerView.PrefetchRegistry.class);
        lm.collectPrefetchPositions(dx, dy, mRecyclerView.mState, prefetchRegistry);

        verify(prefetchRegistry, times(positionData.length)).addPosition(anyInt(), anyInt());
        for (Integer[] aPositionData : positionData) {
            verify(prefetchRegistry).addPosition(aPositionData[0], aPositionData[1]);
        }
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

        mRecyclerView.measure(View.MeasureSpec.AT_MOST | 200, View.MeasureSpec.AT_MOST | 200);
        mRecyclerView.layout(0, 0, 200, 200);

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
        verifyPositionsPrefetched(sglm, 0, 10,
                new Integer[] {5, 0}, new Integer[] {6, 50});

        // Prefetch upward shows nothing
        verifyPositionsPrefetched(sglm, 0, -10);

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
        verifyPositionsPrefetched(sglm, 0, 10,
                new Integer[] {7, 0}, new Integer[] {8, 50});

        // prefetching up shows 1 is 0 pixels away, 0 at 50 pixels away
        verifyPositionsPrefetched(sglm, 0, -10,
                new Integer[] {1, 0}, new Integer[] {0, 50});
    }
}
