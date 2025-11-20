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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class MultiRecyclerViewPrefetchTest {
    private RecyclerView.RecycledViewPool mRecycledViewPool;
    private ArrayList<RecyclerView> mViews = new ArrayList<>();

    private long mMockNanoTime = 0;

    @Before
    public void setup() throws Exception {
        GapWorker gapWorker = GapWorker.sGapWorker.get();
        if (gapWorker != null) {
            assertTrue(gapWorker.mRecyclerViews.isEmpty());
        }
        mMockNanoTime = 0;
        mRecycledViewPool = new RecyclerView.RecycledViewPool();
    }

    @After
    public void teardown() {
        for (RecyclerView rv : mViews) {
            if (rv.isAttachedToWindow()) {
                // ensure we detach views, so ThreadLocal GapWorker's list is cleared
                rv.onDetachedFromWindow();
            }
        }
        GapWorker gapWorker = GapWorker.sGapWorker.get();
        if (gapWorker != null) {
            assertTrue(gapWorker.mRecyclerViews.isEmpty());
        }
        mViews.clear();
    }

    private RecyclerView createRecyclerView() {
        RecyclerView rv = new RecyclerView(getContext()) {
            @Override
            long getNanoTime() {
                return mMockNanoTime;
            }

            @Override
            public int getWindowVisibility() {
                // Pretend to be visible to avoid being filtered out
                return View.VISIBLE;
            }
        };

        // shared stats + enable clearing of pool
        rv.setRecycledViewPool(mRecycledViewPool);

        // enable GapWorker
        rv.onAttachedToWindow();
        mViews.add(rv);

        return rv;
    }

    public void registerTimePassingMs(long ms) {
        mMockNanoTime += TimeUnit.MILLISECONDS.toNanos(ms);
    }

    private Context getContext() {
        return ApplicationProvider.getApplicationContext();
    }

    private void clearCachesAndPool() {
        for (RecyclerView rv : mViews) {
            rv.mRecycler.recycleAndClearCachedViews();
        }
        mRecycledViewPool.clear();
    }

    @Test
    public void prefetchOrdering() throws Throwable {
        for (int i = 0; i < 3; i++) {
            RecyclerView rv = createRecyclerView();

            // first view 50x100 pixels, rest are 100x100 so second column is offset
            rv.setAdapter(new RecyclerView.Adapter() {
                @Override
                public RecyclerView.@NonNull ViewHolder onCreateViewHolder(
                        @NonNull ViewGroup parent, int viewType) {
                    registerTimePassingMs(5);
                    return new RecyclerView.ViewHolder(new View(parent.getContext())) {};
                }

                @Override
                public void onBindViewHolder(
                        RecyclerView.@NonNull ViewHolder holder, int position) {
                    registerTimePassingMs(5);
                    holder.itemView.setMinimumWidth(100);
                    holder.itemView.setMinimumHeight(position == 0 ? 50 : 100);
                }

                @Override
                public int getItemCount() {
                    return 100;
                }
            });
            rv.setLayoutManager(
                    new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));

            // Attach, position 200x200 view at 100 scroll offset, with an empty cache.
            rv.measure(View.MeasureSpec.AT_MOST | 200, View.MeasureSpec.AT_MOST | 200);
            rv.layout(0, 0, 200, 200);
            rv.scrollBy(0, 100);

            rv.setTranslationX(100 * i);
        }

        GapWorker worker = GapWorker.sGapWorker.get();
        assertNotNull(worker);

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

        mViews.get(0).mPrefetchRegistry.setPrefetchVector(0, 10);
        mViews.get(1).mPrefetchRegistry.setPrefetchVector(0, -11);
        mViews.get(2).mPrefetchRegistry.setPrefetchVector(0, 60);

        // prefetch with deadline that has passed - only demand-loaded views
        clearCachesAndPool();
        worker.prefetch(0);
        CacheUtils.verifyCacheContainsPrefetchedPositions(mViews.get(0), 7);
        CacheUtils.verifyCacheContainsPrefetchedPositions(mViews.get(1), 1);
        CacheUtils.verifyCacheContainsPrefetchedPositions(mViews.get(2), 7, 8);


        // prefetch with 54ms - should load demand-loaded views (taking 40ms) + one more
        clearCachesAndPool();
        worker.prefetch(mMockNanoTime + TimeUnit.MILLISECONDS.toNanos(54));
        CacheUtils.verifyCacheContainsPrefetchedPositions(mViews.get(0), 7);
        CacheUtils.verifyCacheContainsPrefetchedPositions(mViews.get(1), 0, 1);
        CacheUtils.verifyCacheContainsPrefetchedPositions(mViews.get(2), 7, 8);
    }
}
