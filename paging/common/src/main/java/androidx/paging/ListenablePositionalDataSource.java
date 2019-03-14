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
}
