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

package android.arch.paging;

import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import java.util.concurrent.Executor;

class TiledPagedList<T> extends PagedList<T>
        implements PagedStorage.Callback {

    private final TiledDataSource<T> mDataSource;

    @SuppressWarnings("unchecked")
    private final PagedStorage<Integer, T> mKeyedStorage = (PagedStorage<Integer, T>) mStorage;

    private final PageResult.Receiver<Integer, T> mReceiver =
            new PageResult.Receiver<Integer, T>() {
        @AnyThread
        @Override
        public void postOnPageResult(@NonNull final PageResult<Integer, T> pageResult) {
            // NOTE: if we're already on main thread, this can delay page receive by a frame
            mMainThreadExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    onPageResult(pageResult);
                }
            });
        }

        // Creation thread for initial synchronous load, otherwise main thread
        // Safe to access main thread only state - no other thread has reference during construction
        @AnyThread
        @Override
        public void onPageResult(@NonNull PageResult<Integer, T> pageResult) {
            if (pageResult.page == null) {
                detach();
                return;
            }

            if (isDetached()) {
                // No op, have detached
                return;
            }

            if (mStorage.getPageCount() == 0) {
                mKeyedStorage.init(
                        pageResult.leadingNulls, pageResult.page, pageResult.trailingNulls,
                        pageResult.positionOffset, TiledPagedList.this);
            } else {
                mKeyedStorage.insertPage(pageResult.leadingNulls, pageResult.page,
                        TiledPagedList.this);
            }

            if (mBoundaryCallback != null) {
                boolean deferEmpty = mStorage.size() == 0;
                boolean deferBegin = !deferEmpty && pageResult.leadingNulls == 0;
                boolean deferEnd = !deferEmpty && pageResult.trailingNulls == 0;
                deferBoundaryCallbacks(deferEmpty, deferBegin, deferEnd);
            }
        }
    };

    @WorkerThread
    TiledPagedList(@NonNull TiledDataSource<T> dataSource,
            @NonNull Executor mainThreadExecutor,
            @NonNull Executor backgroundThreadExecutor,
            @Nullable BoundaryCallback<T> boundaryCallback,
            @NonNull Config config,
            int position) {
        super(new PagedStorage<Integer, T>(), mainThreadExecutor, backgroundThreadExecutor,
                boundaryCallback, config);
        mDataSource = dataSource;

        final int pageSize = mConfig.pageSize;

        final int itemCount = mDataSource.countItems();

        final int firstLoadSize = Math.min(itemCount,
                (Math.max(mConfig.initialLoadSizeHint / pageSize, 2)) * pageSize);
        final int firstLoadPosition = computeFirstLoadPosition(
                position, firstLoadSize, pageSize, itemCount);

        mDataSource.loadRangeInitial(firstLoadPosition, firstLoadSize, pageSize,
                itemCount, mReceiver);
    }

    static int computeFirstLoadPosition(int position, int firstLoadSize, int pageSize, int size) {
        int idealStart = position - firstLoadSize / 2;

        int roundedPageStart = Math.round(idealStart / pageSize) * pageSize;

        // minimum start position is 0
        roundedPageStart = Math.max(0, roundedPageStart);

        // maximum start pos is that which will encompass end of list
        int maximumLoadPage = ((size - firstLoadSize + pageSize - 1) / pageSize) * pageSize;
        roundedPageStart = Math.min(maximumLoadPage, roundedPageStart);

        return roundedPageStart;
    }

    @Override
    boolean isContiguous() {
        return false;
    }

    @Nullable
    @Override
    public Object getLastKey() {
        return mLastLoad;
    }

    @Override
    protected void dispatchUpdatesSinceSnapshot(@NonNull PagedList<T> pagedListSnapshot,
            @NonNull Callback callback) {
        //noinspection UnnecessaryLocalVariable
        final PagedStorage<?, T> snapshot = pagedListSnapshot.mStorage;

        // loop through each page and signal the callback for any pages that are present now,
        // but not in the snapshot.
        final int pageSize = mConfig.pageSize;
        final int leadingNullPages = mStorage.getLeadingNullCount() / pageSize;
        final int pageCount = mStorage.getPageCount();
        for (int i = 0; i < pageCount; i++) {
            int pageIndex = i + leadingNullPages;
            int updatedPages = 0;
            // count number of consecutive pages that were added since the snapshot...
            while (updatedPages < mStorage.getPageCount()
                    && mStorage.hasPage(pageSize, pageIndex + updatedPages)
                    && !snapshot.hasPage(pageSize, pageIndex + updatedPages)) {
                updatedPages++;
            }
            // and signal them all at once to the callback
            if (updatedPages > 0) {
                callback.onChanged(pageIndex * pageSize, pageSize * updatedPages);
                i += updatedPages - 1;
            }
        }
    }

    @Override
    protected void loadAroundInternal(int index) {
        mStorage.allocatePlaceholders(index, mConfig.prefetchDistance, mConfig.pageSize, this);
    }

    @Override
    public void onInitialized(int count) {
        notifyInserted(0, count);
    }

    @Override
    public void onPagePrepended(int leadingNulls, int changed, int added) {
        throw new IllegalStateException("Contiguous callback on TiledPagedList");
    }

    @Override
    public void onPageAppended(int endPosition, int changed, int added) {
        throw new IllegalStateException("Contiguous callback on TiledPagedList");
    }

    @Override
    public void onPagePlaceholderInserted(final int pageIndex) {
        // placeholder means initialize a load
        mBackgroundThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (isDetached()) {
                    return;
                }
                final int pageSize = mConfig.pageSize;
                mDataSource.loadRange(pageIndex * pageSize, pageSize, mReceiver);
            }
        });
    }

    @Override
    public void onPageInserted(int start, int count) {
        notifyChanged(start, count);
    }
}
