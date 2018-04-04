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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.arch.core.util.Function;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Position-based data loader for a fixed-size, countable data set, supporting fixed-size loads at
 * arbitrary page positions.
 * <p>
 * Extend PositionalDataSource if you can load pages of a requested size at arbitrary
 * positions, and provide a fixed item count. If your data source can't support loading arbitrary
 * requested page sizes (e.g. when network page size constraints are only known at runtime), use
 * either {@link PageKeyedDataSource} or {@link ItemKeyedDataSource} instead.
 * <p>
 * Note that unless {@link PagedList.Config#enablePlaceholders placeholders are disabled}
 * PositionalDataSource requires counting the size of the data set. This allows pages to be tiled in
 * at arbitrary, non-contiguous locations based upon what the user observes in a {@link PagedList}.
 * If placeholders are disabled, initialize with the two parameter
 * {@link LoadInitialCallback#onResult(List, int)}.
 * <p>
 * Room can generate a Factory of PositionalDataSources for you:
 * <pre>
 * {@literal @}Dao
 * interface UserDao {
 *     {@literal @}Query("SELECT * FROM user ORDER BY mAge DESC")
 *     public abstract DataSource.Factory&lt;Integer, User> loadUsersByAgeDesc();
 * }</pre>
 *
 * @param <T> Type of items being loaded by the PositionalDataSource.
 */
public abstract class PositionalDataSource<T> extends DataSource<Integer, T> {

    /**
     * Holder object for inputs to {@link #loadInitial(LoadInitialParams, LoadInitialCallback)}.
     */
    @SuppressWarnings("WeakerAccess")
    public static class LoadInitialParams {
        /**
         * Initial load position requested.
         * <p>
         * Note that this may not be within the bounds of your data set, it may need to be adjusted
         * before you execute your load.
         */
        public final int requestedStartPosition;

        /**
         * Requested number of items to load.
         * <p>
         * Note that this may be larger than available data.
         */
        public final int requestedLoadSize;

        /**
         * Defines page size acceptable for return values.
         * <p>
         * List of items passed to the callback must be an integer multiple of page size.
         */
        public final int pageSize;

        /**
         * Defines whether placeholders are enabled, and whether the total count passed to
         * {@link LoadInitialCallback#onResult(List, int, int)} will be ignored.
         */
        public final boolean placeholdersEnabled;

        public LoadInitialParams(
                int requestedStartPosition,
                int requestedLoadSize,
                int pageSize,
                boolean placeholdersEnabled) {
            this.requestedStartPosition = requestedStartPosition;
            this.requestedLoadSize = requestedLoadSize;
            this.pageSize = pageSize;
            this.placeholdersEnabled = placeholdersEnabled;
        }
    }

    /**
     * Holder object for inputs to {@link #loadRange(LoadRangeParams, LoadRangeCallback)}.
     */
    @SuppressWarnings("WeakerAccess")
    public static class LoadRangeParams {
        /**
         * Start position of data to load.
         * <p>
         * Returned data must start at this position.
         */
        public final int startPosition;
        /**
         * Number of items to load.
         * <p>
         * Returned data must be of this size, unless at end of the list.
         */
        public final int loadSize;

        public LoadRangeParams(int startPosition, int loadSize) {
            this.startPosition = startPosition;
            this.loadSize = loadSize;
        }
    }

    /**
     * Callback for {@link #loadInitial(LoadInitialParams, LoadInitialCallback)}
     * to return data, position, and count.
     * <p>
     * A callback should be called only once, and may throw if called again.
     * <p>
     * It is always valid for a DataSource loading method that takes a callback to stash the
     * callback and call it later. This enables DataSources to be fully asynchronous, and to handle
     * temporary, recoverable error states (such as a network error that can be retried).
     *
     * @param <T> Type of items being loaded.
     */
    public abstract static class LoadInitialCallback<T> {
        /**
         * Called to pass initial load state from a DataSource.
         * <p>
         * Call this method from your DataSource's {@code loadInitial} function to return data,
         * and inform how many placeholders should be shown before and after. If counting is cheap
         * to compute (for example, if a network load returns the information regardless), it's
         * recommended to pass the total size to the totalCount parameter. If placeholders are not
         * requested (when {@link LoadInitialParams#placeholdersEnabled} is false), you can instead
         * call {@link #onResult(List, int)}.
         *
         * @param data List of items loaded from the DataSource. If this is empty, the DataSource
         *             is treated as empty, and no further loads will occur.
         * @param position Position of the item at the front of the list. If there are {@code N}
         *                 items before the items in data that can be loaded from this DataSource,
         *                 pass {@code N}.
         * @param totalCount Total number of items that may be returned from this DataSource.
         *                   Includes the number in the initial {@code data} parameter
         *                   as well as any items that can be loaded in front or behind of
         *                   {@code data}.
         */
        public abstract void onResult(@NonNull List<T> data, int position, int totalCount);

        /**
         * Called to pass initial load state from a DataSource without total count,
         * when placeholders aren't requested.
         * <p class="note"><strong>Note:</strong> This method can only be called when placeholders
         * are disabled ({@link LoadInitialParams#placeholdersEnabled} is false).
         * <p>
         * Call this method from your DataSource's {@code loadInitial} function to return data,
         * if position is known but total size is not. If placeholders are requested, call the three
         * parameter variant: {@link #onResult(List, int, int)}.
         *
         * @param data List of items loaded from the DataSource. If this is empty, the DataSource
         *             is treated as empty, and no further loads will occur.
         * @param position Position of the item at the front of the list. If there are {@code N}
         *                 items before the items in data that can be provided by this DataSource,
         *                 pass {@code N}.
         */
        public abstract void onResult(@NonNull List<T> data, int position);
    }

    /**
     * Callback for PositionalDataSource {@link #loadRange(LoadRangeParams, LoadRangeCallback)}
     * to return data.
     * <p>
     * A callback should be called only once, and may throw if called again.
     * <p>
     * It is always valid for a DataSource loading method that takes a callback to stash the
     * callback and call it later. This enables DataSources to be fully asynchronous, and to handle
     * temporary, recoverable error states (such as a network error that can be retried).
     *
     * @param <T> Type of items being loaded.
     */
    public abstract static class LoadRangeCallback<T> {
        /**
         * Called to pass loaded data from {@link #loadRange(LoadRangeParams, LoadRangeCallback)}.
         *
         * @param data List of items loaded from the DataSource. Must be same size as requested,
         *             unless at end of list.
         */
        public abstract void onResult(@NonNull List<T> data);
    }

    static class LoadInitialCallbackImpl<T> extends LoadInitialCallback<T> {
        final LoadCallbackHelper<T> mCallbackHelper;
        private final boolean mCountingEnabled;
        private final int mPageSize;

        LoadInitialCallbackImpl(@NonNull PositionalDataSource dataSource, boolean countingEnabled,
                int pageSize, PageResult.Receiver<T> receiver) {
            mCallbackHelper = new LoadCallbackHelper<>(dataSource, PageResult.INIT, null, receiver);
            mCountingEnabled = countingEnabled;
            mPageSize = pageSize;
            if (mPageSize < 1) {
                throw new IllegalArgumentException("Page size must be non-negative");
            }
        }

        @Override
        public void onResult(@NonNull List<T> data, int position, int totalCount) {
            if (!mCallbackHelper.dispatchInvalidResultIfInvalid()) {
                LoadCallbackHelper.validateInitialLoadParams(data, position, totalCount);
                if (position + data.size() != totalCount
                        && data.size() % mPageSize != 0) {
                    throw new IllegalArgumentException("PositionalDataSource requires initial load"
                            + " size to be a multiple of page size to support internal tiling."
                            + " loadSize " + data.size() + ", position " + position
                            + ", totalCount " + totalCount + ", pageSize " + mPageSize);
                }

                if (mCountingEnabled) {
                    int trailingUnloadedCount = totalCount - position - data.size();
                    mCallbackHelper.dispatchResultToReceiver(
                            new PageResult<>(data, position, trailingUnloadedCount, 0));
                } else {
                    // Only occurs when wrapped as contiguous
                    mCallbackHelper.dispatchResultToReceiver(new PageResult<>(data, position));
                }
            }
        }

        @Override
        public void onResult(@NonNull List<T> data, int position) {
            if (!mCallbackHelper.dispatchInvalidResultIfInvalid()) {
                if (position < 0) {
                    throw new IllegalArgumentException("Position must be non-negative");
                }
                if (data.isEmpty() && position != 0) {
                    throw new IllegalArgumentException(
                            "Initial result cannot be empty if items are present in data set.");
                }
                if (mCountingEnabled) {
                    throw new IllegalStateException("Placeholders requested, but totalCount not"
                            + " provided. Please call the three-parameter onResult method, or"
                            + " disable placeholders in the PagedList.Config");
                }
                mCallbackHelper.dispatchResultToReceiver(new PageResult<>(data, position));
            }
        }
    }

    static class LoadRangeCallbackImpl<T> extends LoadRangeCallback<T> {
        private LoadCallbackHelper<T> mCallbackHelper;
        private final int mPositionOffset;
        LoadRangeCallbackImpl(@NonNull PositionalDataSource dataSource,
                @PageResult.ResultType int resultType, int positionOffset,
                Executor mainThreadExecutor, PageResult.Receiver<T> receiver) {
            mCallbackHelper = new LoadCallbackHelper<>(
                    dataSource, resultType, mainThreadExecutor, receiver);
            mPositionOffset = positionOffset;
        }

        @Override
        public void onResult(@NonNull List<T> data) {
            if (!mCallbackHelper.dispatchInvalidResultIfInvalid()) {
                mCallbackHelper.dispatchResultToReceiver(new PageResult<>(
                        data, 0, 0, mPositionOffset));
            }
        }
    }

    final void dispatchLoadInitial(boolean acceptCount,
            int requestedStartPosition, int requestedLoadSize, int pageSize,
            @NonNull Executor mainThreadExecutor, @NonNull PageResult.Receiver<T> receiver) {
        LoadInitialCallbackImpl<T> callback =
                new LoadInitialCallbackImpl<>(this, acceptCount, pageSize, receiver);

        LoadInitialParams params = new LoadInitialParams(
                requestedStartPosition, requestedLoadSize, pageSize, acceptCount);
        loadInitial(params, callback);

        // If initialLoad's callback is not called within the body, we force any following calls
        // to post to the UI thread. This constructor may be run on a background thread, but
        // after constructor, mutation must happen on UI thread.
        callback.mCallbackHelper.setPostExecutor(mainThreadExecutor);
    }

    final void dispatchLoadRange(@PageResult.ResultType int resultType, int startPosition,
            int count, @NonNull Executor mainThreadExecutor,
            @NonNull PageResult.Receiver<T> receiver) {
        LoadRangeCallback<T> callback = new LoadRangeCallbackImpl<>(
                this, resultType, startPosition, mainThreadExecutor, receiver);
        if (count == 0) {
            callback.onResult(Collections.<T>emptyList());
        } else {
            loadRange(new LoadRangeParams(startPosition, count), callback);
        }
    }

    /**
     * Load initial list data.
     * <p>
     * This method is called to load the initial page(s) from the DataSource.
     * <p>
     * Result list must be a multiple of pageSize to enable efficient tiling.
     *
     * @param params Parameters for initial load, including requested start position, load size, and
     *               page size.
     * @param callback Callback that receives initial load data, including
     *                 position and total data set size.
     */
    @WorkerThread
    public abstract void loadInitial(
            @NonNull LoadInitialParams params,
            @NonNull LoadInitialCallback<T> callback);

    /**
     * Called to load a range of data from the DataSource.
     * <p>
     * This method is called to load additional pages from the DataSource after the
     * LoadInitialCallback passed to dispatchLoadInitial has initialized a PagedList.
     * <p>
     * Unlike {@link #loadInitial(LoadInitialParams, LoadInitialCallback)}, this method must return
     * the number of items requested, at the position requested.
     *
     * @param params Parameters for load, including start position and load size.
     * @param callback Callback that receives loaded data.
     */
    @WorkerThread
    public abstract void loadRange(@NonNull LoadRangeParams params,
            @NonNull LoadRangeCallback<T> callback);

    @Override
    boolean isContiguous() {
        return false;
    }

    @NonNull
    ContiguousDataSource<Integer, T> wrapAsContiguousWithoutPlaceholders() {
        return new ContiguousWithoutPlaceholdersWrapper<>(this);
    }

    /**
     * Helper for computing an initial position in
     * {@link #loadInitial(LoadInitialParams, LoadInitialCallback)} when total data set size can be
     * computed ahead of loading.
     * <p>
     * The value computed by this function will do bounds checking, page alignment, and positioning
     * based on initial load size requested.
     * <p>
     * Example usage in a PositionalDataSource subclass:
     * <pre>
     * class ItemDataSource extends PositionalDataSource&lt;Item> {
     *     private int computeCount() {
     *         // actual count code here
     *     }
     *
     *     private List&lt;Item> loadRangeInternal(int startPosition, int loadCount) {
     *         // actual load code here
     *     }
     *
     *     {@literal @}Override
     *     public void loadInitial({@literal @}NonNull LoadInitialParams params,
     *             {@literal @}NonNull LoadInitialCallback&lt;Item> callback) {
     *         int totalCount = computeCount();
     *         int position = computeInitialLoadPosition(params, totalCount);
     *         int loadSize = computeInitialLoadSize(params, position, totalCount);
     *         callback.onResult(loadRangeInternal(position, loadSize), position, totalCount);
     *     }
     *
     *     {@literal @}Override
     *     public void loadRange({@literal @}NonNull LoadRangeParams params,
     *             {@literal @}NonNull LoadRangeCallback&lt;Item> callback) {
     *         callback.onResult(loadRangeInternal(params.startPosition, params.loadSize));
     *     }
     * }</pre>
     *
     * @param params Params passed to {@link #loadInitial(LoadInitialParams, LoadInitialCallback)},
     *               including page size, and requested start/loadSize.
     * @param totalCount Total size of the data set.
     * @return Position to start loading at.
     *
     * @see #computeInitialLoadSize(LoadInitialParams, int, int)
     */
    public static int computeInitialLoadPosition(@NonNull LoadInitialParams params,
            int totalCount) {
        int position = params.requestedStartPosition;
        int initialLoadSize = params.requestedLoadSize;
        int pageSize = params.pageSize;

        int roundedPageStart = Math.round(position / pageSize) * pageSize;

        // maximum start pos is that which will encompass end of list
        int maximumLoadPage = ((totalCount - initialLoadSize + pageSize - 1) / pageSize) * pageSize;
        roundedPageStart = Math.min(maximumLoadPage, roundedPageStart);

        // minimum start position is 0
        roundedPageStart = Math.max(0, roundedPageStart);

        return roundedPageStart;
    }

    /**
     * Helper for computing an initial load size in
     * {@link #loadInitial(LoadInitialParams, LoadInitialCallback)} when total data set size can be
     * computed ahead of loading.
     * <p>
     * This function takes the requested load size, and bounds checks it against the value returned
     * by {@link #computeInitialLoadPosition(LoadInitialParams, int)}.
     * <p>
     * Example usage in a PositionalDataSource subclass:
     * <pre>
     * class ItemDataSource extends PositionalDataSource&lt;Item> {
     *     private int computeCount() {
     *         // actual count code here
     *     }
     *
     *     private List&lt;Item> loadRangeInternal(int startPosition, int loadCount) {
     *         // actual load code here
     *     }
     *
     *     {@literal @}Override
     *     public void loadInitial({@literal @}NonNull LoadInitialParams params,
     *             {@literal @}NonNull LoadInitialCallback&lt;Item> callback) {
     *         int totalCount = computeCount();
     *         int position = computeInitialLoadPosition(params, totalCount);
     *         int loadSize = computeInitialLoadSize(params, position, totalCount);
     *         callback.onResult(loadRangeInternal(position, loadSize), position, totalCount);
     *     }
     *
     *     {@literal @}Override
     *     public void loadRange({@literal @}NonNull LoadRangeParams params,
     *             {@literal @}NonNull LoadRangeCallback&lt;Item> callback) {
     *         callback.onResult(loadRangeInternal(params.startPosition, params.loadSize));
     *     }
     * }</pre>
     *
     * @param params Params passed to {@link #loadInitial(LoadInitialParams, LoadInitialCallback)},
     *               including page size, and requested start/loadSize.
     * @param initialLoadPosition Value returned by
     *                          {@link #computeInitialLoadPosition(LoadInitialParams, int)}
     * @param totalCount Total size of the data set.
     * @return Number of items to load.
     *
     * @see #computeInitialLoadPosition(LoadInitialParams, int)
     */
    @SuppressWarnings("WeakerAccess")
    public static int computeInitialLoadSize(@NonNull LoadInitialParams params,
            int initialLoadPosition, int totalCount) {
        return Math.min(totalCount - initialLoadPosition, params.requestedLoadSize);
    }

    @SuppressWarnings("deprecation")
    static class ContiguousWithoutPlaceholdersWrapper<Value>
            extends ContiguousDataSource<Integer, Value> {
        @NonNull
        final PositionalDataSource<Value> mSource;

        ContiguousWithoutPlaceholdersWrapper(
                @NonNull PositionalDataSource<Value> source) {
            mSource = source;
        }

        @Override
        public void addInvalidatedCallback(
                @NonNull InvalidatedCallback onInvalidatedCallback) {
            mSource.addInvalidatedCallback(onInvalidatedCallback);
        }

        @Override
        public void removeInvalidatedCallback(
                @NonNull InvalidatedCallback onInvalidatedCallback) {
            mSource.removeInvalidatedCallback(onInvalidatedCallback);
        }

        @Override
        public void invalidate() {
            mSource.invalidate();
        }

        @Override
        public boolean isInvalid() {
            return mSource.isInvalid();
        }

        @NonNull
        @Override
        public <ToValue> DataSource<Integer, ToValue> mapByPage(
                @NonNull Function<List<Value>, List<ToValue>> function) {
            throw new UnsupportedOperationException(
                    "Inaccessible inner type doesn't support map op");
        }

        @NonNull
        @Override
        public <ToValue> DataSource<Integer, ToValue> map(
                @NonNull Function<Value, ToValue> function) {
            throw new UnsupportedOperationException(
                    "Inaccessible inner type doesn't support map op");
        }

        @Override
        void dispatchLoadInitial(@Nullable Integer position, int initialLoadSize, int pageSize,
                boolean enablePlaceholders, @NonNull Executor mainThreadExecutor,
                @NonNull PageResult.Receiver<Value> receiver) {
            final int convertPosition = position == null ? 0 : position;

            // Note enablePlaceholders will be false here, but we don't have a way to communicate
            // this to PositionalDataSource. This is fine, because only the list and its position
            // offset will be consumed by the LoadInitialCallback.
            mSource.dispatchLoadInitial(false, convertPosition, initialLoadSize,
                    pageSize, mainThreadExecutor, receiver);
        }

        @Override
        void dispatchLoadAfter(int currentEndIndex, @NonNull Value currentEndItem, int pageSize,
                @NonNull Executor mainThreadExecutor,
                @NonNull PageResult.Receiver<Value> receiver) {
            int startIndex = currentEndIndex + 1;
            mSource.dispatchLoadRange(
                    PageResult.APPEND, startIndex, pageSize, mainThreadExecutor, receiver);
        }

        @Override
        void dispatchLoadBefore(int currentBeginIndex, @NonNull Value currentBeginItem,
                int pageSize, @NonNull Executor mainThreadExecutor,
                @NonNull PageResult.Receiver<Value> receiver) {

            int startIndex = currentBeginIndex - 1;
            if (startIndex < 0) {
                // trigger empty list load
                mSource.dispatchLoadRange(
                        PageResult.PREPEND, startIndex, 0, mainThreadExecutor, receiver);
            } else {
                int loadSize = Math.min(pageSize, startIndex + 1);
                startIndex = startIndex - loadSize + 1;
                mSource.dispatchLoadRange(
                        PageResult.PREPEND, startIndex, loadSize, mainThreadExecutor, receiver);
            }
        }

        @Override
        Integer getKey(int position, Value item) {
            return position;
        }

    }

    @NonNull
    @Override
    public final <V> PositionalDataSource<V> mapByPage(
            @NonNull Function<List<T>, List<V>> function) {
        return new WrapperPositionalDataSource<>(this, function);
    }

    @NonNull
    @Override
    public final <V> PositionalDataSource<V> map(@NonNull Function<T, V> function) {
        return mapByPage(createListFunction(function));
    }
}
