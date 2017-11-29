/*
 * Copyright 2017 The Android Open Source Project
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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Position-based data loader for a fixed-size, countable data set, supporting loads at arbitrary
 * positions.
 * <p>
 * Extend PositionalDataSource if you can support counting your data set, and loading based on
 * position information.
 * <p>
 * Note that unless {@link PagedList.Config#enablePlaceholders placeholders are disabled}
 * PositionalDataSource requires counting the size of the dataset. This allows pages to be tiled in
 * at arbitrary, non-contiguous locations based upon what the user observes in a {@link PagedList}.
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
     * Callback for PositionalDataSource initial loading methods to return data, position, and
     * (optionally) count information.
     * <p>
     * A callback can be called only once, and will throw if called again.
     * <p>
     * It is always valid for a DataSource loading method that takes a callback to stash the
     * callback and call it later. This enables DataSources to be fully asynchronous, and to handle
     * temporary, recoverable error states (such as a network error that can be retried).
     *
     * @param <T> Type of items being loaded.
     */
    public static class InitialLoadCallback<T> extends BaseLoadCallback<T> {
        private final boolean mCountingEnabled;
        private final int mPageSize;

        InitialLoadCallback(@NonNull PositionalDataSource dataSource, boolean countingEnabled,
                int pageSize, PageResult.Receiver<T> receiver) {
            super(PageResult.INIT, dataSource, null, receiver);
            mCountingEnabled = countingEnabled;
            mPageSize = pageSize;
            if (mPageSize < 1) {
                throw new IllegalArgumentException("Page size must be non-negative");
            }
        }

        /**
         * Called to pass initial load state from a DataSource.
         * <p>
         * Call this method from your DataSource's {@code loadInitial} function to return data,
         * and inform how many placeholders should be shown before and after. If counting is cheap
         * to compute (for example, if a network load returns the information regardless), it's
         * recommended to pass data back through this method.
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
        public void onResult(@NonNull List<T> data, int position, int totalCount) {
            validateInitialLoadParams(data, position, totalCount);
            if (position + data.size() != totalCount
                    && data.size() % mPageSize != 0) {
                throw new IllegalArgumentException("PositionalDataSource requires initial load size"
                        + " to be a multiple of page size to support internal tiling.");
            }

            if (mCountingEnabled) {
                int trailingUnloadedCount = totalCount - position - data.size();
                dispatchResultToReceiver(
                        new PageResult<>(data, position, trailingUnloadedCount, 0));
            } else {
                // Only occurs when wrapped as contiguous
                dispatchResultToReceiver(new PageResult<>(data, position));
            }
        }

        /**
         * Called to pass initial load state from a DataSource without supporting placeholders.
         * <p>
         * Call this method from your DataSource's {@code loadInitial} function to return data,
         * if position is known but total size is not. If counting is not expensive, consider
         * calling the three parameter variant: {@link #onResult(List, int, int)}.
         *
         * @param data List of items loaded from the DataSource. If this is empty, the DataSource
         *             is treated as empty, and no further loads will occur.
         * @param position Position of the item at the front of the list. If there are {@code N}
         *                 items before the items in data that can be provided by this DataSource,
         *                 pass {@code N}.
         */
        void onResult(@NonNull List<T> data, int position) {
            // not counting, don't need to check mAcceptCount
            dispatchResultToReceiver(new PageResult<>(
                    data, 0, 0, position));
        }
    }

    /**
     * Callback for PositionalDataSource {@link #loadRange(int, int, LoadCallback)} methods
     * to return data.
     * <p>
     * A callback can be called only once, and will throw if called again.
     * <p>
     * It is always valid for a DataSource loading method that takes a callback to stash the
     * callback and call it later. This enables DataSources to be fully asynchronous, and to handle
     * temporary, recoverable error states (such as a network error that can be retried).
     *
     * @param <T> Type of items being loaded.
     */
    public static class LoadCallback<T> extends BaseLoadCallback<T> {
        private final int mPositionOffset;
        LoadCallback(@NonNull PositionalDataSource dataSource, int positionOffset,
                Executor mainThreadExecutor, PageResult.Receiver<T> receiver) {
            super(PageResult.TILE, dataSource, mainThreadExecutor, receiver);
            mPositionOffset = positionOffset;
        }

        /**
         * Called to pass loaded data from a DataSource.
         * <p>
         * Call this method from your DataSource's {@code load} methods to return data.
         *
         * @param data List of items loaded from the DataSource.
         */
        public void onResult(@NonNull List<T> data) {
            dispatchResultToReceiver(new PageResult<>(
                    data, 0, 0, mPositionOffset));
        }
    }

    void loadInitial(boolean acceptCount,
            int requestedStartPosition, int requestedLoadSize, int pageSize,
            @NonNull Executor mainThreadExecutor, @NonNull PageResult.Receiver<T> receiver) {
        InitialLoadCallback<T> callback =
                new InitialLoadCallback<>(this, acceptCount, pageSize, receiver);
        loadInitial(requestedStartPosition, requestedLoadSize, pageSize, callback);

        // If initialLoad's callback is not called within the body, we force any following calls
        // to post to the UI thread. This constructor may be run on a background thread, but
        // after constructor, mutation must happen on UI thread.
        callback.setPostExecutor(mainThreadExecutor);
    }

    void loadRange(int startPosition, int count,
            @NonNull Executor mainThreadExecutor, @NonNull PageResult.Receiver<T> receiver) {
        LoadCallback<T> callback =
                new LoadCallback<>(this, startPosition, mainThreadExecutor, receiver);
        if (count == 0) {
            callback.onResult(Collections.<T>emptyList());
        } else {
            loadRange(startPosition, count, callback);
        }
    }

    /**
     * Load initial list data.
     * <p>
     * This method is called to load the initial page(s) from the DataSource.
     * <p>
     * Result list must be a multiple of pageSize to enable efficient tiling.
     *
     * @param requestedStartPosition Initial load position requested. Note that this may not be
     *                               within the bounds of your data set, it should be corrected
     *                               before you make your query.
     * @param requestedLoadSize Requested number of items to load. Note that this may be larger than
     *                          available data.
     * @param pageSize Defines page size acceptable for return values. List of items passed to the
     *                 callback must be an integer multiple of page size.
     * @param callback DataSource.InitialLoadCallback that receives initial load data, including
     *                 position and total data set size.
     */
    @WorkerThread
    public abstract void loadInitial(int requestedStartPosition, int requestedLoadSize,
            int pageSize, @NonNull InitialLoadCallback<T> callback);

    /**
     * Called to load a range of data from the DataSource.
     * <p>
     * This method is called to load additional pages from the DataSource after the
     * InitialLoadCallback passed to loadInitial has initialized a PagedList.
     * <p>
     * Unlike {@link #loadInitial(int, int, int, InitialLoadCallback)}, this method must return the
     * number of items requested, at the position requested.
     *
     * @param startPosition Initial load position.
     * @param count Number of items to load.
     * @param callback DataSource.LoadCallback that receives loaded data.
     */
    @WorkerThread
    public abstract void loadRange(int startPosition, int count, @NonNull LoadCallback<T> callback);

    @Override
    boolean isContiguous() {
        return false;
    }


    @NonNull
    ContiguousDataSource<Integer, T> wrapAsContiguousWithoutPlaceholders() {
        return new ContiguousWithoutPlaceholdersWrapper<>(this);
    }

    static int computeFirstLoadPosition(int position, int firstLoadSize, int pageSize, int size) {
        int roundedPageStart = Math.round(position / pageSize) * pageSize;

        // maximum start pos is that which will encompass end of list
        int maximumLoadPage = ((size - firstLoadSize + pageSize - 1) / pageSize) * pageSize;
        roundedPageStart = Math.min(maximumLoadPage, roundedPageStart);

        // minimum start position is 0
        roundedPageStart = Math.max(0, roundedPageStart);

        return roundedPageStart;
    }

    @SuppressWarnings("deprecation")
    static class ContiguousWithoutPlaceholdersWrapper<Value>
            extends ContiguousDataSource<Integer, Value> {

        @NonNull
        final PositionalDataSource<Value> mPositionalDataSource;

        ContiguousWithoutPlaceholdersWrapper(
                @NonNull PositionalDataSource<Value> positionalDataSource) {
            mPositionalDataSource = positionalDataSource;
        }

        @Override
        void loadInitial(@Nullable Integer position, int initialLoadSize, int pageSize,
                boolean enablePlaceholders, @NonNull Executor mainThreadExecutor,
                @NonNull PageResult.Receiver<Value> receiver) {
            final int convertPosition = position == null ? 0 : position;

            // Note enablePlaceholders will be false here, but we don't have a way to communicate
            // this to PositionalDataSource. This is fine, because only the list and its position
            // offset will be consumed by the InitialLoadCallback.
            mPositionalDataSource.loadInitial(false, convertPosition, initialLoadSize,
                    pageSize, mainThreadExecutor, receiver);
        }

        @Override
        void loadAfter(int currentEndIndex, @NonNull Value currentEndItem, int pageSize,
                @NonNull Executor mainThreadExecutor,
                @NonNull PageResult.Receiver<Value> receiver) {
            int startIndex = currentEndIndex + 1;
            mPositionalDataSource.loadRange(startIndex, pageSize, mainThreadExecutor, receiver);
        }

        @Override
        void loadBefore(int currentBeginIndex, @NonNull Value currentBeginItem, int pageSize,
                @NonNull Executor mainThreadExecutor,
                @NonNull PageResult.Receiver<Value> receiver) {

            int startIndex = currentBeginIndex - 1;
            if (startIndex < 0) {
                // trigger empty list load
                mPositionalDataSource.loadRange(startIndex, 0, mainThreadExecutor, receiver);
            } else {
                int loadSize = Math.min(pageSize, startIndex + 1);
                startIndex = startIndex - loadSize + 1;
                mPositionalDataSource.loadRange(startIndex, loadSize, mainThreadExecutor, receiver);
            }
        }

        @Override
        Integer getKey(int position, Value item) {
            return position;
        }
    }
}
