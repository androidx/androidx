/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.paging;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.paging.futures.FutureCallback;
import androidx.paging.futures.Futures;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

class Pager<K, V> {
    @SuppressWarnings("unchecked")
    Pager(@NonNull PagedList.Config config,
            @NonNull DataSource<K, V> source,
            @NonNull Executor notifyExecutor,
            @NonNull Executor fetchExecutor,
            @NonNull PageConsumer<V> pageConsumer,
            @Nullable AdjacentProvider<V> adjacentProvider,
            @NonNull DataSource.BaseResult<V> result) {
        mConfig = config;
        mSource = source;
        mNotifyExecutor = notifyExecutor;
        mFetchExecutor = fetchExecutor;
        mPageConsumer = pageConsumer;
        if (adjacentProvider == null) {
            adjacentProvider = new SimpleAdjacentProvider<>();
        }
        mAdjacentProvider = adjacentProvider;
        mPrevKey = (K) result.prevKey;
        mNextKey = (K) result.nextKey;
        adjacentProvider.onPageResultResolution(PagedList.LoadType.REFRESH, result);
        mTotalCount = result.totalCount();

        // TODO: move this validation to tiled paging impl, once that's added back
        if (mSource.mType == DataSource.KeyType.POSITIONAL && mConfig.enablePlaceholders) {
            result.validateForInitialTiling(mConfig.pageSize);
        }
    }

    private final int mTotalCount;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @NonNull
    final PagedList.Config mConfig;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @NonNull
    final DataSource<K, V> mSource;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @NonNull
    final Executor mNotifyExecutor;

    @NonNull
    private final Executor mFetchExecutor;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @NonNull
    final PageConsumer<V> mPageConsumer;

    @NonNull
    private final AdjacentProvider<V> mAdjacentProvider;

    @Nullable
    private K mPrevKey;

    @Nullable
    private K mNextKey;

    private final AtomicBoolean mDetached = new AtomicBoolean(false);

    PagedList.LoadStateManager mLoadStateManager = new PagedList.LoadStateManager() {
        @Override
        protected void onStateChanged(@NonNull PagedList.LoadType type,
                @NonNull PagedList.LoadState state, @Nullable Throwable error) {
            mPageConsumer.onStateChanged(type, state, error);
        }
    };

    private void listenTo(@NonNull final PagedList.LoadType type,
            @NonNull final ListenableFuture<? extends DataSource.BaseResult<V>> future) {
        // First listen on the BG thread if the DataSource is invalid, since it can be expensive
        future.addListener(new Runnable() {
            @Override
            public void run() {
                // if invalid, drop result on the floor
                if (mSource.isInvalid()) {
                    detach();
                    return;
                }

                // Source has been verified to be valid after producing data, so sent data to UI
                Futures.addCallback(future, new FutureCallback<DataSource.BaseResult<V>>() {
                    @Override
                    public void onSuccess(DataSource.BaseResult<V> value) {
                        onLoadSuccess(type, value);
                    }

                    @Override
                    public void onError(@NonNull Throwable throwable) {
                        onLoadError(type, throwable);
                    }
                }, mNotifyExecutor);
            }
        }, mFetchExecutor);
    }

    interface PageConsumer<V> {
        // return true if we need to fetch more
        boolean onPageResult(
                @NonNull PagedList.LoadType type,
                @NonNull DataSource.BaseResult<V> pageResult);

        void onStateChanged(@NonNull PagedList.LoadType type,
                @NonNull PagedList.LoadState state, @Nullable Throwable error);
    }

    interface AdjacentProvider<V> {
        V getFirstLoadedItem();

        V getLastLoadedItem();

        int getFirstLoadedItemIndex();

        int getLastLoadedItemIndex();

        /**
         * Notify the AdjacentProvider of new loaded data, to update first/last item/index.
         *
         * NOTE: this data may not be committed (e.g. it may be dropped due to max size). Up to the
         * implementation of the AdjacentProvider to handle this (generally by ignoring this
         * call if dropping is supported).
         */
        void onPageResultResolution(
                @NonNull PagedList.LoadType type,
                @NonNull DataSource.BaseResult<V> result);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    void onLoadSuccess(PagedList.LoadType type, DataSource.BaseResult<V> value) {
        if (isDetached()) {
            // abort!
            return;
        }

        mAdjacentProvider.onPageResultResolution(type, value);

        if (mPageConsumer.onPageResult(type, value)) {
            if (type.equals(PagedList.LoadType.START)) {
                //noinspection unchecked
                mPrevKey = (K) value.prevKey;
                schedulePrepend();
            } else if (type.equals(PagedList.LoadType.END)) {
                //noinspection unchecked
                mNextKey = (K) value.nextKey;
                scheduleAppend();
            } else {
                throw new IllegalStateException("Can only fetch more during append/prepend");
            }
        } else {
            PagedList.LoadState state =
                    value.data.isEmpty() ? PagedList.LoadState.DONE : PagedList.LoadState.IDLE;
            mLoadStateManager.setState(type, state, null);
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    void onLoadError(PagedList.LoadType type, Throwable throwable) {
        if (isDetached()) {
            // abort!
            return;
        }
        // TODO: handle nesting
        PagedList.LoadState state = mSource.isRetryableError(throwable)
                ? PagedList.LoadState.RETRYABLE_ERROR : PagedList.LoadState.ERROR;
        mLoadStateManager.setState(type, state, throwable);
    }

    public void trySchedulePrepend() {
        if (mLoadStateManager.getStart().equals(PagedList.LoadState.IDLE)) {
            schedulePrepend();
        }
    }

    public void tryScheduleAppend() {
        if (mLoadStateManager.getEnd().equals(PagedList.LoadState.IDLE)) {
            scheduleAppend();
        }
    }

    private boolean canPrepend() {
        if (mTotalCount == DataSource.BaseResult.TOTAL_COUNT_UNKNOWN) {
            // don't know count / position from initial load, so be conservative, return true
            return true;
        }

        // position is known, do we have space left?
        return mAdjacentProvider.getFirstLoadedItemIndex() > 0;
    }

    private boolean canAppend() {
        if (mTotalCount == DataSource.BaseResult.TOTAL_COUNT_UNKNOWN) {
            // don't know count / position from initial load, so be conservative, return true
            return true;
        }

        // count is known, do we have space left?
        return mAdjacentProvider.getLastLoadedItemIndex() < mTotalCount - 1;
    }

    private void schedulePrepend() {
        if (!canPrepend()) {
            onLoadSuccess(PagedList.LoadType.START, DataSource.BaseResult.<V>empty());
            return;
        }
        K key;
        switch(mSource.mType) {
            case POSITIONAL:
                //noinspection unchecked
                key = (K) ((Integer) (mAdjacentProvider.getFirstLoadedItemIndex() - 1));
                break;
            case PAGE_KEYED:
                key = mPrevKey;
                break;
            case ITEM_KEYED:
                key = mSource.getKey(mAdjacentProvider.getFirstLoadedItem());
                break;
            default:
                throw new IllegalArgumentException("unknown source type");
        }
        mLoadStateManager.setState(
                PagedList.LoadType.START, PagedList.LoadState.LOADING, null);
        listenTo(PagedList.LoadType.START, mSource.load(new DataSource.Params<>(
                DataSource.LoadType.START,
                key,
                mConfig.initialLoadSizeHint,
                mConfig.enablePlaceholders,
                mConfig.pageSize)));
    }

    private void scheduleAppend() {
        if (!canAppend()) {
            onLoadSuccess(PagedList.LoadType.END, DataSource.BaseResult.<V>empty());
            return;
        }

        K key;
        switch(mSource.mType) {
            case POSITIONAL:
                //noinspection unchecked
                key = (K) ((Integer) (mAdjacentProvider.getLastLoadedItemIndex() + 1));
                break;
            case PAGE_KEYED:
                key = mNextKey;
                break;
            case ITEM_KEYED:
                key = mSource.getKey(mAdjacentProvider.getLastLoadedItem());
                break;
            default:
                throw new IllegalArgumentException("unknown source type");
        }

        mLoadStateManager.setState(PagedList.LoadType.END, PagedList.LoadState.LOADING, null);
        listenTo(PagedList.LoadType.END, mSource.load(new DataSource.Params<>(
                DataSource.LoadType.END,
                key,
                mConfig.initialLoadSizeHint,
                mConfig.enablePlaceholders,
                mConfig.pageSize)));
    }

    void retry() {
        if (mLoadStateManager.getStart().equals(PagedList.LoadState.RETRYABLE_ERROR)) {
            schedulePrepend();
        }
        if (mLoadStateManager.getEnd().equals(PagedList.LoadState.RETRYABLE_ERROR)) {
            scheduleAppend();
        }
    }

    boolean isDetached() {
        return mDetached.get();
    }

    void detach() {
        mDetached.set(true);
    }

    static class SimpleAdjacentProvider<V> implements AdjacentProvider<V> {
        private int mFirstIndex;
        private int mLastIndex;

        private V mFirstItem;
        private V mLastItem;

        boolean mCounted;
        int mLeadingUnloadedCount;
        int mTrailingUnloadedCount;

        @Override
        public V getFirstLoadedItem() {
            return mFirstItem;
        }

        @Override
        public V getLastLoadedItem() {
            return mLastItem;
        }

        @Override
        public int getFirstLoadedItemIndex() {
            return mFirstIndex;
        }

        @Override
        public int getLastLoadedItemIndex() {
            return mLastIndex;
        }

        @Override
        public void onPageResultResolution(@NonNull PagedList.LoadType type,
                @NonNull DataSource.BaseResult<V> result) {
            if (result.data.isEmpty()) {
                return;
            }
            if (type == PagedList.LoadType.START) {
                mFirstIndex -= result.data.size();
                mFirstItem = result.data.get(0);
                if (mCounted) {
                    mLeadingUnloadedCount -= result.data.size();
                }
            } else if (type == PagedList.LoadType.END) {
                mLastIndex += result.data.size();
                mLastItem = result.data.get(result.data.size() - 1);
                if (mCounted) {
                    mTrailingUnloadedCount -= result.data.size();
                }
            } else {
                mFirstIndex = result.leadingNulls + result.offset;
                mLastIndex = mFirstIndex + result.data.size() - 1;
                mFirstItem = result.data.get(0);
                mLastItem = result.data.get(result.data.size() - 1);

                if (result.counted) {
                    mCounted = true;
                    mLeadingUnloadedCount = result.leadingNulls;
                    mTrailingUnloadedCount = result.trailingNulls;
                }
            }
        }
    }
}
