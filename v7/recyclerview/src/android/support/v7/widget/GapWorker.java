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

import android.support.v4.os.TraceCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

class GapWorker implements Runnable {
    static final ThreadLocal<GapWorker> sGapWorker = new ThreadLocal<>();

    private static final long MIN_PREFETCH_TIME_NANOS = TimeUnit.MILLISECONDS.toNanos(4);

    ArrayList<RecyclerView> mRecyclerViews = new ArrayList<>();
    long mPostTimeNs;
    long mFrameIntervalNs;

    public void add(RecyclerView recyclerView) {
        if (RecyclerView.DEBUG && mRecyclerViews.contains(recyclerView)) {
            throw new IllegalStateException("RecyclerView already present in worker list!");
        }
        mRecyclerViews.add(recyclerView);
    }

    public void remove(RecyclerView recyclerView) {
        boolean removeSuccess = mRecyclerViews.remove(recyclerView);
        if (RecyclerView.DEBUG && !removeSuccess) {
            throw new IllegalStateException("RecyclerView removal failed!");
        }
    }

    /**
     * Schedule a prefetch immediately after the current traversal.
     */
    void postFromTraversal(RecyclerView recyclerView, int prefetchDx, int prefetchDy) {
        if (recyclerView.isAttachedToWindow()) {
            if (RecyclerView.DEBUG && !mRecyclerViews.contains(recyclerView)) {
                throw new IllegalStateException("attempting to post unregistered view!");
            }
            if (mPostTimeNs == 0) {
                mPostTimeNs = System.nanoTime();
                recyclerView.post(this);
            }
        }

        recyclerView.mPrefetchDx = prefetchDx;
        recyclerView.mPrefetchDy = prefetchDy;

    }

    void layoutPrefetch(long deadlineNs, RecyclerView view) {
        final int prefetchCount = view.mLayout.getItemPrefetchCount();
        if (view.mAdapter == null
                || view.mLayout == null
                || !view.mLayout.isItemPrefetchEnabled()
                || prefetchCount < 1
                || view.hasPendingAdapterUpdates()) {
            // abort - no work
            return;
        }

        long nowNanos = System.nanoTime();
        if (nowNanos - mPostTimeNs > mFrameIntervalNs
                || deadlineNs - nowNanos < MIN_PREFETCH_TIME_NANOS) {
            // abort - Executing either too far after post,
            // or too near next scheduled vsync
            return;
        }

        if (view.mPrefetchArray == null
                || view.mPrefetchArray.length < prefetchCount) {
            view.mPrefetchArray = new int[prefetchCount];
        }
        Arrays.fill(view.mPrefetchArray, -1);
        int viewCount = view.mLayout.gatherPrefetchIndices(
                view.mPrefetchDx, view.mPrefetchDy,
                view.mState, view.mPrefetchArray);
        view.mRecycler.prefetch(view.mPrefetchArray, viewCount);
    }

    @Override
    public void run() {
        try {
            TraceCompat.beginSection(RecyclerView.TRACE_PREFETCH_TAG);

            if (mRecyclerViews.isEmpty()) {
                // abort - no work to do
                return;
            }

            // Query last vsync so we can predict next one. Note that drawing time not yet
            // valid in animation/input callbacks, so query it here to be safe.
            long lastFrameVsyncNanos = TimeUnit.MILLISECONDS.toNanos(
                    mRecyclerViews.get(0).getDrawingTime());
            if (lastFrameVsyncNanos == 0) {
                // abort - couldn't get last vsync for estimating next
                return;
            }

            long nextFrameNanos = lastFrameVsyncNanos + mFrameIntervalNs;

            // NOTE: it's safe to iterate over mRecyclerViews since we know that (currently),
            // no attach or detach will occur as a result of creating/binding view holders.
            // If any prefetch work attached Views, we'd have to avoid fighting over the list.
            final int count = mRecyclerViews.size();
            for (int i = 0; i < count; i++) {
                layoutPrefetch(nextFrameNanos, mRecyclerViews.get(i));
            }

            // TODO: consider rescheduling self, if there's more work to do
        } finally {
            mPostTimeNs = 0;
            TraceCompat.endSection();
        }
    }
}
