/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.arch.util.paging;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.WorkerThread;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/** @hide */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class TiledPagedList<T> extends PageArrayList<T> {

    private final TiledDataSource<T> mDataSource;
    private final Executor mMainThreadExecutor;
    private final Executor mBackgroundThreadExecutor;
    private final Config mConfig;

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final List<T> mLoadingPlaceholder = new AbstractList<T>() {
        @Override
        public T get(int i) {
            return null;
        }

        @Override
        public int size() {
            return 0;
        }
    };

    private int mLastLoad = -1;

    private AtomicBoolean mDetached = new AtomicBoolean(false);

    private ArrayList<Callback> mCallbacks = new ArrayList<>();

    @WorkerThread
    TiledPagedList(@NonNull TiledDataSource<T> dataSource,
            @NonNull Executor mainThreadExecutor,
            @NonNull Executor backgroundThreadExecutor,
            Config config,
            int position) {
        super(config.mPageSize, dataSource.loadCount());

        mDataSource = dataSource;
        mMainThreadExecutor = mainThreadExecutor;
        mBackgroundThreadExecutor = backgroundThreadExecutor;
        mConfig = config;

        position = Math.min(Math.max(0, position), mCount);

        int firstPage = position / mPageSize;
        List<T> firstPageData = dataSource.loadRange(firstPage * mPageSize, mPageSize);
        if (firstPageData != null) {
            mListOffset = firstPage;
            mList.add(firstPageData);
        } else {
            detach();
            return;
        }

        int secondPage = (position % mPageSize < mPageSize / 2) ? firstPage - 1 : firstPage + 1;
        if (secondPage < 0 || secondPage > (mCount + mPageSize - 1) / mPageSize) {
            // no second page to load
            return;
        }
        List<T> secondPageData = dataSource.loadRange(secondPage * mPageSize, mPageSize);
        if (secondPageData != null) {
            boolean before = secondPage < firstPage;
            mList.add(before ? 0 : 1, secondPageData);
            if (before) {
                mListOffset--;
            }
            return;
        }
        detach();
    }

    @Override
    public void loadAround(int index) {
        mLastLoad = index;

        int minimumPage = (index - mConfig.mPrefetchDistance) / mPageSize;
        int maximumPage = (index + mConfig.mPrefetchDistance) / mPageSize;

        if (minimumPage < mListOffset) {
            mListOffset = minimumPage;
            for (int i = 0; i < mListOffset - minimumPage; i++) {
                mList.add(0, null);
            }
        }
        if (maximumPage > mListOffset + mList.size()) {
            mList.add(mList.size() - 1, null);
        }
        for (int i = minimumPage; i <= maximumPage; i++) {
            scheduleLoadPage(i);
        }
    }

    private void scheduleLoadPage(final int pageIndex) {
        int localPageIndex = pageIndex - mListOffset;
        if (mList.get(localPageIndex) != null) {
            return;
        }
        mList.set(localPageIndex, mLoadingPlaceholder);

        mBackgroundThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (mDetached.get()) {
                    return;
                }
                final List<T> data = mDataSource.loadRange(pageIndex * mPageSize, mPageSize);
                if (data != null) {
                    mMainThreadExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            if (mDetached.get()) {
                                return;
                            }
                            loadPageImpl(pageIndex, data);
                        }
                    });
                } else {
                    detach();
                }
            }
        });

    }

    private void loadPageImpl(int pageIndex, List<T> data) {
        int localPageIndex = pageIndex - mListOffset;

        if (mList.get(localPageIndex) != mLoadingPlaceholder) {
            throw new IllegalStateException("Data inserted before requested.");
        }
        mList.set(pageIndex, data);
        for (Callback callback : mCallbacks) {
            callback.onChanged(pageIndex * mPageSize, data.size());
        }
    }

    @Override
    public boolean isImmutable() {
        // TODO: consider counting loaded pages, return true if mLoadedPages = mMaxPageCount()
        // Could at some point want to support growing past max count
        return isDetached();
    }

    @Override
    public void addCallback(@Nullable PagedList<T> previousSnapshot, @NonNull Callback callback) {
        PageArrayList<T> snapshot = (PageArrayList<T>) previousSnapshot;
        if (snapshot != this && snapshot != null) {
            // TODO: trigger onChanged based on newly loaded pages
        }
        mCallbacks.add(callback);
    }

    @Override
    public void removeCallback(@NonNull Callback callback) {
        mCallbacks.remove(callback);
    }

    /**
     * Returns the last position accessed by the PagedList. Can be used to initialize loads in
     * subsequent PagedList versions.
     *
     * @return Last position accessed by the PagedList.
     */
    @SuppressWarnings("WeakerAccess")
    public int getLastLoad() {
        return mLastLoad;
    }

    /**
     * True if the PagedList has detached the DataSource it was loading from, and will no longer
     * load new data.
     *
     * @return True if the data source is detached.
     */
    @SuppressWarnings("WeakerAccess")
    public boolean isDetached() {
        return mDetached.get();
    }

    /**
     * Detach the PagedList from its DataSource, and attempt to load no more data.
     * <p>
     * This is called automatically when a DataSource load returns <code>null</code>, which is a
     * signal to stop loading. The PagedList will continue to present existing data, but will not
     * load new items.
     */
    @SuppressWarnings("WeakerAccess")
    public void detach() {
        mDetached.set(true);
    }
}
