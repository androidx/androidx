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

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Incremental data loader for paging keyed content, where loaded content uses previously loaded
 * items as input to future loads.
 * <p>
 * Implement a DataSource using ItemKeyedDataSource if you need to use data from item {@code N - 1}
 * to load item {@code N}. This is common, for example, in sorted database queries where
 * attributes of the item such just before the next query define how to execute it.
 * <p>
 * The {@code InMemoryByItemRepository} in the
 * <a href="https://github.com/googlesamples/android-architecture-components/blob/master/PagingWithNetworkSample/README.md">PagingWithNetworkSample</a>
 * shows how to implement a network ItemKeyedDataSource using
 * <a href="https://square.github.io/retrofit/">Retrofit</a>, while
 * handling swipe-to-refresh, network errors, and retry.
 *
 * @param <Key> Type of data used to query Value types out of the DataSource.
 * @param <Value> Type of items being loaded by the DataSource.
 */
public abstract class ItemKeyedDataSource<Key, Value> extends ContiguousDataSource<Key, Value> {

    /**
     * Holder object for inputs to {@link #loadInitial(LoadInitialParams, LoadInitialCallback)}.
     *
     * @param <Key> Type of data used to query Value types out of the DataSource.
     */
    @SuppressWarnings("WeakerAccess")
    public static class LoadInitialParams<Key> {
        /**
         * Load items around this key, or at the beginning of the data set if {@code null} is
         * passed.
         * <p>
         * Note that this key is generally a hint, and may be ignored if you want to always load
         * from the beginning.
         */
        @Nullable
        public final Key requestedInitialKey;

        /**
         * Requested number of items to load.
         * <p>
         * Note that this may be larger than available data.
         */
        public final int requestedLoadSize;

        /**
         * Defines whether placeholders are enabled, and whether the total count passed to
         * {@link LoadInitialCallback#onResult(List, int, int)} will be ignored.
         */
        public final boolean placeholdersEnabled;


        public LoadInitialParams(@Nullable Key requestedInitialKey, int requestedLoadSize,
                boolean placeholdersEnabled) {
            this.requestedInitialKey = requestedInitialKey;
            this.requestedLoadSize = requestedLoadSize;
            this.placeholdersEnabled = placeholdersEnabled;
        }
    }

    /**
     * Holder object for inputs to {@link #loadBefore(LoadParams, LoadCallback)}
     * and {@link #loadAfter(LoadParams, LoadCallback)}.
     *
     * @param <Key> Type of data used to query Value types out of the DataSource.
     */
    @SuppressWarnings("WeakerAccess")
    public static class LoadParams<Key> {
        /**
         * Load items before/after this key.
         * <p>
         * Returned data must begin directly adjacent to this position.
         */
        public final Key key;
        /**
         * Requested number of items to load.
         * <p>
         * Returned page can be of this size, but it may be altered if that is easier, e.g. a
         * network data source where the backend defines page size.
         */
        public final int requestedLoadSize;

        public LoadParams(Key key, int requestedLoadSize) {
            this.key = key;
            this.requestedLoadSize = requestedLoadSize;
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
    }

    static class LoadInitialCallbackImpl<Value> extends LoadInitialCallback<Value> {
        final LoadCallbackHelper<Value> mCallbackHelper;
        private final boolean mCountingEnabled;
        LoadInitialCallbackImpl(@NonNull ItemKeyedDataSource dataSource, boolean countingEnabled,
                @NonNull PageResult.Receiver<Value> receiver) {
            mCallbackHelper = new LoadCallbackHelper<>(dataSource, PageResult.INIT, null, receiver);
            mCountingEnabled = countingEnabled;
        }

        @Override
        public void onResult(@NonNull List<Value> data, int position, int totalCount) {
            if (!mCallbackHelper.dispatchInvalidResultIfInvalid()) {
                LoadCallbackHelper.validateInitialLoadParams(data, position, totalCount);

                int trailingUnloadedCount = totalCount - position - data.size();
                if (mCountingEnabled) {
                    mCallbackHelper.dispatchResultToReceiver(new PageResult<>(
                            data, position, trailingUnloadedCount, 0));
                } else {
                    mCallbackHelper.dispatchResultToReceiver(new PageResult<>(data, position));
                }
            }
        }

        @Override
        public void onResult(@NonNull List<Value> data) {
            if (!mCallbackHelper.dispatchInvalidResultIfInvalid()) {
                mCallbackHelper.dispatchResultToReceiver(new PageResult<>(data, 0, 0, 0));
            }
        }
    }

    static class LoadCallbackImpl<Value> extends LoadCallback<Value> {
        final LoadCallbackHelper<Value> mCallbackHelper;

        LoadCallbackImpl(@NonNull ItemKeyedDataSource dataSource, @PageResult.ResultType int type,
                @Nullable Executor mainThreadExecutor,
                @NonNull PageResult.Receiver<Value> receiver) {
            mCallbackHelper = new LoadCallbackHelper<>(
                    dataSource, type, mainThreadExecutor, receiver);
        }

        @Override
        public void onResult(@NonNull List<Value> data) {
            if (!mCallbackHelper.dispatchInvalidResultIfInvalid()) {
                mCallbackHelper.dispatchResultToReceiver(new PageResult<>(data, 0, 0, 0));
            }
        }
    }

    @Nullable
    @Override
    final Key getKey(int position, Value item) {
        if (item == null) {
            return null;
        }

        return getKey(item);
    }

    @Override
    final void dispatchLoadInitial(@Nullable Key key, int initialLoadSize, int pageSize,
            boolean enablePlaceholders, @NonNull Executor mainThreadExecutor,
            @NonNull PageResult.Receiver<Value> receiver) {
        LoadInitialCallbackImpl<Value> callback =
                new LoadInitialCallbackImpl<>(this, enablePlaceholders, receiver);
        loadInitial(new LoadInitialParams<>(key, initialLoadSize, enablePlaceholders), callback);

        // If initialLoad's callback is not called within the body, we force any following calls
        // to post to the UI thread. This constructor may be run on a background thread, but
        // after constructor, mutation must happen on UI thread.
        callback.mCallbackHelper.setPostExecutor(mainThreadExecutor);
    }

    @Override
    final void dispatchLoadAfter(int currentEndIndex, @NonNull Value currentEndItem,
            int pageSize, @NonNull Executor mainThreadExecutor,
            @NonNull PageResult.Receiver<Value> receiver) {
        loadAfter(new LoadParams<>(getKey(currentEndItem), pageSize),
                new LoadCallbackImpl<>(this, PageResult.APPEND, mainThreadExecutor, receiver));
    }

    @Override
    final void dispatchLoadBefore(int currentBeginIndex, @NonNull Value currentBeginItem,
            int pageSize, @NonNull Executor mainThreadExecutor,
            @NonNull PageResult.Receiver<Value> receiver) {
        loadBefore(new LoadParams<>(getKey(currentBeginItem), pageSize),
                new LoadCallbackImpl<>(this, PageResult.PREPEND, mainThreadExecutor, receiver));
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
     * initializing at the same location. If your data source never invalidates (for example,
     * loading from the network without the network ever signalling that old data must be reloaded),
     * it's fine to ignore the {@code initialLoadKey} and always start from the beginning of the
     * data set.
     *
     * @param params Parameters for initial load, including initial key and requested size.
     * @param callback Callback that receives initial load data.
     */
    public abstract void loadInitial(@NonNull LoadInitialParams<Key> params,
            @NonNull LoadInitialCallback<Value> callback);

    /**
     * Load list data after the key specified in {@link LoadParams#key LoadParams.key}.
     * <p>
     * It's valid to return a different list size than the page size if it's easier, e.g. if your
     * backend defines page sizes. It is generally safer to increase the number loaded than reduce.
     * <p>
     * Data may be passed synchronously during the loadAfter method, or deferred and called at a
     * later time. Further loads going down will be blocked until the callback is called.
     * <p>
     * If data cannot be loaded (for example, if the request is invalid, or the data would be stale
     * and inconsistent, it is valid to call {@link #invalidate()} to invalidate the data source,
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
     * backend defines page sizes. It is generally safer to increase the number loaded than reduce.
     * <p>
     * <p class="note"><strong>Note:</strong> Data returned will be prepended just before the key
     * passed, so if you vary size, ensure that the last item is adjacent to the passed key.
     * <p>
     * Data may be passed synchronously during the loadBefore method, or deferred and called at a
     * later time. Further loads going up will be blocked until the callback is called.
     * <p>
     * If data cannot be loaded (for example, if the request is invalid, or the data would be stale
     * and inconsistent, it is valid to call {@link #invalidate()} to invalidate the data source,
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
