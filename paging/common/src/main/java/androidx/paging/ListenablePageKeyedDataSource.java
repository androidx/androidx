/*
 * Copyright 2019 The Android Open Source Project
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
 *
 * @param <Key> Type of data used to query Value types out of the DataSource.
 * @param <Value> Type of items being loaded by the DataSource.
 */
public abstract class ListenablePageKeyedDataSource<Key, Value> extends DataSource<Key, Value> {
    public ListenablePageKeyedDataSource() {
        super(KeyType.PAGE_KEYED);
    }

    @Override
    final ListenableFuture<? extends BaseResult<Value>> load(
            @NonNull Params<Key> params) {
        if (params.type == LoadType.INITIAL) {
            PageKeyedDataSource.LoadInitialParams<Key> initParams =
                    new PageKeyedDataSource.LoadInitialParams<>(
                            params.initialLoadSize, params.placeholdersEnabled);
            return loadInitial(initParams);
        } else {
            if (params.key == null) {
                // null key, immediately return empty data
                ResolvableFuture<BaseResult<Value>> future = ResolvableFuture.create();
                future.set(BaseResult.<Value>empty());
                return future;
            }

            PageKeyedDataSource.LoadParams<Key> loadParams =
                    new PageKeyedDataSource.LoadParams<>(params.key, params.pageSize);

            if (params.type == LoadType.START) {
                return loadBefore(loadParams);
            } else if (params.type == LoadType.END) {
                return loadAfter(loadParams);
            }
        }
        throw new IllegalArgumentException("Unsupported type " + params.type.toString());
    }

    /**
     * Holder object for inputs to {@code loadInitial()}.
     *
     * @param <Key> Type of data used to query pages.
     */
    @SuppressWarnings("unused")
    public static class LoadInitialParams<Key> {
        /**
         * Requested number of items to load.
         * <p>
         * Note that this may be larger than available data.
         */
        public final int requestedLoadSize;

        /**
         * Defines whether placeholders are enabled, and whether the loaded total count will be
         * ignored.
         */
        public final boolean placeholdersEnabled;


        public LoadInitialParams(int requestedLoadSize, boolean placeholdersEnabled) {
            this.requestedLoadSize = requestedLoadSize;
            this.placeholdersEnabled = placeholdersEnabled;
        }
    }

    /**
     * Holder object for inputs to {@code loadBefore()} and {@code loadAfter()}.
     *
     * @param <Key> Type of data used to query pages.
     */
    public static class LoadParams<Key> {
        /**
         * Load items before/after this key.
         * <p>
         * Returned data must begin directly adjacent to this position.
         */
        @NonNull
        public final Key key;

        /**
         * Requested number of items to load.
         * <p>
         * Returned page can be of this size, but it may be altered if that is easier, e.g. a
         * network data source where the backend defines page size.
         */
        public final int requestedLoadSize;

        public LoadParams(@NonNull Key key, int requestedLoadSize) {
            this.key = key;
            this.requestedLoadSize = requestedLoadSize;
        }
    }

    /**
     * Load initial data.
     * <p>
     * This method is called first to initialize a PagedList with data. If it's possible to count
     * the items that can be loaded by the DataSource, it's recommended to pass the position and
     * count to the
     * {@link InitialResult InitialResult constructor}. This
     * enables PagedLists presenting data from this source to display placeholders to represent
     * unloaded items.
     * <p>
     * {@link LoadInitialParams#requestedLoadSize} is a hint, not a requirement,
     * so it may be may be altered or ignored.
     *
     * @param params Parameters for initial load, including requested load size.
     * @return ListenableFuture of the loaded data.
     */
    @NonNull
    public abstract ListenableFuture<InitialResult<Key, Value>> loadInitial(
            @NonNull LoadInitialParams<Key> params);

    /**
     * Prepend page with the key specified by
     * {@link PageKeyedDataSource.LoadParams#key LoadParams.key}.
     * <p>
     * It's valid to return a different list size than the page size if it's easier, e.g. if your
     * backend defines page sizes. It is generally preferred to increase the number loaded than
     * reduce.
     * <p>
     * If data cannot be loaded (for example, if the request is invalid, or the data would be stale
     * and inconsistent), it is valid to call {@link #invalidate()} to invalidate the data source,
     * and prevent further loading.
     *
     * @param params Parameters for the load, including the key for the new page, and requested load
     *               size.
     * @return ListenableFuture of the loaded data.
     */
    @NonNull
    public abstract ListenableFuture<Result<Key, Value>> loadBefore(
            @NonNull LoadParams<Key> params);
    /**
     * Append page with the key specified by
     * {@link PageKeyedDataSource.LoadParams#key LoadParams.key}.
     * <p>
     * It's valid to return a different list size than the page size if it's easier, e.g. if your
     * backend defines page sizes. It is generally preferred to increase the number loaded than
     * reduce.
     * <p>
     * If data cannot be loaded (for example, if the request is invalid, or the data would be stale
     * and inconsistent), it is valid to call {@link #invalidate()} to invalidate the data source,
     * and prevent further loading.
     *
     * @param params Parameters for the load, including the key for the new page, and requested load
     *               size.
     * @return ListenableFuture of the loaded data.
     */
    @NonNull
    public abstract ListenableFuture<Result<Key, Value>> loadAfter(
            @NonNull LoadParams<Key> params);

    @Nullable
    @Override
    Key getKey(@NonNull Value item) {
        return null;
    }

    @Override
    boolean supportsPageDropping() {
        /* To support page dropping when PageKeyed, we'll need to:
         *    - Stash keys for every page we have loaded (can id by index relative to loadInitial)
         *    - Drop keys for any page not adjacent to loaded content
         *    - And either:
         *        - Allow impl to signal previous page key: onResult(data, nextPageKey, prevPageKey)
         *        - Re-trigger loadInitial, and break assumption it will only occur once.
         */
        return false;
    }

    /**
     * Type produced by {@link #loadInitial(LoadInitialParams)} to represent
     * initially loaded data.
     *
     * @param <Key> Type of key used to identify pages.
     * @param <Value> Type of items being loaded by the DataSource.
     */
    public static class InitialResult<Key, Value> extends BaseResult<Value> {
        public InitialResult(@NonNull List<Value> data, int position, int totalCount,
                @Nullable Key previousPageKey, @Nullable Key nextPageKey) {
            super(data, previousPageKey, nextPageKey,
                    position, totalCount - data.size() - position, position, true);
        }

        public InitialResult(@NonNull List<Value> data, @Nullable Key previousPageKey,
                @Nullable Key nextPageKey) {
            super(data, previousPageKey, nextPageKey, 0, 0, 0, false);
        }
    }

    /**
     * Type produced by {@link #loadBefore(LoadParams)} and {@link #loadAfter(LoadParams)} to
     * represent a page of loaded data.
     *
     * @param <Key> Type of key used to identify pages.
     * @param <Value> Type of items being loaded by the DataSource.
     */
    public static class Result<Key, Value> extends BaseResult<Value> {
        public Result(@NonNull List<Value> data, @Nullable Key adjacentPageKey) {
            super(data, adjacentPageKey, adjacentPageKey, 0, 0, 0, false);
        }
    }
}
