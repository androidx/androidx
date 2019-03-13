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
 * Incremental data loader for page-keyed content, where requests return keys for next/previous
 * pages.
 * <p>
 * Implement a DataSource using PageKeyedDataSource if you need to use data from page {@code N - 1}
 * to load page {@code N}. This is common, for example, in network APIs that include a next/previous
 * link or key with each page load.
 * <p>
 * The {@code InMemoryByPageRepository} in the
 * <a href="https://github.com/googlesamples/android-architecture-components/blob/master/PagingWithNetworkSample/README.md">PagingWithNetworkSample</a>
 * shows how to implement a network PageKeyedDataSource using
 * <a href="https://square.github.io/retrofit/">Retrofit</a>, while
 * handling swipe-to-refresh, network errors, and retry.
 *
 * @param <Key> Type of data used to query Value types out of the DataSource.
 * @param <Value> Type of items being loaded by the DataSource.
 */
public abstract class PageKeyedDataSource<Key, Value>
        extends ListenablePageKeyedDataSource<Key, Value> {
    /**
     * Holder object for inputs to {@link #loadInitial(LoadInitialParams, LoadInitialCallback)}.
     *
     * @param <Key> Type of data used to query pages.
     */
    public static class LoadInitialParams<Key> extends
            ListenablePageKeyedDataSource.LoadInitialParams<Key> {
        public LoadInitialParams(int requestedLoadSize, boolean placeholdersEnabled) {
            super(requestedLoadSize, placeholdersEnabled);
        }
    }

    /**
     * Holder object for inputs to {@link #loadBefore(LoadParams, LoadCallback)} and
     * {@link #loadAfter(LoadParams, LoadCallback)}.
     *
     * @param <Key> Type of data used to query pages.
     */
    public static class LoadParams<Key> extends ListenablePageKeyedDataSource.LoadParams<Key> {
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
     * call the five parameter {@link #onResult(List, int, int, Key, Key)} to pass that
     * information. You can skip passing this information by calling the three parameter
     * {@link #onResult(List, Key, Key)}, either if it's difficult to compute, or if
     * {@link LoadInitialParams#placeholdersEnabled} is {@code false}, so the positioning
     * information will be ignored.
     * <p>
     * It is always valid for a DataSource loading method that takes a callback to stash the
     * callback and call it later. This enables DataSources to be fully asynchronous, and to handle
     * temporary, recoverable error states (such as a network error that can be retried).
     *
     * @param <Key> Type of data used to query pages.
     * @param <Value> Type of items being loaded.
     */
    public abstract static class LoadInitialCallback<Key, Value> {
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
        public abstract void onResult(@NonNull List<Value> data, int position, int totalCount,
                @Nullable Key previousPageKey, @Nullable Key nextPageKey);

        /**
         * Called to pass loaded data from a DataSource.
         * <p>
         * Call this from {@link #loadInitial(LoadInitialParams, LoadInitialCallback)} to
         * initialize without counting available data, or supporting placeholders.
         * <p>
         * It is always valid to pass a different amount of data than what is requested. Pass an
         * empty list if there is no more data to load.
         *
         * @param data List of items loaded from the PageKeyedDataSource.
         * @param previousPageKey Key for page before the initial load result, or {@code null} if no
         *                        more data can be loaded before.
         * @param nextPageKey Key for page after the initial load result, or {@code null} if no
         *                        more data can be loaded after.
         */
        public abstract void onResult(@NonNull List<Value> data, @Nullable Key previousPageKey,
                @Nullable Key nextPageKey);

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
     * Callback for PageKeyedDataSource {@link #loadBefore(LoadParams, LoadCallback)} and
     * {@link #loadAfter(LoadParams, LoadCallback)} to return data.
     * <p>
     * A callback can be called only once, and will throw if called again.
     * <p>
     * It is always valid for a DataSource loading method that takes a callback to stash the
     * callback and call it later. This enables DataSources to be fully asynchronous, and to handle
     * temporary, recoverable error states (such as a network error that can be retried).
     *
     * @param <Key> Type of data used to query pages.
     * @param <Value> Type of items being loaded.
     */
    public abstract static class LoadCallback<Key, Value> {
        /**
         * Called to pass loaded data from a DataSource.
         * <p>
         * Call this method from your PageKeyedDataSource's
         * {@link #loadBefore(LoadParams, LoadCallback)} and
         * {@link #loadAfter(LoadParams, LoadCallback)} methods to return data.
         * <p>
         * It is always valid to pass a different amount of data than what is requested. Pass an
         * empty list if there is no more data to load.
         * <p>
         * Pass the key for the subsequent page to load to adjacentPageKey. For example, if you've
         * loaded a page in {@link #loadBefore(LoadParams, LoadCallback)}, pass the key for the
         * previous page, or {@code null} if the loaded page is the first. If in
         * {@link #loadAfter(LoadParams, LoadCallback)}, pass the key for the next page, or
         * {@code null} if the loaded page is the last.
         *
         * @param data List of items loaded from the PageKeyedDataSource.
         * @param adjacentPageKey Key for subsequent page load (previous page in {@link #loadBefore}
         *                        / next page in {@link #loadAfter}), or {@code null} if there are
         *                        no more pages to load in the current load direction.
         */
        public abstract void onResult(@NonNull List<Value> data, @Nullable Key adjacentPageKey);

        /**
         * Called to report an error from a DataSource.
         * <p>
         * Call this method to report an error from your PageKeyedDataSource's
         * {@link #loadBefore(LoadParams, LoadCallback)} and
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
    public final ListenableFuture<InitialResult<Key, Value>> loadInitial(
            final @NonNull ListenablePageKeyedDataSource.LoadInitialParams<Key> params) {
        final ResolvableFuture<InitialResult<Key, Value>> future = ResolvableFuture.create();
        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                LoadInitialCallback<Key, Value> callback = new LoadInitialCallback<Key, Value>() {
                    @Override
                    public void onResult(@NonNull List<Value> data, int position, int totalCount,
                            @Nullable Key previousPageKey, @Nullable Key nextPageKey) {
                        future.set(new InitialResult<>(data, position, totalCount, previousPageKey,
                                nextPageKey));
                    }

                    @Override
                    public void onResult(@NonNull List<Value> data, @Nullable Key previousPageKey,
                            @Nullable Key nextPageKey) {
                        future.set(new InitialResult<>(data, previousPageKey, nextPageKey));
                    }

                    @Override
                    public void onError(@NonNull Throwable error) {
                        future.setException(error);
                    }
                };
                loadInitial(new LoadInitialParams<Key>(
                                params.requestedLoadSize,
                                params.placeholdersEnabled),
                        callback);
            }
        });
        return future;
    }

    @SuppressWarnings("WeakerAccess")
    LoadCallback<Key, Value> getFutureAsCallback(
            final @NonNull ResolvableFuture<Result<Key, Value>> future) {
        return new LoadCallback<Key, Value>() {
            @Override
            public void onResult(@NonNull List<Value> data, @Nullable Key adjacentPageKey) {
                future.set(new Result<>(data, adjacentPageKey));
            }

            @Override
            public void onError(@NonNull Throwable error) {
                future.setException(error);
            }
        };
    }

    @NonNull
    @Override
    public final ListenableFuture<Result<Key, Value>> loadBefore(
            final @NonNull ListenablePageKeyedDataSource.LoadParams<Key> params) {
        final ResolvableFuture<Result<Key, Value>> future = ResolvableFuture.create();
        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                loadBefore(new LoadParams<>(
                                params.key,
                                params.requestedLoadSize),
                        getFutureAsCallback(future));
            }
        });
        return future;
    }

    @NonNull
    @Override
    public final ListenableFuture<Result<Key, Value>> loadAfter(
            final @NonNull ListenablePageKeyedDataSource.LoadParams<Key> params) {
        final ResolvableFuture<Result<Key, Value>> future = ResolvableFuture.create();
        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                loadAfter(new LoadParams<>(
                        params.key,
                        params.requestedLoadSize), getFutureAsCallback(future));
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
     * {@link LoadInitialCallback#onResult(List, int, int, Key, Key)}. This enables PagedLists
     * presenting data from this source to display placeholders to represent unloaded items.
     * <p>
     * {@link LoadInitialParams#requestedLoadSize} is a hint, not a requirement, so it may be may be
     * altered or ignored.
     *
     * @param params Parameters for initial load, including requested load size.
     * @param callback Callback that receives initial load data.
     */
    public abstract void loadInitial(@NonNull LoadInitialParams<Key> params,
            @NonNull LoadInitialCallback<Key, Value> callback);

    /**
     * Prepend page with the key specified by {@link LoadParams#key LoadParams.key}.
     * <p>
     * It's valid to return a different list size than the page size if it's easier, e.g. if your
     * backend defines page sizes. It is generally preferred to increase the number loaded than
     * reduce.
     * <p>
     * Data may be passed synchronously during the load method, or deferred and called at a
     * later time. Further loads going down will be blocked until the callback is called.
     * <p>
     * If data cannot be loaded (for example, if the request is invalid, or the data would be stale
     * and inconsistent), it is valid to call {@link #invalidate()} to invalidate the data source,
     * and prevent further loading.
     *
     * @param params Parameters for the load, including the key for the new page, and requested load
     *               size.
     * @param callback Callback that receives loaded data.
     */
    public abstract void loadBefore(@NonNull LoadParams<Key> params,
            @NonNull LoadCallback<Key, Value> callback);

    /**
     * Append page with the key specified by {@link LoadParams#key LoadParams.key}.
     * <p>
     * It's valid to return a different list size than the page size if it's easier, e.g. if your
     * backend defines page sizes. It is generally preferred to increase the number loaded than
     * reduce.
     * <p>
     * Data may be passed synchronously during the load method, or deferred and called at a
     * later time. Further loads going down will be blocked until the callback is called.
     * <p>
     * If data cannot be loaded (for example, if the request is invalid, or the data would be stale
     * and inconsistent), it is valid to call {@link #invalidate()} to invalidate the data source,
     * and prevent further loading.
     *
     * @param params Parameters for the load, including the key for the new page, and requested load
     *               size.
     * @param callback Callback that receives loaded data.
     */
    public abstract void loadAfter(@NonNull LoadParams<Key> params,
            @NonNull LoadCallback<Key, Value> callback);

    @NonNull
    @Override
    public final <ToValue> PageKeyedDataSource<Key, ToValue> mapByPage(
            @NonNull Function<List<Value>, List<ToValue>> function) {
        return new WrapperPageKeyedDataSource<>(this, function);
    }

    @NonNull
    @Override
    public final <ToValue> PageKeyedDataSource<Key, ToValue> map(
            @NonNull Function<Value, ToValue> function) {
        return mapByPage(createListFunction(function));
    }
}
