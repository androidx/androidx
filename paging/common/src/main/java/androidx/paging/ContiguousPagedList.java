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

package androidx.paging;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.concurrent.Executor;

class ContiguousPagedList<K, V> extends PagedList<V> implements PagedStorage.Callback,
        Pager.PageConsumer<V> {
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final DataSource<K, V> mDataSource;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    int mPrependItemsRequested = 0;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    int mAppendItemsRequested = 0;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    boolean mReplacePagesWithNulls = false;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final boolean mShouldTrim;

    /**
     * Given a page result, apply or drop it, and return whether more loading is needed.
     */
    @Override
    public boolean onPageResult(@NonNull LoadType type,
            @NonNull DataSource.BaseResult<V> pageResult) {
        boolean continueLoading = false;
        @NonNull List<V> page = pageResult.data;


        // if we end up trimming, we trim from side that's furthest from most recent access
        boolean trimFromFront = mLastLoad > mStorage.getMiddleOfLoadedRange();

        // is the new page big enough to warrant pre-trimming (i.e. dropping) it?
        boolean skipNewPage = mShouldTrim
                && mStorage.shouldPreTrimNewPage(
                mConfig.maxSize, mRequiredRemainder, page.size());

        if (type == LoadType.END) {
            if (skipNewPage && !trimFromFront) {
                // don't append this data, drop it
                mAppendItemsRequested = 0;
            } else {
                mStorage.appendPage(page, ContiguousPagedList.this);
                mAppendItemsRequested -= page.size();
                if (mAppendItemsRequested > 0 && page.size() != 0) {
                    continueLoading = true;
                }
            }
        } else if (type == LoadType.START) {
            if (skipNewPage && trimFromFront) {
                // don't append this data, drop it
                mPrependItemsRequested = 0;
            } else {
                mStorage.prependPage(page, ContiguousPagedList.this);
                mPrependItemsRequested -= page.size();
                if (mPrependItemsRequested > 0 && page.size() != 0) {
                    continueLoading = true;
                }
            }
        } else {
            throw new IllegalArgumentException("unexpected result type " + type);
        }

        if (mShouldTrim) {
            // Try and trim, but only if the side being trimmed isn't actually fetching.
            // For simplicity (both of impl here, and contract w/ DataSource) we don't
            // allow fetches in same direction - this means reading the load state is safe.
            if (trimFromFront) {
                if (mPager.mLoadStateManager.getStart() != LoadState.LOADING) {
                    if (mStorage.trimFromFront(
                            mReplacePagesWithNulls,
                            mConfig.maxSize,
                            mRequiredRemainder,
                            ContiguousPagedList.this)) {
                        // trimmed from front, ensure we can fetch in that dir
                        mPager.mLoadStateManager.setState(LoadType.START, LoadState.IDLE, null);
                    }
                }
            } else {
                if (mPager.mLoadStateManager.getEnd() != LoadState.LOADING) {
                    if (mStorage.trimFromEnd(
                            mReplacePagesWithNulls,
                            mConfig.maxSize,
                            mRequiredRemainder,
                            ContiguousPagedList.this)) {
                        mPager.mLoadStateManager.setState(LoadType.END, LoadState.IDLE, null);
                    }
                }
            }
        }

        triggerBoundaryCallback(type, page);
        return continueLoading;
    }

    @Override
    public void onStateChanged(@NonNull LoadType type, @NonNull LoadState state,
            @Nullable Throwable error) {
        dispatchStateChange(type, state, error);
    }

    private void triggerBoundaryCallback(@NonNull LoadType type, @NonNull List<V> page) {
        if (mBoundaryCallback != null) {
            boolean deferEmpty = mStorage.size() == 0;
            boolean deferBegin = !deferEmpty
                    && type == LoadType.START
                    && page.size() == 0;
            boolean deferEnd = !deferEmpty
                    && type == LoadType.END
                    && page.size() == 0;
            deferBoundaryCallbacks(deferEmpty, deferBegin, deferEnd);
        }
    }

    @NonNull
    private final Pager mPager;

    @Override
    public void retry() {
        super.retry();
        mPager.retry();

        if (mRefreshRetryCallback != null
                && mPager.mLoadStateManager.getRefresh() == LoadState.RETRYABLE_ERROR) {
            // Loading the next PagedList failed, signal the retry callback.
            mRefreshRetryCallback.run();
        }
    }

    static final int LAST_LOAD_UNSPECIFIED = -1;

    ContiguousPagedList(
            @NonNull DataSource<K, V> dataSource,
            @NonNull Executor mainThreadExecutor,
            @NonNull Executor backgroundThreadExecutor,
            @Nullable BoundaryCallback<V> boundaryCallback,
            @NonNull Config config,
            @NonNull DataSource.BaseResult<V> initialResult,
            int lastLoad) {
        super(new PagedStorage<V>(), mainThreadExecutor, backgroundThreadExecutor,
                boundaryCallback, config);
        mDataSource = dataSource;
        mLastLoad = lastLoad;
        mPager = new Pager<>(config, dataSource, mainThreadExecutor, backgroundThreadExecutor,
                this, mStorage, initialResult);

        if (config.enablePlaceholders) {
            // Placeholders enabled, pass raw data to storage init
            mStorage.init(initialResult.leadingNulls, initialResult.data,
                    initialResult.trailingNulls, initialResult.offset, this);
        } else {
            // If placeholder are disabled, avoid passing leading/trailing nulls,
            // since DataSource may have passed them anyway
            mStorage.init(0, initialResult.data,
                    0, initialResult.offset + initialResult.leadingNulls, this);
        }

        mShouldTrim = mDataSource.supportsPageDropping()
                && mConfig.maxSize != Config.MAX_SIZE_UNBOUNDED;

        if (mLastLoad == LAST_LOAD_UNSPECIFIED) {
            // Because the ContiguousPagedList wasn't initialized with a last load position,
            // initialize it to the middle of the initial load
            mLastLoad = initialResult.leadingNulls + initialResult.offset
                    + initialResult.data.size() / 2;
        }
        triggerBoundaryCallback(LoadType.REFRESH, initialResult.data);
    }

    @Override
    void dispatchCurrentLoadState(LoadStateListener listener) {
        mPager.mLoadStateManager.dispatchCurrentLoadState(listener);
    }

    @Override
    void setInitialLoadState(@NonNull LoadState loadState, @Nullable Throwable error) {
        mPager.mLoadStateManager.setState(LoadType.REFRESH, loadState, error);
    }

    @MainThread
    @Override
    void dispatchUpdatesSinceSnapshot(
            @NonNull PagedList<V> pagedListSnapshot, @NonNull Callback callback) {
        final PagedStorage<V> snapshot = pagedListSnapshot.mStorage;

        final int newlyAppended = mStorage.getNumberAppended() - snapshot.getNumberAppended();
        final int newlyPrepended = mStorage.getNumberPrepended() - snapshot.getNumberPrepended();

        final int previousTrailing = snapshot.getTrailingNullCount();
        final int previousLeading = snapshot.getLeadingNullCount();

        // Validate that the snapshot looks like a previous version of this list - if it's not,
        // we can't be sure we'll dispatch callbacks safely
        if (snapshot.isEmpty()
                || newlyAppended < 0
                || newlyPrepended < 0
                || mStorage.getTrailingNullCount() != Math.max(previousTrailing - newlyAppended, 0)
                || mStorage.getLeadingNullCount() != Math.max(previousLeading - newlyPrepended, 0)
                || (mStorage.getStorageCount()
                        != snapshot.getStorageCount() + newlyAppended + newlyPrepended)) {
            throw new IllegalArgumentException("Invalid snapshot provided - doesn't appear"
                    + " to be a snapshot of this PagedList");
        }

        if (newlyAppended != 0) {
            final int changedCount = Math.min(previousTrailing, newlyAppended);
            final int addedCount = newlyAppended - changedCount;

            final int endPosition = snapshot.getLeadingNullCount() + snapshot.getStorageCount();
            if (changedCount != 0) {
                callback.onChanged(endPosition, changedCount);
            }
            if (addedCount != 0) {
                callback.onInserted(endPosition + changedCount, addedCount);
            }
        }
        if (newlyPrepended != 0) {
            final int changedCount = Math.min(previousLeading, newlyPrepended);
            final int addedCount = newlyPrepended - changedCount;

            if (changedCount != 0) {
                callback.onChanged(previousLeading, changedCount);
            }
            if (addedCount != 0) {
                callback.onInserted(0, addedCount);
            }
        }
    }

    static int getPrependItemsRequested(int prefetchDistance, int index, int leadingNulls) {
        return prefetchDistance - (index - leadingNulls);
    }

    static int getAppendItemsRequested(
            int prefetchDistance, int index, int itemsBeforeTrailingNulls) {
        return index + prefetchDistance + 1 - itemsBeforeTrailingNulls;
    }

    @MainThread
    @Override
    protected void loadAroundInternal(int index) {
        int prependItems = getPrependItemsRequested(mConfig.prefetchDistance, index,
                mStorage.getLeadingNullCount());
        int appendItems = getAppendItemsRequested(mConfig.prefetchDistance, index,
                mStorage.getLeadingNullCount() + mStorage.getStorageCount());

        mPrependItemsRequested = Math.max(prependItems, mPrependItemsRequested);
        if (mPrependItemsRequested > 0) {
            mPager.trySchedulePrepend();
        }

        mAppendItemsRequested = Math.max(appendItems, mAppendItemsRequested);
        if (mAppendItemsRequested > 0) {
            mPager.tryScheduleAppend();
        }
    }

    @Override
    public boolean isDetached() {
        return mPager.isDetached();
    }

    @Override
    public void detach() {
        mPager.detach();
    }

    @Override
    boolean isContiguous() {
        return true;
    }

    @NonNull
    @Override
    public DataSource<?, V> getDataSource() {
        return mDataSource;
    }

    @Nullable
    @Override
    public Object getLastKey() {
        return mDataSource.getKey(mLastLoad, mLastItem);
    }

    @MainThread
    @Override
    public void onInitialized(int count) {
        notifyInserted(0, count);
        // simple heuristic to decide if, when dropping pages, we should replace with placeholders
        mReplacePagesWithNulls =
                mStorage.getLeadingNullCount() > 0 || mStorage.getTrailingNullCount() > 0;
    }

    @MainThread
    @Override
    public void onPagePrepended(int leadingNulls, int changedCount, int addedCount) {

        // finally dispatch callbacks, after prepend may have already been scheduled
        notifyChanged(leadingNulls, changedCount);
        notifyInserted(0, addedCount);

        offsetAccessIndices(addedCount);
    }

    @MainThread
    @Override
    public void onPageAppended(int endPosition, int changedCount, int addedCount) {
        // finally dispatch callbacks, after append may have already been scheduled
        notifyChanged(endPosition, changedCount);
        notifyInserted(endPosition + changedCount, addedCount);
    }


    @MainThread
    @Override
    public void onPagePlaceholderInserted(int pageIndex) {
        throw new IllegalStateException("Tiled callback on ContiguousPagedList");
    }

    @MainThread
    @Override
    public void onPageInserted(int start, int count) {
        throw new IllegalStateException("Tiled callback on ContiguousPagedList");
    }

    @Override
    public void onPagesRemoved(int startOfDrops, int count) {
        notifyRemoved(startOfDrops, count);
    }

    @Override
    public void onPagesSwappedToPlaceholder(int startOfDrops, int count) {
        notifyChanged(startOfDrops, count);
    }
}
