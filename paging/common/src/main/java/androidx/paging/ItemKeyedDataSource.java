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
import androidx.arch.core.util.Function;
import androidx.concurrent.futures.ResolvableFuture;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

/**
 * Incremental data loader for paging keyed content, where loaded content uses previously loaded
 * items as input to future loads.
 * <p>
 * Implement a DataSource using ItemKeyedDataSource if you need to use data from item {@code N - 1}
 * to load item {@code N}. This is common, for example, in uniquely sorted database queries where
 * attributes of the item such just before the next query define how to execute it.
 * <p>
 * The {@code InMemoryByItemRepository} in the
 * <a href="https://github.com/googlesamples/android-architecture-components/blob/master/PagingWithNetworkSample/README.md">PagingWithNetworkSample</a>
 * shows how to implement a network ItemKeyedDataSource using
 * <a href="https://square.github.io/retrofit/">Retrofit</a>, while
 * handling swipe-to-refresh, network errors, and retry.
 *
 * @see ListenableItemKeyedDataSource
 *
 * @param <Key> Type of data used to query Value types out of the DataSource.
 * @param <Value> Type of items being loaded by the DataSource.
 */
public abstract class ItemKeyedDataSource<Key, Value> extends
        ListenableItemKeyedDataSource<Key, Value> {

    /**
     * Holder object for inputs to {@link #loadInitial(LoadInitialParams, LoadInitialCallback)}.
     *
     * @param <Key> Type of data used to query Value types out of the DataSource.
     */
    @SuppressWarnings("WeakerAccess")
    public static class LoadInitialParams<Key> extends
            ListenableItemKeyedDataSource.LoadInitialParams<Key> {
        public LoadInitialParams(@Nullable Key requestedInitialKey, int requestedLoadSize,
                boolean placeholdersEnabled) {
            super(requestedInitialKey, requestedLoadSize, placeholdersEnabled);
        }
    }

    /**
     * Holder object for inputs to {@link #loadBefore(LoadParams, LoadCallback)}
     * and {@link #loadAfter(LoadParams, LoadCallback)}.
     *
     * @param <Key> Type of data used to query Value types out of the DataSource.
     */
    @SuppressWarnings("WeakerAccess")
    public static class LoadParams<Key> extends ListenableItemKeyedDataSource.LoadParams<Key> {
        public LoadParams(@NonNull Key key, int requestedLoadSize) {
            super(key, requestedLoadSize);
        }
    }

    /**
     * Callback for {@link #loadInitial(LoadInitialParams, LoadInitialCallback)}
     * to return data and, optionally, position/count information.
     * <p>
     * A callback can be called only once, and will throw if called again.
     * <p>
     * If you can compute the number of items in the data set before and after the loaded range,
     * call the three parameter {@link #onResult(List, int, int)} to pass that information. You
     * can skip passing this information by calling the single parameter {@link #onResult(List)},
     * either if it's difficult to compute, or if {@link LoadInitialParams#placeholdersEnabled} is
     * {@code false}, so the positioning information will be ignored.
     * <p>
     * It is always valid for a DataSource loading method that takes a callback to stash the
     * callback and call it later. This enables DataSources to be fully asynchronous, and to handle
     * temporary, recoverable error states (such as a network error that can be retried).
     *
     * @param <Value> Type of items being loaded.
     */
    public abstract static class LoadInitialCallback<Value> extends LoadCallback<Value> {
        /**
         * Called to pass initial load state from a DataSource.
         * <p>
         * Call this method from your DataSource's {@code loadInitial} function to return data,
         * and inform how many placeholders should be shown before and after. If counting is cheap
         * to compute (for example, if a network load returns the information regardless), it's
         * recommended to pass data back through this method.
         * <p>
         * It is always valid to pass a different amount of data than what is requested. Pass an
         * empty list if there is no more data to load.
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
        public abstract void onResult(@NonNull List<Value> data, int position, int totalCount);
    }

    /**
     * Callback for ItemKeyedDataSource {@link #loadBefore(LoadParams, LoadCallback)}
     * and {@link #loadAfter(LoadParams, LoadCallback)} to return data.
     * <p>
     * A callback can be called only once, and will throw if called again.
     * <p>
     * It is always valid for a DataSource loading method that takes a callback to stash the
     * callback and call it later. This enables DataSources to be fully asynchronous, and to handle
     * temporary, recoverable error states (such as a network error that can be retried).
     *
     * @param <Value> Type of items being loaded.
     */
    public abstract static class LoadCallback<Value> {
        /**
         * Called to pass loaded data from a DataSource.
         * <p>
         * Call this method from your ItemKeyedDataSource's
         * {@link #loadBefore(LoadParams, LoadCallback)} and
         * {@link #loadAfter(LoadParams, LoadCallback)} methods to return data.
         * <p>
         * Call this from {@link #loadInitial(LoadInitialParams, LoadInitialCallback)} to
         * initialize without counting available data, or supporting placeholders.
         * <p>
         * It is always valid to pass a different amount of data than what is requested. Pass an
         * empty list if there is no more data to load.
         *
         * @param data List of items loaded from the ItemKeyedDataSource.
         */
        public abstract void onResult(@NonNull List<Value> data);

        /**
         * Called to report an error from a DataSource.
         * <p>
         * Call this method to report an error from
         * {@link #loadInitial(LoadInitialParams, LoadInitialCallback)},
         * {@link #loadBefore(LoadParams, LoadCallback)}, or
         * {@link #loadAfter(LoadParams, LoadCallback)} methods.
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
    public final ListenableFuture<InitialResult<Value>> loadInitial(
            final @NonNull ListenableItemKeyedDataSource.LoadInitialParams<Key> params) {
        final ResolvableFuture<InitialResult<Value>> future = ResolvableFuture.create();
        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                LoadInitialCallback<Value> callback = new LoadInitialCallback<Value>() {
                    @Override
                    public void onResult(@NonNull List<Value> data, int position, int totalCount) {
                        future.set(new InitialResult<>(data, position, totalCount));
                    }

                    @Override
                    public void onResult(@NonNull List<Value> data) {
                        future.set(new InitialResult<>(data));
                    }

                    @Override
                    public void onError(@NonNull Throwable error) {
                        future.setException(error);
                    }
                };
                loadInitial(new LoadInitialParams<>(
                                params.requestedInitialKey,
                                params.requestedLoadSize,
                                params.placeholdersEnabled),
                        callback);
            }
        });
        return future;
    }

    @SuppressWarnings("WeakerAccess")
    LoadCallback<Value> getFutureAsCallback(
            final @NonNull ResolvableFuture<Result<Value>> future) {
        return new LoadCallback<Value>() {
            @Override
            public void onResult(@NonNull List<Value> data) {
                future.set(new Result<>(data));
            }

            @Override
            public void onError(@NonNull Throwable error) {
                future.setException(error);
            }
        };
    }

    @NonNull
    @Override
    public final ListenableFuture<Result<Value>> loadBefore(
            final @NonNull ListenableItemKeyedDataSource.LoadParams<Key> params) {
        final ResolvableFuture<Result<Value>> future = ResolvableFuture.create();
        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                loadBefore(new LoadParams<>(params.key, params.requestedLoadSize),
                        getFutureAsCallback(future));
            }
        });
        return future;
    }

    @NonNull
    @Override
    public final ListenableFuture<Result<Value>> loadAfter(
            final @NonNull ListenableItemKeyedDataSource.LoadParams<Key> params) {
        final ResolvableFuture<Result<Value>> future = ResolvableFuture.create();
        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                loadAfter(new LoadParams<>(params.key, params.requestedLoadSize),
                        getFutureAsCallback(future));
            }
        });
        return future;
    }

    /**
     * Load initial data.
     * <p>
     * This method is called first to initialize a PagedList with data. If it's possible to count
     * the items that can be loaded by the DataSource, it's recommended to pass the loaded data to
     * the callback via the three-parameter
     * {@link LoadInitialCallback#onResult(List, int, int)}. This enables PagedLists
     * presenting data from this source to display placeholders to represent unloaded items.
     * <p>
     * {@link LoadInitialParams#requestedInitialKey} and {@link LoadInitialParams#requestedLoadSize}
     * are hints, not requirements, so they may be altered or ignored. Note that ignoring the
     * {@code requestedInitialKey} can prevent subsequent PagedList/DataSource pairs from
     * initializing at the same location. If your DataSource never invalidates (for example,
     * loading from the network without the network ever signalling that old data must be reloaded),
     * it's fine to ignore the {@code initialLoadKey} and always start from the beginning of the
     * data set.
     *
     * @param params Parameters for initial load, including initial key and requested size.
     * @param callback Callback that receives initial load data.
     */
    public abstract void loadInitial(
            @NonNull LoadInitialParams<Key> params,
            @NonNull LoadInitialCallback<Value> callback);

    /**
     * Load list data after the key specified in {@link LoadParams#key LoadParams.key}.
     * <p>
     * It's valid to return a different list size than the page size if it's easier, e.g. if your
     * backend defines page sizes. It is generally preferred to increase the number loaded than
     * reduce.
     * <p>
     * Data may be passed synchronously during the loadAfter method, or deferred and called at a
     * later time. Further loads going down will be blocked until the callback is called.
     * <p>
     * If data cannot be loaded (for example, if the request is invalid, or the data would be stale
     * and inconsistent), it is valid to call {@link #invalidate()} to invalidate the data source,
     * and prevent further loading.
     *
     * @param params Parameters for the load, including the key to load after, and requested size.
     * @param callback Callback that receives loaded data.
     */
    public abstract void loadAfter(@NonNull LoadParams<Key> params,
            @NonNull LoadCallback<Value> callback);

    /**
     * Load list data before the key specified in {@link LoadParams#key LoadParams.key}.
     * <p>
     * It's valid to return a different list size than the page size if it's easier, e.g. if your
     * backend defines page sizes. It is generally preferred to increase the number loaded than
     * reduce.
     * <p>
     * <p class="note"><strong>Note:</strong> Data returned will be prepended just before the key
     * passed, so if you vary size, ensure that the last item is adjacent to the passed key.
     * <p>
     * Data may be passed synchronously during the loadBefore method, or deferred and called at a
     * later time. Further loads going up will be blocked until the callback is called.
     * <p>
     * If data cannot be loaded (for example, if the request is invalid, or the data would be stale
     * and inconsistent), it is valid to call {@link #invalidate()} to invalidate the data source,
     * and prevent further loading.
     *
     * @param params Parameters for the load, including the key to load before, and requested size.
     * @param callback Callback that receives loaded data.
     */
    public abstract void loadBefore(@NonNull LoadParams<Key> params,
            @NonNull LoadCallback<Value> callback);

    /**
     * Return a key associated with the given item.
     * <p>
     * If your ItemKeyedDataSource is loading from a source that is sorted and loaded by a unique
     * integer ID, you would return {@code item.getID()} here. This key can then be passed to
     * {@link #loadBefore(LoadParams, LoadCallback)} or
     * {@link #loadAfter(LoadParams, LoadCallback)} to load additional items adjacent to the item
     * passed to this function.
     * <p>
     * If your key is more complex, such as when you're sorting by name, then resolving collisions
     * with integer ID, you'll need to return both. In such a case you would use a wrapper class,
     * such as {@code Pair<String, Integer>} or, in Kotlin,
     * {@code data class Key(val name: String, val id: Int)}
     *
     * @param item Item to get the key from.
     * @return Key associated with given item.
     */
    @NonNull
    @Override
    public abstract Key getKey(@NonNull Value item);

    @NonNull
    @Override
    public final <ToValue> ItemKeyedDataSource<Key, ToValue> mapByPage(
            @NonNull Function<List<Value>, List<ToValue>> function) {
        return new WrapperItemKeyedDataSource<>(this, function);
    }

    @NonNull
    @Override
    public final <ToValue> ItemKeyedDataSource<Key, ToValue> map(
            @NonNull Function<Value, ToValue> function) {
        return mapByPage(createListFunction(function));
    }
}
