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

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

/**
 * Position-based data loader for a fixed-size, countable data set, supporting fixed-size loads at
 * arbitrary page positions.
 * <p>
 * Extend ListenablePositionalDataSource if you can load pages of a requested size at arbitrary
 * positions, and provide a fixed item count. If your data source can't support loading arbitrary
 * requested page sizes (e.g. when network page size constraints are only known at runtime), either
 * use {@link PageKeyedDataSource} or {@link ItemKeyedDataSource}, or pass the initial result with
 *  the two parameter {@link InitialResult InitialResult constructor}.
 *
 * @see PositionalDataSource
 *
 * @param <T> Type of items being loaded by the PositionalDataSource.
 */
public abstract class ListenablePositionalDataSource<T> extends DataSource<Integer, T> {
    public ListenablePositionalDataSource() {
        super(KeyType.POSITIONAL);
    }

    @Override
    final ListenableFuture<? extends BaseResult<T>> load(@NonNull Params<Integer> params) {
        if (params.type == LoadType.INITIAL) {
            int initialPosition = 0;
            int initialLoadSize = params.initialLoadSize;
            if (params.key != null) {
                initialPosition = params.key;

                if (params.placeholdersEnabled) {
                    // snap load size to page multiple (minimum two)
                    initialLoadSize = Math.max(initialLoadSize / params.pageSize, 2)
                            * params.pageSize;

                    // move start so the load is centered around the key, not starting at it
                    final int idealStart = initialPosition - initialLoadSize / 2;
                    initialPosition = Math.max(0, idealStart / params.pageSize * params.pageSize);
                } else {
                    // not tiled, so don't try to snap or force multiple of a page size
                    initialPosition = initialPosition - initialLoadSize / 2;
                }

            }
            PositionalDataSource.LoadInitialParams initParams =
                    new PositionalDataSource.LoadInitialParams(
                            initialPosition,
                            initialLoadSize,
                            params.pageSize,
                            params.placeholdersEnabled);
            return loadInitial(initParams);
        } else {
            int startIndex = params.key;
            int loadSize = params.pageSize;
            if (params.type == LoadType.START) {
                loadSize = Math.min(loadSize, startIndex + 1);
                startIndex = startIndex - loadSize + 1;
            }
            return loadRange(new PositionalDataSource.LoadRangeParams(startIndex, loadSize));
        }
    }

    /**
     * Holder object for inputs to {@code loadInitial()}.
     */
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
         * Defines whether placeholders are enabled, and whether the loaded total count will be
         * ignored.
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
     * Holder object for inputs to {@code loadRange()}.
     */
    public static class LoadRangeParams {
        /**
         * START position of data to load.
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
     * Load initial list data.
     * <p>
     * This method is called to load the initial page(s) from the DataSource.
     * <p>
     * Result list must be a multiple of pageSize to enable efficient tiling.
     *
     * @param params Parameters for initial load, including requested start position, load size, and
     *               page size.
     * @return ListenableFuture of the loaded data.
     */
    @NonNull
    public abstract ListenableFuture<InitialResult<T>> loadInitial(
            @NonNull LoadInitialParams params);

    /**
     * Called to load a range of data from the DataSource.
     * <p>
     * This method is called to load additional pages from the DataSource after the
     * LoadInitialCallback passed to dispatchLoadInitial has initialized a PagedList.
     * <p>
     * Unlike {@link #loadInitial(LoadInitialParams)}, this method must return
     * the number of items requested, at the position requested.
     *
     * @param params Parameters for load, including start position and load size.
     * @return ListenableFuture of the loaded data.
     */
    @NonNull
    public abstract ListenableFuture<RangeResult<T>> loadRange(@NonNull LoadRangeParams params);

    @Nullable
    @Override
    final Integer getKey(@NonNull T item) {
        return null;
    }

    /**
     * Type produced by {@link #loadInitial(LoadInitialParams)} to represent
     * initially loaded data.
     *
     * @param <V> The type of the data loaded.
     */
    public static class InitialResult<V> extends BaseResult<V> {
        public InitialResult(@NonNull List<V> data, int position, int totalCount) {
            super(data, null, null, position, totalCount - data.size() - position, 0, true);
            if (data.isEmpty() && position != 0) {
                throw new IllegalArgumentException(
                        "Initial result cannot be empty if items are present in data set.");
            }
        }

        public InitialResult(@NonNull List<V> data, int position) {
            super(data, null, null, 0, 0, position, false);
            if (data.isEmpty() && position != 0) {
                throw new IllegalArgumentException(
                        "Initial result cannot be empty if items are present in data set.");
            }
        }
    }

    /**
     * Type produced by {@link #loadRange(LoadRangeParams)} to represent a page
     * of loaded data.
     *
     * @param <V> The type of the data loaded.
     */
    public static class RangeResult<V> extends BaseResult<V> {
        public RangeResult(@NonNull List<V> data) {
            super(data, null, null, 0, 0, 0, false);
        }
    }

    /**
     * Helper for computing an initial position in
     * {@link #loadInitial(LoadInitialParams)} when total data set size can be
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
     * @param params Params passed to {@link #loadInitial(LoadInitialParams)},
     *               including page size, and requested start/loadSize.
     * @param totalCount Total size of the data set.
     * @return Position to start loading at.
     *
     *
     * @see #computeInitialLoadSize(ListenablePositionalDataSource.LoadInitialParams, int, int)
     */
    public static int computeInitialLoadPosition(
            @NonNull ListenablePositionalDataSource.LoadInitialParams params,
            int totalCount) {
        int position = params.requestedStartPosition;
        int initialLoadSize = params.requestedLoadSize;
        int pageSize = params.pageSize;

        int pageStart = position / pageSize * pageSize;

        // maximum start pos is that which will encompass end of list
        int maximumLoadPage = ((totalCount - initialLoadSize + pageSize - 1) / pageSize) * pageSize;
        pageStart = Math.min(maximumLoadPage, pageStart);

        // minimum start position is 0
        pageStart = Math.max(0, pageStart);

        return pageStart;
    }

    /**
     * Helper for computing an initial load size in
     * {@link #loadInitial(LoadInitialParams)} when total data set size can be
     * computed ahead of loading.
     * <p>
     * This function takes the requested load size, and bounds checks it against the value returned
     * by
     * {@link #computeInitialLoadPosition(ListenablePositionalDataSource.LoadInitialParams, int)}.
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
     * @param params Params passed to {@link #loadInitial(LoadInitialParams)},
     *               including page size, and requested start/loadSize.
     * @param initialLoadPosition Value returned by
     *   {@link #computeInitialLoadPosition(ListenablePositionalDataSource.LoadInitialParams, int)}
     * @param totalCount Total size of the data set.
     * @return Number of items to load.
     *
     * @see #computeInitialLoadPosition(ListenablePositionalDataSource.LoadInitialParams, int)
     */
    public static int computeInitialLoadSize(@NonNull
            ListenablePositionalDataSource.LoadInitialParams params,
            int initialLoadPosition, int totalCount) {
        return Math.min(totalCount - initialLoadPosition, params.requestedLoadSize);
    }
}
