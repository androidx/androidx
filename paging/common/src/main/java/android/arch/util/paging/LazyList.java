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

import android.arch.core.internal.SafeIterableMap;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Lazy-loading list, which defaults each entry to be null and pages content in on-demand,
 * with prefetching.
 *
 * When a list item is read as null, content is paged in from the CountedDataSource asynchronously.
 *
 * This class is NOT thread safe. All accesses to get()/access() and mutations should be made on the
 * thread defined by the mainThreadExecutor.
 *
 * @param <Type> Data type held by this list.
 */
public class LazyList<Type> extends PagerBase<Type> {
    private int mMissedMinIndex = Integer.MAX_VALUE;
    private int mMissedMaxIndex = -1;

    private int mStartPos; // position is inclusive
    private final int mSize;

    // last position accessed via get()
    private volatile int mLastAccessed;

    private SafeIterableMap<ChangeCallback,
            ChangeCallbackWrapper> mCallbacks = new SafeIterableMap<>();

    private CountedDataSource<Type> mCountedDataSource;

    @WorkerThread
    public LazyList(@NonNull CountedDataSource<Type> countedDataSource,
            @NonNull Executor mainThreadExecutor, @NonNull Executor backgroundThreadExecutor,
            @NonNull ListConfig config) {
        super(mainThreadExecutor, backgroundThreadExecutor, config);
        mCountedDataSource = countedDataSource;
        mSize = mCountedDataSource.loadCount();
    }

    // ---- List ----

    @Override
    @Nullable
    public Type get(int index) {
        if (index < 0 || index >= mSize) {
            throw new IndexOutOfBoundsException(index + " is out of bounds. Size:" + mSize);
        }

        mLastAccessed = index;

        final boolean initialLoadComplete = mItems != null;

        // note: intentionally avoid triggering prefetching, so that prefetches aren't outstanding
        // when LazyList is passed to consumers

        // prefetch
        int minTarget = Math.max(0, index - mConfig.mPrefetchDistance);
        int maxTarget = Math.min(mSize, index + mConfig.mPrefetchDistance);
        if (minTarget < mMissedMinIndex) {
            mMissedMinIndex = minTarget;
            if (initialLoadComplete) {
                loadBeforeIfNeeded();
            }
        }
        if (maxTarget > mMissedMaxIndex) {
            mMissedMaxIndex = maxTarget;
            if (initialLoadComplete) {
                loadAfterIfNeeded();
            }
        }

        if (initialLoadComplete) {
            return access(index);
        } else {
            triggerInitialLoad(getInitialLoadPositionFor(index));
            return null;
        }
    }

    @Override
    public int size() {
        return mSize;
    }

    @Override
    @Nullable
    public Type access(int index) {
        if (index < 0 || index >= mSize) {
            throw new IndexOutOfBoundsException(index + " is out of bounds. Size:" + mSize);
        }
        if (mItems == null || index < mStartPos || index >= mStartPos + mItems.size()) {
            return null;
        } else {
            return mItems.get(index - mStartPos);
        }
    }

    // ---- Initial data ----

    private int getInitialLoadPositionFor(int index) {
        return Math.max(-1, index - mConfig.mPageSize / 2);
    }

    /**
     * @return Suggested initial load position for a new LazyList to load the initial state at.
     */
    public int getInitialLoadPosition() {
        return getInitialLoadPositionFor(mLastAccessed);
    }

    @MainThread
    private void triggerInitialLoad(final int loadAfterPos) {
        if (mInitialized) {
            return;
        }
        mInitialized = true;
        mBackgroundThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (mInvalid.get()) {
                    return;
                }
                // find a good start point to center the index
                final List<Type> load = mCountedDataSource.loadAfterInitial(
                        loadAfterPos, mConfig.mPageSize);

                if (load != null) {
                    mMainThreadExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            if (mInvalid.get()) {
                                return;
                            }
                            setInitialDataAndDispatch(loadAfterPos, load);
                        }
                    });
                } else {
                    freeze();
                }
            }
        });
    }

    /**
     *
     * @param loadAfterPos Position to pass to {@link CountedDataSource#loadAfterInitial(int, int)}
     *                     for warmup.
     * @return True on success.
     */
    public boolean internalInit(int loadAfterPos)  {
        List<Type> load = mCountedDataSource.loadAfterInitial(loadAfterPos, mConfig.mPageSize);
        if (load != null) {
            setInitialDataAndDispatch(loadAfterPos, load);
            mInitialized = true;
            return true;
        } else {
            return false;
        }
    }

    private void setInitialDataAndDispatch(int loadAfterPos, List<Type> items) {
        mStartPos = loadAfterPos + 1;
        setInitialData(items);
        dispatchChange();
    }

    // ---- Callback ----

    /**
     * Adds a change callback, to observe when new content is loaded into the list. The callback is
     * invoked once nulls in the list are loaded from the data source.
     *
     * @param callback The callback, invoked on the main thread.
     *
     * @see #removeCallback(ChangeCallback)
     */
    @SuppressWarnings("WeakerAccess")
    public void addCallback(ChangeCallback callback) {
        int start = mStartPos;
        int end = mStartPos + (mItems == null ? 0 : mItems.size());
        ChangeCallbackWrapper wrapper = new ChangeCallbackWrapper(callback, start, end);
        // do not notify the latest data so the observer can distinguish whether the data has
        // really changed or not.
        mCallbacks.putIfAbsent(callback, wrapper);
    }

    /**
     * Removes a previously added change callback.
     *
     * @param callback The callback, invoked on the main thread.
     *
     * @see #addCallback(ChangeCallback)
     */
    @SuppressWarnings("WeakerAccess")
    public void removeCallback(ChangeCallback callback) {
        mCallbacks.remove(callback);
    }

    private void dispatchChange() {
        int start = mStartPos;
        int end = mStartPos + mItems.size();
        for (Map.Entry<ChangeCallback, ChangeCallbackWrapper> mCallback : mCallbacks) {
            mCallback.getValue().dispatchLoaded(start, end);
        }
    }

    /**
     * Callback that signals when new content is loaded in the list.
     */
    public abstract static class ChangeCallback {
        /**
         * Called when new content is loaded into the LazyList.
         *
         * @param start Start index of the newly loaded items.
         * @param count Number of newly loaded items.
         */
        public abstract void onLoaded(int start, int count);
    }

    private static class ChangeCallbackWrapper {
        private final ChangeCallback mChangeCallback;
        private int mKnownStart;
        private int mKnownEnd;

        ChangeCallbackWrapper(@NonNull ChangeCallback changeCallback,
                int knownStart, int knownEnd) {
            mChangeCallback = changeCallback;
            mKnownStart = knownStart;
            mKnownEnd = knownEnd;
        }

        void dispatchLoaded(int currentStart, int currentEnd) {
            if (currentEnd > mKnownEnd) {
                mChangeCallback.onLoaded(mKnownEnd, currentEnd - mKnownEnd);
                mKnownEnd = currentEnd;
            }
            if (currentStart < mKnownStart) {
                mChangeCallback.onLoaded(currentStart, mKnownStart - currentStart);
                mKnownStart = currentStart;
            }
        }
    }


    // ---- PagerBase implementation ----

    @Override
    void onItemsPrepended(int count) {
        mStartPos -= count;
        dispatchChange();
    }

    @Override
    void onItemsAppended(int count) {
        dispatchChange();
    }

    @Override
    void loadBeforeIfNeeded() {
        if (mMissedMinIndex >= mStartPos) {
            // done loading
            mMissedMinIndex = Integer.MAX_VALUE;
        } else {
            // not done, resume loading
            loadBefore(mStartPos);
        }
    }

    @Override
    void loadAfterIfNeeded() {
        if (mMissedMaxIndex <= mStartPos + mItems.size() - 1) {
            // done loading
            mMissedMaxIndex = 0;
        } else {
            // not done, resume loading
            loadAfter(mStartPos + mItems.size() - 1);
        }
    }

    @Nullable
    @Override
    List<Type> loadBeforeImpl(int position, Type item) {
        return mCountedDataSource.loadBefore(position, item, mConfig.mPageSize);
    }

    @Nullable
    @Override
    List<Type> loadAfterImpl(int position, Type item) {
        return mCountedDataSource.loadAfter(position, item, mConfig.mPageSize);
    }
}
