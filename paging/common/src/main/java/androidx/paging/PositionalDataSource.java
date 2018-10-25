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
import androidx.annotation.WorkerThread;
import androidx.arch.core.util.Function;
import androidx.concurrent.futures.ResolvableFuture;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collections;
import java.util.List;

/**
 * Position-based data loader for a fixed-size, countable data set, supporting fixed-size loads at
 * arbitrary page positions.
 * <p>
 * Extend PositionalDataSource if you can load pages of a requested size at arbitrary
 * positions, and provide a fixed item count. If your data source can't support loading arbitrary
 * requested page sizes (e.g. when network page size constraints are only known at runtime), either
 * use {@link PageKeyedDataSource} or {@link ItemKeyedDataSource}, or pass the initial result with
 * the two parameter {@link LoadInitialCallback#onResult(List, int)}.
 * <p>
 * Room can generate a Factory of PositionalDataSources for you:
 * <pre>
 * {@literal @}Dao
 * interface UserDao {
 *     {@literal @}Query("SELECT * FROM user ORDER BY mAge DESC")
 *     public abstract DataSource.Factory&lt;Integer, User> loadUsersByAgeDesc();
 * }</pre>
 *
 * @see ListenablePositionalDataSource
 *
 * @param <T> Type of items being loaded by the PositionalDataSource.
 */
public abstract class PositionalDataSource<T> extends ListenablePositionalDataSource<T> {

    /**
     * Holder object for inputs to {@link #loadInitial(LoadInitialParams, LoadInitialCallback)}.
     */
    public static class LoadInitialParams extends ListenablePositionalDataSource.LoadInitialParams {
        public LoadInitialParams(
                int requestedStartPosition,
                int requestedLoadSize,
                int pageSize,
                boolean placeholdersEnabled) {
            super(requestedStartPosition, requestedLoadSize, pageSize, placeholdersEnabled);
        }
    }

    /**
     * Holder object for inputs to {@link #loadRange(LoadRangeParams, LoadRangeCallback)}.
     */
    public static class LoadRangeParams extends ListenablePositionalDataSource.LoadRangeParams {
        public LoadRangeParams(int startPosition, int loadSize) {
            super(startPosition, loadSize);
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

        /**
         * Called to report an error from a DataSource.
         * <p>
         * Call this method to report an error from
         * {@link #loadInitial(LoadInitialParams, LoadInitialCallback)}.
         *
         * @param error The error that occurred during loading.
         */
        public void onError(@NonNull Throwable error) {
            // TODO: remove default implementation in 3.0
            throw new IllegalStateException(
                    "You must implement onError if implementing your own load callback");
        }
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

        /**
         * Called to report an error from a DataSource.
         * <p>
         * Call this method to report an error from
         * {@link #loadRange(LoadRangeParams, LoadRangeCallback)}.
         *
         * @param error The error that occurred during loading.
         */
        public void onError(@NonNull Throwable error) {
            // TODO: remove default implementation in 3.0
            throw new IllegalStateException(
                    "You must implement onError if implementing your own load callback");
        }
    }

    @NonNull
    @Override
    public final ListenableFuture<InitialResult<T>> loadInitial(
            @NonNull final ListenablePositionalDataSource.LoadInitialParams params) {
        final ResolvableFuture<InitialResult<T>> future = ResolvableFuture.create();
        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                final LoadInitialParams newParams = new LoadInitialParams(
                        params.requestedStartPosition,
                        params.requestedLoadSize,
                        params.pageSize,
                        params.placeholdersEnabled);
                LoadInitialCallback<T> callback = new LoadInitialCallback<T>() {
                    @Override
                    public void onResult(@NonNull List<T> data, int position, int totalCount) {
                        if (isInvalid()) {
                            // NOTE: this isInvalid() check works around
                            // https://issuetracker.google.com/issues/124511903
                            future.set(new InitialResult<>(Collections.<T>emptyList(), 0, 0));
                        } else {
                            setFuture(newParams, new InitialResult<>(data, position, totalCount));
                        }
                    }

                    @Override
                    public void onResult(@NonNull List<T> data, int position) {
                        if (isInvalid()) {
                            // NOTE: this isInvalid() check works around
                            // https://issuetracker.google.com/issues/124511903
                            future.set(new InitialResult<>(Collections.<T>emptyList(), 0));
                        } else {
                            setFuture(newParams, new InitialResult<>(data, position));
                        }
                    }

                    private void setFuture(
                            @NonNull ListenablePositionalDataSource.LoadInitialParams params,
                            @NonNull InitialResult<T> result) {
                        if (params.placeholdersEnabled) {
                            result.validateForInitialTiling(params.pageSize);
                        }
                        future.set(result);
                    }


                    @Override
                    public void onError(@NonNull Throwable error) {
                        future.setException(error);
                    }
                };
                loadInitial(newParams,
                        callback);
            }
        });
        return future;
    }

    @NonNull
    @Override
    public final ListenableFuture<RangeResult<T>> loadRange(
            final @NonNull ListenablePositionalDataSource.LoadRangeParams params) {
        final ResolvableFuture<RangeResult<T>> future = ResolvableFuture.create();
        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                LoadRangeCallback<T> callback = new LoadRangeCallback<T>() {
                    @Override
                    public void onResult(@NonNull List<T> data) {
                        if (isInvalid()) {
                            future.set(new RangeResult<>(Collections.<T>emptyList()));
                        } else {
                            future.set(new RangeResult<>(data));
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable error) {
                        future.setException(error);
                    }
                };
                loadRange(new LoadRangeParams(
                                params.startPosition,
                                params.loadSize),
                        callback);
            }
        });
        return future;
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
     * {@link #loadInitial(LoadInitialParams, LoadInitialCallback)} when total data set size can be
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
     * @param params Params passed to {@link #loadInitial(LoadInitialParams, LoadInitialCallback)},
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
