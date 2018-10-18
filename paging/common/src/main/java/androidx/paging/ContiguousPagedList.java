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

import androidx.annotation.AnyThread;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.concurrent.Executor;

class ContiguousPagedList<K, V> extends PagedList<V> implements PagedStorage.Callback {
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final ContiguousDataSource<K, V> mDataSource;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    int mPrependItemsRequested = 0;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    int mAppendItemsRequested = 0;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    boolean mReplacePagesWithNulls = false;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final boolean mShouldTrim;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    PageResult.Receiver<V> mReceiver = new PageResult.Receiver<V>() {
        // Creation thread for initial synchronous load, otherwise main thread
        // Safe to access main thread only state - no other thread has reference during construction
        @AnyThread
        @Override
        public void onPageResult(@PageResult.ResultType int resultType,
                @NonNull PageResult<V> pageResult) {
            if (pageResult.isInvalid()) {
                detach();
                return;
            }

            if (isDetached()) {
                // No op, have detached
                return;
            }

            List<V> page = pageResult.page;
            if (resultType == PageResult.INIT) {
                mStorage.init(pageResult.leadingNulls, page, pageResult.trailingNulls,
                        pageResult.positionOffset, ContiguousPagedList.this);
                // TODO: signal that this list is ready to be dispatched to observer

                if (mLastLoad == LAST_LOAD_UNSPECIFIED) {
                    // Because the ContiguousPagedList wasn't initialized with a last load position,
                    // initialize it to the middle of the initial load
                    mLastLoad =
                            pageResult.leadingNulls + pageResult.positionOffset + page.size() / 2;
                }
            } else {
                // if we end up trimming, we trim from side that's furthest from most recent access
                boolean trimFromFront = mLastLoad > mStorage.getMiddleOfLoadedRange();

                // is the new page big enough to warrant pre-trimming (i.e. dropping) it?
                boolean skipNewPage = mShouldTrim
                        && mStorage.shouldPreTrimNewPage(
                                mConfig.maxSize, mRequiredRemainder, page.size());

                if (resultType == PageResult.APPEND) {
                    if (skipNewPage && !trimFromFront) {
                        // don't append this data, drop it
                        mAppendItemsRequested = 0;
                        mLoadStateManager.setState(LoadType.END, LoadState.IDLE, null);
                    } else {
                        mStorage.appendPage(page, ContiguousPagedList.this);
                    }
                } else if (resultType == PageResult.PREPEND) {
                    if (skipNewPage && trimFromFront) {
                        // don't append this data, drop it
                        mPrependItemsRequested = 0;
                        mLoadStateManager.setState(LoadType.START, LoadState.IDLE, null);
                    } else {
                        mStorage.prependPage(page, ContiguousPagedList.this);
                    }
                } else {
                    throw new IllegalArgumentException("unexpected resultType " + resultType);
                }

                if (mShouldTrim) {
                    // Try and trim, but only if the side being trimmed isn't actually fetching.
                    // For simplicity (both of impl here, and contract w/ DataSource) we don't want
                    // simultaneous fetches in same direction.
                    if (trimFromFront) {
                        if (mLoadStateManager.getStart() != LoadState.LOADING) {
                            if (mStorage.trimFromFront(
                                    mReplacePagesWithNulls,
                                    mConfig.maxSize,
                                    mRequiredRemainder,
                                    ContiguousPagedList.this)) {
                                // trimmed from front, ensure we can fetch in that dir
                                mLoadStateManager.setState(LoadType.START, LoadState.IDLE, null);
                            }
                        }
                    } else {
                        if (mLoadStateManager.getEnd() != LoadState.LOADING) {
                            if (mStorage.trimFromEnd(
                                    mReplacePagesWithNulls,
                                    mConfig.maxSize,
                                    mRequiredRemainder,
                                    ContiguousPagedList.this)) {
                                mLoadStateManager.setState(LoadType.END, LoadState.IDLE, null);
                            }
                        }
                    }
                }
            }

            if (mBoundaryCallback != null) {
                boolean deferEmpty = mStorage.size() == 0;
                boolean deferBegin = !deferEmpty
                        && resultType == PageResult.PREPEND
                        && pageResult.page.size() == 0;
                boolean deferEnd = !deferEmpty
                        && resultType == PageResult.APPEND
                        && pageResult.page.size() == 0;
                deferBoundaryCallbacks(deferEmpty, deferBegin, deferEnd);
            }
        }

        @Override
        public void onPageError(@PageResult.ResultType int resultType,
                @NonNull Throwable error, boolean retryable) {
            LoadState errorState = retryable ? LoadState.RETRYABLE_ERROR : LoadState.ERROR;

            if (resultType == PageResult.PREPEND) {
                mLoadStateManager.setState(LoadType.START, errorState, error);
            } else if (resultType == PageResult.APPEND) {
                mLoadStateManager.setState(LoadType.END, errorState, error);
            } else {
                // TODO: pass init signal through to *previous* list
                throw new IllegalStateException("TODO");
            }
        }
    };

    @Override
    public void retry() {
        super.retry();
        if (mLoadStateManager.getStart() == LoadState.RETRYABLE_ERROR) {
            schedulePrepend();
        }
        if (mLoadStateManager.getEnd() == LoadState.RETRYABLE_ERROR) {
            scheduleAppend();
        }
    }

    static final int LAST_LOAD_UNSPECIFIED = -1;

    ContiguousPagedList(
            @NonNull ContiguousDataSource<K, V> dataSource,
            @NonNull Executor mainThreadExecutor,
            @NonNull Executor backgroundThreadExecutor,
            @Nullable BoundaryCallback<V> boundaryCallback,
            @NonNull Config config,
            @Nullable K key,
            int lastLoad) {
        super(new PagedStorage<V>(), mainThreadExecutor, backgroundThreadExecutor,
                boundaryCallback, config);
        mDataSource = dataSource;
        mLastLoad = lastLoad;

        if (mDataSource.isInvalid()) {
            detach();
        } else {
            mDataSource.dispatchLoadInitial(key,
                    mConfig.initialLoadSizeHint,
                    mConfig.pageSize,
                    mConfig.enablePlaceholders,
                    mMainThreadExecutor,
                    mReceiver);
        }
        mShouldTrim = mDataSource.supportsPageDropping()
                && mConfig.maxSize != Config.MAX_SIZE_UNBOUNDED;
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
        if (mPrependItemsRequested > 0 && mLoadStateManager.getStart() == LoadState.IDLE) {
            schedulePrepend();
        }

        mAppendItemsRequested = Math.max(appendItems, mAppendItemsRequested);
        if (mAppendItemsRequested > 0 && mLoadStateManager.getEnd() == LoadState.IDLE) {
            scheduleAppend();
        }
    }

    @MainThread
    private void schedulePrepend() {
        mLoadStateManager.setState(LoadType.START, LoadState.LOADING, null);

        final int position = mStorage.getLeadingNullCount() + mStorage.getPositionOffset();

        // safe to access first item here - mStorage can't be empty if we're prepending
        final V item = mStorage.getFirstLoadedItem();
        mBackgroundThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (isDetached()) {
                    return;
                }
                if (mDataSource.isInvalid()) {
                    detach();
                } else {
                    mDataSource.dispatchLoadBefore(position, item, mConfig.pageSize,
                            mMainThreadExecutor, mReceiver);
                }
            }
        });
    }

    @MainThread
    private void scheduleAppend() {
        mLoadStateManager.setState(LoadType.END, LoadState.LOADING, null);

        final int position = mStorage.getLeadingNullCount()
                + mStorage.getStorageCount() - 1 + mStorage.getPositionOffset();

        // safe to access first item here - mStorage can't be empty if we're appending
        final V item = mStorage.getLastLoadedItem();
        mBackgroundThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (isDetached()) {
                    return;
                }
                if (mDataSource.isInvalid()) {
                    detach();
                } else {
                    mDataSource.dispatchLoadAfter(position, item, mConfig.pageSize,
                            mMainThreadExecutor, mReceiver);
                }
            }
        });
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
        // consider whether to post more work, now that a page is fully prepended
        mPrependItemsRequested = mPrependItemsRequested - changedCount - addedCount;
        if (mPrependItemsRequested > 0) {
            // not done prepending, keep going
            schedulePrepend();
        } else {
            mLoadStateManager.setState(LoadType.START, LoadState.IDLE, null);
        }

        // finally dispatch callbacks, after prepend may have already been scheduled
        notifyChanged(leadingNulls, changedCount);
        notifyInserted(0, addedCount);

        offsetAccessIndices(addedCount);
    }

    @MainThread
    @Override
    public void onEmptyPrepend() {
        mLoadStateManager.setState(LoadType.START, LoadState.DONE, null);
    }

    @MainThread
    @Override
    public void onPageAppended(int endPosition, int changedCount, int addedCount) {
        // consider whether to post more work, now that a page is fully appended
        mAppendItemsRequested = mAppendItemsRequested - changedCount - addedCount;
        if (mAppendItemsRequested > 0) {
            // not done appending, keep going
            scheduleAppend();
        } else {
            mLoadStateManager.setState(LoadType.END, LoadState.IDLE, null);
        }

        // finally dispatch callbacks, after append may have already been scheduled
        notifyChanged(endPosition, changedCount);
        notifyInserted(endPosition + changedCount, addedCount);
    }

    @MainThread
    @Override
    public void onEmptyAppend() {
        mLoadStateManager.setState(LoadType.END, LoadState.DONE, null);
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
