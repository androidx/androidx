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

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Incremental data loader for paging keyed content, where loaded content uses previously loaded
 * items as input to future loads.
 * <p>
 * Implement a DataSource using KeyedDataSource if you need to use data from item {@code N - 1}
 * to load item {@code N}. This is common, for example, in sorted database queries where
 * attributes of the item such just before the next query define how to execute it.
 *
 * @param <Key> Type of data used to query Value types out of the DataSource.
 * @param <Value> Type of items being loaded by the DataSource.
 */
public abstract class KeyedDataSource<Key, Value> extends ContiguousDataSource<Key, Value> {

    /**
     * Callback for KeyedDataSource initial loading methods to return data and (optionally)
     * position/count information.
     * <p>
     * A callback can be called only once, and will throw if called again.
     * <p>
     * It is always valid for a DataSource loading method that takes a callback to stash the
     * callback and call it later. This enables DataSources to be fully asynchronous, and to handle
     * temporary, recoverable error states (such as a network error that can be retried).
     *
     * @param <T> Type of items being loaded.
     */
    public static class InitialLoadCallback<T> extends LoadCallback<T> {
        private final boolean mCountingEnabled;
        InitialLoadCallback(@NonNull KeyedDataSource dataSource, boolean countingEnabled,
                @NonNull PageResult.Receiver<T> receiver) {
            super(dataSource, PageResult.INIT, null, receiver);
            mCountingEnabled = countingEnabled;
        }

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
        public void onResult(@NonNull List<T> data, int position, int totalCount) {
            validateInitialLoadParams(data, position, totalCount);

            int trailingUnloadedCount = totalCount - position - data.size();
            if (mCountingEnabled) {
                dispatchResultToReceiver(new PageResult<>(
                        data, position, trailingUnloadedCount, 0));
            } else {
                dispatchResultToReceiver(new PageResult<>(data, position));
            }
        }
    }

    /**
     * Callback for KeyedDataSource {@link #loadBefore(Object, int, LoadCallback)}
     * and {@link #loadAfter(Object, int, LoadCallback)} methods to return data.
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
        LoadCallback(@NonNull KeyedDataSource dataSource, @PageResult.ResultType int type,
                @Nullable Executor mainThreadExecutor, @NonNull PageResult.Receiver<T> receiver) {
            super(type, dataSource, mainThreadExecutor, receiver);
        }

        /**
         * Called to pass loaded data from a DataSource.
         * <p>
         * Call this method from your KeyedDataSource's
         * {@link #loadBefore(Object, int, LoadCallback)} and
         * {@link #loadAfter(Object, int, LoadCallback)} methods to return data.
         * <p>
         * Call this from {@link #loadInitial(Object, int, boolean, InitialLoadCallback)} to
         * initialize without counting available data, or supporting placeholders.
         * <p>
         * It is always valid to pass a different amount of data than what is requested. Pass an
         * empty list if there is no more data to load.
         *
         * @param data List of items loaded from the KeyedDataSource.
         */
        public void onResult(@NonNull List<T> data) {
            dispatchResultToReceiver(new PageResult<>(data, 0, 0, 0));
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
    public void loadInitial(@Nullable Key key, int initialLoadSize, int pageSize,
            boolean enablePlaceholders, @NonNull Executor mainThreadExecutor,
            @NonNull PageResult.Receiver<Value> receiver) {
        InitialLoadCallback<Value> callback =
                new InitialLoadCallback<>(this, enablePlaceholders, receiver);
        loadInitial(key, initialLoadSize, enablePlaceholders, callback);

        // If initialLoad's callback is not called within the body, we force any following calls
        // to post to the UI thread. This constructor may be run on a background thread, but
        // after constructor, mutation must happen on UI thread.
        callback.setPostExecutor(mainThreadExecutor);
    }

    @Override
    void loadAfter(int currentEndIndex, @NonNull Value currentEndItem, int pageSize,
            @NonNull Executor mainThreadExecutor, @NonNull PageResult.Receiver<Value> receiver) {
        loadAfter(getKey(currentEndItem), pageSize,
                new LoadCallback<>(this, PageResult.APPEND, mainThreadExecutor, receiver));
    }

    @Override
    void loadBefore(int currentBeginIndex, @NonNull Value currentBeginItem, int pageSize,
            @NonNull Executor mainThreadExecutor, @NonNull PageResult.Receiver<Value> receiver) {
        loadBefore(getKey(currentBeginItem), pageSize,
                new LoadCallback<>(this, PageResult.PREPEND, mainThreadExecutor, receiver));
    }

    /**
     * Load initial data.
     * <p>
     * This method is called first to initialize a PagedList with data. If it's possible to count
     * the items that can be loaded by the DataSource, it's recommended to pass the loaded data to
     * the callback via the three-parameter
     * {@link InitialLoadCallback#onResult(List, int, int)}. This enables PagedLists
     * presenting data from this source to display placeholders to represent unloaded items.
     * <p>
     * {@code initialLoadKey} and {@code requestedLoadSize} are hints, not requirements, so if it is
     * difficult or impossible to respect them, they may be altered. Note that ignoring the
     * {@code initialLoadKey} can prevent subsequent PagedList/DataSource pairs from initializing at
     * the same location. If your data source never invalidates (for example, loading from the
     * network without the network ever signalling that old data must be reloaded), it's fine to
     * ignore the {@code initialLoadKey} and always start from the beginning of the data set.
     *
     * @param initialLoadKey Load items around this key, or at the beginning of the data set if null
     *                       is passed.
     * @param requestedLoadSize Suggested number of items to load.
     * @param enablePlaceholders Signals whether counting is requested. If false, you can
     *                           potentially save work by calling the single-parameter variant of
     *                           {@link LoadCallback#onResult(List)} and not counting the
     *                           number of items in the data set.
     * @param callback DataSource.LoadCallback that receives initial load data.
     */
    public abstract void loadInitial(@Nullable Key initialLoadKey, int requestedLoadSize,
            boolean enablePlaceholders, @NonNull InitialLoadCallback<Value> callback);

    /**
     * Load list data after the specified item.
     * <p>
     * It's valid to return a different list size than the page size, if it's easier for this data
     * source. It is generally safer to increase the number loaded than reduce.
     * <p>
     * Data may be passed synchronously during the loadAfter method, or deferred and called at a
     * later time. Further loads going down will be blocked until the callback is called.
     * <p>
     * If data cannot be loaded (for example, if the request is invalid, or the data would be stale
     * and inconsistent, it is valid to call {@link #invalidate()} to invalidate the data source,
     * and prevent further loading.
     *
     * @param currentEndKey Load items after this key. May be null on initial load, to indicate load
     *                      from beginning.
     * @param pageSize Suggested number of items to load.
     * @param callback DataSource.LoadCallback that receives loaded data.
     */
    public abstract void loadAfter(@NonNull Key currentEndKey, int pageSize,
            @NonNull LoadCallback<Value> callback);

    /**
     * Load data before the currently loaded content.
     * <p>
     * It's valid to return a different list size than the page size, if it's easier for this data
     * source. It is generally safer to increase the number loaded than reduce. Note that the last
     * item returned must be directly adjacent to the key passed, so varying size from the pageSize
     * requested should effectively grow or shrink the list by modifying the beginning, not the end.
     * <p>
     * Data may be passed synchronously during the loadBefore method, or deferred and called at a
     * later time. Further loads going up will be blocked until the callback is called.
     * <p>
     * If data cannot be loaded (for example, if the request is invalid, or the data would be stale
     * and inconsistent, it is valid to call {@link #invalidate()} to invalidate the data source,
     * and prevent further loading.
     * <p class="note"><strong>Note:</strong> Data must be returned in the order it will be
     * presented in the list.
     *
     * @param currentBeginKey Load items before this key.
     * @param pageSize Suggested number of items to load.
     * @param callback DataSource.LoadCallback that receives loaded data.
     */
    public abstract void loadBefore(@NonNull Key currentBeginKey, int pageSize,
            @NonNull LoadCallback<Value> callback);

    /**
     * Return a key associated with the given item.
     * <p>
     * If your KeyedDataSource is loading from a source that is sorted and loaded by a unique
     * integer ID, you would return {@code item.getID()} here. This key can then be passed to
     * {@link #loadBefore(Object, int, LoadCallback)} or
     * {@link #loadAfter(Object, int, LoadCallback)} to load additional items adjacent to the item
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
}
