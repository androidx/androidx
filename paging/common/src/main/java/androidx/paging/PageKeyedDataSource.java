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

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.util.Function;

import java.util.List;
import java.util.concurrent.Executor;

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
public abstract class PageKeyedDataSource<Key, Value> extends ContiguousDataSource<Key, Value> {
    private final Object mKeyLock = new Object();

    @Nullable
    @GuardedBy("mKeyLock")
    private Key mNextKey = null;

    @Nullable
    @GuardedBy("mKeyLock")
    private Key mPreviousKey = null;

    private void initKeys(@Nullable Key previousKey, @Nullable Key nextKey) {
        synchronized (mKeyLock) {
            mPreviousKey = previousKey;
            mNextKey = nextKey;
        }
    }

    private void setPreviousKey(@Nullable Key previousKey) {
        synchronized (mKeyLock) {
            mPreviousKey = previousKey;
        }
    }

    private void setNextKey(@Nullable Key nextKey) {
        synchronized (mKeyLock) {
            mNextKey = nextKey;
        }
    }

    private @Nullable Key getPreviousKey() {
        synchronized (mKeyLock) {
            return mPreviousKey;
        }
    }

    private @Nullable Key getNextKey() {
        synchronized (mKeyLock) {
            return mNextKey;
        }
    }

    /**
     * Holder object for inputs to {@link #loadInitial(LoadInitialParams, LoadInitialCallback)}.
     *
     * @param <Key> Type of data used to query pages.
     */
    @SuppressWarnings("WeakerAccess")
    public static class LoadInitialParams<Key> {
        /**
         * Requested number of items to load.
         * <p>
         * Note that this may be larger than available data.
         */
        public final int requestedLoadSize;

        /**
         * Defines whether placeholders are enabled, and whether the total count passed to
         * {@link LoadInitialCallback#onResult(List, int, int, Key, Key)} will be ignored.
         */
        public final boolean placeholdersEnabled;


        public LoadInitialParams(int requestedLoadSize, boolean placeholdersEnabled) {
            this.requestedLoadSize = requestedLoadSize;
            this.placeholdersEnabled = placeholdersEnabled;
        }
    }

    /**
     * Holder object for inputs to {@link #loadBefore(LoadParams, LoadCallback)} and
     * {@link #loadAfter(LoadParams, LoadCallback)}.
     *
     * @param <Key> Type of data used to query pages.
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
     * call the five parameter {@link #onResult(List, int, int, Object, Object)} to pass that
     * information. You can skip passing this information by calling the three parameter
     * {@link #onResult(List, Object, Object)}, either if it's difficult to compute, or if
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
    }

    static class LoadInitialCallbackImpl<Key, Value> extends LoadInitialCallback<Key, Value> {
        final LoadCallbackHelper<Value> mCallbackHelper;
        private final PageKeyedDataSource<Key, Value> mDataSource;
        private final boolean mCountingEnabled;
        LoadInitialCallbackImpl(@NonNull PageKeyedDataSource<Key, Value> dataSource,
                boolean countingEnabled, @NonNull PageResult.Receiver<Value> receiver) {
            mCallbackHelper = new LoadCallbackHelper<>(
                    dataSource, PageResult.INIT, null, receiver);
            mDataSource = dataSource;
            mCountingEnabled = countingEnabled;
        }

        @Override
        public void onResult(@NonNull List<Value> data, int position, int totalCount,
                @Nullable Key previousPageKey, @Nullable Key nextPageKey) {
            if (!mCallbackHelper.dispatchInvalidResultIfInvalid()) {
                LoadCallbackHelper.validateInitialLoadParams(data, position, totalCount);

                // setup keys before dispatching data, so guaranteed to be ready
                mDataSource.initKeys(previousPageKey, nextPageKey);

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
        public void onResult(@NonNull List<Value> data, @Nullable Key previousPageKey,
                @Nullable Key nextPageKey) {
            if (!mCallbackHelper.dispatchInvalidResultIfInvalid()) {
                mDataSource.initKeys(previousPageKey, nextPageKey);
                mCallbackHelper.dispatchResultToReceiver(new PageResult<>(data, 0, 0, 0));
            }
        }
    }

    static class LoadCallbackImpl<Key, Value> extends LoadCallback<Key, Value> {
        final LoadCallbackHelper<Value> mCallbackHelper;
        private final PageKeyedDataSource<Key, Value> mDataSource;
        LoadCallbackImpl(@NonNull PageKeyedDataSource<Key, Value> dataSource,
                @PageResult.ResultType int type, @Nullable Executor mainThreadExecutor,
                @NonNull PageResult.Receiver<Value> receiver) {
            mCallbackHelper = new LoadCallbackHelper<>(
                    dataSource, type, mainThreadExecutor, receiver);
            mDataSource = dataSource;
        }

        @Override
        public void onResult(@NonNull List<Value> data, @Nullable Key adjacentPageKey) {
            if (!mCallbackHelper.dispatchInvalidResultIfInvalid()) {
                if (mCallbackHelper.mResultType == PageResult.APPEND) {
                    mDataSource.setNextKey(adjacentPageKey);
                } else {
                    mDataSource.setPreviousKey(adjacentPageKey);
                }
                mCallbackHelper.dispatchResultToReceiver(new PageResult<>(data, 0, 0, 0));
            }
        }
    }

    @Nullable
    @Override
    final Key getKey(int position, Value item) {
        // don't attempt to persist keys, since we currently don't pass them to initial load
        return null;
    }

    @Override
    final void dispatchLoadInitial(@Nullable Key key, int initialLoadSize, int pageSize,
            boolean enablePlaceholders, @NonNull Executor mainThreadExecutor,
            @NonNull PageResult.Receiver<Value> receiver) {
        LoadInitialCallbackImpl<Key, Value> callback =
                new LoadInitialCallbackImpl<>(this, enablePlaceholders, receiver);
        loadInitial(new LoadInitialParams<Key>(initialLoadSize, enablePlaceholders), callback);

        // If initialLoad's callback is not called within the body, we force any following calls
        // to post to the UI thread. This constructor may be run on a background thread, but
        // after constructor, mutation must happen on UI thread.
        callback.mCallbackHelper.setPostExecutor(mainThreadExecutor);
    }


    @Override
    final void dispatchLoadAfter(int currentEndIndex, @NonNull Value currentEndItem,
            int pageSize, @NonNull Executor mainThreadExecutor,
            @NonNull PageResult.Receiver<Value> receiver) {
        @Nullable Key key = getNextKey();
        if (key != null) {
            loadAfter(new LoadParams<>(key, pageSize),
                    new LoadCallbackImpl<>(this, PageResult.APPEND, mainThreadExecutor, receiver));
        }
    }

    @Override
    final void dispatchLoadBefore(int currentBeginIndex, @NonNull Value currentBeginItem,
            int pageSize, @NonNull Executor mainThreadExecutor,
            @NonNull PageResult.Receiver<Value> receiver) {
        @Nullable Key key = getPreviousKey();
        if (key != null) {
            loadBefore(new LoadParams<>(key, pageSize),
                    new LoadCallbackImpl<>(this, PageResult.PREPEND, mainThreadExecutor, receiver));
        }
    }

    /**
     * Load initial data.
     * <p>
     * This method is called first to initialize a PagedList with data. If it's possible to count
     * the items that can be loaded by the DataSource, it's recommended to pass the loaded data to
     * the callback via the three-parameter
     * {@link LoadInitialCallback#onResult(List, int, int, Object, Object)}. This enables PagedLists
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
     * backend defines page sizes. It is generally safer to increase the number loaded than reduce.
     * <p>
     * Data may be passed synchronously during the load method, or deferred and called at a
     * later time. Further loads going down will be blocked until the callback is called.
     * <p>
     * If data cannot be loaded (for example, if the request is invalid, or the data would be stale
     * and inconsistent, it is valid to call {@link #invalidate()} to invalidate the data source,
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
     * backend defines page sizes. It is generally safer to increase the number loaded than reduce.
     * <p>
     * Data may be passed synchronously during the load method, or deferred and called at a
     * later time. Further loads going down will be blocked until the callback is called.
     * <p>
     * If data cannot be loaded (for example, if the request is invalid, or the data would be stale
     * and inconsistent, it is valid to call {@link #invalidate()} to invalidate the data source,
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
