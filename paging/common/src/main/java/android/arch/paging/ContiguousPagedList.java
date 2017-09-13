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

import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.WorkerThread;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/** @hide */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ContiguousPagedList<T> extends NullPaddedList<T> {

    private final ContiguousDataSource<?, T> mDataSource;
    private final Executor mMainThreadExecutor;
    private final Executor mBackgroundThreadExecutor;
    private final Config mConfig;

    private boolean mPrependWorkerRunning = false;
    private boolean mAppendWorkerRunning = false;

    private int mPrependItemsRequested = 0;
    private int mAppendItemsRequested = 0;

    private int mLastLoad = 0;
    private T mLastItem = null;

    private AtomicBoolean mDetached = new AtomicBoolean(false);

    private ArrayList<WeakReference<Callback>> mCallbacks = new ArrayList<>();

    @WorkerThread
    <K> ContiguousPagedList(@NonNull ContiguousDataSource<K, T> dataSource,
            @NonNull Executor mainThreadExecutor,
            @NonNull Executor backgroundThreadExecutor,
            Config config,
            @Nullable K key) {
        super();

        mDataSource = dataSource;
        mMainThreadExecutor = mainThreadExecutor;
        mBackgroundThreadExecutor = backgroundThreadExecutor;
        mConfig = config;
        NullPaddedList<T> initialState = dataSource.loadInitial(
                key, config.mInitialLoadSizeHint, config.mEnablePlaceholders);

        if (initialState != null) {
            mPositionOffset = initialState.getPositionOffset();

            mLeadingNullCount = initialState.getLeadingNullCount();
            mList = new ArrayList<>(initialState.mList);
            mTrailingNullCount = initialState.getTrailingNullCount();

            if (initialState.getLeadingNullCount() == 0
                    && initialState.getTrailingNullCount() == 0
                    && config.mPrefetchDistance < 1) {
                throw new IllegalArgumentException("Null padding is required to support the 0"
                        + " prefetch case - require either null items or prefetching to fetch"
                        + " beyond initial load.");
            }

            if (initialState.size() != 0) {
                mLastLoad = mLeadingNullCount + mList.size() / 2;
                mLastItem = mList.get(mList.size() / 2);
            }
        } else {
            mList = new ArrayList<>();
            detach();
        }
        if (mList.size() == 0) {
            // Empty initial state, so don't try and fetch data.
            mPrependWorkerRunning = true;
            mAppendWorkerRunning = true;
        }
    }

    @Override
    public T get(int index) {
        T item = super.get(index);
        if (item != null) {
            mLastItem = item;
        }
        return item;
    }

    @Override
    public void loadAround(int index) {
        mLastLoad = index + mPositionOffset;

        int prependItems = mConfig.mPrefetchDistance - (index - mLeadingNullCount);
        int appendItems = index + mConfig.mPrefetchDistance - (mLeadingNullCount + mList.size());

        mPrependItemsRequested = Math.max(prependItems, mPrependItemsRequested);
        if (mPrependItemsRequested > 0) {
            schedulePrepend();
        }

        mAppendItemsRequested = Math.max(appendItems, mAppendItemsRequested);
        if (mAppendItemsRequested > 0) {
            scheduleAppend();
        }
    }

    @Override
    public int getLoadedCount() {
        return mList.size();
    }

    @Override
    public int getLeadingNullCount() {
        return mLeadingNullCount;
    }

    @Override
    public int getTrailingNullCount() {
        return mTrailingNullCount;
    }

    @MainThread
    private void schedulePrepend() {
        if (mPrependWorkerRunning) {
            return;
        }
        mPrependWorkerRunning = true;

        final int position = mLeadingNullCount + mPositionOffset;
        final T item = mList.get(0);
        mBackgroundThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (mDetached.get()) {
                    return;
                }

                final List<T> data = mDataSource.loadBefore(position, item, mConfig.mPageSize);
                if (data != null) {
                    mMainThreadExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            if (mDetached.get()) {
                                return;
                            }
                            prependImpl(data);
                        }
                    });
                } else {
                    detach();
                }
            }
        });
    }

    @MainThread
    private void scheduleAppend() {
        if (mAppendWorkerRunning) {
            return;
        }
        mAppendWorkerRunning = true;

        final int position = mLeadingNullCount + mList.size() - 1 + mPositionOffset;
        final T item = mList.get(mList.size() - 1);
        mBackgroundThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (mDetached.get()) {
                    return;
                }

                final List<T> data = mDataSource.loadAfter(position, item, mConfig.mPageSize);
                if (data != null) {
                    mMainThreadExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            if (mDetached.get()) {
                                return;
                            }
                            appendImpl(data);
                        }
                    });
                } else {
                    detach();
                }
            }
        });
    }

    @MainThread
    private void prependImpl(List<T> before) {
        final int count = before.size();
        if (count == 0) {
            // Nothing returned from source, stop loading in this direction
            return;
        }

        Collections.reverse(before);
        mList.addAll(0, before);

        final int changedCount = Math.min(mLeadingNullCount, count);
        final int addedCount = count - changedCount;

        if (changedCount != 0) {
            mLeadingNullCount -= changedCount;
        }
        mPositionOffset -= addedCount;
        mNumberPrepended += count;


        // only try to post more work after fully prepended (with offsets / null counts updated)
        mPrependItemsRequested -= count;
        mPrependWorkerRunning = false;
        if (mPrependItemsRequested > 0) {
            // not done prepending, keep going
            schedulePrepend();
        }

        // finally dispatch callbacks, after prepend may have already been scheduled
        for (WeakReference<Callback> weakRef : mCallbacks) {
            Callback callback = weakRef.get();
            if (callback != null) {
                if (changedCount != 0) {
                    callback.onChanged(mLeadingNullCount, changedCount);
                }
                if (addedCount != 0) {
                    callback.onInserted(0, addedCount);
                }
            }
        }
    }

    @MainThread
    private void appendImpl(List<T> after) {
        final int count = after.size();
        if (count == 0) {
            // Nothing returned from source, stop loading in this direction
            return;
        }

        mList.addAll(after);

        final int changedCount = Math.min(mTrailingNullCount, count);
        final int addedCount = count - changedCount;

        if (changedCount != 0) {
            mTrailingNullCount -= changedCount;
        }
        mNumberAppended += count;

        // only try to post more work after fully appended (with null counts updated)
        mAppendItemsRequested -= count;
        mAppendWorkerRunning = false;
        if (mAppendItemsRequested > 0) {
            // not done appending, keep going
            scheduleAppend();
        }

        // finally dispatch callbacks, after append may have already been scheduled
        for (WeakReference<Callback> weakRef : mCallbacks) {
            Callback callback = weakRef.get();
            if (callback != null) {
                final int endPosition = mLeadingNullCount + mList.size() - count;
                if (changedCount != 0) {
                    callback.onChanged(endPosition, changedCount);
                }
                if (addedCount != 0) {
                    callback.onInserted(endPosition + changedCount, addedCount);
                }
            }
        }
    }

    @Override
    public boolean isImmutable() {
        // TODO: return true if had nulls, and now getLoadedCount() == size(). Is that safe?
        // Currently we don't prevent DataSources from returning more items than their null counts
        return isDetached();
    }

    @Override
    public void addWeakCallback(@Nullable PagedList<T> previousSnapshot,
            @NonNull Callback callback) {
        NullPaddedList<T> snapshot = (NullPaddedList<T>) previousSnapshot;
        if (snapshot != this && snapshot != null) {
            final int newlyAppended = mNumberAppended - snapshot.getNumberAppended();
            final int newlyPrepended = mNumberPrepended - snapshot.getNumberPrepended();

            final int previousTrailing = snapshot.getTrailingNullCount();
            final int previousLeading = snapshot.getLeadingNullCount();

            // Validate that the snapshot looks like a previous version of this list - if it's not,
            // we can't be sure we'll dispatch callbacks safely
            if (newlyAppended < 0
                    || newlyPrepended < 0
                    || mTrailingNullCount != Math.max(previousTrailing - newlyAppended, 0)
                    || mLeadingNullCount != Math.max(previousLeading - newlyPrepended, 0)
                    || snapshot.getLoadedCount() + newlyAppended + newlyPrepended != mList.size()) {
                throw new IllegalArgumentException("Invalid snapshot provided - doesn't appear"
                        + " to be a snapshot of this list");
            }

            if (newlyAppended != 0) {
                final int changedCount = Math.min(previousTrailing, newlyAppended);
                final int addedCount = newlyAppended - changedCount;

                final int endPosition =
                        snapshot.getLeadingNullCount() + snapshot.getLoadedCount();
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
        mCallbacks.add(new WeakReference<>(callback));
    }

    @Override
    public void removeWeakCallback(@NonNull Callback callback) {
        for (int i = mCallbacks.size() - 1; i >= 0; i--) {
            Callback currentCallback = mCallbacks.get(i).get();
            if (currentCallback == null || currentCallback == callback) {
                mCallbacks.remove(i);
            }
        }
    }

    @Override
    public boolean isDetached() {
        return mDetached.get();
    }

    @SuppressWarnings("WeakerAccess")
    public void detach() {
        mDetached.set(true);
    }

    @Nullable
    @Override
    public Object getLastKey() {
        return mDataSource.getKey(mLastLoad, mLastItem);
    }
}
