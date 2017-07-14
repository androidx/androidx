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
import android.support.annotation.RestrictTo;
import android.support.annotation.WorkerThread;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Lazy loading list that starts at size 0, and expands its size by loading pages
 * asynchronously from a DataSource.
 *
 * @param <Type> Data type held by this list.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PagedList<Type> extends PagerBase<Type> {
    private static final int INVALID_DATA_POSITION = -1;

    // Distance from front of list that should be prefetched.
    private int mMinTarget = 0;

    // Distance from tail of list that should be prefetched.
    private int mMaxTarget = 0;

    /**
     * Stores the delta between the anchor and the most recently accessed item.
     *
     * The most recently accessed item's index can be referenced
     * by adding this value to {@link #mAnchor}.
     */
    private volatile int mInterestedKeyOffset = -1;

    // anchor position used to calculate deltas based on what revision observers last saw
    private int mAnchor = 0;

    private DataSource<Object, Type> mDataSource;

    private SafeIterableMap<ChangeCallback,
                ChangeCallbackWrapper> mCallbacks = new SafeIterableMap<>();

    @WorkerThread
    public <Key> PagedList(@NonNull DataSource<Key, Type> dataSource,
            @NonNull Executor mainThreadExecutor, @NonNull Executor backgroundThreadExecutor,
            @NonNull ListConfig config) {
        super(mainThreadExecutor, backgroundThreadExecutor, config);
        if (config.mPrefetchDistance <= 0) {
            // TODO: catch this in PagedListAdapterHelper too
            throw new IllegalArgumentException("PagedList requires a positive prefetch distance"
                    + " to be able to trigger page loading.");
        }

        mDataSource = (DataSource<Object, Type>) dataSource;
    }

    // ---- List ----

    @Override
    @NonNull
    public Type get(int index) {
        if (index < 0 || index >= size()) {
            throw new IllegalArgumentException();
        }
        final int prefetchDistance = mConfig.mPrefetchDistance;
        int minTarget = prefetchDistance - index;
        if (minTarget > mMinTarget) {
            mMinTarget = minTarget;
            loadBeforeIfNeeded();
        }
        int maxTarget = index + prefetchDistance - size();
        if (maxTarget > mMaxTarget) {
            mMaxTarget = maxTarget;
            loadAfterIfNeeded();
        }

        mInterestedKeyOffset = index - mAnchor;

        return mItems.get(index);
    }

    @Override
    @NonNull
    public Type access(int index) {
        if (index < 0 || index >= size()) {
            throw new IllegalArgumentException();
        }
        return mItems.get(index);
    }

    @Override
    public int size() {
        return mItems == null ? 0 : mItems.size();
    }


    // ---- Initial data ----

    /**
     * Post a background task to execute loadAfter(null), and initialize the PagedList
     * with its result.
     */
    @MainThread
    public <Key> void triggerInitialLoad(final Key initialLoadKey) {
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

                final List<Type> initialData =
                        mDataSource.loadAfterInitial(initialLoadKey, mConfig.mPageSize);
                if (initialData != null) {
                    mMainThreadExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            if (mInvalid.get()) {
                                return;
                            }
                            setInitialDataAndDispatch(initialData);
                        }
                    });
                } else {
                    freeze();
                }
            }
        });
    }

    /**
     * Initialize the PagedList from the most-recently-accessed position information from the 'old'
     * PagedList.
     *
     * This may be called on a background thread, and will only access immutable-once-invalid state
     * in the old list, plus the volatile mInterestedKeyOffset field.
     */
    @WorkerThread
    public boolean initializeFrom(PagedList<Type> old) {
        // Note: even though old may be actively used on the foreground thread, we carefully only
        // access parts that are immutable once old is invalid:
        // mAnchor, and mItems (since no further prepends/appends can occur)

        int targetIndex = (old.mAnchor + old.mInterestedKeyOffset);
        int loadAfterIndex = Math.max(0, targetIndex - mConfig.mPageSize / 2) - 1;

        Object loadAfterKey = null;
        if (loadAfterIndex >= 0) {
            Type loadAfterItem = old.mItems.get(loadAfterIndex);
            loadAfterKey = mDataSource.getKey(loadAfterItem);
        }

        List<Type> initialData =
                mDataSource.loadAfterInitial(loadAfterKey, mConfig.mPageSize);
        if (initialData != null) {
            setInitialDataAndDispatch(initialData);
            mInitialized = true;
            return true;
        } else {
            freeze();
            return false;
        }
    }

    // ---- Callback ----

    private void setInitialDataAndDispatch(List<Type> items) {
        // NOTE: while it's possible to prefetch given initial data, but we defer first prefetch
        // until a consumer triggers it, so that the list is immutable until get() is called.

        setInitialData(items);
        dispatchChange();
    }

    /**
     * Adds a change callback, to observe when new content is loaded into the list. The callback is
     * invoked when the list has newly loaded content prepended, or appended.
     *
     * @param callback The callback, invoked on the main thread.
     *
     * @see #removeCallback(ChangeCallback)
     */
    @SuppressWarnings("WeakerAccess")
    public void addCallback(ChangeCallback callback) {
        ChangeCallbackWrapper wrapper = new ChangeCallbackWrapper(callback, mAnchor, size());
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
        int anchor = mAnchor;
        int size = mItems.size();
        for (Map.Entry<ChangeCallback, ChangeCallbackWrapper> mCallback : mCallbacks) {
            mCallback.getValue().dispatchLoaded(anchor, size);
        }
    }

    /**
     * Callback that signals when new content is loaded in the list.
     */
    public abstract static class ChangeCallback {
        /**
         * Called when new content is inserted into the PagedList.
         *
         * @param start Index where the items have been inserted.
         * @param count Number of newly inserted items.
         */
        public abstract void onInserted(int start, int count);
    }

    static class ChangeCallbackWrapper {
        final ChangeCallback mChangeCallback;
        private int mKnownAnchor;
        private int mKnownSize;

        ChangeCallbackWrapper(@NonNull ChangeCallback changeCallback,
                int knownAnchor, int knownSize) {
            mChangeCallback = changeCallback;
            mKnownAnchor = knownAnchor;
            mKnownSize = knownSize;
        }

        void dispatchLoaded(int currentAnchor, int currentSize) {
            if (currentAnchor > mKnownAnchor) {
                int itemsAddedToFront = currentAnchor - mKnownAnchor;
                mChangeCallback.onInserted(0, itemsAddedToFront);
                mKnownSize += itemsAddedToFront;
                mKnownAnchor = currentAnchor;
            }
            if (mKnownSize < currentSize) {
                int itemsAddedToBack = currentSize - mKnownSize;
                mChangeCallback.onInserted(mKnownSize, itemsAddedToBack);
                mKnownSize += itemsAddedToBack;
            }
        }
    }


    // ---- PagerBase implementation ----

    @Override
    void onItemsPrepended(int count) {
        if (mMinTarget > 0) {
            mMinTarget -= count;
        }
        mAnchor += count;
        dispatchChange();
    }

    @Override
    void onItemsAppended(int count) {
        if (mMaxTarget > 0) {
            mMaxTarget -= count;
        }
        dispatchChange();
    }

    @Override
    void loadBeforeIfNeeded() {
        if (mMinTarget > 0) {
            loadBefore(INVALID_DATA_POSITION);
        }
    }

    @Override
    void loadAfterIfNeeded() {
        if (mMaxTarget > 0) {
            loadAfter(INVALID_DATA_POSITION);
        }
    }

    @Nullable
    @Override
    List<Type> loadBeforeImpl(int position, Type item) {
        return mDataSource.loadBefore(item, mConfig.mPageSize);
    }

    @Nullable
    @Override
    List<Type> loadAfterImpl(int position, Type item) {
        return mDataSource.loadAfter(item, mConfig.mPageSize);
    }
}
