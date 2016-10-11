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
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;
import android.view.View;
import android.view.ViewGroup;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.List;

@SmallTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
@RunWith(AndroidJUnit4.class)
public class RecyclerViewCacheTest {
    RecyclerView mRecyclerView;
    RecyclerView.Recycler mRecycler;
    RecyclerView.ViewPrefetcher mViewPrefetcher;

    @Before
    public void setUp() throws Exception {
        mRecyclerView = new RecyclerView(getContext());
        mRecycler = mRecyclerView.mRecycler;
        mViewPrefetcher = mRecyclerView.mViewPrefetcher;
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
            int getItemPrefetchCount() {
                return 3;
            }

            @Override
            int gatherPrefetchIndices(int dx, int dy, RecyclerView.State state, int[] outIndices) {
                outIndices[0] = 0;
                outIndices[1] = 1;
                outIndices[2] = 2;
                return 3;
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
            int[] itemPrefetchArray = new int[] {-1, -1, -1};
            int viewCount = prefetchingLayoutManager.gatherPrefetchIndices(1, 1,
                    mRecyclerView.mState, itemPrefetchArray);
            mRecycler.prefetch(itemPrefetchArray, viewCount);

            // ...but should only see the same three items fetched/bound once each
            verify(mockAdapter, times(3)).onCreateViewHolder(any(ViewGroup.class), anyInt());
            verify(mockAdapter, times(3)).onBindViewHolder(
                    any(RecyclerView.ViewHolder.class), anyInt(), any(List.class));

            assertTrue(mRecycler.mCachedViews.size() == 3);
            verifyCacheContainsPositions(0, 1, 2);
        }
    }

    private void verifyCacheDoesNotContainPosition(int position) {
        for (int i = 0; i < mRecycler.mCachedViews.size(); i++) {
            assertNotEquals("Cache must not contain position " + position,
                    position, mRecycler.mCachedViews.get(i).mPosition);
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

        mViewPrefetcher.mItemPrefetchArray = new int[] { 3, 4, 5 };
        mRecycler.prefetch(mViewPrefetcher.mItemPrefetchArray, 3);
        verifyCacheContainsPositions(3, 4, 5);

        // further views recycled, as though from scrolling, shouldn't evict prefetched views:
        mRecycler.recycleView(mRecycler.getViewForPosition(10));
        verifyCacheContainsPositions(3, 4, 5, 10);

        mRecycler.recycleView(mRecycler.getViewForPosition(20));
        verifyCacheContainsPositions(3, 4, 5, 10, 20);

        mRecycler.recycleView(mRecycler.getViewForPosition(30));
        verifyCacheContainsPositions(3, 4, 5, 20, 30);

        mRecycler.recycleView(mRecycler.getViewForPosition(40));
        verifyCacheContainsPositions(3, 4, 5, 30, 40);


        // After clearing the cache, the prefetch priorities should be cleared as well:
        mRecyclerView.mRecycler.recycleAndClearCachedViews();
        for (int i : new int[] {3, 4, 5, 50, 60, 70, 80, 90}) {
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

        layout(300, 150);
        mRecyclerView.scrollBy(0, 75);
        assertTrue(mRecycler.mCachedViews.isEmpty());

        // rows 0, 1, and 2 are all attached and visible. Prefetch row 3:
        mViewPrefetcher.mItemPrefetchArray = new int[] {-1, -1, -1};
        int viewCount = mRecyclerView.getLayoutManager().gatherPrefetchIndices(0, 1,
                mRecyclerView.mState, mViewPrefetcher.mItemPrefetchArray);
        mRecycler.prefetch(mViewPrefetcher.mItemPrefetchArray, viewCount);

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
            public int getItemCount() { return 10; }
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
        verifyCacheDoesNotContainPosition(2);
        assertNotNull("Animating view should be found, hidden",
                mRecyclerView.mChildHelper.findHiddenNonRemovedView(2, RecyclerView.INVALID_TYPE));

        // ...but must not be removed for prefetch
        mViewPrefetcher.mItemPrefetchArray = new int[] {-1};
        int viewCount = mRecyclerView.getLayoutManager().gatherPrefetchIndices(0, 1,
                mRecyclerView.mState, mViewPrefetcher.mItemPrefetchArray);
        mRecycler.prefetch(mViewPrefetcher.mItemPrefetchArray, viewCount);
        int prefetchTarget = mViewPrefetcher.mItemPrefetchArray[0];
        assertEquals("Prefetch must target view 2", 2, prefetchTarget);

        // animating view still observable as hidden, uncached
        verifyCacheDoesNotContainPosition(2);
        assertNotNull("Animating view should be found, hidden",
                mRecyclerView.mChildHelper.findHiddenNonRemovedView(2, RecyclerView.INVALID_TYPE));

        assertTrue(itemAnimator.isRunning());
        assertEquals(2, llm.getChildCount());
        assertEquals(4, mRecyclerView.getChildCount());
    }
}